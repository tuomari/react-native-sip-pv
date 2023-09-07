package com.carusto.ReactNativePjSip.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.carusto.ReactNativePjSip.R;

public class PjSipForegroundHelper implements AutoCloseable {
    public static final String CHANNEL_ID = "IccPjSIPForegroundServiceChannel";
    private static final int CALL_NOTIFICATION_ID = 1;

    private static final String TAG = "PjSipForegroundHelper";

    private final NotificationManager mNotificationManager;
    private final Service service;
    private NotificationCompat.Builder mNotificationBuilder;


    public PjSipForegroundHelper(Service service){
        this.service = service;
        NotificationChannel serviceChannel = new NotificationChannel(
            CHANNEL_ID,
            "PjSip foreground Service Channel",
            NotificationManager.IMPORTANCE_HIGH
        );
        this.mNotificationManager = service.getSystemService(NotificationManager.class);
        this.mNotificationManager.createNotificationChannel(serviceChannel);

    }

    public void removeFromForeground() {
        mNotificationBuilder = null;
        service.stopForeground(Service.STOP_FOREGROUND_REMOVE);
    }

    public void putToForeground(String destination) {

        String notificationText = null;
        try {
            if (destination != null && !destination.isBlank()) {
                int startIdx = destination.indexOf(':');
                int endIdx = destination.indexOf('@');

                if (endIdx > 0) {
                    notificationText = destination.substring(startIdx + 1, endIdx);
                }
            }
        } catch (Exception e) {
            // never throw exception while parsing phone number
        }

        if (mNotificationBuilder != null) {
            Log.w(TAG, "Notification already exists. Only update.. with " + notificationText);
            if (notificationText != null) {
                mNotificationBuilder.setContentText(notificationText);
                mNotificationManager.notify(CALL_NOTIFICATION_ID, mNotificationBuilder.build());
            }
            return;
        }

        if (notificationText == null) {
            notificationText = "-";
        }

        this.mNotificationBuilder = new NotificationCompat.Builder(service, CHANNEL_ID);

        Intent notificationIntent = service.getPackageManager()
            .getLaunchIntentForPackage(service.getPackageName())
            .setPackage(null)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);


        //notificationIntent.setPackage("com.mobileclientv2");
        PendingIntent pendingIntent = PendingIntent.getActivity(service.getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = mNotificationBuilder
            // TODO: Localizations!
            .setContentTitle("Active call in ICC Manager")
            .setContentText(notificationText)
            //´.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSmallIcon(R.drawable.phone_icon)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_CALL)
            .build();
        service.startForeground(CALL_NOTIFICATION_ID, notification);

    }

    @Override
    public void close() throws Exception {

    }
}
