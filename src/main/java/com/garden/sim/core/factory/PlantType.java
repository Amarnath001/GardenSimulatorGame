package com.garden.sim.core.factory;

/**
 * Enum representing different types of plants, each associated with a unique emoji and harvest time.
 * Similar to the oops_project design pattern.
 */
public enum PlantType {
    TOMATO("🍅", 5),        // 5 days to harvest
    ROSE("🌹", 7),          // 7 days to harvest
    BASIL("🌿", 3),         // 3 days to harvest (fast growing)
    PEPPER("🌶️", 6),       // 6 days to harvest
    CUCUMBER("🥒", 4),     // 4 days to harvest
    LETTUCE("🥬", 3),      // 3 days to harvest (fast growing)
    CARROT("🥕", 5),       // 5 days to harvest
    STRAWBERRY("🍓", 6),   // 6 days to harvest
    SUNFLOWER("🌻", 8),    // 8 days to harvest (slow growing)
    MARIGOLD("🌼", 4);     // 4 days to harvest

    private final String emoji;
    private final int daysToHarvest;

    /**
     * Constructor to associate an emoji and harvest time with a PlantType.
     *
     * @param emoji The emoji representing the plant type.
     * @param daysToHarvest The number of days required for the plant to be ready for harvest.
     */
    PlantType(String emoji, int daysToHarvest) {
        this.emoji = emoji;
        this.daysToHarvest = daysToHarvest;
    }

    /**
     * Retrieves the emoji associated with the plant type.
     *
     * @return The emoji as a String.
     */
    public String getEmoji() {
        return emoji;
    }
    
    /**
     * Retrieves the number of days required for this plant type to be ready for harvest.
     *
     * @return The days to harvest as an int.
     */
    public int getDaysToHarvest() {
        return daysToHarvest;
    }
    
    /**
     * Get PlantType from string name (case-insensitive).
     */
    public static PlantType fromString(String name) {
        try {
            return PlantType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}




