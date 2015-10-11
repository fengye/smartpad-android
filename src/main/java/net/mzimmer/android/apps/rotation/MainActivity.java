package net.mzimmer.android.apps.rotation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener, OnTextChangeListener, SensorEventListener {
	private static final String ACTION_NETWORK_STARTED;
	private static final String ACTION_NETWORK_STOPPED;
	private static final String ACTION_NETWORK_FAILED;

	private static final String EXTRA_EXCEPTION;

	private static final HashMap<Integer, Integer> SENSOR_DELAY_RADIO_BUTTON_IDS;

	static {
		ACTION_NETWORK_STARTED = "net.mzimmer.android.apps.rotation.NetworkService.action.network.started";
		ACTION_NETWORK_STOPPED = "net.mzimmer.android.apps.rotation.NetworkService.action.network.stopped";
		ACTION_NETWORK_FAILED = "net.mzimmer.android.apps.rotation.NetworkService.action.network.failed";

		EXTRA_EXCEPTION = "net.mzimmer.android.apps.rotation.NetworkService.extra.exception";

		SENSOR_DELAY_RADIO_BUTTON_IDS = new HashMap<>();
		SENSOR_DELAY_RADIO_BUTTON_IDS.put(SensorManager.SENSOR_DELAY_FASTEST, R.id.sensor_delay_fastest);
		SENSOR_DELAY_RADIO_BUTTON_IDS.put(SensorManager.SENSOR_DELAY_GAME, R.id.sensor_delay_game);
		SENSOR_DELAY_RADIO_BUTTON_IDS.put(SensorManager.SENSOR_DELAY_NORMAL, R.id.sensor_delay_normal);
		SENSOR_DELAY_RADIO_BUTTON_IDS.put(SensorManager.SENSOR_DELAY_UI, R.id.sensor_delay_ui);
	}

	private Handler handler;
	private NetworkServiceBroadcastReceiver networkServiceBroadcastReceiver;
	private SensorManager sensorManager;
	private Preferences preferences;
	private ViewGroup setup;
	private ViewGroup waiting;
	private ViewGroup running;
	private FloatingActionButton start;
	private FloatingActionButton stop;
	private RadioGroup sensorDelay;
	private EditText destinationHost;
	private EditText destinationPort;
	private TextView info;

	public static Intent viewIntent(Context context) {
		return new Intent(context, MainActivity.class);
	}

	public static void triggerNetworkStarted(Context context) {
		Intent intent = new Intent(context, MainActivity.NetworkServiceBroadcastReceiver.class);
		intent.setAction(ACTION_NETWORK_STARTED);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}

	public static void triggerNetworkStopped(Context context) {
		Intent intent = new Intent(context, MainActivity.NetworkServiceBroadcastReceiver.class);
		intent.setAction(ACTION_NETWORK_STOPPED);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}

	public static void triggerNetworkFailed(Context context, Exception e) {
		Intent intent = new Intent(context, MainActivity.NetworkServiceBroadcastReceiver.class);
		intent.setAction(ACTION_NETWORK_FAILED);
		intent.putExtra(EXTRA_EXCEPTION, e);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		registerNetworkServiceBroadcastReceiver();
		preferences = RotationApplication.getInstance().getPreferences();
		setup = (ViewGroup) findViewById(R.id.setup);
		waiting = (ViewGroup) findViewById(R.id.waiting);
		running = (ViewGroup) findViewById(R.id.running);
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

		updateUI();
	}

	private void registerNetworkServiceBroadcastReceiver() {
		initNetworkServiceBroadcastReceiver();
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
		manager.registerReceiver(networkServiceBroadcastReceiver, new IntentFilter(ACTION_NETWORK_STARTED));
		manager.registerReceiver(networkServiceBroadcastReceiver, new IntentFilter(ACTION_NETWORK_STOPPED));
		manager.registerReceiver(networkServiceBroadcastReceiver, new IntentFilter(ACTION_NETWORK_FAILED));
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Registered main activities network service broadcast receiver");
	}

	private void updateUI() {
		UIState state = NetworkService.isRunning() ? UIState.RUNNING : UIState.SETUP;
		updateUI(state);
	}

	private void initNetworkServiceBroadcastReceiver() {
		if (networkServiceBroadcastReceiver == null) {
			networkServiceBroadcastReceiver = new NetworkServiceBroadcastReceiver();
		}
	}

	private void updateUI(final UIState state) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				switch (state) {
					case SETUP: {
						getSensorDelayRadioButtonFromValue(preferences.getSensorDelay()).setChecked(true);
						destinationHost.setText(preferences.getDestinationHost());
						destinationPort.setText(String.valueOf(preferences.getDestinationPort()));
						setup.setVisibility(View.VISIBLE);
						waiting.setVisibility(View.GONE);
						running.setVisibility(View.GONE);
						unregisterSensorListener();
						break;
					}
					case WAITING: {
						waiting.setVisibility(View.VISIBLE);
						break;
					}
					case RUNNING: {
						setup.setVisibility(View.GONE);
						waiting.setVisibility(View.GONE);
						registerSensorListener();
						running.setVisibility(View.VISIBLE);
						break;
					}
					default:
						throw new IllegalStateException();
				}
			}
		});
	}

	private RadioButton getSensorDelayRadioButtonFromValue(int value) {
		return ((RadioButton) findViewById(getSensorDelayRadioButtonIdFromValue(value)));
	}

	private void unregisterSensorListener() {
		initSensorListener();
		sensorManager.unregisterListener(MainActivity.this, RotationApplication.getInstance().getSensor());
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Unregistered main activities sensor listener");
	}

	private void registerSensorListener() {
		initSensorListener();
		initHandler();
		sensorManager.registerListener(MainActivity.this, RotationApplication.getInstance().getSensor(), SensorManager.SENSOR_DELAY_UI, handler);
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Registered main activities sensor listener");
	}

	private static int getSensorDelayRadioButtonIdFromValue(int value) {
		if (!SENSOR_DELAY_RADIO_BUTTON_IDS.containsKey(value)) {
			value = Preferences.DEFAULT_SENSOR_DELAY;
		}
		return SENSOR_DELAY_RADIO_BUTTON_IDS.get(value);
	}

	private void initSensorListener() {
		if (sensorManager == null) {
			sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		}
	}

	private void initHandler() {
		if (handler == null) {
			HandlerThread thread = new HandlerThread("MainActivityHandlerThread");
			thread.start();
			handler = new Handler(thread.getLooper());
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterSensorListener();
		unregisterNetworkServiceBroadcastReceiver();
	}

	private void unregisterNetworkServiceBroadcastReceiver() {
		initNetworkServiceBroadcastReceiver();
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
		manager.unregisterReceiver(networkServiceBroadcastReceiver);
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Unregistered main activities network service broadcast receiver");
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.start: {
				MainActivity.this.updateUI(UIState.WAITING);
				String host = preferences.getDestinationHost();
				int port = preferences.getDestinationPort();
				NetworkService.start(getApplicationContext(), host, port);
				break;
			}
			case R.id.stop: {
				MainActivity.this.updateUI(UIState.WAITING);
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

	private static int getSensorDelayValueFromRadioButtonId(int radioButtonId) {
		for (Map.Entry<Integer, Integer> entry : SENSOR_DELAY_RADIO_BUTTON_IDS.entrySet()) {
			if (radioButtonId == entry.getValue()) {
				return entry.getKey();
			}
		}
		return Preferences.DEFAULT_SENSOR_DELAY;
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
		updateUIInfo(stringBuilder.toString());
	}

	private void updateUIInfo(final String info) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MainActivity.this.info.setText(info);
			}
		});
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	private enum UIState {
		SETUP, WAITING, RUNNING
	}

	private class NetworkServiceBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null) {
				final String action = intent.getAction();
				if (ACTION_NETWORK_STARTED.equals(action)) {
					handleActionNetworkStart();
				} else if (ACTION_NETWORK_STOPPED.equals(action)) {
					handleActionNetworkStop();
				} else if (ACTION_NETWORK_FAILED.equals(action)) {
					final Exception e = (Exception) intent.getSerializableExtra(EXTRA_EXCEPTION);
					handleActionNetworkFail(e);
				} else {
					throw new IllegalArgumentException();
				}
			}
		}

		public void handleActionNetworkStart() {
			MainActivity.this.updateUI(UIState.RUNNING);
		}

		public void handleActionNetworkStop() {
			MainActivity.this.updateUI(UIState.SETUP);
		}

		public void handleActionNetworkFail(Exception e) {
			MainActivity.this.updateUI(UIState.SETUP);
			e.printStackTrace();
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).warning(e.getLocalizedMessage());
			View container = findViewById(R.id.container);
			Snackbar.make(container, e.getLocalizedMessage(), Snackbar.LENGTH_SHORT);
		}
	}
}
