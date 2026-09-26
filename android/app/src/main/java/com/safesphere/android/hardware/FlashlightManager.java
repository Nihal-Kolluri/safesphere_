package com.safesphere.android.hardware;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Build;

/**
 * Optical Rescue Strobe Controller.
 * Pulses the camera LED flash at 5Hz to visually signal first responders in low-visibility crises.
 */
public class FlashlightManager {
    private final CameraManager cameraManager;
    private String cameraId;
    private Thread strobeThread;
    private volatile boolean isStrobing = false;

    public FlashlightManager(Context context) {
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager != null) {
                String[] idList = cameraManager.getCameraIdList();
                if (idList.length > 0) {
                    this.cameraId = idList[0];
                }
            }
        } catch (Exception ignored) {}
    }

    public synchronized void startStrobe() {
        if (isStrobing || cameraManager == null || cameraId == null) return;
        isStrobing = true;

        strobeThread = new Thread(() -> {
            boolean state = false;
            while (isStrobing) {
                try {
                    state = !state;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        cameraManager.setTorchMode(cameraId, state);
                    }
                    Thread.sleep(120); // ~4.2 Hz blink frequency
                } catch (Exception e) {
                    break;
                }
            }
            // Turn off torch when exiting
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager.setTorchMode(cameraId, false);
                }
            } catch (Exception ignored) {}
        }, "SafeSphere-Strobe-Thread");

        strobeThread.start();
    }

    public synchronized void stopStrobe() {
        isStrobing = false;
        if (strobeThread != null) {
            strobeThread.interrupt();
            strobeThread = null;
        }
        try {
            if (cameraManager != null && cameraId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, false);
            }
        } catch (Exception ignored) {}
    }

    public boolean isStrobing() {
        return isStrobing;
    }
}
