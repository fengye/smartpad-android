package net.mzimmer.android.apps.rotation;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.hardware.Sensor;
import android.support.v4.app.NotificationCompat;

import java.util.HashSet;
import java.util.Set;

public class Rotation extends Application {
    static NotificationListener notificationListener;
    static SensorListener sensorListener;

    @Override
    public void onCreate() {
        super.onCreate();

        Intent viewMainActivityIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent viewMainActivityPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, viewMainActivityIntent, 0);

        Intent stopSensorServiceIntent = EventService.intent(getApplicationContext(), StartStopTrigger.ACTION_STOP, null);
        PendingIntent stopSensorServicePendingIntent = PendingIntent.getService(getApplicationContext(), 0, stopSensorServiceIntent, 0);
        NotificationCompat.Action stopSensorServiceAction = new NotificationCompat.Action(R.drawable.abc_ic_clear_mtrl_alpha, getApplicationContext().getString(R.string.stop), stopSensorServicePendingIntent);

        Set<NotificationCompat.Action> actions = new HashSet<>();
        actions.add(stopSensorServiceAction);

        notificationListener = new NotificationListener(getApplicationContext(), R.mipmap.rotation, getString(R.string.rotation), getString(R.string.sensor_polling_active), true, viewMainActivityPendingIntent, actions);
        sensorListener = new SensorListener(getApplicationContext(), Sensor.TYPE_ROTATION_VECTOR);
    }
}

