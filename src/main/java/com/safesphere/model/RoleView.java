package com.safesphere.model;

/**
 * Role-Based Access Control (RBAC) perspectives for Emergency Capsule dissemination.
 */
public enum RoleView {
    POLICE_DISPATCHER("112 / Police Dispatcher View (Full Telemetry & Medical Evidence)"),
    VOLUNTEER("Verified Volunteer View (Privacy-Preserved & Mathematically Blurred)");

    private final String label;

    RoleView(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
