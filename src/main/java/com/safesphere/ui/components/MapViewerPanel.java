package com.safesphere.ui.components;

import com.safesphere.model.Volunteer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 * Visual Incident GIS Map (M5).
 * Renders situational tactical map showing victim location, route deviation path,
 * and responder distance radii.
 */
public class MapViewerPanel extends JPanel {
    private double victimLat = 17.3850;
    private double victimLon = 78.4867;
    private boolean routeDeviated = false;
    private List<Volunteer> volunteers;
    private String assignedResponderId = null;

    public MapViewerPanel() {
        setBackground(new Color(15, 23, 42)); // Slate 900
        setBorder(new EmptyBorder(10, 10, 10, 10));
    }

    public void updateMap(double lat, double lon, boolean routeDeviated, List<Volunteer> volunteers, String assignedId) {
        this.victimLat = lat;
        this.victimLon = lon;
        this.routeDeviated = routeDeviated;
        this.volunteers = volunteers;
        this.assignedResponderId = assignedId;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Draw Map Grid / Radar concentric circles
        g2.setColor(new Color(30, 41, 59));
        for (int r = 60; r < Math.max(width, height); r += 60) {
            g2.drawOval(width / 2 - r, height / 2 - r, r * 2, r * 2);
        }

        // Draw Crosshairs
        g2.setColor(new Color(51, 65, 85, 120));
        g2.drawLine(width / 2, 0, width / 2, height);
        g2.drawLine(0, height / 2, width, height / 2);

        // Safe Journey Corridor
        g2.setColor(new Color(34, 197, 94, 160)); // Emerald
        g2.setStroke(new BasicStroke(3.0f));
        g2.drawLine(40, height - 60, width / 2, height / 2);

        if (routeDeviated) {
            // Draw Route Deviation Vector (Red Dashed)
            g2.setColor(new Color(239, 68, 68));
            float[] dash = {6.0f, 6.0f};
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            g2.drawLine(width / 2, height / 2, width - 80, height / 2 - 50);

            // Deviation Label
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.drawString("! CRITICAL ROUTE DEVIATION (+420m)", width - 230, height / 2 - 60);
        }

        // Draw Volunteers relative to center
        if (volunteers != null) {
            int[][] offsets = {
                    {-100, -80},  // VOL-101 (0.4 km)
                    {120, -110},  // VOL-204 (0.8 km)
                    {50, 40},     // VOL-305 (0.25 km)
                    {-60, 90},    // VOL-409 (0.3 km)
                    {160, 120}    // VOL-512 (1.2 km)
            };

            for (int i = 0; i < volunteers.size(); i++) {
                Volunteer v = volunteers.get(i);
                int ox = width / 2 + (i < offsets.length ? offsets[i][0] : (i * 30 - 90));
                int oy = height / 2 + (i < offsets.length ? offsets[i][1] : (i * 25 - 70));

                boolean isAssigned = v.getResponderId().equalsIgnoreCase(assignedResponderId);

                // Draw volunteer node
                if (isAssigned) {
                    // Highlight assigned responder with pulsing gold ring
                    g2.setColor(new Color(245, 158, 11, 100));
                    g2.fillOval(ox - 14, oy - 14, 28, 28);
                    g2.setColor(new Color(245, 158, 11));
                    g2.setStroke(new BasicStroke(2.0f));
                    g2.drawOval(ox - 14, oy - 14, 28, 28);

                    // Connection vector to victim
                    g2.setColor(new Color(245, 158, 11, 180));
                    g2.drawLine(width / 2, height / 2, ox, oy);
                }

                Color vColor = "ONLINE".equalsIgnoreCase(v.getStatus()) ?
                        (v.hasCpr() ? new Color(59, 130, 246) : new Color(148, 163, 184)) :
                        new Color(100, 116, 139);

                g2.setColor(vColor);
                g2.fillOval(ox - 7, oy - 7, 14, 14);
                g2.setColor(Color.WHITE);
                g2.drawOval(ox - 7, oy - 7, 14, 14);

                // Text badge
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                String label = v.getResponderId() + (v.hasCpr() ? " [CPR]" : "");
                if (isAssigned) label += " (ASSIGNED)";
                g2.drawString(label, ox + 10, oy + 4);
            }
        }

        // Draw Victim Center Marker (Pulsating Red Circle)
        int cx = width / 2;
        int cy = height / 2;

        g2.setColor(new Color(239, 68, 68, 60));
        g2.fillOval(cx - 24, cy - 24, 48, 48);

        g2.setColor(new Color(239, 68, 68, 140));
        g2.fillOval(cx - 15, cy - 15, 30, 30);

        g2.setColor(new Color(220, 38, 38));
        g2.fillOval(cx - 8, cy - 8, 16, 16);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawOval(cx - 8, cy - 8, 16, 16);

        // Victim text
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.setColor(Color.WHITE);
        g2.drawString("VICTIM INCIDENT LOCATION", cx - 80, cy + 32);
        g2.setFont(new Font("Consolas", Font.PLAIN, 10));
        g2.setColor(new Color(203, 213, 225));
        g2.drawString(String.format("%.4f° N, %.4f° E", victimLat, victimLon), cx - 60, cy + 46);

        // Map Legend
        drawLegend(g2, width, height);

        g2.dispose();
    }

    private void drawLegend(Graphics2D g2, int width, int height) {
        int lx = 15;
        int ly = 15;

        g2.setColor(new Color(15, 23, 42, 210));
        g2.fillRoundRect(lx, ly, 220, 95, 8, 8);
        g2.setColor(new Color(51, 65, 85));
        g2.drawRoundRect(lx, ly, 220, 95, 8, 8);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.setColor(Color.WHITE);
        g2.drawString("TACTICAL GIS MAP OVERLAY", lx + 10, ly + 18);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        // Victim
        g2.setColor(new Color(239, 68, 68));
        g2.fillOval(lx + 10, ly + 28, 8, 8);
        g2.setColor(new Color(203, 213, 225));
        g2.drawString("Victim Location (Safe Corridor)", lx + 26, ly + 36);

        // Volunteer with CPR
        g2.setColor(new Color(59, 130, 246));
        g2.fillOval(lx + 10, ly + 46, 8, 8);
        g2.setColor(new Color(203, 213, 225));
        g2.drawString("Verified Responder (CPR Certified)", lx + 26, ly + 54);

        // Assigned
        g2.setColor(new Color(245, 158, 11));
        g2.fillOval(lx + 10, ly + 64, 8, 8);
        g2.setColor(new Color(203, 213, 225));
        g2.drawString("Matched & Dispatched Responder", lx + 26, ly + 72);

        // Distance ring note
        g2.setColor(new Color(148, 163, 184));
        g2.drawString("Concentric radar rings: 250m intervals", lx + 10, ly + 88);
    }

    /**
     * Generates and launches an interactive Leaflet HTML map in the default browser.
     */
    public static void generateAndOpenInteractiveMap(double lat, double lon, List<Volunteer> volunteers) {
        try {
            File htmlFile = new File("safesphere_live_map.html");
            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'/>");
            sb.append("<title>SafeSphere Tactical GIS Map</title>");
            sb.append("<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>");
            sb.append("<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>");
            sb.append("<style>body{margin:0;padding:0;}#map{width:100vw;height:100vh;background:#0f172a;}</style>");
            sb.append("</head><body><div id='map'></div><script>");
            sb.append(String.format("var map = L.map('map').setView([%.4f, %.4f], 15);", lat, lon));
            sb.append("L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {maxZoom: 19}).addTo(map);");

            // Victim Marker
            sb.append(String.format("var victimIcon = L.circleMarker([%.4f, %.4f], {radius: 12, fillColor: '#dc2626', color: '#fff', weight: 2, fillOpacity: 0.9}).addTo(map);", lat, lon));
            sb.append("victimIcon.bindPopup('<b>VICTIM LOCATION (CR-8924)</b><br>State: ACTIVE_EMERGENCY<br>Battery: 12% (Survival Mode)').openPopup();");

            // Proximity circle
            sb.append(String.format("L.circle([%.4f, %.4f], {radius: 500, color: '#ef4444', fillOpacity: 0.1}).addTo(map);", lat, lon));

            if (volunteers != null) {
                for (Volunteer v : volunteers) {
                    String color = v.hasCpr() ? "#2563eb" : "#64748b";
                    sb.append(String.format("L.circleMarker([%.4f, %.4f], {radius: 8, fillColor: '%s', color: '#fff', weight: 1.5, fillOpacity: 0.9}).addTo(map)",
                            v.getLatitude(), v.getLongitude(), color));
                    sb.append(String.format(".bindPopup('<b>%s - %s</b><br>Status: %s<br>Dist: %.2f km<br>CPR: %s<br>Score: %.2f');",
                            v.getResponderId(), v.getName(), v.getStatus(), v.getDistanceKm(), v.hasCpr() ? "YES" : "NO", v.getMatchScore()));
                }
            }

            sb.append("</script></body></html>");

            try (FileWriter writer = new FileWriter(htmlFile)) {
                writer.write(sb.toString());
            }

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(htmlFile.toURI());
            }
        } catch (Exception e) {
            System.err.println("Failed to launch browser map: " + e.getMessage());
        }
    }
}
