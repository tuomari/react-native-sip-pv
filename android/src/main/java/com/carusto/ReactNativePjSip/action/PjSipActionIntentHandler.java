package com.carusto.ReactNativePjSip.action;

import android.content.Intent;
import com.carusto.ReactNativePjSip.PjSipService;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;

public interface PjSipActionIntentHandler {
    void handle(PjSipService pjSipService, Intent intent);
}
