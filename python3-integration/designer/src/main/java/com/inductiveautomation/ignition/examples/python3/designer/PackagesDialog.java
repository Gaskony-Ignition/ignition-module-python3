package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.File;

/**
 * Python package management dialog.
 * <p>
 * Features:
 * <ul>
 *   <li>Search PyPI for packages</li>
 *   <li>Install packages from PyPI (with optional version)</li>
 *   <li>Upload .whl files for air-gapped installations</li>
 *   <li>View installed packages</li>
 *   <li>Uninstall packages</li>
 * </ul>
 *
 * @since v2.7.0
 */
public class PackagesDialog extends JDialog {
    private static final int DIALOG_WIDTH = 900;
    private static final int DIALOG_HEIGHT = 750;  // Increased to eliminate scrolling

    private final Python3IDE idePanel;

    // UI Components
    private JTextField searchField;
    private JTextField installField;
    private JLabel warningLabel;
    private JPanel warningPanel;
    private JTable packagesTable;
    private DefaultTableModel tableModel;
    private JLabel packageCountLabel;

    /**
     * Creates a new Packages dialog.
     *
     * @param parent Parent frame
     * @param idePanel IDE panel reference for accessing REST client
     */
    public PackagesDialog(Frame parent, Python3IDE idePanel) {
        super(parent, "Python Packages", true);
        this.idePanel = idePanel;

        initComponents();
        layoutComponents();
        checkConnectionAndLoad();

        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * Initialize UI components.
     */
    private void initComponents() {
        // Warning banner
        warningLabel = new JLabel("⚠ Not connected to Gateway. Please connect first to manage packages.");
        warningLabel.setFont(ModernTheme.FONT_REGULAR);
        warningLabel.setForeground(new Color(102, 60, 0));  // Dark orange text

        // Search field
        searchField = new JTextField();
        searchField.setBackground(ModernTheme.INPUT_BACKGROUND);
        searchField.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        searchField.setFont(ModernTheme.FONT_REGULAR);
        searchField.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ModernTheme.INPUT_BORDER),
            new EmptyBorder(5, 8, 5, 8)
        ));

        // Install field
        installField = new JTextField();
        installField.setBackground(ModernTheme.INPUT_BACKGROUND);
        installField.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        installField.setFont(ModernTheme.FONT_REGULAR);
        installField.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);
        installField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ModernTheme.INPUT_BORDER),
            new EmptyBorder(5, 8, 5, 8)
        ));

        // Package count label
        packageCountLabel = new JLabel("Installed Packages (0)");
        packageCountLabel.setFont(ModernTheme.FONT_TITLE);
        packageCountLabel.setForeground(ModernTheme.ACCENT_PRIMARY);

        // Packages table
        String[] columnNames = {"Package Name", "Version", "Actions"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2;  // Only Actions column is editable (for buttons)
            }
        };
        packagesTable = new JTable(tableModel);
        packagesTable.setBackground(ModernTheme.BACKGROUND_DARKER);
        packagesTable.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        packagesTable.setFont(ModernTheme.FONT_REGULAR);
        packagesTable.setGridColor(ModernTheme.BORDER_DEFAULT);
        packagesTable.setRowHeight(30);
        packagesTable.getTableHeader().setBackground(ModernTheme.PANEL_BACKGROUND);
        packagesTable.getTableHeader().setForeground(ModernTheme.FOREGROUND_PRIMARY);
        packagesTable.getTableHeader().setFont(ModernTheme.FONT_BOLD);
    }

    /**
     * Layout components in the dialog.
     */
    private void layoutComponents() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(ModernTheme.BACKGROUND_DARK);
        contentPanel.setBorder(new EmptyBorder(10, 16, 10, 16));  // Further reduced to eliminate scrolling

        // === Warning Banner (shown when not connected) ===
        warningPanel = new JPanel(new BorderLayout());
        warningPanel.setBackground(new Color(255, 244, 229));  // Light orange background
        warningPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 160, 0)),
            new EmptyBorder(12, 16, 12, 16)
        ));
        warningPanel.add(warningLabel, BorderLayout.CENTER);
        warningPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        warningPanel.setVisible(false);  // Hidden by default, shown if not connected

        contentPanel.add(warningPanel);
        contentPanel.add(Box.createVerticalStrut(8));  // Further reduced to eliminate scrolling

        // === Experimental Feature Disclaimer ===
        JPanel disclaimerPanel = new JPanel(new BorderLayout());
        disclaimerPanel.setBackground(new Color(255, 249, 235));  // Very light yellow background
        disclaimerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 193, 7)),  // Yellow/amber border
            new EmptyBorder(10, 14, 10, 14)
        ));
        disclaimerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel disclaimerLabel = new JLabel("<html><b>⚠ EXPERIMENTAL FEATURE</b> - Package management is experimental and has not been fully tested. Use with caution in production environments.</html>");
        disclaimerLabel.setFont(ModernTheme.FONT_REGULAR);
        disclaimerLabel.setForeground(new Color(142, 94, 0));  // Dark amber text
        disclaimerPanel.add(disclaimerLabel, BorderLayout.CENTER);

        contentPanel.add(disclaimerPanel);
        contentPanel.add(Box.createVerticalStrut(12));

        // === Search PyPI Section ===
        JPanel searchSection = createSection("Search PyPI");

        JLabel searchHint = createHintLabel("Search for exact package name to see details and install");
        searchSection.add(searchHint);
        searchSection.add(Box.createVerticalStrut(8));

        JPanel searchRow = new JPanel();
        searchRow.setLayout(new BoxLayout(searchRow, BoxLayout.X_AXIS));
        searchRow.setBackground(ModernTheme.PANEL_BACKGROUND);
        searchRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));  // Reduced from 35 to 32
        searchRow.add(searchField);
        searchRow.add(Box.createHorizontalStrut(12));

        JButton searchButton = ModernButton.createPrimary("Search");
        searchButton.setPreferredSize(new Dimension(100, 32));  // Reduced from 35 to 32
        searchButton.setMaximumSize(new Dimension(100, 32));
        searchButton.addActionListener(e -> searchPackage());
        searchRow.add(searchButton);

        searchSection.add(searchRow);

        contentPanel.add(searchSection);
        contentPanel.add(Box.createVerticalStrut(8));  // Further reduced to eliminate scrolling

        // === Install from PyPI Section ===
        JPanel installSection = createSection("Install from PyPI");

        JLabel installHint = createHintLabel("Enter a package name from PyPI. Version can be specified (e.g., numpy==1.24.0)");
        installSection.add(installHint);
        installSection.add(Box.createVerticalStrut(8));

        JPanel installRow = new JPanel();
        installRow.setLayout(new BoxLayout(installRow, BoxLayout.X_AXIS));
        installRow.setBackground(ModernTheme.PANEL_BACKGROUND);
        installRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        installField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));  // Reduced from 35 to 32
        installRow.add(installField);
        installRow.add(Box.createHorizontalStrut(12));

        JButton installButton = ModernButton.createPrimary("Install");
        installButton.setPreferredSize(new Dimension(100, 32));  // Reduced from 35 to 32
        installButton.setMaximumSize(new Dimension(100, 32));
        installButton.addActionListener(e -> installPackage());
        installRow.add(installButton);

        installSection.add(installRow);

        contentPanel.add(installSection);
        contentPanel.add(Box.createVerticalStrut(8));  // Further reduced to eliminate scrolling

        // === Upload .whl File Section ===
        JPanel uploadSection = createSection("Upload .whl File (Air-gapped Install)");

        JLabel uploadHint = createHintLabel("Upload a .whl file for offline/air-gapped installations");
        uploadSection.add(uploadHint);
        uploadSection.add(Box.createVerticalStrut(8));

        JPanel uploadRow = new JPanel();
        uploadRow.setLayout(new BoxLayout(uploadRow, BoxLayout.X_AXIS));
        uploadRow.setBackground(ModernTheme.PANEL_BACKGROUND);
        uploadRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton chooseFileButton = ModernButton.createDefault("Choose File...");
        chooseFileButton.setPreferredSize(new Dimension(120, 32));  // Reduced from 35 to 32
        chooseFileButton.addActionListener(e -> chooseWheelFile());
        uploadRow.add(chooseFileButton);
        uploadRow.add(Box.createHorizontalStrut(12));

        JButton uploadButton = ModernButton.createPrimary("Upload");
        uploadButton.setPreferredSize(new Dimension(100, 32));  // Reduced from 35 to 32
        uploadButton.setMaximumSize(new Dimension(100, 32));
        uploadButton.addActionListener(e -> uploadWheelFile());
        uploadRow.add(uploadButton);
        uploadRow.add(Box.createHorizontalGlue());

        uploadSection.add(uploadRow);

        contentPanel.add(uploadSection);
        contentPanel.add(Box.createVerticalStrut(8));  // Further reduced to eliminate scrolling

        // === Installed Packages Section ===
        JPanel packagesSection = new JPanel();
        packagesSection.setLayout(new BoxLayout(packagesSection, BoxLayout.Y_AXIS));
        packagesSection.setBackground(ModernTheme.PANEL_BACKGROUND);
        packagesSection.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ModernTheme.PANEL_BORDER),
            new EmptyBorder(16, 16, 16, 16)
        ));
        packagesSection.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Section header
        JPanel headerRow = new JPanel();
        headerRow.setLayout(new BoxLayout(headerRow, BoxLayout.X_AXIS));
        headerRow.setBackground(ModernTheme.PANEL_BACKGROUND);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        packageCountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerRow.add(packageCountLabel);
        headerRow.add(Box.createHorizontalGlue());

        JButton refreshButton = ModernButton.createDefault("Refresh");
        refreshButton.setPreferredSize(new Dimension(100, 30));
        refreshButton.addActionListener(e -> refreshPackagesList());
        headerRow.add(refreshButton);

        packagesSection.add(headerRow);
        packagesSection.add(Box.createVerticalStrut(8));

        // Packages table (with fixed height and scrolling)
        JScrollPane tableScrollPane = new JScrollPane(packagesTable);
        tableScrollPane.setBackground(ModernTheme.BACKGROUND_DARKER);
        tableScrollPane.setBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT));
        tableScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableScrollPane.setPreferredSize(new Dimension(850, 250));  // Fixed height, scrolls internally
        tableScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));

        packagesSection.add(tableScrollPane);

        contentPanel.add(packagesSection);
        contentPanel.add(Box.createVerticalStrut(16));  // Fixed spacing

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

        // Assemble dialog WITHOUT main scroll pane (only table scrolls)
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(contentPanel, BorderLayout.CENTER);
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
        section.add(Box.createVerticalStrut(8));

        return section;
    }

    /**
     * Create a hint label with proper styling.
     */
    private JLabel createHintLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));
        label.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * Check connection and load packages list.
     */
    private void checkConnectionAndLoad() {
        Python3RestClient restClient = idePanel.getRestClient();

        if (restClient == null) {
            // Not connected - show warning banner
            warningPanel.setVisible(true);
            disableAllInputs();
        } else {
            // Connected - hide warning and load packages
            warningPanel.setVisible(false);
            enableAllInputs();
            refreshPackagesList();
        }
    }

    /**
     * Disable all input controls when not connected.
     */
    private void disableAllInputs() {
        searchField.setEnabled(false);
        installField.setEnabled(false);
        packagesTable.setEnabled(false);
    }

    /**
     * Enable all input controls when connected.
     */
    private void enableAllInputs() {
        searchField.setEnabled(true);
        installField.setEnabled(true);
        packagesTable.setEnabled(true);
    }

    /**
     * Search for a package on PyPI.
     */
    private void searchPackage() {
        String packageName = searchField.getText().trim();
        if (packageName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a package name to search.",
                "Search Package",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // TODO: Implement when REST endpoint is available
        JOptionPane.showMessageDialog(this,
            "Search functionality will be available once the Gateway REST endpoint is implemented.\n\n" +
            "Endpoint: POST /data/python3integration/api/v1/packages/search\n" +
            "Package: " + packageName,
            "Not Yet Implemented",
            JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Install a package from PyPI.
     */
    private void installPackage() {
        String packageSpec = installField.getText().trim();
        if (packageSpec.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a package name to install (e.g., numpy or numpy==1.24.0).",
                "Install Package",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Parse package name and version
        String packageName = packageSpec;
        String version = null;
        if (packageSpec.contains("==")) {
            String[] parts = packageSpec.split("==");
            packageName = parts[0].trim();
            version = parts.length > 1 ? parts[1].trim() : null;
        }

        // TODO: Implement when REST endpoint is available
        JOptionPane.showMessageDialog(this,
            "Install functionality will be available once the Gateway REST endpoint is implemented.\n\n" +
            "Endpoint: POST /data/python3integration/api/v1/packages/install\n" +
            "Package: " + packageName + (version != null ? " (version: " + version + ")" : ""),
            "Not Yet Implemented",
            JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Choose a .whl file for upload.
     */
    private File selectedWheelFile = null;

    private void chooseWheelFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".whl");
            }

            @Override
            public String getDescription() {
                return "Python Wheel Files (*.whl)";
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedWheelFile = fileChooser.getSelectedFile();
            JOptionPane.showMessageDialog(this,
                "Selected file: " + selectedWheelFile.getName() + "\n\n" +
                "Click 'Upload' to install this wheel file.",
                "File Selected",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Upload a .whl file to the Gateway.
     */
    private void uploadWheelFile() {
        if (selectedWheelFile == null) {
            JOptionPane.showMessageDialog(this,
                "Please choose a .whl file first using the 'Choose File...' button.",
                "Upload Wheel File",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // TODO: Implement when REST endpoint is available
        JOptionPane.showMessageDialog(this,
            "Upload functionality will be available once the Gateway REST endpoint is implemented.\n\n" +
            "Endpoint: POST /data/python3integration/api/v1/packages/upload\n" +
            "File: " + selectedWheelFile.getAbsolutePath(),
            "Not Yet Implemented",
            JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Refresh the packages list from the Gateway.
     */
    private void refreshPackagesList() {
        Python3RestClient restClient = idePanel.getRestClient();

        if (restClient == null) {
            tableModel.setRowCount(0);
            packageCountLabel.setText("Installed Packages (0)");
            return;
        }

        // TODO: Implement when REST endpoint is available
        // For now, show placeholder message
        tableModel.setRowCount(0);
        packageCountLabel.setText("Installed Packages (0)");

        // Placeholder: Add some fake data to show table structure
        tableModel.addRow(new Object[]{"(Refresh will load packages once REST endpoint is implemented)", "", ""});
    }
}
