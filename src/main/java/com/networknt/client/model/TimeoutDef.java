package com.networknt.client.model;

import java.util.concurrent.TimeUnit;

public class TimeoutDef {
    private long timeout;
    private TimeUnit unit;

    public TimeoutDef(long timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.unit = timeUnit;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public void setUnit(TimeUnit unit) {
        this.unit = unit;
    }
}
