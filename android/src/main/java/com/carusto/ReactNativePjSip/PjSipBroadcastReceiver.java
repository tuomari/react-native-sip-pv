package com.carusto.ReactNativePjSip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import com.carusto.ReactNativePjSip.utils.ArgumentUtils;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

public class PjSipBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "PjSipBroadcastReceiver";

    private final AtomicInteger seq = new AtomicInteger(0);

    private ReactApplicationContext context;

    private final Map<Integer, Callback> callbacks = new ConcurrentHashMap<>();

    public PjSipBroadcastReceiver(ReactApplicationContext context) {
        this.context = context;
    }

    public void setContext(ReactApplicationContext context) {
        this.context = context;
    }

    public int register(Callback callback) {
        final int id = seq.incrementAndGet();
        callbacks.put(id, callback);
        return id;
    }

    public IntentFilter getFilter() {
        IntentFilter filter = new IntentFilter();
        for (PjEventType type : PjEventType.values()) {
            filter.addAction(type.eventName);
        }

        return filter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "Received \"" + action + "\" response from service (" + ArgumentUtils.dumpIntentExtraParameters(intent) + ")");
        final PjEventType actionEvent = PjEventType.findEventByName(action);
        if (actionEvent == null) {
            Log.w(TAG, "No event found by action: " + action);
            return;
        }
        if (actionEvent.reactEventName != null) {
            callReactAction(intent, actionEvent.reactEventName);
        } else {
            onCallback(intent);
        }

    }

    private void callReactAction(Intent intent, String reactEventName) {
        String json = intent.getStringExtra("data");
        Object params = ArgumentUtils.fromJson(json);
        emit(reactEventName, params);
    }

    private void onCallback(Intent intent) {
        // Define callback
        Callback callback = null;

        if (intent.hasExtra("callback_id")) {
            int id = intent.getIntExtra("callback_id", -1);
            if (callbacks.containsKey(id)) {
                callback = callbacks.remove(id);
            } else {
                Log.w(TAG, "Callback with \"" + id + "\" identifier not found (\"" + intent.getAction() + "\")");
            }
        }

        if (callback == null) {
            return;
        }

        // -----
        if (intent.hasExtra("exception")) {
            Log.w(TAG, "Callback executed with exception state: " + intent.getStringExtra("exception"));
            callback.invoke(false, intent.getStringExtra("exception"));
        } else if (intent.hasExtra("data")) {
            Object params = ArgumentUtils.fromJson(intent.getStringExtra("data"));
            callback.invoke(true, params);
        } else {
            callback.invoke(true, true);
        }
    }

    private void emit(String eventName, @Nullable Object data) {
        Log.d(TAG, "emit " + eventName + " / " + data);

        context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, data);
    }
}
