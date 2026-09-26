package com.safesphere.fsm;

import com.safesphere.crypto.EvidenceVault;
import com.safesphere.cv.ThreatResult;
import com.safesphere.event.CapsuleUpdatedEvent;
import com.safesphere.event.SafeSphereEventBus;
import com.safesphere.event.StateTransitionEvent;
import com.safesphere.event.TimelineLogEvent;
import com.safesphere.model.EmergencyCapsule;
import com.safesphere.model.FSMState;
import com.safesphere.model.Telemetry;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Deterministic Safety State Engine (M4).
 * Enforces strict transitions across the emergency lifecycle and manages silent confirmation windows.
 */
public class EmergencyStateEngine {
    private final Map<FSMState, Set<FSMState>> validTransitions = new EnumMap<>(FSMState.class);
    private final SafeSphereEventBus eventBus = SafeSphereEventBus.getInstance();
    private final EvidenceVault evidenceVault = new EvidenceVault();

    private volatile FSMState currentState = FSMState.SAFE;
    private final EmergencyCapsule currentCapsule;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "SafeSphere-FSM-Timer");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> countdownTask;
    private final AtomicInteger remainingSeconds = new AtomicInteger(10);
    private final List<Consumer<Integer>> countdownListeners = new CopyOnWriteArrayList<>();

    public EmergencyStateEngine() {
        this.currentCapsule = new EmergencyCapsule();
        initializeTransitionMatrix();
    }

    public EmergencyStateEngine(EmergencyCapsule capsule) {
        this.currentCapsule = capsule;
        initializeTransitionMatrix();
    }

    private void initializeTransitionMatrix() {
        // SAFE transitions
        validTransitions.put(FSMState.SAFE, EnumSet.of(
                FSMState.SUSPICIOUS,
                FSMState.CHECKING,
                FSMState.EMERGENCY
        ));

        // SUSPICIOUS transitions
        validTransitions.put(FSMState.SUSPICIOUS, EnumSet.of(
                FSMState.CHECKING,
                FSMState.EMERGENCY,
                FSMState.SAFE
        ));

        // CHECKING transitions (Confirmation window active)
        validTransitions.put(FSMState.CHECKING, EnumSet.of(
                FSMState.EMERGENCY,
                FSMState.SAFE
        ));

        // EMERGENCY transitions
        validTransitions.put(FSMState.EMERGENCY, EnumSet.of(
                FSMState.ESCALATING,
                FSMState.RESPONDER_ASSIGNED,
                FSMState.RESOLVED
        ));

        // ESCALATING transitions
        validTransitions.put(FSMState.ESCALATING, EnumSet.of(
                FSMState.RESPONDER_ASSIGNED,
                FSMState.RESOLVED
        ));

        // RESPONDER_ASSIGNED transitions
        validTransitions.put(FSMState.RESPONDER_ASSIGNED, EnumSet.of(
                FSMState.RESOLVED,
                FSMState.ESCALATING
        ));

        // RESOLVED transitions (Can re-arm back to SAFE)
        validTransitions.put(FSMState.RESOLVED, EnumSet.of(
                FSMState.SAFE
        ));
    }

    /**
     * Attempts to transition to a new state. Rejects illegal jumps with an IllegalStateException.
     */
    public synchronized void transitionTo(FSMState targetState, String reason) {
        if (targetState == currentState) {
            return;
        }

        Set<FSMState> allowed = validTransitions.get(currentState);
        if (allowed == null || !allowed.contains(targetState)) {
            String errorMsg = String.format("Illegal FSM transition rejected: Cannot jump from %s to %s. Reason provided: %s",
                    currentState, targetState, reason);
            eventBus.publish(new TimelineLogEvent("FSM_REJECT", errorMsg));
            throw new IllegalStateException(errorMsg);
        }

        FSMState oldState = currentState;
        currentState = targetState;
        currentCapsule.setFsmState(targetState);

        // Cancel countdown if leaving CHECKING or SUSPICIOUS
        if (oldState == FSMState.CHECKING || oldState == FSMState.SUSPICIOUS) {
            cancelCountdown();
        }

        // Action when entering EMERGENCY: Encrypt and seal evidence
        if (targetState == FSMState.EMERGENCY) {
            sealEvidenceVault();
        }

        // Publish events
        eventBus.publish(new StateTransitionEvent(oldState, targetState, reason));
        eventBus.publish(new CapsuleUpdatedEvent(currentCapsule));
        eventBus.publish(new TimelineLogEvent("FSM_STATE",
                String.format("State transitioned: [%s] -> [%s] (%s)", oldState, targetState, reason)));
    }

    /**
     * Triggered by sensor anomaly (accelerometer crash spike or route deviation).
     */
    public synchronized void evaluateSensorSpike(Telemetry telemetry, String spikeDescription) {
        if (currentState == FSMState.SAFE) {
            transitionTo(FSMState.SUSPICIOUS, "Sensor Anomaly: " + spikeDescription);
            startSilentConfirmationWindow(10);
        }
    }

    /**
     * User pressed the SOS button directly.
     */
    public synchronized void triggerSOS() {
        if (currentState == FSMState.SAFE || currentState == FSMState.SUSPICIOUS) {
            transitionTo(FSMState.CHECKING, "Manual SOS triggered by victim");
            startSilentConfirmationWindow(10);
        }
    }

    /**
     * Computer Vision Hazard Analysis callback (M2).
     * If confidence > 0.85, bypasses the silent confirmation countdown immediately!
     */
    public synchronized void evaluateThreatResult(ThreatResult result) {
        currentCapsule.setThreatVerification(result);
        eventBus.publish(new TimelineLogEvent("CV_ANALYSIS",
                String.format("YOLOv8 CV Analysis: Hazard=%s (%s), Confidence=%.2f",
                        result.isHazardDetected(), result.getHazardType(), result.getConfidence())));

        if (result.shouldBypassSilentConfirmation()) {
            eventBus.publish(new TimelineLogEvent("CV_BYPASS",
                    String.format("CRITICAL: CV Confidence %.2f > 0.85 for %s! Bypassing silent confirmation window.",
                            result.getConfidence(), result.getHazardType())));
            cancelCountdown();
            transitionTo(FSMState.EMERGENCY, "CV Hazard Verified: " + result.getHazardType() + " (" + String.format("%.0f%%", result.getConfidence() * 100) + ")");
        }
    }

    /**
     * Starts the 10-second silent confirmation countdown.
     */
    public synchronized void startSilentConfirmationWindow(int durationSeconds) {
        cancelCountdown();
        remainingSeconds.set(durationSeconds);
        notifyCountdownListeners(durationSeconds);

        eventBus.publish(new TimelineLogEvent("COUNTDOWN",
                String.format("Initiated %d-second silent confirmation window.", durationSeconds)));

        countdownTask = scheduler.scheduleAtFixedRate(() -> {
            int remaining = remainingSeconds.decrementAndGet();
            notifyCountdownListeners(remaining);

            if (remaining <= 0) {
                cancelCountdown();
                try {
                    transitionTo(FSMState.EMERGENCY, "Silent confirmation timer expired without user cancellation.");
                } catch (Exception e) {
                    System.err.println("Error escalating upon countdown expiry: " + e.getMessage());
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Cancels the countdown window (e.g. false alarm).
     */
    public synchronized void cancelCountdown() {
        if (countdownTask != null && !countdownTask.isDone()) {
            countdownTask.cancel(true);
            countdownTask = null;
        }
    }

    /**
     * User clicks "Cancel - False Alarm".
     */
    public synchronized void cancelFalseAlarm() {
        cancelCountdown();
        if (currentState == FSMState.CHECKING || currentState == FSMState.SUSPICIOUS) {
            transitionTo(FSMState.SAFE, "Victim confirmed false alarm.");
        }
    }

    /**
     * User clicks "Confirm Immediately".
     */
    public synchronized void confirmEmergencyImmediately() {
        cancelCountdown();
        transitionTo(FSMState.EMERGENCY, "Victim explicitly confirmed emergency immediately.");
    }

    /**
     * Assigns a responder (M6 Matcher result).
     */
    public synchronized void assignResponder(String responderId) {
        currentCapsule.setAssignedResponderId(responderId);
        transitionTo(FSMState.RESPONDER_ASSIGNED, "Responder assigned: " + responderId);
    }

    /**
     * Seals the evidence vault with AES-256-GCM.
     */
    private void sealEvidenceVault() {
        try {
            String encrypted = evidenceVault.packageEvidence(
                    currentCapsule.getCapsuleId(),
                    currentCapsule.getTelemetry().toString(),
                    currentCapsule.getMedicalSummary(),
                    "AUDIO_STREAM_SAMPLE_SECURED"
            );
            currentCapsule.setEncryptedEvidence(encrypted);
            eventBus.publish(new TimelineLogEvent("CRYPTO_AES",
                    "Evidence Vault locked with AES-256-GCM. Digital SHA-256 seal verified."));
        } catch (Exception e) {
            eventBus.publish(new TimelineLogEvent("CRYPTO_ERROR", "AES Encryption error: " + e.getMessage()));
        }
    }

    public void addCountdownListener(Consumer<Integer> listener) {
        countdownListeners.add(listener);
    }

    public void removeCountdownListener(Consumer<Integer> listener) {
        countdownListeners.remove(listener);
    }

    private void notifyCountdownListeners(int seconds) {
        for (Consumer<Integer> listener : countdownListeners) {
            try {
                listener.accept(seconds);
            } catch (Exception ignored) {}
        }
    }

    public FSMState getCurrentState() {
        return currentState;
    }

    public EmergencyCapsule getCurrentCapsule() {
        return currentCapsule;
    }

    public EvidenceVault getEvidenceVault() {
        return evidenceVault;
    }

    public int getRemainingSeconds() {
        return remainingSeconds.get();
    }

    public boolean isAllowedTransition(FSMState target) {
        Set<FSMState> allowed = validTransitions.get(currentState);
        return allowed != null && allowed.contains(target);
    }

    public void shutdown() {
        cancelCountdown();
        scheduler.shutdownNow();
    }
}
