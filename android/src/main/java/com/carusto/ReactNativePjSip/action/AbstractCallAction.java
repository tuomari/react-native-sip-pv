package com.carusto.ReactNativePjSip.action;

import android.content.Context;
import android.content.Intent;
import com.carusto.ReactNativePjSip.PjActionType;
import com.carusto.ReactNativePjSip.PjSipCall;
import com.carusto.ReactNativePjSip.PjSipService;

public abstract class AbstractCallAction extends PjSipReactAction implements PjSipActionIntentHandler {
    protected static final String CALL_EXTRA_KEY = "call_id";

    public static Intent createCallIntent(PjActionType type, int callbackId, int callId, Context context) {
        Intent intent = new Intent(context, PjSipService.class);
        intent.setAction(type.actionName);
        intent.putExtra(CALLBACK_EXTRA_KEY, callbackId);
        intent.putExtra(CALL_EXTRA_KEY, callId);

        return intent;
    }

    protected abstract void handleCall(PjSipService service, PjSipCall call, Intent intent) throws Exception;

    @Override
    public final void handle(PjSipService service, Intent intent) {
        try {
            int callId = intent.getIntExtra(CALL_EXTRA_KEY, -1);
            PjSipCall call = service.findCall(callId);
            this.handleCall(service, call, intent);
            sendEventHandled(service, intent);
        } catch (Exception e) {
            sendEventException(service, intent, e);
        }
    }

}
