package com.garden.sim.modules;

import com.garden.sim.core.*;
import com.garden.sim.core.logger.Logger;
import com.garden.sim.core.sensors.ParasiteSensor;
import java.util.*;

/**
 * Pest Control module that uses parasite sensors
 * to detect and treat parasite infestations on plants.
 */
public class PestControl {
    private final Garden garden;
    private final Random rnd = new Random();
    
    // Parasite sensors for each plant
    private final Map<Plant, ParasiteSensor> sensors = new HashMap<>();

    public PestControl(Garden garden) {
        this.garden = garden;
        initializeSensors();
        
        // Subscribe to plant additions to add sensors dynamically
        // Note: We need EventBus access, but PestControl doesn't have it
        // For now, sensors will be added during dailySweep if missing
    }
    
    /**
     * Initializes parasite sensors for all plants in the garden.
     */
    private void initializeSensors() {
        for (Plant plant : garden.getPlantsList()) {
            sensors.put(plant, new ParasiteSensor(plant));
        }
        Logger.log(Logger.LogLevel.INFO, "PestControl: Initialized " + sensors.size() + " parasite sensors");
    }
    
    /**
     * Adds a parasite sensor for a new plant.
     */
    private void addSensor(Plant plant) {
        sensors.put(plant, new ParasiteSensor(plant));
    }
    
    /**
     * Removes sensor for a harvested/removed plant.
     */
    private void removeSensor(Plant plant) {
        sensors.remove(plant);
    }

    public void dailySweep() {
        int cures = 0;
        int totalInfestations = 0;
        
        // Update sensors for any new plants
        Set<Plant> currentPlants = new HashSet<>(garden.getPlantsList());
        for (Plant plant : currentPlants) {
            if (!sensors.containsKey(plant)) {
                addSensor(plant);
            }
        }
        
        // Check each plant using its parasite sensor
        for (Plant p : garden.getPlantsList()) {
            if (p.isDead()) {
                removeSensor(p);
                continue;
            }
            
            ParasiteSensor sensor = sensors.get(p);
            if (sensor == null) {
                addSensor(p);
                sensor = sensors.get(p);
            }
            
            // Use sensor to detect parasites
            if (sensor.hasParasites()) {
                Set<String> parasites = new HashSet<>(sensor.getDetectedParasites());
                totalInfestations += parasites.size();
                
                for (String parasite : parasites) {
                    int eff = 50 + rnd.nextInt(51); // 50..100
                    boolean wasCured = p.getParasites().contains(parasite);
                    p.cure(parasite, eff);
                    boolean isCured = !p.getParasites().contains(parasite);
                    
                    if (isCured && wasCured) {
                        cures++;
                        if (eff >= 100) {
                            Logger.log(Logger.LogLevel.INFO, 
                                "PestControl: Parasite sensor " + sensor.getName() + 
                                " detected " + parasite + " on " + p.getName() + 
                                ". Treatment applied (efficacy: " + eff + "%) - guaranteed cure (100% efficacy)");
                        } else {
                            Logger.log(Logger.LogLevel.INFO, 
                                "PestControl: Parasite sensor " + sensor.getName() + 
                                " detected " + parasite + " on " + p.getName() + 
                                ". Treatment applied (efficacy: " + eff + "%) - probabilistic cure succeeded (60% chance at " + eff + "% efficacy)");
                        }
                    } else if (wasCured && !isCured) {
                        Logger.log(Logger.LogLevel.INFO, 
                            "PestControl: Parasite sensor " + sensor.getName() + 
                            " detected " + parasite + " on " + p.getName() + 
                            ". Treatment applied (efficacy: " + eff + "%) - probabilistic cure failed (parasite resisted, 40% failure chance at " + eff + "% efficacy)");
                    }
                }
            }
        }
        
        if (totalInfestations > 0) {
            double successRate = totalInfestations > 0 ? (100.0 * cures / totalInfestations) : 0;
            Logger.log(Logger.LogLevel.INFO, 
                "PestControl: Parasite sensors detected " + totalInfestations + " infestation(s), " +
                "treated and cured " + cures + " (success rate: " + 
                String.format("%.1f", successRate) + "%). " +
                "Note: Treatment is probabilistic - efficacy 50-99% has 60% cure chance, 100% efficacy is guaranteed.");
        } else {
            Logger.log(Logger.LogLevel.INFO, 
                "PestControl: No infestations detected - all plants are parasite-free");
        }
    }
}
