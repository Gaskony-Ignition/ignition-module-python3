package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Custom tab button for replacing JTabbedPane.
 * Provides complete control over tab rendering with zero white gaps.
 *
 * v2.5.17: Created to eliminate JTabbedPane white rectangle issues
 */
public class CustomTabButton extends JPanel {
    private final JLabel label;
    private boolean selected = false;
    private boolean hovered = false;
    private Runnable clickAction;

    private static final Color BACKGROUND_SELECTED = new Color(30, 30, 30);      // Match editor
    private static final Color BACKGROUND_UNSELECTED = new Color(23, 23, 23);    // Darker
    private static final Color BACKGROUND_HOVER = new Color(35, 35, 35);         // Slightly lighter
    private static final Color FOREGROUND_SELECTED = new Color(204, 204, 204);   // Bright
    private static final Color FOREGROUND_UNSELECTED = new Color(150, 150, 150); // Dimmer
    private static final Color BORDER_COLOR = new Color(64, 64, 64);             // Subtle grey

    public CustomTabButton(String text) {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(BACKGROUND_UNSELECTED);

        // Create label
        label = new JLabel(text);
        label.setFont(ModernTheme.FONT_REGULAR);
        label.setForeground(FOREGROUND_UNSELECTED);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));

        add(label, BorderLayout.CENTER);

        // Mouse listeners for hover and click
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!selected) {
                    hovered = true;
                    updateAppearance();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                updateAppearance();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (clickAction != null) {
                    clickAction.run();
                }
            }
        });

        // Set preferred size
        setPreferredSize(new Dimension(100, 32));
        setMinimumSize(new Dimension(80, 32));
    }

    /**
     * Sets whether this tab is selected.
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
        updateAppearance();
    }

    /**
     * Returns whether this tab is selected.
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets the action to run when this tab is clicked.
     */
    public void setClickAction(Runnable action) {
        this.clickAction = action;
    }

    /**
     * Updates the visual appearance based on state.
     */
    private void updateAppearance() {
        if (selected) {
            setBackground(BACKGROUND_SELECTED);
            label.setForeground(FOREGROUND_SELECTED);
            setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ModernTheme.ACCENT_PRIMARY));
        } else if (hovered) {
            setBackground(BACKGROUND_HOVER);
            label.setForeground(FOREGROUND_SELECTED);
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        } else {
            setBackground(BACKGROUND_UNSELECTED);
            label.setForeground(FOREGROUND_UNSELECTED);
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        }
        repaint();
    }
}
