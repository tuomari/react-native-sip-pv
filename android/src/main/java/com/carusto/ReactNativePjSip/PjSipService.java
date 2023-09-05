package com.carusto.ReactNativePjSip;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MicrophoneInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Process;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.carusto.ReactNativePjSip.dto.AccountConfigurationDTO;
import com.carusto.ReactNativePjSip.dto.CallSettingsDTO;
import com.carusto.ReactNativePjSip.dto.ServiceConfigurationDTO;
import com.carusto.ReactNativePjSip.dto.SipMessageDTO;
import com.carusto.ReactNativePjSip.service.PjSipServiceAudiohelper;
import com.carusto.ReactNativePjSip.utils.ArgumentUtils;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.pjsip.pjsua2.*;

import java.util.*;

public class PjSipService extends Service {

    private static String TAG = "PjSipService";
    public static final String CHANNEL_ID = "IccPjSIPForegroundServiceChannel";
    private NotificationCompat.Builder mNotificationBuilder;
    private NotificationManager mnotificationManager;
    private boolean mInitialized;

    private HandlerThread mWorkerThread;

    private Handler mHandler;

    private Endpoint mEndpoint;


    private final Map<String, Integer> transportIds = new HashMap<>();

    private ServiceConfigurationDTO mServiceConfiguration = new ServiceConfigurationDTO();

    private PjSipLogWriter mLogWriter;

    private PjSipBroadcastEmiter mEmitter;

    private List<PjSipAccount> mAccounts = new ArrayList<>();

    private List<PjSipCall> mCalls = new ArrayList<>();

    // In order to ensure that GC will not destroy objects that are used in PJSIP
    // Also there is limitation of pjsip that thread should be registered first before working with library
    // (but we couldn't register GC thread in pjsip)
    private List<Object> mTrash = new LinkedList<>();

    private PowerManager mPowerManager;

    private PowerManager.WakeLock mIncallWakeLock;

    private TelephonyManager mTelephonyManager;

    private WifiManager mWifiManager;

    private WifiManager.WifiLock mWifiLock;

    private boolean mGSMIdle;

    private BroadcastReceiver mPhoneStateChangedReceiver = new PhoneStateChangedReceiver();
    private PjSipServiceAudiohelper mAudioHelper;

    public PjSipBroadcastEmiter getEmitter() {
        return mEmitter;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public PjSipService() {
        super();
        Log.w(TAG, "Constructor of PjSipService");
    }

    private void load() {
        // Load native libraries
            /*
        Log.i(TAG, "Loading PjsipService");
        try {
            System.loadLibrary("openh264");
        } catch (UnsatisfiedLinkError error) {
            Log.e(TAG, "Error while loading OpenH264 native library", error);
            throw new RuntimeException(error);
        }
*/
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

        try {
            System.loadLibrary("pjsua2");
        } catch (UnsatisfiedLinkError error) {
            Log.e(TAG, "Error while loading PJSIP pjsua2 native library", error);
            throw new RuntimeException(error);
        }
        Log.i(TAG, "Loaded pjsip module");

        // Start stack
        try {
            Log.i(TAG, "Creating endpoint");

            mEndpoint = new Endpoint();
            Log.i(TAG, "Libcreate on endpoint");

            mEndpoint.libCreate();
            Log.i(TAG, "LibRegisterThread on endpoint");
            final String threadName = Thread.currentThread().getName();

            // if(mEndpoint.libIsThreadRegistered()){
            if (false) {
                Log.e(TAG, "Thread is registerd. Not registering again" + threadName);
            } else {
                Log.e(TAG, "Thread is not registered.. wtf... " + threadName);

            }
            //mEndpoint.libRegisterThread(Thread.currentThread().getName());

            Log.i(TAG, "Created enpoint");

            // Register main thread
            mHandler.post(() -> {
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                    Log.e(TAG, "Registering thread in runnable");
                    mEndpoint.libRegisterThread(Thread.currentThread().getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            final ServiceConfigurationDTO cfg = mServiceConfiguration;
            // Configure endpoint
            EpConfig epConfig = new EpConfig();
            epConfig.getMedConfig().setHasIoqueue(true);

            if (cfg.getMsgLogging() != null)
                epConfig.getLogConfig().setMsgLogging(cfg.getMsgLogging());
            if (cfg.getLogLevel() != null)
                epConfig.getLogConfig().setLevel(cfg.getLogLevel());
            if (cfg.getConsoleLogLevel() != null)
                epConfig.getLogConfig().setConsoleLevel(cfg.getConsoleLogLevel());

            mLogWriter = new PjSipLogWriter();
            epConfig.getLogConfig().setWriter(mLogWriter);

            if (mServiceConfiguration.isUserAgentNotEmpty()) {
                epConfig.getUaConfig().setUserAgent(mServiceConfiguration.getUserAgent());
            } else {
                epConfig.getUaConfig().setUserAgent("React Native PjSip (" + mEndpoint.libVersion().getFull() + ")");
            }

            if (mServiceConfiguration.isStunServersNotEmpty()) {
                Log.e(TAG, "Stun servers found" + mServiceConfiguration.getStunServers());
                epConfig.getUaConfig().setStunServer(mServiceConfiguration.getStunServers());
            } else {
                Log.e(TAG, "No stun servers found");
            }

            Log.e(TAG, "Setting noVad");

            epConfig.getMedConfig().setNoVad(cfg.getNoVad());
            if (cfg.getMediaClockRate() != null)
                epConfig.getMedConfig().setClockRate(cfg.getMediaClockRate());
            if (cfg.getMediaQuality() != null)
                epConfig.getMedConfig().setQuality(cfg.getMediaQuality());
            if (cfg.getEcOptions() != null)
                epConfig.getMedConfig().setEcOptions(cfg.getEcOptions());
            if (cfg.getEcTailLen() != null)
                epConfig.getMedConfig().setEcTailLen(cfg.getEcTailLen());
            if (cfg.getMediaThreadCount() != null)
                epConfig.getMedConfig().setThreadCnt(cfg.getMediaThreadCount());
            else {
                epConfig.getMedConfig().setThreadCnt(1);
            }


            mEndpoint.libInit(epConfig);

            mTrash.add(epConfig);

            // Configure transports
            {
                Log.w(TAG, "Creating UDP transport");
                TransportConfig transportConfig = new TransportConfig();
                //transportConfig.setPort(59483);
                //transportConfig.setRandomizePort(true);
                //transportConfig.setPublicAddress("2.58.220.35");
                transportConfig.setQosType(pj_qos_type.PJ_QOS_TYPE_VOICE);
                transportIds.put("UDP", mEndpoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, transportConfig));
                mTrash.add(transportConfig);
            }
            {
                Log.w(TAG, "Creating TCP transport");
                TransportConfig transportConfig = new TransportConfig();
                //transportConfig.setPort(59488);
                //transportConfig.setRandomizePort(true);
                transportConfig.setQosType(pj_qos_type.PJ_QOS_TYPE_VOICE);
                transportIds.put("TCP", mEndpoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TCP, transportConfig));
                mTrash.add(transportConfig);
            }
            {
                Log.w(TAG, "Creating TLS transport");

                TransportConfig transportConfig = new TransportConfig();
                //transportConfig.setRandomizePort(true);
                transportConfig.setQosType(pj_qos_type.PJ_QOS_TYPE_VOICE);
                transportIds.put("TLS", mEndpoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TLS, transportConfig));
                mTrash.add(transportConfig);
            }


            mEndpoint.libStart();


        } catch (Exception e) {
            Log.e(TAG, "Error while starting PJSIP", e);
        }
    }


    @NotNull
    public Map<String, Integer> getTransportIds() {
        return transportIds;
    }

    private static final int CALL_NOTIFICATION_ID = 1;

    public void removeFromForeground() {
        mNotificationBuilder = null;
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    public void putToForeground(String destination) {

        String notificationText = null;
        try {
            if (destination != null && !destination.isBlank()) {
                int startIdx = destination.indexOf(':');
                int endIdx = destination.indexOf('@');

                if (endIdx > 0) {
                    notificationText = destination.substring(startIdx + 1, endIdx);
                }
            }
        } catch (Exception e) {
            // never throw exception while parsing phone number
        }

        if (mNotificationBuilder != null) {
            Log.w(TAG, "Notification already exists. Only update.. with " + notificationText);
            if (notificationText != null) {
                mNotificationBuilder.setContentText(notificationText);
                mnotificationManager.notify(CALL_NOTIFICATION_ID, mNotificationBuilder.build());
            }
            return;
        }

        if (notificationText == null) {
            notificationText = "-";
        }

        this.mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);

        Intent notificationIntent = getPackageManager()
            .getLaunchIntentForPackage(getPackageName())
            .setPackage(null)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);


        //notificationIntent.setPackage("com.mobileclientv2");
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = mNotificationBuilder
            .setContentTitle("Active call in ICC Manager")
            .setContentText(notificationText)
            //´.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSmallIcon(R.drawable.phone_icon)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_CALL)
            .build();
        startForeground(CALL_NOTIFICATION_ID, notification);

    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Log.w(TAG, "StartCmd for pjsip");
        if (!mInitialized) {
            if (intent != null && intent.hasExtra("service")) {
                mServiceConfiguration = ServiceConfigurationDTO.fromMap((Map) intent.getSerializableExtra("service"));
            }

            mWorkerThread = new HandlerThread(getClass().getSimpleName(), Process.THREAD_PRIORITY_FOREGROUND);
            mWorkerThread.setPriority(Thread.MAX_PRIORITY);
            mWorkerThread.start();
            mHandler = new Handler(mWorkerThread.getLooper());
            mEmitter = new PjSipBroadcastEmiter(this);
            mAudioHelper = new PjSipServiceAudiohelper((AudioManager) getApplicationContext().getSystemService(AUDIO_SERVICE), mEmitter);

            mPowerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
            mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            mWifiLock = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, this.getPackageName() + "-wifi-call-lock");
            mWifiLock.setReferenceCounted(false);
            mTelephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            mGSMIdle = mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE;
            IntentFilter phoneStateFilter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
            registerReceiver(mPhoneStateChangedReceiver, phoneStateFilter);
            NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "PjSip goreground Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            );
            this.mnotificationManager = getSystemService(NotificationManager.class);
            mnotificationManager.createNotificationChannel(serviceChannel);
            mInitialized = true;
            Log.w(TAG, "Start command starting load job");

            job(() -> load());
        }

        if (intent != null) {
            job(() -> handle(intent));
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mWorkerThread.quitSafely();
        }

        try {
            if (mEndpoint != null) {
                mEndpoint.libDestroy();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to destroy PjSip library", e);
        }

        unregisterReceiver(mPhoneStateChangedReceiver);

        super.onDestroy();
    }

    private void job(Runnable job) {
        mHandler.post(job);
    }

    protected synchronized AudDevManager getAudDevManager() {
        return mEndpoint.audDevManager();
    }

    public void evict(final PjSipAccount account) {
        if (mHandler.getLooper().getThread() != Thread.currentThread()) {
            job(() -> evict(account));
            return;
        }

        // Remove link to account
        mAccounts.remove(account);

        // Remove transport
        try {
            mEndpoint.transportClose(account.getTransportId());
        } catch (Exception e) {
            Log.w(TAG, "Failed to close transport for account", e);
        }

        // Remove account in PjSip
        account.delete();

    }

    public void evict(final PjSipCall call) {
        if (mHandler.getLooper().getThread() != Thread.currentThread()) {
            job(() -> evict(call));
            return;
        }

        mCalls.remove(call);
        call.delete();
    }


    private void handle(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        Log.d(TAG, "Handle \"" + intent.getAction() + "\" action (" + ArgumentUtils.dumpIntentExtraParameters(intent) + ")");
        PjActionType actionType = PjActionType.findActionByName(intent.getAction());
        if (actionType == null) {
            Log.w(TAG, "No action found for action string: " + intent.getAction());
            return;
        }
        if (actionType.intentHandler != null) {
            actionType.intentHandler.handle(this, intent);
        }
    }


    public void updateServiceConfiguration(ServiceConfigurationDTO configuration) {
        mServiceConfiguration = configuration;
    }

    public ServiceConfigurationDTO getServiceConfiguration() {
        return mServiceConfiguration;
    }


    public void addAccount(Account account) {
        mAccounts.add(account);
    }

    public PjSipAccount findAccount(int id) throws Exception {
        for (PjSipAccount account : mAccounts) {
            if (account.getId() == id) {
                return account;
            }
        }

        throw new Exception("Account with specified \"" + id + "\" id not found");
    }

    public PjSipCall findCall(int id) throws Exception {
        for (PjSipCall call : mCalls) {
            if (call.getId() == id) {
                return call;
            }
        }

        throw new Exception("Call with specified \"" + id + "\" id not found");
    }

    void emmitRegistrationChanged(PjSipAccount account, OnRegStateParam prm) {
        this.fireEvent(PjEventType.EVENT_REGISTRATION_CHANGED, account.toJson());
    }

    void emmitMessageReceived(PjSipAccount account, PjSipMessage message) {
        this.fireEvent(PjEventType.EVENT_MESSAGE_RECEIVED, message.toJson());
    }


    public void fireEvent(PjEventType type, JSONObject data){
        Intent intent = new Intent();
        intent.setAction(type.eventName);
        intent.putExtra("data", data.toString());

        this.sendBroadcast(intent);
    }

    
    void emmitCallReceived(PjSipAccount account, PjSipCall call) {
        // Automatically decline incoming call when user uses GSM
        if (!mGSMIdle) {
            try {
                call.hangup(new CallOpParam(true));
            } catch (Exception e) {
                Log.w(TAG, "Failed to decline incoming call when user uses GSM", e);
            }

            return;
        }

        /**
         // Automatically start application when incoming call received.
         if (mAppHidden) {
         try {
         String ns = getApplicationContext().getPackageName();
         String cls = ns + ".MainActivity";

         Intent intent = new Intent(getApplicationContext(), Class.forName(cls));
         intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.EXTRA_DOCK_STATE_CAR);
         intent.addCategory(Intent.CATEGORY_LAUNCHER);
         intent.putExtra("foreground", true);

         startActivity(intent);
         } catch (Exception e) {
         Log.w(TAG, "Failed to open application on received call", e);
         }
         }

         job(new Runnable() {
        @Override public void run() {
        // Brighten screen at least 10 seconds
        PowerManager.WakeLock wl = mPowerManager.newWakeLock(
        PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE | PowerManager.FULL_WAKE_LOCK,
        "incoming_call"
        );
        wl.acquire(10000);

        if (mCalls.size() == 0) {
        mAudioManager.setSpeakerphoneOn(true);
        }
        }
        });
         **/

        // -----
        mCalls.add(call);
        this.fireEvent(PjEventType.EVENT_CALL_RECEIVED, call.toJson());
    }

    void emmitCallStateChanged(PjSipCall call, OnCallStateParam prm) {
        try {
            if (call.getInfo().getState() == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) {
                emmitCallTerminated(call, prm);
            } else {
                emmitCallChanged(call, prm);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to handle call state event", e);
        }
    }

    void emmitCallChanged(PjSipCall call, OnCallStateParam prm) {
        try {
            final int callId = call.getId();
            final int callState = call.getInfo().getState();
            putToForeground(call.getInfo().getRemoteUri());
            job(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Log.d(TAG, "Call changed Foreground service type: " + getForegroundServiceType());
                    }

                    // Acquire wake lock
                    if (mIncallWakeLock == null) {
                        mIncallWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
                    }
                    if (!mIncallWakeLock.isHeld()) {
                        mIncallWakeLock.acquire();
                    }

                    // Acquire wifi lock
                    mWifiLock.acquire();

                    if (callState == pjsip_inv_state.PJSIP_INV_STATE_EARLY || callState == pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED) {
                        mAudioHelper.callStarted();
                    }
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Failed to retrieve call state", e);
        }

        this.fireEvent(PjEventType.EVENT_CALL_CHANGED, call.toJson());
    }


    void emmitCallTerminated(PjSipCall call, OnCallStateParam prm) {
        final int callId = call.getId();

        job(() -> {
            // Release wake lock
            if (mCalls.size() == 1) {
                if (mIncallWakeLock != null && mIncallWakeLock.isHeld()) {
                    mIncallWakeLock.release();
                }
            }

            // Reset audio settings
            if (mCalls.size() == 1) {
                mWifiLock.release();
                mAudioHelper.callClosed();
                removeFromForeground();

            }
        });

        this.fireEvent(PjEventType.EVENT_CALL_TERMINATED, call.toJson());
        evict(call);
    }

    void emmitCallUpdated(PjSipCall call) {
        this.fireEvent(PjEventType.EVENT_CALL_CHANGED, call.toJson());
    }

    /**
     * Pauses active calls once user answer to incoming calls.
     */
    public void doPauseParallelCalls(PjSipCall activeCall) {
        for (PjSipCall call : mCalls) {
            if (activeCall.getId() == call.getId()) {
                continue;
            }

            try {
                call.hold();
            } catch (Exception e) {
                Log.w(TAG, "Failed to put call on hold", e);
            }
        }
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

    public void addCall(PjSipCall call) {
        mCalls.add(call);
    }

    public Endpoint getEndpoint() {
        return mEndpoint;
    }

    public List<PjSipAccount> getAccounts() {
        return mAccounts;
    }

    public List<PjSipCall> getCalls() {
        return mCalls;
    }


    protected class PhoneStateChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String extraState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

            if (TelephonyManager.EXTRA_STATE_RINGING.equals(extraState) || TelephonyManager.EXTRA_STATE_OFFHOOK.equals(extraState)) {
                Log.d(TAG, "GSM call received, pause all SIP calls and do not accept incoming SIP calls.");

                mGSMIdle = false;

                job(new Runnable() {
                    @Override
                    public void run() {
                        // TODO: Do not put calls on hold, when receiving GSM call...
                        // doPauseAllCalls();
                    }
                });
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(extraState)) {
                Log.d(TAG, "GSM call released, allow to accept incoming calls.");
                mGSMIdle = true;
            }
        }
    }

    public void addTrash(Object trash) {
        mTrash.add(trash);
    }
}
