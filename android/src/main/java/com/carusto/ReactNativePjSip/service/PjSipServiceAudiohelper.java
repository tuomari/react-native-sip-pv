package com.carusto.ReactNativePjSip.service;

import android.bluetooth.BluetoothHeadset;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import com.carusto.ReactNativePjSip.PjEventType;
import com.carusto.ReactNativePjSip.PjSipService;
import com.carusto.ReactNativePjSip.utils.AudioDeviceUtils;

import org.json.JSONException;

import java.util.*;


public class PjSipServiceAudiohelper implements AutoCloseable {

    private static final String TAG = "PjSipServiceAudiohelper";
    private final AudioManager mAudioManager;
    private final PjSipService service;

    private Integer preferredAudioDeviceId = null;

    private final AudioDeviceCallback newAudioDeviceCallback = new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            try {
                service.fireEvent(PjEventType.EVENT_AUDIODEVICES_ADDED, AudioDeviceUtils.toJson(addedDevices).toString());
            } catch (JSONException e) {
                Log.w(TAG, "Error creating json from added devices", e);
            }

        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            try {
                service.fireEvent(PjEventType.EVENT_AUDIODEVICES_REMOVED, AudioDeviceUtils.toJson(removedDevices).toString());
            } catch (JSONException e) {

                Log.w(TAG, "Error creating json from removed devices", e);
            }
        }
    };

    private AudioFocusRequest audioFocus = null;

    public PjSipServiceAudiohelper(PjSipService service, AudioManager audioManager) {
        this.service = service;
        this.mAudioManager = audioManager;


    }


    public AudioDeviceInfo selectAudioDevice(int deviceId) {
        // TODO: mAudioManager is supported only in API-version 31 and later.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return null;
        }

        boolean callOpen = mAudioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION && !service.getCalls().isEmpty();

        if (deviceId > 0) {
            this.preferredAudioDeviceId = deviceId;
            for (AudioDeviceInfo device : mAudioManager.getAvailableCommunicationDevices()) {
                if (deviceId == device.getId()) {
                    if (callOpen) {
                        mAudioManager.setCommunicationDevice(device);
                    }
                    return device;
                }
            }
            throw new NullPointerException("Audio device with id " + deviceId + " was not found");

        } else {
            this.preferredAudioDeviceId = null;
            if (callOpen) {
                mAudioManager.clearCommunicationDevice();
            }
        }
        return null;
    }

    public void callStarted() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).build();
            mAudioManager.requestAudioFocus(audioFocus);
        }
        // TODO: mAudioManager is supported only in API-version 31 and later.
        // Implement also in the old way...
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Optional<AudioDeviceInfo> selectedDevice = Optional.empty();
            // If there is a preferred device selected, use it
            Log.d(TAG, "Trying to select preferred device: " + preferredAudioDeviceId);
            if (preferredAudioDeviceId != null) {
                selectedDevice = mAudioManager.getAvailableCommunicationDevices().stream().filter(t -> t.getId() == preferredAudioDeviceId).findAny();
            }
            // Else try to use the bluetooth device with the biggest ID ( ie. added latest )
            if (!selectedDevice.isPresent()) {
                Log.d(TAG, "No preferred device found. Trying to select a bluetooth device");
                selectedDevice = mAudioManager.getAvailableCommunicationDevices().stream().filter(t -> t.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO).max(Comparator.comparingInt(AudioDeviceInfo::getId));
            }

            selectedDevice.ifPresent(mAudioManager::setCommunicationDevice);

        }

    }

    public void callClosed() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mAudioManager.clearCommunicationDevice();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocus != null) {
            mAudioManager.abandonAudioFocusRequest(audioFocus);

        }
    }

    @Override
    public void close() {
        mAudioManager.unregisterAudioDeviceCallback(newAudioDeviceCallback);
    }


    public List<AudioDeviceInfo> getCommunicationDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return mAudioManager.getAvailableCommunicationDevices();
        }
        return Collections.emptyList();
    }

    public AudioDeviceInfo getSelectedDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return mAudioManager.getCommunicationDevice();
        }
        return null;
    }

    public AudioManager getAudioManager() {
        return mAudioManager;
    }
}
