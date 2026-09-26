package com.safesphere.model;

/**
 * Emergency State Machine Taxonomy as defined in SafeSphere Design Document Section 8.
 */
public enum FSMState {
    /**
     * Baseline state. App is idle and monitoring passive telemetry.
     */
    SAFE("Baseline monitoring. System idle."),

    /**
     * Sensor anomaly detected (e.g. route deviation, crash spike).
     * Initiates 10-second silent confirmation window.
     */
    SUSPICIOUS("Sensor anomaly detected. Initiating verification."),

    /**
     * User triggered SOS or anomaly escalated; waiting for confirmation window.
     * High confidence threat from CV will bypass directly to EMERGENCY.
     */
    CHECKING("Active countdown window. Awaiting user cancellation or CV verification."),

    /**
     * Timer expired or threat confirmed.
     * Pings Level 1 (Trusted Contacts) & packages AES evidence capsule.
     */
    EMERGENCY("Threat confirmed! Dispatching Level 1 alerts & broadcasting capsule."),

    /**
     * No response from Level 1 contacts within threshold.
     * Escalates to Level 3 (112 ERSS / Verified Responders).
     */
    ESCALATING("Escalating to Level 3 (112 ERSS / Regional Responders)."),

    /**
     * M6 Relational Capability Matcher successfully paired with volunteer.
     * Live incident timeline updated.
     */
    RESPONDER_ASSIGNED("Responder assigned via capability matching."),

    /**
     * Dispatcher closes incident. Final cryptographic audit record generated.
     */
    RESOLVED("Incident resolved. Audit trail sealed.");

    private final String description;

    FSMState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
