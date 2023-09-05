package com.carusto.ReactNativePjSip;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class PjSipBroadcastEmiter {

    private static String TAG = "PjSipBroadcastEmiter";

    private Context context;

    public PjSipBroadcastEmiter(Context context) {
        this.context = context;
    }






    public void fireEvent(PjEventType type, JSONObject data){
        Intent intent = new Intent();
        intent.setAction(type.eventName);
        intent.putExtra("data", data.toString());

        context.sendBroadcast(intent);
    }

}
