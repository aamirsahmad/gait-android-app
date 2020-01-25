package com.example.gaitanalyzer.eventbus;

public class ChronometerEvent {
    private long time;

    public ChronometerEvent(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }
}
