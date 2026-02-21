package com.inductiveautomation.ignition.examples.python3.designer.ui;

import com.inductiveautomation.ignition.examples.python3.designer.ModernTheme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionListener;

/**
 * A panel wrapper that can be collapsed/expanded with a button.
 * Useful for side panels that users may want to hide for more screen space.
 *
 * @since v2.8.0
 */
public class CollapsiblePanel extends JPanel {
    private final JPanel contentPanel;
    private final JButton collapseButton;
    private final String title;
    private boolean collapsed = false;
    private int expandedWidth = 250;  // Default width when expanded

    /**
     * Creates a new collapsible panel.
     *
     * @param title         the panel title
     * @param contentPanel  the panel content to wrap
     */
    public CollapsiblePanel(String title, JPanel contentPanel) {
        this.title = title;
        this.contentPanel = contentPanel;
        this.collapseButton = createCollapseButton();

        initializeUI();
    }

    /**
     * Initializes the UI layout.
     */
    private void initializeUI() {
        setLayout(new BorderLayout(0, 0));
        setBackground(ModernTheme.BACKGROUND_DARK);

        // Header with collapse button
        JPanel header = new JPanel(new BorderLayout(5, 0));
        header.setBackground(ModernTheme.PANEL_BACKGROUND);
        header.setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(ModernTheme.FONT_BOLD);
        titleLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);

        header.add(titleLabel, BorderLayout.CENTER);
        header.add(collapseButton, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * Creates the collapse/expand button.
     */
    private JButton createCollapseButton() {
        JButton button = new JButton("◀");  // Left arrow (collapsed)
        button.setFont(ModernTheme.FONT_BOLD);
        button.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        button.setBackground(ModernTheme.BUTTON_BACKGROUND);
        button.setBorder(new EmptyBorder(2, 8, 2, 8));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText("Collapse Panel");

        button.addActionListener(e -> toggleCollapse());

        return button;
    }

    /**
     * Toggles the collapsed state.
     */
    public void toggleCollapse() {
        setCollapsed(!collapsed);
    }

    /**
     * Sets the collapsed state.
     *
     * @param collapsed true to collapse, false to expand
     */
    public void setCollapsed(boolean collapsed) {
        if (this.collapsed == collapsed) {
            return;  // No change
        }

        this.collapsed = collapsed;

        if (collapsed) {
            // Remember current width before collapsing
            expandedWidth = getWidth();

            // Hide content, show only header
            contentPanel.setVisible(false);
            collapseButton.setText("▶");  // Right arrow (expand)
            collapseButton.setToolTipText("Expand Panel");

            // Set to minimum width (just header)
            setPreferredSize(new Dimension(30, getHeight()));
            setMinimumSize(new Dimension(30, 0));
            setMaximumSize(new Dimension(30, Integer.MAX_VALUE));
        } else {
            // Show content
            contentPanel.setVisible(true);
            collapseButton.setText("◀");  // Left arrow (collapse)
            collapseButton.setToolTipText("Collapse Panel");

            // Restore width
            setPreferredSize(new Dimension(expandedWidth, getHeight()));
            setMinimumSize(new Dimension(200, 0));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        }

        revalidate();
        repaint();

        // Notify parent container to relayout
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    /**
     * Checks if the panel is collapsed.
     *
     * @return true if collapsed
     */
    public boolean isCollapsed() {
        return collapsed;
    }

    /**
     * Sets the expanded width (used when expanding).
     *
     * @param width the width in pixels
     */
    public void setExpandedWidth(int width) {
        this.expandedWidth = width;
    }

    /**
     * Adds an action listener that is notified when collapse state changes.
     *
     * @param listener the listener to add
     */
    public void addCollapseListener(ActionListener listener) {
        collapseButton.addActionListener(listener);
    }
}
