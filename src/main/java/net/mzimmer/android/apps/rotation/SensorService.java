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

import java.util.HashMap;
import java.util.Map;

public class SensorService extends IntentService {
    private static final int NOTIFICATION_ID = 0;

    private static final String ACTION_START = "net.mzimmer.android.apps.rotation.action.start";
    private static final String ACTION_STOP = "net.mzimmer.android.apps.rotation.action.stop";
    private static final String ACTION_ADD_LISTENER = "net.mzimmer.android.apps.rotation.action.addListener";
    private static final String ACTION_REMOVE_LISTENER = "net.mzimmer.android.apps.rotation.action.removeListener";

    private static final String EXTRA_LISTENER = "net.mzimmer.android.apps.rotation.extra.listener";
    private static final String EXTRA_SENSOR_DELAY = "net.mzimmer.android.apps.rotation.extra.sensorDelay";

    private static final HashMap<Integer, SensorEventListener> listeners;
    private static final int ILLEGAL_LISTENER_HASH;

    static {
        listeners = new HashMap<>();
        ILLEGAL_LISTENER_HASH = -1;
    }

    private SensorManager sensorManager;
    private Sensor sensor;
    private NotificationManager notificationManager;
    private Notification notification;

    public SensorService() {
        super("SensorService");
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, SensorService.class);
        intent.setAction(ACTION_START);
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, SensorService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    public static boolean addListener(Context context, SensorEventListener listener, int sensorDelay) {
        int hash = listener.hashCode();
        assert hash != ILLEGAL_LISTENER_HASH;
        if (listeners.put(hash, listener) == null) {
            Intent intent = new Intent(context, SensorService.class);
            intent.setAction(ACTION_ADD_LISTENER);
            intent.putExtra(EXTRA_LISTENER, hash);
            intent.putExtra(EXTRA_SENSOR_DELAY, sensorDelay);
            context.startService(intent);
            return true;
        }
        return false;
    }

    public static boolean removeListener(Context context, SensorEventListener listener) {
        int hash = listener.hashCode();
        assert hash != ILLEGAL_LISTENER_HASH;
        if (listeners.remove(hash) != null) {
            Intent intent = new Intent(context, SensorService.class);
            intent.setAction(ACTION_REMOVE_LISTENER);
            intent.putExtra(EXTRA_LISTENER, hash);
            context.startService(intent);
            return true;
        }
        return false;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_ADD_LISTENER.equals(action) || ACTION_REMOVE_LISTENER.equals(action)) {
                final int hash = intent.getIntExtra(EXTRA_LISTENER, ILLEGAL_LISTENER_HASH);
                assert hash != ILLEGAL_LISTENER_HASH;
                SensorEventListener listener = listeners.get(hash);
                if (ACTION_ADD_LISTENER.equals(action)) {
                    final int sensorDelay = intent.getIntExtra(EXTRA_SENSOR_DELAY, Preferences.DEFAULT_SENSOR_DELAY);
                    handleActionAddListener(listener, sensorDelay);
                } else {
                    handleActionRemoveListener(listener);
                }
            } else if (ACTION_START.equals(action)) {
                handleActionStart();
            } else if (ACTION_STOP.equals(action)) {
                handleActionStop();
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent viewRunIntent = new Intent(getApplicationContext(), RunActivity.class);
        PendingIntent viewRunPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, viewRunIntent, 0);

        Intent stopSensorIntent = new Intent(getApplicationContext(), SensorService.class);
        stopSensorIntent.setAction(ACTION_STOP);
        PendingIntent stopSensorPendingIntent = PendingIntent.getService(getApplicationContext(), 0, stopSensorIntent, 0);
        NotificationCompat.Action stopAction = new NotificationCompat.Action(R.drawable.abc_ic_clear_mtrl_alpha, getString(R.string.stop), stopSensorPendingIntent);

        notification = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.rotation)
                .setContentTitle(getString(R.string.rotation))
                .setContentText(getString(R.string.sensor_polling_active))
                .setOngoing(true)
                .setContentIntent(viewRunPendingIntent)
                .addAction(stopAction)
                .build();
    }

    private void handleActionStart() {
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void handleActionStop() {
        for (Map.Entry<Integer, SensorEventListener> entry : listeners.entrySet()) {
            sensorManager.unregisterListener(entry.getValue());
        }
        listeners.clear();
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void handleActionAddListener(SensorEventListener listener, int sensorDelay) {
        sensorManager.registerListener(listener, sensor, sensorDelay);
    }

    private void handleActionRemoveListener(SensorEventListener listener) {
        sensorManager.unregisterListener(listener, sensor);
    }
}
