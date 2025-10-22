package com.inductiveautomation.ignition.examples.python3.designer;

import java.awt.*;

/**
 * Modern UI theme with color palette inspired by VS Code, Cursor, and Warp.
 * Provides consistent colors and styles across the IDE.
 */
public class ModernTheme {

    // === Modern Color Palette (v2.7.0 - Matched to Web UI) ===

    // Primary Colors (Matched from python3moduleWEB.png)
    public static final Color BACKGROUND_DARK = new Color(26, 29, 35);          // #1A1D23 - Very dark background
    public static final Color BACKGROUND_DARKER = new Color(20, 22, 27);        // #14161B - Darker variant
    public static final Color BACKGROUND_LIGHT = new Color(32, 35, 42);         // #20232A - Lighter panels

    // Foreground Colors
    public static final Color FOREGROUND_PRIMARY = new Color(204, 204, 204);    // #CCCCCC
    public static final Color FOREGROUND_SECONDARY = new Color(150, 150, 150);  // #969696
    public static final Color FOREGROUND_MUTED = new Color(96, 96, 96);         // #606060

    // Accent Colors (Matched from Web UI - brighter blue)
    public static final Color ACCENT_PRIMARY = new Color(59, 157, 255);         // #3B9DFF - Bright blue from Web UI
    public static final Color ACCENT_HOVER = new Color(79, 177, 255);           // #4FB1FF - Lighter on hover
    public static final Color ACCENT_ACTIVE = new Color(39, 137, 235);          // #2789EB - Darker when active

    // Semantic Colors
    public static final Color SUCCESS = new Color(76, 175, 80);                 // #4CAF50
    public static final Color WARNING = new Color(255, 167, 38);                // #FFA726
    public static final Color ERROR = new Color(244, 67, 54);                   // #F44336
    public static final Color INFO = new Color(33, 150, 243);                   // #2196F3

    // Border Colors (v2.5.7: REVERTED - User liked grey borders, wants WHITE lines removed)
    public static final Color BORDER_DEFAULT = new Color(64, 64, 64);           // #404040 - Nice subtle grey (user's preference)
    public static final Color BORDER_FOCUSED = new Color(14, 99, 156);          // #0E639C
    public static final Color BORDER_HOVER = new Color(80, 80, 80);             // #505050

    // UI Element Colors (v2.7.0 - Updated to match Web UI)
    public static final Color BUTTON_BACKGROUND = new Color(42, 45, 50);        // #2A2D32 - Subtle elevation
    public static final Color BUTTON_HOVER = new Color(52, 55, 60);             // #34373C - Lighter on hover
    public static final Color BUTTON_ACTIVE = new Color(32, 35, 40);            // #202328 - Darker when active

    public static final Color INPUT_BACKGROUND = new Color(32, 35, 42);         // #20232A - Matches BACKGROUND_LIGHT
    public static final Color INPUT_BORDER = new Color(48, 52, 60);             // #30343C - Subtle border

    public static final Color PANEL_BACKGROUND = new Color(32, 35, 42);         // #20232A - Matches BACKGROUND_LIGHT
    public static final Color PANEL_BORDER = new Color(48, 52, 60);             // #30343C - Subtle border

    // Tree Colors
    public static final Color TREE_BACKGROUND = new Color(32, 35, 42);          // #20232A
    public static final Color TREE_SELECTION = new Color(39, 137, 235);         // #2789EB - Matches ACCENT_ACTIVE
    public static final Color TREE_HOVER = new Color(42, 45, 52);               // #2A2D34

    // Spacing and Sizing
    public static final int CORNER_RADIUS = 4;
    public static final int CORNER_RADIUS_LARGE = 6;
    public static final int SPACING_SMALL = 4;
    public static final int SPACING_MEDIUM = 8;
    public static final int SPACING_LARGE = 12;

    // Fonts
    public static final Font FONT_REGULAR = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    public static final Font FONT_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    public static final Font FONT_MONOSPACE = new Font("Monospaced", Font.PLAIN, 12);
    public static final Font FONT_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 14);

    // === Utility Methods ===

    /**
     * Creates a semi-transparent version of a color.
     *
     * @param color the base color
     * @param alpha alpha value (0-255)
     * @return color with specified alpha
     */
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /**
     * Lightens a color by a percentage.
     *
     * @param color   the base color
     * @param percent percentage to lighten (0.0 - 1.0)
     * @return lightened color
     */
    public static Color lighten(Color color, double percent) {
        int r = Math.min(255, (int) (color.getRed() + (255 - color.getRed()) * percent));
        int g = Math.min(255, (int) (color.getGreen() + (255 - color.getGreen()) * percent));
        int b = Math.min(255, (int) (color.getBlue() + (255 - color.getBlue()) * percent));
        return new Color(r, g, b);
    }

    /**
     * Darkens a color by a percentage.
     *
     * @param color   the base color
     * @param percent percentage to darken (0.0 - 1.0)
     * @return darkened color
     */
    public static Color darken(Color color, double percent) {
        int r = Math.max(0, (int) (color.getRed() * (1 - percent)));
        int g = Math.max(0, (int) (color.getGreen() * (1 - percent)));
        int b = Math.max(0, (int) (color.getBlue() * (1 - percent)));
        return new Color(r, g, b);
    }

    /**
     * Returns a font with the specified size.
     *
     * @param baseFont the base font
     * @param size     the font size
     * @return font with new size
     */
    public static Font withSize(Font baseFont, int size) {
        return baseFont.deriveFont((float) size);
    }

    /**
     * Returns a bold version of the font.
     *
     * @param baseFont the base font
     * @return bold font
     */
    public static Font bold(Font baseFont) {
        return baseFont.deriveFont(Font.BOLD);
    }

    // === Component Styling Helpers ===

    /**
     * Applies modern theme to a component.
     *
     * @param component the component to style
     */
    public static void applyToComponent(Component component) {
        component.setBackground(PANEL_BACKGROUND);
        component.setForeground(FOREGROUND_PRIMARY);
        component.setFont(FONT_REGULAR);
    }

    /**
     * Applies modern theme to a button.
     *
     * @param button the button to style
     */
    public static void applyToButton(Component button) {
        button.setBackground(BUTTON_BACKGROUND);
        button.setForeground(FOREGROUND_PRIMARY);
        button.setFont(FONT_BOLD);
    }

    /**
     * Gets status color for a given message type.
     *
     * @param type the message type ("success", "error", "warning", "info")
     * @return appropriate color
     */
    public static Color getStatusColor(String type) {
        switch (type.toLowerCase()) {
            case "success":
                return SUCCESS;
            case "error":
                return ERROR;
            case "warning":
                return WARNING;
            case "info":
                return INFO;
            default:
                return FOREGROUND_PRIMARY;
        }
    }
}
