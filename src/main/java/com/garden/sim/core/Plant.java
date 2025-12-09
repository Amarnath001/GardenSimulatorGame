package com.garden.sim.core;

import com.garden.sim.core.factory.PlantType;
import java.util.*;

/**
 * Represents a plant in the garden simulation.
 * Tracks plant health, moisture, temperature stress, parasite infestations, and growth.
 * 
 * <p>This class is immutable in terms of name and species, but mutable in terms of
 * health, moisture, parasites, and growth state. Thread safety should be handled
 * by the containing Garden class.
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

    private static final int INITIAL_HEALTH_MIN = 60;
    private static final int INITIAL_HEALTH_MAX = 80;
    private static final int INITIAL_HEALTH_RANGE = INITIAL_HEALTH_MAX - INITIAL_HEALTH_MIN + 1;
    private static final int DEFAULT_DAYS_TO_HARVEST = 5;
    
    public Plant(String name, Species species) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Plant name cannot be null or empty");
        }
        if (species == null) {
            throw new IllegalArgumentException("Plant species cannot be null");
        }
        this.name = name;
        this.species = species;
        // Set random initial health between INITIAL_HEALTH_MIN-INITIAL_HEALTH_MAX%
        java.util.Random random = new java.util.Random();
        this.health = INITIAL_HEALTH_MIN + random.nextInt(INITIAL_HEALTH_RANGE);
        
        // Get days to harvest from PlantType
        PlantType plantType = PlantType.fromString(species.getName());
        this.daysToHarvest = plantType != null ? plantType.getDaysToHarvest() : DEFAULT_DAYS_TO_HARVEST;
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
    private static final int BASE_REWARD_MATURE = 30;
    private static final int BASE_REWARD_NORMAL = 20;
    private static final int HEALTH_BONUS_HIGH = 20;
    private static final int HEALTH_BONUS_MEDIUM = 10;
    private static final int HEALTH_BONUS_LOW = 5;
    private static final int HEALTH_THRESHOLD_HIGH = 80;
    private static final int HEALTH_THRESHOLD_MEDIUM = 50;
    
    public int calculateHarvestReward() {
        boolean isFullyMature = daysPlanted >= daysToHarvest + 1; // Extra day = fully mature
        int baseReward = isFullyMature ? BASE_REWARD_MATURE : BASE_REWARD_NORMAL;
        int healthBonus = health > HEALTH_THRESHOLD_HIGH ? HEALTH_BONUS_HIGH 
                         : health > HEALTH_THRESHOLD_MEDIUM ? HEALTH_BONUS_MEDIUM 
                         : HEALTH_BONUS_LOW;
        return baseReward + healthBonus;
    }

    private static final int HEALTH_DAMAGE_WHEN_DRY = 5;
    private static final int NATURAL_MOISTURE_DECAY = 5; // Natural evaporation/decay per day
    private static final int HEALTH_RECOVERY_RATE = 2; // Health recovery per day when conditions are good
    private static final int MOISTURE_THRESHOLD_FOR_RECOVERY = 40; // Minimum moisture for health recovery
    private static final int MAX_HEALTH = 100; // Maximum health cap
    
    /**
     * Applies daily water consumption and natural moisture decay.
     * Total daily moisture loss is 2% (natural evaporation only).
     * Plant water requirements are used for other calculations but don't directly reduce moisture.
     */
    public void applyDailyWaterNeed() {
        // Total daily moisture decay is 2% (natural evaporation)
        soilMoisture = Math.max(0, soilMoisture - NATURAL_MOISTURE_DECAY);
        
        // Note: Plant water requirements are still tracked for API/getPlants() but
        // don't directly reduce moisture here - only natural decay does
        
        if (soilMoisture == 0) {
            health = Math.max(0, health - HEALTH_DAMAGE_WHEN_DRY);
        }
    }
    
    /**
     * Recovers plant health when conditions are favorable.
     * Health recovers when:
     * - Moisture is adequate (above threshold)
     * - Temperature stress is low (within optimal range)
     * - No parasites are present
     * 
     * Recovery is gradual to prevent instant healing.
     */
    public void recoverHealth() {
        // Only recover if plant is alive and not at max health
        if (isDead() || health >= MAX_HEALTH) {
            return;
        }
        
        // Check conditions for recovery
        boolean hasAdequateMoisture = soilMoisture >= MOISTURE_THRESHOLD_FOR_RECOVERY;
        boolean hasLowTempStress = temperatureStress <= TEMPERATURE_STRESS_DAMAGE_THRESHOLD / 2; // Half of damage threshold
        boolean hasNoParasites = parasites.isEmpty();
        
        // All conditions must be met for recovery
        if (hasAdequateMoisture && hasLowTempStress && hasNoParasites) {
            health = Math.min(MAX_HEALTH, health + HEALTH_RECOVERY_RATE);
        }
    }

    public void rain(int amount) {
        soilMoisture = Math.min(100, soilMoisture + amount);
    }

    private static final int TEMPERATURE_STRESS_RECOVERY_RATE = 2;
    private static final int TEMPERATURE_STRESS_DAMAGE_THRESHOLD = 20;
    private static final int TEMPERATURE_STRESS_DAMAGE = 3;
    
    public void setTemperature(int f) {
        int min = species.getTempMin();
        int max = species.getTempMax();
        if (f < min) temperatureStress += (min - f);
        else if (f > max) temperatureStress += (f - max);
        else temperatureStress = Math.max(0, temperatureStress - TEMPERATURE_STRESS_RECOVERY_RATE);
        if (temperatureStress > TEMPERATURE_STRESS_DAMAGE_THRESHOLD) {
            health = Math.max(0, health - TEMPERATURE_STRESS_DAMAGE);
        }
    }

    /**
     * Infests the plant with a parasite.
     * Parasites can attack any plant regardless of vulnerability.
     * 
     * @param parasite The parasite name to infest the plant with
     */
    public void infest(String parasite) {
        if (parasite != null && !parasite.trim().isEmpty()) {
            parasites.add(parasite.trim());
        }
    }

    private static final int EFFICACY_THRESHOLD_GUARANTEED = 100;
    private static final int EFFICACY_THRESHOLD_CHANCE = 50;
    private static final double CURE_SUCCESS_CHANCE = 0.6;
    
    public void cure(String parasite, int efficacy) {
        if (parasite == null || !parasites.contains(parasite)) {
            return;
        }
        if (efficacy >= EFFICACY_THRESHOLD_GUARANTEED) {
            parasites.remove(parasite);
        } else if (efficacy >= EFFICACY_THRESHOLD_CHANCE) {
            java.util.Random random = new java.util.Random();
            if (random.nextDouble() < CURE_SUCCESS_CHANCE) {
                parasites.remove(parasite);
            }
        }
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
