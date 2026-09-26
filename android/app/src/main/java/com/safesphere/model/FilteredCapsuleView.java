package com.safesphere.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Serializable;

/**
 * Filtered Capsule View supporting Role-Based Access Control (RBAC) (Section 6.2).
 * Mathematically blurs location and hides medical evidence when viewed by civilian volunteers.
 */
public class FilteredCapsuleView implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final String capsule_id;
    private final String fsm_state;
    private final RoleView active_role;

    // Volunteer view fields (blurred / privacy-preserving)
    private final Integer approximate_distance_meters;
    private final String task_requirement;
    private final Integer eta_target_mins;

    // Dispatcher / Police view fields (full access)
    private final Double exact_latitude;
    private final Double exact_longitude;
    private final Integer battery_level;
    private final String network_quality;
    private final String medical_summary;
    private final String encrypted_evidence;
    private final String threat_type;
    private final Double threat_confidence;
    private final String assigned_responder_id;

    private FilteredCapsuleView(Builder builder) {
        this.capsule_id = builder.capsule_id;
        this.fsm_state = builder.fsm_state;
        this.active_role = builder.active_role;
        this.approximate_distance_meters = builder.approximate_distance_meters;
        this.task_requirement = builder.task_requirement;
        this.eta_target_mins = builder.eta_target_mins;
        this.exact_latitude = builder.exact_latitude;
        this.exact_longitude = builder.exact_longitude;
        this.battery_level = builder.battery_level;
        this.network_quality = builder.network_quality;
        this.medical_summary = builder.medical_summary;
        this.encrypted_evidence = builder.encrypted_evidence;
        this.threat_type = builder.threat_type;
        this.threat_confidence = builder.threat_confidence;
        this.assigned_responder_id = builder.assigned_responder_id;
    }

    public static FilteredCapsuleView filter(EmergencyCapsule capsule, RoleView roleView,
                                             double observerLat, double observerLon) {
        Builder builder = new Builder();
        builder.capsule_id = capsule.getCapsuleId();
        builder.fsm_state = capsule.getFsmState().name();
        builder.active_role = roleView;

        // Haversine distance calculation in meters
        double distanceMeters = calculateHaversineMeters(
                capsule.getTelemetry().getLatitude(), capsule.getTelemetry().getLongitude(),
                observerLat, observerLon);

        // Blurring for volunteer view: round to nearest 50 meters
        int blurredMeters = (int) (Math.round(distanceMeters / 50.0) * 50);
        if (blurredMeters == 0) blurredMeters = 50;

        int etaMins = Math.max(1, (int) Math.ceil(distanceMeters / 250.0)); // assume ~15 km/h urban volunteer speed

        if (roleView == RoleView.VOLUNTEER) {
            builder.approximate_distance_meters = blurredMeters;
            builder.task_requirement = "Immediate First Aid Required. Victim cannot speak.";
            builder.eta_target_mins = etaMins;
            // Deliberately omit exact GPS, medical summary, and encrypted evidence!
        } else {
            // Police / 112 Dispatcher view gets complete telemetry
            builder.approximate_distance_meters = (int) distanceMeters;
            builder.task_requirement = "Full Emergency Dispatch & Medical Interception";
            builder.eta_target_mins = etaMins;
            builder.exact_latitude = capsule.getTelemetry().getLatitude();
            builder.exact_longitude = capsule.getTelemetry().getLongitude();
            builder.battery_level = capsule.getTelemetry().getBatteryLevel();
            builder.network_quality = capsule.getTelemetry().getNetworkQuality().name();
            builder.medical_summary = capsule.getMedicalSummary();
            builder.encrypted_evidence = capsule.getEncryptedEvidence();
            if (capsule.getThreatVerification() != null) {
                builder.threat_type = capsule.getThreatVerification().getHazardType();
                builder.threat_confidence = capsule.getThreatVerification().getConfidence();
            }
            builder.assigned_responder_id = capsule.getAssignedResponderId();
        }

        return builder.build();
    }

    private static double calculateHaversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radius of the Earth in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public String getCapsuleId() {
        return capsule_id;
    }

    public String getFsmState() {
        return fsm_state;
    }

    public RoleView getActiveRole() {
        return active_role;
    }

    public Integer getApproximateDistanceMeters() {
        return approximate_distance_meters;
    }

    public String getTaskRequirement() {
        return task_requirement;
    }

    public Integer getEtaTargetMins() {
        return eta_target_mins;
    }

    public Double getExactLatitude() {
        return exact_latitude;
    }

    public Double getExactLongitude() {
        return exact_longitude;
    }

    public Integer getBatteryLevel() {
        return battery_level;
    }

    public String getNetworkQuality() {
        return network_quality;
    }

    public String getMedicalSummary() {
        return medical_summary;
    }

    public String getEncryptedEvidence() {
        return encrypted_evidence;
    }

    public String getThreatType() {
        return threat_type;
    }

    public Double getThreatConfidence() {
        return threat_confidence;
    }

    public String getAssignedResponderId() {
        return assigned_responder_id;
    }

    public static class Builder {
        private String capsule_id;
        private String fsm_state;
        private RoleView active_role;
        private Integer approximate_distance_meters;
        private String task_requirement;
        private Integer eta_target_mins;
        private Double exact_latitude;
        private Double exact_longitude;
        private Integer battery_level;
        private String network_quality;
        private String medical_summary;
        private String encrypted_evidence;
        private String threat_type;
        private Double threat_confidence;
        private String assigned_responder_id;

        public FilteredCapsuleView build() {
            return new FilteredCapsuleView(this);
        }
    }
}
