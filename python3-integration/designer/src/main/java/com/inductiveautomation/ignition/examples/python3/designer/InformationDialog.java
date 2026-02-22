package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;

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
 */
public class InformationDialog {

    // Theme colors - updated dynamically based on IDE theme
    private static boolean useDarkTheme = true;

    // Colors are sourced from ModernTheme constants - no private palette needed here.

    /**
     * Sets the theme for all future dialogs.
     *
     * @param darkTheme true for dark theme, false for light theme
     */
    public static void setDarkTheme(boolean darkTheme) {
        useDarkTheme = darkTheme;
    }

    // Get current theme colors - delegates to ModernTheme constants
    private static Color getBackground() {
        return useDarkTheme ? ModernTheme.PANEL_BACKGROUND : Color.WHITE;
    }

    private static Color getBackgroundDarker() {
        return useDarkTheme ? ModernTheme.BACKGROUND_DARKER : ModernTheme.LIGHT_BACKGROUND_DARKER;
    }

    private static Color getForeground() {
        return useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK;
    }

    private static Color getHeadingColor() {
        return useDarkTheme ? ModernTheme.ACCENT_PRIMARY : new Color(0, 80, 200);
    }

    private static Color getAccentColor() {
        return useDarkTheme ? ModernTheme.FOREGROUND_SECONDARY : ModernTheme.LIGHT_FOREGROUND_MUTED;
    }

    private static Color getButtonBg() {
        return useDarkTheme ? ModernTheme.BUTTON_BACKGROUND : ModernTheme.LIGHT_BUTTON_BG;
    }

    private static Color getBorderColor() {
        return useDarkTheme ? ModernTheme.BORDER_DEFAULT : ModernTheme.LIGHT_BORDER;
    }

    /**
     * Shows the information dialog with comprehensive user guide.
     *
     * @param parent parent component
     */
    public static void show(Component parent) {
        JDialog dialog = createBaseDialog(parent, "Python 3 IDE - User Guide");

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(getBackground());
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Create scrollable content area
        JPanel infoPanel = createInfoPanel();
        JScrollPane scrollPane = new JScrollPane(infoPanel);
        scrollPane.setBackground(getBackground());
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Close button
        JButton closeButton = createThemedButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(getBackground());
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
        panel.setBackground(getBackground());
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
        label.setForeground(getForeground());
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
        shortcutPanel.setBackground(getBackground());
        shortcutPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        shortcutPanel.setMaximumSize(new Dimension(650, 25));

        // Key label (monospace, accent color)
        JLabel keyLabel = new JLabel(String.format("%-20s", key));
        keyLabel.setForeground(getAccentColor());
        keyLabel.setFont(ModernTheme.FONT_CODE.deriveFont(java.awt.Font.BOLD, 13f));
        shortcutPanel.add(keyLabel);

        // Description label
        JLabel descLabel = new JLabel(description);
        descLabel.setForeground(getForeground());
        descLabel.setFont(ModernTheme.FONT_REGULAR);
        shortcutPanel.add(descLabel);

        panel.add(shortcutPanel);
    }

    private static void addCodeBlock(JPanel panel, String code) {
        JTextArea codeArea = new JTextArea(code);
        codeArea.setBackground(getBackgroundDarker());
        codeArea.setForeground(getForeground());
        codeArea.setFont(ModernTheme.withSize(ModernTheme.FONT_CODE, 12));
        codeArea.setEditable(false);
        codeArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        codeArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setBorder(BorderFactory.createLineBorder(getBorderColor(), 1));
        codeScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        codeScroll.setMaximumSize(new Dimension(650, 150));
        codeScroll.setPreferredSize(new Dimension(650, 150));

        panel.add(codeScroll);
    }

    private static void addSpacer(JPanel panel, int height) {
        panel.add(Box.createRigidArea(new Dimension(0, height)));
    }

    private static JDialog createBaseDialog(Component parent, String title) {
        Window owner = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(getBackground());
        dialog.setIconImage(DarkDialog.createPython3Icon());  // v2.5.4: Custom icon
        return dialog;
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
        return "3.6.7";
    }

    private static JButton createThemedButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(getButtonBg());
        button.setForeground(getForeground());
        button.setFont(ModernTheme.FONT_REGULAR);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getBorderColor(), 1),
            new EmptyBorder(5, 15, 5, 15)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover effect (theme-aware)
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (useDarkTheme) {
                    button.setBackground(ModernTheme.BUTTON_HOVER);
                } else {
                    button.setBackground(ModernTheme.LIGHT_BUTTON_HOVER);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(getButtonBg());
            }
        });

        return button;
    }
}
