package com.carusto.ReactNativePjSip;

import android.util.Log;

public interface NamespacedEnum {
    static final String TAG = "NamespacedEnum";
    static <T extends Enum<T>> T findByNamespacedName(Class<T> clz, String eventName) {
        int pos = PjsipConfig.getNamespace().length();
        if (eventName.length() > pos && eventName.startsWith(PjsipConfig.getNamespace())) {
            try {
                return T.valueOf(clz, eventName.substring(pos));
            } catch (Exception e) {
                Log.e(TAG, "Unknown event type " + eventName + " " + e.toString());
            }
        }
        return null;
    }
}
