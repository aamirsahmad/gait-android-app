package com.example.gaitanalyzer.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.gaitanalyzer.MainActivity;
import com.example.gaitanalyzer.R;
import com.example.gaitanalyzer.eventbus.MessageEvent;
import com.example.gaitanalyzer.utils.TimeSeriesUtil;

import org.greenrobot.eventbus.EventBus;

import static com.example.gaitanalyzer.App.CHANNEL_ID;

public class SensorService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Gait Sensors Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_android)
                .setContentIntent(pendingIntent)
                .build();

//        // send data to EventBus from Service thread
//        EventBus.getDefault().post(new MessageEvent("Hello everyone!"));
        Log.d("SensorService", "starting thread");

        // send data to EventBus from a child thread of Service thread
        ExampleRunnable exampleRunnable = new ExampleRunnable();
        new Thread(exampleRunnable).start();

        startForeground(1, notification);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class ExampleRunnable implements Runnable, SensorEventListener2 {
        private boolean isRunning = false;

        String data = "";
        @Override
        public void run() {

            while (isRunning) {

            }
            EventBus.getDefault().post(new MessageEvent(data));
            Log.d("SensorService", data);


//            for (int i = 0; i < 5; i++) {
//
//                EventBus.getDefault().post(new MessageEvent("" + i));
//
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
        }

        public void resume() {
            isRunning = true;
        }


        @Override
        public void onFlushCompleted(Sensor sensor) {

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                data = String.format("%d; ACC; %f; %f; %f; %f; %f; %f\n", event.timestamp, event.values[0], event.values[1], event.values[2], 0.f, 0.f, 0.f);

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        int refreshRate;
        String ip;
        String port;
        boolean stream;
        String userId;

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
            sb.append(refreshRate + ", " + ip + ", " + port + ", " + stream + ", " + ip + ", " + userId + "\n\n");
            sb.append("index,userID,timeMs,accX,accY,accZ,vSum\n" + accData + "\n\n");
            sb.append("Current broker queue size: " + queueSize + "\n\n");
            sb.append("Total messages (chunks) sent: " + messages + "\n\n");
            sb.append("Total data-points collected: " + dataPointsCollected + "\n\n");
            sb.append("Current sampling frequency: " + frequency + "Hz \n\n");

            log.setText(sb.toString());

        }
    }
}
