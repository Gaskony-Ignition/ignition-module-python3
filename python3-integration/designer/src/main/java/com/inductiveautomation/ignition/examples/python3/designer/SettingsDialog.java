package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * Settings dialog for Python 3 IDE configuration.
 * <p>
 * Features:
 * <ul>
 *   <li>Auto-detected Gateway URL display (read-only)</li>
 *   <li>Gateway URL override (optional)</li>
 *   <li>Effective URL calculation and display</li>
 *   <li>Auto-connect on startup checkbox</li>
 *   <li>Process pool size configuration (1-20)</li>
 *   <li>Connect to Gateway button</li>
 * </ul>
 *
 * @since v2.7.0
 */
public class SettingsDialog extends JDialog {
    private static final int DIALOG_WIDTH = 750;
    private static final int DIALOG_HEIGHT = 650;

    private final Python3IDE idePanel;
    private final Preferences prefs;

    // UI Components
    private JTextField detectedUrlField;
    private JTextField overrideUrlField;
    private JLabel effectiveUrlLabel;
    private JCheckBox autoConnectCheckbox;
    private JComboBox<Integer> poolSizeDropdown;
    private JSpinner fontSizeSpinner;

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
        // Detected Gateway URL (read-only)
        detectedUrlField = new JTextField();
        detectedUrlField.setEditable(false);
        detectedUrlField.setBackground(ModernTheme.INPUT_BACKGROUND);
        detectedUrlField.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        detectedUrlField.setFont(ModernTheme.FONT_REGULAR);
        detectedUrlField.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);

        // Gateway URL Override (editable)
        overrideUrlField = new JTextField();
        overrideUrlField.setBackground(ModernTheme.INPUT_BACKGROUND);
        overrideUrlField.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        overrideUrlField.setFont(ModernTheme.FONT_REGULAR);
        overrideUrlField.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);
        overrideUrlField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ModernTheme.INPUT_BORDER),
            new EmptyBorder(5, 8, 5, 8)
        ));

        // Add listener to update effective URL as user types
        overrideUrlField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateEffectiveUrl(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateEffectiveUrl(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateEffectiveUrl(); }
        });

        // Effective URL (read-only, blue text)
        effectiveUrlLabel = new JLabel();
        effectiveUrlLabel.setFont(ModernTheme.FONT_REGULAR);
        effectiveUrlLabel.setForeground(ModernTheme.ACCENT_PRIMARY);

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
    }

    /**
     * Layout components in the dialog.
     */
    private void layoutComponents() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(ModernTheme.BACKGROUND_DARK);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // === Gateway Connection Section ===
        JPanel gatewaySection = createSection("Gateway Connection");

        // Detected Gateway URL
        gatewaySection.add(createLabel("Detected Gateway URL"));
        gatewaySection.add(Box.createVerticalStrut(8));
        gatewaySection.add(detectedUrlField);
        gatewaySection.add(Box.createVerticalStrut(16));

        // Gateway URL Override
        gatewaySection.add(createLabel("Gateway URL Override (optional)"));
        gatewaySection.add(Box.createVerticalStrut(8));
        gatewaySection.add(overrideUrlField);
        gatewaySection.add(Box.createVerticalStrut(4));
        JLabel hint = createLabel("Leave empty to use detected URL. Provide a custom URL to override.");
        hint.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));
        hint.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        gatewaySection.add(hint);
        gatewaySection.add(Box.createVerticalStrut(16));

        // Effective URL
        gatewaySection.add(createLabel("Effective URL (will be used for connections)"));
        gatewaySection.add(Box.createVerticalStrut(8));
        JPanel effectivePanel = new JPanel(new BorderLayout());
        effectivePanel.setBackground(ModernTheme.BACKGROUND_DARKER);
        effectivePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT),
            new EmptyBorder(8, 12, 8, 12)
        ));
        effectivePanel.add(effectiveUrlLabel, BorderLayout.CENTER);
        gatewaySection.add(effectivePanel);
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
        contentPanel.add(Box.createVerticalStrut(24));

        // === Process Pool Section ===
        JPanel poolSection = createSection("Process Pool");

        poolSection.add(createLabel("Pool Size (1-20)"));
        poolSection.add(Box.createVerticalStrut(8));
        poolSizeDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        poolSection.add(poolSizeDropdown);
        poolSection.add(Box.createVerticalStrut(4));
        JLabel poolHint = createLabel("Number of Python processes in the pool. Higher values allow more concurrent executions.");
        poolHint.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));
        poolHint.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        poolSection.add(poolHint);

        contentPanel.add(poolSection);
        contentPanel.add(Box.createVerticalStrut(24));

        // === Editor Appearance Section ===
        JPanel appearanceSection = createSection("Editor Appearance");

        appearanceSection.add(createLabel("Font Size (8-24)"));
        appearanceSection.add(Box.createVerticalStrut(8));
        fontSizeSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        appearanceSection.add(fontSizeSpinner);
        appearanceSection.add(Box.createVerticalStrut(4));
        JLabel fontHint = createLabel("Code editor font size. Changes apply immediately.");
        fontHint.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));
        fontHint.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        appearanceSection.add(fontHint);

        contentPanel.add(appearanceSection);
        contentPanel.add(Box.createVerticalGlue());

        // === Button Panel ===
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonPanel.setBackground(ModernTheme.BACKGROUND_DARK);
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, ModernTheme.BORDER_DEFAULT),
            new EmptyBorder(16, 20, 16, 20)
        ));

        JButton resetButton = ModernButton.createDefault("Reset to Defaults");
        resetButton.addActionListener(e -> resetToDefaults());

        JButton cancelButton = ModernButton.createDefault("Cancel");
        cancelButton.addActionListener(e -> dispose());

        JButton saveButton = ModernButton.createPrimary("Save Settings");
        saveButton.addActionListener(e -> {
            saveSettings();
            dispose();
        });

        buttonPanel.add(resetButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

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
        // Load detected URL from IDE panel
        String detected = idePanel.getDetectedGatewayUrl();
        detectedUrlField.setText(detected != null ? detected : "");

        // Load override URL from preferences
        String override = prefs.get("python3ide.gateway.override", "");
        overrideUrlField.setText(override);

        // Load auto-connect preference
        boolean autoConnect = prefs.getBoolean("python3ide.gateway.autoconnect", true);
        autoConnectCheckbox.setSelected(autoConnect);

        // Load pool size
        int poolSize = prefs.getInt("python3ide.pool.size", 3);
        poolSizeDropdown.setSelectedItem(poolSize);

        // Load font size
        int fontSize = prefs.getInt("python3ide.fontsize", 12);
        fontSizeSpinner.setValue(fontSize);

        // Update effective URL
        updateEffectiveUrl();
    }

    /**
     * Save settings to preferences.
     */
    private void saveSettings() {
        // Save override URL
        String override = overrideUrlField.getText().trim();
        prefs.put("python3ide.gateway.override", override);

        // Save auto-connect preference
        prefs.putBoolean("python3ide.gateway.autoconnect", autoConnectCheckbox.isSelected());

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

        // Notify IDE panel to reload settings
        idePanel.reloadSettingsFromPreferences();
    }

    /**
     * Reset all settings to defaults.
     */
    private void resetToDefaults() {
        overrideUrlField.setText("");
        autoConnectCheckbox.setSelected(true);
        poolSizeDropdown.setSelectedItem(3);
        fontSizeSpinner.setValue(12);
        updateEffectiveUrl();
    }

    /**
     * Update the effective URL label based on override and detected URLs.
     */
    private void updateEffectiveUrl() {
        String override = overrideUrlField.getText().trim();
        String detected = detectedUrlField.getText().trim();

        String effectiveUrl;
        if (override != null && !override.isEmpty()) {
            effectiveUrl = override;
        } else {
            effectiveUrl = detected;
        }

        effectiveUrlLabel.setText(effectiveUrl);
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
