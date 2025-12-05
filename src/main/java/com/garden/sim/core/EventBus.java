package com.garden.sim.core;

import com.garden.sim.core.logger.Logger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Minimal synchronous pub/sub event bus to decouple modules and Garden core.
 * Provides thread-safe event publishing and subscription mechanism.
 */
public class EventBus {
    public enum Topic { DAY_TICK, RAIN, TEMPERATURE, PARASITE, PLANT_ADDED, PLANT_REMOVED, HEATING_ACTIVATED }

    private final Map<Topic, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    /**
     * Subscribes a handler to a topic.
     * @param topic The event topic to subscribe to
     * @param handler The handler function to call when events are published
     */
    public void subscribe(Topic topic, Consumer<Object> handler) {
        if (topic == null || handler == null) {
            Logger.log(Logger.LogLevel.WARNING, "EventBus: Attempted to subscribe with null topic or handler");
            return;
        }
        listeners.computeIfAbsent(topic, k -> new ArrayList<>()).add(handler);
    }

    /**
     * Publishes an event to all subscribers of the topic.
     * @param topic The event topic
     * @param payload The event payload data
     */
    public void publish(Topic topic, Object payload) {
        if (topic == null) {
            Logger.log(Logger.LogLevel.WARNING, "EventBus: Attempted to publish with null topic");
            return;
        }
        List<Consumer<Object>> list = listeners.get(topic);
        if (list == null || list.isEmpty()) return;
        for (Consumer<Object> h : List.copyOf(list)) {
            try { 
                h.accept(payload); 
            } catch (Throwable t) {
                Logger.log(Logger.LogLevel.ERROR, "EventBus: Handler exception for topic " + topic + ": " + t.getMessage());
            }
        }
    }
}
