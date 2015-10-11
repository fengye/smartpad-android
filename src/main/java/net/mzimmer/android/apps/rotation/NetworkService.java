package net.mzimmer.android.apps.rotation;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class NetworkService extends Service implements SensorEventListener {
    private static final String ACTION_NETWORK_STOP;

    private static final String EXTRA_HOST;
    private static final String EXTRA_PORT;

    private static final int NOTIFICATION_ID;
    private static NetworkService instance;

    static {
        ACTION_NETWORK_STOP = "net.mzimmer.android.apps.rotation.NetworkService.action.network.stop";

        EXTRA_HOST = "net.mzimmer.android.apps.rotation.NetworkService.extra.host";
        EXTRA_PORT = "net.mzimmer.android.apps.rotation.NetworkService.extra.port";

        NOTIFICATION_ID = 0;
        instance = null;
    }

    private Handler handler;
    private DatagramSocket socket;
    private SensorEventPacketFactory factory;
    private NotificationManager notificationManager;
    private SensorManager sensorManager;

    static void start(Context context, String host, int port) {
        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra(EXTRA_HOST, host);
        intent.putExtra(EXTRA_PORT, port);
        context.startService(intent);
    }

    static void stop(Context context) {
        Intent intent = new Intent(context, NetworkService.class);
        context.stopService(intent);
    }

    static boolean isRunning() {
        return instance != null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        HandlerThread thread = new HandlerThread("NetworkHandlerThread");
        thread.start();
        handler = new Handler(thread.getLooper());
        StopBroadcastReceiver stopBroadcastReceiver = new StopBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(stopBroadcastReceiver, new IntentFilter(ACTION_NETWORK_STOP));
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        socket = new DatagramSocket();

                        final String host = intent.getStringExtra(EXTRA_HOST);
                        final int port = intent.getIntExtra(EXTRA_PORT, -1);

                        InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(host), port);
                        factory = new SensorEventPacketFactory(address);

                        sensorManager.registerListener(NetworkService.this, RotationApplication.getInstance().getSensor(), RotationApplication.getInstance().getPreferences().getSensorDelay());
                        MainActivity.triggerNetworkStarted(getApplicationContext());

                        Notification notification = buildNotification(address);
                        notificationManager.notify(NOTIFICATION_ID, notification);
                    } catch (UnknownHostException | SocketException e) {
                        MainActivity.triggerNetworkFailed(getApplicationContext(), e);
                    }
                }
            });
        }

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        sensorManager.unregisterListener(this, RotationApplication.getInstance().getSensor());
        notificationManager.cancel(NOTIFICATION_ID);

        MainActivity.triggerNetworkStopped(getApplicationContext());
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramPacket packet = factory.createDatagramPacket(event);
                    socket.send(packet);
                } catch (IOException e) {
                    stopSelf();
                    MainActivity.triggerNetworkFailed(getApplicationContext(), e);
                }
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private Notification buildNotification(InetSocketAddress address) {
        Intent viewMainActivityIntent = MainActivity.viewIntent(getApplicationContext());
        PendingIntent viewMainActivityPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, viewMainActivityIntent, 0);

        Intent stopNetworkServiceIntent = new Intent(getApplicationContext(), StopBroadcastReceiver.class);
        stopNetworkServiceIntent.setAction(ACTION_NETWORK_STOP);
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

    public class StopBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                final String action = intent.getAction();
                if (ACTION_NETWORK_STOP.equals(action)) {
                    NetworkService.this.stopSelf();
                }
            }
        }
    }
}
