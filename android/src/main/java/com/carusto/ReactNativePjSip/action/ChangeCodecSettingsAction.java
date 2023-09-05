package com.carusto.ReactNativePjSip.action;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.carusto.ReactNativePjSip.PjActionType;
import com.carusto.ReactNativePjSip.PjSipAccount;
import com.carusto.ReactNativePjSip.PjSipService;
import com.facebook.react.bridge.ReadableMap;

public class ChangeCodecSettingsAction extends PjSipReactAction implements PjSipActionIntentHandler {

    private static final String TAG = "ChangeCodecSettingsAction";

    private static final String KEY_ACCOUNT_ID = "account_id";
    public static Intent createIntent(int callbackId, ReadableMap codecSettings, Context context) {
        Intent intent = new Intent(context, PjSipService.class);
        intent.setAction(PjActionType.ACTION_CHANGE_CODEC_SETTINGS.actionName);
        intent.putExtra(CALLBACK_EXTRA_KEY, callbackId);

        formatIntent(intent, codecSettings);

        return intent;
    }

    @Override
    public void handle(PjSipService service, Intent intent) {
        try {
            Bundle codecSettings = intent.getExtras();

            // -----
            if (codecSettings != null) {
                for (String key : codecSettings.keySet()) {
                    if (!key.equals(CALLBACK_EXTRA_KEY)) {
                        short priority = (short) codecSettings.getInt(key);
                        service.getEndpoint().codecSetPriority(key, priority);
                    }

                }
            }

            sendEventHandled(service, intent);
        } catch (Exception e) {
            sendEventException(service, intent, e);
        }
    }


}
