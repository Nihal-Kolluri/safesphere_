package com.safesphere.survival;

import com.safesphere.event.SafeSphereEventBus;
import com.safesphere.event.TimelineLogEvent;
import com.safesphere.model.EmergencyCapsule;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Store-and-Forward Mesh Network Layer (M3).
 * Simulates Bluetooth Low Energy (BLE) / Wi-Fi Direct opportunistic mesh buffering
 * when cellular connectivity drops to OFFLINE.
 */
public class MeshStoreAndForward {
    private final Queue<String> packetBuffer = new ConcurrentLinkedQueue<>();
    private final SafeSphereEventBus eventBus = SafeSphereEventBus.getInstance();
    private volatile boolean isOffline = false;

    public synchronized void onNetworkChange(boolean offline) {
        this.isOffline = offline;
        if (offline) {
            eventBus.publish(new TimelineLogEvent("MESH_NETWORK",
                    "Cellular carrier unreachable. Activated Store-and-Forward Mesh buffering."));
        } else {
            flushBuffer();
        }
    }

    public synchronized void bufferCapsule(EmergencyCapsule capsule) {
        String packet = capsule.toJson();
        packetBuffer.offer(packet);
        eventBus.publish(new TimelineLogEvent("MESH_BUFFER",
                String.format("Emergency Capsule %s buffered to local mesh queue (%d packets queued).",
                        capsule.getCapsuleId(), packetBuffer.size())));
    }

    public synchronized int flushBuffer() {
        int count = packetBuffer.size();
        if (count > 0) {
            packetBuffer.clear();
            eventBus.publish(new TimelineLogEvent("MESH_FLUSH",
                    String.format("Internet restored! Flushed %d buffered mesh packets to 112 Dispatch Hub.", count)));
        }
        return count;
    }

    public int getQueueDepth() {
        return packetBuffer.size();
    }

    public boolean isOffline() {
        return isOffline;
    }
}
