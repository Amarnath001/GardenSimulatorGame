package com.garden.sim.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Minimal synchronous pub/sub to decouple modules and Garden core. */
public class EventBus {
    public enum Topic { DAY_TICK, RAIN, TEMPERATURE, PARASITE, PLANT_ADDED, PLANT_REMOVED, HEATING_ACTIVATED }

    private final Map<Topic, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    public void subscribe(Topic topic, Consumer<Object> handler) {
        listeners.computeIfAbsent(topic, k -> new ArrayList<>()).add(handler);
    }

    public void publish(Topic topic, Object payload) {
        List<Consumer<Object>> list = listeners.get(topic);
        if (list == null) return;
        for (Consumer<Object> h : List.copyOf(list)) {
            try { h.accept(payload); } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
