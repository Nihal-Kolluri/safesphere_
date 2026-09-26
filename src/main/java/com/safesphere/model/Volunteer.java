package com.safesphere.model;

import java.io.Serializable;

/**
 * Verified Volunteer / First Responder entity used in Capability Matching (M6).
 */
public class Volunteer implements Serializable {
    private static final long serialVersionUID = 1L;

    private String responderId;
    private String name;
    private String phone;
    private double latitude;
    private double longitude;
    private double distanceKm;
    private int cprCert;
    private int vehicleAccess;
    private String status;
    private String specialization;
    private double matchScore;

    public Volunteer() {}

    public Volunteer(String responderId, String name, String phone, double latitude, double longitude,
                     double distanceKm, int cprCert, int vehicleAccess, String status, String specialization) {
        this.responderId = responderId;
        this.name = name;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceKm = distanceKm;
        this.cprCert = cprCert;
        this.vehicleAccess = vehicleAccess;
        this.status = status;
        this.specialization = specialization;
        this.matchScore = calculateScore(distanceKm, cprCert, vehicleAccess);
    }

    /**
     * Mathematical weighting from Section 10 of SafeSphere Design Doc:
     * match_score = ( (1 / distance_km) * 0.4 ) + ( cpr_cert * 0.3 ) + ( vehicle_access * 0.3 )
     */
    public static double calculateScore(double distanceKm, int cprCert, int vehicleAccess) {
        double safeDist = Math.max(0.05, distanceKm); // prevent divide by zero
        return ((1.0 / safeDist) * 0.4) + (cprCert * 0.3) + (vehicleAccess * 0.3);
    }

    public String getResponderId() {
        return responderId;
    }

    public void setResponderId(String responderId) {
        this.responderId = responderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
        this.matchScore = calculateScore(this.distanceKm, this.cprCert, this.vehicleAccess);
    }

    public int getCprCert() {
        return cprCert;
    }

    public void setCprCert(int cprCert) {
        this.cprCert = cprCert;
        this.matchScore = calculateScore(this.distanceKm, this.cprCert, this.vehicleAccess);
    }

    public int getVehicleAccess() {
        return vehicleAccess;
    }

    public void setVehicleAccess(int vehicleAccess) {
        this.vehicleAccess = vehicleAccess;
        this.matchScore = calculateScore(this.distanceKm, this.cprCert, this.vehicleAccess);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(double matchScore) {
        this.matchScore = matchScore;
    }

    public boolean hasCpr() {
        return cprCert == 1;
    }

    public boolean hasVehicle() {
        return vehicleAccess == 1;
    }

    @Override
    public String toString() {
        return String.format("%s - %s (Dist: %.2f km, CPR: %s, Vehicle: %s, Score: %.2f)",
                responderId, name, distanceKm, hasCpr() ? "YES" : "NO", hasVehicle() ? "YES" : "NO", matchScore);
    }
}
