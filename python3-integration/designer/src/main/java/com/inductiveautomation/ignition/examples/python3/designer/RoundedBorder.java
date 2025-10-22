package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom border with rounded corners for modern UI appearance.
 * Supports customizable radius, color, and thickness.
 */
public class RoundedBorder extends AbstractBorder {
    private final Color color;
    private final int radius;
    private final int thickness;
    private final Insets insets;

    /**
     * Creates a rounded border with default settings.
     */
    public RoundedBorder() {
        this(ModernTheme.BORDER_DEFAULT, ModernTheme.CORNER_RADIUS, 1);
    }

    /**
     * Creates a rounded border with specified color.
     *
     * @param color the border color
     */
    public RoundedBorder(Color color) {
        this(color, ModernTheme.CORNER_RADIUS, 1);
    }

    /**
     * Creates a rounded border with specified color and radius.
     *
     * @param color  the border color
     * @param radius the corner radius in pixels
     */
    public RoundedBorder(Color color, int radius) {
        this(color, radius, 1);
    }

    /**
     * Creates a fully customized rounded border.
     *
     * @param color     the border color
     * @param radius    the corner radius in pixels
     * @param thickness the border thickness in pixels
     */
    public RoundedBorder(Color color, int radius, int thickness) {
        this.color = color;
        this.radius = radius;
        this.thickness = thickness;
        this.insets = new Insets(thickness + 2, thickness + 2, thickness + 2, thickness + 2);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g.create();

        // Enable anti-aliasing for smooth rounded corners
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw rounded rectangle border
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(thickness));

        int adjustedWidth = width - thickness;
        int adjustedHeight = height - thickness;
        int adjustedX = x + thickness / 2;
        int adjustedY = y + thickness / 2;

        RoundRectangle2D roundedRectangle = new RoundRectangle2D.Double(
                adjustedX,
                adjustedY,
                adjustedWidth,
                adjustedHeight,
                radius,
                radius
        );

        g2d.draw(roundedRectangle);
        g2d.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return insets;
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = this.insets.left;
        insets.top = this.insets.top;
        insets.right = this.insets.right;
        insets.bottom = this.insets.bottom;
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

    // === Factory Methods ===

    /**
     * Creates a default rounded border.
     */
    public static RoundedBorder createDefault() {
        return new RoundedBorder();
    }

    /**
     * Creates a focused border (highlighted).
     */
    public static RoundedBorder createFocused() {
        return new RoundedBorder(ModernTheme.BORDER_FOCUSED, ModernTheme.CORNER_RADIUS, 2);
    }

    /**
     * Creates a hover border.
     */
    public static RoundedBorder createHover() {
        return new RoundedBorder(ModernTheme.BORDER_HOVER, ModernTheme.CORNER_RADIUS);
    }

    /**
     * Creates a large radius border (for panels).
     */
    public static RoundedBorder createLarge() {
        return new RoundedBorder(ModernTheme.BORDER_DEFAULT, ModernTheme.CORNER_RADIUS_LARGE);
    }

    /**
     * Creates an accent border (primary color).
     */
    public static RoundedBorder createAccent() {
        return new RoundedBorder(ModernTheme.ACCENT_PRIMARY, ModernTheme.CORNER_RADIUS, 2);
    }
}
