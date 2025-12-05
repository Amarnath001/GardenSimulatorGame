package com.garden.sim.core;

import com.garden.sim.core.factory.PlantType;
import java.util.*;

/**
 * Represents a plant in the garden simulation.
 * Tracks plant health, moisture, temperature stress, parasite infestations, and growth.
 */
public class Plant {
    private final String name;
    private final Species species;

    private int soilMoisture = 50;
    private int health;
    private int temperatureStress = 0;
    private final Set<String> parasites = new HashSet<>();
    
    // Growth tracking
    private int daysPlanted = 0;
    private int daysToHarvest;
    private int growthStage = 0; // 0=seed, 1=sprout, 2=small, 3=medium, 4=ready

    public Plant(String name, Species species) {
        this.name = name;
        this.species = species;
        // Set random initial health between 60-80%
        this.health = 60 + (int)(Math.random() * 21); // 60 + (0-20) = 60-80
        
        // Get days to harvest from PlantType
        PlantType plantType = PlantType.fromString(species.getName());
        this.daysToHarvest = plantType != null ? plantType.getDaysToHarvest() : 5;
    }

    public String getName() { return name; }
    public Species getSpecies() { return species; }
    public int getSoilMoisture() { return soilMoisture; }
    public int getHealth() { return health; }
    public int getTemperatureStress() { return temperatureStress; }
    public Set<String> getParasites() { return Collections.unmodifiableSet(parasites); }
    public int getDaysPlanted() { return daysPlanted; }
    public int getDaysToHarvest() { return daysToHarvest; }
    public int getGrowthStage() { return growthStage; }
    public boolean isReadyToHarvest() { return daysPlanted >= daysToHarvest; }

    public boolean isDead() { return health <= 0; }
    
    /**
     * Advances the plant's growth by one day.
     * Called during day tick events.
     */
    public void advanceDay() {
        daysPlanted++;
        // Calculate growth stage based on days planted vs days to harvest
        // Stage 0: seed (day 0)
        // Stage 1: sprout (25% of harvest time)
        // Stage 2: small (50% of harvest time)
        // Stage 3: medium (75% of harvest time)
        // Stage 4: ready (100% of harvest time)
        if (daysPlanted >= daysToHarvest) {
            growthStage = 4; // Fully ready
        } else {
            double progress = (double) daysPlanted / daysToHarvest;
            if (progress >= 0.75) {
                growthStage = 3; // Medium
            } else if (progress >= 0.50) {
                growthStage = 2; // Small
            } else if (progress >= 0.25) {
                growthStage = 1; // Sprout
            } else {
                growthStage = 0; // Seed
            }
        }
    }
    
    /**
     * Calculates harvest reward based on health and maturity.
     * @return The coin reward for harvesting this plant
     */
    public int calculateHarvestReward() {
        boolean isFullyMature = daysPlanted >= daysToHarvest + 1; // Extra day = fully mature
        int baseReward = isFullyMature ? 30 : 20; // Fully mature = more coins
        int healthBonus = health > 80 ? 20 : health > 50 ? 10 : 5;
        return baseReward + healthBonus;
    }

    public void applyDailyWaterNeed() {
        soilMoisture = Math.max(0, soilMoisture - species.getDailyWaterNeed());
        if (soilMoisture == 0) {
            health = Math.max(0, health - 5);
        }
    }

    public void rain(int amount) {
        soilMoisture = Math.min(100, soilMoisture + amount);
    }

    public void setTemperature(int f) {
        int min = species.getTempMin(), max = species.getTempMax();
        if (f < min) temperatureStress += (min - f);
        else if (f > max) temperatureStress += (f - max);
        else temperatureStress = Math.max(0, temperatureStress - 2);
        if (temperatureStress > 20) {
            health = Math.max(0, health - 3);
        }
    }

    /**
     * Infests the plant with a parasite.
     * Parasites can attack any plant regardless of vulnerability.
     * 
     * @param parasite The parasite name to infest the plant with
     */
    public void infest(String parasite) {
        if (parasite != null && !parasite.isEmpty()) {
            parasites.add(parasite);
        }
    }

    public void cure(String parasite, int efficacy) {
        if (parasites.contains(parasite) && efficacy >= 100) parasites.remove(parasite);
        else if (parasites.contains(parasite) && efficacy >= 50 && Math.random() < 0.6) parasites.remove(parasite);
    }

    /**
     * Applies daily damage from parasites. Different parasites cause different amounts of damage.
     * Damage rates:
     * - aphid: 2 health per day (mild damage)
     * - spider_mite: 5 health per day (moderate damage)
     * - whitefly: 3 health per day (moderate damage)
     * - thrips: 6 health per day (severe damage)
     * - mite: 5 health per day (moderate damage, legacy support)
     * 
     * If multiple parasites are present, damage is cumulative.
     */
    public void dailyParasiteDamage() {
        if (parasites.isEmpty()) return;
        
        int totalDamage = 0;
        for (String parasite : parasites) {
            totalDamage += getParasiteDamageRate(parasite);
        }
        
        health = Math.max(0, health - totalDamage);
    }
    
    /**
     * Returns the daily damage rate for a specific parasite type.
     * 
     * @param parasite The parasite name
     * @return The damage amount per day for this parasite
     */
    private int getParasiteDamageRate(String parasite) {
        return switch (parasite.toLowerCase()) {
            case "aphid" -> 2;           // Mild damage
            case "spider_mite", "mite" -> 5;  // Moderate damage
            case "whitefly" -> 3;        // Moderate damage
            case "thrips" -> 6;         // Severe damage
            default -> 4;                // Default damage for unknown parasites
        };
    }
}
