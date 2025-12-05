package com.garden.sim.modules;

import com.garden.sim.core.logger.Logger;
import com.garden.sim.core.sensors.TemperatureSensor;

/**
 * Heating System module that uses temperature sensors
 * to monitor and regulate garden temperature.
 */
public class HeatingSystem {
    private final TemperatureSensor temperatureSensor;
    private int targetMin = 60;

    public HeatingSystem() {
        this.temperatureSensor = new TemperatureSensor("GardenTemperatureSensor");
        Logger.log(Logger.LogLevel.INFO, "HeatingSystem: Initialized with temperature sensor " + temperatureSensor.getName());
    }
    
    /**
     * Gets the temperature sensor used by this heating system.
     * @return The temperature sensor
     */
    public TemperatureSensor getTemperatureSensor() {
        return temperatureSensor;
    }

    public int mitigate(int currentF) {
        // Update sensor reading
        temperatureSensor.updateTemperature(currentF);
        
        // Check sensor reading
        if (temperatureSensor.isTemperatureLow()) {
            int lift = Math.min(10, targetMin - currentF);
            int newTemp = currentF + lift;
            Logger.log(Logger.LogLevel.INFO, 
                "HeatingSystem: Temperature sensor " + temperatureSensor.getName() + 
                " detected low temperature (" + currentF + "°F, minimum: " + targetMin + "°F). " +
                "Heating system activated and raised temperature to " + newTemp + "°F");
            temperatureSensor.updateTemperature(newTemp);
            return newTemp;
        }
        return currentF;
    }
}
