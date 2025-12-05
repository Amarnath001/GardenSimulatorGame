package com.garden.sim.core;

import java.util.*;

/**
 * Represents a plant species with its characteristics:
 * water requirements, temperature tolerance, and parasite vulnerabilities.
 */
public class Species {
    private final String name;
    private final int dailyWaterNeed;
    private final int tempMin;
    private final int tempMax;
    private final List<String> parasiteVulns;

    public Species(String name, int dailyWaterNeed, int tempMin, int tempMax, List<String> parasiteVulns) {
        this.name = name;
        this.dailyWaterNeed = dailyWaterNeed;
        this.tempMin = tempMin;
        this.tempMax = tempMax;
        this.parasiteVulns = List.copyOf(parasiteVulns);
    }

    public String getName() { return name; }
    public int getDailyWaterNeed() { return dailyWaterNeed; }
    public int getTempMin() { return tempMin; }
    public int getTempMax() { return tempMax; }
    public List<String> getParasiteVulns() { return parasiteVulns; }
}
