package com.carusto.ReactNativePjSip.action;

import android.content.Context;
import android.content.Intent;
import com.carusto.ReactNativePjSip.PjSipCall;
import com.carusto.ReactNativePjSip.PjSipService;

import static com.carusto.ReactNativePjSip.PjActionType.ACTION_UNHOLD_CALL;

public class CallUholdAction extends AbstractCallAction implements PjSipActionIntentHandler {

    private static final String TAG = "CallUholdAction";

    public static Intent createIntent(int callbackId, int callId, Context context) {
        return createCallIntent(ACTION_UNHOLD_CALL, callbackId, callId, context);
    }

    public void handleCall(PjSipService service, PjSipCall call, Intent intent) throws Exception {
        call.unhold();

        // Automatically put other calls on hold.
        service.doPauseParallelCalls(call);
    }

}
