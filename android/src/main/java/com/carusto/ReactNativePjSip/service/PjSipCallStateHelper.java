package com.carusto.ReactNativePjSip.service;

import android.app.Service;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.util.Log;

import com.carusto.ReactNativePjSip.PjEventType;
import com.carusto.ReactNativePjSip.PjSipAccount;
import com.carusto.ReactNativePjSip.PjSipCall;
import com.carusto.ReactNativePjSip.PjSipMessage;
import com.carusto.ReactNativePjSip.PjSipService;

import org.pjsip.pjsua2.AudDevManager;
import org.pjsip.pjsua2.OnCallStateParam;
import org.pjsip.pjsua2.OnRegStateParam;
import org.pjsip.pjsua2.pjsip_inv_state;

import java.util.ArrayList;
import java.util.List;

public class PjSipCallStateHelper implements PjSipCallStateListener, AutoCloseable {
    private final WifiManager mWifiManager;
    private final PowerManager mPowerManager;
    private final List<PjSipCall> mCalls = new ArrayList<>();
    private WifiManager.WifiLock mWifiLock;

    private PowerManager.WakeLock mIncallWakeLock;

    private final PjSipService service;
    private static final String TAG = "PjSipCallStateHelper";

    public PjSipCallStateHelper(PjSipService service) {
        this.service = service;
        mPowerManager = (PowerManager) service.getApplicationContext().getSystemService(Service.POWER_SERVICE);
        mWifiManager = (WifiManager) service.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiLock = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, service.getPackageName() + "-wifi-call-lock");
        mWifiLock.setReferenceCounted(false);
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    void emitCallStateChanged(PjSipCall call, OnCallStateParam prm) {
        try {
            if (call.getInfo().getState() == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) {
                emitCallTerminated(call, prm);
            } else {
                emitCallChanged(call, prm);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to handle call state event", e);
        }
    }

    void emitCallChanged(PjSipCall call, OnCallStateParam prm) {
        try {
            final int callId = call.getId();
            final int callState = call.getInfo().getState();
            service.getForegroundHelper().putToForeground(call.getInfo().getRemoteUri());
            service.job(new Runnable() {
                @Override
                public void run() {

                    // Acquire wake lock
                    if (mIncallWakeLock == null) {
                        mIncallWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pjsip:" + TAG);
                    }
                    if (!mIncallWakeLock.isHeld()) {
                        mIncallWakeLock.acquire();
                    }

                    // Acquire wifi lock
                    mWifiLock.acquire();

                    if (callState == pjsip_inv_state.PJSIP_INV_STATE_EARLY || callState == pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED) {
                        service.getAudioHelper().callStarted();
                    }
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Failed to retrieve call state", e);
        }

        service.fireEvent(PjEventType.EVENT_CALL_CHANGED, call.toJson());
    }

    void emitCallTerminated(PjSipCall call, OnCallStateParam prm) {
        final int callId = call.getId();

        service.job(() -> {
            // Release wake lock
            if (mCalls.size() == 1) {
                if (mIncallWakeLock != null && mIncallWakeLock.isHeld()) {
                    mIncallWakeLock.release();
                }
            }

            // Reset audio settings
            if (mCalls.size() == 1) {
                mWifiLock.release();
                service.getAudioHelper().callClosed();
                service.getForegroundHelper().removeFromForeground();

            }
        });

        service.fireEvent(PjEventType.EVENT_CALL_TERMINATED, call.toJson());
        evict(call);
    }

    @Override
    public void emitCallUpdated(PjSipCall call) {
        service.fireEvent(PjEventType.EVENT_CALL_CHANGED, call.toJson());
    }

    @Override
    public AudDevManager getAudDevManager() {
        return service.getAudDevManager();
    }

    /**
     * Pauses all calls, used when received GSM call.
     */
    private void doPauseAllCalls() {
        for (PjSipCall call : mCalls) {
            try {
                call.hold();
            } catch (Exception e) {
                Log.w(TAG, "Failed to put call on hold", e);
            }
        }
    }

    @Override
    public void emitRegistrationChanged(PjSipAccount account, OnRegStateParam prm) {
        service.fireEvent(PjEventType.EVENT_REGISTRATION_CHANGED, account.toJson());
    }

    @Override
   public void emitMessageReceived(PjSipAccount account, PjSipMessage message) {
        service.fireEvent(PjEventType.EVENT_MESSAGE_RECEIVED, message.toJson());
    }


    @Override
    public void emitCallReceived(PjSipAccount account, PjSipCall call) {
        // TODO: How do we handl the GSM stuff..
        // Should anyway be in TelephonyHelper

        // Automatically decline incoming call when user uses GSM
        /*
        if (!mGSMIdle) {
            try {
                call.hangup(new CallOpParam(true));
            } catch (Exception e) {
                Log.w(TAG, "Failed to decline incoming call when user uses GSM", e);
            }

            return;
        }
*/

        // -----
        mCalls.add(call);
        service.fireEvent(PjEventType.EVENT_CALL_RECEIVED, call.toJson());
    }

    public void addCall(PjSipCall call) {
        mCalls.add(call);
    }

    public void evict(PjSipCall call) {
        mCalls.remove(call);
    }

    public List<PjSipCall> getCalls() {
        return mCalls;
    }
}
