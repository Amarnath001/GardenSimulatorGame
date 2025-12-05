package com.garden.sim.modules;

import com.garden.sim.core.*;

public class WateringSystem {
    private static final int LOW_MOISTURE_THRESHOLD = 30;
    private static final int WATER_AMOUNT = 10;
    
    private final Garden garden;
    private final Logger log;
    @SuppressWarnings("unused") // Used via event bus subscription
    private final EventBus bus;

    public WateringSystem(Garden garden, Logger log, EventBus bus) {
        this.garden = garden; this.log = log; this.bus = bus;
        bus.subscribe(EventBus.Topic.DAY_TICK, e -> onDayTick());
        bus.subscribe(EventBus.Topic.RAIN, e -> log.info("External rain event observed by WateringSystem"));
    }

    private void onDayTick() {
        int watered = 0;
        int totalChecked = 0;
        for (Plant p : garden.getPlantsList()) {
            if (p.isDead()) continue;
            totalChecked++;
            if (p.getSoilMoisture() < LOW_MOISTURE_THRESHOLD) {
                int before = p.getSoilMoisture();
                p.rain(WATER_AMOUNT);
                watered++;
                log.info("WateringSystem: watered " + p.getName() + " (moisture: " + before + "% -> " + p.getSoilMoisture() + "%)");
            }
        }
        if (watered > 0) {
            log.info("WateringSystem: irrigated " + watered + " of " + totalChecked + " plants");
        }
    }
}

