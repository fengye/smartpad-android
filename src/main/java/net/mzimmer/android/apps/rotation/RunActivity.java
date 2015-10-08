package net.mzimmer.android.apps.rotation;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

public class RunActivity extends AppCompatActivity implements SensorEventListener {
    private TextView info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        info = (TextView) findViewById(R.id.info);

        SensorService.addListener(getApplicationContext(), this, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SensorService.removeListener(getApplicationContext(), this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(event.timestamp);
        stringBuilder.append(System.getProperty("line.separator"));
        for (int i = 0; i < event.values.length; ++i) {
            stringBuilder.append(System.getProperty("line.separator"));
            stringBuilder.append(i);
            stringBuilder.append(':');
            stringBuilder.append(' ');
            stringBuilder.append(Float.toString(event.values[i]));
        }
        info.setText(stringBuilder.toString());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
