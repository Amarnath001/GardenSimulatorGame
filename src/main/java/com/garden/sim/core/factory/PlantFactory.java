package com.garden.sim.core.factory;

import com.garden.sim.core.Plant;
import com.garden.sim.core.Species;
import java.util.List;

/**
 * Factory class responsible for creating instances of Plant subclasses based on PlantType.
 * Implements Factory Pattern similar to oops_project.
 * 
 * <p>This factory provides a centralized way to create Plant instances,
 * ensuring consistent initialization and type safety.
 */
public final class PlantFactory {
    
    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private PlantFactory() {
        throw new AssertionError("PlantFactory should not be instantiated");
    }

    /**
     * Creates and returns an instance of a Plant based on the provided PlantType.
     *
     * @param type                The type of plant to create.
     * @param name                The name of the plant.
     * @param waterRequirement    The water requirement of the plant.
     * @param tempMin             The lower bound of temperature tolerance.
     * @param tempMax             The upper bound of temperature tolerance.
     * @param parasiteVulns       List of parasites that the plant is vulnerable to.
     * @return An instance of Plant.
     * @throws IllegalArgumentException if the PlantType is unsupported or parameters are invalid.
     */
    public static Plant createPlant(
            PlantType type,
            String name,
            int waterRequirement,
            int tempMin,
            int tempMax,
            List<String> parasiteVulns
    ) {
        if (type == null) {
            throw new IllegalArgumentException("PlantType cannot be null");
        }

        // Create Species object
        Species species = new Species(
            type.name(),
            waterRequirement,
            tempMin,
            tempMax,
            parasiteVulns
        );

        // Create and return Plant with the species
        return new Plant(name, species);
    }

    /**
     * Creates a plant from a Species object (for backward compatibility).
     */
    public static Plant createPlantFromSpecies(String name, Species species) {
        PlantType type = PlantType.fromString(species.getName());
        if (type == null) {
            // Fallback: create plant directly
            return new Plant(name, species);
        }
        return createPlant(type, name, 
            species.getDailyWaterNeed(),
            species.getTempMin(),
            species.getTempMax(),
            species.getParasiteVulns());
    }
}

