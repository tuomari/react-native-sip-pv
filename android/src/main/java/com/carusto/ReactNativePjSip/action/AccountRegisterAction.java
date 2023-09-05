package com.carusto.ReactNativePjSip.action;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.carusto.ReactNativePjSip.PjActionType;
import com.carusto.ReactNativePjSip.PjSipAccount;
import com.carusto.ReactNativePjSip.PjSipService;
import com.carusto.ReactNativePjSip.dto.AccountConfigurationDTO;
import com.facebook.react.bridge.ReadableMap;
import org.jetbrains.annotations.NotNull;
import org.pjsip.pjsua2.*;

import java.util.Map;

public class AccountRegisterAction extends PjSipReactAction implements PjSipActionIntentHandler {

    private static final String TAG = "AccountRegisterAction";

    private static final String KEY_ACCOUNT_ID = "account_id";
    private static final String KEY_RENEW = "renew";

    public static Intent createIntent(int callbackId, int accountId, boolean renew, Context context) {
        Intent intent = new Intent(context, PjSipService.class);
        intent.setAction(PjActionType.ACTION_REGISTER_ACCOUNT.actionName);
        intent.putExtra(CALLBACK_EXTRA_KEY, callbackId);
        intent.putExtra(KEY_ACCOUNT_ID, accountId);
        intent.putExtra(KEY_RENEW, renew);
        return intent;
    }

    @Override
    public void handle(PjSipService service, Intent intent) {
        try {
            int accountId = intent.getIntExtra(KEY_ACCOUNT_ID, -1);
            boolean renew = intent.getBooleanExtra(KEY_RENEW, false);
            PjSipAccount account = null;

            for (PjSipAccount a : service.getAccounts()) {
                if (a.getId() == accountId) {
                    account = a;
                    break;
                }
            }

            if (account == null) {
                throw new Exception("Account with \"" + accountId + "\" id not found");
            }

            account.register(renew);


            super.sendEventHandled(service, intent);
        } catch (Exception e) {
            super.sendEventException(service, intent, e);
        }
    }


}
