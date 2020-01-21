package com.example.gaitanalyzer.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.gaitanalyzer.MainActivity;
import com.example.gaitanalyzer.R;
import com.example.gaitanalyzer.eventbus.MessageEvent;

import org.greenrobot.eventbus.EventBus;

import static com.example.gaitanalyzer.App.CHANNEL_ID;

public class AccelerometerService extends Service {

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
//
//        // send data to EventBus from a child thread of Service thread
//        ExampleRunnable exampleRunnable = new ExampleRunnable();
//        new Thread(exampleRunnable).start();

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

    class ExampleRunnable implements Runnable {

        @Override
        public void run() {
            for (int i = 0; i < 5; i++) {

                EventBus.getDefault().post(new MessageEvent("" + i));

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
