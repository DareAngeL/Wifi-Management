package com.rene.wifimanagement;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class NotificationService {

    public static final int NOTIFY_ID = 170103;
    public static final String CHANNEL_NAME = "notification";
    public static final String CHANNEL_DESCRIPTION = "notify the owner";
    public static final String CHANNEL_ID = "170103";
    private static final String textTitleCurrentDay = "'s DUE DATE TODAY!";
    private static final String textTitlePastDueDate = " is past on his due date! Remind him/her Now!";

    private final Context context;
    private NotificationCompat.Builder builder;

    public NotificationService(Context _context) {
        context = _context;
    }

    public NotificationService BuildNotification(String userName, String content, boolean isPastDueDate) {
        builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(userName + (isPastDueDate? textTitlePastDueDate : textTitleCurrentDay))
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        builder.setStyle(new NotificationCompat.InboxStyle());
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(alarmSound);

        _createNotificationChannel();
        return this;
    }

    private void _createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
            channel.setDescription(CHANNEL_DESCRIPTION);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void Notify() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(NOTIFY_ID, builder.build());
    }
}