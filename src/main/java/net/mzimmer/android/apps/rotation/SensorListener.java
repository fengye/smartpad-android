package net.mzimmer.android.apps.rotation;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SensorListener implements EventService.Listener {
    private final HashMap<SensorEventListener, Integer> listeners;
    private final SensorManager sensorManager;
    private final Sensor sensor;

    public SensorListener(Context context, int sensorType) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(sensorType);
        listeners = new HashMap<>();
    }

    public void add(SensorEventListener listener, int sensorDelay) {
        listeners.put(listener, sensorDelay);
    }

    public void remove(SensorEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void on(String action, Serializable data) {
        if (StartStopTrigger.ACTION_START.equals(action) || StartStopTrigger.ACTION_STOP.equals(action)) {
            for (Map.Entry<SensorEventListener, Integer> listener : listeners.entrySet()) {
                if (StartStopTrigger.ACTION_START.equals(action)) {
                    sensorManager.registerListener(listener.getKey(), sensor, listener.getValue());
                } else {
                    sensorManager.unregisterListener(listener.getKey(), sensor);
                }
            }
        }
    }
}
