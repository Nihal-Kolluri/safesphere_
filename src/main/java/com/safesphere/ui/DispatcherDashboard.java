package com.safesphere.ui;

import com.safesphere.event.CapsuleUpdatedEvent;
import com.safesphere.event.SafeSphereEventBus;
import com.safesphere.event.TimelineLogEvent;
import com.safesphere.fsm.EmergencyStateEngine;
import com.safesphere.matcher.DatabaseMatcher;
import com.safesphere.model.*;
import com.safesphere.ui.components.MapViewerPanel;
import com.safesphere.ui.components.ModernUIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Dispatcher Command Dashboard (M5) & Relational Matcher Console (M6).
 * Dual-pane interface with Active Incident Queue, RBAC data blurring,
 * Relational Capability Matcher dispatching, and Live Incident Timeline.
 */
public class DispatcherDashboard extends JFrame {
    private final EmergencyStateEngine stateEngine;
    private final DatabaseMatcher databaseMatcher;
    private final SafeSphereEventBus eventBus = SafeSphereEventBus.getInstance();

    // UI Components
    private JPanel rootPanel;
    private JSplitPane mainSplitPane;
    private JTable incidentTable;
    private DefaultTableModel tableModel;
    private JTextArea timelineArea;
    private JComboBox<RoleView> rbacSelector;

    // Detail Panel Cards
    private JLabel detailCapsuleId;
    private JLabel detailStateBadge;
    private JLabel detailLocationLabel;
    private JLabel detailMedicalSummary;
    private JLabel detailBatteryNetworkLabel;
    private JLabel detailThreatLabel;
    private JLabel detailAssignedResponder;
    private JLabel detailTriageLabel;

    // Action buttons
    private JButton dispatchMatchButton;
    private JButton escalateButton;
    private JButton resolveButton;
    private JButton openMapButton;

    private RoleView currentRoleView = RoleView.POLICE_DISPATCHER;

    public DispatcherDashboard(EmergencyStateEngine stateEngine, DatabaseMatcher databaseMatcher) {
        this.stateEngine = stateEngine;
        this.databaseMatcher = databaseMatcher;

        initUI();
        wireEventSubscriptions();
        refreshIncidentDisplay();
    }

    private void initUI() {
        setTitle("SafeSphere — 112 Dispatch Command Desk & Capability Matcher (M5 / M6)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1040, 740);
        setMinimumSize(new Dimension(860, 600));
        setLocationRelativeTo(null);

        rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(ModernUIHelper.COLOR_BG_DARK);

        buildTopToolbar();

        JPanel leftPane = buildLeftIncidentQueuePane();
        JPanel rightPane = buildRightDetailAndTimelinePane();

        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPane, rightPane);
        mainSplitPane.setDividerLocation(460);
        mainSplitPane.setDividerSize(6);
        mainSplitPane.setBackground(ModernUIHelper.COLOR_BG_DARK);
        mainSplitPane.setBorder(null);

        rootPanel.add(mainSplitPane, BorderLayout.CENTER);
        setContentPane(rootPanel);
    }

    private void buildTopToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(ModernUIHelper.COLOR_CARD_BG);
        toolbar.setBorder(new EmptyBorder(10, 16, 10, 16));

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleBox.setOpaque(false);

        JLabel logo = new JLabel("🛡 SAFESPHERE DISPATCH COMMAND DESK");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        logo.setForeground(ModernUIHelper.COLOR_TEXT_MAIN);
        titleBox.add(logo);

        JPanel rbacBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rbacBox.setOpaque(false);

        JLabel rbacLabel = new JLabel("Active RBAC Perspective:");
        rbacLabel.setFont(ModernUIHelper.FONT_BODY_BOLD);
        rbacLabel.setForeground(ModernUIHelper.COLOR_TEXT_MAIN);

        rbacSelector = new JComboBox<>(RoleView.values());
        rbacSelector.setSelectedItem(RoleView.POLICE_DISPATCHER);
        rbacSelector.setFont(ModernUIHelper.FONT_SMALL);
        rbacSelector.setBackground(ModernUIHelper.COLOR_BG_DARK);
        rbacSelector.setForeground(Color.WHITE);

        rbacSelector.addActionListener(e -> {
            currentRoleView = (RoleView) rbacSelector.getSelectedItem();
            eventBus.publish(new TimelineLogEvent("RBAC_SWITCH",
                    "Security context switched to: " + currentRoleView.name()));
            refreshIncidentDisplay();
        });

        rbacBox.add(rbacLabel);
        rbacBox.add(rbacSelector);

        toolbar.add(titleBox, BorderLayout.WEST);
        toolbar.add(rbacBox, BorderLayout.EAST);
        rootPanel.add(toolbar, BorderLayout.NORTH);
    }

    private JPanel buildLeftIncidentQueuePane() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(ModernUIHelper.COLOR_BG_DARK);
        panel.setBorder(new EmptyBorder(12, 12, 12, 6));

        JLabel title = ModernUIHelper.createHeaderLabel("ACTIVE INCIDENT QUEUE");
        panel.add(title, BorderLayout.NORTH);

        String[] columns = {"ID", "Time", "State", "Batt", "Net", "Assigned"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        incidentTable = new JTable(tableModel);
        incidentTable.setBackground(ModernUIHelper.COLOR_CARD_BG);
        incidentTable.setForeground(ModernUIHelper.COLOR_TEXT_MAIN);
        incidentTable.setFont(ModernUIHelper.FONT_BODY);
        incidentTable.setRowHeight(28);
        incidentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        incidentTable.getTableHeader().setBackground(new Color(51, 65, 85));
        incidentTable.getTableHeader().setForeground(Color.WHITE);
        incidentTable.getTableHeader().setFont(ModernUIHelper.FONT_BODY_BOLD);

        // Center render table columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < incidentTable.getColumnCount(); i++) {
            incidentTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane tableScroll = new JScrollPane(incidentTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(ModernUIHelper.COLOR_CARD_BORDER));
        tableScroll.getViewport().setBackground(ModernUIHelper.COLOR_CARD_BG);

        // Action Toolbar
        JPanel actionBox = new JPanel(new GridLayout(2, 2, 8, 8));
        actionBox.setOpaque(false);

        dispatchMatchButton = ModernUIHelper.createStyledButton("⚡ Dispatch Best Match (M6)",
                new Color(37, 99, 235), Color.WHITE, ModernUIHelper.FONT_BODY_BOLD);
        dispatchMatchButton.addActionListener(e -> executeCapabilityDispatch());

        escalateButton = ModernUIHelper.createStyledButton("🔺 Escalate to 112 ERSS",
                new Color(185, 28, 28), Color.WHITE, ModernUIHelper.FONT_SMALL);
        escalateButton.addActionListener(e -> executeEscalation());

        resolveButton = ModernUIHelper.createStyledButton("✓ Resolve Incident",
                new Color(22, 101, 52), Color.WHITE, ModernUIHelper.FONT_SMALL);
        resolveButton.addActionListener(e -> executeResolution());

        openMapButton = ModernUIHelper.createStyledButton("🗺 Interactive Tactical Map",
                new Color(126, 34, 206), Color.WHITE, ModernUIHelper.FONT_SMALL);
        openMapButton.addActionListener(e -> openTacticalMapWindow());

        actionBox.add(dispatchMatchButton);
        actionBox.add(openMapButton);
        actionBox.add(escalateButton);
        actionBox.add(resolveButton);

        panel.add(tableScroll, BorderLayout.CENTER);
        panel.add(actionBox, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildRightDetailAndTimelinePane() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(ModernUIHelper.COLOR_BG_DARK);
        panel.setBorder(new EmptyBorder(12, 6, 12, 12));

        // Details Panel Card
        JPanel detailsCard = ModernUIHelper.createCardPanel();
        detailsCard.setLayout(new BoxLayout(detailsCard, BoxLayout.Y_AXIS));

        JLabel detailsTitle = ModernUIHelper.createHeaderLabel("EMERGENCY CAPSULE & MEDICAL PROFILE");
        detailsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailCapsuleId = new JLabel("Capsule ID: CR-8924");
        detailCapsuleId.setFont(ModernUIHelper.FONT_BODY_BOLD);
        detailCapsuleId.setForeground(Color.WHITE);

        detailStateBadge = new JLabel("Status: SAFE");
        detailStateBadge.setFont(ModernUIHelper.FONT_BODY_BOLD);
        detailStateBadge.setForeground(ModernUIHelper.COLOR_ACCENT_GREEN);

        detailLocationLabel = new JLabel("Location: 17.3850° N, 78.4867° E");
        detailLocationLabel.setFont(ModernUIHelper.FONT_BODY);
        detailLocationLabel.setForeground(ModernUIHelper.COLOR_TEXT_MAIN);

        detailMedicalSummary = new JLabel("Medical: Blood: O+, Allergies: Penicillin, Asthma");
        detailMedicalSummary.setFont(ModernUIHelper.FONT_BODY);
        detailMedicalSummary.setForeground(ModernUIHelper.COLOR_TEXT_MAIN);

        detailBatteryNetworkLabel = new JLabel("Telemetry: 85% Battery | STRONG (5G)");
        detailBatteryNetworkLabel.setFont(ModernUIHelper.FONT_BODY);
        detailBatteryNetworkLabel.setForeground(ModernUIHelper.COLOR_TEXT_MUTED);

        detailThreatLabel = new JLabel("Threat CV: No Hazard Verified");
        detailThreatLabel.setFont(ModernUIHelper.FONT_BODY);
        detailThreatLabel.setForeground(ModernUIHelper.COLOR_TEXT_MUTED);

        detailAssignedResponder = new JLabel("Assigned Responder: NONE");
        detailAssignedResponder.setFont(ModernUIHelper.FONT_BODY_BOLD);
        detailAssignedResponder.setForeground(ModernUIHelper.COLOR_ACCENT_AMBER);

        detailTriageLabel = new JLabel("Silent Triage: Awaiting Input");
        detailTriageLabel.setFont(ModernUIHelper.FONT_BODY);
        detailTriageLabel.setForeground(ModernUIHelper.COLOR_TEXT_MUTED);

        detailsCard.add(detailsTitle);
        detailsCard.add(Box.createVerticalStrut(8));
        detailsCard.add(detailCapsuleId);
        detailsCard.add(detailStateBadge);
        detailsCard.add(Box.createVerticalStrut(4));
        detailsCard.add(detailLocationLabel);
        detailsCard.add(detailMedicalSummary);
        detailsCard.add(detailBatteryNetworkLabel);
        detailsCard.add(detailThreatLabel);
        detailsCard.add(detailTriageLabel);
        detailsCard.add(Box.createVerticalStrut(4));
        detailsCard.add(detailAssignedResponder);

        // Timeline Panel Card
        JPanel timelineCard = ModernUIHelper.createCardPanel();
        timelineCard.setLayout(new BorderLayout(0, 6));

        JLabel timelineTitle = ModernUIHelper.createHeaderLabel("LIVE INCIDENT TIMELINE & AUDIT LOG");
        timelineCard.add(timelineTitle, BorderLayout.NORTH);

        timelineArea = new JTextArea();
        timelineArea.setBackground(new Color(15, 23, 42));
        timelineArea.setForeground(new Color(226, 232, 240));
        timelineArea.setFont(ModernUIHelper.FONT_MONO);
        timelineArea.setEditable(false);
        timelineArea.setLineWrap(true);
        timelineArea.setWrapStyleWord(true);

        JScrollPane timelineScroll = new JScrollPane(timelineArea);
        timelineScroll.setBorder(BorderFactory.createLineBorder(ModernUIHelper.COLOR_CARD_BORDER));

        timelineCard.add(timelineScroll, BorderLayout.CENTER);

        panel.add(detailsCard, BorderLayout.NORTH);
        panel.add(timelineCard, BorderLayout.CENTER);

        return panel;
    }

    private void executeCapabilityDispatch() {
        EmergencyCapsule capsule = stateEngine.getCurrentCapsule();
        Telemetry tel = capsule.getTelemetry();

        // Run M6 SQL Capability Matcher
        Volunteer best = databaseMatcher.findBestMatch(tel.getLatitude(), tel.getLongitude());

        if (best != null) {
            stateEngine.assignResponder(best.getResponderId());
            databaseMatcher.updateVolunteerStatus(best.getResponderId(), "DISPATCHED");

            String msg = String.format("Capability Match Dispatched: %s (%s) [Score: %.2f | Dist: %.2f km | CPR: %s]",
                    best.getResponderId(), best.getName(), best.getMatchScore(),
                    best.getDistanceKm(), best.hasCpr() ? "YES" : "NO");

            JOptionPane.showMessageDialog(this, msg, "M6 Capability Dispatch Successful", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "No available verified volunteers online in this sector.", "Dispatch Notice", JOptionPane.WARNING_MESSAGE);
        }

        refreshIncidentDisplay();
    }

    private void executeEscalation() {
        try {
            stateEngine.transitionTo(FSMState.ESCALATING, "Manual dispatcher escalation to 112 ERSS");
            refreshIncidentDisplay();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "FSM Transition Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void executeResolution() {
        try {
            stateEngine.transitionTo(FSMState.RESOLVED, "Dispatcher closed and audited incident.");
            refreshIncidentDisplay();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "FSM Transition Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openTacticalMapWindow() {
        JFrame mapFrame = new JFrame("SafeSphere — Live Tactical GIS Map Overlay");
        mapFrame.setSize(750, 600);
        mapFrame.setLocationRelativeTo(this);

        EmergencyCapsule cap = stateEngine.getCurrentCapsule();
        Telemetry tel = cap.getTelemetry();
        List<Volunteer> allVolunteers = databaseMatcher.getAllVolunteers();

        MapViewerPanel mapPanel = new MapViewerPanel();
        mapPanel.updateMap(tel.getLatitude(), tel.getLongitude(),
                tel.isRouteDeviated(), allVolunteers, cap.getAssignedResponderId());

        JPanel container = new JPanel(new BorderLayout());
        container.add(mapPanel, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottomBar.setBackground(ModernUIHelper.COLOR_CARD_BG);

        JButton browserMapBtn = ModernUIHelper.createStyledButton("🌐 Open Interactive HTML Map in Browser",
                ModernUIHelper.COLOR_SECONDARY, Color.WHITE, ModernUIHelper.FONT_SMALL);
        browserMapBtn.addActionListener(e -> {
            MapViewerPanel.generateAndOpenInteractiveMap(tel.getLatitude(), tel.getLongitude(), allVolunteers);
        });

        bottomBar.add(browserMapBtn);
        container.add(bottomBar, BorderLayout.SOUTH);

        mapFrame.setContentPane(container);
        mapFrame.setVisible(true);
    }

    public void appendTimeline(String logLine) {
        SwingUtilities.invokeLater(() -> {
            timelineArea.append(logLine + "\n");
            timelineArea.setCaretPosition(timelineArea.getDocument().getLength());
        });
    }

    /**
     * Applies RBAC filtering and updates table & profile cards.
     */
    public void refreshIncidentDisplay() {
        SwingUtilities.invokeLater(() -> {
            EmergencyCapsule capsule = stateEngine.getCurrentCapsule();
            Telemetry tel = capsule.getTelemetry();

            // Calculate RBAC filtered view (Section 6.2)
            FilteredCapsuleView filtered = capsule.toFilteredView(currentRoleView, 17.3820, 78.4840);

            // Update details
            detailCapsuleId.setText("Capsule ID: " + filtered.getCapsuleId());
            detailStateBadge.setText("Status: " + filtered.getFsmState());

            // RBAC Conditional Display
            if (currentRoleView == RoleView.VOLUNTEER) {
                detailLocationLabel.setText(String.format("Location: ~%d meters away (Approximate Distance)",
                        filtered.getApproximateDistanceMeters()));
                detailMedicalSummary.setText("Medical: [REDACTED FOR VOLUNTEER PRIVACY]");
                detailBatteryNetworkLabel.setText("Task Requirement: " + filtered.getTaskRequirement());
                detailThreatLabel.setText("Estimated Target ETA: ~" + filtered.getEtaTargetMins() + " mins");
            } else {
                detailLocationLabel.setText(String.format("Location: %.4f° N, %.4f° E",
                        filtered.getExactLatitude(), filtered.getExactLongitude()));
                detailMedicalSummary.setText("Medical: " + filtered.getMedicalSummary());
                detailBatteryNetworkLabel.setText(String.format("Telemetry: %d%% Battery | %s | %s",
                        filtered.getBatteryLevel(), filtered.getNetworkQuality(),
                        filtered.getEncryptedEvidence() != null && !filtered.getEncryptedEvidence().isEmpty() ?
                                "AES-256 Locked" : "Pending Evidence"));
                detailThreatLabel.setText("Threat CV: " + (filtered.getThreatType() != null ?
                        filtered.getThreatType() + " (" + String.format("%.0f%%", filtered.getThreatConfidence() * 100) + ")" : "Clean"));
            }

            detailAssignedResponder.setText("Assigned Responder: " + capsule.getAssignedResponderId());
            detailTriageLabel.setText("Silent Triage: " + capsule.getQuestionnaireStatus());

            // Update Table Row
            tableModel.setRowCount(0);
            tableModel.addRow(new Object[]{
                    capsule.getCapsuleId(),
                    capsule.getTimestamp().substring(11, 19),
                    capsule.getFsmState().name(),
                    tel.getBatteryLevel() + "%",
                    tel.getNetworkQuality().name(),
                    capsule.getAssignedResponderId()
            });
        });
    }

    private void wireEventSubscriptions() {
        eventBus.subscribe(TimelineLogEvent.class, event -> {
            appendTimeline(event.toLogLine());
        });

        eventBus.subscribe(CapsuleUpdatedEvent.class, event -> {
            refreshIncidentDisplay();
        });

        eventBus.subscribe(com.safesphere.event.StateTransitionEvent.class, event -> {
            refreshIncidentDisplay();
        });
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }
}
