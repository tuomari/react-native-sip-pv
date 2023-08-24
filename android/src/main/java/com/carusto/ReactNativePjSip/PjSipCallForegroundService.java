package com.carusto.ReactNativePjSip;

import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class PjSipCallForegroundService extends Service {
  public static final String CHANNEL_ID = "IccPjSIPForegroundServiceChannel";
  public static final String TAG = "PjSipCallForegroundService";

  @Override
  public void onCreate() {
    super.onCreate();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d(TAG, "Received intent " + intent);
    String input = intent.getStringExtra("inputExtra");
    if (input == null || input.isBlank()) {
      input = "Foobar1234";
    }
    createNotificationChannel();
    Intent notificationIntent = new Intent(Intent.ACTION_MAIN);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
    Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Foreground Service")
      .setContentText(input)
      //.setSmallIcon(R.drawable.autofill_inline_suggestion_chip_background)
      .setContentIntent(pendingIntent)
      .build();
    startForeground(1, notification);

    //do heavy work on a background thread
    //stopSelf();
    return START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
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
      NotificationManager manager = getSystemService(NotificationManager.class);
      manager.createNotificationChannel(serviceChannel);
    }
  }
}
