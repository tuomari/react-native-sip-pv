package com.carusto.ReactNativePjSip.service;

import com.carusto.ReactNativePjSip.PjSipAccount;
import com.carusto.ReactNativePjSip.PjSipCall;
import com.carusto.ReactNativePjSip.PjSipMessage;

import org.pjsip.pjsua2.AudDevManager;
import org.pjsip.pjsua2.OnCallStateParam;
import org.pjsip.pjsua2.OnRegStateParam;

public interface PjSipCallStateListener {
    void emitCallUpdated(PjSipCall pjSipCall);

    AudDevManager getAudDevManager();

    void emitCallStateChanged(PjSipCall pjSipCall, OnCallStateParam prm);

    void emitRegistrationChanged(PjSipAccount account, OnRegStateParam prm);

    void emitMessageReceived(PjSipAccount account, PjSipMessage message);

    void emitCallReceived(PjSipAccount pjSipAccount, PjSipCall call);
}
