package com.example.gaitanalyzer.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.example.gaitanalyzer.MainActivity;
import com.example.gaitanalyzer.R;
import com.example.gaitanalyzer.WebSocketEcho;
import com.example.gaitanalyzer.eventbus.LogEvent;
import com.example.gaitanalyzer.logs.LogData;
import com.example.gaitanalyzer.utils.Defaults;
import com.example.gaitanalyzer.utils.TimeSeriesUtil;
import com.example.gaitanalyzer.websocket.Broker;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.gaitanalyzer.App.CHANNEL_ID;

public class SensorService extends Service implements SensorEventListener {
    private static final String TAG = SensorService.class.getSimpleName();

    private SensorManager mSensorManager = null;

    long rawAccelerometerEventsCount = 0;
    long accelerometerEventsCollected = 0;
    long prevAccelerometerEventCaptureTime = 0;
    private final int collectionRateMs = 19; // todo move to settings
    SharedPreferences sharedPreferences;
    Defaults defaults;

    FileWriter writer;
    BufferedWriter bufferedWriter;
    File myDir;

    ExecutorService threadPool;
    WebSocketEcho socket;

    private Broker broker;
    private Broker broker2;

    String userId;
    Boolean stream;
    int refreshRate;
    String ip;
    String port;

    private LogData logData;
    private File file;

    Thread timer;
    private boolean timerRunning;


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // get sensor manager on starting the service
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        this.defaults = new Defaults(getApplicationContext());
        refreshPreferences();
        createRecordingFile();
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // todo : check from shared preferences which type of sensor to collect
        int samplingRate = 0; // fastest possible
        if (mSensorManager != null) {
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), samplingRate);
        }

        String input = intent.getStringExtra("msg");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Gait Sensors Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_android)
                .setContentIntent(pendingIntent)
                .build();

        EventBus.getDefault().register(this);

        timerRunning = true;
        timer = new Thread(new TimeElapsed());
        timer.start();

        startForeground(1, notification);

        if (stream) {
            initStreaming();
        }

        return START_NOT_STICKY;
    }

    private void initStreaming() {
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

    private void createRecordingFile() {
        try {
            myDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "gait_data");
            System.out.println("myDir.getAbsolutePath()" + myDir.getAbsolutePath());
            System.out.println("myDir.canRead()" + myDir.canRead());
            String FILENAME = "sensors_" + System.currentTimeMillis() + ".csv";
            file = new File(myDir, FILENAME);
            Log.d(TAG, "Writing to " + file.getAbsolutePath());
            writer = new FileWriter(file);
            bufferedWriter = new BufferedWriter(writer);
            bufferedWriter.write("index,userID,timeMs,accX,accY,accZ,vSum\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        mSensorManager.flush(this);
        mSensorManager.unregisterListener(this);
        EventBus.getDefault().unregister(this);

        if (stream) {
            broker.continueProducing = Boolean.FALSE;
        }
        prevAccelerometerEventCaptureTime = 0;
        if (stream) {
            closePool();
        }
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (timer != null) {
            timerRunning = false;
            timer.interrupt();
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                rawAccelerometerEventsCount++;

                long timeInMillis = System.currentTimeMillis() - SystemClock.elapsedRealtime() + (event.timestamp / 1000000L);
                if (prevAccelerometerEventCaptureTime == 0 || timeInMillis - prevAccelerometerEventCaptureTime >= collectionRateMs) {
                    accelerometerEventsCollected++;
                    double vsum = Math.sqrt(
                            (event.values[0] * event.values[0])
                                    + (event.values[1] * event.values[1])
                                    + (event.values[2] * event.values[2])
                    );

                    prevAccelerometerEventCaptureTime = timeInMillis;
                    Log.d(TAG, "date.getTime: " + timeInMillis);

                    String accData = String.format("%d, %s, %d, %f, %f, %f, %f", accelerometerEventsCollected, userId, timeInMillis, event.values[0], event.values[1], event.values[2], vsum);
                    if (logData != null) {
                        logData.setIndex(accelerometerEventsCollected);
                        logData.setUserID(userId);
                        logData.setTimeMs(timeInMillis);
                        logData.setAccX(event.values[0]);
                        logData.setAccY(event.values[1]);
                        logData.setAccZ(event.values[2]);
                        logData.setvSum(vsum);
                    }

                    bufferedWriter.write(accData + "\n");

                    int socketMessage = -1;
                    if (stream) {
                        socketMessage = socket.getMessagesWebSocket();
                    }

                    int brokerSize = stream ? broker.getQueueSize() : -1;
                    if (logData != null) {
                        updateLog(accData, brokerSize, rawAccelerometerEventsCount, collectionRateMs, socketMessage);
                    }

                    // ADD DATA TO BROKER
                    if (stream) {
                        addData(accData, this.broker);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    private void updateLog(String accData, int queueSize, long dataPointsCollected,
                           int collectionRateMs, int messages) {
        /**
         * Accelerometer readings:
         * Current broker queue size:
         * Total data-points collected:
         * Current sampling frequency:
         */
        int frequency = TimeSeriesUtil.getFrequency(collectionRateMs);
        StringBuilder sb = new StringBuilder();
        sb.append("index,userID,timeMs,accX,accY,accZ,vSum\n" + accData + "\n\n");


        logData.setQueueSize(queueSize);
        logData.setMessages(messages);
        logData.setDataPointsCollected(dataPointsCollected);
        logData.setFrequency(frequency);
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

    private void refreshPreferences() {
        refreshRate = Integer.parseInt(sharedPreferences.getString("sensor_refresh_rate", defaults.getRefreshRate()));
        ip = sharedPreferences.getString("ip", defaults.getHostname());
        port = sharedPreferences.getString("port", defaults.getPort());
        stream = sharedPreferences.getBoolean("stream", defaults.getStream());
        userId = sharedPreferences.getString("user_id", defaults.getUserId());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLogEvent(LogEvent event) {
        logData = event.getLogData();
        logData.setCurrentRecordingAbsolutePath(file.getAbsolutePath());
    }

    public static long elapsedTimeS = 0;

    class TimeElapsed implements Runnable {
        @Override
        public void run() {
            while (timerRunning) {
                try {
                    elapsedTimeS++;
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
