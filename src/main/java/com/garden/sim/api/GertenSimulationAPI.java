package com.garden.sim.api;

import java.util.Map;

/**
 * API interface for the Garden Simulation system.
 * Provides methods to interact with the garden simulation.
 */
public interface GertenSimulationAPI {
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

    /** Dump final state to log/UI; called after 24 simulated days by grader. */
    void getState();
}
