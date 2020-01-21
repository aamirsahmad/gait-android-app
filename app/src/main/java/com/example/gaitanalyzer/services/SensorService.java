package com.example.gaitanalyzer.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.gaitanalyzer.MainActivity;
import com.example.gaitanalyzer.R;
import com.example.gaitanalyzer.eventbus.MessageEvent;

import org.greenrobot.eventbus.EventBus;

import static com.example.gaitanalyzer.App.CHANNEL_ID;

public class SensorService extends Service implements SensorEventListener {

    /**
     * loging tag
     */
    private static final String TAG = SensorService.class.getSimpleName();

    private SensorManager mSensorManager = null;


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // get sensor manager on starting the service
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // todo : check from shared preferences which type of sensor to collect
        int samplingRate = 0; // fastest possible
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), samplingRate);

        String input = intent.getStringExtra("inputExtra");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Gait Sensors Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_android)
                .setContentIntent(pendingIntent)
                .build();


//
//        // send data to EventBus from a child thread of Service thread
//        SensorListener exampleRunnable = new SensorListener();
//        new Thread(exampleRunnable).start();

        startForeground(1, notification);

        return START_NOT_STICKY;
    }

    public void showNotification() {

    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // grab the values
        StringBuilder sb = new StringBuilder();
        for (float value : event.values)
            sb.append(String.valueOf(value)).append(" | ");

        Log.d(TAG, "received sensor valures are: " + sb.toString());
                // send data to EventBus from Service thread
        EventBus.getDefault().post(new MessageEvent(sb.toString()));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }
}
