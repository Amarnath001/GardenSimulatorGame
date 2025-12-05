package com.garden.sim.core;

import com.garden.sim.core.factory.PlantFactory;
import com.garden.sim.core.factory.PlantType;
import com.garden.sim.core.logger.Logger;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import com.garden.sim.core.orgjson.*;

/**
 * Core garden management class.
 * Manages plants, species definitions, coin economy, and plot assignments.
 * Thread-safe for read operations; write operations should be synchronized externally if needed.
 */
public class Garden {
    private static final int DEFAULT_TEMPERATURE_F = 72;
    private static final int INITIAL_COINS = 100;
    private static final int LOW_MOISTURE_THRESHOLD = 30;
    private static final int TEMPERATURE_STRESS_THRESHOLD = 20;
    private static final double RANDOM_PARASITE_ATTACK_CHANCE = 0.15;
    
    @SuppressWarnings("unused") // Used via event bus subscription
    private final EventBus bus;
    private final List<Plant> plants = new ArrayList<>();
    private int lastTemperatureF = DEFAULT_TEMPERATURE_F;
    
    // Species definitions loaded from config
    private final Map<String, Species> speciesByName = new HashMap<>();
    
    // Game state
    private int coins = INITIAL_COINS;
    private final Map<String, String> plotAssignments = new HashMap<>(); // "row,col" -> plantName

    /**
     * Creates a new Garden instance.
     * @param bus The event bus to subscribe to and publish events on
     * @throws IllegalArgumentException if bus is null
     */
    public Garden(EventBus bus) {
        if (bus == null) {
            throw new IllegalArgumentException("EventBus cannot be null");
        }
        this.bus = bus;
        // Subscribe to day ticks to update plants daily
        bus.subscribe(EventBus.Topic.DAY_TICK, e -> dayTick());
    }
    
    public int getCoins() { return coins; }
    
    public void deductCoins(int amount) {
        coins = Math.max(0, coins - amount);
    }
    
    public void addCoins(int amount) {
        coins += amount;
    }
    
    public void assignPlot(String plotKey, String plantName) {
        plotAssignments.put(plotKey, plantName);
    }
    
    public void unassignPlot(String plotKey) {
        plotAssignments.remove(plotKey);
    }
    
    public String getPlantAtPlot(String plotKey) {
        return plotAssignments.get(plotKey);
    }

    public void seedFromConfig(String classpathResource) {
        try (InputStream is = Garden.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (is == null) throw new IllegalArgumentException("Missing resource: " + classpathResource);
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(json);
            
            // Load species definitions
            for (Object sObj : root.getJSONArray("species")) {
                JSONObject s = (JSONObject) sObj;
                speciesByName.put(
                    s.getString("name"),
                    new Species(
                        s.getString("name"),
                        10, // default water need; overridden by plant waterRequirement
                        s.getInt("tempMin"),
                        s.getInt("tempMax"),
                        toList(s.optJSONArray("parasites"))
                    )
                );
            }
            // Load plants from config (if any) - plants are now created by users via UI
            JSONArray plantsArray = root.optJSONArray("plants");
            if (plantsArray != null) {
                for (Object pObj : plantsArray) {
                    JSONObject p = (JSONObject) pObj;
                    String speciesName = p.getString("species");
                    Species base = speciesByName.get(speciesName);
                    
                    // Use PlantFactory pattern (similar to oops_project)
                    PlantType plantType = PlantType.fromString(speciesName);
                    if (plantType != null) {
                        Plant plant = PlantFactory.createPlant(
                            plantType,
                            p.getString("name"),
                            p.getInt("waterRequirement"),
                            base.getTempMin(),
                            base.getTempMax(),
                            toList(p.optJSONArray("parasites"))
                        );
                        plants.add(plant);
                    } else {
                        // Fallback for unknown species
                        Species withWaterNeed = new Species(
                            base.getName(),
                            p.getInt("waterRequirement"),
                            base.getTempMin(),
                            base.getTempMax(),
                            toList(p.optJSONArray("parasites"))
                        );
                        plants.add(new Plant(p.getString("name"), withWaterNeed));
                    }
                }
            }
            Logger.log(Logger.LogLevel.INFO, "Loaded " + speciesByName.size() + " species definitions" + 
                      (plantsArray != null && plantsArray.length() > 0 ? " and " + plants.size() + " pre-planted plants" : "") + 
                      " from " + classpathResource);
        } catch (Exception e) {
            Logger.log(Logger.LogLevel.ERROR, "seedFromConfig failed: " + e.getMessage());
        }
    }

    private static List<String> toList(JSONArray arr) {
        if (arr == null) return List.of();
        List<String> out = new ArrayList<>();
        for (int i=0;i<arr.length();i++) out.add(arr.get(i).toString());
        return out;
    }

    public List<Plant> getPlantsList() { return Collections.unmodifiableList(plants); }
    
    /**
     * Adds a new plant to the garden.
     * Used when user plants a seed from the UI.
     * @param plotKey The plot key (e.g., "row,col")
     * @param plantName The name of the plant
     * @param speciesName The species name
     * @param waterRequirement Daily water requirement
     * @param tempMin Minimum temperature
     * @param tempMax Maximum temperature
     * @param parasiteVulns List of parasite vulnerabilities
     * @param seedPrice The price of the seed
     * @return true if plant was added successfully, false if insufficient coins or plot occupied
     */
    public boolean addPlant(String plotKey, String plantName, String speciesName, int waterRequirement, int tempMin, int tempMax, List<String> parasiteVulns, int seedPrice) {
        // Input validation
        if (plotKey == null || plotKey.trim().isEmpty()) {
            Logger.log(Logger.LogLevel.WARNING, "addPlant: Invalid plotKey");
            return false;
        }
        if (plantName == null || plantName.trim().isEmpty()) {
            Logger.log(Logger.LogLevel.WARNING, "addPlant: Invalid plantName");
            return false;
        }
        if (speciesName == null || speciesName.trim().isEmpty()) {
            Logger.log(Logger.LogLevel.WARNING, "addPlant: Invalid speciesName");
            return false;
        }
        if (seedPrice < 0) {
            Logger.log(Logger.LogLevel.WARNING, "addPlant: Invalid seedPrice: " + seedPrice);
            return false;
        }
        
        // Check if plot is already occupied
        if (plotAssignments.containsKey(plotKey)) {
            Logger.log(Logger.LogLevel.INFO, "addPlant: Plot " + plotKey + " is already occupied");
            return false; // Plot occupied
        }
        
        // Check if user has enough coins
        if (coins < seedPrice) {
            Logger.log(Logger.LogLevel.INFO, "addPlant: Insufficient coins. Have: " + coins + ", Need: " + seedPrice);
            return false; // Insufficient coins
        }
        
        // Deduct coins
        deductCoins(seedPrice);
        
        // Create and add plant
        PlantType plantType = PlantType.fromString(speciesName);
        Plant plant;
        if (plantType != null) {
            plant = PlantFactory.createPlant(
                plantType,
                plantName,
                waterRequirement,
                tempMin,
                tempMax,
                parasiteVulns
            );
        } else {
            // Fallback for unknown species
            Species species = new Species(speciesName, waterRequirement, tempMin, tempMax, parasiteVulns);
            plant = new Plant(plantName, species);
        }
        
        plants.add(plant);
        assignPlot(plotKey, plantName);
        
        // Notify modules about new plant (for sensor/sprinkler initialization)
        bus.publish(EventBus.Topic.PLANT_ADDED, plant);
        
        Logger.log(Logger.LogLevel.INFO, "Added plant " + plantName + " (" + speciesName + ") to garden at plot " + plotKey);
        return true;
    }

    /**
     * Exports plant definitions and species information.
     * Returns both existing plants and species templates for UI use.
     */
    public Map<String, Object> exportPlantDefs() {
        // Return existing plants (if any from config)
        List<String> names = plants.stream().map(Plant::getName).collect(Collectors.toList());
        List<Integer> water = plants.stream().map(p -> p.getSpecies().getDailyWaterNeed()).collect(Collectors.toList());
        List<List<String>> parasites = plants.stream()
            .map(p -> new ArrayList<>(p.getSpecies().getParasiteVulns())).collect(Collectors.toList());
        
        // Also return species information for UI to use when planting
        List<String> speciesNames = new ArrayList<>(speciesByName.keySet());
        List<Integer> speciesWaterDefaults = speciesNames.stream()
            .map(s -> speciesByName.get(s).getDailyWaterNeed())
            .collect(Collectors.toList());
        List<List<String>> speciesParasites = speciesNames.stream()
            .map(s -> new ArrayList<>(speciesByName.get(s).getParasiteVulns()))
            .collect(Collectors.toList());
        List<Integer> speciesTempMin = speciesNames.stream()
            .map(s -> speciesByName.get(s).getTempMin())
            .collect(Collectors.toList());
        List<Integer> speciesTempMax = speciesNames.stream()
            .map(s -> speciesByName.get(s).getTempMax())
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("plants", names);
        result.put("waterRequirement", water);
        result.put("parasites", parasites);
        result.put("species", speciesNames);
        result.put("speciesWaterDefaults", speciesWaterDefaults);
        result.put("speciesParasites", speciesParasites);
        result.put("speciesTempMin", speciesTempMin);
        result.put("speciesTempMax", speciesTempMax);
        return result;
    }
    
    /**
     * Gets species information by name.
     * @param speciesName The species name
     * @return Species object or null if not found
     */
    public Species getSpeciesInfo(String speciesName) {
        if (speciesName == null || speciesName.trim().isEmpty()) {
            Logger.log(Logger.LogLevel.WARNING, "getSpeciesInfo: Invalid speciesName");
            return null;
        }
        return speciesByName.get(speciesName);
    }

    public Map<String, Object> exportPlantStates() {
        List<String> names = new ArrayList<>();
        List<Integer> health = new ArrayList<>();
        List<Integer> moisture = new ArrayList<>();
        List<Integer> tempStress = new ArrayList<>();
        List<List<String>> activeParasites = new ArrayList<>();
        List<Boolean> isDead = new ArrayList<>();
        List<String> species = new ArrayList<>();
        List<Integer> daysPlanted = new ArrayList<>();
        List<Integer> growthStage = new ArrayList<>();
        List<Boolean> readyToHarvest = new ArrayList<>();

        for (Plant p : plants) {
            names.add(p.getName());
            health.add(p.getHealth());
            moisture.add(p.getSoilMoisture());
            tempStress.add(p.getTemperatureStress());
            activeParasites.add(new ArrayList<>(p.getParasites()));
            isDead.add(p.isDead());
            species.add(p.getSpecies().getName());
            daysPlanted.add(p.getDaysPlanted());
            growthStage.add(p.getGrowthStage());
            readyToHarvest.add(p.isReadyToHarvest());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("plants", names);
        result.put("health", health);
        result.put("moisture", moisture);
        result.put("tempStress", tempStress);
        result.put("activeParasites", activeParasites);
        result.put("isDead", isDead);
        result.put("species", species);
        result.put("daysPlanted", daysPlanted);
        result.put("growthStage", growthStage);
        result.put("readyToHarvest", readyToHarvest);
        result.put("coins", coins);
        return result;
    }
    
    /**
     * Harvests a plant and returns the reward.
     * @param plantName The name of the plant to harvest
     * @return The coin reward, or -1 if plant not found or not ready
     */
    public int harvestPlant(String plantName) {
        Plant plant = plants.stream()
            .filter(p -> p.getName().equals(plantName))
            .findFirst()
            .orElse(null);
        
        if (plant == null || plant.isDead()) {
            return -1; // Plant not found or dead
        }
        
        if (!plant.isReadyToHarvest()) {
            return -2; // Not ready to harvest
        }
        
        int reward = plant.calculateHarvestReward();
        addCoins(reward);
        
        // Remove plant from garden
        plants.remove(plant);
        
        // Unassign plot
        plotAssignments.entrySet().removeIf(e -> e.getValue().equals(plantName));
        
        // Notify modules about plant removal (for sensor/sprinkler cleanup)
        bus.publish(EventBus.Topic.PLANT_REMOVED, plant);
        
        Logger.log(Logger.LogLevel.INFO, "Harvested " + plantName + " - Earned " + reward + " coins");
        return reward;
    }
    
    /**
     * Harvests all ready plants and returns total reward.
     * @return Total coins earned
     */
    public int harvestAllReady() {
        List<Plant> readyPlants = new ArrayList<>();
        for (Plant p : plants) {
            if (!p.isDead() && p.isReadyToHarvest()) {
                readyPlants.add(p);
            }
        }
        
        if (readyPlants.isEmpty()) {
            return 0; // Nothing to harvest
        }
        
        int totalReward = 0;
        for (Plant p : readyPlants) {
            totalReward += p.calculateHarvestReward();
            plants.remove(p);
        }
        
        // Unassign plots for harvested plants and notify modules
        for (Plant p : readyPlants) {
            plotAssignments.entrySet().removeIf(e -> e.getValue().equals(p.getName()));
            bus.publish(EventBus.Topic.PLANT_REMOVED, p);
        }
        
        addCoins(totalReward);
        Logger.log(Logger.LogLevel.INFO, "Harvested " + readyPlants.size() + " plant(s) - Total earnings: " + totalReward + " coins");
        return totalReward;
    }

    public void startModules() {
        // modules subscribe via constructors
    }

    // Event handlers
    public void dayTick() {
        // Random temperature generation is handled by API layer to go through heating system
        
        int deadBefore = (int) plants.stream().filter(Plant::isDead).count();
        int lowMoistureCount = 0;
        int infestedCount = 0;
        int stressedCount = 0;
        
        for (Plant p : plants) {
            try {
                // Advance growth
                p.advanceDay();
                
                p.applyDailyWaterNeed();
                if (p.getSoilMoisture() < LOW_MOISTURE_THRESHOLD) lowMoistureCount++;
                
                if (!p.getParasites().isEmpty()) infestedCount++;
                p.dailyParasiteDamage();
                
                p.setTemperature(lastTemperatureF);
                if (p.getTemperatureStress() > TEMPERATURE_STRESS_THRESHOLD) stressedCount++;
            } catch (Throwable t) {
                Logger.log(Logger.LogLevel.ERROR, "dayTick plant error for " + p.getName() + ": " + t.getMessage());
            }
        }
        
        int deadAfter = (int) plants.stream().filter(Plant::isDead).count();
        int newlyDead = deadAfter - deadBefore;
        
        Logger.log(Logger.LogLevel.INFO, "DayTick applied to " + plants.size() + " plants | " +
                 "Low moisture: " + lowMoistureCount + " | " +
                 "Infested: " + infestedCount + " | " +
                 "Temp stressed: " + stressedCount + " | " +
                 "Newly dead: " + newlyDead);
    }
    
    /**
     * Checks for random parasite attacks on occupied plots.
     * Has a 15% chance to randomly attack occupied plots with a random parasite type.
     * This simulates natural pest infestations in the garden.
     */
    public void checkRandomParasiteAttack() {
        if (plants.isEmpty()) {
            return; // No plants to attack
        }
        
        Random random = new Random();
        
        if (random.nextDouble() >= RANDOM_PARASITE_ATTACK_CHANCE) {
            return; // No attack this time
        }
        
        // Random parasite types
        String[] parasiteTypes = {"aphid", "spider_mite", "whitefly", "thrips"};
        String randomParasiteType = parasiteTypes[random.nextInt(parasiteTypes.length)];
        
        // Apply parasite attack
        onParasite(randomParasiteType);
        
        Logger.log(Logger.LogLevel.WARNING, "Random parasite attack: " + randomParasiteType + " attacked garden");
    }

    public void onRain(int amount) {
        if (amount < 0) {
            Logger.log(Logger.LogLevel.WARNING, "onRain: Invalid rain amount: " + amount);
            return;
        }
        int watered = 0;
        for (Plant p : plants) {
            try { 
                int before = p.getSoilMoisture();
                p.rain(amount);
                if (p.getSoilMoisture() > before) watered++;
            } catch (Throwable t) { 
                Logger.log(Logger.LogLevel.ERROR, "rain error for " + p.getName() + ": " + t.getMessage()); 
            }
        }
        Logger.log(Logger.LogLevel.INFO, "Rain applied: amount=" + amount + " to " + plants.size() + " plants (watered: " + watered + ")");
    }

    public void onTemperature(int f) {
        // Temperature range validation (per API spec: 40-120 F)
        if (f < 40 || f > 120) {
            Logger.log(Logger.LogLevel.WARNING, "onTemperature: Temperature " + f + "F is outside valid range (40-120F)");
        }
        this.lastTemperatureF = f;
        Logger.log(Logger.LogLevel.INFO, "Temperature set to " + f + "F");
    }

    public void onParasite(String name) {
        if (name == null || name.trim().isEmpty()) {
            Logger.log(Logger.LogLevel.WARNING, "onParasite: Invalid parasite name");
            return;
        }
        int infested = 0;
        for (Plant p : plants) {
            try { 
                boolean wasInfested = !p.getParasites().isEmpty();
                p.infest(name);
                if (!p.getParasites().isEmpty() && !wasInfested) infested++;
            } catch (Throwable t) { 
                Logger.log(Logger.LogLevel.ERROR, "parasite error for " + p.getName() + ": " + t.getMessage()); 
            }
        }
        Logger.log(Logger.LogLevel.INFO, "Parasite introduced: " + name + " (affected " + infested + " plants)");
    }
    
    /**
     * Infects a specific plant by name with a parasite.
     * @param plantName The name of the plant to infect
     * @param parasiteName The parasite name to introduce
     * @return true if the plant was found and infected, false otherwise
     */
    public boolean infectPlant(String plantName, String parasiteName) {
        if (plantName == null || plantName.trim().isEmpty()) {
            Logger.log(Logger.LogLevel.WARNING, "infectPlant: Invalid plant name");
            return false;
        }
        if (parasiteName == null || parasiteName.trim().isEmpty()) {
            Logger.log(Logger.LogLevel.WARNING, "infectPlant: Invalid parasite name");
            return false;
        }
        
        Plant plant = plants.stream()
            .filter(p -> p.getName().equals(plantName))
            .findFirst()
            .orElse(null);
        
        if (plant == null) {
            Logger.log(Logger.LogLevel.WARNING, "infectPlant: Plant not found: " + plantName);
            return false;
        }
        
        if (plant.isDead()) {
            Logger.log(Logger.LogLevel.WARNING, "infectPlant: Cannot infect dead plant: " + plantName);
            return false;
        }
        
        boolean wasInfested = !plant.getParasites().isEmpty();
        plant.infest(parasiteName);
        boolean isNowInfested = !plant.getParasites().isEmpty();
        
        if (isNowInfested && !wasInfested) {
            Logger.log(Logger.LogLevel.INFO, "Parasite " + parasiteName + " introduced to plant " + plantName);
        } else if (isNowInfested) {
            Logger.log(Logger.LogLevel.INFO, "Parasite " + parasiteName + " added to plant " + plantName + " (already had other parasites)");
        }
        
        return true;
    }

    public void reportState() {
        long alive = plants.stream().filter(p -> !p.isDead()).count();
        long dead = plants.size() - alive;
        double avgHealth = plants.stream()
            .filter(p -> !p.isDead())
            .mapToInt(Plant::getHealth)
            .average()
            .orElse(0.0);
        double avgMoisture = plants.stream()
            .filter(p -> !p.isDead())
            .mapToInt(Plant::getSoilMoisture)
            .average()
            .orElse(0.0);
        long infested = plants.stream()
            .filter(p -> !p.isDead() && !p.getParasites().isEmpty())
            .count();
        
        Logger.log(Logger.LogLevel.INFO, "REPORT: alive=" + alive + " dead=" + dead + 
                 " | avgHealth=" + String.format("%.1f", avgHealth) + 
                 "% | avgMoisture=" + String.format("%.1f", avgMoisture) + 
                 "% | infested=" + infested + 
                 " | temp=" + lastTemperatureF + "F");
    }
}
