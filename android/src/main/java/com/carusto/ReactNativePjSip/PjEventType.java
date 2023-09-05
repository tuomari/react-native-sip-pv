package com.carusto.ReactNativePjSip;

/**
 * Events that can be emitted to the react application.
 */
public enum PjEventType implements NamespacedEnum {

    // EVENT_STARTED(),
    // EVENT_ACCOUNT_CREATED(),
    EVENT_REGISTRATION_CHANGED("pjSipRegistrationChanged"),
    EVENT_CALL_CHANGED("pjSipCallChanged"),
    EVENT_CALL_TERMINATED("pjSipCallTerminated"),
    EVENT_CALL_RECEIVED("pjSipCallReceived"),
    //EVENT_CALL_SCREEN_LOCKED("pjSipCallScreenLocked"),
    EVENT_MESSAGE_RECEIVED("pjSipMessageReceived"),
    EVENT_HANDLED(),
    ;

    private static final String TAG = "PjEventType";
    public final String eventName;
    public final String reactEventName;

    PjEventType(String reactEventName) {
        this.reactEventName = reactEventName;
        this.eventName = PjsipConfig.getNamespace() + name();
    }

    PjEventType() {
        this(null);
    }

    static PjEventType findEventByName(String eventName) {
        return NamespacedEnum.findByNamespacedName(PjEventType.class, eventName);
    }


}
