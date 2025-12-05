package com.garden.sim.core.sensors;

/**
 * Sensor that monitors ambient temperature in the garden.
 * Used by the HeatingSystem to determine when heating is needed.
 */
public class TemperatureSensor implements Sensor {
    private final String name;
    private int currentTemperature;
    private static final int MIN_SAFE_TEMPERATURE = 60;
    
    public TemperatureSensor(String name) {
        this.name = name;
        this.currentTemperature = 72; // Default temperature
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public int readValue() {
        return currentTemperature;
    }
    
    @Override
    public boolean isActive() {
        return true; // Temperature sensor is always active
    }
    
    /**
     * Updates the temperature reading.
     * @param temperature The new temperature in Fahrenheit
     */
    public void updateTemperature(int temperature) {
        this.currentTemperature = temperature;
    }
    
    /**
     * Checks if the temperature is below the minimum safe threshold.
     * @return true if heating is needed
     */
    public boolean isTemperatureLow() {
        return currentTemperature < MIN_SAFE_TEMPERATURE;
    }
    
    /**
     * Gets the minimum safe temperature threshold.
     * @return The threshold value
     */
    public int getMinSafeTemperature() {
        return MIN_SAFE_TEMPERATURE;
    }
}

