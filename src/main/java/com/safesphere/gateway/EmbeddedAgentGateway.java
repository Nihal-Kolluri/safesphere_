package com.safesphere.gateway;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.safesphere.cv.ThreatResult;
import com.safesphere.event.SafeSphereEventBus;
import com.safesphere.event.TimelineLogEvent;
import com.safesphere.fsm.EmergencyStateEngine;
import com.safesphere.model.Telemetry;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Embedded Agent API Gateway (M4).
 * Exposes REST endpoints (e.g. POST /api/v1/agent/trigger) for external IoT wearables,
 * ride-sharing apps, and autonomous agents to interface directly with SafeSphere.
 */
public class EmbeddedAgentGateway {
    private final int port;
    private final EmergencyStateEngine stateEngine;
    private final SafeSphereEventBus eventBus = SafeSphereEventBus.getInstance();
    private HttpServer server;

    public EmbeddedAgentGateway(EmergencyStateEngine stateEngine) {
        this(8080, stateEngine);
    }

    public EmbeddedAgentGateway(int port, EmergencyStateEngine stateEngine) {
        this.port = port;
        this.stateEngine = stateEngine;
    }

    public synchronized void start() {
        if (server != null) return;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);

            // POST /api/v1/agent/trigger
            server.createContext("/api/v1/agent/trigger", new TriggerHandler());

            // GET /api/v1/agent/capsule
            server.createContext("/api/v1/agent/capsule", new CapsuleHandler());

            // GET /api/v1/agent/health
            server.createContext("/api/v1/agent/health", new HealthHandler());

            server.setExecutor(null); // default executor
            server.start();

            eventBus.publish(new TimelineLogEvent("AGENT_API",
                    "Embedded Agent Gateway listening on http://localhost:" + port + " (POST /api/v1/agent/trigger)"));
        } catch (IOException e) {
            System.err.println("[EmbeddedAgentGateway] Note: Port " + port + " could not be bound (" + e.getMessage() + ")");
        }
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private class TriggerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                try {
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    String sensorType = json.has("sensor_type") ? json.get("sensor_type").getAsString() : "EXTERNAL_IOT";
                    double intensity = json.has("intensity_g") ? json.get("intensity_g").getAsDouble() : 5.0;
                    String threatHint = json.has("threat_hint") ? json.get("threat_hint").getAsString() : "NONE";

                    eventBus.publish(new TimelineLogEvent("AGENT_API",
                            String.format("Inbound IoT Trigger: %s (intensity: %.1fg, hint: %s)",
                                    sensorType, intensity, threatHint)));

                    Telemetry tel = stateEngine.getCurrentCapsule().getTelemetry();
                    tel.setCrashImpactG(intensity);

                    if (json.has("latitude") && json.has("longitude")) {
                        tel.setLatitude(json.get("latitude").getAsDouble());
                        tel.setLongitude(json.get("longitude").getAsDouble());
                    }
                    if (json.has("capsule_id")) {
                        stateEngine.getCurrentCapsule().setCapsuleId(json.get("capsule_id").getAsString());
                    }
                    if (json.has("encrypted_payload")) {
                        stateEngine.getCurrentCapsule().setEncryptedEvidence(json.get("encrypted_payload").getAsString());
                    }

                    if (threatHint.equalsIgnoreCase("weapon") || threatHint.equalsIgnoreCase("fire") || threatHint.contains("HAZARD")) {
                        ThreatResult threat = threatHint.toLowerCase().contains("weapon") ?
                                ThreatResult.weaponDetected(0.95) : ThreatResult.fireDetected(0.92);
                        stateEngine.evaluateThreatResult(threat);
                    } else if (threatHint.equalsIgnoreCase("CONFIRMED_SOS")) {
                        stateEngine.confirmEmergencyImmediately();
                    } else if (threatHint.equalsIgnoreCase("MANUAL_SOS")) {
                        stateEngine.triggerSOS();
                    } else {
                        stateEngine.evaluateSensorSpike(tel, "Inbound Mobile Sensor: " + sensorType + " (" + threatHint + ")");
                    }

                    String response = stateEngine.getCurrentCapsule().toJson();
                    sendResponse(exchange, 200, response, "application/json");
                } catch (Exception e) {
                    String err = "{\"error\":\"" + e.getMessage() + "\"}";
                    sendResponse(exchange, 400, err, "application/json");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private class CapsuleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = stateEngine.getCurrentCapsule().toJson();
            sendResponse(exchange, 200, response, "application/json");
        }
    }

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{\"status\":\"UP\",\"platform\":\"SafeSphere\",\"port\":" + port + "}";
            sendResponse(exchange, 200, response, "application/json");
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public int getPort() {
        return port;
    }
}
