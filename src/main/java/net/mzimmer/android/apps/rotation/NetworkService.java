package net.mzimmer.android.apps.rotation;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class NetworkService extends Service implements SensorEventListener {
	private static final String EXTRA_HOST;
	private static final String EXTRA_PORT;

	private static final int NOTIFICATION_ID;
	private static boolean running;

	static {
		EXTRA_HOST = "net.mzimmer.android.apps.rotation.NetworkService.extra.host";
		EXTRA_PORT = "net.mzimmer.android.apps.rotation.NetworkService.extra.port";

		NOTIFICATION_ID = 0;
		running = false;
	}

	private Handler handler;
	private DatagramSocket socket;
	private SensorEventPacketFactory factory;
	private NotificationManager notificationManager;
	private SensorManager sensorManager;

	public static void start(Context context, String host, int port) {
		Intent intent = new Intent(context, NetworkService.class);
		intent.putExtra(EXTRA_HOST, host);
		intent.putExtra(EXTRA_PORT, port);
		context.startService(intent);
	}

	public static void stop(Context context) {
		Intent intent = new Intent(context, NetworkService.class);
		context.stopService(intent);
	}

	public static boolean isRunning() {
		return running;
	}

	@Override
	public int onStartCommand(final Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		if (intent != null) {
			initHandler();
			handler.post(new Runnable() {
				@Override
				public void run() {
					try {
						final String host = intent.getStringExtra(EXTRA_HOST);
						final int port = intent.getIntExtra(EXTRA_PORT, -1);

						InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(host), port);
						factory = new SensorEventPacketFactory(address);

						registerSensorListener();
						MainActivity.triggerNetworkStarted(getApplicationContext());

						Notification notification = buildNotification(address);
						displayNotification(notification);
					} catch (UnknownHostException e) {
						MainActivity.triggerNetworkFailed(getApplicationContext(), e);
						stopSelf();
					}
				}
			});
		}

		return Service.START_STICKY;
	}

	private void initHandler() {
		if (handler == null) {
			HandlerThread thread = new HandlerThread("NetworkServiceHandlerThread");
			thread.start();
			handler = new Handler(thread.getLooper());
		}
	}

	private void registerSensorListener() {
		initSensorManager();
		initHandler();
		sensorManager.registerListener(NetworkService.this, RotationApplication.getInstance().getSensor(), RotationApplication.getInstance().getPreferences().getSensorDelay(), handler);
		running = true;
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Registered network service' sensor listener");
	}

	private Notification buildNotification(InetSocketAddress address) {
		Intent viewMainActivityIntent = MainActivity.viewIntent(getApplicationContext());
		PendingIntent viewMainActivityPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, viewMainActivityIntent, 0);

		Intent stopNetworkServiceIntent = Stop.intent(getApplicationContext());
		PendingIntent stopNetworkServicePendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, stopNetworkServiceIntent, 0);
		NotificationCompat.Action stopNetworkServiceAction = new NotificationCompat.Action(R.drawable.abc_ic_clear_mtrl_alpha, getApplicationContext().getString(R.string.stop), stopNetworkServicePendingIntent);

		return new NotificationCompat.Builder(getApplicationContext())
				.setSmallIcon(R.mipmap.rotation)
				.setContentTitle(getString(R.string.rotation))
				.setContentText(getString(R.string.streaming_to) + " " + address.toString())
				.setOngoing(true)
				.setContentIntent(viewMainActivityPendingIntent)
				.addAction(stopNetworkServiceAction)
				.build();
	}

	private void displayNotification(Notification notification) {
		initNotificationManager();
		notificationManager.notify(NOTIFICATION_ID, notification);
	}

	private void initSensorManager() {
		if (sensorManager == null) {
			sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		}
	}

	private void initNotificationManager() {
		if (notificationManager == null) {
			notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		unregisterSensorListener();
		cancelNotification();

		MainActivity.triggerNetworkStopped(getApplicationContext());
	}

	private void unregisterSensorListener() {
		initSensorManager();
		running = false;
		sensorManager.unregisterListener(this, RotationApplication.getInstance().getSensor());
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Unregistered network service' sensor listener");
	}

	private void cancelNotification() {
		initNotificationManager();
		notificationManager.cancel(NOTIFICATION_ID);
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onSensorChanged(final SensorEvent event) {
		try {
			DatagramPacket packet = factory.createDatagramPacket(event);
			initSocket();
			socket.send(packet);
		} catch (IOException e) {
			stopSelf();
			MainActivity.triggerNetworkFailed(getApplicationContext(), e);
		}
	}

	private void initSocket() throws SocketException {
		if (socket == null) {
			socket = new DatagramSocket();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	public static class Stop extends BroadcastReceiver {
		private static final String ACTION_NETWORK_STOP;

		static {
			ACTION_NETWORK_STOP = "net.mzimmer.android.apps.rotation.NetworkService.action.network.stop";
		}

		public static Intent intent(Context context) {
			Intent intent = new Intent(context, Stop.class);
			intent.setAction(Stop.ACTION_NETWORK_STOP);
			return intent;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null) {
				final String action = intent.getAction();
				if (ACTION_NETWORK_STOP.equals(action)) {
					stop(context);
				}
			}
		}
	}
}
