package com.safesphere.event;

public class SurvivalModeChangedEvent {
    private final int batteryLevel;
    private final int gpsPollingIntervalSeconds;
    private final String uiTheme;
    private final String evidenceCaptureMode;
    private final String profileName;
    private final boolean extremeSurvivalActive;

    public SurvivalModeChangedEvent(int batteryLevel, int gpsPollingIntervalSeconds,
                                    String uiTheme, String evidenceCaptureMode,
                                    String profileName, boolean extremeSurvivalActive) {
        this.batteryLevel = batteryLevel;
        this.gpsPollingIntervalSeconds = gpsPollingIntervalSeconds;
        this.uiTheme = uiTheme;
        this.evidenceCaptureMode = evidenceCaptureMode;
        this.profileName = profileName;
        this.extremeSurvivalActive = extremeSurvivalActive;
    }

    public int getBatteryLevel() {
        return batteryLevel;
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

    public String getProfileName() {
        return profileName;
    }

    public boolean isExtremeSurvivalActive() {
        return extremeSurvivalActive;
    }
}
