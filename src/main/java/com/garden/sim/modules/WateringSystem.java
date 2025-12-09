package com.garden.sim.modules;

import com.garden.sim.core.*;
import com.garden.sim.core.logger.Logger;
import com.garden.sim.core.sensors.MoistureSensor;
import com.garden.sim.core.sprinklers.Sprinkler;
import java.util.*;

/**
 * Watering System module that uses moisture sensors and sprinklers
 * to automatically irrigate plants when soil moisture is low.
 */
public class WateringSystem {
    private final Garden garden;
    @SuppressWarnings("unused") // Used via event bus subscription
    private final EventBus bus;
    
    // Sensors and sprinklers for each plant
    private final Map<Plant, MoistureSensor> sensors = new HashMap<>();
    private final Map<Plant, Sprinkler> sprinklers = new HashMap<>();

    public WateringSystem(Garden garden, EventBus bus) {
        this.garden = garden; 
        this.bus = bus;
        
        // Initialize sensors and sprinklers for existing plants
        initializeSensorsAndSprinklers();
        
        // Subscribe to DAY_TICK_COMPLETE instead of DAY_TICK to ensure we run AFTER moisture decay
        bus.subscribe(EventBus.Topic.DAY_TICK_COMPLETE, e -> onDayTick());
        // Also check after rain events to see if more watering is needed
        bus.subscribe(EventBus.Topic.RAIN, e -> {
            Logger.log(Logger.LogLevel.INFO, "External rain event observed by WateringSystem");
            // Check if plants still need watering after rain (in case rain wasn't enough)
            checkAndWater();
        });
        bus.subscribe(EventBus.Topic.PLANT_ADDED, e -> {
            if (e instanceof Plant) {
                addPlant((Plant) e);
            }
        });
        bus.subscribe(EventBus.Topic.PLANT_REMOVED, e -> {
            if (e instanceof Plant) {
                removePlant((Plant) e);
            }
        });
    }
    
    /**
     * Initializes moisture sensors and sprinklers for all plants in the garden.
     */
    private void initializeSensorsAndSprinklers() {
        for (Plant plant : garden.getPlantsList()) {
            MoistureSensor sensor = new MoistureSensor(plant);
            Sprinkler sprinkler = new Sprinkler("Sprinkler-" + plant.getName(), plant);
            sensors.put(plant, sensor);
            sprinklers.put(plant, sprinkler);
        }
        Logger.log(Logger.LogLevel.INFO, "WateringSystem: Initialized " + sensors.size() + " moisture sensors and " + sprinklers.size() + " sprinklers");
    }
    
    /**
     * Adds a sensor and sprinkler for a new plant.
     */
    public void addPlant(Plant plant) {
        MoistureSensor sensor = new MoistureSensor(plant);
        Sprinkler sprinkler = new Sprinkler("Sprinkler-" + plant.getName(), plant);
        sensors.put(plant, sensor);
        sprinklers.put(plant, sprinkler);
        Logger.log(Logger.LogLevel.INFO, "WateringSystem: Added sensor and sprinkler for " + plant.getName());
    }
    
    /**
     * Removes sensor and sprinkler for a harvested/removed plant.
     */
    public void removePlant(Plant plant) {
        sensors.remove(plant);
        sprinklers.remove(plant);
    }

    private void onDayTick() {
        checkAndWater();
    }
    
    /**
     * Checks all plants and activates sprinklers for those with low moisture.
     * This method can be called from day tick or after rain events.
     */
    private void checkAndWater() {
        int watered = 0;
        int totalChecked = 0;
        int lowMoistureCount = 0;
        
        // Update sensors and sprinklers for any new plants
        Set<Plant> currentPlants = new HashSet<>(garden.getPlantsList());
        for (Plant plant : currentPlants) {
            if (!sensors.containsKey(plant)) {
                addPlant(plant);
            }
        }
        
        // Check each plant using its moisture sensor
        for (Plant plant : garden.getPlantsList()) {
            if (plant.isDead()) {
                removePlant(plant);
                continue;
            }
            
            MoistureSensor sensor = sensors.get(plant);
            if (sensor == null) {
                addPlant(plant);
                sensor = sensors.get(plant);
            }
            
            totalChecked++;
            int moisture = sensor.readValue();
            
            // Use sensor to check if watering is needed
            if (sensor.isMoistureLow()) {
                lowMoistureCount++;
                Sprinkler sprinkler = sprinklers.get(plant);
                if (sprinkler != null && sprinkler.activate()) {
                    watered++;
                    int afterMoisture = plant.getSoilMoisture();
                    Logger.log(Logger.LogLevel.INFO, "WateringSystem: Sprinkler activated for " + plant.getName() + 
                             " (moisture was " + moisture + "%, threshold: " + sensor.getLowMoistureThreshold() + "%)");
                    // Publish event for UI to display
                    bus.publish(EventBus.Topic.SPRINKLER_ACTIVATED, Map.of(
                        "plant", plant.getName(),
                        "before", moisture,
                        "after", afterMoisture
                    ));
                } else {
                    Logger.log(Logger.LogLevel.WARNING, "WateringSystem: Failed to activate sprinkler for " + plant.getName());
                }
            }
        }
        
        if (watered > 0) {
            String summary = "WateringSystem: Activated " + watered + " sprinkler(s) to irrigate " + watered + " of " + totalChecked + " plant(s)";
            Logger.log(Logger.LogLevel.INFO, summary);
            // Publish summary for UI (only if we watered multiple plants, individual ones already published)
            if (watered > 1) {
                bus.publish(EventBus.Topic.SPRINKLER_ACTIVATED, Map.of("summary", summary, "count", watered));
            }
        } else if (lowMoistureCount > 0) {
            Logger.log(Logger.LogLevel.WARNING, "WateringSystem: Detected " + lowMoistureCount + " plant(s) with low moisture but no sprinklers activated");
        } else if (totalChecked > 0) {
            Logger.log(Logger.LogLevel.DEBUG, "WateringSystem: Checked " + totalChecked + " plant(s) - all have adequate moisture (>= " + 
                     (sensors.isEmpty() ? 50 : sensors.values().iterator().next().getLowMoistureThreshold()) + "%)");
        }
    }
}

