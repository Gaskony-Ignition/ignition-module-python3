package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.prefs.Preferences;

/**
 * Settings dialog for Python 3 IDE configuration.
 * <p>
 * Features (v2.8.1):
 * <ul>
 *   <li>Gateway URL override (single editable field)</li>
 *   <li>Auto-connect on startup checkbox</li>
 *   <li>Process pool size configuration (1-20)</li>
 *   <li>Font size controls with A+/A- buttons</li>
 *   <li>Connect to Gateway button</li>
 * </ul>
 *
 * @since v2.7.0
 */
public class SettingsDialog extends JDialog {
    private static final int DIALOG_WIDTH = 800;
    private static final int DIALOG_HEIGHT = 650;  // Increased to eliminate scrolling

    private final Python3IDE idePanel;
    private final Preferences prefs;

    // UI Components
    private JTextField gatewayUrlField;
    private JCheckBox autoConnectCheckbox;
    private JComboBox<Integer> poolSizeDropdown;
    private JSpinner fontSizeSpinner;
    private JButton fontIncreaseButton;
    private JButton fontDecreaseButton;
    private JComboBox<String> themeSelector;  // v2.11.4: Theme selector moved from toolbar

    private boolean connectRequested = false;

    /**
     * Creates a new Settings dialog.
     *
     * @param parent Parent frame
     * @param idePanel IDE panel reference for callbacks
     */
    public SettingsDialog(Frame parent, Python3IDE idePanel) {
        super(parent, "Settings", true);
        this.idePanel = idePanel;
        this.prefs = Preferences.userNodeForPackage(Python3IDE.class);

        initComponents();
        layoutComponents();
        loadSettings();

        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * Initialize UI components.
     */
    private void initComponents() {
        // Gateway URL (single editable field) - v2.11.5: Reduced height
        gatewayUrlField = new JTextField();
        gatewayUrlField.setBackground(ModernTheme.INPUT_BACKGROUND);
        gatewayUrlField.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        gatewayUrlField.setFont(ModernTheme.FONT_REGULAR);
        gatewayUrlField.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);
        gatewayUrlField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ModernTheme.INPUT_BORDER),
            new EmptyBorder(5, 8, 5, 8)
        ));
        gatewayUrlField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));  // v2.11.5: Limit vertical height

        // Auto-connect checkbox
        autoConnectCheckbox = new JCheckBox("Auto-connect on startup");
        autoConnectCheckbox.setBackground(ModernTheme.PANEL_BACKGROUND);
        autoConnectCheckbox.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        autoConnectCheckbox.setFont(ModernTheme.FONT_REGULAR);
        autoConnectCheckbox.setSelected(true);  // Default: enabled

        // Pool size dropdown
        Integer[] poolSizes = new Integer[20];
        for (int i = 0; i < 20; i++) {
            poolSizes[i] = i + 1;
        }
        poolSizeDropdown = new JComboBox<>(poolSizes);
        poolSizeDropdown.setSelectedItem(3);  // Default: 3
        poolSizeDropdown.setBackground(ModernTheme.INPUT_BACKGROUND);
        poolSizeDropdown.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        poolSizeDropdown.setFont(ModernTheme.FONT_REGULAR);

        // Font size spinner (8-24)
        SpinnerNumberModel fontModel = new SpinnerNumberModel(12, 8, 24, 1);
        fontSizeSpinner = new JSpinner(fontModel);
        fontSizeSpinner.setFont(ModernTheme.FONT_REGULAR);
        ((JSpinner.DefaultEditor) fontSizeSpinner.getEditor()).getTextField().setBackground(ModernTheme.INPUT_BACKGROUND);
        ((JSpinner.DefaultEditor) fontSizeSpinner.getEditor()).getTextField().setForeground(ModernTheme.FOREGROUND_PRIMARY);

        // Font size A+/A- buttons
        fontIncreaseButton = ModernButton.createSmall("A+");
        fontDecreaseButton = ModernButton.createSmall("A-");

        fontIncreaseButton.addActionListener(e -> {
            int currentSize = (Integer) fontSizeSpinner.getValue();
            if (currentSize < 24) {
                fontSizeSpinner.setValue(currentSize + 1);
            }
        });

        // Theme selector (v2.11.4: Moved from Python3IDE toolbar)
        String[] themes = {"Dark", "VS Code Dark+", "Monokai", "Dracula", "Default (Light)", "IntelliJ Light", "Eclipse"};
        themeSelector = new JComboBox<>(themes);
        themeSelector.setBackground(ModernTheme.INPUT_BACKGROUND);
        themeSelector.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        themeSelector.setFont(ModernTheme.FONT_REGULAR);
        themeSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // Apply theme immediately when changed (v2.11.4)
        themeSelector.addActionListener(e -> {
            String selectedTheme = (String) themeSelector.getSelectedItem();
            if (selectedTheme != null) {
                idePanel.applyThemeByName(selectedTheme);
            }
        });

        fontDecreaseButton.addActionListener(e -> {
            int currentSize = (Integer) fontSizeSpinner.getValue();
            if (currentSize > 8) {
                fontSizeSpinner.setValue(currentSize - 1);
            }
        });
    }

    /**
     * Layout components in the dialog.
     */
    private void layoutComponents() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(ModernTheme.BACKGROUND_DARK);
        contentPanel.setBorder(new EmptyBorder(16, 20, 16, 20));  // Reduced top/bottom from 20 to 16

        // === Gateway Connection Section ===
        JPanel gatewaySection = createSection("Gateway Connection");

        // Gateway URL (single editable field)
        gatewaySection.add(createLabel("Gateway URL"));
        gatewaySection.add(Box.createVerticalStrut(8));
        gatewaySection.add(gatewayUrlField);
        gatewaySection.add(Box.createVerticalStrut(4));
        JLabel hint = createLabel("Override the auto-detected Gateway URL (e.g., http://localhost:8088)");
        hint.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));
        hint.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        gatewaySection.add(hint);
        gatewaySection.add(Box.createVerticalStrut(16));

        // Auto-connect checkbox
        gatewaySection.add(autoConnectCheckbox);
        gatewaySection.add(Box.createVerticalStrut(16));

        // Connect button
        JButton connectButton = ModernButton.createPrimary("Connect to Gateway");
        connectButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        connectButton.addActionListener(e -> {
            connectRequested = true;
            saveSettings();
            dispose();
        });
        gatewaySection.add(connectButton);

        contentPanel.add(gatewaySection);
        contentPanel.add(Box.createVerticalStrut(16));  // Reduced from 24 to 16

        // === Process Pool and Editor Appearance (2-column layout) ===
        JPanel twoColumnPanel = new JPanel();
        twoColumnPanel.setLayout(new BoxLayout(twoColumnPanel, BoxLayout.X_AXIS));
        twoColumnPanel.setBackground(ModernTheme.BACKGROUND_DARK);
        twoColumnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // LEFT COLUMN: Process Pool Section
        JPanel poolSection = createSection("Process Pool");
        poolSection.setPreferredSize(new Dimension(350, 200));
        poolSection.setMaximumSize(new Dimension(350, 200));

        poolSection.add(createLabel("Pool Size (1-20)"));
        poolSection.add(Box.createVerticalStrut(8));
        poolSizeDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        poolSection.add(poolSizeDropdown);
        poolSection.add(Box.createVerticalStrut(4));
        JLabel poolHint = createLabel("Number of Python processes in the pool. Higher values allow more concurrent executions.");
        poolHint.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));
        poolHint.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        poolSection.add(poolHint);

        // RIGHT COLUMN: Editor Appearance Section (v2.11.5: Simplified layout)
        JPanel appearanceSection = createSection("Editor Appearance");
        appearanceSection.setPreferredSize(new Dimension(350, 200));
        appearanceSection.setMaximumSize(new Dimension(350, 200));

        // Font size controls panel (A+/A- buttons only, no label)
        JPanel fontControlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fontControlsPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        fontControlsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontControlsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        fontControlsPanel.add(fontDecreaseButton);
        fontControlsPanel.add(fontIncreaseButton);

        appearanceSection.add(fontControlsPanel);
        appearanceSection.add(Box.createVerticalStrut(4));
        JLabel fontHint = createLabel("Code editor font size (A- / A+). Changes apply immediately.");
        fontHint.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));
        fontHint.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        appearanceSection.add(fontHint);

        // Theme selector (v2.11.4: Moved from toolbar, v2.11.5: Moved up for better layout)
        appearanceSection.add(Box.createVerticalStrut(12));
        appearanceSection.add(createLabel("Editor Theme"));
        appearanceSection.add(Box.createVerticalStrut(8));
        themeSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        appearanceSection.add(themeSelector);
        appearanceSection.add(Box.createVerticalStrut(4));
        JLabel themeHint = createLabel("Code editor color theme. Changes apply immediately.");
        themeHint.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));
        themeHint.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        appearanceSection.add(themeHint);

        // Assemble 2-column layout
        twoColumnPanel.add(poolSection);
        twoColumnPanel.add(Box.createHorizontalStrut(16));  // Gap between columns
        twoColumnPanel.add(appearanceSection);

        contentPanel.add(twoColumnPanel);
        contentPanel.add(Box.createVerticalGlue());

        // === Button Panel ===
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonPanel.setBackground(ModernTheme.BACKGROUND_DARK);
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, ModernTheme.BORDER_DEFAULT),
            new EmptyBorder(16, 20, 16, 20)
        ));

        JButton resetButton = ModernButton.createDefault("Reset to Defaults");
        resetButton.setPreferredSize(new Dimension(160, 32));
        resetButton.addActionListener(e -> resetToDefaults());

        JButton closeButton = ModernButton.createDefault("Close");
        closeButton.setPreferredSize(new Dimension(100, 32));
        closeButton.addActionListener(e -> dispose());

        JButton saveButton = ModernButton.createPrimary("Save Settings");
        saveButton.setPreferredSize(new Dimension(140, 32));
        saveButton.addActionListener(e -> {
            saveSettings();
            dispose();
        });

        buttonPanel.add(resetButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(Box.createHorizontalGlue());  // Push Close to far right
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
     * Create a label with proper styling.
     */
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ModernTheme.FONT_REGULAR);
        label.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * Load settings from preferences.
     */
    private void loadSettings() {
        // Load Gateway URL (auto-detected or override)
        String gatewayUrl = idePanel.getEffectiveGatewayUrl();
        gatewayUrlField.setText(gatewayUrl != null ? gatewayUrl : "");

        // Auto-connect preference: v2.11.5 - Always default to checked, don't persist (user request)
        // The checkbox is initialized to true in the constructor, so we don't need to load it here

        // Load pool size
        int poolSize = prefs.getInt("python3ide.pool.size", 3);
        poolSizeDropdown.setSelectedItem(poolSize);

        // Load font size
        int fontSize = prefs.getInt("python3ide.fontsize", 12);
        fontSizeSpinner.setValue(fontSize);

        // Load theme (v2.11.4)
        String theme = prefs.get("python3ide.theme", "Dark");
        themeSelector.setSelectedItem(theme);
    }

    /**
     * Save settings to preferences.
     */
    private void saveSettings() {
        // Save Gateway URL
        String gatewayUrl = gatewayUrlField.getText().trim();
        prefs.put("python3ide.gateway.override", gatewayUrl);

        // Auto-connect preference: v2.11.5 - Always save as true (checkbox always checked, user request)
        prefs.putBoolean("python3ide.gateway.autoconnect", true);

        // Save pool size
        Integer poolSize = (Integer) poolSizeDropdown.getSelectedItem();
        if (poolSize != null) {
            prefs.putInt("python3ide.pool.size", poolSize);
        }

        // Save font size
        Integer fontSize = (Integer) fontSizeSpinner.getValue();
        if (fontSize != null) {
            prefs.putInt("python3ide.fontsize", fontSize);
        }

        // Save theme (v2.11.4)
        String theme = (String) themeSelector.getSelectedItem();
        if (theme != null) {
            prefs.put("python3ide.theme", theme);
        }

        // Notify IDE panel to reload settings
        idePanel.reloadSettingsFromPreferences();
    }

    /**
     * Reset all settings to defaults.
     */
    private void resetToDefaults() {
        String detected = idePanel.getDetectedGatewayUrl();
        gatewayUrlField.setText(detected != null ? detected : "http://localhost:8088");
        autoConnectCheckbox.setSelected(true);
        poolSizeDropdown.setSelectedItem(3);
        fontSizeSpinner.setValue(12);
        themeSelector.setSelectedItem("Dark");  // v2.11.4: Reset theme to default
    }

    /**
     * Check if user requested to connect to gateway.
     *
     * @return true if Connect button was clicked
     */
    public boolean isConnectRequested() {
        return connectRequested;
    }
}
