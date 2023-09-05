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

public class StartPjsipAction extends PjSipReactAction implements PjSipActionIntentHandler {

    private static final String TAG = "StartPjsipAction";

    public static Intent createIntent(int callbackId, ReadableMap configuration, Context context) {
        Intent intent = new Intent(context, PjSipService.class);
        intent.setAction(PjActionType.ACTION_START.actionName);
        intent.putExtra(CALLBACK_EXTRA_KEY, callbackId);
        formatIntent(intent, configuration);
        return intent;
    }

    public void handle(PjSipService service, Intent intent) {

        try {
            // Modify existing configuration if it changes during application reload.
            if (intent.hasExtra("service")) {
                ServiceConfigurationDTO newServiceConfiguration = ServiceConfigurationDTO.fromMap((Map) intent.getSerializableExtra("service"));
                if (!newServiceConfiguration.equals(service.getServiceConfiguration())) {
                    service.updateServiceConfiguration(newServiceConfiguration);
                }
            }

            CodecInfoVector2 codVect = service.getEndpoint().codecEnum2();
            JSONObject codecs = new JSONObject();

            for (CodecInfo codInfo : codVect) {
                String codId = codInfo.getCodecId();
                short priority = codInfo.getPriority();
                codecs.put(codId, priority);
                codInfo.delete();
            }

            JSONObject settings = service.getServiceConfiguration().toJson();
            settings.put("codecs", codecs);

            this.sendResponse(service, intent, settings);
        } catch (Exception error) {
            sendEventException(service, intent, error);

            Log.e(TAG, "Error while building codecs list", error);
            throw new RuntimeException(error);
        }
    }

    private void sendResponse(PjSipService service, Intent original, JSONObject settings) {
        try {
            JSONArray dataAccounts = new JSONArray();
            for (PjSipAccount account : service.getAccounts()) {
                dataAccounts.put(account.toJson());
            }

            JSONArray dataCalls = new JSONArray();
            for (PjSipCall call : service.getCalls()) {
                dataCalls.put(call.toJson());
            }

            JSONObject data = new JSONObject();
            data.put("accounts", dataAccounts);
            data.put("calls", dataCalls);
            data.put("settings", settings);

            sendEventHandledJson(service, original, data);

        } catch (Exception e) {
            Log.e(TAG, "Failed to send ACTION_START event", e);
            sendEventException(service, original, e);
        }
    }

}
