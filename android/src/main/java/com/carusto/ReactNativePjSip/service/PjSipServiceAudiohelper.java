package com.carusto.ReactNativePjSip.service;

import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;


public class PjSipServiceAudiohelper implements AutoCloseable {

    private static final String TAG = "PjSipServiceAudiohelper";
    private final AudioManager mAudioManager;

    private final AudioDeviceCallback newAudioDeviceCallback = new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            super.onAudioDevicesAdded(addedDevices);
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            super.onAudioDevicesRemoved(removedDevices);
        }
    };

    public PjSipServiceAudiohelper(AudioManager audioManager) {
        this.mAudioManager = audioManager;

        mAudioManager.registerAudioDeviceCallback(newAudioDeviceCallback, null);
    }

    public boolean selectAudioDevice(int deviceId) {
        // TODO: mAudioManager is supported only in API-version 31 and later.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (AudioDeviceInfo device : mAudioManager.getAvailableCommunicationDevices()) {
                if (deviceId == device.getId()) {
                    mAudioManager.setCommunicationDevice(device);
                    return true;
                }
            }
        }
        return false;
    }

    public void callStarted() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        // TODO: mAudioManager is supported only in API-version 31 and later.
        // Implement also in the old way...
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            && mAudioManager.getCommunicationDevice().getType() != AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
            for (AudioDeviceInfo dev : mAudioManager.getAvailableCommunicationDevices()) {
                Log.d(TAG, "Found Bluetooth device. Using it by default");
                if (dev.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    mAudioManager.setCommunicationDevice(dev);
                    break;
                }
            }

        }

    }

    public void callClosed() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mAudioManager.clearCommunicationDevice();
        }
    }

    @Override
    public void close() {
        mAudioManager.unregisterAudioDeviceCallback(newAudioDeviceCallback);
    }


}
