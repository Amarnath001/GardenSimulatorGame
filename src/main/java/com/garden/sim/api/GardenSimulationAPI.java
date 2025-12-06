package com.garden.sim.api;

import java.util.List;
import java.util.Map;

/**
 * API interface for the Garden Simulation system.
 * Provides methods to interact with the garden simulation.
 */
public interface GardenSimulationAPI {
    /** Starts the simulation: 1 real hour == 1 simulated day. */
    void initializeGarden();

    /** Returns a compact description of plants (names, waterRequirement, parasites). */
    Map<String, Object> getPlants();

    /** Simulated rain; amount units aligned with species waterRequirement units. */
    void rain(int amount);

    /** Set/announce the current day's temperature in Fahrenheit. */
    void temperature(int fahrenheit);

    /** Introduce a parasite by name (e.g., "aphid") into the garden. */
    void parasite(String name);
    
    /** Infect a specific plant by name with a parasite. */
    boolean infectPlant(String plantName, String parasiteName);
    
    /** Add a new plant to the garden when user plants a seed. */
    boolean addPlant(String plotKey, String plantName, String species, int waterRequirement, int tempMin, int tempMax, List<String> parasiteVulns, int seedPrice);
    
    /** Harvest a specific plant by name. Returns reward coins, or -1 if not found, -2 if not ready. */
    int harvestPlant(String plantName);
    
    /** Harvest all ready plants. Returns total coins earned. */
    int harvestAllReady();
    
    /** Get current coin balance. */
    int getCoins();
    
    /** Get seed price for a species. */
    int getSeedPrice(String species);

    /** Dump final state to log/UI; called after 24 simulated days by grader. */
    void getState();
}
