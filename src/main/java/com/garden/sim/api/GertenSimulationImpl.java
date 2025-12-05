package com.garden.sim.api;

import com.garden.sim.core.*;
import com.garden.sim.modules.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Implementation of the Garden Simulation API.
 * Manages the garden, clock, and all automated subsystems.
 * All methods are wrapped in exception handling to ensure the system never crashes.
 */
public final class GertenSimulationImpl implements GertenSimulationAPI {
    private final Garden garden;
    private final Logger log;
    private final EventBus bus;
    private final Clock clock;
    private final ScheduledExecutorService svc = Executors.newSingleThreadScheduledExecutor();
    private final PestControl pest;
    private final HeatingSystem heating;
    @SuppressWarnings("unused") // Used via event bus subscription
    private final WateringSystem watering;

    public GertenSimulationImpl() {
        this.log = new Logger("log.txt");
        this.bus = new EventBus();
        this.garden = new Garden(log, bus);
        this.clock = new Clock(bus, log);
        this.watering = new WateringSystem(garden, log, bus);
        this.heating = new HeatingSystem(log);
        this.pest = new PestControl(garden, log);
    }

    @Override public void initializeGarden() {
        safe(() -> {
            garden.seedFromConfig("config/garden.json");
            clock.start();
            // Dev mode: faster ticks. Uncomment to speed up: 1 day per 5 sec.
            // clock.setScaleSecondsPerDay(5);
            svc.scheduleAtFixedRate(() -> safe(() -> pest.dailySweep()), 1, 1, TimeUnit.HOURS);
            log.info("Simulation initialized; subsystems started.");
        });
    }

    @Override public Map<String, Object> getPlants() {
        return garden.exportPlantDefs();
    }

    /** Returns current plant states (health, moisture, parasites, etc.) */
    public Map<String, Object> getPlantStates() {
        return garden.exportPlantStates();
    }

    /** Returns the EventBus for subscribing to events. */
    public EventBus getEventBus() {
        return bus;
    }

    @Override public void rain(int amount) { safe(() -> { garden.onRain(amount); bus.publish(EventBus.Topic.RAIN, amount); }); }

    @Override public void temperature(int fahrenheit) {
        safe(() -> {
            int mitigated = heating.mitigate(fahrenheit);
            garden.onTemperature(mitigated);
            bus.publish(EventBus.Topic.TEMPERATURE, mitigated);
        });
    }

    @Override public void parasite(String name) { safe(() -> { garden.onParasite(name); bus.publish(EventBus.Topic.PARASITE, name); }); }

    @Override public void getState() { safe(() -> garden.reportState()); }

    private void safe(Runnable r) {
        try { r.run(); } catch (Throwable t) { log.error("Unhandled", t); }
    }
}
