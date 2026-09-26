package com.safesphere.survival;

import com.safesphere.event.SafeSphereEventBus;
import com.safesphere.event.SurvivalModeChangedEvent;
import com.safesphere.event.TimelineLogEvent;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * SafeSphere Survival Engine (M3).
 * Dynamically trades nonessential phone resources (display rendering, high-frequency GPS, video capture)
 * to maximize emergency beacon lifetime during critical low-power scenarios.
 */
public class SurvivalEngine {
    private final SafeSphereEventBus eventBus = SafeSphereEventBus.getInstance();
    private volatile int currentBatteryPercent = 85;
    private volatile SurvivalProfile currentProfile = SurvivalProfile.forBattery(85);
    private final List<Consumer<SurvivalProfile>> profileListeners = new CopyOnWriteArrayList<>();

    public SurvivalEngine() {}

    /**
     * Updates device battery level and applies resource throttling model (Section 9).
     */
    public synchronized void updateBatteryLevel(int batteryPercent) {
        int sanitized = Math.max(0, Math.min(100, batteryPercent));
        this.currentBatteryPercent = sanitized;

        SurvivalProfile newProfile = SurvivalProfile.forBattery(sanitized);
        boolean profileChanged = !newProfile.getName().equals(currentProfile.getName());

        SurvivalProfile oldProfile = currentProfile;
        currentProfile = newProfile;

        if (profileChanged) {
            eventBus.publish(new TimelineLogEvent("M3_SURVIVAL",
                    String.format("Battery %d%% -> Active Profile: %s. GPS throttled to %ds. Evidence: %s",
                            sanitized, newProfile.getName(),
                            newProfile.getGpsPollingIntervalSeconds(),
                            newProfile.getEvidenceCaptureMode())));

            if (newProfile.isExtremeSurvivalActive()) {
                eventBus.publish(new TimelineLogEvent("M3_SURVIVAL",
                        "CRITICAL: Extreme Survival Mode Engaged! Screen switched to Pitch Black (OLED saver). Non-essential UI hidden."));
            }
        }

        // Notify EventBus
        eventBus.publish(new SurvivalModeChangedEvent(
                sanitized,
                newProfile.getGpsPollingIntervalSeconds(),
                newProfile.getUiTheme(),
                newProfile.getEvidenceCaptureMode(),
                newProfile.getName(),
                newProfile.isExtremeSurvivalActive()
        ));

        // Notify UI listeners safely on Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            for (Consumer<SurvivalProfile> listener : profileListeners) {
                try {
                    listener.accept(newProfile);
                } catch (Exception e) {
                    System.err.println("Error notifying survival listener: " + e.getMessage());
                }
            }
        });
    }

    public void addProfileListener(Consumer<SurvivalProfile> listener) {
        profileListeners.add(listener);
        // Dispatch current profile immediately
        listener.accept(currentProfile);
    }

    public void removeProfileListener(Consumer<SurvivalProfile> listener) {
        profileListeners.remove(listener);
    }

    public int getCurrentBatteryPercent() {
        return currentBatteryPercent;
    }

    public SurvivalProfile getCurrentProfile() {
        return currentProfile;
    }
}
