package net.mzimmer.android.apps.rotation;

import android.content.Intent;
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

import java.util.HashMap;
import java.util.Map;

public class SetupActivity extends AppCompatActivity {

    private static final HashMap<Integer, Integer> SENSOR_SAMPLING_RADIO_BUTTON_IDS;

    static {
        SENSOR_SAMPLING_RADIO_BUTTON_IDS = new HashMap<>();
        SENSOR_SAMPLING_RADIO_BUTTON_IDS.put(SensorManager.SENSOR_DELAY_FASTEST, R.id.sensor_sampling_fastest);
        SENSOR_SAMPLING_RADIO_BUTTON_IDS.put(SensorManager.SENSOR_DELAY_GAME, R.id.sensor_sampling_game);
        SENSOR_SAMPLING_RADIO_BUTTON_IDS.put(SensorManager.SENSOR_DELAY_NORMAL, R.id.sensor_sampling_normal);
        SENSOR_SAMPLING_RADIO_BUTTON_IDS.put(SensorManager.SENSOR_DELAY_UI, R.id.sensor_sampling_ui);
    }

    private Preferences preferences;
    private RadioGroup sensorSamplingRadioGroup;
    private EditText destinationHostEditText;
    private EditText destinationPortEditText;

    public static int getSensorSamplingRadioButtonIdFromValue(int value) {
        if (SENSOR_SAMPLING_RADIO_BUTTON_IDS.containsKey(value)) {
            return SENSOR_SAMPLING_RADIO_BUTTON_IDS.get(value);
        } else {
            return SENSOR_SAMPLING_RADIO_BUTTON_IDS.get(Preferences.SENSOR_SAMPLING_DEFAULT);
        }
    }

    public static int getSensorSamplingValueFromRadioButtonId(int radioButtonId) {
        for (Map.Entry<Integer, Integer> entry : SENSOR_SAMPLING_RADIO_BUTTON_IDS.entrySet()) {
            if (radioButtonId == entry.getValue()) {
                return entry.getKey();
            }
        }
        return Preferences.SENSOR_SAMPLING_DEFAULT;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(getApplicationContext(), RunActivity.class), 0);
            }
        });

        preferences = new Preferences(getApplicationContext());

        sensorSamplingRadioGroup = (RadioGroup) findViewById(R.id.sensor_sampling);
        destinationHostEditText = (EditText) findViewById(R.id.destination_host);
        destinationPortEditText = (EditText) findViewById(R.id.destination_port);

        sensorSamplingRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (group.getId()) {
                    case R.id.sensor_sampling:
                        preferences.setSensorSampling(getSensorSamplingValueFromRadioButtonId(checkedId));
                }
            }
        });
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

        int sensorSampling = preferences.getSensorSampling();
        String destinationHost = preferences.getDestinationHost();
        int destinationPort = preferences.getDestinationPort();

        ((RadioButton) findViewById(getSensorSamplingRadioButtonIdFromValue(sensorSampling))).setChecked(true);
        destinationHostEditText.setText(destinationHost);
        destinationPortEditText.setText(String.valueOf(destinationPort));
    }
}
