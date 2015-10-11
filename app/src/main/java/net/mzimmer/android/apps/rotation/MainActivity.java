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
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener, OnTextChangeListener, SensorEventListener {
	private static final String ACTION_NETWORK_STARTED;
	private static final String ACTION_NETWORK_STOPPED;
	private static final String ACTION_NETWORK_FAILED;
	private static final String ACTION_NETWORK_FAILED_INVALID_HOST;
	private static final String ACTION_NETWORK_FAILED_INVALID_PORT;

	private static final String EXTRA_EXCEPTION;

	private static final SparseIntArray SENSOR_DELAY_RADIO_BUTTON_IDS;

	static {
		ACTION_NETWORK_STARTED = "net.mzimmer.android.apps.rotation.NetworkService.action.network.started";
		ACTION_NETWORK_STOPPED = "net.mzimmer.android.apps.rotation.NetworkService.action.network.stopped";
		ACTION_NETWORK_FAILED = "net.mzimmer.android.apps.rotation.NetworkService.action.network.failed";
		ACTION_NETWORK_FAILED_INVALID_HOST = "net.mzimmer.android.apps.rotation.NetworkService.action.network.failed.invalidHost";
		ACTION_NETWORK_FAILED_INVALID_PORT = "net.mzimmer.android.apps.rotation.NetworkService.action.network.failed.invalidPort";

		EXTRA_EXCEPTION = "net.mzimmer.android.apps.rotation.NetworkService.extra.exception";

		SENSOR_DELAY_RADIO_BUTTON_IDS = new SparseIntArray(4);
		SENSOR_DELAY_RADIO_BUTTON_IDS.put(SensorManager.SENSOR_DELAY_FASTEST, R.id.sensor_delay_fastest);
		SENSOR_DELAY_RADIO_BUTTON_IDS.put(SensorManager.SENSOR_DELAY_GAME, R.id.sensor_delay_game);
		SENSOR_DELAY_RADIO_BUTTON_IDS.put(SensorManager.SENSOR_DELAY_NORMAL, R.id.sensor_delay_normal);
		SENSOR_DELAY_RADIO_BUTTON_IDS.put(SensorManager.SENSOR_DELAY_UI, R.id.sensor_delay_ui);
	}

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

	public static void triggerNetworkFailedInvalidHost(Context context) {
		Intent intent = new Intent(context, MainActivity.NetworkServiceBroadcastReceiver.class);
		intent.setAction(ACTION_NETWORK_FAILED_INVALID_HOST);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}

	public static void triggerNetworkFailedInvalidPort(Context context) {
		Intent intent = new Intent(context, MainActivity.NetworkServiceBroadcastReceiver.class);
		intent.setAction(ACTION_NETWORK_FAILED_INVALID_PORT);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}

	private static int getSensorDelayRadioButtonIdFromValue(int value) {
		if (SENSOR_DELAY_RADIO_BUTTON_IDS.indexOfKey(value) < 0) {
			throw new IllegalArgumentException();
		}
		return SENSOR_DELAY_RADIO_BUTTON_IDS.get(value);
	}

	private static int getSensorDelayValueFromRadioButtonId(int radioButtonId) {
		int index = SENSOR_DELAY_RADIO_BUTTON_IDS.indexOfValue(radioButtonId);
		if (index < 0) {
			throw new IllegalArgumentException();
		}
		return SENSOR_DELAY_RADIO_BUTTON_IDS.keyAt(index);
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
	private EditText destinationHost;
	private EditText destinationPort;
	private TextView info;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		registerNetworkServiceBroadcastReceiver();
		preferences = new Preferences(getApplicationContext());
		setup = (ViewGroup) findViewById(R.id.setup);
		waiting = (ViewGroup) findViewById(R.id.waiting);
		running = (ViewGroup) findViewById(R.id.running);
		start = (FloatingActionButton) findViewById(R.id.start);
		stop = (FloatingActionButton) findViewById(R.id.stop);
		RadioGroup sensorDelay = (RadioGroup) findViewById(R.id.sensor_delay);
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

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unregisterSensorListener();
		unregisterNetworkServiceBroadcastReceiver();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.start: {
				MainActivity.this.updateUI(UIState.WAITING);
				String host = preferences.getDestinationHost();
				int port = preferences.getDestinationPort();
				int sensorDelay = preferences.getSensorDelay();
				NetworkService.start(getApplicationContext(), host, port, sensorDelay);
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

	@Override
	public void onTextChange(TextView view, String text) {
		switch (view.getId()) {
			case R.id.destination_host: {
				preferences.setDestinationHost(text);
				break;
			}
			case R.id.destination_port: {
				int port;
				try {
					port = Integer.parseInt(text);
					preferences.setDestinationPort(port);
					if (port < 1 || port > 65535) {
						destinationPort.setError(getString(R.string.invalid_port));
					} else {
						destinationPort.setError(null);
					}
				} catch (NumberFormatException e) {
					preferences.setDestinationPort(-1);
					destinationPort.setError(getString(R.string.unable_to_parse_integer));
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

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	private RadioButton getSensorDelayRadioButtonFromValue(int value) {
		return ((RadioButton) findViewById(getSensorDelayRadioButtonIdFromValue(value)));
	}

	private void updateUI() {
		UIState state = NetworkService.isRunning() ? UIState.RUNNING : UIState.SETUP;
		updateUI(state);
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
						start.setVisibility(View.VISIBLE);
						stop.setVisibility(View.GONE);

						unregisterSensorListener();

						break;
					}
					case WAITING: {
						waiting.setVisibility(View.VISIBLE);

						break;
					}
					case RUNNING: {
						updateUIInfo("");

						setup.setVisibility(View.GONE);
						waiting.setVisibility(View.GONE);
						running.setVisibility(View.VISIBLE);
						start.setVisibility(View.GONE);
						stop.setVisibility(View.VISIBLE);

						registerSensorListener();

						break;
					}
					default:
						throw new IllegalStateException();
				}
			}
		});
	}

	private void updateUIInfo(final String info) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MainActivity.this.info.setText(info);
			}
		});
	}

	private void initHandler() {
		if (handler == null) {
			HandlerThread thread = new HandlerThread("MainActivityHandlerThread");
			thread.start();
			handler = new Handler(thread.getLooper());
		}
	}

	private void initNetworkServiceBroadcastReceiver() {
		if (networkServiceBroadcastReceiver == null) {
			networkServiceBroadcastReceiver = new NetworkServiceBroadcastReceiver();
		}
	}

	private void registerNetworkServiceBroadcastReceiver() {
		initNetworkServiceBroadcastReceiver();
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
		manager.registerReceiver(networkServiceBroadcastReceiver, new IntentFilter(ACTION_NETWORK_STARTED));
		manager.registerReceiver(networkServiceBroadcastReceiver, new IntentFilter(ACTION_NETWORK_STOPPED));
		manager.registerReceiver(networkServiceBroadcastReceiver, new IntentFilter(ACTION_NETWORK_FAILED));
		manager.registerReceiver(networkServiceBroadcastReceiver, new IntentFilter(ACTION_NETWORK_FAILED_INVALID_HOST));
		manager.registerReceiver(networkServiceBroadcastReceiver, new IntentFilter(ACTION_NETWORK_FAILED_INVALID_PORT));
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Registered main activities network service broadcast receiver");
	}

	private void unregisterNetworkServiceBroadcastReceiver() {
		initNetworkServiceBroadcastReceiver();
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
		manager.unregisterReceiver(networkServiceBroadcastReceiver);
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Unregistered main activities network service broadcast receiver");
	}

	private void initSensorManager() {
		if (sensorManager == null) {
			sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		}
	}

	private void registerSensorListener() {
		initSensorManager();
		initHandler();
		sensorManager.registerListener(MainActivity.this, RotationApplication.getSensor(), SensorManager.SENSOR_DELAY_UI, handler);
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Registered main activities sensor listener");
	}

	private void unregisterSensorListener() {
		initSensorManager();
		sensorManager.unregisterListener(MainActivity.this, RotationApplication.getSensor());
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Unregistered main activities sensor listener");
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
					handleActionNetworkStarted();
				} else if (ACTION_NETWORK_STOPPED.equals(action)) {
					handleActionNetworkStopped();
				} else if (ACTION_NETWORK_FAILED.equals(action)) {
					final Exception e = (Exception) intent.getSerializableExtra(EXTRA_EXCEPTION);
					handleActionNetworkFailed(e);
				} else if (ACTION_NETWORK_FAILED_INVALID_HOST.equals(action)) {
					handleActionNetworkFailedInvalidHost();
				} else if (ACTION_NETWORK_FAILED_INVALID_PORT.equals(action)) {
					handleActionNetworkFailedInvalidPort();
				} else {
					throw new IllegalArgumentException();
				}
			}
		}

		public void handleActionNetworkStarted() {
			MainActivity.this.updateUI(UIState.RUNNING);
		}

		public void handleActionNetworkStopped() {
			MainActivity.this.updateUI(UIState.SETUP);
		}

		public void handleActionNetworkFailed(Exception e) {
			MainActivity.this.updateUI(UIState.SETUP);
			e.printStackTrace();
			View container = findViewById(R.id.container);
			Snackbar.make(container, e.getLocalizedMessage(), android.support.design.widget.Snackbar.LENGTH_LONG).show();
		}

		public void handleActionNetworkFailedInvalidHost() {
			MainActivity.this.updateUI(UIState.SETUP);
			View container = findViewById(R.id.container);
			Snackbar.make(container, getString(R.string.invalid_host), android.support.design.widget.Snackbar.LENGTH_LONG).show();
			destinationHost.requestFocus();
		}

		public void handleActionNetworkFailedInvalidPort() {
			MainActivity.this.updateUI(UIState.SETUP);
			View container = findViewById(R.id.container);
			Snackbar.make(container, getString(R.string.invalid_port), android.support.design.widget.Snackbar.LENGTH_LONG).show();
			destinationPort.requestFocus();
		}
	}
}
