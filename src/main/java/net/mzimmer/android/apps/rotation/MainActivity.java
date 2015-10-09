package net.mzimmer.android.apps.rotation;

import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener, EventService.Listener {
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
    private RadioGroup sensorDelayRadioGroup;
    private EditText destinationHostEditText;
    private EditText destinationPortEditText;
    private TextView info;
    private SensorEventTextViewListener sensorEventTextViewListener;

    private static int getSensorDelayRadioButtonIdFromValue(int value) {
        if (SENSOR_DELAY_RADIO_BUTTON_IDS.containsKey(value)) {
            return SENSOR_DELAY_RADIO_BUTTON_IDS.get(value);
        } else {
            return SENSOR_DELAY_RADIO_BUTTON_IDS.get(Preferences.DEFAULT_SENSOR_DELAY);
        }
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        preferences = new Preferences(getApplicationContext());
        start = (FloatingActionButton) findViewById(R.id.start);
        stop = (FloatingActionButton) findViewById(R.id.stop);
        sensorDelayRadioGroup = (RadioGroup) findViewById(R.id.sensor_delay);
        destinationHostEditText = (EditText) findViewById(R.id.destination_host);
        destinationPortEditText = (EditText) findViewById(R.id.destination_port);
        info = (TextView) findViewById(R.id.info);

        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        sensorDelayRadioGroup.setOnCheckedChangeListener(this);
        destinationHostEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                preferences.setDestinationHost(s.toString());
            }
        });
        destinationPortEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int destinationPort = Integer.parseInt(s.toString());
                    preferences.setDestinationPort(destinationPort);
                    destinationPortEditText.setError(null);
                } catch (NumberFormatException e) {
                    destinationPortEditText.setError(getString(R.string.unable_to_parse_integer));
                }
            }
        });

        int sensorDelay = preferences.getSensorDelay();
        String destinationHost = preferences.getDestinationHost();
        int destinationPort = preferences.getDestinationPort();

        ((RadioButton) findViewById(getSensorDelayRadioButtonIdFromValue(sensorDelay))).setChecked(true);
        destinationHostEditText.setText(destinationHost);
        destinationPortEditText.setText(String.valueOf(destinationPort));

        sensorEventTextViewListener = new SensorEventTextViewListener(info);
        Rotation.sensorListener.add(sensorEventTextViewListener, SensorManager.SENSOR_DELAY_UI);
        EventService.add(this);

        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Rotation.sensorListener.remove(sensorEventTextViewListener);
        EventService.remove(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start: {
                StartStopTrigger.start(getApplicationContext());
                break;
            }
            case R.id.stop: {
                StartStopTrigger.stop(getApplicationContext());
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
    public void on(final String action, Serializable data) {
        if (StartStopTrigger.ACTION_START.equals(action) || StartStopTrigger.ACTION_STOP.equals(action)) {
            updateUI();
        }
    }

    private void updateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (EventService.isRunning()) {
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
}
