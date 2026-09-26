package com.safesphere.ui;

import com.safesphere.cv.ThreatResult;
import com.safesphere.cv.ThreatVerificationService;
import com.safesphere.event.SafeSphereEventBus;
import com.safesphere.event.TimelineLogEvent;
import com.safesphere.fsm.EmergencyStateEngine;
import com.safesphere.model.FSMState;
import com.safesphere.model.NetworkQuality;
import com.safesphere.model.Telemetry;
import com.safesphere.survival.MeshStoreAndForward;
import com.safesphere.survival.SurvivalEngine;
import com.safesphere.survival.SurvivalProfile;
import com.safesphere.ui.components.ModernUIHelper;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Victim Interface & Edge Telemetry (M1) + Survival Mode (M3).
 * Simulates mobile application client with simulated hardware telemetry sliders,
 * silent countdown window, "I Can't Speak" questionnaire, and OLED survival transformation.
 */
public class VictimInterface extends JFrame {
    private final EmergencyStateEngine stateEngine;
    private final SurvivalEngine survivalEngine;
    private final ThreatVerificationService cvService;
    private final MeshStoreAndForward meshNetwork;
    private final SafeSphereEventBus eventBus = SafeSphereEventBus.getInstance();

    // UI Panels
    private JPanel rootPanel;
    private JPanel phoneScreenPanel;
    private JPanel headerPanel;
    private JPanel sosPanel;
    private JPanel countdownPanel;
    private JPanel questionnairePanel;
    private JPanel hardwareSimulatorPanel;
    private JPanel survivalAlertBanner;

    // UI Components
    private JLabel statusLabel;
    private JLabel batteryStatusLabel;
    private JLabel countdownLabel;
    private JProgressBar countdownProgressBar;
    private JButton sosButton;
    private JButton cancelCountdownButton;
    private JButton confirmCountdownButton;
    private JSlider batterySlider;
    private JComboBox<NetworkQuality> networkCombo;
    private JButton crashButton;
    private JButton routeDevButton;
    private JButton weaponFireButton;
    private JLabel survivalAlertText;

    // Questionnaire buttons
    private JButton injuredYesBtn, injuredNoBtn;
    private JButton threatYesBtn, threatNoBtn;
    private JLabel questionnaireFeedback;

    private boolean isExtremeSurvival = false;

    public VictimInterface(EmergencyStateEngine stateEngine, SurvivalEngine survivalEngine,
                           ThreatVerificationService cvService, MeshStoreAndForward meshNetwork) {
        this.stateEngine = stateEngine;
        this.survivalEngine = survivalEngine;
        this.cvService = cvService;
        this.meshNetwork = meshNetwork;

        initUI();
        wireEventSubscriptions();
    }

    private void initUI() {
        setTitle("SafeSphere — Victim Mobile Client (M1 / M3)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(480, 840);
        setMinimumSize(new Dimension(440, 720));
        setLocationRelativeTo(null);

        rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(ModernUIHelper.COLOR_BG_DARK);

        phoneScreenPanel = new JPanel();
        phoneScreenPanel.setLayout(new BoxLayout(phoneScreenPanel, BoxLayout.Y_AXIS));
        phoneScreenPanel.setBackground(ModernUIHelper.COLOR_BG_DARK);
        phoneScreenPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        buildHeader();
        buildSurvivalBanner();
        buildCountdownPanel();
        buildSosPanel();
        buildQuestionnairePanel();
        buildHardwareSimulator();

        phoneScreenPanel.add(headerPanel);
        phoneScreenPanel.add(Box.createVerticalStrut(10));
        phoneScreenPanel.add(survivalAlertBanner);
        phoneScreenPanel.add(Box.createVerticalStrut(10));
        phoneScreenPanel.add(countdownPanel);
        phoneScreenPanel.add(Box.createVerticalStrut(12));
        phoneScreenPanel.add(sosPanel);
        phoneScreenPanel.add(Box.createVerticalStrut(12));
        phoneScreenPanel.add(questionnairePanel);
        phoneScreenPanel.add(Box.createVerticalStrut(14));
        phoneScreenPanel.add(hardwareSimulatorPanel);

        JScrollPane scrollPane = new JScrollPane(phoneScreenPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        rootPanel.add(scrollPane, BorderLayout.CENTER);

        setContentPane(rootPanel);
    }

    private void buildHeader() {
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setMaximumSize(new Dimension(440, 50));

        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setOpaque(false);

        JLabel appTitle = new JLabel("SafeSphere Mobile");
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        appTitle.setForeground(ModernUIHelper.COLOR_TEXT_MAIN);

        statusLabel = new JLabel("● SYSTEM SAFE · MONITORING");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusLabel.setForeground(ModernUIHelper.COLOR_ACCENT_GREEN);

        titleBox.add(appTitle);
        titleBox.add(statusLabel);

        batteryStatusLabel = new JLabel("⚡ 85%  📶 5G");
        batteryStatusLabel.setFont(ModernUIHelper.FONT_MONO);
        batteryStatusLabel.setForeground(ModernUIHelper.COLOR_TEXT_MUTED);

        headerPanel.add(titleBox, BorderLayout.WEST);
        headerPanel.add(batteryStatusLabel, BorderLayout.EAST);
    }

    private void buildSurvivalBanner() {
        survivalAlertBanner = new JPanel(new BorderLayout());
        survivalAlertBanner.setBackground(new Color(185, 28, 28)); // Crimson Warning
        survivalAlertBanner.setBorder(new EmptyBorder(8, 12, 8, 12));
        survivalAlertBanner.setMaximumSize(new Dimension(440, 60));
        survivalAlertBanner.setVisible(false);

        survivalAlertText = new JLabel("⚠ EXTREME SURVIVAL MODE ENGAGED · GPS THROTTLED TO 45s");
        survivalAlertText.setFont(new Font("Segoe UI", Font.BOLD, 11));
        survivalAlertText.setForeground(Color.WHITE);
        survivalAlertText.setHorizontalAlignment(SwingConstants.CENTER);

        survivalAlertBanner.add(survivalAlertText, BorderLayout.CENTER);
    }

    private void buildCountdownPanel() {
        countdownPanel = ModernUIHelper.createCardPanel();
        countdownPanel.setLayout(new BoxLayout(countdownPanel, BoxLayout.Y_AXIS));
        countdownPanel.setMaximumSize(new Dimension(440, 130));
        countdownPanel.setVisible(false); // Hidden until checking/suspicious

        JLabel timerTitle = new JLabel("SILENT CONFIRMATION WINDOW");
        timerTitle.setFont(ModernUIHelper.FONT_HEADER);
        timerTitle.setForeground(ModernUIHelper.COLOR_ACCENT_AMBER);
        timerTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        countdownLabel = new JLabel("10s");
        countdownLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        countdownLabel.setForeground(Color.WHITE);
        countdownLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        countdownProgressBar = new JProgressBar(0, 10);
        countdownProgressBar.setValue(10);
        countdownProgressBar.setForeground(ModernUIHelper.COLOR_ACCENT_AMBER);
        countdownProgressBar.setMaximumSize(new Dimension(380, 8));
        countdownProgressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnRow.setOpaque(false);

        cancelCountdownButton = ModernUIHelper.createStyledButton("Cancel (False Alarm)",
                new Color(71, 85, 105), Color.WHITE, ModernUIHelper.FONT_SMALL);
        cancelCountdownButton.addActionListener(e -> stateEngine.cancelFalseAlarm());

        confirmCountdownButton = ModernUIHelper.createStyledButton("Confirm Immediately",
                ModernUIHelper.COLOR_PRIMARY, Color.WHITE, ModernUIHelper.FONT_SMALL);
        confirmCountdownButton.addActionListener(e -> stateEngine.confirmEmergencyImmediately());

        btnRow.add(cancelCountdownButton);
        btnRow.add(confirmCountdownButton);

        countdownPanel.add(timerTitle);
        countdownPanel.add(Box.createVerticalStrut(4));
        countdownPanel.add(countdownLabel);
        countdownPanel.add(Box.createVerticalStrut(4));
        countdownPanel.add(countdownProgressBar);
        countdownPanel.add(Box.createVerticalStrut(6));
        countdownPanel.add(btnRow);
    }

    private void buildSosPanel() {
        sosPanel = new JPanel();
        sosPanel.setLayout(new BoxLayout(sosPanel, BoxLayout.Y_AXIS));
        sosPanel.setOpaque(false);
        sosPanel.setMaximumSize(new Dimension(440, 150));

        sosButton = new JButton("SOS");
        sosButton.setFont(new Font("Segoe UI", Font.BOLD, 38));
        sosButton.setBackground(ModernUIHelper.COLOR_PRIMARY);
        sosButton.setForeground(Color.WHITE);
        sosButton.setFocusPainted(false);
        sosButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sosButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        sosButton.setPreferredSize(new Dimension(170, 95));
        sosButton.setMaximumSize(new Dimension(170, 95));
        sosButton.setBorder(new CompoundBorder(
                new LineBorder(new Color(254, 202, 202), 3, true),
                new EmptyBorder(10, 10, 10, 10)
        ));

        sosButton.addActionListener(e -> {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    stateEngine.triggerSOS();
                    return null;
                }
            }.execute();
        });

        JLabel hintLabel = ModernUIHelper.createMutedLabel("Press to initiate silent confirmation & broadcast capsule");
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        sosPanel.add(sosButton);
        sosPanel.add(Box.createVerticalStrut(6));
        sosPanel.add(hintLabel);
    }

    private void buildQuestionnairePanel() {
        questionnairePanel = ModernUIHelper.createCardPanel();
        questionnairePanel.setLayout(new BoxLayout(questionnairePanel, BoxLayout.Y_AXIS));
        questionnairePanel.setMaximumSize(new Dimension(440, 150));

        JLabel qTitle = ModernUIHelper.createHeaderLabel("“I CAN’T SPEAK” SILENT TRIAGE");
        qTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Q1: Injured?
        JPanel row1 = new JPanel(new BorderLayout());
        row1.setOpaque(false);
        JLabel q1Label = new JLabel("Are you injured?");
        q1Label.setForeground(ModernUIHelper.COLOR_TEXT_MAIN);
        q1Label.setFont(ModernUIHelper.FONT_BODY);

        JPanel btnGroup1 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnGroup1.setOpaque(false);
        injuredYesBtn = ModernUIHelper.createStyledButton("YES", new Color(185, 28, 28), Color.WHITE, ModernUIHelper.FONT_SMALL);
        injuredNoBtn = ModernUIHelper.createStyledButton("NO", new Color(71, 85, 105), Color.WHITE, ModernUIHelper.FONT_SMALL);

        injuredYesBtn.addActionListener(e -> submitTriageAnswer("Injured: YES"));
        injuredNoBtn.addActionListener(e -> submitTriageAnswer("Injured: NO"));
        btnGroup1.add(injuredYesBtn);
        btnGroup1.add(injuredNoBtn);

        row1.add(q1Label, BorderLayout.WEST);
        row1.add(btnGroup1, BorderLayout.EAST);

        // Q2: Threat Nearby?
        JPanel row2 = new JPanel(new BorderLayout());
        row2.setOpaque(false);
        JLabel q2Label = new JLabel("Is threat nearby?");
        q2Label.setForeground(ModernUIHelper.COLOR_TEXT_MAIN);
        q2Label.setFont(ModernUIHelper.FONT_BODY);

        JPanel btnGroup2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnGroup2.setOpaque(false);
        threatYesBtn = ModernUIHelper.createStyledButton("YES", new Color(185, 28, 28), Color.WHITE, ModernUIHelper.FONT_SMALL);
        threatNoBtn = ModernUIHelper.createStyledButton("NO", new Color(71, 85, 105), Color.WHITE, ModernUIHelper.FONT_SMALL);

        threatYesBtn.addActionListener(e -> submitTriageAnswer("Threat Nearby: YES"));
        threatNoBtn.addActionListener(e -> submitTriageAnswer("Threat Nearby: NO"));
        btnGroup2.add(threatYesBtn);
        btnGroup2.add(threatNoBtn);

        row2.add(q2Label, BorderLayout.WEST);
        row2.add(btnGroup2, BorderLayout.EAST);

        questionnaireFeedback = ModernUIHelper.createMutedLabel("Responses are packaged into the Emergency Capsule");
        questionnaireFeedback.setAlignmentX(Component.CENTER_ALIGNMENT);

        questionnairePanel.add(qTitle);
        questionnairePanel.add(Box.createVerticalStrut(8));
        questionnairePanel.add(row1);
        questionnairePanel.add(Box.createVerticalStrut(6));
        questionnairePanel.add(row2);
        questionnairePanel.add(Box.createVerticalStrut(6));
        questionnairePanel.add(questionnaireFeedback);
    }

    private void buildHardwareSimulator() {
        hardwareSimulatorPanel = ModernUIHelper.createCardPanel();
        hardwareSimulatorPanel.setLayout(new BoxLayout(hardwareSimulatorPanel, BoxLayout.Y_AXIS));
        hardwareSimulatorPanel.setMaximumSize(new Dimension(440, 240));

        JLabel simTitle = new JLabel("SIMULATED HARDWARE & SENSORS");
        simTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        simTitle.setForeground(ModernUIHelper.COLOR_ACCENT_PURPLE);
        simTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Battery Slider
        JPanel battRow = new JPanel(new BorderLayout());
        battRow.setOpaque(false);
        JLabel battLabel = new JLabel("Battery Telemetry:");
        battLabel.setFont(ModernUIHelper.FONT_BODY);
        battLabel.setForeground(ModernUIHelper.COLOR_TEXT_MAIN);

        JLabel battValLabel = new JLabel("85%");
        battValLabel.setFont(ModernUIHelper.FONT_BODY_BOLD);
        battValLabel.setForeground(ModernUIHelper.COLOR_ACCENT_GREEN);

        battRow.add(battLabel, BorderLayout.WEST);
        battRow.add(battValLabel, BorderLayout.EAST);

        batterySlider = new JSlider(0, 100, 85);
        batterySlider.setOpaque(false);
        batterySlider.setMajorTickSpacing(25);
        batterySlider.setPaintTicks(true);
        batterySlider.setPaintLabels(true);
        batterySlider.setForeground(ModernUIHelper.COLOR_TEXT_MUTED);

        batterySlider.addChangeListener(e -> {
            int val = batterySlider.getValue();
            battValLabel.setText(val + "%");
            if (val <= 15) {
                battValLabel.setForeground(new Color(239, 68, 68));
            } else if (val <= 50) {
                battValLabel.setForeground(ModernUIHelper.COLOR_ACCENT_AMBER);
            } else {
                battValLabel.setForeground(ModernUIHelper.COLOR_ACCENT_GREEN);
            }

            // Update telemetry and notify survival engine
            stateEngine.getCurrentCapsule().getTelemetry().setBatteryLevel(val);
            survivalEngine.updateBatteryLevel(val);
        });

        // Network Quality Combo
        JPanel netRow = new JPanel(new BorderLayout());
        netRow.setOpaque(false);
        JLabel netLabel = new JLabel("Network Link:");
        netLabel.setFont(ModernUIHelper.FONT_BODY);
        netLabel.setForeground(ModernUIHelper.COLOR_TEXT_MAIN);

        networkCombo = new JComboBox<>(NetworkQuality.values());
        networkCombo.setSelectedItem(NetworkQuality.STRONG);
        networkCombo.setFont(ModernUIHelper.FONT_SMALL);
        networkCombo.setBackground(ModernUIHelper.COLOR_CARD_BG);
        networkCombo.setForeground(Color.WHITE);

        networkCombo.addActionListener(e -> {
            NetworkQuality nq = (NetworkQuality) networkCombo.getSelectedItem();
            stateEngine.getCurrentCapsule().getTelemetry().setNetworkQuality(nq);
            meshNetwork.onNetworkChange(nq == NetworkQuality.OFFLINE);
            batteryStatusLabel.setText(String.format("⚡ %d%%  📶 %s",
                    batterySlider.getValue(), nq.name()));
        });

        netRow.add(netLabel, BorderLayout.WEST);
        netRow.add(networkCombo, BorderLayout.EAST);

        // Action Trigger Buttons (SwingWorker for background execution)
        JPanel actionBtnRow = new JPanel(new GridLayout(1, 3, 6, 0));
        actionBtnRow.setOpaque(false);

        crashButton = ModernUIHelper.createStyledButton("Simulate Crash",
                new Color(153, 27, 27), Color.WHITE, ModernUIHelper.FONT_SMALL);
        crashButton.addActionListener(e -> triggerSimulatedCrash());

        routeDevButton = ModernUIHelper.createStyledButton("Route Deviation",
                new Color(180, 83, 9), Color.WHITE, ModernUIHelper.FONT_SMALL);
        routeDevButton.addActionListener(e -> triggerSimulatedRouteDeviation());

        weaponFireButton = ModernUIHelper.createStyledButton("CV Weapon/Fire",
                new Color(126, 34, 206), Color.WHITE, ModernUIHelper.FONT_SMALL);
        weaponFireButton.addActionListener(e -> triggerSimulatedCVThreat());

        actionBtnRow.add(crashButton);
        actionBtnRow.add(routeDevButton);
        actionBtnRow.add(weaponFireButton);

        hardwareSimulatorPanel.add(simTitle);
        hardwareSimulatorPanel.add(Box.createVerticalStrut(6));
        hardwareSimulatorPanel.add(battRow);
        hardwareSimulatorPanel.add(batterySlider);
        hardwareSimulatorPanel.add(Box.createVerticalStrut(4));
        hardwareSimulatorPanel.add(netRow);
        hardwareSimulatorPanel.add(Box.createVerticalStrut(8));
        hardwareSimulatorPanel.add(actionBtnRow);
    }

    private void submitTriageAnswer(String answer) {
        stateEngine.getCurrentCapsule().setQuestionnaireStatus(answer);
        questionnaireFeedback.setText("Triage updated: " + answer);
        eventBus.publish(new TimelineLogEvent("TRIAGE", "Victim answered: " + answer));
    }

    private void triggerSimulatedCrash() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                Telemetry tel = stateEngine.getCurrentCapsule().getTelemetry();
                tel.setCrashImpactG(8.6); // 8.6G spike
                stateEngine.evaluateSensorSpike(tel, "Severe Deceleration Crash Spike (8.6G)");
                // Analyze frame
                ThreatResult result = cvService.verifyThreat("Severe vehicle crash impact");
                stateEngine.evaluateThreatResult(result);
                return null;
            }
        }.execute();
    }

    private void triggerSimulatedRouteDeviation() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                Telemetry tel = stateEngine.getCurrentCapsule().getTelemetry();
                tel.setRouteDeviated(true);
                // Shift coordinates away from safe corridor
                tel.setLatitude(tel.getLatitude() + 0.0040);
                tel.setLongitude(tel.getLongitude() + 0.0055);
                stateEngine.evaluateSensorSpike(tel, "Critical Safe Corridor Route Deviation (+420m off path)");
                return null;
            }
        }.execute();
    }

    private void triggerSimulatedCVThreat() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                // If in SAFE, move to CHECKING first
                if (stateEngine.getCurrentState() == FSMState.SAFE) {
                    stateEngine.triggerSOS();
                }
                // Verify weapon/fire hazard (confidence > 0.85 bypasses countdown immediately)
                ThreatResult result = cvService.verifyThreat("optical fire and weapon detected");
                stateEngine.evaluateThreatResult(result);
                return null;
            }
        }.execute();
    }

    private void wireEventSubscriptions() {
        // Countdown listener
        stateEngine.addCountdownListener(seconds -> SwingUtilities.invokeLater(() -> {
            countdownLabel.setText(seconds + "s");
            countdownProgressBar.setValue(seconds);
        }));

        // FSM State changes
        eventBus.subscribe(com.safesphere.event.StateTransitionEvent.class, event -> SwingUtilities.invokeLater(() -> {
            FSMState state = event.getToState();
            updateFsmUIState(state);
        }));

        // Survival Mode Profile Listener
        survivalEngine.addProfileListener(profile -> SwingUtilities.invokeLater(() -> {
            applySurvivalProfileUI(profile);
        }));
    }

    private void updateFsmUIState(FSMState state) {
        switch (state) {
            case SAFE -> {
                statusLabel.setText("● SYSTEM SAFE · MONITORING");
                statusLabel.setForeground(ModernUIHelper.COLOR_ACCENT_GREEN);
                countdownPanel.setVisible(false);
                sosButton.setEnabled(true);
                sosButton.setBackground(ModernUIHelper.COLOR_PRIMARY);
            }
            case SUSPICIOUS, CHECKING -> {
                statusLabel.setText("● " + state.name() + " · SILENT WINDOW");
                statusLabel.setForeground(ModernUIHelper.COLOR_ACCENT_AMBER);
                countdownPanel.setVisible(true);
            }
            case EMERGENCY -> {
                statusLabel.setText("● ACTIVE EMERGENCY · BROADCASTING");
                statusLabel.setForeground(new Color(239, 68, 68));
                countdownPanel.setVisible(false);
                sosButton.setBackground(new Color(153, 27, 27));
            }
            case ESCALATING -> {
                statusLabel.setText("● ESCALATING TO 112 ERSS");
                statusLabel.setForeground(new Color(239, 68, 68));
            }
            case RESPONDER_ASSIGNED -> {
                statusLabel.setText("● RESPONDER EN ROUTE");
                statusLabel.setForeground(new Color(59, 130, 246));
            }
            case RESOLVED -> {
                statusLabel.setText("● INCIDENT RESOLVED");
                statusLabel.setForeground(ModernUIHelper.COLOR_ACCENT_GREEN);
                countdownPanel.setVisible(false);
            }
        }
    }

    /**
     * Requirement: If battery drops below 15%, invoke EDT logic that changes JFrame
     * background to Color.BLACK, hides non-essential buttons, and logs "GPS throttled to 45s heartbeat".
     */
    private void applySurvivalProfileUI(SurvivalProfile profile) {
        boolean extreme = profile.isExtremeSurvivalActive();
        this.isExtremeSurvival = extreme;

        batteryStatusLabel.setText(String.format("⚡ %d%%  📶 %s",
                survivalEngine.getCurrentBatteryPercent(),
                networkCombo.getSelectedItem()));

        if (extreme) {
            // Apply Pitch Black (OLED conservation)
            rootPanel.setBackground(Color.BLACK);
            phoneScreenPanel.setBackground(Color.BLACK);
            survivalAlertBanner.setVisible(true);
            survivalAlertText.setText("⚠ EXTREME SURVIVAL MODE ENGAGED · GPS THROTTLED TO 45s HEARTBEAT");

            // Hide non-essential buttons and panels to conserve render energy
            questionnairePanel.setVisible(false);
            crashButton.setVisible(false);
            routeDevButton.setVisible(false);
            weaponFireButton.setVisible(false);

            sosButton.setBackground(new Color(185, 28, 28));
        } else {
            // Restore standard UI
            rootPanel.setBackground(ModernUIHelper.COLOR_BG_DARK);
            phoneScreenPanel.setBackground(ModernUIHelper.COLOR_BG_DARK);
            survivalAlertBanner.setVisible(false);

            questionnairePanel.setVisible(true);
            crashButton.setVisible(true);
            routeDevButton.setVisible(true);
            weaponFireButton.setVisible(true);
        }

        revalidate();
        repaint();
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }
}
