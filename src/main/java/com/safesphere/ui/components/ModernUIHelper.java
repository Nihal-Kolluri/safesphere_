package com.safesphere.ui.components;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Modern UI Styling & Color Palette Helper for SafeSphere Swing components.
 */
public class ModernUIHelper {
    // Modern Dark & Slate Palette
    public static final Color COLOR_PRIMARY = new Color(220, 38, 38);       // Crimson Red for Emergency
    public static final Color COLOR_PRIMARY_HOVER = new Color(185, 28, 28);
    public static final Color COLOR_SECONDARY = new Color(37, 99, 235);     // Deep Blue
    public static final Color COLOR_BG_DARK = new Color(15, 23, 42);         // Slate 900
    public static final Color COLOR_CARD_BG = new Color(30, 41, 59);        // Slate 800
    public static final Color COLOR_CARD_BORDER = new Color(51, 65, 85);    // Slate 700
    public static final Color COLOR_TEXT_MAIN = new Color(248, 250, 252);    // Slate 50
    public static final Color COLOR_TEXT_MUTED = new Color(148, 163, 184);  // Slate 400
    public static final Color COLOR_ACCENT_GREEN = new Color(34, 197, 94);  // Emerald 500
    public static final Color COLOR_ACCENT_AMBER = new Color(245, 158, 11); // Amber 500
    public static final Color COLOR_ACCENT_PURPLE = new Color(168, 85, 247);// Purple 500
    public static final Color COLOR_PITCH_BLACK = new Color(5, 5, 5);       // OLED Zero-Emission

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);

    public static JButton createStyledButton(String text, Color bg, Color fg, Font font) {
        JButton button = new JButton(text);
        button.setFont(font != null ? font : FONT_BODY_BOLD);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(
                new LineBorder(bg.darker(), 1, true),
                new EmptyBorder(8, 14, 8, 14)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(COLOR_CARD_BG);
        panel.setBorder(new CompoundBorder(
                new LineBorder(COLOR_CARD_BORDER, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));
        return panel;
    }

    public static JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_HEADER);
        label.setForeground(COLOR_TEXT_MAIN);
        return label;
    }

    public static JLabel createMutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SMALL);
        label.setForeground(COLOR_TEXT_MUTED);
        return label;
    }
}
