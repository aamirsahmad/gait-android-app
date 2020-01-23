package com.example.gaitanalyzer.eventbus;

import com.example.gaitanalyzer.logs.LogData;

public class LogEvent {

    private final LogData logData;

    public LogEvent(LogData logData) {
        this.logData = logData;
    }

    public LogData getLogData() {
        return logData;
    }
}
