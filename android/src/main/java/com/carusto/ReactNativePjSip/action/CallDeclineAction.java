package com.carusto.ReactNativePjSip.action;

import android.content.Context;
import android.content.Intent;
import com.carusto.ReactNativePjSip.PjSipCall;
import com.carusto.ReactNativePjSip.PjSipService;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.pjsip_status_code;

import static com.carusto.ReactNativePjSip.PjActionType.ACTION_DECLINE_CALL;

public class CallDeclineAction extends AbstractCallAction implements PjSipActionIntentHandler {

    private static final String TAG = "CallDeclineAction";

    public static Intent createIntent(int callbackId, int callId, Context context) {
        return createCallIntent(ACTION_DECLINE_CALL, callbackId, callId, context);
    }

    public void handleCall(PjSipService service, PjSipCall call, Intent intent) throws Exception {
        CallOpParam prm = new CallOpParam(true);
        prm.setStatusCode(pjsip_status_code.PJSIP_SC_DECLINE);
        call.hangup(prm);
        prm.delete();
    }

}
