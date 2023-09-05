package com.carusto.ReactNativePjSip.action;

import android.content.Context;
import android.content.Intent;
import com.carusto.ReactNativePjSip.PjActionType;
import com.carusto.ReactNativePjSip.PjSipCall;
import com.carusto.ReactNativePjSip.PjSipService;
import org.pjsip.pjsua2.CallOpParam;

public class CallXferAction extends AbstractCallAction implements PjSipActionIntentHandler {

    private static final String TAG = "CallXferAction";

    private static final String KEY_DESTINATION = "destination";

    public static Intent createIntent(int callbackId, int callId, String destination, Context context) {
        Intent intent = createCallIntent(PjActionType.ACTION_XFER_CALL, callbackId, callId, context);
        intent.putExtra(KEY_DESTINATION, destination);
        return intent;
    }

    public void handleCall(PjSipService service, PjSipCall call, Intent intent) throws Exception {
        String destination = intent.getStringExtra(KEY_DESTINATION);
        call.xfer(destination, new CallOpParam(true));
    }

}
