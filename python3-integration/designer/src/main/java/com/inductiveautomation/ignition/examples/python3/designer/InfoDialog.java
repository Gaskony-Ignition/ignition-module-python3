package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Information dialog showing module and Python version details.
 * <p>
 * Displays:
 * <ul>
 *   <li>Module version (from version.properties)</li>
 *   <li>Python version (from REST API)</li>
 *   <li>Gateway connection status</li>
 *   <li>Process pool statistics</li>
 *   <li>Connection health</li>
 * </ul>
 *
 * @since v2.7.0
 */
public class InfoDialog extends JDialog {
    private static final int DIALOG_WIDTH = 600;
    private static final int DIALOG_HEIGHT = 500;

    private final Python3IDE idePanel;

    // UI Components
    private JLabel moduleVersionLabel;
    private JLabel pythonVersionLabel;
    private JLabel connectionStatusLabel;
    private JLabel poolSizeLabel;
    private JLabel healthyProcessesLabel;
    private JLabel availableProcessesLabel;
    private JLabel inUseProcessesLabel;

    /**
     * Creates a new Info dialog.
     *
     * @param parent Parent frame
     * @param idePanel IDE panel reference for accessing REST client
     */
    public InfoDialog(Frame parent, Python3IDE idePanel) {
        super(parent, "About Python 3 IDE", true);
        this.idePanel = idePanel;

        initComponents();
        layoutComponents();
        loadInformation();

        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * Initialize UI components.
     */
    private void initComponents() {
        // Module version
        moduleVersionLabel = createValueLabel();

        // Python version
        pythonVersionLabel = createValueLabel();

        // Connection status
        connectionStatusLabel = createValueLabel();

        // Pool statistics
        poolSizeLabel = createValueLabel();
        healthyProcessesLabel = createValueLabel();
        availableProcessesLabel = createValueLabel();
        inUseProcessesLabel = createValueLabel();
    }

    /**
     * Layout components in the dialog.
     */
    private void layoutComponents() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(ModernTheme.BACKGROUND_DARK);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // === Header Section ===
        JPanel headerSection = createHeaderSection();
        contentPanel.add(headerSection);
        contentPanel.add(Box.createVerticalStrut(20));

        // === Module Information Section ===
        JPanel moduleSection = createSection("Module Information");

        addInfoRow(moduleSection, "Module Version:", moduleVersionLabel);
        moduleSection.add(Box.createVerticalStrut(12));

        addInfoRow(moduleSection, "Python Version:", pythonVersionLabel);

        contentPanel.add(moduleSection);
        contentPanel.add(Box.createVerticalStrut(20));

        // === Gateway Connection Section ===
        JPanel connectionSection = createSection("Gateway Connection");

        addInfoRow(connectionSection, "Status:", connectionStatusLabel);

        contentPanel.add(connectionSection);
        contentPanel.add(Box.createVerticalStrut(20));

        // === Process Pool Section ===
        JPanel poolSection = createSection("Process Pool Statistics");

        addInfoRow(poolSection, "Pool Size:", poolSizeLabel);
        poolSection.add(Box.createVerticalStrut(12));

        addInfoRow(poolSection, "Healthy Processes:", healthyProcessesLabel);
        poolSection.add(Box.createVerticalStrut(12));

        addInfoRow(poolSection, "Available:", availableProcessesLabel);
        poolSection.add(Box.createVerticalStrut(12));

        addInfoRow(poolSection, "In Use:", inUseProcessesLabel);

        contentPanel.add(poolSection);
        contentPanel.add(Box.createVerticalGlue());

        // === Button Panel ===
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonPanel.setBackground(ModernTheme.BACKGROUND_DARK);
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, ModernTheme.BORDER_DEFAULT),
            new EmptyBorder(16, 20, 16, 20)
        ));

        JButton refreshButton = ModernButton.createDefault("Refresh");
        refreshButton.addActionListener(e -> loadInformation());

        JButton closeButton = ModernButton.createPrimary("Close");
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(refreshButton);
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
     * Add an information row (label + value).
     */
    private void addInfoRow(JPanel panel, String labelText, JLabel valueLabel) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(ModernTheme.PANEL_BACKGROUND);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        JLabel label = new JLabel(labelText);
        label.setFont(ModernTheme.FONT_REGULAR);
        label.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        label.setPreferredSize(new Dimension(150, 25));

        row.add(label);
        row.add(Box.createHorizontalStrut(12));
        row.add(valueLabel);
        row.add(Box.createHorizontalGlue());

        panel.add(row);
    }

    /**
     * Create a value label with proper styling.
     */
    private JLabel createValueLabel() {
        JLabel label = new JLabel("Loading...");
        label.setFont(ModernTheme.FONT_REGULAR);
        label.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        return label;
    }

    /**
     * Load module version from version.properties.
     */
    private String loadModuleVersion() {
        try {
            // Load version.properties from classpath
            InputStream is = getClass().getClassLoader().getResourceAsStream("version.properties");
            if (is != null) {
                Properties props = new Properties();
                props.load(is);

                String major = props.getProperty("version.major", "0");
                String minor = props.getProperty("version.minor", "0");
                String patch = props.getProperty("version.patch", "0");

                return major + "." + minor + "." + patch;
            }
        } catch (IOException e) {
            // Fallback to hardcoded version if properties file not found
        }

        // Fallback: try to get from DesignerHook if available
        return "2.7.0";  // Default for this release
    }

    /**
     * Load information from Gateway.
     */
    private void loadInformation() {
        // Load module version (local)
        String moduleVersion = loadModuleVersion();
        moduleVersionLabel.setText(moduleVersion);
        moduleVersionLabel.setForeground(ModernTheme.ACCENT_PRIMARY);

        // Check if connected to Gateway
        Python3RestClient restClient = idePanel.getRestClient();

        if (restClient == null) {
            // Not connected
            pythonVersionLabel.setText("Not connected");
            pythonVersionLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);

            connectionStatusLabel.setText("Disconnected");
            connectionStatusLabel.setForeground(ModernTheme.ERROR);

            poolSizeLabel.setText("-");
            healthyProcessesLabel.setText("-");
            availableProcessesLabel.setText("-");
            inUseProcessesLabel.setText("-");

            return;
        }

        // Connected - fetch data from Gateway
        connectionStatusLabel.setText("Connected");
        connectionStatusLabel.setForeground(ModernTheme.SUCCESS);

        // Load Python version
        try {
            String pythonVersion = restClient.getPythonVersion();
            pythonVersionLabel.setText(pythonVersion);
            pythonVersionLabel.setForeground(ModernTheme.ACCENT_PRIMARY);
        } catch (IOException e) {
            pythonVersionLabel.setText("Error: " + e.getMessage());
            pythonVersionLabel.setForeground(ModernTheme.ERROR);
        }

        // Load pool statistics
        try {
            PoolStats poolStats = restClient.getPoolStats();

            poolSizeLabel.setText(String.valueOf(poolStats.getTotalSize()));
            poolSizeLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);

            healthyProcessesLabel.setText(String.valueOf(poolStats.getHealthy()));
            healthyProcessesLabel.setForeground(
                poolStats.getHealthy() == poolStats.getTotalSize()
                    ? ModernTheme.SUCCESS
                    : ModernTheme.WARNING
            );

            availableProcessesLabel.setText(String.valueOf(poolStats.getAvailable()));
            availableProcessesLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);

            inUseProcessesLabel.setText(String.valueOf(poolStats.getInUse()));
            inUseProcessesLabel.setForeground(
                poolStats.getInUse() > 0
                    ? ModernTheme.ACCENT_PRIMARY
                    : ModernTheme.FOREGROUND_SECONDARY
            );

        } catch (IOException e) {
            poolSizeLabel.setText("Error");
            healthyProcessesLabel.setText("Error");
            availableProcessesLabel.setText("Error");
            inUseProcessesLabel.setText("Error");

            poolSizeLabel.setForeground(ModernTheme.ERROR);
            healthyProcessesLabel.setForeground(ModernTheme.ERROR);
            availableProcessesLabel.setForeground(ModernTheme.ERROR);
            inUseProcessesLabel.setForeground(ModernTheme.ERROR);
        }
    }
}
