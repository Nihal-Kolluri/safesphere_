package com.safesphere.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.safesphere.cv.ThreatResult;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Emergency Capsule DTO as defined in SafeSphere Design Document Section 6.
 * Encrypted, structured incident package distributed across EventBus and network layers.
 */
public class EmergencyCapsule implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private String capsule_id;
    private String timestamp;
    private FSMState fsm_state;
    private Telemetry telemetry;
    private String medical_summary;
    private String encrypted_evidence;
    private ThreatResult threat_verification;
    private String assigned_responder_id;
    private String questionnaire_status;

    public EmergencyCapsule() {
        this.capsule_id = "CR-" + (int)(1000 + Math.random() * 9000);
        this.timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        this.fsm_state = FSMState.SAFE;
        this.telemetry = new Telemetry();
        this.medical_summary = "Blood: O+, Allergies: Penicillin, Emergency Contact: +91-98480-12345";
        this.encrypted_evidence = "";
        this.threat_verification = ThreatResult.clear();
        this.assigned_responder_id = "UNASSIGNED";
        this.questionnaire_status = "PENDING";
    }

    public EmergencyCapsule(String capsuleId, Telemetry telemetry, String medicalSummary) {
        this();
        this.capsule_id = capsuleId;
        this.telemetry = telemetry;
        this.medical_summary = medicalSummary;
    }

    public String getCapsuleId() {
        return capsule_id;
    }

    public void setCapsuleId(String capsule_id) {
        this.capsule_id = capsule_id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public FSMState getFsmState() {
        return fsm_state;
    }

    public void setFsmState(FSMState fsm_state) {
        this.fsm_state = fsm_state;
        this.timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

    public Telemetry getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
    }

    public String getMedicalSummary() {
        return medical_summary;
    }

    public void setMedicalSummary(String medical_summary) {
        this.medical_summary = medical_summary;
    }

    public String getEncryptedEvidence() {
        return encrypted_evidence;
    }

    public void setEncryptedEvidence(String encrypted_evidence) {
        this.encrypted_evidence = encrypted_evidence;
    }

    public ThreatResult getThreatVerification() {
        return threat_verification;
    }

    public void setThreatVerification(ThreatResult threat_verification) {
        this.threat_verification = threat_verification;
    }

    public String getAssignedResponderId() {
        return assigned_responder_id;
    }

    public void setAssignedResponderId(String assigned_responder_id) {
        this.assigned_responder_id = assigned_responder_id;
    }

    public String getQuestionnaireStatus() {
        return questionnaire_status;
    }

    public void setQuestionnaireStatus(String questionnaire_status) {
        this.questionnaire_status = questionnaire_status;
    }

    /**
     * Serializes raw, full capsule to JSON (Section 6.1).
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    public static EmergencyCapsule fromJson(String json) {
        return GSON.fromJson(json, EmergencyCapsule.class);
    }

    /**
     * Produces RBAC filtered view according to active perspective (Section 6.2).
     */
    public FilteredCapsuleView toFilteredView(RoleView roleView, double observerLat, double observerLon) {
        return FilteredCapsuleView.filter(this, roleView, observerLat, observerLon);
    }

    @Override
    public String toString() {
        return String.format("Capsule[%s | %s | %s | Batt: %d%% | %s]",
                capsule_id, fsm_state, telemetry.getNetworkQuality(),
                telemetry.getBatteryLevel(), assigned_responder_id);
    }
}
