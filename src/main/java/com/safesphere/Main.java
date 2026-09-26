package com.safesphere;

import com.safesphere.cv.ThreatVerificationService;
import com.safesphere.fsm.EmergencyStateEngine;
import com.safesphere.gateway.EmbeddedAgentGateway;
import com.safesphere.matcher.DatabaseMatcher;
import com.safesphere.survival.MeshStoreAndForward;
import com.safesphere.survival.SurvivalEngine;
import com.safesphere.ui.DispatcherDashboard;
import com.safesphere.ui.UnifiedDemoFrame;
import com.safesphere.ui.VictimInterface;

import javax.swing.*;

/**
 * Main application launcher for the SafeSphere Emergency Orchestration Platform.
 */
public class Main {
    public static void main(String[] args) {
        printBanner();

        // 1. Initialize Core Engine & Modules
        EmergencyStateEngine stateEngine = new EmergencyStateEngine();
        SurvivalEngine survivalEngine = new SurvivalEngine();
        ThreatVerificationService cvService = new ThreatVerificationService();
        MeshStoreAndForward meshNetwork = new MeshStoreAndForward();
        DatabaseMatcher databaseMatcher = new DatabaseMatcher();

        // 2. Start Embedded Agent REST Gateway (Port 8080)
        EmbeddedAgentGateway gateway = new EmbeddedAgentGateway(8080, stateEngine);
        gateway.start();

        // 3. Determine Launch Mode
        String mode = args.length > 0 ? args[0].toLowerCase() : "--demo";

        try {
            // Apply system Look & Feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            switch (mode) {
                case "--victim" -> {
                    System.out.println("[SafeSphere] Launching Standalone Victim Interface (M1 / M3)...");
                    VictimInterface victimUI = new VictimInterface(stateEngine, survivalEngine, cvService, meshNetwork);
                    victimUI.setVisible(true);
                }
                case "--dispatch" -> {
                    System.out.println("[SafeSphere] Launching Standalone Dispatcher Command Desk (M5 / M6)...");
                    DispatcherDashboard dispatchUI = new DispatcherDashboard(stateEngine, databaseMatcher);
                    dispatchUI.setVisible(true);
                }
                case "--cli" -> {
                    System.out.println("[SafeSphere] Running Headless / CLI Test Engine...");
                    runHeadlessVerification(stateEngine, survivalEngine, databaseMatcher);
                }
                default -> {
                    System.out.println("[SafeSphere] Launching Unified Side-by-Side Presentation Demo Suite...");
                    UnifiedDemoFrame demoFrame = new UnifiedDemoFrame(
                            stateEngine, survivalEngine, cvService, meshNetwork, databaseMatcher);
                    demoFrame.setVisible(true);
                }
            }
        });

        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SafeSphere] Shutting down services...");
            gateway.stop();
            stateEngine.shutdown();
        }));
    }

    private static void printBanner() {
        System.out.println("""
            ========================================================================
              ███████╗ █████╗ ███████╗███████╗███████╗██████╗ ██╗  ██╗███████╗██████╗ ███████╗
              ██╔════╝██╔══██╗██╔════╝██╔════╝██╔════╝██╔══██╗██║  ██║██╔════╝██╔══██╗██╔════╝
              ███████╗███████║█████╗  █████╗  ███████╗██████╔╝███████║█████╗  ██████╔╝█████╗  
              ╚════██║██╔══██║██╔══╝  ██╔══╝  ╚════██║██╔═══╝ ██╔══██║██╔══╝  ██╔══██╗██╔══╝  
              ███████║██║  ██║██║     ███████╗███████║██║     ██║  ██║███████╗██║  ██║███████╗
              ╚══════╝╚═╝  ╚═╝╚═╝     ╚══════╝╚══════╝╚═╝     ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚══════╝
                     Intelligent Resilient Emergency Orchestration Platform · v1.0
            ========================================================================
            [M1] Edge Victim Telemetry    [M2] YOLOv8 CV Threat Verification
            [M3] Resource Survival Engine [M4] Deterministic FSM & AES-256 Crypto
            [M5] Dispatcher Command Desk  [M6] Relational Capability Matcher
            ========================================================================
            API Gateway active on: http://localhost:8080/api/v1/agent/trigger
            """);
    }

    private static void runHeadlessVerification(EmergencyStateEngine stateEngine,
                                                SurvivalEngine survivalEngine,
                                                DatabaseMatcher databaseMatcher) {
        System.out.println("[CLI] Verifying Capability Matcher...");
        var best = databaseMatcher.findBestMatch(17.3850, 78.4867);
        System.out.println("[CLI] Best Capability Match: " + best);

        System.out.println("[CLI] Verifying Survival Mode Decay...");
        survivalEngine.updateBatteryLevel(12);
        System.out.println("[CLI] Active Profile at 12%: " + survivalEngine.getCurrentProfile());

        System.out.println("[CLI] Verifying State Engine Transitions...");
        stateEngine.triggerSOS();
        System.out.println("[CLI] State after SOS: " + stateEngine.getCurrentState());
        stateEngine.confirmEmergencyImmediately();
        System.out.println("[CLI] State after confirm: " + stateEngine.getCurrentState());
        System.out.println("[CLI] AES Encrypted Capsule Payload: " + stateEngine.getCurrentCapsule().getEncryptedEvidence());
    }
}
