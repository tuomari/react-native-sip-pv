package com.carusto.ReactNativePjSip.service;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAssignedNumbers;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.carusto.ReactNativePjSip.PjSipService;

import java.util.List;

public class BluetoothReceiver extends BroadcastReceive implements AutoCloseable {
    private static final String TAG = "BluetoothReceiver";
    private final AudioManager mAudioManager;
    private final PjSipService mService;
    private final BluetoothReceiver mReceiver;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mIsBluetoothHeadsetConnected = false;
    private boolean mIsBluetoothHeadsetScoConnected = false;
    private BluetoothHeadset mBluetoothHeadset;


    private void startBluetooth() {

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            Log.i(TAG, "[Audio Manager] [Bluetooth] Adapter found");
            if (mAudioManager.isBluetoothScoAvailableOffCall()) {
                Log.i(TAG, "[Audio Manager] [Bluetooth] SCO available off call, continue");
            } else {
                Log.w(TAG, "[Audio Manager] [Bluetooth] SCO not available off call !");
            }
            bluetoothAdapterStateChanged();
        }
    }


    public void bluetoothAdapterStateChanged() {
        if (mBluetoothAdapter.isEnabled()) {
            Log.i(TAG, "[Audio Manager] [Bluetooth] Adapter enabled");
            mIsBluetoothHeadsetConnected = false;
            mIsBluetoothHeadsetScoConnected = false;

            BluetoothProfile.ServiceListener bluetoothServiceListener = new BluetoothProfile.ServiceListener() {
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    if (profile == BluetoothProfile.HEADSET) {
                        Log.i(TAG, "[Audio Manager] [Bluetooth] HEADSET profile connected" + proxy);
                        mBluetoothHeadset = (BluetoothHeadset) proxy;

                        List<BluetoothDevice> devices = mBluetoothHeadset.getConnectedDevices();
                        for (BluetoothDevice device : devices) {
                            Log.i(TAG, "[Bluetooth] device " + device);
                            Log.i(TAG, "[Bluetooth] device Type " + device.getType());
                            Log.i(TAG, "[Bluetooth] device Name " + device.getName());
                            Log.i(TAG, "[Bluetooth] device Alias " + device.getAlias());
                            Log.i(TAG, "[Bluetooth] device UUIDs " + device.getUuids());

                        }
                        if (devices.size() > 0) {

                            Log.i(TAG, "[Audio Manager] [Bluetooth] A device is already connected");
                        }

                        Log.i(TAG, "[Audio Manager] [Bluetooth] Registering bluetooth receiver");

                        IntentFilter filter = new IntentFilter();
                        filter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
                        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
                        filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
                        filter.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);

                        Intent sticky =
                            mService.registerReceiver(mReceiver, filter);
                        Log.i(TAG, "[Audio Manager] [Bluetooth] Bluetooth receiver registered");
                        int state =
                            sticky.getIntExtra(
                                AudioManager.EXTRA_SCO_AUDIO_STATE,
                                AudioManager.SCO_AUDIO_STATE_DISCONNECTED);
                        if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                            Log.i(TAG, "[Audio Manager] [Bluetooth] Bluetooth headset SCO connected");
                            //   bluetoothHeadetScoConnectionChanged(true);
                        } else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                            Log.i(TAG, "[Audio Manager] [Bluetooth] Bluetooth headset SCO disconnected");
                            // bluetoothHeadetScoConnectionChanged(false);
                        } else if (state == AudioManager.SCO_AUDIO_STATE_CONNECTING) {
                            Log.i(TAG, "[Audio Manager] [Bluetooth] Bluetooth headset SCO connecting");
                        } else if (state == AudioManager.SCO_AUDIO_STATE_ERROR) {
                            Log.i(TAG,
                                "[Audio Manager] [Bluetooth] Bluetooth headset SCO connection error");
                        } else {
                            Log.w(TAG, "[Audio Manager] [Bluetooth] Bluetooth headset unknown SCO state changed: " + state);
                        }
                    }
                }

                public void onServiceDisconnected(int profile) {
                    if (profile == BluetoothProfile.HEADSET) {
                        Log.i(TAG, "[Audio Manager] [Bluetooth] HEADSET profile disconnected");
                        mBluetoothHeadset = null;
                        mIsBluetoothHeadsetConnected = false;
                        mIsBluetoothHeadsetScoConnected = false;
                    }
                }
            };

            mBluetoothAdapter.getProfileProxy(mService, bluetoothServiceListener, BluetoothProfile.HEADSET);
        } else {
            Log.w(TAG, "[Audio Manager] [Bluetooth] Adapter disabled");
        }
    }

    public BluetoothReceiver(PjSipService service) {
        mReceiver = this;
        mService = service;
        mAudioManager = service.getAudioHelper().getAudioManager();

        startBluetooth();
        Log.i(TAG, "[Bluetooth] Bluetooth receiver created");
        //service.getAudioHelper().getAudioManager().registerAudioDeviceCallback(newAudioDeviceCallback, null);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);

        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
        filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED);


        filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        filter.addAction(Intent.ACTION_CALL_BUTTON);
        filter.addAction(Intent.ACTION_MEDIA_BUTTON);
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        //filter.addAction(TelephonyManager.ACTION);

        {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
            intentFilter.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY + '.' + BluetoothAssignedNumbers.BOSE);

/*
            for (int i = 0; i < 65535; i++) {
                intentFilter.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY + '.' + i);
            }
*/
            Intent eventIntent = service.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "[Bluetooth] Event receiver WTF" + intent);
                }
            }, intentFilter, Context.RECEIVER_EXPORTED);
            Log.d(TAG, "[Bluetooth] Event intent" + eventIntent);
        }

        Intent sticky =
            service.registerReceiver(this, filter, Context.RECEIVER_EXPORTED);
        Log.i(TAG, "[Audio Manager] [Bluetooth] Bluetooth receiver registered");
        int state =
            sticky.getIntExtra(
                AudioManager.EXTRA_SCO_AUDIO_STATE,
                AudioManager.SCO_AUDIO_STATE_DISCONNECTED);
        if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
            Log.i(TAG, "[Audio Manager] [Bluetooth] Bluetooth headset SCO connected");
            //    bluetoothHeadetScoConnectionChanged(true);
        } else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
            Log.i(TAG, "[Audio Manager] [Bluetooth] Bluetooth headset SCO disconnected");
            //  bluetoothHeadetScoConnectionChanged(false);
        } else if (state == AudioManager.SCO_AUDIO_STATE_CONNECTING) {
            Log.i(TAG, "[Audio Manager] [Bluetooth] Bluetooth headset SCO connecting");
        } else if (state == AudioManager.SCO_AUDIO_STATE_ERROR) {
            Log.i(TAG, "[Audio Manager] [Bluetooth] Bluetooth headset SCO connection error");
        } else {
            Log.w(TAG, "[Audio Manager] [Bluetooth] Bluetooth headset unknown SCO state changed: " + state);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "[Bluetooth] Bluetooth broadcast received Intent: " + intent);
        //Log.i(TAG, "[Bluetooth] Bluetooth broadcast received Extras: " + intent.getExtras() );
        for (String key : intent.getExtras().keySet()) {
            Log.d(TAG, "[Bluetooth] intent key: " + key + " Value " + intent.getExtras().get(key));
        }
        Log.i(TAG, "[Bluetooth] Bluetooth broadcast received Action " + action);

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Log.w(TAG, "[Bluetooth] Adapter has been turned off");
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.w(TAG, "[Bluetooth] Adapter is being turned off");
                    break;
                case BluetoothAdapter.STATE_ON:
                    Log.i(TAG, "[Bluetooth] Adapter has been turned on");
                    //LinphoneManager.getAudioManager().bluetoothAdapterStateChanged();
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.i(TAG, "[Bluetooth] Adapter is being turned on");
                    break;
                case BluetoothAdapter.ERROR:
                    Log.e(TAG, "[Bluetooth] Adapter is in error state !");
                    break;
                default:
                    Log.w(TAG, "[Bluetooth] Unknown adapter state: " + state);
                    break;
            }
        } else if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
            int state =
                intent.getIntExtra(
                    BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);
            if (state == BluetoothHeadset.STATE_CONNECTED) {
                Log.i(TAG, "[Bluetooth] Bluetooth headset connected");
                //LinphoneManager.getAudioManager().bluetoothHeadetConnectionChanged(true);
            } else if (state == BluetoothHeadset.STATE_DISCONNECTED) {
                Log.i(TAG, "[Bluetooth] Bluetooth headset disconnected");
                //LinphoneManager.getAudioManager().bluetoothHeadetConnectionChanged(false);
            } else {
                Log.w(TAG, "[Bluetooth] Bluetooth headset unknown state changed: " + state);
            }
        } else if (action.equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)) {
            int state =
                intent.getIntExtra(
                    BluetoothHeadset.EXTRA_STATE,
                    BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
            if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                Log.i(TAG, "[Bluetooth] Bluetooth headset audio connected");
                // LinphoneManager.getAudioManager().bluetoothHeadetAudioConnectionChanged(true);
            } else if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
                Log.i(TAG, "[Bluetooth] Bluetooth headset audio disconnected");
                //LinphoneManager.getAudioManager().bluetoothHeadetAudioConnectionChanged(false);
            } else if (state == BluetoothHeadset.STATE_AUDIO_CONNECTING) {
                Log.i(TAG, "[Bluetooth] Bluetooth headset audio connecting");
            } else {
                Log.w(TAG, "[Bluetooth] Bluetooth headset unknown audio state changed: " + state);
            }
        } else if (action.equals(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)) {
            int state =
                intent.getIntExtra(
                    AudioManager.EXTRA_SCO_AUDIO_STATE,
                    AudioManager.SCO_AUDIO_STATE_DISCONNECTED);
            if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                Log.i(TAG, "[Bluetooth] Bluetooth headset SCO connected");
                //LinphoneManager.getAudioManager().bluetoothHeadetScoConnectionChanged(true);
            } else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                Log.i(TAG, "[Bluetooth] Bluetooth headset SCO disconnected");
                //LinphoneManager.getAudioManager().bluetoothHeadetScoConnectionChanged(false);
            } else if (state == AudioManager.SCO_AUDIO_STATE_CONNECTING) {
                Log.i(TAG, "[Bluetooth] Bluetooth headset SCO connecting");
            } else if (state == AudioManager.SCO_AUDIO_STATE_ERROR) {
                Log.i(TAG, "[Bluetooth] Bluetooth headset SCO connection error");
            } else {
                Log.w(TAG, "[Bluetooth] Bluetooth headset unknown SCO state changed: " + state);
            }
        } else if (action.equals(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)) {
            String command =
                intent.getStringExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD);
            int type =
                intent.getIntExtra(
                    BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE, -1);

            String commandType;
            switch (type) {
                case BluetoothHeadset.AT_CMD_TYPE_ACTION:
                    commandType = "AT Action";
                    break;
                case BluetoothHeadset.AT_CMD_TYPE_READ:
                    commandType = "AT Read";
                    break;
                case BluetoothHeadset.AT_CMD_TYPE_TEST:
                    commandType = "AT Test";
                    break;
                case BluetoothHeadset.AT_CMD_TYPE_SET:
                    commandType = "AT Set";
                    break;
                case BluetoothHeadset.AT_CMD_TYPE_BASIC:
                    commandType = "AT Basic";
                    break;
                default:
                    commandType = "AT Unknown";
                    break;
            }
            Log.i(TAG, "[Bluetooth] Vendor action " + commandType + " : " + command);
        } else {
            Log.w(TAG, "[Bluetooth] Bluetooth unknown action: " + action);
        }
    }

    @Override
    public void close() throws Exception {
        // TODO: Remove listeners
    }
}
