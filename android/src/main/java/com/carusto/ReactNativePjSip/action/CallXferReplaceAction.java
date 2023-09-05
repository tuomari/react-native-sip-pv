package com.carusto.ReactNativePjSip.action;

import android.content.Context;
import android.content.Intent;
import com.carusto.ReactNativePjSip.PjActionType;
import com.carusto.ReactNativePjSip.PjSipCall;
import com.carusto.ReactNativePjSip.PjSipService;
import org.pjsip.pjsua2.CallOpParam;

public class CallXferReplaceAction extends AbstractCallAction implements PjSipActionIntentHandler {

    private static final String TAG = "CallXferReplaceAction";
    private static final String KEY_DEST_CALL_ID = "dest_call_id";

    public static Intent createIntent(int callbackId, int callId, int destCallId, Context context) {
        Intent intent = createCallIntent(PjActionType.ACTION_XFER_REPLACES_CALL, callbackId, callId, context);
        intent.putExtra(KEY_DEST_CALL_ID, destCallId);
        return intent;
    }

    public void handleCall(PjSipService service, PjSipCall call, Intent intent) throws Exception {
        int destinationCallId = intent.getIntExtra(KEY_DEST_CALL_ID, -1);
        PjSipCall destinationCall = service.findCall(destinationCallId);

        call.xferReplaces(destinationCall, new CallOpParam(true));
    }

}
