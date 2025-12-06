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
 * 
 * <p>This class is thread-safe and provides proper resource management.
 * Call {@link #shutdown()} when done to clean up resources.
 */
public final class GardenSimulationImpl implements GardenSimulationAPI {
    private static final int RANDOM_TEMP_MIN = 50;
    private static final int RANDOM_TEMP_MAX = 80;
    private static final int RANDOM_TEMP_RANGE = RANDOM_TEMP_MAX - RANDOM_TEMP_MIN + 1;
    
    private final Garden garden;
    private final EventBus bus;
    private final Clock clock;
    private final ScheduledExecutorService svc = Executors.newSingleThreadScheduledExecutor();
    private final PestControl pest;
    private final HeatingSystem heating;
    @SuppressWarnings("unused") // Used via event bus subscription
    private final WateringSystem watering;

    public GardenSimulationImpl() {
        this.bus = new EventBus();
        this.garden = new Garden(bus);
        this.clock = new Clock(bus);
        this.watering = new WateringSystem(garden, bus);
        this.heating = new HeatingSystem();
        this.pest = new PestControl(garden);
        
        // Subscribe to day ticks to generate random temperature
        bus.subscribe(EventBus.Topic.DAY_TICK, e -> {
            // Generate random temperature between RANDOM_TEMP_MIN-RANDOM_TEMP_MAX°F each day
            java.util.Random random = new java.util.Random();
            int randomTemp = RANDOM_TEMP_MIN + random.nextInt(RANDOM_TEMP_RANGE);
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
    public boolean infectPlant(String plantName, String parasiteName) {
        return safe(() -> {
            boolean result = garden.infectPlant(plantName, parasiteName);
            if (result) {
                bus.publish(EventBus.Topic.PARASITE, parasiteName);
            }
            return result;
        }, false);
    }
    
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
    
    private static final int DEFAULT_SEED_PRICE = 10;
    
    @Override
    public int getSeedPrice(String species) {
        if (species == null || species.trim().isEmpty()) {
            return DEFAULT_SEED_PRICE;
        }
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
            default -> DEFAULT_SEED_PRICE;
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
    
    /**
     * Shuts down the simulation and releases all resources.
     * Should be called when the simulation is no longer needed.
     */
    public void shutdown() {
        safe(() -> {
            svc.shutdown();
            clock.stop();
            Logger.log(Logger.LogLevel.INFO, "Simulation shutdown completed");
        });
    }

    private void safe(Runnable r) {
        try { 
            r.run(); 
        } catch (Throwable t) { 
            Logger.log(Logger.LogLevel.ERROR, "Unhandled exception: " + t.getMessage());
            if (t.getCause() != null) {
                Logger.log(Logger.LogLevel.ERROR, "Caused by: " + t.getCause().getMessage());
            }
        }
    }
    
    private <T> T safe(java.util.function.Supplier<T> supplier, T defaultValue) {
        try { 
            return supplier.get(); 
        } catch (Throwable t) { 
            Logger.log(Logger.LogLevel.ERROR, "Unhandled exception: " + t.getMessage());
            if (t.getCause() != null) {
                Logger.log(Logger.LogLevel.ERROR, "Caused by: " + t.getCause().getMessage());
            }
            return defaultValue;
        }
    }
}
