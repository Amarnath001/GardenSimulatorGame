package com.garden.sim.core.sensors;

import com.garden.sim.core.Plant;

/**
 * Sensor that detects parasite infestations on plants.
 * Used by the PestControl system to identify infested plants.
 */
public class ParasiteSensor implements Sensor {
    private final Plant plant;
    private final String name;
    
    public ParasiteSensor(Plant plant) {
        this.plant = plant;
        this.name = "ParasiteSensor-" + plant.getName();
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public int readValue() {
        // Returns the number of parasites detected
        return plant.getParasites().size();
    }
    
    @Override
    public boolean isActive() {
        return !plant.isDead();
    }
    
    /**
     * Checks if parasites are detected on the plant.
     * @return true if parasites are present
     */
    public boolean hasParasites() {
        return isActive() && readValue() > 0;
    }
    
    /**
     * Gets the set of parasite names detected.
     * @return Set of parasite names
     */
    public java.util.Set<String> getDetectedParasites() {
        return plant.getParasites();
    }
}

