package net.mzimmer.android.apps.rotation;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class RunActivity extends AppCompatActivity {

    private Preferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        preferences = new Preferences(getApplicationContext());

        Snackbar.make(toolbar, preferences.getSensorSampling() + " -> " + preferences.getDestinationHost() + ":" + preferences.getDestinationPort(), Snackbar.LENGTH_LONG).show();
    }

}
