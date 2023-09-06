package com.carusto.ReactNativePjSip.utils;

import android.media.AudioDeviceInfo;


import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AudioDeviceUtils {
    public static JSONObject toJson(AudioDeviceInfo device) throws JSONException {
        JSONObject object = new JSONObject();
        // IOS has ids as strings.. Let's keep them as strings here also.
        object.put("id", Integer.toString(device.getId()));
        object.put("type", device.getType()); // TODO: Handle ios compatibility
        object.put("name", device.getProductName()); // This is more or less useless in android!
        return object;
    }

    public static JSONArray toJson(AudioDeviceInfo[] devices) throws JSONException {
        JSONArray array = new JSONArray();
        for (AudioDeviceInfo device : devices) {
            array.put(toJson(device));
        }
        return array;
    }
}
