package com.inductiveautomation.ignition.examples.python3.designer;

import java.awt.*;

/**
 * Modern UI theme with color palette inspired by VS Code, Cursor, and Warp.
 * Provides consistent colors and styles across the IDE.
 */
public class ModernTheme {

    // === Modern Color Palette (v2.8.1 - Matched to Styling.png) ===

    // Primary Colors - FIXED: Sidebar darker than editor for proper hierarchy
    public static final Color EDITOR_BACKGROUND = new Color(30, 34, 40);        // #1E2228 - Editor (slightly lighter)
    public static final Color SIDEBAR_BACKGROUND = new Color(22, 25, 30);       // #16191E - Sidebar (darker than editor)
    public static final Color BACKGROUND_DARK = new Color(26, 29, 35);          // #1A1D23 - General dark background
    public static final Color BACKGROUND_DARKER = new Color(20, 22, 27);        // #14161B - Darkest (status bar)
    public static final Color BACKGROUND_LIGHT = new Color(32, 35, 42);         // #20232A - Lighter panels

    // Foreground Colors - Slightly adjusted for better readability
    public static final Color FOREGROUND_PRIMARY = new Color(212, 212, 212);    // #D4D4D4 - Brighter for readability
    public static final Color FOREGROUND_SECONDARY = new Color(157, 157, 157);  // #9D9D9D - Medium muted
    public static final Color FOREGROUND_MUTED = new Color(106, 106, 106);      // #6A6A6A - Muted elements

    // Accent Colors (Matched from Styling.png)
    public static final Color ACCENT_PRIMARY = new Color(74, 158, 255);         // #4A9EFF - Execute button blue
    public static final Color ACCENT_HOVER = new Color(94, 178, 255);           // #5EB2FF - Lighter on hover
    public static final Color ACCENT_ACTIVE = new Color(54, 138, 235);          // #368AEB - Darker when active

    // Semantic Colors
    public static final Color SUCCESS = new Color(76, 175, 80);                 // #4CAF50 - Save button green
    public static final Color WARNING = new Color(255, 167, 38);                // #FFA726
    public static final Color ERROR = new Color(244, 67, 54);                   // #F44336
    public static final Color ERROR_BRIGHT = new Color(255, 68, 68);            // #FF4444 - Disconnected indicator
    public static final Color INFO = new Color(33, 150, 243);                   // #2196F3

    // Border Colors - More subtle to reduce visual noise
    public static final Color BORDER_SUBTLE = new Color(42, 45, 50);            // #2A2D32 - Very subtle
    public static final Color BORDER_DEFAULT = new Color(46, 46, 46);           // #2E2E2E - Default borders
    public static final Color BORDER_FOCUSED = new Color(14, 99, 156);          // #0E639C
    public static final Color BORDER_HOVER = new Color(58, 58, 58);             // #3A3A3A

    // UI Element Colors (v2.8.1 - Updated for better hierarchy)
    public static final Color BUTTON_BACKGROUND = new Color(42, 45, 50);        // #2A2D32 - Subtle elevation
    public static final Color BUTTON_HOVER = new Color(52, 55, 60);             // #34373C - Lighter on hover
    public static final Color BUTTON_ACTIVE = new Color(32, 35, 40);            // #202328 - Darker when active

    public static final Color INPUT_BACKGROUND = new Color(42, 45, 50);         // #2A2D32 - Input fields
    public static final Color INPUT_BORDER = new Color(58, 58, 58);             // #3A3A3A - Subtle border

    public static final Color PANEL_BACKGROUND = new Color(30, 34, 40);         // #1E2228 - Matches editor
    public static final Color PANEL_BORDER = new Color(42, 45, 50);             // #2A2D32 - Subtle border

    // Tree Colors - FIXED: Darker background, subtle selection
    public static final Color TREE_BACKGROUND = new Color(26, 29, 35);          // #1A1D23 - Darker than editor
    public static final Color TREE_SELECTION = new Color(42, 45, 52);           // #2A2D34 - Subtle gray, not bright blue
    public static final Color TREE_HOVER = new Color(36, 39, 48);               // #242730 - Subtle hover

    // Tab Colors
    public static final Color TAB_ACTIVE_BG = new Color(42, 45, 50);            // #2A2D32 - Active tab
    public static final Color TAB_INACTIVE_BG = new Color(30, 34, 40);          // #1E2228 - Inactive tab
    public static final Color TAB_TEXT_ACTIVE = Color.WHITE;                    // #FFFFFF - Pure white
    public static final Color TAB_TEXT_INACTIVE = new Color(128, 128, 128);     // #808080 - Muted

    // Status Colors
    public static final Color STATUS_TEXT = new Color(128, 128, 128);           // #808080 - Status bar muted text

    // Spacing and Sizing (v2.8.1 - Increased for better breathing room)
    public static final int CORNER_RADIUS = 4;
    public static final int CORNER_RADIUS_LARGE = 6;
    public static final int SPACING_SMALL = 4;
    public static final int SPACING_MEDIUM = 8;
    public static final int SPACING_LARGE = 12;
    public static final int SPACING_XL = 16;
    public static final int BUTTON_GAP = 10;           // Gap between buttons in toolbar
    public static final int TOOLBAR_VPADDING = 12;     // Vertical padding for toolbar

    // Button Sizing (v2.8.1 - Proper visual hierarchy)
    public static final int BUTTON_HEIGHT_PRIMARY = 32;      // Execute, Save buttons
    public static final int BUTTON_HEIGHT_SECONDARY = 30;    // Other action buttons
    public static final int BUTTON_HEIGHT_SMALL = 24;        // A+/A- buttons
    public static final int BUTTON_PADDING_H_PRIMARY = 20;   // Horizontal padding for primary
    public static final int BUTTON_PADDING_H_SECONDARY = 16; // Horizontal padding for secondary
    public static final int BUTTON_PADDING_V_PRIMARY = 10;   // Vertical padding for primary
    public static final int BUTTON_PADDING_V_SECONDARY = 8;  // Vertical padding for secondary

    // Fonts (v2.8.1 - Increased editor font for readability)
    public static final Font FONT_REGULAR = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    public static final Font FONT_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    public static final Font FONT_BUTTON = new Font(Font.SANS_SERIF, Font.BOLD, 13);  // Slightly larger for buttons
    public static final Font FONT_MONOSPACE = new Font("Monospaced", Font.PLAIN, 14); // INCREASED from 12 to 14
    public static final Font FONT_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    public static final Font FONT_STATUS = new Font(Font.SANS_SERIF, Font.PLAIN, 10); // Smaller for status bar

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
