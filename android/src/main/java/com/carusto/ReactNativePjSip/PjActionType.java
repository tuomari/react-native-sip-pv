package com.carusto.ReactNativePjSip;

import com.carusto.ReactNativePjSip.action.*;
import org.jetbrains.annotations.Nullable;

public enum PjActionType implements NamespacedEnum {
    ACTION_START(new StartPjsipAction()),
    ACTION_CREATE_ACCOUNT(new AccountCreateAction()),
    ACTION_CHANGE_CODEC_SETTINGS(new ChangeCodecSettingsAction()),
    ACTION_REGISTER_ACCOUNT(new AccountRegisterAction()),
    ACTION_DELETE_ACCOUNT(new AccountDeleteAction()),
    ACTION_MAKE_CALL(new MakeCallAction()),
    ACTION_HANGUP_CALL(new CallHangupAction()),
    ACTION_DECLINE_CALL(new CallDeclineAction()),
    ACTION_ANSWER_CALL(new CallAnswerAction()),
    ACTION_HOLD_CALL(new CallHoldAction()),
    ACTION_UNHOLD_CALL(new CallUholdAction()),
    ACTION_MUTE_CALL(new CallMuteAction()),
    ACTION_UNMUTE_CALL(new CallUnmuteAction()),
    ACTION_XFER_CALL(new CallXferAction()),
    ACTION_XFER_REPLACES_CALL(new CallXferReplaceAction()),
    ACTION_REDIRECT_CALL(new CallRedirectAction()),
    ACTION_DTMF_CALL(new CallDTFMAction()),
    ACTION_SET_SERVICE_CONFIGURATION( new SetServiceConfigAction()),

    ;

    private static final String TAG = "PjActionType";
    @Nullable
    public final PjSipActionIntentHandler intentHandler ;


    PjActionType(PjSipActionIntentHandler intentHandler) {
        this.intentHandler = intentHandler;
        this.actionName = PjsipConfig.getNamespace() + name();
    }


    static PjActionType findActionByName(String eventName) {
       return NamespacedEnum.findByNamespacedName(PjActionType.class, eventName);
    }

    public final String actionName;

}
