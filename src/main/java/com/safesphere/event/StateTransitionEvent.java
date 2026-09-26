package com.safesphere.event;

import com.safesphere.model.FSMState;

public class StateTransitionEvent {
    private final FSMState fromState;
    private final FSMState toState;
    private final String reason;
    private final long timestamp;

    public StateTransitionEvent(FSMState fromState, FSMState toState, String reason) {
        this.fromState = fromState;
        this.toState = toState;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
    }

    public FSMState getFromState() {
        return fromState;
    }

    public FSMState getToState() {
        return toState;
    }

    public String getReason() {
        return reason;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("FSM State Transition [%s -> %s] Reason: %s", fromState, toState, reason);
    }
}
