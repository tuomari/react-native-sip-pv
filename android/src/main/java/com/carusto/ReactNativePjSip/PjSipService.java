package com.carusto.ReactNativePjSip;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.Process;
import android.telecom.ConnectionService;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.carusto.ReactNativePjSip.dto.ServiceConfigurationDTO;
import com.carusto.ReactNativePjSip.service.BluetoothReceiver;
import com.carusto.ReactNativePjSip.service.PjSipCallStateHelper;
import com.carusto.ReactNativePjSip.service.PjSipCallStateListener;
import com.carusto.ReactNativePjSip.service.PjSipForegroundHelper;
import com.carusto.ReactNativePjSip.service.PjSipServiceAudiohelper;
import com.carusto.ReactNativePjSip.service.PjSipTelephonyHelper;
import com.carusto.ReactNativePjSip.utils.ArgumentUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.pjsip.pjsua2.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class PjSipService extends ConnectionService {

    private static String TAG = "PjSipService";
    private boolean mInitialized;

    private HandlerThread mWorkerThread;

    private Handler mHandler;

    private Endpoint mEndpoint;


    private final Map<String, Integer> transportIds = new HashMap<>();

    private ServiceConfigurationDTO mServiceConfiguration = new ServiceConfigurationDTO();

    private PjSipLogWriter mLogWriter;

    private List<PjSipAccount> mAccounts = new ArrayList<>();


    // In order to ensure that GC will not destroy objects that are used in PJSIP
    // Also there is limitation of pjsip that thread should be registered first before working with library
    // (but we couldn't register GC thread in pjsip)
    private List<Object> mTrash = new LinkedList<>();



    private TelephonyManager mTelephonyManager;

    private WifiManager mWifiManager;



    private PjSipServiceAudiohelper mAudioHelper;
    private BluetoothReceiver mBluetoothReceiver;
    private PjSipTelephonyHelper mTelephonyHelper;
    private PjSipForegroundHelper mForegroundHelper;
    private final List<AutoCloseable> cleanupObjects = new ArrayList<>();
    private PjSipCallStateHelper mCallStateHelper;


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
            addToCleanup(() -> mEndpoint.libDestroy());

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


    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId)
        Log.w(TAG, "StartCmd for pjsip");
        if (!mInitialized) {
            if (intent != null && intent.hasExtra("service")) {
                mServiceConfiguration = ServiceConfigurationDTO.fromMap((Map) intent.getSerializableExtra("service"));
            }

            mWorkerThread = new HandlerThread(getClass().getSimpleName(), Process.THREAD_PRIORITY_FOREGROUND);
            addToCleanup(() -> mWorkerThread.quitSafely());
            mWorkerThread.setPriority(Thread.MAX_PRIORITY);
            mWorkerThread.start();

            mHandler = new Handler(mWorkerThread.getLooper());
            mAudioHelper = addToCleanup(new PjSipServiceAudiohelper(this, (AudioManager) getApplicationContext().getSystemService(AUDIO_SERVICE)));


            mTelephonyHelper = addToCleanup(new PjSipTelephonyHelper(this));
            /* Handling of GSM stuff should be in TelephonyHelper
            IntentFilter phoneStateFilter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
            registerReceiver(mPhoneStateChangedReceiver, phoneStateFilter);
             */
            mBluetoothReceiver = addToCleanup(new BluetoothReceiver(this));
            mForegroundHelper = addToCleanup(new PjSipForegroundHelper(this));
            mCallStateHelper = addToCleanup(new PjSipCallStateHelper(this));
            mInitialized = true;
            Log.w(TAG, "Start command starting load job");

            job(() -> load());
        }

        if (intent != null) {
            job(() -> handle(intent));
        }

        return START_NOT_STICKY;
    }

    public PjSipCallStateHelper getCallStateHelper() {
        return mCallStateHelper;
    }


    @FunctionalInterface
    private interface Closeuppable extends AutoCloseable {
        @Override
        void close() throws Exception;
    }

    private void addToCleanup(Closeuppable consumer) {
        cleanupObjects.add(consumer);
    }

    private <T extends AutoCloseable> T addToCleanup(T cleanupObject) {
        cleanupObjects.add(cleanupObject);
        return cleanupObject;
    }

    @Override
    public void onDestroy() {
        while (!cleanupObjects.isEmpty()) {
            try (AutoCloseable ignored = cleanupObjects.remove(0)) {
            } catch (Exception e) {
                Log.w(TAG, "Error closing resource", e);
            }
        }
        super.onDestroy();
    }

    public void job(Runnable job) {
        mHandler.post(job);
    }

    public synchronized AudDevManager getAudDevManager() {
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
        mCallStateHelper.evict(call);
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


    public void addAccount(PjSipAccount account) {
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




    public void fireEvent(PjEventType type, JSONObject data) {
        fireEvent(type, data.toString());
    }

    public void fireEvent(PjEventType type, String data) {
        Intent intent = new Intent();
        intent.setAction(type.eventName);
        intent.putExtra("data", data);
        service.sendBroadcast(intent);
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




    public Endpoint getEndpoint() {
        return mEndpoint;
    }

    public List<PjSipAccount> getAccounts() {
        return mAccounts;
    }

    public List<PjSipCall> getCalls() {
        return mCallStateHelper.getCalls();
    }

    public PjSipServiceAudiohelper getAudioHelper() {
        return mAudioHelper;
    }

    // TODO: How do we handle GSM Stuff. Should be in Telephony helper anyway
    /*
    private BroadcastReceiver mPhoneStateChangedReceiver = new PhoneStateChangedReceiver();

    protected class PhoneStateChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String extraState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

            if (TelephonyManager.EXTRA_STATE_RINGING.equals(extraState) || TelephonyManager.EXTRA_STATE_OFFHOOK.equals(extraState)) {
                Log.d(TAG, "GSM call received, pause all SIP calls and do not accept incoming SIP calls.");

                //mGSMIdle = false;

                job(new Runnable() {
                    @Override
                    public void run() {
                        // TODO: Do not put calls on hold, when receiving GSM call...
                        // doPauseAllCalls();
                    }
                });
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(extraState)) {
                Log.d(TAG, "GSM call released, allow to accept incoming calls.");
                //mGSMIdle = true;
            }
        }
    }
*/
    public void addTrash(Object trash) {
        mTrash.add(trash);
    }

    public PjSipForegroundHelper getForegroundHelper() {
        return mForegroundHelper;
    }
}
