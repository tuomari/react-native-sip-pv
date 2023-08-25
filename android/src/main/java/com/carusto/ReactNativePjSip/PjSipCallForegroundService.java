package com.carusto.ReactNativePjSip;

import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.carusto.ReactNativePjSip.utils.ArgumentUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

public class PjSipCallForegroundService extends Service {
  public static final String CHANNEL_ID = "IccPjSIPForegroundServiceChannel";
  public static final String TAG = "PjSipCallForegroundService";
  private NotificationCompat.Builder notificationBuilder;
  private NotificationManager manager;

  @Override
  public void onCreate() {
    super.onCreate();
    createNotificationChannel();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    //super.onStartCommand(intent, flags, startId);
    String callJson = intent.getStringExtra("call");
    String destination = intent.getStringExtra("destination");

    String notificationText = null;
    try {
      if (destination != null && !destination.isBlank()) {
        int startIdx = destination.indexOf(':');
        int endIdx = destination.indexOf('@');

        if (endIdx > 0) {
          notificationText = destination.substring(startIdx + 1, endIdx);
        }
      } else if (callJson != null && !callJson.isBlank()) {
        JsonObject call = JsonParser.parseString(callJson).getAsJsonObject();
        JsonElement remotenr = call.get("_remoteNumber");
        if (remotenr != null) {
          notificationText = remotenr.getAsString();
        }else {
          JsonElement remoteUri = call.get("remoteUri");
        }
      }
    } catch (Exception e) {
      // never throw exception while parsing phone number
    }


    if (notificationBuilder != null) {
      Log.w(TAG, "Notification already exists. Only update.. with " + notificationText);
      if (notificationText != null) {
        notificationBuilder.setContentText(notificationText);
        manager.notify(1, notificationBuilder.build());
      }
      return START_NOT_STICKY;
    }

    if (notificationText == null) {
      notificationText = "-";
    }

    this.notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);

    Intent notificationIntent = getPackageManager()
      .getLaunchIntentForPackage(getPackageName())
      .setPackage(null)
      .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);


    //notificationIntent.setPackage("com.mobileclientv2");
    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    Notification notification = notificationBuilder
      .setContentTitle("Active call in ICC Manager")
      .setContentText(notificationText)
      .setSmallIcon(R.drawable.autofill_inline_suggestion_chip_background)
      .setContentIntent(pendingIntent)
      .setCategory("call")
      .build();
    startForeground(1, notification);
    notification = notification;
    //do heavy work on a background thread
    //stopSelf();
    return START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    notificationBuilder = null;
    manager = null;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel serviceChannel = new NotificationChannel(
        CHANNEL_ID,
        "Foreground Service Channel",
        NotificationManager.IMPORTANCE_HIGH
      );
      this.manager = getSystemService(NotificationManager.class);
      manager.createNotificationChannel(serviceChannel);
    }
  }
}
