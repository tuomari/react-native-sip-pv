package com.carusto.ReactNativePjSip.action;

import android.content.Intent;
import com.carusto.ReactNativePjSip.PjActionType;
import com.carusto.ReactNativePjSip.PjSipCall;
import com.carusto.ReactNativePjSip.PjSipService;
import com.facebook.react.bridge.ReactApplicationContext;
import org.pjsip.pjsua2.CallOpParam;

public class CallHangupAction extends AbstractCallAction implements PjSipActionIntentHandler {

    private static final String TAG = "CallHangupAction";

    public static Intent createIntent(int callbackId, int callId, ReactApplicationContext context) {
        return createCallIntent(PjActionType.ACTION_HANGUP_CALL, callbackId, callId, context);
    }

    @Override
    public void handleCall(PjSipService service, PjSipCall call, Intent intent) throws Exception {
        call.hangup(new CallOpParam(true));
    }

}
