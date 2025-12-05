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
        
        bus.subscribe(EventBus.Topic.DAY_TICK, e -> onDayTick());
        bus.subscribe(EventBus.Topic.RAIN, e -> Logger.log(Logger.LogLevel.INFO, "External rain event observed by WateringSystem"));
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
        int watered = 0;
        int totalChecked = 0;
        
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
            
            // Use sensor to check if watering is needed
            if (sensor.isMoistureLow()) {
                Sprinkler sprinkler = sprinklers.get(plant);
                if (sprinkler != null && sprinkler.activate()) {
                    watered++;
                }
            }
        }
        
        if (watered > 0) {
            Logger.log(Logger.LogLevel.INFO, "WateringSystem: Activated " + watered + " sprinklers to irrigate " + watered + " of " + totalChecked + " plants");
        }
    }
}

