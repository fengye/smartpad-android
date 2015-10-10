package net.mzimmer.android.apps.rotation;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

/*
 * TODO Rewrite with Service instead of IntentService
 * TODO Fully switch UI, use wrappers and merge content_main.xml and activity_main.xml
 * TODO Display hint for working after button clicks
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener, OnTextChangeListener, SensorService.Listener, NetworkService.Listener {
    private static final HashMap<Integer, Integer> SENSOR_DELAY_RADIO_BUTTON_IDS;

    static {
        SENSOR_DELAY_RADIO_BUTTON_IDS = new HashMap<>();
        SENSOR_DELAY_RADIO_BUTTON_IDS.put(SensorManager.SENSOR_DELAY_FASTEST, R.id.sensor_delay_fastest);
        SENSOR_DELAY_RADIO_BUTTON_IDS.put(SensorManager.SENSOR_DELAY_GAME, R.id.sensor_delay_game);
        SENSOR_DELAY_RADIO_BUTTON_IDS.put(SensorManager.SENSOR_DELAY_NORMAL, R.id.sensor_delay_normal);
        SENSOR_DELAY_RADIO_BUTTON_IDS.put(SensorManager.SENSOR_DELAY_UI, R.id.sensor_delay_ui);
    }

    private Preferences preferences;
    private FloatingActionButton start;
    private FloatingActionButton stop;
    private RadioGroup sensorDelay;
    private EditText destinationHost;
    private EditText destinationPort;
    private TextView info;

    public static Intent viewIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    private static int getSensorDelayRadioButtonIdFromValue(int value) {
        if (!SENSOR_DELAY_RADIO_BUTTON_IDS.containsKey(value)) {
            value = Preferences.DEFAULT_SENSOR_DELAY;
        }
        return SENSOR_DELAY_RADIO_BUTTON_IDS.get(value);
    }

    private static int getSensorDelayValueFromRadioButtonId(int radioButtonId) {
        for (Map.Entry<Integer, Integer> entry : SENSOR_DELAY_RADIO_BUTTON_IDS.entrySet()) {
            if (radioButtonId == entry.getValue()) {
                return entry.getKey();
            }
        }
        return Preferences.DEFAULT_SENSOR_DELAY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        preferences = RotationApplication.getInstance().getPreferences();
        start = (FloatingActionButton) findViewById(R.id.start);
        stop = (FloatingActionButton) findViewById(R.id.stop);
        sensorDelay = (RadioGroup) findViewById(R.id.sensor_delay);
        destinationHost = (EditText) findViewById(R.id.destination_host);
        destinationPort = (EditText) findViewById(R.id.destination_port);
        info = (TextView) findViewById(R.id.info);

        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        sensorDelay.setOnCheckedChangeListener(this);
        OnTextChangeListener.Helper.register(destinationHost, this);
        OnTextChangeListener.Helper.register(destinationPort, this);

        getSensorDelayRadioButtonFromValue(preferences.getSensorDelay()).setChecked(true);
        destinationHost.setText(preferences.getDestinationHost());
        destinationPort.setText(String.valueOf(preferences.getDestinationPort()));
        updateUI();

        SensorService.add(this);
        NetworkService.add(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SensorService.remove(this);
        NetworkService.remove(this);
    }

    private void updateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (SensorService.isRunning() && NetworkService.isRunning()) {
                    start.setVisibility(View.GONE);
                    stop.setVisibility(View.VISIBLE);
                    info.setVisibility(View.VISIBLE);
                } else {
                    start.setVisibility(View.VISIBLE);
                    stop.setVisibility(View.GONE);
                    info.setVisibility(View.GONE);
                }
            }
        });
    }

    private RadioButton getSensorDelayRadioButtonFromValue(int value) {
        return ((RadioButton) findViewById(getSensorDelayRadioButtonIdFromValue(value)));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start: {
                String host = preferences.getDestinationHost();
                int port = preferences.getDestinationPort();
                NetworkService.start(getApplicationContext(), host, port);
                break;
            }
            case R.id.stop: {
                NetworkService.stop(getApplicationContext());
                break;
            }
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (group.getId()) {
            case R.id.sensor_delay: {
                preferences.setSensorDelay(getSensorDelayValueFromRadioButtonId(checkedId));
                break;
            }
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void onTextChange(TextView view, String text) {
        switch (view.getId()) {
            case R.id.destination_host: {
                preferences.setDestinationHost(text);
                break;
            }
            case R.id.destination_port: {
                try {
                    int port = Integer.parseInt(text);
                    if (port < 0 || port > 65535) {
                        throw new NumberFormatException();
                    }
                    preferences.setDestinationPort(port);
                    destinationPort.setError(null);
                } catch (NumberFormatException e) {
                    destinationPort.setError(getString(R.string.unable_to_parse_port));
                }
                break;
            }
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void onSensorServiceCreate(SensorService service) {
        updateUI();
    }

    @Override
    public void onSensorServiceDestroy(SensorService service) {
        updateUI();
    }

    @Override
    public Sensor getSensor() {
        return RotationApplication.getInstance().getSensor();
    }

    @Override
    public int getSensorDelay() {
        return preferences.getSensorDelay();
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                info.setText(stringBuilder.toString());
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // ignored
    }

    @Override
    public void onNetworkServiceCreate(NetworkService service) {
        updateUI();
    }

    @Override
    public void onNetworkServiceDestroy(NetworkService service) {
        updateUI();
    }
}