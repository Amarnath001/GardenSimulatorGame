package com.garden.sim.core;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Clock {
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private final EventBus bus;
    private final Logger log;
    private volatile long periodSeconds = 3600; // 1 real hour = 1 simulated day (real-time)
    private final AtomicInteger day = new AtomicInteger(0);

    public Clock(EventBus bus, Logger log) {
        this.bus = bus; this.log = log;
    }

    public void start() {
        exec.scheduleAtFixedRate(() -> {
            int d = day.incrementAndGet();
            log.info("DayTick " + d);
            bus.publish(EventBus.Topic.DAY_TICK, d);
        }, 0, periodSeconds, TimeUnit.SECONDS);
        log.info("Clock started: 1 tick per " + periodSeconds + " sec");
    }

    public void stop() { exec.shutdownNow(); }

    /** For development: use smaller seconds-per-day. */
    public void setScaleSecondsPerDay(long seconds) {
        this.periodSeconds = Math.max(1, seconds);
    }

    public int currentDay() { return day.get(); }
}
