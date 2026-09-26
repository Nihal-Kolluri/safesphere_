package com.safesphere;

import com.safesphere.matcher.DatabaseMatcher;
import com.safesphere.model.Volunteer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseMatcherTest {
    private DatabaseMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new DatabaseMatcher();
    }

    @Test
    @DisplayName("Section 10 Formula: Capability weighting outperforms raw proximity")
    void testCapabilityMatchingWeightingFormula() {
        // Volunteer A: Closer (0.4 km) but NO CPR (0) and NO Vehicle (0)
        double scoreA = Volunteer.calculateScore(0.40, 0, 0); // (1/0.4)*0.4 = 1.00

        // Volunteer B: Further (0.8 km) but HAS CPR (1) and HAS Vehicle (1)
        double scoreB = Volunteer.calculateScore(0.80, 1, 1); // (1/0.8)*0.4 + 0.3 + 0.3 = 0.50 + 0.60 = 1.10

        assertTrue(scoreB > scoreA,
                "Volunteer with CPR and Vehicle must score higher than a non-certified responder who is closer");
    }

    @Test
    @DisplayName("DatabaseMatcher finds top-ranked responder excluding BUSY personnel")
    void testFindBestMatch() {
        Volunteer best = matcher.findBestMatch(17.3850, 78.4867);

        assertNotNull(best);
        assertEquals("ONLINE", best.getStatus());
        assertTrue(best.hasCpr(), "Selected responder should have CPR certification for severe trauma");
        assertTrue(best.getMatchScore() > 1.0, "Score should reflect capability bonuses");
    }

    @Test
    @DisplayName("Updating volunteer status excludes them from subsequent dispatch queries")
    void testStatusUpdate() {
        Volunteer initialBest = matcher.findBestMatch(17.3850, 78.4867);
        assertNotNull(initialBest);

        // Mark as DISPATCHED / BUSY
        matcher.updateVolunteerStatus(initialBest.getResponderId(), "BUSY");

        // Next query should not select initialBest
        Volunteer nextBest = matcher.findBestMatch(17.3850, 78.4867);
        assertNotNull(nextBest);
        assertNotEquals(initialBest.getResponderId(), nextBest.getResponderId());
    }
}
