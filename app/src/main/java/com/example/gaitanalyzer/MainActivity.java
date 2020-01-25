package com.example.gaitanalyzer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Chronometer;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.example.gaitanalyzer.eventbus.ChronometerEvent;
import com.example.gaitanalyzer.logs.LogActivity;
import com.example.gaitanalyzer.services.SensorService;
import com.example.gaitanalyzer.utils.Defaults;
import com.example.gaitanalyzer.utils.PermissionHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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

    private Chronometer chronometer;
    private boolean isChronometerRunning;
    private long chronometerPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "GaitAnalyzer::SensorsWakeLockTag");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        chronometer = findViewById(R.id.chronometer);
        chronometer.setFormat("Elapsed Time: %s");

        recordingButton = findViewById(R.id.play_pause);

//        recordingButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                MaterialPlayPauseDrawable.State newState;
//                if (recordingButton.getState() == MaterialPlayPauseDrawable.State.Play) {
//                    newState = MaterialPlayPauseDrawable.State.Pause;
//                    startRecordingService(v);
//                }
//                else {
//                    newState = MaterialPlayPauseDrawable.State.Play;
//                    stopRecordingService(v);
//                }
//                recordingButton.setState(newState);
//            }
//        });

        // Preferences
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);
        defaults = new Defaults(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        refreshPreferences();

        PermissionHelper.getPermissionsFromAndroidOS(this);
        myDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "gait_data");
        myDir.mkdirs();

        EventBus.getDefault().register(this);
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
        Toast.makeText(this, "onResume",
                Toast.LENGTH_LONG).show();
        super.onResume();
        defaults = new Defaults(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        refreshPreferences();
        // update recording button state
        if (sharedPreferences.getString("PlayPauseBtn", "").equals("Pause")) {
            recordingButton.setState(MaterialPlayPauseDrawable.State.Pause);
        }

        chronometer.setBase(SystemClock.elapsedRealtime() + elapsedTimeOfService);
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
        startChronometer();
    }

    public void stopRecordingService(View v) {
        Intent serviceIntent = new Intent(this, SensorService.class);
        stopService(serviceIntent);
        stopChronometer(v);
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
        chronometerPause = chronometer.getBase();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void startChronometer() {
        if (!isChronometerRunning) {
            chronometer.setBase(SystemClock.elapsedRealtime() + elapsedTimeOfService);
            chronometer.start();
            isChronometerRunning = true;
        }
    }

    public void stopChronometer(View v) {
        long elapsedTime = SystemClock.elapsedRealtime() - chronometer.getBase();
        Toast.makeText(this, "Elapsed time of trial " + elapsedTime / 1000,
                Toast.LENGTH_LONG).show();
        chronometer.stop();
        isChronometerRunning = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLogEvent(ChronometerEvent event) {
        System.out.println("Chronometer " + event.getTime());
        Log.d(TAG, "Chronometer " + event.getTime());
        elapsedTimeOfService = event.getTime();
    }

    long elapsedTimeOfService;
}
