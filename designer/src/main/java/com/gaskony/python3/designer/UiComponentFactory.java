package com.gaskony.python3.designer;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Font;

/**
 * Factory methods for commonly created, pre-styled Swing components in the
 * Python 3 IDE.
 *
 * <p>Repeated boilerplate — read-only text areas, themed scroll panes — is
 * centralised here so individual panels only describe <em>what</em> a
 * component is for, not the low-level styling sequence.</p>
 */
public final class UiComponentFactory {

    private UiComponentFactory() {
        // Utility class — no instances
    }

    // -------------------------------------------------------------------------
    // Text areas
    // -------------------------------------------------------------------------

    /**
     * Creates a read-only, line-wrapping {@link JTextArea} styled for an output
     * or log panel.
     *
     * @param font       font to use (typically {@link ModernTheme#FONT_MONOSPACE})
     * @param background background colour
     * @param foreground foreground (text) colour
     * @return configured text area
     */
    public static JTextArea createOutputArea(Font font, Color background, Color foreground) {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(font);
        area.setBackground(background);
        area.setForeground(foreground);
        area.setCaretColor(foreground);
        area.setBorder(null);
        return area;
    }

    /**
     * Creates a dark-themed read-only output area using the standard monospace
     * font and {@link ModernTheme#BACKGROUND_DARKER} / {@link ModernTheme#FOREGROUND_PRIMARY}.
     *
     * @return configured dark output text area
     */
    public static JTextArea createDarkOutputArea() {
        return createOutputArea(
                ModernTheme.FONT_MONOSPACE,
                ModernTheme.BACKGROUND_DARKER,
                ModernTheme.FOREGROUND_PRIMARY
        );
    }

    /**
     * Creates a dark-themed read-only error area using the standard monospace
     * font and {@link ModernTheme#BACKGROUND_DARKER} / {@link ModernTheme#ERROR}.
     *
     * @return configured dark error text area
     */
    public static JTextArea createDarkErrorArea() {
        return createOutputArea(
                ModernTheme.FONT_MONOSPACE,
                ModernTheme.BACKGROUND_DARKER,
                ModernTheme.ERROR
        );
    }

    // -------------------------------------------------------------------------
    // Scroll panes
    // -------------------------------------------------------------------------

    /**
     * Creates a {@link JScrollPane} wrapping {@code view} with the given scroll
     * bar policies and background colour applied to both the pane and its
     * viewport.
     *
     * @param view             the component to wrap
     * @param vPolicy          vertical scroll bar policy constant
     * @param hPolicy          horizontal scroll bar policy constant
     * @param background       background colour for pane and viewport
     * @return configured scroll pane
     */
    public static JScrollPane createScrollPane(
            java.awt.Component view, int vPolicy, int hPolicy, Color background) {
        JScrollPane pane = new JScrollPane(view, vPolicy, hPolicy);
        pane.setBackground(background);
        pane.getViewport().setBackground(background);
        pane.setBorder(null);
        return pane;
    }
}
