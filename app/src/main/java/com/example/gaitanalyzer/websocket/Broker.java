package com.example.gaitanalyzer.websocket;

//import android.util.Log;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

//import java.util.concurrent.ArrayBlockingQueue;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.LinkedBlockingDeque;

public class Broker
{
    private final String TAG = "Broker";

//    public BlockingQueue<String> queue = new LinkedBlockingDeque<>(500);
    public PriorityBlockingQueue<String> queue = new PriorityBlockingQueue<>(500, new Comp());
    public Boolean continueProducing = Boolean.TRUE;

    public void put(String data) throws InterruptedException
    {
//        Log.d(TAG, "producer added" + data);

        this.queue.put(data);
    }

    public String get() throws InterruptedException
    {
        return this.queue.poll(1, TimeUnit.SECONDS);
    }

    public int getQueueSize(){
        return queue.size();
    }
}

class Comp implements Comparator<String> {

    // Overriding compare()method of Comparator
    // for descending order of cgpa
    public int compare(String s1, String s2) {
        s1 = s1.split(",")[0];
        s2 = s2.split(",")[0];
        long i1 = Long.parseLong(s1);

        long i2 = Long.parseLong(s2);
        if (i1 > i2)
            return 1;
        else if (i1 < i2)
            return -1;
        return 0;
    }
}