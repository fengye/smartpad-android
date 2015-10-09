package net.mzimmer.android.apps.rotation;

import android.content.Context;

public class StartStopTrigger {
    public static final String ACTION_START;
    public static final String ACTION_STOP;

    static {
        ACTION_START = "net.mzimmer.android.apps.rotation.startStopTrigger.action.start";
        ACTION_STOP = "net.mzimmer.android.apps.rotation.startStopTrigger.action.stop";
    }

    public static void start(Context context) {
        EventService.trigger(context, ACTION_START, null);
    }

    public static void stop(Context context) {
        EventService.trigger(context, ACTION_STOP, null);
    }
}
