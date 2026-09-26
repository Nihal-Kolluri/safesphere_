package com.safesphere;

import com.safesphere.survival.SurvivalEngine;
import com.safesphere.survival.SurvivalProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SurvivalEngineTest {
    private SurvivalEngine survivalEngine;

    @BeforeEach
    void setUp() {
        survivalEngine = new SurvivalEngine();
    }

    @Test
    @DisplayName("Battery > 50% activates OPTIMAL profile with 5s GPS polling")
    void testOptimalProfile() {
        survivalEngine.updateBatteryLevel(75);
        SurvivalProfile profile = survivalEngine.getCurrentProfile();

        assertEquals("OPTIMAL", profile.getName());
        assertEquals(5, profile.getGpsPollingIntervalSeconds());
        assertFalse(profile.isExtremeSurvivalActive());
    }

    @Test
    @DisplayName("15% < Battery <= 50% activates CONSERVATIVE profile with 15s GPS polling")
    void testConservativeProfile() {
        survivalEngine.updateBatteryLevel(35);
        SurvivalProfile profile = survivalEngine.getCurrentProfile();

        assertEquals("CONSERVATIVE", profile.getName());
        assertEquals(15, profile.getGpsPollingIntervalSeconds());
        assertFalse(profile.isExtremeSurvivalActive());
    }

    @Test
    @DisplayName("Battery <= 15% engages EXTREME_SURVIVAL mode with 45s heartbeat and OLED dark theme")
    void testExtremeSurvivalProfile() {
        survivalEngine.updateBatteryLevel(12);
        SurvivalProfile profile = survivalEngine.getCurrentProfile();

        assertEquals("EXTREME_SURVIVAL", profile.getName());
        assertEquals(45, profile.getGpsPollingIntervalSeconds());
        assertTrue(profile.isExtremeSurvivalActive());
        assertTrue(profile.getUiTheme().contains("Pitch Black"));
    }
}
