package com.carusto.ReactNativePjSip.service;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.carusto.ReactNativePjSip.PjSipService;

public class PjSipTelephonyHelper extends AutoCloseable {
    private final PjSipService service;
    private final TelephonyManager mTelephonyManager;

    public PjSipTelephonyHelper(PjSipService service) {
        this.service = service;
        mTelephonyManager = (TelephonyManager) service.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        //mGSMIdle = mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE;

    }

    @Override
    public void close() throws Exception {

    }
}
