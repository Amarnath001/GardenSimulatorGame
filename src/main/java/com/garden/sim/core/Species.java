package com.garden.sim.core;

import java.util.*;

/**
 * Represents a plant species with its characteristics:
 * water requirements, temperature tolerance, and parasite vulnerabilities.
 * 
 * <p>This is an immutable value object. All fields are final and set at construction.
 */
public class Species {
    private final String name;
    private final int dailyWaterNeed;
    private final int tempMin;
    private final int tempMax;
    private final List<String> parasiteVulns;

    /**
     * Creates a new Species instance.
     * @param name The species name
     * @param dailyWaterNeed Daily water requirement in units
     * @param tempMin Minimum safe temperature in Fahrenheit
     * @param tempMax Maximum safe temperature in Fahrenheit
     * @param parasiteVulns List of parasite names this species is vulnerable to
     * @throws IllegalArgumentException if name is null or empty, or if tempMin > tempMax
     */
    public Species(String name, int dailyWaterNeed, int tempMin, int tempMax, List<String> parasiteVulns) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Species name cannot be null or empty");
        }
        if (tempMin > tempMax) {
            throw new IllegalArgumentException("tempMin (" + tempMin + ") cannot be greater than tempMax (" + tempMax + ")");
        }
        if (dailyWaterNeed < 0) {
            throw new IllegalArgumentException("dailyWaterNeed cannot be negative: " + dailyWaterNeed);
        }
        this.name = name;
        this.dailyWaterNeed = dailyWaterNeed;
        this.tempMin = tempMin;
        this.tempMax = tempMax;
        this.parasiteVulns = parasiteVulns != null ? List.copyOf(parasiteVulns) : List.of();
    }

    public String getName() { return name; }
    public int getDailyWaterNeed() { return dailyWaterNeed; }
    public int getTempMin() { return tempMin; }
    public int getTempMax() { return tempMax; }
    public List<String> getParasiteVulns() { return parasiteVulns; }
}
