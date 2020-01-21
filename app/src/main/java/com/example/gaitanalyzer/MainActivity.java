package com.example.gaitanalyzer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.example.gaitanalyzer.eventbus.MessageEvent;
import com.example.gaitanalyzer.services.AccelerometerService;
import com.example.gaitanalyzer.utils.Defaults;
import com.example.gaitanalyzer.utils.TimeSeriesUtil;
import com.example.gaitanalyzer.websocket.Broker;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseButton;
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable;

public class MainActivity extends AppCompatActivity implements SensorEventListener2 {

    SensorManager manager;
    TextView outputPath;
    TextView serverReply;
    TextView log;
    boolean isRunning;
    final String TAG = "SensorLog";
    FileWriter writer;
    WebSocketEcho socket;

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

    MaterialPlayPauseButton recordingButton;

    // WakeLock
    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "GaitAnalyzer::SensorsWakeLockTag");

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
                    startService(view);

//                    ExampleRunnable runnable = new ExampleRunnable();
//                    new Thread(runnable).start();
                } else {
                    currentState = MaterialPlayPauseDrawable.State.Play;
                    stopRecording();
                    stopService(view);
                }
                recordingButton.setState(currentState);

            }
        });
    }

    private void startRecording() {
        wakeLock.acquire();

        Log.d(TAG, "Writing to " + getStorageDir());
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
        wakeLock.release();
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
        int frequency = TimeSeriesUtil.getFrequency(collectionRateMs);
        StringBuilder sb = new StringBuilder();
        sb.append(refreshRate + ", " + ip + ", " + port + ", " + stream + ", " + ip + ", " + userId + " " + data + "\n\n");
        sb.append("index,userID,timeMs,accX,accY,accZ,vSum\n" + accData + "\n\n");
        sb.append("Current broker queue size: " + queueSize + "\n\n");
        sb.append("Total messages (chunks) sent: " + messages + "\n\n");
        sb.append("Total data-points collected: " + dataPointsCollected + "\n\n");
        sb.append("Current sampling frequency: " + frequency + "Hz \n\n");

        log.setText(sb.toString());

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
        if (item.getItemId() == R.id.m1) {
            Intent intent = new Intent(this, SettingsActivity.class);
            this.startActivity(intent);
        }
        return true;
    }

    public void startService(View v) {
        Intent serviceIntent = new Intent(this, AccelerometerService.class);
        serviceIntent.putExtra("inputExtra", "true");

        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, AccelerometerService.class);
        stopService(serviceIntent);
    }

    private String data = "";

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
//        Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show();
        data = event.message;
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }
}
