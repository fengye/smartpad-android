package net.mzimmer.android.apps.rotation;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class EventService extends IntentService {
    private static final String ACTION_TRIGGER;
    private static final String EXTRA_ACTION;
    private static final String EXTRA_DATA;
    private static final Set<Listener> listeners;
    private static boolean running;

    static {
        ACTION_TRIGGER = "net.mzimmer.android.apps.rotation.eventService.action.trigger";
        EXTRA_ACTION = "net.mzimmer.android.apps.rotation.eventService.extra.action";
        EXTRA_DATA = "net.mzimmer.android.apps.rotation.eventService.extra.data";
        listeners = new HashSet<>();
        running = false;
    }

    public EventService() {
        super("EventService");
    }

    public static void add(Listener listener) {
        listeners.add(listener);
    }

    public static void remove(Listener listener) {
        listeners.remove(listener);
    }

    public static Intent intent(Context context, String action, Serializable data) {
        Intent intent = new Intent(context, EventService.class);
        intent.setAction(ACTION_TRIGGER);
        intent.putExtra(EXTRA_ACTION, action);
        intent.putExtra(EXTRA_DATA, data);
        return intent;
    }

    public static void trigger(Context context, String action, Serializable data) {
        Intent intent = intent(context, action, data);
        context.startService(intent);
    }

    public static boolean isRunning() {
        return running;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        add(Rotation.notificationListener);
        add(Rotation.sensorListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        remove(Rotation.notificationListener);
        remove(Rotation.sensorListener);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            if (ACTION_TRIGGER.equals(intent.getAction())) {
                final String action = intent.getStringExtra(EXTRA_ACTION);
                final Serializable data = intent.getSerializableExtra(EXTRA_DATA);
                for (Listener listener : listeners) {
                    listener.on(action, data);
                }
                if (StartStopTrigger.ACTION_START.equals(action)) {
                    running = true;
                } else if (StartStopTrigger.ACTION_STOP.equals(action)) {
                    running = false;
                    stopSelf();
                }
            }
        }
    }

    public interface Listener {
        void on(String action, Serializable data);
    }
}
