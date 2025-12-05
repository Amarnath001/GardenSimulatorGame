package com.garden.sim.core;

import com.garden.sim.core.logger.Logger;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Clock {
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private final EventBus bus;
    private volatile long periodSeconds = 3600; // 1 real hour = 1 simulated day (real-time)
    private final AtomicInteger day = new AtomicInteger(1); // Start from day 1

    public Clock(EventBus bus) {
        this.bus = bus;
    }

    public void start() {
        // Log and publish initial day 1
        int initialDay = day.get();
        Logger.log(Logger.LogLevel.INFO, "DayTick " + initialDay);
        bus.publish(EventBus.Topic.DAY_TICK, initialDay);
        
        // Schedule subsequent day ticks (starting from day 2 after 1 hour)
        exec.scheduleAtFixedRate(() -> {
            int d = day.incrementAndGet();
            Logger.log(Logger.LogLevel.INFO, "DayTick " + d);
            bus.publish(EventBus.Topic.DAY_TICK, d);
        }, periodSeconds, periodSeconds, TimeUnit.SECONDS);
        Logger.log(Logger.LogLevel.INFO, "Clock started: 1 tick per " + periodSeconds + " sec (first tick after " + periodSeconds + " sec)");
    }

    public void stop() { exec.shutdownNow(); }

    /** For development: use smaller seconds-per-day. */
    public void setScaleSecondsPerDay(long seconds) {
        this.periodSeconds = Math.max(1, seconds);
    }

    public int currentDay() { return day.get(); }
}
