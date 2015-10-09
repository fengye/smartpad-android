package net.mzimmer.android.apps.rotation;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.widget.TextView;

public class SensorEventTextViewListener implements SensorEventListener {
    private final TextView textView;

    public SensorEventTextViewListener(TextView textView) {
        this.textView = textView;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(event.timestamp);
        stringBuilder.append(System.getProperty("line.separator"));
        for (int i = 0; i < event.values.length; ++i) {
            stringBuilder.append(System.getProperty("line.separator"));
            stringBuilder.append(i);
            stringBuilder.append(':');
            stringBuilder.append(' ');
            stringBuilder.append(Float.toString(event.values[i]));
        }
        ((Activity) textView.getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(stringBuilder.toString());
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
