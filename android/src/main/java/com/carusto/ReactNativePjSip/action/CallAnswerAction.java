package com.carusto.ReactNativePjSip.action;

import android.content.Context;
import android.content.Intent;
import com.carusto.ReactNativePjSip.PjSipCall;
import com.carusto.ReactNativePjSip.PjSipService;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.pjsip_status_code;

import static com.carusto.ReactNativePjSip.PjActionType.ACTION_ANSWER_CALL;

public class CallAnswerAction extends AbstractCallAction implements PjSipActionIntentHandler {

    private static final String TAG = "CallAnswerAction";

    public static Intent createIntent(int callbackId, int callId, Context context) {
        return createCallIntent(ACTION_ANSWER_CALL, callbackId, callId, context);
    }

    public void handleCall(PjSipService service, PjSipCall call, Intent intent) throws Exception {
        CallOpParam prm = new CallOpParam();
        prm.setStatusCode(pjsip_status_code.PJSIP_SC_OK);
        call.answer(prm);

        // Automatically put other calls on hold.
        service.doPauseParallelCalls(call);
    }

}
