package com.carusto.ReactNativePjSip.action;

import android.content.Intent;
import android.util.Log;
import com.carusto.ReactNativePjSip.PjEventType;
import com.carusto.ReactNativePjSip.PjSipService;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class PjSipReactAction {

    public static final String CALLBACK_EXTRA_KEY = "callback_id";

    private static final String TAG = "PjSipReactAction";


    protected void sendEventHandled(PjSipService service, Intent original) {
        sendEventHandled(service, original, null);
    }

    protected void sendEventHandledJson(PjSipService service, Intent original, JSONObject result) {
        this.sendEventHandled(service, original, result.toString());
    }

    protected void sendEventHandled(PjSipService service, Intent original, String data) {
        Intent intent = new Intent();
        intent.setAction(PjEventType.EVENT_HANDLED.eventName);
        intent.putExtra(CALLBACK_EXTRA_KEY, original.getIntExtra(CALLBACK_EXTRA_KEY, -1));
        if (data != null) {
            intent.putExtra("data", data);
        }
        service.sendBroadcast(intent);
    }

    protected void sendEventException(PjSipService service, Intent original, Exception e) {
        Intent intent = new Intent();
        intent.setAction(PjEventType.EVENT_HANDLED.eventName);
        intent.putExtra(CALLBACK_EXTRA_KEY, original.getIntExtra(CALLBACK_EXTRA_KEY, -1));
        intent.putExtra("exception", e.getMessage());

        service.sendBroadcast(intent);
    }

    protected static void formatIntent(Intent intent, ReadableMap configuration) {
        if (configuration == null) {
            return;
        }

        ReadableMapKeySetIterator it = configuration.keySetIterator();
        while (it.hasNextKey()) {
            String key = it.nextKey();

            switch (configuration.getType(key)) {
                case Null:
                    intent.putExtra(key, (String) null);
                    break;
                case String:
                    intent.putExtra(key, configuration.getString(key));
                    break;
                case Number:
                    intent.putExtra(key, configuration.getInt(key));
                    break;
                case Boolean:
                    intent.putExtra(key, configuration.getBoolean(key));
                    break;
                case Map:
                    intent.putExtra(key, (Serializable) formatMap(configuration.getMap(key)));
                    break;
                default:
                    Log.w(TAG, "Unable to put extra information for intent: unknown type \"" + configuration.getType(key) + "\"");
                    break;
            }
        }
    }

    private static Map<String, Object> formatMap(ReadableMap map) {
        Map<String, Object> value = new HashMap<>();
        ReadableMapKeySetIterator mapIt = map.keySetIterator();

        while (mapIt.hasNextKey()) {
            String mapKey = mapIt.nextKey();

            switch (map.getType(mapKey)) {
                case Null:
                    value.put(mapKey, null);
                    break;
                case String:
                    value.put(mapKey, map.getString(mapKey));
                    break;
                case Number:
                    value.put(mapKey, map.getInt(mapKey));
                    break;
                case Boolean:
                    value.put(mapKey, map.getBoolean(mapKey));
                    break;
                case Array:
                    value.put(mapKey, map.getArray(mapKey).toArrayList());
                    break;
                case Map:
                    value.put(mapKey, formatMap(map.getMap(mapKey)));
                    break;
                default:
                    Log.w(TAG, "Unable to put extra information for intent: unknown type \"" + map.getType(mapKey) + "\"");
                    break;
            }
        }

        return value;
    }
}
