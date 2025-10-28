package com.inductiveautomation.ignition.examples.python3.designer;

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
        JPanel overviewSection = createSection("Module Overview");
        addBulletPoint(overviewSection, "Write and execute Python 3 scripts directly in Ignition Designer");
        addBulletPoint(overviewSection, "Save scripts with metadata and organize into folders");
        addBulletPoint(overviewSection, "Execute scripts on Gateway with real-time output");
        addBulletPoint(overviewSection, "Interactive terminal mode with shell commands");
        contentPanel.add(overviewSection);
        contentPanel.add(Box.createVerticalStrut(10));  // Reduced from 16

        // === Keyboard Shortcuts Section (2 columns to save vertical space) ===
        JPanel shortcutsSection = createSection("Keyboard Shortcuts");

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

        shortcutsSection.add(shortcutsGrid);
        contentPanel.add(shortcutsSection);
        contentPanel.add(Box.createVerticalStrut(12));  // Reduced from 16

        // === Using Scripts in Designer Section ===
        JPanel scriptsSection = createSection("Using Scripts in Ignition");
        addInfoText(scriptsSection, "Scripts saved in this IDE are stored on the Gateway and can be executed from anywhere in Ignition:");
        scriptsSection.add(Box.createVerticalStrut(8));  // Reduced from 12
        addBulletPoint(scriptsSection, "Script Console: system.python3.execScript('script_name')");
        addBulletPoint(scriptsSection, "Gateway Events: Use Python3ScriptModule functions");
        addBulletPoint(scriptsSection, "REST API: POST to /data/python3integration/api/v1/exec");
        addBulletPoint(scriptsSection, "Perspective: Call from Perspective script actions");
        contentPanel.add(scriptsSection);
        contentPanel.add(Box.createVerticalStrut(10));  // Reduced from 16

        // === Execution Modes Section ===
        JPanel modesSection = createSection("Execution Modes");
        addInfoText(modesSection, "Python IDE: Execute Python 3 scripts with full package support");
        modesSection.add(Box.createVerticalStrut(6));  // Reduced from 8
        addInfoText(modesSection, "Terminal: Run shell commands (ls, pwd, pip, etc.) interactively");
        contentPanel.add(modesSection);
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

    /**
     * Create a section panel with title.
     */
    private JPanel createSection(String title) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(ModernTheme.PANEL_BACKGROUND);
        section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ModernTheme.PANEL_BORDER),
            new EmptyBorder(16, 16, 16, 16)
        ));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Section title
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(ModernTheme.FONT_TITLE);
        titleLabel.setForeground(ModernTheme.ACCENT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(titleLabel);
        section.add(Box.createVerticalStrut(16));

        return section;
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
