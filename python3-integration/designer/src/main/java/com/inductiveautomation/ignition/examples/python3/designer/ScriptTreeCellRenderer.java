package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Custom tree cell renderer for the script browser.
 * Styled to match Ignition's Tag Browser with professional icons and selection colors.
 */
public class ScriptTreeCellRenderer extends DefaultTreeCellRenderer {

    // Ignition Tag Browser-style colors
    private static final Color IGNITION_BLUE_SELECTION = new Color(51, 153, 255);

    // Use UIManager's default tree icons for professional appearance
    private static final Icon FOLDER_ICON = UIManager.getIcon("Tree.closedIcon");
    private static final Icon FOLDER_OPEN_ICON = UIManager.getIcon("Tree.openIcon");
    private static final Icon SCRIPT_ICON = UIManager.getIcon("Tree.leafIcon");

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        // Apply Ignition Tag Browser-style selection colors
        if (selected) {
            setBackground(IGNITION_BLUE_SELECTION);
            setForeground(Color.WHITE);
            setOpaque(true);
        } else {
            setBackground(tree.getBackground());
            setForeground(tree.getForeground());
            setOpaque(false);
        }

        // Use consistent 12pt font for all items (like Ignition Tag Browser)
        setFont(new Font("SansSerif", Font.PLAIN, 12));

        // Standard icon-text spacing
        setIconTextGap(4);

        if (value instanceof ScriptTreeNode) {
            ScriptTreeNode node = (ScriptTreeNode) value;

            if (node.isFolder()) {
                setIcon(expanded ? FOLDER_OPEN_ICON : FOLDER_ICON);
            } else if (node.isScript()) {
                setIcon(SCRIPT_ICON);
            }
        }

        // Minimal padding for compact appearance
        setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));

        return this;
    }
}
