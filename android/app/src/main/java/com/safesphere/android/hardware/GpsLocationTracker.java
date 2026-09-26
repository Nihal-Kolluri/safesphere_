package com.safesphere.android.hardware;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Dynamic GPS Telemetry Tracker.
 * Manages fine GPS polling and throttles update frequency based on the Survival Decay Profile.
 */
public class GpsLocationTracker implements LocationListener {
    private final LocationManager locationManager;
    private final Context context;
    private long currentIntervalMs = 5000;
    private Location lastKnownLocation;
    private final List<Consumer<Location>> locationListeners = new CopyOnWriteArrayList<>();
    private boolean isTracking = false;

    // Default safe journey planned corridor (Hyderabad City Center anchor points)
    private double corridorStartLat = 17.3850;
    private double corridorStartLon = 78.4867;

    public GpsLocationTracker(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    @SuppressLint("MissingPermission")
    public synchronized void startTracking(long intervalMs) {
        this.currentIntervalMs = intervalMs;
        if (locationManager == null) return;

        try {
            // Get last known location first
            Location lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (lastGps != null) {
                lastKnownLocation = lastGps;
            } else if (lastNet != null) {
                lastKnownLocation = lastNet;
            }

            // Register for updates
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, currentIntervalMs, 2.0f, this);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, currentIntervalMs, 5.0f, this);
            }
            isTracking = true;
        } catch (Exception ignored) {}
    }

    public synchronized void updateInterval(long newIntervalMs) {
        if (newIntervalMs != currentIntervalMs && isTracking) {
            stopTracking();
            startTracking(newIntervalMs);
        }
    }

    public synchronized void stopTracking() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        isTracking = false;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        this.lastKnownLocation = location;
        for (Consumer<Location> listener : locationListeners) {
            try {
                listener.accept(location);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    public void addLocationListener(Consumer<Location> listener) {
        locationListeners.add(listener);
        if (lastKnownLocation != null) {
            listener.accept(lastKnownLocation);
        }
    }

    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    /**
     * Checks if the user has deviated by more than deviationThresholdMeters from the safe corridor.
     */
    public boolean isRouteDeviated(double currentLat, double currentLon, double maxAllowedMeters) {
        float[] results = new float[1];
        Location.distanceBetween(corridorStartLat, corridorStartLon, currentLat, currentLon, results);
        return results[0] > maxAllowedMeters;
    }
}
