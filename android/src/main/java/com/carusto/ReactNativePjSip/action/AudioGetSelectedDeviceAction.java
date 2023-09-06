package com.carusto.ReactNativePjSip.action;

import android.content.Context;
import android.content.Intent;
import android.media.AudioDeviceInfo;

import com.carusto.ReactNativePjSip.PjActionType;
import com.carusto.ReactNativePjSip.PjSipService;
import com.carusto.ReactNativePjSip.utils.AudioDeviceUtils;

public class AudioGetSelectedDeviceAction extends PjSipReactAction implements PjSipActionIntentHandler {

    private static final String TAG = "AudioGetSelectedDeviceAction";

    public static Intent createIntent(int callbackId, Context context) {
       return createCallbackIntent(PjActionType.ACTION_AUDIO_GET_SELECTED_DEVICE, callbackId, context);
    }

    @Override
    public void handle(PjSipService service, Intent intent) {
        try {
            AudioDeviceInfo device = service.getAudioHelper().getSelectedDevice();
            if (device == null) {
                throw new NullPointerException("No selected device. Call might not be active");
            }
            sendEventHandled(service, intent, AudioDeviceUtils.toJson(device).toString());
        } catch (Exception e) {
            super.sendEventException(service, intent, e);
        }

    }

}
