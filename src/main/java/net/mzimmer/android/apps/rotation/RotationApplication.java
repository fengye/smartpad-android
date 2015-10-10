package net.mzimmer.android.apps.rotation;

import android.app.Application;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

public class RotationApplication extends Application {
    private static RotationApplication instance;
    private Preferences preferences;
    private Sensor sensor;

    static RotationApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        preferences = new Preferences(getApplicationContext());
    }

    Preferences getPreferences() {
        return preferences;
    }

    Sensor getSensor() {
        if (sensor == null) {
            SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }
        return sensor;
    }
}
