package com.carusto.ReactNativePjSip.action;

import android.content.Context;
import android.content.Intent;
import android.media.AudioDeviceInfo;
import android.util.Log;

import com.carusto.ReactNativePjSip.PjActionType;
import com.carusto.ReactNativePjSip.PjSipCall;
import com.carusto.ReactNativePjSip.PjSipService;
import com.carusto.ReactNativePjSip.utils.AudioDeviceUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public class AudioGetDevicesAction extends PjSipReactAction implements PjSipActionIntentHandler {

    private static final String TAG = "AudioGetDevicesAction";

    public static Intent createIntent(int callbackId, Context context) {
        return createCallbackIntent(PjActionType.ACTION_AUDIO_GET_DEVICES, callbackId, context);
    }

    @Override
    public void handle(PjSipService service, Intent intent) {
        try {
            JSONArray devices = new JSONArray();
            for (AudioDeviceInfo device : service.getAudioHelper().getCommunicationDevices()) {
                devices.put(AudioDeviceUtils.toJson(device));
            }
            String deviceStr = devices.toString();
            Log.d(TAG, "Returning audio devices: " + deviceStr);
            sendEventHandled(service, intent, deviceStr);

        } catch (Exception e) {
            sendEventException(service, intent, e);
        }
    }

}
