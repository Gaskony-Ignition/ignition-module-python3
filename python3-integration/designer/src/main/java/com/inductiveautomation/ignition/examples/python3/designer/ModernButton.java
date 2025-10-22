package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern button with rounded corners, hover effects, and smooth animations.
 * Styled to match VS Code / Cursor / Warp aesthetic.
 */
public class ModernButton extends JButton {
    private boolean isHovered = false;
    private boolean isPressed = false;
    private final int cornerRadius;
    private Color normalBackground;
    private Color hoverBackground;
    private Color pressedBackground;

    /**
     * Creates a modern button with default text.
     *
     * @param text the button text
     */
    public ModernButton(String text) {
        this(text, ModernTheme.CORNER_RADIUS);
    }

    /**
     * Creates a modern button with custom corner radius.
     *
     * @param text         the button text
     * @param cornerRadius the corner radius in pixels
     */
    public ModernButton(String text, int cornerRadius) {
        super(text);
        this.cornerRadius = cornerRadius;
        this.normalBackground = ModernTheme.BUTTON_BACKGROUND;
        this.hoverBackground = ModernTheme.BUTTON_HOVER;
        this.pressedBackground = ModernTheme.BUTTON_ACTIVE;

        initButton();
    }

    /**
     * Initializes button appearance and behavior.
     */
    private void initButton() {
        setFont(ModernTheme.FONT_BOLD);
        setForeground(ModernTheme.FOREGROUND_PRIMARY);
        setBackground(normalBackground);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);

        // Add padding - generous for text readability
        setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        // Add hover and press listeners
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });

        // Auto-size based on text content, but set minimum size
        setMinimumSize(new Dimension(80, 28));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();

        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Determine background color based on state
        Color bgColor = normalBackground;
        if (!isEnabled()) {
            bgColor = ModernTheme.darken(normalBackground, 0.3);
        } else if (isPressed) {
            bgColor = pressedBackground;
        } else if (isHovered) {
            bgColor = hoverBackground;
        }

        // Draw rounded background
        RoundRectangle2D roundedRect = new RoundRectangle2D.Double(
                0, 0,
                getWidth(), getHeight(),
                cornerRadius, cornerRadius
        );

        g2d.setColor(bgColor);
        g2d.fill(roundedRect);

        // Draw border for focused state
        if (isFocusOwner()) {
            g2d.setColor(ModernTheme.BORDER_FOCUSED);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(roundedRect);
        }

        g2d.dispose();

        // Paint text and icon
        super.paintComponent(g);
    }

    // === Style Setters ===

    /**
     * Sets the normal background color.
     */
    public void setNormalBackground(Color color) {
        this.normalBackground = color;
        repaint();
    }

    /**
     * Sets the hover background color.
     */
    public void setHoverBackground(Color color) {
        this.hoverBackground = color;
    }

    /**
     * Sets the pressed background color.
     */
    public void setPressedBackground(Color color) {
        this.pressedBackground = color;
    }

    // === Factory Methods ===

    /**
     * Creates a primary button with accent color.
     */
    public static ModernButton createPrimary(String text) {
        ModernButton button = new ModernButton(text);
        button.setNormalBackground(ModernTheme.ACCENT_PRIMARY);
        button.setHoverBackground(ModernTheme.ACCENT_HOVER);
        button.setPressedBackground(ModernTheme.ACCENT_ACTIVE);
        return button;
    }

    /**
     * Creates a success button (green).
     */
    public static ModernButton createSuccess(String text) {
        ModernButton button = new ModernButton(text);
        button.setNormalBackground(ModernTheme.SUCCESS);
        button.setHoverBackground(ModernTheme.lighten(ModernTheme.SUCCESS, 0.1));
        button.setPressedBackground(ModernTheme.darken(ModernTheme.SUCCESS, 0.1));
        return button;
    }

    /**
     * Creates a warning button (orange).
     */
    public static ModernButton createWarning(String text) {
        ModernButton button = new ModernButton(text);
        button.setNormalBackground(ModernTheme.WARNING);
        button.setHoverBackground(ModernTheme.lighten(ModernTheme.WARNING, 0.1));
        button.setPressedBackground(ModernTheme.darken(ModernTheme.WARNING, 0.1));
        return button;
    }

    /**
     * Creates a danger button (red).
     */
    public static ModernButton createDanger(String text) {
        ModernButton button = new ModernButton(text);
        button.setNormalBackground(ModernTheme.ERROR);
        button.setHoverBackground(ModernTheme.lighten(ModernTheme.ERROR, 0.1));
        button.setPressedBackground(ModernTheme.darken(ModernTheme.ERROR, 0.1));
        return button;
    }

    /**
     * Creates a default button (neutral gray).
     */
    public static ModernButton createDefault(String text) {
        return new ModernButton(text);
    }

    /**
     * Creates a small button with reduced padding.
     */
    public static ModernButton createSmall(String text) {
        ModernButton button = new ModernButton(text);
        button.setFont(ModernTheme.withSize(ModernTheme.FONT_BOLD, 11));
        button.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        button.setMinimumSize(new Dimension(70, 24));
        return button;
    }

    /**
     * Creates a large button with increased padding.
     */
    public static ModernButton createLarge(String text) {
        ModernButton button = new ModernButton(text, ModernTheme.CORNER_RADIUS_LARGE);
        button.setFont(ModernTheme.withSize(ModernTheme.FONT_BOLD, 14));
        button.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        button.setMinimumSize(new Dimension(120, 44));
        return button;
    }
}
