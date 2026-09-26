package com.safesphere.cv;

import java.io.Serializable;

/**
 * Result of computer vision threat verification (M2).
 */
public class ThreatResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean hazardDetected;
    private final String hazardType;
    private final double confidence;
    private final String details;
    private final long timestamp;

    public ThreatResult(boolean hazardDetected, String hazardType, double confidence, String details) {
        this.hazardDetected = hazardDetected;
        this.hazardType = hazardType;
        this.confidence = confidence;
        this.details = details;
        this.timestamp = System.currentTimeMillis();
    }

    public static ThreatResult clear() {
        return new ThreatResult(false, "NONE", 0.05, "No weapon, fire, or crash detected in visual field.");
    }

    public static ThreatResult weaponDetected(double confidence) {
        return new ThreatResult(true, "WEAPON", confidence, "High-confidence edged/ballistic weapon detected via YOLOv8.");
    }

    public static ThreatResult fireDetected(double confidence) {
        return new ThreatResult(true, "FIRE", confidence, "Thermal/optical fire or smoke plume signature detected.");
    }

    public static ThreatResult crashDetected(double confidence) {
        return new ThreatResult(true, "COLLISION", confidence, "Severe vehicular impact and airbag deployment signature verified.");
    }

    public boolean isHazardDetected() {
        return hazardDetected;
    }

    public String getHazardType() {
        return hazardType;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getDetails() {
        return details;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean shouldBypassSilentConfirmation() {
        return hazardDetected && confidence > 0.85;
    }

    @Override
    public String toString() {
        return String.format("ThreatResult[hazard=%s, type=%s, conf=%.2f, details=%s]",
                hazardDetected, hazardType, confidence, details);
    }
}
