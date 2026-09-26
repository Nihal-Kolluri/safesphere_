package com.safesphere;

import com.safesphere.crypto.EvidenceVault;
import com.safesphere.cv.ThreatResult;
import com.safesphere.fsm.EmergencyStateEngine;
import com.safesphere.matcher.DatabaseMatcher;
import com.safesphere.model.*;
import com.safesphere.survival.SurvivalEngine;
import com.safesphere.survival.SurvivalProfile;

/**
 * Standalone Zero-Dependency Test Suite Runner.
 * Executes all core test cases and prints validation status.
 */
public class SafeSphereTestSuite {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("  SAFESPHERE AUTOMATED VERIFICATION TEST SUITE");
        System.out.println("=========================================================\n");

        testStateTransitions();
        testIllegalStateJumps();
        testCvBypassRule();
        testCryptoVaultRoundtrip();
        testCryptoTamperDetection();
        testSurvivalModeDecay();
        testCapabilityMatcherFormula();
        testRbacDataFiltering();

        System.out.println("\n---------------------------------------------------------");
        System.out.printf("  TOTAL: %d | PASSED: %d | FAILED: %d\n", (passed + failed), passed, failed);
        System.out.println("=========================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testStateTransitions() {
        try {
            EmergencyStateEngine engine = new EmergencyStateEngine();
            assertCondition(engine.getCurrentState() == FSMState.SAFE, "Initial state is SAFE");

            engine.transitionTo(FSMState.SUSPICIOUS, "Anomaly");
            assertCondition(engine.getCurrentState() == FSMState.SUSPICIOUS, "Transition to SUSPICIOUS");

            engine.transitionTo(FSMState.CHECKING, "SOS trigger");
            assertCondition(engine.getCurrentState() == FSMState.CHECKING, "Transition to CHECKING");

            engine.transitionTo(FSMState.EMERGENCY, "Timer expired");
            assertCondition(engine.getCurrentState() == FSMState.EMERGENCY, "Transition to EMERGENCY");

            engine.transitionTo(FSMState.RESPONDER_ASSIGNED, "Matcher paired");
            assertCondition(engine.getCurrentState() == FSMState.RESPONDER_ASSIGNED, "Transition to RESPONDER_ASSIGNED");

            engine.transitionTo(FSMState.RESOLVED, "Closed");
            assertCondition(engine.getCurrentState() == FSMState.RESOLVED, "Transition to RESOLVED");

            engine.shutdown();
            recordPass("FSM Legal State Transitions");
        } catch (Exception e) {
            recordFail("FSM Legal State Transitions", e.getMessage());
        }
    }

    private static void testIllegalStateJumps() {
        EmergencyStateEngine engine = new EmergencyStateEngine();
        try {
            engine.transitionTo(FSMState.RESPONDER_ASSIGNED, "Direct illegal jump");
            recordFail("FSM Illegal Transition Rejection", "Should have thrown IllegalStateException");
        } catch (IllegalStateException expected) {
            recordPass("FSM Illegal Transition Rejection (SAFE -> RESPONDER_ASSIGNED rejected)");
        } catch (Exception e) {
            recordFail("FSM Illegal Transition Rejection", e.getMessage());
        } finally {
            engine.shutdown();
        }
    }

    private static void testCvBypassRule() {
        EmergencyStateEngine engine = new EmergencyStateEngine();
        try {
            engine.transitionTo(FSMState.CHECKING, "Window active");
            ThreatResult result = ThreatResult.weaponDetected(0.95);
            assertCondition(result.shouldBypassSilentConfirmation(), "Weapon result bypass flag set");

            engine.evaluateThreatResult(result);
            assertCondition(engine.getCurrentState() == FSMState.EMERGENCY, "Bypassed directly to EMERGENCY");
            recordPass("CV Threat Verification (Confidence > 0.85 bypasses countdown)");
        } catch (Exception e) {
            recordFail("CV Threat Verification", e.getMessage());
        } finally {
            engine.shutdown();
        }
    }

    private static void testCryptoVaultRoundtrip() {
        try {
            EvidenceVault vault = new EvidenceVault();
            String original = "{\"incident\":\"CR-8924\",\"medical\":\"O+\"}";
            String encrypted = vault.encrypt(original);
            assertCondition(encrypted != null && !encrypted.equals(original), "Encrypted payload valid");

            String decrypted = vault.decrypt(encrypted);
            assertCondition(original.equals(decrypted), "Decrypted plaintext matches original");
            recordPass("Evidence Vault AES-256-GCM Encryption / Decryption");
        } catch (Exception e) {
            recordFail("Evidence Vault AES-256-GCM Encryption / Decryption", e.getMessage());
        }
    }

    private static void testCryptoTamperDetection() {
        try {
            EvidenceVault vault = new EvidenceVault();
            String encrypted = vault.encrypt("IMMUTABLE_CHAIN_OF_CUSTODY");

            byte[] raw = java.util.Base64.getDecoder().decode(encrypted);
            raw[raw.length - 2] ^= 0x55; // tamper
            String tampered = java.util.Base64.getEncoder().encodeToString(raw);

            try {
                vault.decrypt(tampered);
                recordFail("AES-256-GCM Tamper Detection", "Tampered ciphertext was not detected!");
            } catch (SecurityException expected) {
                recordPass("AES-256-GCM Tamper Detection (AEAD Bad Tag detected)");
            }
        } catch (Exception e) {
            recordFail("AES-256-GCM Tamper Detection", e.getMessage());
        }
    }

    private static void testSurvivalModeDecay() {
        try {
            SurvivalEngine engine = new SurvivalEngine();

            // Optimal (> 50%)
            engine.updateBatteryLevel(80);
            assertCondition(engine.getCurrentProfile().getGpsPollingIntervalSeconds() == 5, "Optimal GPS 5s");

            // Conservative (16-50%)
            engine.updateBatteryLevel(30);
            assertCondition(engine.getCurrentProfile().getGpsPollingIntervalSeconds() == 15, "Conservative GPS 15s");

            // Extreme Survival (<= 15%)
            engine.updateBatteryLevel(12);
            SurvivalProfile extreme = engine.getCurrentProfile();
            assertCondition(extreme.getGpsPollingIntervalSeconds() == 45, "Extreme GPS 45s heartbeat");
            assertCondition(extreme.isExtremeSurvivalActive(), "Extreme Survival flag active");
            recordPass("Resource Conservation Model (Survival Mode Decay)");
        } catch (Exception e) {
            recordFail("Resource Conservation Model (Survival Mode Decay)", e.getMessage());
        }
    }

    private static void testCapabilityMatcherFormula() {
        try {
            // Distance vs Capability weighting formula
            double scoreA = Volunteer.calculateScore(0.40, 0, 0); // 1.00
            double scoreB = Volunteer.calculateScore(0.80, 1, 1); // 1.10
            assertCondition(scoreB > scoreA, "Doctor further away scores higher than untrained closer volunteer");

            DatabaseMatcher matcher = new DatabaseMatcher();
            Volunteer best = matcher.findBestMatch(17.3850, 78.4867);
            assertCondition(best != null, "Best match found");
            assertCondition("ONLINE".equals(best.getStatus()), "Selected responder is ONLINE");
            recordPass("M6 Relational Capability Matcher (Section 10 Weighted Formula)");
        } catch (Exception e) {
            recordFail("M6 Relational Capability Matcher", e.getMessage());
        }
    }

    private static void testRbacDataFiltering() {
        try {
            EmergencyCapsule capsule = new EmergencyCapsule();
            capsule.getTelemetry().setLatitude(17.3850);
            capsule.getTelemetry().setLongitude(78.4867);
            capsule.setMedicalSummary("Blood: O+, Allergies: Penicillin");

            // Volunteer View
            FilteredCapsuleView volView = capsule.toFilteredView(RoleView.VOLUNTEER, 17.3800, 78.4800);
            assertCondition(volView.getExactLatitude() == null, "Volunteer view strips exact latitude");
            assertCondition(volView.getMedicalSummary() == null, "Volunteer view strips medical records");
            assertCondition(volView.getApproximateDistanceMeters() != null, "Volunteer view provides approximate distance");

            // Police View
            FilteredCapsuleView polView = capsule.toFilteredView(RoleView.POLICE_DISPATCHER, 17.3800, 78.4800);
            assertCondition(polView.getExactLatitude() != null, "Police view retains exact latitude");
            assertCondition(polView.getMedicalSummary() != null, "Police view retains medical records");

            recordPass("Role-Based Access Control (RBAC Data Blurring & Redaction)");
        } catch (Exception e) {
            recordFail("Role-Based Access Control (RBAC Data Blurring & Redaction)", e.getMessage());
        }
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }

    private static void recordPass(String testName) {
        System.out.printf("  [PASS] %s\n", testName);
        passed++;
    }

    private static void recordFail(String testName, String reason) {
        System.out.printf("  [FAIL] %s -> %s\n", testName, reason);
        failed++;
    }
}
