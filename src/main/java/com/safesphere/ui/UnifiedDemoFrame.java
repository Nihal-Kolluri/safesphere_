package com.safesphere.ui;

import com.safesphere.cv.ThreatVerificationService;
import com.safesphere.fsm.EmergencyStateEngine;
import com.safesphere.matcher.DatabaseMatcher;
import com.safesphere.survival.MeshStoreAndForward;
import com.safesphere.survival.SurvivalEngine;
import com.safesphere.ui.components.ModernUIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Unified Side-by-Side Presentation Demo Frame.
 * Embeds both Victim Mobile Client (left) and Dispatcher Command Console (right)
 * into a single unified window for high-impact hackathon presentation.
 */
public class UnifiedDemoFrame extends JFrame {
    private final VictimInterface victimInterface;
    private final DispatcherDashboard dispatcherDashboard;

    public UnifiedDemoFrame(EmergencyStateEngine stateEngine, SurvivalEngine survivalEngine,
                            ThreatVerificationService cvService, MeshStoreAndForward meshNetwork,
                            DatabaseMatcher databaseMatcher) {
        this.victimInterface = new VictimInterface(stateEngine, survivalEngine, cvService, meshNetwork);
        this.dispatcherDashboard = new DispatcherDashboard(stateEngine, databaseMatcher);

        initUI();
    }

    private void initUI() {
        setTitle("SafeSphere — Emergency Orchestration Platform [Unified Hackathon Demo Suite]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1460, 860);
        setMinimumSize(new Dimension(1200, 750));
        setLocationRelativeTo(null);

        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ModernUIHelper.COLOR_BG_DARK);

        // Demo Header Banner
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(new Color(15, 23, 42));
        banner.setBorder(new EmptyBorder(8, 20, 8, 20));

        JLabel title = new JLabel("🛡 SAFESPHERE LIVE DEMO SUITE · (M1-M6 FULL INTEGRATION)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(ModernUIHelper.COLOR_TEXT_MAIN);

        JLabel subtitle = new JLabel("Left: Simulated Edge Mobile Device | Right: Regional 112 Dispatch Command Hub");
        subtitle.setFont(ModernUIHelper.FONT_SMALL);
        subtitle.setForeground(ModernUIHelper.COLOR_TEXT_MUTED);

        banner.add(title, BorderLayout.WEST);
        banner.add(subtitle, BorderLayout.EAST);

        // Split Pane: Left = Victim Mobile Screen, Right = Dispatcher Dashboard
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                victimInterface.getRootPanel(),
                dispatcherDashboard.getRootPanel());
        splitPane.setDividerLocation(460);
        splitPane.setDividerSize(6);
        splitPane.setBackground(ModernUIHelper.COLOR_BG_DARK);
        splitPane.setBorder(null);

        mainContainer.add(banner, BorderLayout.NORTH);
        mainContainer.add(splitPane, BorderLayout.CENTER);

        setContentPane(mainContainer);
    }
}
