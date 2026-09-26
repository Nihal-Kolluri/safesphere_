package com.safesphere.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Thread-safe concurrent publish-subscribe EventBus (M4).
 * Decouples Edge Telemetry (M1), CV (M2), Survival (M3), FSM (M4), Dispatch (M5), and Matcher (M6).
 */
public class SafeSphereEventBus {
    private static volatile SafeSphereEventBus instance;
    private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    private SafeSphereEventBus() {}

    public static SafeSphereEventBus getInstance() {
        if (instance == null) {
            synchronized (SafeSphereEventBus.class) {
                if (instance == null) {
                    instance = new SafeSphereEventBus();
                }
            }
        }
        return instance;
    }

    /**
     * Subscribes a listener to events of type T.
     */
    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventClass, Consumer<T> listener) {
        listeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>())
                 .add(listener);
    }

    /**
     * Unsubscribes a listener from events of type T.
     */
    public <T> void unsubscribe(Class<T> eventClass, Consumer<T> listener) {
        List<Consumer<?>> eventListeners = listeners.get(eventClass);
        if (eventListeners != null) {
            eventListeners.remove(listener);
        }
    }

    /**
     * Publishes an event to all registered subscribers.
     */
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        if (event == null) return;
        List<Consumer<?>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (Consumer<?> listener : eventListeners) {
                try {
                    ((Consumer<T>) listener).accept(event);
                } catch (Exception e) {
                    System.err.println("[SafeSphereEventBus] Error dispatching event " + event.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Clears all subscribers (useful for testing).
     */
    public void reset() {
        listeners.clear();
    }
}
