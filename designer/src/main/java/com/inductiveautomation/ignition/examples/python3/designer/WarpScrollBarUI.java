package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Ultra-minimal scrollbar UI inspired by Warp terminal.
 *
 * Features:
 * - Completely invisible track (no background)
 * - No arrow buttons
 * - Tiny indicator (3px wide, expands to 6px on hover)
 * - Auto-hides after 1.5 seconds of inactivity
 * - Only appears when scrolling or on hover
 * - Extremely subtle - doesn't distract from content
 *
 * v2.4.1: Created for Warp-inspired minimal UX
 */
public class WarpScrollBarUI extends BasicScrollBarUI {

    private static final int THUMB_MIN_WIDTH = 3;      // Minimal width (barely visible)
    private static final int THUMB_HOVER_WIDTH = 6;    // Expanded on hover
    private static final int THUMB_RADIUS = 2;         // Rounded corners
    private static final int FADE_DELAY = 1500;        // 1.5 seconds before hiding

    private boolean isHovered = false;
    private boolean isVisible = false;
    private int currentAlpha = 0;                     // 0 = invisible, 255 = fully visible
    private Timer fadeTimer;
    private Timer fadeOutTimer;

    @Override
    protected void installListeners() {
        super.installListeners();

        // Show on mouse hover
        scrollbar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                showScrollbar();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                scheduleHide();
            }
        });

        // Show when scrolling
        scrollbar.addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                showScrollbar();
                scheduleHide();
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
     * Creates a zero-size invisible button (no arrows).
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
        // Completely invisible track - paint nothing
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }

        // Don't paint if invisible
        if (currentAlpha == 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Calculate thumb size based on hover state
        int thumbWidth = isHovered ? THUMB_HOVER_WIDTH : THUMB_MIN_WIDTH;

        // Very subtle grey color with alpha
        Color thumbColor = new Color(150, 150, 150, currentAlpha);
        g2.setColor(thumbColor);

        // Calculate centered position for thumb
        int x, y, width, height;

        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            // Vertical scrollbar - center horizontally, full height
            width = thumbWidth;
            height = thumbBounds.height;
            x = thumbBounds.x + (thumbBounds.width - width) / 2;
            y = thumbBounds.y;
        } else {
            // Horizontal scrollbar - center vertically, full width
            width = thumbBounds.width;
            height = thumbWidth;
            x = thumbBounds.x;
            y = thumbBounds.y + (thumbBounds.height - height) / 2;
        }

        // Draw tiny rounded indicator
        g2.fillRoundRect(x, y, width, height, THUMB_RADIUS, THUMB_RADIUS);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        // Minimal size
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            return new Dimension(THUMB_HOVER_WIDTH, 48);
        } else {
            return new Dimension(48, THUMB_HOVER_WIDTH);
        }
    }

    /**
     * Shows the scrollbar with fade-in animation.
     */
    private void showScrollbar() {
        isVisible = true;

        // Cancel any existing hide timer
        if (fadeOutTimer != null && fadeOutTimer.isRunning()) {
            fadeOutTimer.stop();
        }

        // Fade in quickly
        if (fadeTimer != null && fadeTimer.isRunning()) {
            fadeTimer.stop();
        }

        fadeTimer = new Timer(16, e -> {  // ~60fps
            currentAlpha = Math.min(255, currentAlpha + 30);  // Fade in fast
            scrollbar.repaint();

            if (currentAlpha >= 255) {
                ((Timer) e.getSource()).stop();
            }
        });
        fadeTimer.start();
    }

    /**
     * Schedules the scrollbar to hide after delay.
     */
    private void scheduleHide() {
        if (fadeOutTimer != null && fadeOutTimer.isRunning()) {
            fadeOutTimer.restart();
            return;
        }

        fadeOutTimer = new Timer(FADE_DELAY, e -> {
            if (!isHovered) {
                hideScrollbar();
            }
        });
        fadeOutTimer.setRepeats(false);
        fadeOutTimer.start();
    }

    /**
     * Hides the scrollbar with fade-out animation.
     */
    private void hideScrollbar() {
        isVisible = false;

        // Fade out slowly
        if (fadeTimer != null && fadeTimer.isRunning()) {
            fadeTimer.stop();
        }

        fadeTimer = new Timer(16, e -> {  // ~60fps
            currentAlpha = Math.max(0, currentAlpha - 15);  // Fade out slower
            scrollbar.repaint();

            if (currentAlpha <= 0) {
                ((Timer) e.getSource()).stop();
            }
        });
        fadeTimer.start();
    }
}
