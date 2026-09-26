package com.safesphere.model;

/**
 * Telemetry network connectivity state.
 */
public enum NetworkQuality {
    STRONG(1.0, "High-bandwidth 5G/LTE connection active"),
    MODERATE(0.7, "Standard 4G connection active"),
    WEAK(0.3, "Degraded 2G/3G connectivity. Heartbeats prioritized"),
    OFFLINE(0.0, "No cellular/Wi-Fi connection. Mesh Store-and-Forward engaged");

    private final double reliabilityFactor;
    private final String description;

    NetworkQuality(double reliabilityFactor, String description) {
        this.reliabilityFactor = reliabilityFactor;
        this.description = description;
    }

    public double getReliabilityFactor() {
        return reliabilityFactor;
    }

    public String getDescription() {
        return description;
    }
}
