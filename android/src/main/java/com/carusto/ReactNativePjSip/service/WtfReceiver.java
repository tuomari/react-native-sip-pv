package com.carusto.ReactNativePjSip.service;

import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class WtfReceiver extends BroadcastReceiver {
    private static final String TAG = "WtfReceiver";

    public WtfReceiver(){
        Log.w(TAG, "[WTF] receiver created");
    }

    public static void init(Context ctx) {
        IntentFilter f = new IntentFilter();
        f.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
        ctx.registerReceiver(new WtfReceiver(), f);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "[WTF] Bluetooth broadcast received Intent: " + intent);
        //Log.i(TAG, "[Bluetooth] Bluetooth broadcast received Extras: " + intent.getExtras() );
        for (String key : intent.getExtras().keySet()) {
            Log.d(TAG, "[WTF] intent key: " + key + " Value " + intent.getExtras().get(key));
        }
        Log.i(TAG, "[WTF] Bluetooth broadcast received Action " + action);

    }
}
