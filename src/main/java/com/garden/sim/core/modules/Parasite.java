package com.garden.sim.core.modules;

import com.garden.sim.core.factory.PlantType;
import java.util.Map;

/**
 * Parasite/Insect class similar to oops_project Insect class.
 * Represents a parasite that can attack specific plant types with varying damage.
 */
public class Parasite {
    private final String name;
    private final String emoji;
    private final Map<String, Integer> damageMap; // PlantType name -> Damage amount

    /**
     * Creates a new Parasite.
     *
     * @param name      The name of the parasite (e.g., "aphid", "mite").
     * @param emoji     The emoji representation.
     * @param damageMap Map of plant type names to damage amounts.
     */
    public Parasite(String name, String emoji, Map<String, Integer> damageMap) {
        this.name = name;
        this.emoji = emoji;
        this.damageMap = damageMap;
    }

    /**
     * Gets the damage this parasite inflicts on a specific plant type.
     *
     * @param plantType The plant type to check damage for.
     * @return The damage amount, or 0 if this parasite doesn't affect this plant type.
     */
    public int getDamage(PlantType plantType) {
        if (plantType == null) return 0;
        return damageMap.getOrDefault(plantType.name(), 0);
    }

    /**
     * Gets the damage this parasite inflicts on a plant type by name.
     *
     * @param plantTypeName The plant type name (e.g., "ROSE", "TOMATO").
     * @return The damage amount, or 0 if this parasite doesn't affect this plant type.
     */
    public int getDamage(String plantTypeName) {
        return damageMap.getOrDefault(plantTypeName, 0);
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getEmoji() {
        return emoji;
    }

    public Map<String, Integer> getDamageMap() {
        return damageMap;
    }

    /**
     * Check if this parasite affects a specific plant type.
     */
    public boolean affects(PlantType plantType) {
        return getDamage(plantType) > 0;
    }

    @Override
    public String toString() {
        return name + " " + emoji;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Parasite parasite = (Parasite) obj;
        return name.equals(parasite.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}




