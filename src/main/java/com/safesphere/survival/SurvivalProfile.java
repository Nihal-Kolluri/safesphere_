package com.safesphere.survival;

/**
 * Operating Profile dictated by the SafeSphere Survival Decay Model (Section 9).
 */
public class SurvivalProfile {
    private final String name;
    private final int gpsPollingIntervalSeconds;
    private final String uiTheme;
    private final String evidenceCaptureMode;
    private final boolean extremeSurvivalActive;
    private final String explanation;

    public SurvivalProfile(String name, int gpsPollingIntervalSeconds, String uiTheme,
                           String evidenceCaptureMode, boolean extremeSurvivalActive, String explanation) {
        this.name = name;
        this.gpsPollingIntervalSeconds = gpsPollingIntervalSeconds;
        this.uiTheme = uiTheme;
        this.evidenceCaptureMode = evidenceCaptureMode;
        this.extremeSurvivalActive = extremeSurvivalActive;
        this.explanation = explanation;
    }

    public static SurvivalProfile forBattery(int batteryPercent) {
        if (batteryPercent > 50) {
            return new SurvivalProfile(
                    "OPTIMAL",
                    5,
                    "Standard (Full Contrast)",
                    "High-Res Video + Continuous Audio",
                    false,
                    "High device endurance. Full telemetry and multimedia recording."
            );
        } else if (batteryPercent > 15) {
            return new SurvivalProfile(
                    "CONSERVATIVE",
                    15,
                    "Standard (Power Saving)",
                    "Compressed Audio Only",
                    false,
                    "Battery degraded. GPS throttled to 15s; video disabled to preserve radio power."
            );
        } else {
            return new SurvivalProfile(
                    "EXTREME_SURVIVAL",
                    45,
                    "Pitch Black (OLED Zero-Emission)",
                    "Disabled (Network preserved for SOS Beacon)",
                    true,
                    "Critical resource state (<= 15%). UI collapsed to black, non-essential controls hidden, GPS throttled to 45s heartbeat."
            );
        }
    }

    public String getName() {
        return name;
    }

    public int getGpsPollingIntervalSeconds() {
        return gpsPollingIntervalSeconds;
    }

    public String getUiTheme() {
        return uiTheme;
    }

    public String getEvidenceCaptureMode() {
        return evidenceCaptureMode;
    }

    public boolean isExtremeSurvivalActive() {
        return extremeSurvivalActive;
    }

    public String getExplanation() {
        return explanation;
    }

    @Override
    public String toString() {
        return String.format("[%s] GPS: %ds, Theme: %s, Evidence: %s",
                name, gpsPollingIntervalSeconds, uiTheme, evidenceCaptureMode);
    }
}
