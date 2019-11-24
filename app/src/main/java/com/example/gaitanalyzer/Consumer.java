package com.example.gaitanalyzer;

import android.util.Log;

public class Consumer implements Runnable
{

    private String name;
    private Broker broker;

    private final String TAG = "CONSUMER";
    public Consumer(String name, Broker broker)
    {
        this.name = name;
        this.broker = broker;
    }


    @Override
    public void run()
    {
        try
        {
            String data;

            while (broker.continueProducing)
            {
                data = broker.get();
//                System.out.println("Consumer " + this.name + " processed data from broker: " + data);
                Log.d(TAG, "Consumer " + this.name + " processed data from broker: " + data);
                Thread.sleep(20);
            }


//            System.out.println("Comsumer " + this.name + " finished its job; terminating.");
            Log.d(TAG, "Comsumer " + this.name + " finished its job; terminating.");

        }
        catch (InterruptedException ex)
        {
            ex.printStackTrace();
        }
    }

}