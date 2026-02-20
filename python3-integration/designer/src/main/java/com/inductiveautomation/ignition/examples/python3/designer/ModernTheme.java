package com.inductiveautomation.ignition.examples.python3.designer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modern UI theme with color palette aligned to the Gateway Web UI.
 * Provides consistent colors and styles across the Designer IDE.
 *
 * v3.6.0: Colors aligned to web CSS variables (--bg-primary, --text-primary, etc.)
 * v3.6.0: Font resolution with JetBrains Mono bundled, Inter for UI
 */
public class ModernTheme {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModernTheme.class);

    // === Color Palette (v3.6.0 - Aligned to Gateway Web UI CSS) ===

    // Primary Backgrounds - from web CSS --bg-primary, --bg-secondary, --bg-tertiary
    public static final Color BACKGROUND_DARKER = new Color(0x0a, 0x0e, 0x14);     // #0a0e14 - --bg-primary (darkest)
    public static final Color EDITOR_BACKGROUND = new Color(0x14, 0x18, 0x1f);     // #14181f - --bg-secondary (editor)
    public static final Color PANEL_BACKGROUND = new Color(0x14, 0x18, 0x1f);      // #14181f - --bg-secondary (panels)
    public static final Color BACKGROUND_LIGHT = new Color(0x1a, 0x1f, 0x28);      // #1a1f28 - --bg-tertiary (lighter panels)
    public static final Color SIDEBAR_BACKGROUND = new Color(0x0a, 0x0e, 0x14);    // #0a0e14 - --bg-primary (sidebar)
    public static final Color BACKGROUND_DARK = new Color(0x0a, 0x0e, 0x14);       // #0a0e14 - --bg-primary (general)

    // Foreground Colors - from web CSS --text-primary, --text-secondary, --text-tertiary
    public static final Color FOREGROUND_PRIMARY = new Color(0xe6, 0xe8, 0xeb);    // #e6e8eb - --text-primary
    public static final Color FOREGROUND_SECONDARY = new Color(0xb3, 0xb8, 0xc4);  // #b3b8c4 - --text-secondary
    public static final Color FOREGROUND_MUTED = new Color(0x7d, 0x85, 0x94);      // #7d8594 - --text-tertiary

    // Accent Colors - from web CSS --accent-primary
    public static final Color ACCENT_PRIMARY = new Color(0x61, 0xaf, 0xef);         // #61afef - --accent-primary
    public static final Color ACCENT_HOVER = new Color(0x7b, 0xbf, 0xf3);           // #7bbff3 - lighter on hover
    public static final Color ACCENT_ACTIVE = new Color(0x4a, 0x9a, 0xdf);          // #4a9adf - darker when active

    // Semantic Colors - from web CSS --accent-secondary, --accent-error, --accent-warning
    public static final Color SUCCESS = new Color(0x98, 0xc3, 0x79);                // #98c379 - --accent-secondary
    public static final Color WARNING = new Color(0xe5, 0xc0, 0x7b);                // #e5c07b - --accent-warning
    public static final Color ERROR = new Color(0xe0, 0x6c, 0x75);                  // #e06c75 - --accent-error
    public static final Color ERROR_BRIGHT = new Color(0xef, 0x7a, 0x83);           // #ef7a83 - brighter error
    public static final Color INFO = new Color(0x61, 0xaf, 0xef);                   // #61afef - matches accent

    // Border Colors - from web CSS --border-default, --border-light
    public static final Color BORDER_SUBTLE = new Color(0x20, 0x25, 0x2e);          // #20252e - --border-default
    public static final Color BORDER_DEFAULT = new Color(0x2a, 0x30, 0x39);         // #2a3039 - --border-light
    public static final Color BORDER_FOCUSED = new Color(0x61, 0xaf, 0xef);         // #61afef - accent on focus
    public static final Color BORDER_HOVER = new Color(0x35, 0x3c, 0x48);           // #353c48 - lighter on hover

    // UI Element Colors
    public static final Color BUTTON_BACKGROUND = new Color(0x1a, 0x1f, 0x28);     // #1a1f28 - --bg-tertiary
    public static final Color BUTTON_HOVER = new Color(0x24, 0x2a, 0x34);           // #242a34 - lighter on hover
    public static final Color BUTTON_ACTIVE = new Color(0x14, 0x18, 0x1f);          // #14181f - darker when active

    public static final Color INPUT_BACKGROUND = new Color(0x14, 0x18, 0x1f);      // #14181f - --bg-secondary
    public static final Color INPUT_BORDER = new Color(0x2a, 0x30, 0x39);           // #2a3039 - --border-light

    public static final Color PANEL_BORDER = new Color(0x20, 0x25, 0x2e);           // #20252e - --border-default

    // Tree Colors
    public static final Color TREE_BACKGROUND = new Color(0x0a, 0x0e, 0x14);       // #0a0e14 - --bg-primary
    public static final Color TREE_SELECTION = new Color(0x1e, 0x33, 0x50);         // #1e3350 - blue-tinted selection
    public static final Color TREE_HOVER = new Color(0x14, 0x1a, 0x24);             // #141a24 - subtle hover

    // Tab Colors
    public static final Color TAB_ACTIVE_BG = new Color(0x14, 0x18, 0x1f);          // #14181f - matches editor
    public static final Color TAB_INACTIVE_BG = new Color(0x0a, 0x0e, 0x14);        // #0a0e14 - darker
    public static final Color TAB_TEXT_ACTIVE = FOREGROUND_PRIMARY;
    public static final Color TAB_TEXT_INACTIVE = FOREGROUND_MUTED;

    // Status Colors
    public static final Color STATUS_TEXT = FOREGROUND_MUTED;

    // Spacing and Sizing (v3.6.0 - Increased for modern spacing)
    public static final int CORNER_RADIUS = 6;
    public static final int CORNER_RADIUS_LARGE = 8;
    public static final int SPACING_SMALL = 4;
    public static final int SPACING_MEDIUM = 8;
    public static final int SPACING_LARGE = 16;
    public static final int SPACING_XL = 24;
    public static final int BUTTON_GAP = 6;
    public static final int TOOLBAR_VPADDING = 12;

    // Button Sizing (v3.6.0 - Increased primary height)
    public static final int BUTTON_HEIGHT_PRIMARY = 40;
    public static final int BUTTON_HEIGHT_SECONDARY = 34;
    public static final int BUTTON_HEIGHT_SMALL = 28;
    public static final int BUTTON_PADDING_H_PRIMARY = 16;
    public static final int BUTTON_PADDING_H_SECONDARY = 12;
    public static final int BUTTON_PADDING_V_PRIMARY = 10;
    public static final int BUTTON_PADDING_V_SECONDARY = 8;

    // === Font Resolution (v3.6.0) ===

    /** Resolved code font - JetBrains Mono preferred */
    public static final Font FONT_CODE;

    /** Resolved UI font - Inter preferred */
    public static final Font FONT_UI;

    // Standard font variants derived from resolved fonts
    public static final Font FONT_REGULAR;
    public static final Font FONT_BOLD;
    public static final Font FONT_BUTTON;
    public static final Font FONT_MONOSPACE;
    public static final Font FONT_TITLE;
    public static final Font FONT_STATUS;

    static {
        // Register bundled JetBrains Mono font
        Font registeredCodeFont = null;
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            InputStream regular = ModernTheme.class.getResourceAsStream("/fonts/JetBrainsMono-Regular.ttf");
            InputStream bold = ModernTheme.class.getResourceAsStream("/fonts/JetBrainsMono-Bold.ttf");

            if (regular != null) {
                Font regFont = Font.createFont(Font.TRUETYPE_FONT, regular);
                ge.registerFont(regFont);
                registeredCodeFont = regFont;
                regular.close();
            }
            if (bold != null) {
                Font boldFont = Font.createFont(Font.TRUETYPE_FONT, bold);
                ge.registerFont(boldFont);
                bold.close();
            }
        } catch (FontFormatException | IOException e) {
            LOGGER.warn("Could not load bundled JetBrains Mono font", e);
        }

        // Resolve code font with fallback chain
        FONT_CODE = resolveCodeFont(registeredCodeFont);
        FONT_MONOSPACE = FONT_CODE.deriveFont(14f);

        // Resolve UI font with fallback chain
        FONT_UI = resolveUiFont();
        FONT_REGULAR = FONT_UI.deriveFont(Font.PLAIN, 12f);
        FONT_BOLD = FONT_UI.deriveFont(Font.BOLD, 12f);
        FONT_BUTTON = FONT_UI.deriveFont(Font.BOLD, 13f);
        FONT_TITLE = FONT_UI.deriveFont(Font.BOLD, 14f);
        FONT_STATUS = FONT_UI.deriveFont(Font.PLAIN, 10f);

        LOGGER.info("ModernTheme initialized: code font='{}', ui font='{}'",
            FONT_CODE.getFontName(), FONT_UI.getFontName());
    }

    /**
     * Resolves the best available code font.
     * Fallback chain: JetBrains Mono -> Cascadia Code -> Fira Code -> Consolas -> Monospaced
     */
    private static Font resolveCodeFont(Font registeredFont) {
        if (registeredFont != null) {
            return registeredFont.deriveFont(14f);
        }
        String[] codeFonts = {"JetBrains Mono", "Cascadia Code", "Fira Code", "Consolas"};
        for (String name : codeFonts) {
            Font f = new Font(name, Font.PLAIN, 14);
            if (!f.getFontName().equals(Font.DIALOG)) {
                return f;
            }
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, 14);
    }

    /**
     * Resolves the best available UI font.
     * Fallback chain: Inter -> Segoe UI -> SansSerif
     */
    private static Font resolveUiFont() {
        String[] uiFonts = {"Inter", "Segoe UI"};
        for (String name : uiFonts) {
            Font f = new Font(name, Font.PLAIN, 12);
            if (!f.getFontName().equals(Font.DIALOG)) {
                return f;
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    }

    // === Utility Methods ===

    /**
     * Creates a semi-transparent version of a color.
     */
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /**
     * Lightens a color by a percentage.
     */
    public static Color lighten(Color color, double percent) {
        int r = Math.min(255, (int) (color.getRed() + (255 - color.getRed()) * percent));
        int g = Math.min(255, (int) (color.getGreen() + (255 - color.getGreen()) * percent));
        int b = Math.min(255, (int) (color.getBlue() + (255 - color.getBlue()) * percent));
        return new Color(r, g, b);
    }

    /**
     * Darkens a color by a percentage.
     */
    public static Color darken(Color color, double percent) {
        int r = Math.max(0, (int) (color.getRed() * (1 - percent)));
        int g = Math.max(0, (int) (color.getGreen() * (1 - percent)));
        int b = Math.max(0, (int) (color.getBlue() * (1 - percent)));
        return new Color(r, g, b);
    }

    /**
     * Returns a font with the specified size.
     */
    public static Font withSize(Font baseFont, int size) {
        return baseFont.deriveFont((float) size);
    }

    /**
     * Returns a bold version of the font.
     */
    public static Font bold(Font baseFont) {
        return baseFont.deriveFont(Font.BOLD);
    }

    // === Component Styling Helpers ===

    /**
     * Applies modern theme to a component.
     */
    public static void applyToComponent(Component component) {
        component.setBackground(PANEL_BACKGROUND);
        component.setForeground(FOREGROUND_PRIMARY);
        component.setFont(FONT_REGULAR);
    }

    /**
     * Applies modern theme to a button.
     */
    public static void applyToButton(Component button) {
        button.setBackground(BUTTON_BACKGROUND);
        button.setForeground(FOREGROUND_PRIMARY);
        button.setFont(FONT_BOLD);
    }

    /**
     * Gets status color for a given message type.
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
