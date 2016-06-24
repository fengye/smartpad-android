package net.mzimmer.android.apps.smartpad;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

public class UnityQuaternion {
	private static final String NEW_LINE = System.getProperty("line.separator");
	private static final float F = (float) Math.sqrt(2.0d) / 2.0f;

	public static UnityQuaternion from(SensorEvent event) {
		switch (event.sensor.getType()) {
			case Sensor.TYPE_ROTATION_VECTOR:
				switch (event.values.length) {
					case 4:
					case 5:
						return new UnityQuaternion(
								F * (event.values[0] + event.values[3]),
								-F * (event.values[1] - event.values[2]),
								-F * (event.values[2] + event.values[1]),
								F * (event.values[3] - event.values[0]));
					default:
						throw new IllegalArgumentException("Unknown number of values for sensor: " + event.sensor.getName());
				}
			default:
				throw new IllegalArgumentException("Unknown sensor type: " + event.sensor.getName());
		}
	}

	public final float x;
	public final float y;
	public final float z;
	public final float w;

	public UnityQuaternion(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	public String toText() {
		return "x: " + x + NEW_LINE + "y: " + y + NEW_LINE + "z: " + z + NEW_LINE + "w: " + w;
	}
}
