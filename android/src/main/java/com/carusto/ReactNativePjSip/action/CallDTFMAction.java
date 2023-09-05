package com.carusto.ReactNativePjSip.action;

import android.content.Context;
import android.content.Intent;
import com.carusto.ReactNativePjSip.PjActionType;
import com.carusto.ReactNativePjSip.PjSipCall;
import com.carusto.ReactNativePjSip.PjSipService;

public class CallDTFMAction extends AbstractCallAction implements PjSipActionIntentHandler {

    private static final String TAG = "CallDTFMAction";
    private static final String KEY_DIGITS = "destination";

    public static Intent createIntent(int callbackId, int callId, String digits, Context context) {
        Intent intent = createCallIntent(PjActionType.ACTION_DTMF_CALL, callbackId, callId, context);
        intent.putExtra(KEY_DIGITS, digits);
        return intent;
    }

    public void handleCall(PjSipService service, PjSipCall call, Intent intent) throws Exception {
        String digits = intent.getStringExtra(KEY_DIGITS);
        call.dialDtmf(digits);
    }

}
