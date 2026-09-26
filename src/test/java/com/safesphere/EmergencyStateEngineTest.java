package com.safesphere;

import com.safesphere.cv.ThreatResult;
import com.safesphere.fsm.EmergencyStateEngine;
import com.safesphere.model.FSMState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EmergencyStateEngineTest {
    private EmergencyStateEngine engine;

    @BeforeEach
    void setUp() {
        engine = new EmergencyStateEngine();
    }

    @AfterEach
    void tearDown() {
        engine.shutdown();
    }

    @Test
    @DisplayName("FSM initializes to SAFE baseline state")
    void testInitialState() {
        assertEquals(FSMState.SAFE, engine.getCurrentState());
    }

    @Test
    @DisplayName("FSM allows valid sequential state transitions")
    void testValidTransitions() {
        engine.transitionTo(FSMState.SUSPICIOUS, "Sensor anomaly detected");
        assertEquals(FSMState.SUSPICIOUS, engine.getCurrentState());

        engine.transitionTo(FSMState.CHECKING, "SOS trigger initiated");
        assertEquals(FSMState.CHECKING, engine.getCurrentState());

        engine.transitionTo(FSMState.EMERGENCY, "Timer expired or threat confirmed");
        assertEquals(FSMState.EMERGENCY, engine.getCurrentState());

        engine.transitionTo(FSMState.ESCALATING, "Level 1 contacts unresponsive");
        assertEquals(FSMState.ESCALATING, engine.getCurrentState());

        engine.transitionTo(FSMState.RESPONDER_ASSIGNED, "Volunteer paired via M6");
        assertEquals(FSMState.RESPONDER_ASSIGNED, engine.getCurrentState());

        engine.transitionTo(FSMState.RESOLVED, "Incident closed by dispatcher");
        assertEquals(FSMState.RESOLVED, engine.getCurrentState());

        engine.transitionTo(FSMState.SAFE, "Re-armed to baseline");
        assertEquals(FSMState.SAFE, engine.getCurrentState());
    }

    @Test
    @DisplayName("FSM strictly rejects illegal state jumps")
    void testIllegalStateJumps() {
        // Jumping from SAFE directly to RESPONDER_ASSIGNED is forbidden
        assertThrows(IllegalStateException.class, () -> {
            engine.transitionTo(FSMState.RESPONDER_ASSIGNED, "Illegal direct jump");
        });

        // Jumping from SAFE directly to RESOLVED is forbidden
        assertThrows(IllegalStateException.class, () -> {
            engine.transitionTo(FSMState.RESOLVED, "Illegal resolve");
        });
    }

    @Test
    @DisplayName("CV Hazard with confidence > 0.85 bypasses silent countdown window")
    void testCvBypassRule() {
        engine.transitionTo(FSMState.CHECKING, "Awaiting confirmation");
        assertEquals(FSMState.CHECKING, engine.getCurrentState());

        // Simulate high-confidence weapon detection
        ThreatResult weaponResult = ThreatResult.weaponDetected(0.94);
        assertTrue(weaponResult.shouldBypassSilentConfirmation());

        engine.evaluateThreatResult(weaponResult);
        assertEquals(FSMState.EMERGENCY, engine.getCurrentState(),
                "State must immediately bypass to EMERGENCY when CV confidence > 0.85");
    }

    @Test
    @DisplayName("Victim can cancel false alarm back to SAFE")
    void testFalseAlarmCancellation() {
        engine.transitionTo(FSMState.CHECKING, "Accidental SOS tap");
        engine.cancelFalseAlarm();
        assertEquals(FSMState.SAFE, engine.getCurrentState());
    }
}
