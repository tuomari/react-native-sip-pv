package com.carusto.ReactNativePjSip.action;

import android.content.Context;
import android.content.Intent;
import android.media.AudioDeviceInfo;

import com.carusto.ReactNativePjSip.PjActionType;
import com.carusto.ReactNativePjSip.PjSipCall;
import com.carusto.ReactNativePjSip.PjSipService;
import com.carusto.ReactNativePjSip.utils.AudioDeviceUtils;

public class AudioSelectDeviceAction extends PjSipReactAction implements PjSipActionIntentHandler {

    private static final String TAG = "AudioSelectDeviceAction";
    private static final String KEY_DEVICE_ID = "device_id";

    public static Intent createIntent(int callbackId, String deviceId, Context context) {
        Intent intent = createCallbackIntent(PjActionType.ACTION_AUDIO_SELECT_DEVICE, callbackId, context);
        intent.putExtra(KEY_DEVICE_ID, deviceId);
        return intent;
    }

    @Override
    public void handle(PjSipService service, Intent intent) {
        try {
            String deviceID = intent.getStringExtra(KEY_DEVICE_ID);
            int id = Integer.parseInt(deviceID);
            AudioDeviceInfo device = service.getAudioHelper().selectAudioDevice(id);
            if (device == null) {
                throw new NullPointerException("Device with id " + deviceID + " was not found for selection");
            }
            sendEventHandled(service, intent, AudioDeviceUtils.toJson(device).toString());
        } catch (Exception e) {
            super.sendEventException(service, intent, e);
        }

    }

}
