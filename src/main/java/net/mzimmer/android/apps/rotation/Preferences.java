package net.mzimmer.android.apps.rotation;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;

public class Preferences {

    public static final int SENSOR_SAMPLING_DEFAULT;
    public static final String DESTINATION_HOST_DEFAULT;
    public static final int DESTINATION_PORT_DEFAULT;
    private static final String SHARED_PREFERENCES_NAME;
    private static final String SENSOR_SAMPLING_PREFERENCES_KEY;
    private static final String DESTINATION_HOST_PREFERENCES_KEY;
    private static final String DESTINATION_PORT_PREFERENCES_KEY;

    static {
        SHARED_PREFERENCES_NAME = "rotationPreferences";
        SENSOR_SAMPLING_PREFERENCES_KEY = "sensorSampling";
        DESTINATION_HOST_PREFERENCES_KEY = "destinationHost";
        DESTINATION_PORT_PREFERENCES_KEY = "destinationPort";

        SENSOR_SAMPLING_DEFAULT = SensorManager.SENSOR_DELAY_GAME;
        DESTINATION_HOST_DEFAULT = "";
        DESTINATION_PORT_DEFAULT = 0;
    }

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;

    public Preferences(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();
    }

    public int getSensorSampling() {
        return sharedPreferences.getInt(SENSOR_SAMPLING_PREFERENCES_KEY, SENSOR_SAMPLING_DEFAULT);
    }

    public void setSensorSampling(int sensorSampling) {
        sharedPreferencesEditor.putInt(SENSOR_SAMPLING_PREFERENCES_KEY, sensorSampling);
        sharedPreferencesEditor.commit();
    }

    public String getDestinationHost() {
        return sharedPreferences.getString(DESTINATION_HOST_PREFERENCES_KEY, DESTINATION_HOST_DEFAULT);
    }

    public void setDestinationHost(String destinationHost) {
        sharedPreferencesEditor.putString(DESTINATION_HOST_PREFERENCES_KEY, destinationHost);
        sharedPreferencesEditor.commit();
    }

    public int getDestinationPort() {
        return sharedPreferences.getInt(DESTINATION_PORT_PREFERENCES_KEY, DESTINATION_PORT_DEFAULT);
    }

    public void setDestinationPort(int destinationPort) {
        sharedPreferencesEditor.putInt(DESTINATION_PORT_PREFERENCES_KEY, destinationPort);
        sharedPreferencesEditor.commit();
    }
}
