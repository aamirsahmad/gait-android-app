package com.example.gaitanalyzer;

public class Producer implements Runnable
{
    private Broker broker;
    private boolean isRunning;

    public Producer(Broker broker)
    {
        this.broker = broker;
        isRunning = true;
    }


    @Override
    public void run()
    {
        try
        {
            while(isRunning){

            }
            for (Integer i = 1; i < 5 + 1; ++i)
            {
                System.out.println("Producer produced: " + i);
                Thread.sleep(100);
                broker.put("");
            }

            this.broker.continueProducing = Boolean.FALSE;
            System.out.println("Producer finished its job; terminating.");
        }
        catch (InterruptedException ex)
        {
            ex.printStackTrace();
        }

    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }
}
