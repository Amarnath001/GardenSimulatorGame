package com.garden.sim.core;

import java.util.*;

/**
 * Represents a plant in the garden simulation.
 * Tracks plant health, moisture, temperature stress, and parasite infestations.
 */
public class Plant {
    private final String name;
    private final Species species;

    private int soilMoisture = 50;
    private int health = 100;
    private int temperatureStress = 0;
    private final Set<String> parasites = new HashSet<>();

    public Plant(String name, Species species) {
        this.name = name;
        this.species = species;
    }

    public String getName() { return name; }
    public Species getSpecies() { return species; }
    public int getSoilMoisture() { return soilMoisture; }
    public int getHealth() { return health; }
    public int getTemperatureStress() { return temperatureStress; }
    public Set<String> getParasites() { return Collections.unmodifiableSet(parasites); }

    public boolean isDead() { return health <= 0; }

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

    public void infest(String parasite) {
        if (species.getParasiteVulns().contains(parasite)) parasites.add(parasite);
    }

    public void cure(String parasite, int efficacy) {
        if (parasites.contains(parasite) && efficacy >= 100) parasites.remove(parasite);
        else if (parasites.contains(parasite) && efficacy >= 50 && Math.random() < 0.6) parasites.remove(parasite);
    }

    public void dailyParasiteDamage() {
        if (!parasites.isEmpty()) {
            health = Math.max(0, health - 4);
        }
    }
}
