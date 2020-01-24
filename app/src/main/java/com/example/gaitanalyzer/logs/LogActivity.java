package com.example.gaitanalyzer.logs;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gaitanalyzer.R;
import com.example.gaitanalyzer.eventbus.LogEvent;

import org.greenrobot.eventbus.EventBus;

public class LogActivity extends AppCompatActivity {

    TextView log;
    LogData logData;
    volatile boolean isRunning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        log = (TextView) findViewById(R.id.log);
        log.setText("new logs");
        logData = new LogData();
        EventBus.getDefault().post(new LogEvent(logData));

        new Thread(new ViewUpdater()).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().post(new LogData());
    }

    @Override
    protected void onDestroy() {
        isRunning = false;
        super.onDestroy();
    }

    class ViewUpdater implements Runnable {

        @Override
        public void run() {
            while (isRunning) {
                try {
                    Thread.sleep(100);
                    log.setText(logData.toHTMLString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
