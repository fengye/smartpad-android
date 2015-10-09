package net.mzimmer.android.apps.rotation;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import java.io.Serializable;
import java.util.Set;

public class NotificationListener implements EventService.Listener {
    private static int next = 0;
    private final NotificationManager notificationManager;
    private final Notification notification;
    private final int id;

    public NotificationListener(Context context, int smallIcon, String contentTitle, String contentText, boolean ongoing, PendingIntent contentIntent, Set<NotificationCompat.Action> actions) {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(smallIcon)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setOngoing(ongoing)
                .setContentIntent(contentIntent);
        for (NotificationCompat.Action action : actions) {
            builder.addAction(action);
        }
        notification = builder.build();
        id = next++;
    }

    @Override
    public void on(String action, Serializable data) {
        if (StartStopTrigger.ACTION_START.equals(action)) {
            notificationManager.notify(id, notification);
        } else if (StartStopTrigger.ACTION_STOP.equals(action)) {
            notificationManager.cancel(id);
        }
    }
}
