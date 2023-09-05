package com.carusto.ReactNativePjSip.action;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.carusto.ReactNativePjSip.PjActionType;
import com.carusto.ReactNativePjSip.PjSipAccount;
import com.carusto.ReactNativePjSip.PjSipCall;
import com.carusto.ReactNativePjSip.PjSipService;
import com.carusto.ReactNativePjSip.dto.ServiceConfigurationDTO;
import com.facebook.react.bridge.ReadableMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.pjsip.pjsua2.CodecInfo;
import org.pjsip.pjsua2.CodecInfoVector2;

import java.util.Map;

public class SetServiceConfigAction extends PjSipReactAction implements PjSipActionIntentHandler {

    private static final String TAG = "StartPjsipAction";

    public static Intent createIntent(int callbackId, ReadableMap configuration, Context context) {
        Intent intent = new Intent(context, PjSipService.class);
        intent.setAction(PjActionType.ACTION_SET_SERVICE_CONFIGURATION.actionName);
        intent.putExtra(CALLBACK_EXTRA_KEY, callbackId);

        formatIntent(intent, configuration);

        return intent;
    }

    @Override
    public void handle(PjSipService service, Intent intent) {
        try {
            ServiceConfigurationDTO config = ServiceConfigurationDTO.fromIntent(intent);
            service.updateServiceConfiguration(config);

            sendEventHandledJson(service, intent, config.toJson());
            // Emmit response
        } catch (Exception e) {
            sendEventException(service, intent, e);
        }
    }

}
