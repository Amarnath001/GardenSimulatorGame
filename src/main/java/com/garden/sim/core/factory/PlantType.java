package com.garden.sim.core.factory;

/**
 * Enum representing different types of plants, each associated with a unique emoji.
 * Similar to the oops_project design pattern.
 */
public enum PlantType {
    TOMATO("🍅"),
    ROSE("🌹"),
    BASIL("🌿"),
    LAVENDER("💜"),
    PEPPER("🌶️"),
    CUCUMBER("🥒"),
    LETTUCE("🥬"),
    CARROT("🥕"),
    STRAWBERRY("🍓"),
    SUNFLOWER("🌻"),
    MARIGOLD("🌼");

    private final String emoji;

    /**
     * Constructor to associate an emoji with a PlantType.
     *
     * @param emoji The emoji representing the plant type.
     */
    PlantType(String emoji) {
        this.emoji = emoji;
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




