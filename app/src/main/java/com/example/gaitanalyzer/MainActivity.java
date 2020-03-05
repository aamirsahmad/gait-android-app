package com.example.gaitanalyzer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import com.example.gaitanalyzer.infocard.InfoCardData;
import com.example.gaitanalyzer.logs.LogActivity;
import com.example.gaitanalyzer.services.SensorService;
import com.example.gaitanalyzer.utils.Defaults;
import com.example.gaitanalyzer.utils.PermissionHelper;
import com.example.gaitanalyzer.utils.TimeUtil;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;

import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseButton;
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable;
import android.os.Handler;


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
    private long startTime = 0;

    // Info card
    private TextView username;
    private TextView duration;
    private TextView filePath;
    private InfoCardData infoCardData;
    private Button shareButton;
    private Button openButton;
    private Button deleteButton;
    private Gson gson = new Gson();


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
        chronometer.setFormat("%s");

        recordingButton = findViewById(R.id.play_pause);

        // Info card
        infoCardData = InfoCardData.getInstance();
        username = findViewById(R.id.username);
        duration = findViewById(R.id.duration);
        filePath = findViewById(R.id.filePath);

        shareButton = findViewById(R.id.shareFile);
        deleteButton = findViewById(R.id.openFile);
        openButton = findViewById(R.id.deleteFile);


        // Preferences
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);
        defaults = new Defaults(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.clear();
//        editor.commit(); // commit changes
        refreshPreferences();

        PermissionHelper.getPermissionsFromAndroidOS(this);
        myDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "gait_data");
        myDir.mkdirs();
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

            String readableTime = TimeUtil.getReadableTime(SensorService.elapsedTimeS);
            Log.d(TAG, "readableTime = " + readableTime);
            infoCardData.setDuration(readableTime);

            // update sharedPreferences with latest trial info
            String infoCardDataJson = gson.toJson(infoCardData);
            editor.putString("InfoCardData", infoCardDataJson);

            editor.apply(); // commit changes

            updateInfoCard(infoCardData);

            stopRecordingService(view);

            editor.putString("PlayPauseBtn", "Play");

            setInfoCardButtons(true);
        }
        editor.apply(); // commit changes
        recordingButton.setState(currentState);
    }

    private void setInfoCardButtons(boolean b) {
        shareButton.setEnabled(b);
        openButton.setEnabled(b);
        deleteButton.setEnabled(b);
    }


    private void refreshPreferences() {
//        refreshRate = sharedPreferences.getInt("sensor_refresh_rate", defaults.getRefreshRate());
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

        startTime = SystemClock.elapsedRealtime() - (SensorService.elapsedTimeS * 1000);
        if (sharedPreferences.getBoolean("isChronometerRunning", false)) {
            isChronometerRunning = true;
            Log.d(TAG, "SensorService.elapsedTimeS " + SensorService.elapsedTimeS);
            chronometer.setBase(startTime);
            chronometer.start();
        } else {
            chronometer.setBase(startTime);
        }
        Log.d(TAG, "isChronometerRunning " + isChronometerRunning);

        InfoCardData infoCardData = getInfoCardDataFromPreferences();
        if (infoCardData == null) {
            return;
        } else {
            // username, duration, filepath
            updateInfoCard(infoCardData);
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
        final Handler handler = new Handler();
        int delay = sharedPreferences.getInt("sensor_initial_delay", 0) * 1000;
        Log.d(TAG, "startRecordingService.delay = " + delay);
        startTime = SystemClock.elapsedRealtime();
        chronometer.setBase(startTime);
        chronometer.stop();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Do something after 5s = 5000ms
                wakeLock.acquire();
                Intent serviceIntent = new Intent(MainActivity.this, SensorService.class);
                serviceIntent.putExtra("msg", "Collecting gait data ...");
                ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
                startChronometer();
            }
        }, delay);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void startChronometer() {
        Log.d(TAG, "startChronometer");

        if (!isChronometerRunning) {
            startTime = SystemClock.elapsedRealtime();
            chronometer.setBase(startTime);
            chronometer.start();
            isChronometerRunning = true;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isChronometerRunning", isChronometerRunning);
            editor.apply(); // commit changes
        }
    }

    public void stopChronometer(View v) {
        Log.d(TAG, "stopChronometer");
        chronometer.stop();
        isChronometerRunning = false;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isChronometerRunning", isChronometerRunning);

        editor.apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    //        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); // for file manager use case

    public void openTextFile(View view) throws FileNotFoundException {
        InfoCardData infoCardData = getInfoCardDataFromPreferences();
        if (infoCardData == null) {
            Toast.makeText(this, "File not found", Toast.LENGTH_LONG).show();
            return;
        }
        File initialFile = new File(infoCardData.getFilePath());
        Uri uri = FileProvider.getUriForFile(
                MainActivity.this,
                "com.example.gaitanalyzer.provider", //(use your app signature + ".provider" )
                initialFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "text/csv");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Open file..."));
    }

    public void shareTextFile(View view) {
        InfoCardData infoCardData = getInfoCardDataFromPreferences();
        if (infoCardData == null) {
            Toast.makeText(this, "File not found", Toast.LENGTH_LONG).show();
            return;
        }
        File initialFile = new File(infoCardData.getFilePath());
        Intent intent = new Intent(Intent.ACTION_SEND);
        Uri uri = FileProvider.getUriForFile(
                MainActivity.this,
                "com.example.gaitanalyzer.provider",
                initialFile);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share file..."));
    }

    private InfoCardData getInfoCardDataFromPreferences() {
        String infoCardDataStr = sharedPreferences.getString("InfoCardData", "");

        if (infoCardDataStr.equals("")) {
            clearInfoCard();
            return null;
        }
        InfoCardData infoCardData = gson.fromJson(infoCardDataStr, InfoCardData.class);

        File file = new File(String.valueOf(infoCardData.getFilePath()));
        if (!file.exists()) {
            clearInfoCard();
            return null;
        }

        return infoCardData;
    }

    public void deleteTextFile(View view) {
        InfoCardData infoCardData = getInfoCardDataFromPreferences();
        if (infoCardData == null) {
            Toast.makeText(this, "File not found", Toast.LENGTH_LONG).show();
            return;
        }
        File file = new File(String.valueOf(infoCardData.getFilePath()));
        file.delete();
        clearInfoCard();
    }

    private void updateInfoCard(InfoCardData infoCardData) {
        username.setText(infoCardData.getUserID());
        duration.setText(infoCardData.getDuration());

        String path = infoCardData.getFilePath();
        if (path != null && path.contains("Documents")) {
            filePath.setText(path.substring(path.indexOf("Documents")));
        }
    }

    private void clearInfoCard() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("InfoCardData", "");
        editor.apply();

        username.setText("N/A");
        duration.setText("N/A");
        filePath.setText("N/A");
        setInfoCardButtons(false);
    }
}
