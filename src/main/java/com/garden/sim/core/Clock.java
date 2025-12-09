package com.garden.sim.core;

import com.garden.sim.core.logger.Logger;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Clock manages the simulation's day tick cycle.
 * Each real-world hour represents one simulated day.
 * Thread-safe implementation using AtomicInteger for day counter.
 */
public class Clock {
    private static final long DEFAULT_PERIOD_SECONDS = 3600; // 1 real hour = 1 simulated day
    
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private final EventBus bus;
    private volatile long periodSeconds = DEFAULT_PERIOD_SECONDS;
    private final AtomicInteger day = new AtomicInteger(1); // Start from day 1

    /**
     * Creates a new Clock instance.
     * @param bus The event bus to publish day tick events to
     */
    public Clock(EventBus bus) {
        if (bus == null) {
            throw new IllegalArgumentException("EventBus cannot be null");
        }
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

    /**
     * Stops the clock and shuts down the executor service.
     * Should be called when the simulation is shutting down.
     */
    public void stop() { 
        exec.shutdownNow(); 
    }

    /**
     * Sets the time scale for development/testing.
     * For example, setScaleSecondsPerDay(5) means 1 simulated day = 5 real seconds.
     * 
     * @param seconds The number of real seconds per simulated day (minimum 1)
     */
    public void setScaleSecondsPerDay(long seconds) {
        if (seconds < 1) {
            Logger.log(Logger.LogLevel.WARNING, "Clock: Invalid periodSeconds: " + seconds + ", using minimum 1");
            seconds = 1;
        }
        this.periodSeconds = seconds;
    }

    public int currentDay() { return day.get(); }
}
