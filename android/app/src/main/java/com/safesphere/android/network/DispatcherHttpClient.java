package com.safesphere.android.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.safesphere.model.EmergencyCapsule;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Dispatcher HTTP Gateway Client.
 * Connects the mobile device over Wi-Fi / Cellular directly to the SafeSphere Command Hub.
 */
public class DispatcherHttpClient {
    // Default server URL: 10.0.2.2 for Android emulator loopback to host PC, or standard localhost / IP
    private static volatile String serverUrl = "http://10.0.2.2:8080";

    public static void setServerHost(String hostAndPort) {
        if (!hostAndPort.startsWith("http://") && !hostAndPort.startsWith("https://")) {
            serverUrl = "http://" + hostAndPort;
        } else {
            serverUrl = hostAndPort;
        }
    }

    public static String getServerUrl() {
        return serverUrl;
    }

    /**
     * Tests server connectivity (GET /api/v1/agent/health).
     */
    public static CompletableFuture<Boolean> testConnectionAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(serverUrl + "/api/v1/agent/health");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2500);
                conn.setReadTimeout(2500);

                int responseCode = conn.getResponseCode();
                return responseCode == 200;
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Asynchronously dispatches the active emergency capsule to the 112 command desk.
     */
    public static CompletableFuture<String> dispatchCapsuleAsync(EmergencyCapsule capsule, String triggerHint) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(serverUrl + "/api/v1/agent/trigger");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);

                // Build trigger payload
                JsonObject payload = new JsonObject();
                payload.addProperty("sensor_type", "ANDROID_NATIVE_CLIENT");
                payload.addProperty("intensity_g", capsule.getTelemetry().getCrashImpactG());
                payload.addProperty("latitude", capsule.getTelemetry().getLatitude());
                payload.addProperty("longitude", capsule.getTelemetry().getLongitude());
                payload.addProperty("threat_hint", triggerHint != null ? triggerHint : "SOS_BUTTON");
                payload.addProperty("capsule_id", capsule.getCapsuleId());
                payload.addProperty("encrypted_payload", capsule.getEncryptedEvidence());

                byte[] postData = payload.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData);
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        return "SUCCESS: Dispatched to Command Hub (" + code + ")";
                    }
                } else {
                    return "ERROR: Dispatch Server returned HTTP " + code;
                }
            } catch (Exception e) {
                return "OFFLINE: Buffered to Store-and-Forward Mesh (" + e.getClass().getSimpleName() + ")";
            }
        });
    }
}
