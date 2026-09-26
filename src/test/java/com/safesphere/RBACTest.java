package com.safesphere;

import com.safesphere.model.EmergencyCapsule;
import com.safesphere.model.FilteredCapsuleView;
import com.safesphere.model.RoleView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RBACTest {
    private EmergencyCapsule capsule;

    @BeforeEach
    void setUp() {
        capsule = new EmergencyCapsule();
        capsule.getTelemetry().setLatitude(17.3850);
        capsule.getTelemetry().setLongitude(78.4867);
        capsule.setMedicalSummary("Blood: O+, Allergies: Penicillin");
        capsule.setEncryptedEvidence("AES_CIPHERTEXT_SECURE_PAYLOAD");
    }

    @Test
    @DisplayName("Police View retains full exact GPS and medical records")
    void testPoliceViewAccess() {
        FilteredCapsuleView policeView = capsule.toFilteredView(RoleView.POLICE_DISPATCHER, 17.3800, 78.4800);

        assertNotNull(policeView.getExactLatitude());
        assertEquals(17.3850, policeView.getExactLatitude(), 0.0001);
        assertEquals("Blood: O+, Allergies: Penicillin", policeView.getMedicalSummary());
        assertNotNull(policeView.getEncryptedEvidence());
    }

    @Test
    @DisplayName("Volunteer View mathematically blurs distance and redacts medical records")
    void testVolunteerViewPrivacyBlurring() {
        FilteredCapsuleView volunteerView = capsule.toFilteredView(RoleView.VOLUNTEER, 17.3800, 78.4800);

        // Exact coordinates must be redacted / null
        assertNull(volunteerView.getExactLatitude());
        assertNull(volunteerView.getExactLongitude());

        // Approximate distance must be provided
        assertNotNull(volunteerView.getApproximateDistanceMeters());
        assertTrue(volunteerView.getApproximateDistanceMeters() > 0);

        // Medical summary must be redacted / null
        assertNull(volunteerView.getMedicalSummary());
        assertNull(volunteerView.getEncryptedEvidence());

        // Task requirement and ETA must be present
        assertNotNull(volunteerView.getTaskRequirement());
        assertTrue(volunteerView.getEtaTargetMins() > 0);
    }
}
