package net.mzimmer.android.apps.rotation;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.NotificationCompat;

import java.util.HashSet;
import java.util.Set;

public class SensorService extends IntentService {
    public static final String ACTION_STOP;
    private static final Set<Listener> listeners;
    private static final int NOTIFICATION_ID;
    private static SensorService instance;

    static {
        ACTION_STOP = "net.mzimmer.android.apps.rotation.SensorService.action.stop";

        instance = null;
        listeners = new HashSet<>();
        NOTIFICATION_ID = 0;
    }

    private SensorManager sensorManager;
    private NotificationManager notificationManager;
    private Notification notification;

    public SensorService() {
        super("SensorService");
    }

    static void start(Context context) {
        Intent intent = new Intent(context, SensorService.class);
        context.startService(intent);
    }

    private static Intent stopIntent(Context context) {
        Intent intent = new Intent(context, SensorService.class);
        intent.setAction(ACTION_STOP);
        return intent;
    }

    static void stop(Context context) {
        context.startService(stopIntent(context));
    }

    public static boolean isRunning() {
        return instance != null;
    }

    public static void add(Listener listener) {
        listeners.add(listener);

        if (isRunning()) {
            instance.sensorManager.registerListener(listener, listener.getSensor(), listener.getSensorDelay());
        }
    }

    public static void remove(Listener listener) {
        listeners.remove(listener);

        if (isRunning()) {
            instance.sensorManager.unregisterListener(listener, listener.getSensor());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        for (Listener listener : listeners) {
            sensorManager.registerListener(listener, listener.getSensor(), listener.getSensorDelay());
            listener.onSensorServiceCreate(this);
        }

        Intent viewMainActivityIntent = MainActivity.viewIntent(getApplicationContext());
        PendingIntent viewMainActivityPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, viewMainActivityIntent, 0);

        Intent stopSensorServiceIntent = stopIntent(getApplicationContext());
        PendingIntent stopSensorServicePendingIntent = PendingIntent.getService(getApplicationContext(), 0, stopSensorServiceIntent, 0);
        NotificationCompat.Action stopSensorServiceAction = new NotificationCompat.Action(R.drawable.abc_ic_clear_mtrl_alpha, getApplicationContext().getString(R.string.stop), stopSensorServicePendingIntent);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notification = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.rotation)
                .setContentTitle(getString(R.string.rotation))
                .setContentText(getString(R.string.sensor_polling_active))
                .setOngoing(true)
                .setContentIntent(viewMainActivityPendingIntent)
                .addAction(stopSensorServiceAction).build();

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        instance = null;

        notificationManager.cancel(NOTIFICATION_ID);

        for (Listener listener : listeners) {
            sensorManager.unregisterListener(listener, listener.getSensor());
            listener.onSensorServiceDestroy(this);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_STOP.equals(action)) {
                stopSelf();
            }
        }
    }

    public interface Listener extends SensorEventListener {
        void onSensorServiceCreate(SensorService service);

        void onSensorServiceDestroy(SensorService service);

        Sensor getSensor();

        int getSensorDelay();
    }
}