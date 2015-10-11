package net.mzimmer.android.apps.rotation;

import android.app.Application;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

public class RotationApplication extends Application {
	private static Sensor sensor;

	public static Sensor getSensor() {
		return sensor;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
	}
}
