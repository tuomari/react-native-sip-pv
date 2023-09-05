package com.carusto.ReactNativePjSip.action;

import android.content.Context;
import android.content.Intent;
import com.carusto.ReactNativePjSip.PjActionType;
import com.carusto.ReactNativePjSip.PjSipAccount;
import com.carusto.ReactNativePjSip.PjSipService;

public class AccountDeleteAction extends PjSipReactAction implements PjSipActionIntentHandler {

    private static final String TAG = "AccountDeleteAction";

    private static final String KEY_ACCOUNT_ID = "account_id";
    public static Intent createIntent(int callbackId, int accountId, Context context) {
        Intent intent = new Intent(context, PjSipService.class);
        intent.setAction(PjActionType.ACTION_DELETE_ACCOUNT.actionName);
        intent.putExtra(CALLBACK_EXTRA_KEY, callbackId);
        intent.putExtra(KEY_ACCOUNT_ID, accountId);
        return intent;
    }

    @Override
    public void handle(PjSipService service, Intent intent) {
        try {
            int accountId = intent.getIntExtra(KEY_ACCOUNT_ID, -1);
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
            service.evict(account);

            sendEventHandled(service, intent);
        } catch (Exception e) {
            sendEventException(service, intent, e);
        }
    }


}
