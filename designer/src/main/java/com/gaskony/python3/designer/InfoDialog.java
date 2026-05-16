package com.gaskony.python3.designer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Information dialog showing module usage guide and help.
 * <p>
 * Displays:
 * <ul>
 *   <li>Module overview and features</li>
 *   <li>Keyboard shortcuts</li>
 *   <li>How to use scripts in Designer/Gateway</li>
 *   <li>Script execution modes</li>
 * </ul>
 *
 * @since v2.7.0
 */
public class InfoDialog extends JDialog {
    private static final int DIALOG_WIDTH = 800;
    private static final int DIALOG_HEIGHT = 700;

    private final Python3IDE idePanel;

    /**
     * Creates a new Info dialog.
     *
     * @param parent Parent frame
     * @param idePanel IDE panel reference
     */
    public InfoDialog(Frame parent, Python3IDE idePanel) {
        super(parent, "Python 3 IDE - Help & Usage", true);
        this.idePanel = idePanel;

        layoutComponents();

        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * Layout components in the dialog.
     */
    private void layoutComponents() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(ModernTheme.BACKGROUND_DARK);
        contentPanel.setBorder(new EmptyBorder(16, 20, 16, 20));  // Reduced vertical padding

        // === Header Section ===
        JPanel headerSection = createHeaderSection();
        contentPanel.add(headerSection);
        contentPanel.add(Box.createVerticalStrut(12));  // Reduced from 20

        // === Overview Section ===
        SectionPanel overviewSP = createSection("Module Overview");
        addBulletPoint(overviewSP.content, "Write and execute Python 3 scripts directly in Ignition Designer");
        addBulletPoint(overviewSP.content, "Save scripts with metadata and organize into folders");
        addBulletPoint(overviewSP.content, "Execute scripts on Gateway with real-time output");
        addBulletPoint(overviewSP.content, "Interactive terminal mode with shell commands");
        contentPanel.add(overviewSP.wrapper);
        contentPanel.add(Box.createVerticalStrut(10));  // Reduced from 16

        // === Keyboard Shortcuts Section (2 columns to save vertical space) ===
        SectionPanel shortcutsSP = createSection("Keyboard Shortcuts");

        // Create 2-column layout for shortcuts
        JPanel shortcutsGrid = new JPanel(new GridBagLayout());
        shortcutsGrid.setBackground(ModernTheme.PANEL_BACKGROUND);
        shortcutsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 0, 2, 20);  // Reduced spacing, 20px gap between columns

        // Column 1
        addShortcutToGrid(shortcutsGrid, gbc, "Ctrl+Enter", "Execute script");
        gbc.gridy++;
        addShortcutToGrid(shortcutsGrid, gbc, "Ctrl+S", "Save script");
        gbc.gridy++;
        addShortcutToGrid(shortcutsGrid, gbc, "Ctrl+Shift+S", "Save script as...");
        gbc.gridy++;
        addShortcutToGrid(shortcutsGrid, gbc, "Ctrl+F", "Find text");
        gbc.gridy++;
        addShortcutToGrid(shortcutsGrid, gbc, "Ctrl+H", "Replace text");
        gbc.gridy++;
        addShortcutToGrid(shortcutsGrid, gbc, "Ctrl+B", "Toggle sidebar");

        // Column 2
        gbc.gridx = 1;
        gbc.gridy = 0;
        addShortcutToGrid(shortcutsGrid, gbc, "Ctrl+Shift+P", "Command Palette");
        gbc.gridy++;
        addShortcutToGrid(shortcutsGrid, gbc, "Ctrl+Space", "Auto-complete");
        gbc.gridy++;
        addShortcutToGrid(shortcutsGrid, gbc, "Ctrl++", "Increase font size");
        gbc.gridy++;
        addShortcutToGrid(shortcutsGrid, gbc, "Ctrl+-", "Decrease font size");
        gbc.gridy++;
        addShortcutToGrid(shortcutsGrid, gbc, "Ctrl+L", "Clear output");

        shortcutsSP.content.add(shortcutsGrid);
        contentPanel.add(shortcutsSP.wrapper);
        contentPanel.add(Box.createVerticalStrut(12));  // Reduced from 16

        // === Using Saved Scripts Section (v2.15.3: Updated with correct usage) ===
        SectionPanel scriptsSP = createSection("Using Saved Scripts in Ignition");
        addInfoText(scriptsSP.content, "Scripts saved in this IDE are stored on the Gateway and can be executed using:");
        scriptsSP.content.add(Box.createVerticalStrut(8));

        // Method 1: REST API
        addSubheading(scriptsSP.content, "1. REST API (Recommended)");
        addBulletPoint(scriptsSP.content, "Load script: GET /data/python3integration/api/v1/scripts/:name");
        addBulletPoint(scriptsSP.content, "Execute code: POST /data/python3integration/api/v1/exec with {'code': '...'}");
        scriptsSP.content.add(Box.createVerticalStrut(6));

        // Method 2: Script Console
        addSubheading(scriptsSP.content, "2. Script Console / Gateway Events");
        addBulletPoint(scriptsSP.content, "Direct code: system.python3.exec('print(\"Hello\")') ");
        addBulletPoint(scriptsSP.content, "With variables: system.python3.exec(code, {'myVar': value})");
        addBulletPoint(scriptsSP.content, "Available functions: exec(), eval(), getVersion(), getPoolStats()");
        scriptsSP.content.add(Box.createVerticalStrut(6));

        // Method 3: Copy and paste
        addSubheading(scriptsSP.content, "3. Copy Script Code");
        addBulletPoint(scriptsSP.content, "Open script in IDE, copy code, paste into Script Console or Gateway Events");

        contentPanel.add(scriptsSP.wrapper);
        contentPanel.add(Box.createVerticalStrut(10));

        // === Execution Modes Section ===
        SectionPanel modesSP = createSection("Execution Modes");
        addInfoText(modesSP.content, "Python IDE: Execute Python 3 scripts with full package support");
        modesSP.content.add(Box.createVerticalStrut(6));  // Reduced from 8
        addInfoText(modesSP.content, "Terminal: Run shell commands (ls, pwd, pip, etc.) interactively");
        contentPanel.add(modesSP.wrapper);
        contentPanel.add(Box.createVerticalGlue());

        // === Button Panel ===
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonPanel.setBackground(ModernTheme.BACKGROUND_DARK);
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, ModernTheme.BORDER_DEFAULT),
            new EmptyBorder(16, 20, 16, 20)
        ));

        JButton closeButton = ModernButton.createPrimary("Close");
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(closeButton);

        // Wrap content in scroll pane
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBackground(ModernTheme.BACKGROUND_DARK);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Assemble dialog
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        getContentPane().setBackground(ModernTheme.BACKGROUND_DARK);
    }

    /**
     * Create header section with logo and title.
     */
    private JPanel createHeaderSection() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(ModernTheme.BACKGROUND_DARK);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Title
        JLabel titleLabel = new JLabel("Python 3 IDE");
        titleLabel.setFont(ModernTheme.withSize(ModernTheme.FONT_BOLD, 24));
        titleLabel.setForeground(ModernTheme.ACCENT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(titleLabel);

        header.add(Box.createVerticalStrut(8));

        // Subtitle
        JLabel subtitleLabel = new JLabel("Ignition Python 3 Integration Module");
        subtitleLabel.setFont(ModernTheme.FONT_REGULAR);
        subtitleLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(subtitleLabel);

        return header;
    }

    /** Holds the outer section wrapper (for layout/sizing) and inner content panel (for adding items). */
    private static class SectionPanel {
        final JPanel wrapper;
        final JPanel content;
        SectionPanel(JPanel wrapper, JPanel content) {
            this.wrapper = wrapper;
            this.content = content;
        }
    }

    /**
     * Create a section panel with card header title.
     */
    private SectionPanel createSection(String title) {
        JPanel wrapper = new JPanel(new java.awt.BorderLayout(0, 0));
        wrapper.setBackground(ModernTheme.PANEL_BACKGROUND);
        wrapper.setBorder(BorderFactory.createLineBorder(ModernTheme.PANEL_BORDER));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        wrapper.add(ModernTheme.createCardHeader(title, null), java.awt.BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(ModernTheme.PANEL_BACKGROUND);
        content.setBorder(new EmptyBorder(8, 16, 12, 16));
        wrapper.add(content, java.awt.BorderLayout.CENTER);

        return new SectionPanel(wrapper, content);
    }

    /**
     * Add a bullet point item.
     */
    private void addBulletPoint(JPanel panel, String text) {
        JLabel label = new JLabel("• " + text);
        label.setFont(ModernTheme.FONT_REGULAR);
        label.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(8));
    }

    /**
     * Add an informational text line.
     */
    private void addInfoText(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setFont(ModernTheme.FONT_REGULAR);
        label.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
    }

    /**
     * Add a subheading (smaller than section title, larger than regular text).
     * v2.15.3: Added for better content organization
     */
    private void addSubheading(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setFont(ModernTheme.withSize(ModernTheme.FONT_BOLD, 13));
        label.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
    }

    /**
     * Add a keyboard shortcut to a GridBagLayout panel (for 2-column layout).
     */
    private void addShortcutToGrid(JPanel panel, GridBagConstraints gbc, String keys, String description) {
        // Keys label (bold, monospace style)
        JLabel keysLabel = new JLabel(keys);
        keysLabel.setFont(ModernTheme.FONT_BOLD);
        keysLabel.setForeground(ModernTheme.ACCENT_PRIMARY);

        // Description label
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(ModernTheme.FONT_REGULAR);
        descLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);

        // Create horizontal panel for this shortcut
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(ModernTheme.PANEL_BACKGROUND);

        row.add(keysLabel);
        row.add(Box.createHorizontalStrut(12));
        row.add(descLabel);

        panel.add(row, gbc);
    }
}
