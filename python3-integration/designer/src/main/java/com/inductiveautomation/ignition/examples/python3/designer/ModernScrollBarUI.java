package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Modern, minimal scrollbar UI for a sleek, unobtrusive design.
 *
 * Features:
 * - Thin scrollbar track (invisible until hover)
 * - Rounded, semi-transparent thumb
 * - Smooth hover effects
 * - Minimal width (8px, expands to 10px on hover)
 * - Matches modern IDE aesthetics (VS Code, IntelliJ)
 *
 * v2.4.0: Created for improved UX - sleek modern scrollbars
 */
public class ModernScrollBarUI extends BasicScrollBarUI {

    private static final int THUMB_SIZE = 8;
    private static final int THUMB_SIZE_HOVER = 10;
    private static final int THUMB_RADIUS = 4;
    private static final int TRACK_ALPHA = 0;  // Invisible track
    private static final int THUMB_ALPHA = 120;
    private static final int THUMB_ALPHA_HOVER = 180;

    private boolean isHovered = false;
    private boolean isDarkTheme = true;

    /**
     * Creates a modern scrollbar UI.
     *
     * @param isDarkTheme true for dark theme colors, false for light theme
     */
    public ModernScrollBarUI(boolean isDarkTheme) {
        this.isDarkTheme = isDarkTheme;
    }

    @Override
    protected void installListeners() {
        super.installListeners();

        // Add hover listener for thumb size expansion
        scrollbar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                scrollbar.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                scrollbar.repaint();
            }
        });
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createInvisibleButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createInvisibleButton();
    }

    /**
     * Creates an invisible button (no arrows, minimal space).
     *
     * @return invisible zero-size button
     */
    private JButton createInvisibleButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Invisible track (only visible on hover if desired)
        Color trackColor = isDarkTheme
            ? new Color(30, 30, 30, TRACK_ALPHA)
            : new Color(240, 240, 240, TRACK_ALPHA);

        g2.setColor(trackColor);
        g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);

        g2.dispose();
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }

        // Determine thumb size based on hover state
        int thumbSize = isHovered ? THUMB_SIZE_HOVER : THUMB_SIZE;
        int alpha = isHovered ? THUMB_ALPHA_HOVER : THUMB_ALPHA;

        // Thumb color (semi-transparent)
        Color thumbColor = isDarkTheme
            ? new Color(150, 150, 150, alpha)
            : new Color(100, 100, 100, alpha);

        g2.setColor(thumbColor);

        // Calculate centered position for thumb
        int x, y, width, height;

        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            // Vertical scrollbar - center horizontally
            width = thumbSize;
            height = thumbBounds.height;
            x = thumbBounds.x + (thumbBounds.width - width) / 2;
            y = thumbBounds.y;
        } else {
            // Horizontal scrollbar - center vertically
            width = thumbBounds.width;
            height = thumbSize;
            x = thumbBounds.x;
            y = thumbBounds.y + (thumbBounds.height - height) / 2;
        }

        // Draw rounded thumb
        g2.fillRoundRect(x, y, width, height, THUMB_RADIUS, THUMB_RADIUS);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        // Thin scrollbar
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            return new Dimension(THUMB_SIZE_HOVER, 48);
        } else {
            return new Dimension(48, THUMB_SIZE_HOVER);
        }
    }

    /**
     * Sets the theme for the scrollbar.
     *
     * @param isDark true for dark theme, false for light theme
     */
    public void setDarkTheme(boolean isDark) {
        this.isDarkTheme = isDark;
        if (scrollbar != null) {
            scrollbar.repaint();
        }
    }
}
