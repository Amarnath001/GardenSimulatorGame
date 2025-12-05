package com.garden.sim.api;

import com.garden.sim.core.*;
import com.garden.sim.core.logger.Logger;
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
    private final EventBus bus;
    private final Clock clock;
    private final ScheduledExecutorService svc = Executors.newSingleThreadScheduledExecutor();
    private final PestControl pest;
    private final HeatingSystem heating;
    @SuppressWarnings("unused") // Used via event bus subscription
    private final WateringSystem watering;

    public GertenSimulationImpl() {
        this.bus = new EventBus();
        this.garden = new Garden(bus);
        this.clock = new Clock(bus);
        this.watering = new WateringSystem(garden, bus);
        this.heating = new HeatingSystem();
        this.pest = new PestControl(garden);
        
        // Subscribe to day ticks to generate random temperature
        bus.subscribe(EventBus.Topic.DAY_TICK, e -> {
            // Generate random temperature between 50-80°F each day
            java.util.Random random = new java.util.Random();
            int randomTemp = 50 + random.nextInt(31); // 50 + (0-30) = 50-80
            temperature(randomTemp); // This will go through heating system
        });
    }

    @Override public void initializeGarden() {
        safe(() -> {
            garden.seedFromConfig("config/garden.json");
            clock.start();
            // Dev mode: faster ticks. Uncomment to speed up: 1 day per 5 sec.
            // clock.setScaleSecondsPerDay(5);
            svc.scheduleAtFixedRate(() -> safe(() -> pest.dailySweep()), 1, 1, TimeUnit.HOURS);
            // Random parasite attacks: check every 5 minutes (300 seconds) with 15% chance
            svc.scheduleAtFixedRate(() -> safe(() -> garden.checkRandomParasiteAttack()), 300, 300, TimeUnit.SECONDS);
            Logger.log(Logger.LogLevel.INFO, "Simulation initialized; subsystems started.");
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
    
    /** Gets species information by name. */
    public com.garden.sim.core.Species getSpeciesInfo(String speciesName) {
        return garden.getSpeciesInfo(speciesName);
    }

    @Override public void rain(int amount) { safe(() -> { garden.onRain(amount); bus.publish(EventBus.Topic.RAIN, amount); }); }

    @Override public void temperature(int fahrenheit) {
        safe(() -> {
            int originalTemp = fahrenheit;
            int mitigated = heating.mitigate(fahrenheit);
            garden.onTemperature(mitigated);
            // Publish temperature event - UI will log it
            bus.publish(EventBus.Topic.TEMPERATURE, mitigated);
            // If heating was applied, also publish heating info for UI logging
            if (mitigated != originalTemp) {
                bus.publish(EventBus.Topic.HEATING_ACTIVATED, Map.of("original", originalTemp, "mitigated", mitigated));
            }
        });
    }

    @Override public void parasite(String name) { safe(() -> { garden.onParasite(name); bus.publish(EventBus.Topic.PARASITE, name); }); }
    
    @Override 
    public boolean addPlant(String plotKey, String plantName, String species, int waterRequirement, int tempMin, int tempMax, List<String> parasiteVulns, int seedPrice) {
        return safe(() -> garden.addPlant(plotKey, plantName, species, waterRequirement, tempMin, tempMax, parasiteVulns, seedPrice), false);
    }
    
    @Override
    public int harvestPlant(String plantName) {
        return safe(() -> garden.harvestPlant(plantName), -1);
    }
    
    @Override
    public int harvestAllReady() {
        return safe(() -> garden.harvestAllReady(), 0);
    }
    
    @Override
    public int getCoins() {
        return safe(() -> garden.getCoins(), 0);
    }
    
    @Override
    public int getSeedPrice(String species) {
        // Seed prices for all 10 plant types
        return switch(species) {
            case "Tomato" -> 10;
            case "Rose" -> 15;
            case "Basil" -> 5;
            case "Pepper" -> 8;
            case "Cucumber" -> 10;
            case "Lettuce" -> 6;
            case "Carrot" -> 7;
            case "Strawberry" -> 12;
            case "Sunflower" -> 9;
            case "Marigold" -> 5;
            default -> 10;
        };
    }

    @Override public void getState() { safe(() -> garden.reportState()); }
    
    /** Manually triggers pest control to treat all infested plants. */
    public void triggerPestControl() {
        safe(() -> {
            pest.dailySweep();
            Logger.log(Logger.LogLevel.INFO, "Manual pest control triggered by user");
        });
    }

    private void safe(Runnable r) {
        try { r.run(); } catch (Throwable t) { 
            Logger.log(Logger.LogLevel.ERROR, "Unhandled exception: " + t.getMessage());
        }
    }
    
    private <T> T safe(java.util.function.Supplier<T> supplier, T defaultValue) {
        try { 
            return supplier.get(); 
        } catch (Throwable t) { 
            Logger.log(Logger.LogLevel.ERROR, "Unhandled exception: " + t.getMessage());
            return defaultValue;
        }
    }
}
