package com.example.gaitanalyzer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import com.aditya.filebrowser.Constants;
import com.aditya.filebrowser.FileChooser;
import com.aditya.filebrowser.FolderChooser;
import com.aditya.filebrowser.utils.UIUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseButton;
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable;


public class MainActivity extends AppCompatActivity implements SensorEventListener2 {
    static int PICK_FILE_REQUEST = 1;

    SensorManager manager;
    TextView outputPath;
    TextView serverReply;
    TextView log;
    boolean isRunning;
    final String TAG = "SensorLog";
    FileWriter writer;
    WebSocketEcho socket;
    File reterivedFile;

    private Broker broker;
    private Broker broker2;
    ExecutorService threadPool;

    long dataPointsCollected = 0; // refactor to big int

    long lastTime = 0;

    int samplingRate = 0;// 25000 micro-seconds = 40 Hz, Default: SensorManager.SENSOR_DELAY_NORMAL
    int collectionRateMs = 19;
    long samplingIndex = 0;


    // remove
    int refreshRate;
    String ip;
    String port;
    boolean stream;
    String userId;

    Defaults defaults;
    SharedPreferences sharedPreferences;


    //    @BindView(R.id.play_pause)
    MaterialPlayPauseButton recordingButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        isRunning = false;

        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        outputPath = (TextView) findViewById(R.id.outputPath);
        serverReply = (TextView) findViewById(R.id.serverReply);
        log = (TextView) findViewById(R.id.log);
        recordingButton = findViewById(R.id.play_pause);

        // Preferences
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);
        defaults = new Defaults(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        refreshPreferences();

        recordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MaterialPlayPauseDrawable.State currentState = recordingButton.getState();
                if (currentState == MaterialPlayPauseDrawable.State.Play) {
                    currentState = MaterialPlayPauseDrawable.State.Pause;
                    startRecording();
                } else {
                    currentState = MaterialPlayPauseDrawable.State.Play;
                    stopRecording();
                }
                recordingButton.setState(currentState);

            }
        });
    }

    private void startRecording() {
        Log.d(TAG, "Writing to " + getStorageDir());
//        try {
//            if(ExternalStorageUtil.isExternalStorageMounted()) {
//
//                // Check whether this app has write external storage permission or not.
//                int writeExternalStoragePermission = ContextCompat.checkSelfPermission(ExternalStorageActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
//                // If do not grant write external storage permission.
//                if(writeExternalStoragePermission!= PackageManager.PERMISSION_GRANTED)
//                {
//                    // Request user to grant write external storage permission.
//                    ActivityCompat.requestPermissions(ExternalStorageActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
//                }else {
//
//                    // Save email_public.txt file to /storage/emulated/0/DCIM folder
//                    String publicDcimDirPath = ExternalStorageUtil.getPublicExternalStorageBaseDir(Environment.DIRECTORY_DCIM);
//
//                    File newFile = new File(publicDcimDirPath, "email_public.txt");
//
//                    FileWriter fw = new FileWriter(newFile);
//
//                    fw.write(emailEditor.getText().toString());
//
//                    fw.flush();
//
//                    fw.close();
//
//                    Toast.makeText(getApplicationContext(), "Save to public external storage success. File Path " + newFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
//                }
//            }
//
//        }catch (Exception ex)
//        {
//            Log.e(LOG_TAG_EXTERNAL_STORAGE, ex.getMessage(), ex);
//
//            Toast.makeText(getApplicationContext(), "Save to public external storage failed. Error message is " + ex.getMessage(), Toast.LENGTH_LONG).show();
//        }
        try {
            String path = getStorageDir() + "/sensors_" + System.currentTimeMillis() + ".csv";
            writer = new FileWriter(new File(path));
            writer.write("index,userID,timeMs,accX,accY,accZ,vSum\n");

            outputPath.setText(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), samplingRate);
        samplingIndex = 0;
//                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), 0);
//                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED), 0);
//                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 0);
//                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED), 0);
//                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), 0);
//                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR), 0);

        isRunning = true;
        try {
            broker = new Broker();
            broker2 = new Broker();
            threadPool = Executors.newFixedThreadPool(1);
            // stream data if in streaming mode
            if (stream == true) {
                String url = "http://" + ip + ":" + port + "/gait";
                socket = new WebSocketEcho("1", broker, broker2, userId, new URL(url));
                threadPool.execute(socket);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        isRunning = false;
        manager.flush(MainActivity.this);
        manager.unregisterListener(MainActivity.this);
        broker.continueProducing = Boolean.FALSE;
        lastTime = 0;
        closePool();
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshPreferences() {
        refreshRate = Integer.parseInt(sharedPreferences.getString("sensor_refresh_rate", defaults.getRefreshRate()));
        ip = sharedPreferences.getString("ip", defaults.getHostname());
        port = sharedPreferences.getString("port", defaults.getPort());
        stream = sharedPreferences.getBoolean("stream", defaults.getStream());
        userId = sharedPreferences.getString("user_id", defaults.getUserId());
    }

//    @Override
//    protected void onPause() {
//
//    }


    @Override
    protected void onResume() {
        super.onResume();
        defaults = new Defaults(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        refreshPreferences();
        if(reterivedFile != null){
            Intent intentShareFile = new Intent(Intent.ACTION_SEND);

            intentShareFile.setType("text/csv");
            intentShareFile.putExtra(Intent.EXTRA_STREAM,
                    Uri.parse("content://"+reterivedFile.getAbsolutePath()));

            //if you need
            this.grantUriPermission(getApplicationContext().getPackageName(), Uri.fromFile(reterivedFile), intentShareFile.FLAG_GRANT_WRITE_URI_PERMISSION | intentShareFile.FLAG_GRANT_READ_URI_PERMISSION);

            sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,Uri.parse(reterivedFile.getAbsolutePath())));

            this.startActivity(Intent.createChooser(intentShareFile, reterivedFile.getName()));

            reterivedFile = null;
        }
    }

    void closePool() {
        this.threadPool.shutdown();
    }

    void addData(String data, Broker broker) {
        class OneShotTask implements Runnable {
            String data;
            Broker broker;

            OneShotTask(String str, Broker broker) {
                data = str;
                this.broker = broker;
            }

            public void run() {
                try {
                    broker.put(data);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        Thread t = new Thread(new OneShotTask(data, broker));
        t.start();
    }

    private String getStorageDir() {
        return this.getExternalFilesDir(null).getAbsolutePath();
        //  return "/storage/emulated/0/Android/data/com.iam360.sensorlog/";
    }


    @Override
    public void onFlushCompleted(Sensor sensor) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isRunning) {
            try {
                switch (event.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                        dataPointsCollected++;
//                        String accData = String.format("%d; ACC; %f; %f; %f; %f; %f; %f\n", event.timestamp, event.values[0], event.values[1], event.values[2], 0.f, 0.f, 0.f);
//                        long timeInMillis = (new Date()).getTime()
//                                + (event.timestamp - System.currentTimeMillis()) / 1000000L;
//                        long timeInMillis = (new Date().getTime() - SystemClock.elapsedRealtime()) * 1000000
//                                + event.timestamp;
//                        long timeNow = System.currentTimeMillis();
                        long timeInMillis = System.currentTimeMillis() - SystemClock.elapsedRealtime() + (event.timestamp / 1000000L);
                        if (lastTime == 0 || timeInMillis - lastTime >= collectionRateMs) {
                            samplingIndex++;
                            double vsum = Math.sqrt(
                                    (event.values[0] * event.values[0])
                                            + (event.values[1] * event.values[1])
                                            + (event.values[2] * event.values[2])
                            );

                            lastTime = timeInMillis;
                            Log.d(TAG, "date.getTime: " + timeInMillis);

//                            String accData = String.format("%d, %d, %d, %f, %f, %f, %f", samplingIndex, userID, timeInMillis, event.values[0], event.values[1], event.values[2], vsum);
                            String accData = String.format("%d, %s, %d, %f, %f, %f, %f", samplingIndex, userId, timeInMillis, event.values[0], event.values[1], event.values[2], vsum);

                            writer.write(accData + "\n");
                            int socketMessage = -1;
                            if (socket != null) {
                                socketMessage = socket.messagesWebSocket;
                            }
                            updateLog(log, accData, broker.getQueueSize(), dataPointsCollected, collectionRateMs, socketMessage);

                            // ADD DATA TO BROKER
                            if (stream == true) {
                                addData(accData, this.broker);
                            }
                        }

                        break;
//                    case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
//                        writer.write(String.format("%d; GYRO_UN; %f; %f; %f; %f; %f; %f\n", event.timestamp, event.values[0], event.values[1], event.values[2], event.values[3], event.values[4], event.values[5]));
//                        break;
//                    case Sensor.TYPE_GYROSCOPE:
//                        writer.write(String.format("%d; GYRO; %f; %f; %f; %f; %f; %f\n", event.timestamp, event.values[0], event.values[1], event.values[2], 0.f, 0.f, 0.f));
//                        break;
//                    case Sensor.TYPE_MAGNETIC_FIELD:
//                        writer.write(String.format("%d; MAG; %f; %f; %f; %f; %f; %f\n", event.timestamp, event.values[0], event.values[1], event.values[2], 0.f, 0.f, 0.f));
//                        break;
//                    case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
//                        writer.write(String.format("%d; MAG_UN; %f; %f; %f; %f; %f; %f\n", event.timestamp, event.values[0], event.values[1], event.values[2], 0.f, 0.f, 0.f));
//                        break;
//                    case Sensor.TYPE_ROTATION_VECTOR:
//                        writer.write(String.format("%d; ROT; %f; %f; %f; %f; %f; %f\n", event.timestamp, event.values[0], event.values[1], event.values[2], event.values[3], 0.f, 0.f));
//                        break;
//                    case Sensor.TYPE_GAME_ROTATION_VECTOR:
//                        writer.write(String.format("%d; GAME_ROT; %f; %f; %f; %f; %f; %f\n", event.timestamp, event.values[0], event.values[1], event.values[2], event.values[3], 0.f, 0.f));
//                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    private void updateLog(TextView log, String accData, int queueSize, long dataPointsCollected,
//                           int collectionRateMs, int messages) {
//        /**
//         * Accelerometer readings:
//         * Current broker queue size:
//         * Total data-points collected:
//         * Current sampling frequency:
//         */
//        int frequency = getFrequency(collectionRateMs);
//        StringBuilder sb = new StringBuilder();
//        sb.append("index,userID,timeMs,accX,accY,accZ,vSum\n" + accData + "\n\n");
//        sb.append("Current broker queue size: " + queueSize + "\n\n");
//        sb.append("Total messages (chunks) sent: " + messages + "\n\n");
//        sb.append("Total data-points collected: " + dataPointsCollected + "\n\n");
//        sb.append("Current sampling frequency: " + frequency + "Hz \n\n");
//
//        log.setText(sb.toString());
//
//    }

    private void updateLog(TextView log, String accData, int queueSize, long dataPointsCollected,
                           int collectionRateMs, int messages) {
        /**
         * Accelerometer readings:
         * Current broker queue size:
         * Total data-points collected:
         * Current sampling frequency:
         */
        int frequency = getFrequency(collectionRateMs);
        StringBuilder sb = new StringBuilder();
        sb.append(refreshRate + ", " + ip + ", " + port + ", " + stream + ", " + ip + ", " + userId + "\n\n");
        sb.append("index,userID,timeMs,accX,accY,accZ,vSum\n" + accData + "\n\n");
        sb.append("Current broker queue size: " + queueSize + "\n\n");
        sb.append("Total messages (chunks) sent: " + messages + "\n\n");
        sb.append("Total data-points collected: " + dataPointsCollected + "\n\n");
        sb.append("Current sampling frequency: " + frequency + "Hz \n\n");

        log.setText(sb.toString());

    }

    private int getFrequency(int collectionRateMs) {
        float seconds = (float) collectionRateMs / 1000;
        float frequency = (float) 1 / seconds;

        return (int) frequency;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.m2) {
            Intent intent = new Intent(this, SettingsActivity.class);
            this.startActivity(intent);
        }
        else if (item.getItemId() == R.id.m1) {
            Intent i = new Intent(this, FileChooserMod.class);
            i.putExtra(Constants.INITIAL_DIRECTORY, new File(getStorageDir()).getAbsolutePath());

            i.putExtra(Constants.ALLOWED_FILE_EXTENSIONS, "csv;");

            i.putExtra(Constants.SELECTION_MODE, Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            startActivityForResult(i, PICK_FILE_REQUEST);

        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && data!=null) {
            if (resultCode == RESULT_OK) {
                Uri f = data.getData();
                File file = new File(f.getPath());
                reterivedFile = file;

            }
        }
    }


}
