package com.garden.sim.core;

import com.garden.sim.core.factory.PlantFactory;
import com.garden.sim.core.factory.PlantType;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import com.garden.sim.core.orgjson.*;

public class Garden {
    private static final int DEFAULT_TEMPERATURE_F = 72;
    
    private final Logger log;
    @SuppressWarnings("unused") // Used via event bus subscription
    private final EventBus bus;
    private final List<Plant> plants = new ArrayList<>();
    private int lastTemperatureF = DEFAULT_TEMPERATURE_F;

    public Garden(Logger log, EventBus bus) {
        this.log = log; 
        this.bus = bus;
        // Subscribe to day ticks to update plants daily
        bus.subscribe(EventBus.Topic.DAY_TICK, e -> dayTick());
    }

    public void seedFromConfig(String classpathResource) {
        try (InputStream is = Garden.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (is == null) throw new IllegalArgumentException("Missing resource: " + classpathResource);
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(json);
            Map<String, Species> speciesByName = new HashMap<>();
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
            for (Object pObj : root.getJSONArray("plants")) {
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
            log.info("Seeded " + plants.size() + " plants from " + classpathResource);
        } catch (Exception e) {
            log.error("seedFromConfig failed", e);
        }
    }

    private static List<String> toList(JSONArray arr) {
        if (arr == null) return List.of();
        List<String> out = new ArrayList<>();
        for (int i=0;i<arr.length();i++) out.add(arr.get(i).toString());
        return out;
    }

    public List<Plant> getPlantsList() { return Collections.unmodifiableList(plants); }

    public Map<String, Object> exportPlantDefs() {
        List<String> names = plants.stream().map(Plant::getName).collect(Collectors.toList());
        List<Integer> water = plants.stream().map(p -> p.getSpecies().getDailyWaterNeed()).collect(Collectors.toList());
        List<List<String>> parasites = plants.stream()
            .map(p -> new ArrayList<>(p.getSpecies().getParasiteVulns())).collect(Collectors.toList());
        return Map.of("plants", names, "waterRequirement", water, "parasites", parasites);
    }

    public Map<String, Object> exportPlantStates() {
        List<String> names = new ArrayList<>();
        List<Integer> health = new ArrayList<>();
        List<Integer> moisture = new ArrayList<>();
        List<Integer> tempStress = new ArrayList<>();
        List<List<String>> activeParasites = new ArrayList<>();
        List<Boolean> isDead = new ArrayList<>();
        List<String> species = new ArrayList<>();

        for (Plant p : plants) {
            names.add(p.getName());
            health.add(p.getHealth());
            moisture.add(p.getSoilMoisture());
            tempStress.add(p.getTemperatureStress());
            activeParasites.add(new ArrayList<>(p.getParasites()));
            isDead.add(p.isDead());
            species.add(p.getSpecies().getName());
        }

        return Map.of(
            "plants", names,
            "health", health,
            "moisture", moisture,
            "tempStress", tempStress,
            "activeParasites", activeParasites,
            "isDead", isDead,
            "species", species
        );
    }

    public void startModules() {
        // modules subscribe via constructors
    }

    // Event handlers
    public void dayTick() {
        int deadBefore = (int) plants.stream().filter(Plant::isDead).count();
        int lowMoistureCount = 0;
        int infestedCount = 0;
        int stressedCount = 0;
        
        for (Plant p : plants) {
            try {
                p.applyDailyWaterNeed();
                if (p.getSoilMoisture() < 30) lowMoistureCount++;
                
                if (!p.getParasites().isEmpty()) infestedCount++;
                p.dailyParasiteDamage();
                
                p.setTemperature(lastTemperatureF);
                if (p.getTemperatureStress() > 20) stressedCount++;
            } catch (Throwable t) {
                log.error("dayTick plant error for " + p.getName(), t);
            }
        }
        
        int deadAfter = (int) plants.stream().filter(Plant::isDead).count();
        int newlyDead = deadAfter - deadBefore;
        
        log.info("DayTick applied to " + plants.size() + " plants | " +
                 "Low moisture: " + lowMoistureCount + " | " +
                 "Infested: " + infestedCount + " | " +
                 "Temp stressed: " + stressedCount + " | " +
                 "Newly dead: " + newlyDead);
    }

    public void onRain(int amount) {
        int watered = 0;
        for (Plant p : plants) {
            try { 
                int before = p.getSoilMoisture();
                p.rain(amount);
                if (p.getSoilMoisture() > before) watered++;
            } catch (Throwable t) { 
                log.error("rain error for " + p.getName(), t); 
            }
        }
        log.info("Rain applied: amount=" + amount + " to " + plants.size() + " plants (watered: " + watered + ")");
    }

    public void onTemperature(int f) {
        this.lastTemperatureF = f;
        log.info("Temperature set to " + f + "F");
    }

    public void onParasite(String name) {
        int infested = 0;
        for (Plant p : plants) {
            try { 
                boolean wasInfested = !p.getParasites().isEmpty();
                p.infest(name);
                if (!p.getParasites().isEmpty() && !wasInfested) infested++;
            } catch (Throwable t) { 
                log.error("parasite error for " + p.getName(), t); 
            }
        }
        log.info("Parasite introduced: " + name + " (affected " + infested + " plants)");
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
        
        log.info("REPORT: alive=" + alive + " dead=" + dead + 
                 " | avgHealth=" + String.format("%.1f", avgHealth) + 
                 "% | avgMoisture=" + String.format("%.1f", avgMoisture) + 
                 "% | infested=" + infested + 
                 " | temp=" + lastTemperatureF + "F");
    }
}
