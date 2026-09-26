package com.safesphere.model;

import java.io.Serializable;

/**
 * Real-time device sensor and environmental telemetry.
 */
public class Telemetry implements Serializable {
    private static final long serialVersionUID = 1L;

    private double latitude;
    private double longitude;
    private int batteryLevel;
    private NetworkQuality networkQuality;
    private long timestamp;
    private boolean routeDeviated;
    private double crashImpactG;

    public Telemetry() {
        this(17.3850, 78.4867, 85, NetworkQuality.STRONG);
    }

    public Telemetry(double latitude, double longitude, int batteryLevel, NetworkQuality networkQuality) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.batteryLevel = batteryLevel;
        this.networkQuality = networkQuality;
        this.timestamp = System.currentTimeMillis();
        this.routeDeviated = false;
        this.crashImpactG = 1.0;
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

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = Math.max(0, Math.min(100, batteryLevel));
    }

    public NetworkQuality getNetworkQuality() {
        return networkQuality;
    }

    public void setNetworkQuality(NetworkQuality networkQuality) {
        this.networkQuality = networkQuality;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRouteDeviated() {
        return routeDeviated;
    }

    public void setRouteDeviated(boolean routeDeviated) {
        this.routeDeviated = routeDeviated;
    }

    public double getCrashImpactG() {
        return crashImpactG;
    }

    public void setCrashImpactG(double crashImpactG) {
        this.crashImpactG = crashImpactG;
    }

    @Override
    public String toString() {
        return String.format("Telemetry[lat=%.4f, lon=%.4f, batt=%d%%, net=%s, g=%.1f]",
                latitude, longitude, batteryLevel, networkQuality, crashImpactG);
    }
}
