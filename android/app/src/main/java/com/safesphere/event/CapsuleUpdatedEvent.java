package com.safesphere.event;

import com.safesphere.model.EmergencyCapsule;

public class CapsuleUpdatedEvent {
    private final EmergencyCapsule capsule;

    public CapsuleUpdatedEvent(EmergencyCapsule capsule) {
        this.capsule = capsule;
    }

    public EmergencyCapsule getCapsule() {
        return capsule;
    }
}
