package net.mzimmer.android.apps.rotation;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

public class NetworkService extends IntentService implements SensorService.Listener {
    public static final String ACTION_START;
    public static final String ACTION_STOP;
    public static final String EXTRA_HOST;
    public static final String EXTRA_PORT;
    private static final Set<Listener> listeners;
    private static NetworkService instance;

    static {
        ACTION_START = "net.mzimmer.android.apps.rotation.NetworkService.action.start";
        ACTION_STOP = "net.mzimmer.android.apps.rotation.NetworkService.action.stop";

        EXTRA_HOST = "net.mzimmer.android.apps.rotation.NetworkService.extra.host";
        EXTRA_PORT = "net.mzimmer.android.apps.rotation.NetworkService.extra.port";

        instance = null;
        listeners = new HashSet<>();
    }

    DatagramSocket socket;
    SensorEventPacketFactory factory;

    public NetworkService() {
        super("NetworkService");
    }

    static void start(Context context, String host, int port) {
        Intent intent = new Intent(context, NetworkService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_HOST, host);
        intent.putExtra(EXTRA_PORT, port);
        context.startService(intent);
    }

    static void stop(Context context) {
        Intent intent = new Intent(context, NetworkService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    public static boolean isRunning() {
        return instance != null;
    }

    public static void add(Listener listener) {
        listeners.add(listener);
    }

    public static void remove(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        for (Listener listener : listeners) {
            listener.onNetworkServiceCreate(this);
        }

        SensorService.add(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        instance = null;

        for (Listener listener : listeners) {
            listener.onNetworkServiceDestroy(this);
        }

        SensorService.remove(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                final String host = intent.getStringExtra(EXTRA_HOST);
                final int port = intent.getIntExtra(EXTRA_PORT, -1);
                try {
                    handleActionStart(host, port);
                } catch (UnknownHostException e) {
                    // TODO
                    e.printStackTrace();
                    System.exit(0);
                } catch (SocketException e) {
                    // MUST NOT happen
                    e.printStackTrace();
                    System.exit(1);
                }
            } else if (ACTION_STOP.equals(action)) {
                handleActionStop();
            }
        }
    }

    private void handleActionStart(String host, int port) throws UnknownHostException, SocketException {
        SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(host), port);
        factory = new SensorEventPacketFactory(socketAddress);
        socket = new DatagramSocket();

        SensorService.start(getApplicationContext());
    }

    private void handleActionStop() {
        SensorService.stop(getApplicationContext());
    }

    @Override
    public void onSensorServiceCreate(SensorService service) {
    }

    @Override
    public void onSensorServiceDestroy(SensorService service) {
        stopSelf();
    }

    @Override
    public Sensor getSensor() {
        return RotationApplication.getInstance().getSensor();
    }

    @Override
    public int getSensorDelay() {
        return RotationApplication.getInstance().getPreferences().getSensorDelay();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            DatagramPacket packet = factory.createDatagramPacket(event);
            socket.send(packet);
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
            System.exit(0);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public interface Listener {
        void onNetworkServiceCreate(NetworkService service);

        void onNetworkServiceDestroy(NetworkService service);
    }
}
