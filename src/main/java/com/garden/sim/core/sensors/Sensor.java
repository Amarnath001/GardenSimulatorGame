package com.garden.sim.core.sensors;

/**
 * Base interface for all sensors in the garden simulation.
 * Sensors monitor various environmental conditions and plant states.
 */
public interface Sensor {
    /**
     * Gets the name/type of this sensor.
     * @return The sensor name
     */
    String getName();
    
    /**
     * Reads the current value from the sensor.
     * @return The sensor reading value
     */
    int readValue();
    
    /**
     * Checks if the sensor is active and functioning.
     * @return true if sensor is active
     */
    boolean isActive();
}

