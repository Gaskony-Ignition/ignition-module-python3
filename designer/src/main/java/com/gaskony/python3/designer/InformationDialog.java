package com.gaskony.python3.designer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Information dialog for Python 3 IDE showing comprehensive user guide.
 *
 * Displays:
 * - Keyboard shortcuts
 * - Workflows (IDE to production scripts)
 * - Feature highlights
 * - Tips and best practices
 *
 * v2.5.1: Created to provide in-app help for new users
 * v3.6.10: Refactored to delegate shared theme/dialog methods to DarkDialog
 */
public class InformationDialog {

    /**
     * Shows the information dialog with comprehensive user guide.
     *
     * @param parent parent component
     */
    public static void show(Component parent) {
        JDialog dialog = DarkDialog.createBaseDialog(parent, "Python 3 IDE - User Guide");

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(DarkDialog.getBackground());
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Create scrollable content area
        JPanel infoPanel = createInfoPanel();
        JScrollPane scrollPane = new JScrollPane(infoPanel);
        scrollPane.setBackground(DarkDialog.getBackground());
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Close button
        JButton closeButton = DarkDialog.createThemedButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(DarkDialog.getBackground());
        buttonPanel.add(closeButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.setSize(700, 600);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static JPanel createInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(DarkDialog.getBackground());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Keyboard Shortcuts (matching web version from info.png)
        addHeading(panel, "Keyboard Shortcuts", 16, true);
        addShortcut(panel, "Ctrl+Enter", "Execute code on Gateway");
        addShortcut(panel, "Ctrl+S", "Save current script");
        addShortcut(panel, "Ctrl+Shift+S", "Save As (with metadata)");
        addShortcut(panel, "Ctrl+N", "New script (clear editor)");
        addShortcut(panel, "Ctrl+F", "Find in editor");
        addShortcut(panel, "Ctrl+H", "Find and Replace");
        addShortcut(panel, "Ctrl++", "Increase font size");
        addShortcut(panel, "Ctrl+-", "Decrease font size");

        addSpacer(panel, 15);

        // Features (matching web version from info.png)
        addHeading(panel, "Features", 16, false);
        addText(panel, "• Syntax Highlighting: Full Python 3 syntax support with Monaco Editor");
        addText(panel, "• Script Management: Save, load, and organize scripts in folders");
        addText(panel, "• Real-time Execution: Execute code on Gateway and see results instantly");
        addText(panel, "• Error Reporting: Detailed Python tracebacks in Error panel");
        addText(panel, "• Performance Monitoring: View pool stats, memory, and CPU usage");
        addText(panel, "• Theme Support: Multiple editor themes available");

        addSpacer(panel, 15);

        // Workflow: IDE to Production (matching web version from info.png)
        addHeading(panel, "Workflow: IDE to Production", 16, false);
        addText(panel, "1. Develop and test code in IDE");
        addText(panel, "2. Save working scripts with metadata");
        addText(panel, "3. Copy tested code to Production scripts (Project Library, Tag Event Scripts, etc.)");
        addText(panel, "4. Use system.python3.exec() in production to execute Python 3 code");

        addSpacer(panel, 10);
        addAccent(panel, "Python 3 Integration Module v" + getModuleVersion());
        addAccent(panel, "Built with Ignition SDK 8.3 | Developed by Gaskony with Claude Code");

        return panel;
    }

    // Helper methods for consistent formatting

    private static void addHeading(JPanel panel, String text, int fontSize, boolean first) {
        JLabel label = new JLabel(text);
        label.setForeground(getHeadingColor());
        label.setFont(ModernTheme.FONT_BOLD.deriveFont((float) fontSize));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (!first) {
            label.setBorder(new EmptyBorder(0, 0, 8, 0));
        }
        panel.add(label);
    }

    private static void addText(JPanel panel, String text) {
        JLabel label = new JLabel("<html><body style='width: 600px'>" + text + "</body></html>");
        label.setForeground(DarkDialog.getForeground());
        label.setFont(ModernTheme.FONT_REGULAR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(3, 0, 3, 0));
        panel.add(label);
    }

    private static void addAccent(JPanel panel, String text) {
        JLabel label = new JLabel("<html><body style='width: 600px'>" + text + "</body></html>");
        label.setForeground(getAccentColor());
        label.setFont(ModernTheme.FONT_REGULAR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(2, 0, 2, 0));
        panel.add(label);
    }

    private static void addShortcut(JPanel panel, String key, String description) {
        JPanel shortcutPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        shortcutPanel.setBackground(DarkDialog.getBackground());
        shortcutPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        shortcutPanel.setMaximumSize(new Dimension(650, 25));

        // Key label (monospace, accent color)
        JLabel keyLabel = new JLabel(String.format("%-20s", key));
        keyLabel.setForeground(getAccentColor());
        keyLabel.setFont(ModernTheme.FONT_CODE.deriveFont(java.awt.Font.BOLD, 13f));
        shortcutPanel.add(keyLabel);

        // Description label
        JLabel descLabel = new JLabel(description);
        descLabel.setForeground(DarkDialog.getForeground());
        descLabel.setFont(ModernTheme.FONT_REGULAR);
        shortcutPanel.add(descLabel);

        panel.add(shortcutPanel);
    }

    private static void addCodeBlock(JPanel panel, String code) {
        JTextArea codeArea = new JTextArea(code);
        codeArea.setBackground(DarkDialog.getBackgroundDarker());
        codeArea.setForeground(DarkDialog.getForeground());
        codeArea.setFont(ModernTheme.withSize(ModernTheme.FONT_CODE, 12));
        codeArea.setEditable(false);
        codeArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        codeArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setBorder(BorderFactory.createLineBorder(DarkDialog.getBorderColor(), 1));
        codeScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        codeScroll.setMaximumSize(new Dimension(650, 150));
        codeScroll.setPreferredSize(new Dimension(650, 150));

        panel.add(codeScroll);
    }

    private static void addSpacer(JPanel panel, int height) {
        panel.add(Box.createRigidArea(new Dimension(0, height)));
    }

    private static String getModuleVersion() {
        try (java.io.InputStream is = InformationDialog.class.getResourceAsStream("/version.properties")) {
            if (is != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(is);
                String major = props.getProperty("version.major", "3");
                String minor = props.getProperty("version.minor", "0");
                String patch = props.getProperty("version.patch", "0");
                return major + "." + minor + "." + patch;
            }
        } catch (Exception e) {
            // Fallback
        }
        return "3.12.14";
    }

    private static Color getHeadingColor() {
        return DarkDialog.isDarkTheme() ? ModernTheme.ACCENT_PRIMARY : new Color(0, 80, 200);
    }

    private static Color getAccentColor() {
        return DarkDialog.isDarkTheme() ? ModernTheme.FOREGROUND_SECONDARY : ModernTheme.LIGHT_FOREGROUND_MUTED;
    }
}
