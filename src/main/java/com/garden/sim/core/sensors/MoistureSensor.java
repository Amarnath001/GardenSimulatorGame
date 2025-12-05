package com.garden.sim.core.sensors;

import com.garden.sim.core.Plant;

/**
 * Sensor that monitors soil moisture levels for plants.
 * Used by the WateringSystem to determine when irrigation is needed.
 */
public class MoistureSensor implements Sensor {
    private final Plant plant;
    private final String name;
    private static final int LOW_MOISTURE_THRESHOLD = 30;
    
    public MoistureSensor(Plant plant) {
        this.plant = plant;
        this.name = "MoistureSensor-" + plant.getName();
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public int readValue() {
        return plant.getSoilMoisture();
    }
    
    @Override
    public boolean isActive() {
        return !plant.isDead();
    }
    
    /**
     * Checks if the soil moisture is below the threshold.
     * @return true if moisture is low and watering is needed
     */
    public boolean isMoistureLow() {
        return isActive() && readValue() < LOW_MOISTURE_THRESHOLD;
    }
    
    /**
     * Gets the low moisture threshold.
     * @return The threshold value
     */
    public int getLowMoistureThreshold() {
        return LOW_MOISTURE_THRESHOLD;
    }
}

