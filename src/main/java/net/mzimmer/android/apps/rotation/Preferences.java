package net.mzimmer.android.apps.rotation;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;

class Preferences {
    public static final int DEFAULT_SENSOR_DELAY;
    public static final String DEFAULT_DESTINATION_HOST;
    public static final int DEFAULT_DESTINATION_PORT;

    private static final String SHARED_PREFERENCES_NAME;
    private static final String SENSOR_DELAY_PREFERENCES_KEY;
    private static final String DESTINATION_HOST_PREFERENCES_KEY;
    private static final String DESTINATION_PORT_PREFERENCES_KEY;

    static {
        DEFAULT_SENSOR_DELAY = SensorManager.SENSOR_DELAY_GAME;
        DEFAULT_DESTINATION_HOST = "";
        DEFAULT_DESTINATION_PORT = 0;

        SHARED_PREFERENCES_NAME = "rotationPreferences";
        SENSOR_DELAY_PREFERENCES_KEY = "sensorDelay";
        DESTINATION_HOST_PREFERENCES_KEY = "destinationHost";
        DESTINATION_PORT_PREFERENCES_KEY = "destinationPort";
    }

    private final SharedPreferences sharedPreferences;

    public Preferences(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public int getSensorDelay() {
        return sharedPreferences.getInt(SENSOR_DELAY_PREFERENCES_KEY, DEFAULT_SENSOR_DELAY);
    }

    public void setSensorDelay(int sensorDelay) {
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putInt(SENSOR_DELAY_PREFERENCES_KEY, sensorDelay);
        sharedPreferencesEditor.apply();
    }

    public String getDestinationHost() {
        return sharedPreferences.getString(DESTINATION_HOST_PREFERENCES_KEY, DEFAULT_DESTINATION_HOST);
    }

    public void setDestinationHost(String destinationHost) {
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putString(DESTINATION_HOST_PREFERENCES_KEY, destinationHost);
        sharedPreferencesEditor.apply();
    }

    public int getDestinationPort() {
        return sharedPreferences.getInt(DESTINATION_PORT_PREFERENCES_KEY, DEFAULT_DESTINATION_PORT);
    }

    public void setDestinationPort(int destinationPort) {
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putInt(DESTINATION_PORT_PREFERENCES_KEY, destinationPort);
        sharedPreferencesEditor.apply();
    }
}
