package com.example.gaitanalyzer;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.example.gaitanalyzer.logs.LogActivity;
import com.example.gaitanalyzer.services.SensorService;
import com.example.gaitanalyzer.utils.Defaults;

import java.io.File;

import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseButton;
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    // init on create
    private File myDir;

    Defaults defaults;
    SharedPreferences sharedPreferences;

    MaterialPlayPauseButton recordingButton;

    // WakeLock
    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    String userId;
    Boolean stream;
    int refreshRate;
    String ip;
    String port;

    private String data = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "GaitAnalyzer::SensorsWakeLockTag");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        recordingButton = findViewById(R.id.play_pause);

        // Preferences
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);
        defaults = new Defaults(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        refreshPreferences();

        initDir();
    }

    public void onClickPlayPause(View view) {
        MaterialPlayPauseDrawable.State currentState = recordingButton.getState();
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (currentState == MaterialPlayPauseDrawable.State.Play) {
            currentState = MaterialPlayPauseDrawable.State.Pause;
            startRecordingService(view);
            editor.putString("PlayPauseBtn", "Pause");

        } else {
            currentState = MaterialPlayPauseDrawable.State.Play;
            stopRecordingService(view);
            editor.putString("PlayPauseBtn", "Play");
        }
        editor.commit(); // commit changes
        recordingButton.setState(currentState);
    }

    private void refreshPreferences() {
        refreshRate = Integer.parseInt(sharedPreferences.getString("sensor_refresh_rate", defaults.getRefreshRate()));
        ip = sharedPreferences.getString("ip", defaults.getHostname());
        port = sharedPreferences.getString("port", defaults.getPort());
        stream = sharedPreferences.getBoolean("stream", defaults.getStream());
        userId = sharedPreferences.getString("user_id", defaults.getUserId());
    }

    @Override
    protected void onResume() {
        super.onResume();
        defaults = new Defaults(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        refreshPreferences();
        // update recording button state
        if (sharedPreferences.getString("PlayPauseBtn", "").equals("Pause")) {
            recordingButton.setState(MaterialPlayPauseDrawable.State.Pause);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.m1) {
            Intent intent = new Intent(this, LogActivity.class);
            this.startActivity(intent);
        } else if (item.getItemId() == R.id.m2) {
            Intent intent = new Intent(this, SettingsActivity.class);
            this.startActivity(intent);
        }
        return true;
    }

    public void startRecordingService(View v) {
        wakeLock.acquire();
        Intent serviceIntent = new Intent(this, SensorService.class);
        serviceIntent.putExtra("msg", "Collecting gait data ...");
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopRecordingService(View v) {
        Intent serviceIntent = new Intent(this, SensorService.class);
        stopService(serviceIntent);
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initDir() {
        int permission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, 1);
        }

        myDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "gait_data");
        myDir.mkdirs();
    }
}
