package com.garden.sim.core.sprinklers;

import com.garden.sim.core.Plant;
import com.garden.sim.core.logger.Logger;

/**
 * Represents a sprinkler that can water plants.
 * Each sprinkler can irrigate a specific plant when activated.
 */
public class Sprinkler {
    private final String name;
    private final Plant targetPlant;
    private static final int WATER_AMOUNT = 10;
    private boolean isActive = true;
    
    public Sprinkler(String name, Plant targetPlant) {
        this.name = name;
        this.targetPlant = targetPlant;
    }
    
    /**
     * Gets the sprinkler name.
     * @return The sprinkler name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the target plant for this sprinkler.
     * @return The plant being watered
     */
    public Plant getTargetPlant() {
        return targetPlant;
    }
    
    /**
     * Activates the sprinkler to water the target plant.
     * @return true if watering was successful
     */
    public boolean activate() {
        if (!isActive || targetPlant.isDead()) {
            return false;
        }
        
        int before = targetPlant.getSoilMoisture();
        targetPlant.rain(WATER_AMOUNT);
        int after = targetPlant.getSoilMoisture();
        
        Logger.log(Logger.LogLevel.INFO, 
            "Sprinkler " + name + " activated: watered " + targetPlant.getName() + 
            " (moisture: " + before + "% -> " + after + "%)");
        
        return true;
    }
    
    /**
     * Checks if the sprinkler is active.
     * @return true if active
     */
    public boolean isActive() {
        return isActive && !targetPlant.isDead();
    }
    
    /**
     * Sets the active state of the sprinkler.
     * @param active The active state
     */
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    /**
     * Gets the water amount dispensed per activation.
     * @return Water amount in units
     */
    public int getWaterAmount() {
        return WATER_AMOUNT;
    }
}

