package com.carusto.ReactNativePjSip.action;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.carusto.ReactNativePjSip.*;
import com.carusto.ReactNativePjSip.dto.CallSettingsDTO;
import com.carusto.ReactNativePjSip.dto.SipMessageDTO;
import com.facebook.react.bridge.ReadableMap;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.CallSetting;
import org.pjsip.pjsua2.SipTxOption;


public class MakeCallAction extends PjSipReactAction implements PjSipActionIntentHandler {


    private static final String TAG = "MakeCallAction";

    public static Intent createIntent(int callbackId, int accountId, String destination, ReadableMap settings, ReadableMap message, Context context) {
        Intent intent = new Intent(context, PjSipService.class);
        intent.setAction(PjActionType.ACTION_MAKE_CALL.actionName);
        intent.putExtra(CALLBACK_EXTRA_KEY, callbackId);
        intent.putExtra("account_id", accountId);
        intent.putExtra("destination", destination);

        if (settings != null) {
            intent.putExtra("settings", CallSettingsDTO.fromReadableMap(settings).toJson());
        }

        if (message != null) {
            intent.putExtra("message", SipMessageDTO.fromReadableMap(message).toJson());
        }

        return intent;
    }

    @Override
    public void handle(PjSipService service, Intent intent) {
        try {
            int accountId = intent.getIntExtra("account_id", -1);
            PjSipAccount account = service.findAccount(accountId);
            String destination = intent.getStringExtra("destination");
            String settingsJson = intent.getStringExtra("settings");
            String messageJson = intent.getStringExtra("message");

            service.getForegroundHelper().putToForeground(destination);

            // -----
            CallOpParam callOpParam = new CallOpParam(true);

            if (settingsJson != null) {
                CallSettingsDTO settingsDTO = CallSettingsDTO.fromJson(settingsJson);
                CallSetting callSettings = new CallSetting();

                if (settingsDTO.getAudioCount() != null) {
                    callSettings.setAudioCount(settingsDTO.getAudioCount());
                }
                if (settingsDTO.getVideoCount() != null) {
                    callSettings.setVideoCount(settingsDTO.getVideoCount());
                }
                if (settingsDTO.getFlag() != null) {
                    callSettings.setFlag(settingsDTO.getFlag());
                }
                if (settingsDTO.getRequestKeyframeMethod() != null) {
                    callSettings.setReqKeyframeMethod(settingsDTO.getRequestKeyframeMethod());
                }

                callOpParam.setOpt(callSettings);

                service.addTrash(callSettings);
            }

            if (messageJson != null) {
                SipMessageDTO messageDTO = SipMessageDTO.fromJson(messageJson);
                SipTxOption callTxOption = new SipTxOption();

                if (messageDTO.getTargetUri() != null) {
                    callTxOption.setTargetUri(messageDTO.getTargetUri());
                }
                if (messageDTO.getContentType() != null) {
                    callTxOption.setContentType(messageDTO.getContentType());
                }
                if (messageDTO.getHeaders() != null) {
                    callTxOption.setHeaders(PjSipUtils.mapToSipHeaderVector(messageDTO.getHeaders()));
                }
                if (messageDTO.getBody() != null) {
                    callTxOption.setMsgBody(messageDTO.getBody());
                }

                callOpParam.setTxOption(callTxOption);

                service.addTrash(callTxOption);
            }

            PjSipCall call = new PjSipCall(account);
            call.makeCall(destination, callOpParam);
            callOpParam.delete();

            // Automatically put other calls on hold.
            //service.doPauseParallelCalls(call);

            service.getCallStateHelper().addCall(call);
            Log.w(TAG, "Created call" + call.toJsonString());

            sendEventHandledJson(service, intent, call.toJson());

        } catch (Exception e) {
            sendEventException(service, intent, e);
        }
    }


}
