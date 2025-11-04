package com.inductiveautomation.ignition.examples.python3.designer;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;
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
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
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
    private JPanel venvInfoPanel;
    private JLabel venvStatusLabel;

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

        // Virtual environment status label
        venvStatusLabel = new JLabel("Python Environment: Checking...");
        venvStatusLabel.setFont(ModernTheme.FONT_REGULAR);
        venvStatusLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);

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
        contentPanel.add(Box.createVerticalStrut(12));

        // === Virtual Environment Info Panel ===
        venvInfoPanel = new JPanel(new BorderLayout());
        venvInfoPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        venvInfoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT),
            new EmptyBorder(10, 14, 10, 14)
        ));
        venvInfoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        venvInfoPanel.add(venvStatusLabel, BorderLayout.CENTER);

        contentPanel.add(venvInfoPanel);
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
     * Search for a package on PyPI (v2.11.4: Implemented using PyPI JSON API).
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

        Python3RestClient restClient = idePanel.getRestClient();
        if (restClient == null) {
            JOptionPane.showMessageDialog(this,
                "Not connected to gateway. Please connect first.",
                "Connection Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // v2.11.4: Search PyPI using Python requests library via REST API
        // v2.11.7: Fixed - serialize result to JSON string for proper Gateway parsing
        String pythonCode = String.format(
            "import json\n" +
            "try:\n" +
            "    import urllib.request\n" +
            "    url = 'https://pypi.org/pypi/%s/json'\n" +
            "    with urllib.request.urlopen(url) as response:\n" +
            "        data = json.loads(response.read())\n" +
            "    result = json.dumps({\n" +
            "        'name': data['info']['name'],\n" +
            "        'version': data['info']['version'],\n" +
            "        'summary': data['info']['summary'] or 'No description available',\n" +
            "        'author': data['info']['author'] or 'Unknown',\n" +
            "        'license': data['info']['license'] or 'Unknown',\n" +
            "        'home_page': data['info']['home_page'] or 'N/A'\n" +
            "    })\n" +
            "except Exception as e:\n" +
            "    result = json.dumps({'error': str(e)})\n",
            packageName.replace("'", "'\\''")  // Escape single quotes
        );

        try {
            ExecutionResult result = restClient.executeCode(pythonCode, null);
            String resultStr = result.getResult();

            // v2.11.7: Parse result using proper JSON parser (now that Python returns JSON string)
            try {
                JsonObject resultJson = JsonParser.parseString(resultStr).getAsJsonObject();

                // Check for error
                if (resultJson.has("error")) {
                    String errorMsg = resultJson.get("error").getAsString();
                    DarkDialog.showMessage(this,
                        "Package not found on PyPI: " + packageName + "\n\n" +
                        "Error: " + errorMsg + "\n\n" +
                        "Make sure the package name is spelled correctly.",
                        "Package Not Found");
                } else {
                    // Extract package details from JSON
                    String name = resultJson.has("name") ? resultJson.get("name").getAsString() : packageName;
                    String version = resultJson.has("version") ? resultJson.get("version").getAsString() : "Unknown";
                    String summary = resultJson.has("summary") ? resultJson.get("summary").getAsString() : "No description available";
                    String author = resultJson.has("author") ? resultJson.get("author").getAsString() : "Unknown";
                    String license = resultJson.has("license") ? resultJson.get("license").getAsString() : "Not specified";
                    String homePage = resultJson.has("home_page") ? resultJson.get("home_page").getAsString() : "N/A";

                    // Display results using DarkDialog for proper theming
                    String message = String.format(
                        "%s %s\n\n" +
                        "Summary: %s\n\n" +
                        "Author: %s\n" +
                        "License: %s\n" +
                        "Homepage: %s\n\n" +
                        "To install this package, use the 'Install from PyPI' section below.",
                        name, version, summary, author, license, homePage
                    );

                    DarkDialog.showMessage(this, message, "PyPI Package Details");

                    // Pre-fill install field with package name
                    installField.setText(name);
                }
            } catch (Exception parseEx) {
                // Fallback if JSON parsing fails
                DarkDialog.showMessage(this,
                    "Failed to parse search result. Raw response:\n\n" + resultStr,
                    "Parse Error");
            }
        } catch (Exception ex) {
            DarkDialog.showMessage(this,
                "Failed to search PyPI:\n\n" + ex.getMessage() + "\n\nCheck gateway connection and network access.",
                "Search Error");
        }
    }

    /**
     * Extract a value from a JSON string (simple parser for basic key-value extraction).
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) {
            return "";
        }

        startIndex += searchKey.length();
        // Skip whitespace and opening quote
        while (startIndex < json.length() && (json.charAt(startIndex) == ' ' || json.charAt(startIndex) == '"')) {
            startIndex++;
        }

        // Find closing quote or comma
        int endIndex = startIndex;
        boolean inString = json.charAt(startIndex - 1) == '"';
        while (endIndex < json.length()) {
            char c = json.charAt(endIndex);
            if (inString && c == '"' && json.charAt(endIndex - 1) != '\\') {
                break;
            } else if (!inString && (c == ',' || c == '}')) {
                break;
            }
            endIndex++;
        }

        String value = json.substring(startIndex, endIndex).trim();
        // Remove trailing quote if present
        if (value.endsWith("\"")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * Install a package from PyPI (v2.11.4: Implemented using pip subprocess, v2.11.5: DarkDialog theming + better error handling).
     */
    private void installPackage() {
        String packageSpec = installField.getText().trim();
        if (packageSpec.isEmpty()) {
            DarkDialog.showMessage(this,
                "Please enter a package name to install (e.g., numpy or numpy==1.24.0).",
                "Install Package");
            return;
        }

        Python3RestClient restClient = idePanel.getRestClient();
        if (restClient == null) {
            DarkDialog.showMessage(this,
                "Not connected to gateway. Please connect first.",
                "Connection Required");
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

        // Confirm installation
        boolean confirm = DarkDialog.showConfirm(this,
            "Install package: " + packageSpec + "\n\n" +
            "This will run 'pip install " + packageSpec + " --break-system-packages'\n" +
            "on the gateway. Continue?",
            "Confirm Installation");

        if (!confirm) {
            return;
        }

        // v2.11.4: Install package using pip via Python subprocess
        // v2.11.7: Fixed - renamed variable to avoid collision, serialize result to JSON string
        String pythonCode = String.format(
            "import subprocess\n" +
            "import json\n" +
            "import os\n" +
            "os.environ['PATH'] = '/usr/local/bin:/usr/bin:/bin:/usr/local/sbin:/usr/sbin:/sbin'\n" +
            "try:\n" +
            "    proc_result = subprocess.run(['pip3', 'install', '%s', '--break-system-packages'], " +
            "        capture_output=True, text=True, timeout=300)\n" +
            "    output = proc_result.stdout\n" +
            "    if proc_result.stderr:\n" +
            "        output += '\\n' + proc_result.stderr\n" +
            "    if proc_result.returncode == 0:\n" +
            "        result = json.dumps({'success': True, 'output': output})\n" +
            "    else:\n" +
            "        result = json.dumps({'success': False, 'error': output})\n" +
            "except Exception as e:\n" +
            "    result = json.dumps({'success': False, 'error': str(e)})\n",
            packageSpec.replace("'", "'\\''")  // Escape single quotes
        );

        try {
            // Show progress dialog (non-blocking)
            JOptionPane progressPane = new JOptionPane(
                "Installing " + packageSpec + "...\nThis may take a minute...",
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                new Object[]{},
                null);
            JDialog progressDialog = progressPane.createDialog(this, "Installing Package");
            progressDialog.setModal(false);
            progressDialog.setVisible(true);

            try {
                ExecutionResult result = restClient.executeCode(pythonCode, null);
                String resultStr = result.getResult();

                progressDialog.dispose();

                // v2.11.7: Parse result using proper JSON parser (now that Python returns JSON string)
                try {
                    JsonObject resultJson = JsonParser.parseString(resultStr).getAsJsonObject();

                    if (resultJson.has("success") && resultJson.get("success").getAsBoolean()) {
                        DarkDialog.showMessage(this,
                            "Package installed successfully: " + packageSpec + "\n\n" +
                            "Refresh the packages list to see the new package.",
                            "Installation Successful");

                        // Refresh packages list
                        refreshPackagesList();
                    } else {
                        // Extract error from JSON
                        String error = resultJson.has("error")
                            ? resultJson.get("error").getAsString()
                            : "Installation failed. Check gateway logs for details.\n\n" +
                              "Common causes:\n" +
                              "- Package not found on PyPI\n" +
                              "- Network connectivity issues\n" +
                              "- Missing system dependencies\n" +
                              "- Insufficient permissions";

                        DarkDialog.showMessage(this,
                            "Failed to install package: " + packageSpec + "\n\n" +
                            "Error:\n" + error,
                            "Installation Failed");
                    }
                } catch (Exception parseEx) {
                    // Fallback if JSON parsing fails
                    DarkDialog.showMessage(this,
                        "Failed to parse installation result. Raw response:\n\n" + resultStr,
                        "Parse Error");
                }
            } finally {
                progressDialog.dispose();
            }
        } catch (Exception ex) {
            DarkDialog.showMessage(this,
                "Failed to install package:\n\n" + ex.getMessage() + "\n\n" +
                "Check gateway connection and network access.",
                "Installation Error");
        }
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
     * Check and display virtual environment status.
     */
    private void checkVenvStatus() {
        Python3RestClient restClient = idePanel.getRestClient();

        if (restClient == null) {
            venvStatusLabel.setText("Python Environment: Not connected to Gateway");
            venvStatusLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);
            return;
        }

        // Execute Python code to detect virtual environment
        String pythonCode =
            "import sys\n" +
            "import os\n" +
            "import json\n" +
            "in_venv = hasattr(sys, 'real_prefix') or (hasattr(sys, 'base_prefix') and sys.base_prefix != sys.prefix)\n" +
            "venv_path = os.environ.get('VIRTUAL_ENV', '')\n" +
            "result = json.dumps({\n" +
            "    'using_venv': in_venv,\n" +
            "    'venv_path': venv_path,\n" +
            "    'python_path': sys.executable,\n" +
            "    'python_version': sys.version.split()[0]\n" +
            "})";

        try {
            ExecutionResult execResult = restClient.executeCode(pythonCode, new java.util.HashMap<>());
            String resultStr = execResult.getResult();
            JsonObject result = JsonParser.parseString(resultStr).getAsJsonObject();

            boolean usingVenv = result.has("using_venv") && result.get("using_venv").getAsBoolean();
            String venvPath = result.has("venv_path") ? result.get("venv_path").getAsString() : "";
            String pythonPath = result.has("python_path") ? result.get("python_path").getAsString() : "";
            String pythonVersion = result.has("python_version") ? result.get("python_version").getAsString() : "";

            if (usingVenv && !venvPath.isEmpty()) {
                venvStatusLabel.setText(String.format(
                    "<html><b>Virtual Environment Active:</b> %s<br/>" +
                    "<span style='color: #888888;'>Python %s at %s</span></html>",
                    venvPath, pythonVersion, pythonPath
                ));
                venvStatusLabel.setForeground(new Color(76, 175, 80));  // Green for active venv
            } else {
                venvStatusLabel.setText(String.format(
                    "<html><b>System Python:</b> Python %s<br/>" +
                    "<span style='color: #888888;'>%s (No virtual environment)</span></html>",
                    pythonVersion, pythonPath
                ));
                venvStatusLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);
            }
        } catch (Exception e) {
            venvStatusLabel.setText("Python Environment: Error detecting environment - " + e.getMessage());
            venvStatusLabel.setForeground(new Color(244, 67, 54));  // Red for error
        }
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

        // Update virtual environment status
        checkVenvStatus();

        // Get installed packages using pip list --format json
        String pythonCode =
            "import subprocess\n" +
            "import json\n" +
            "import os\n" +
            "os.environ['PATH'] = '/usr/local/bin:/usr/bin:/bin:/usr/local/sbin:/usr/sbin:/sbin'\n" +
            "try:\n" +
            "    proc = subprocess.run(['pip3', 'list', '--format', 'json'], " +
            "        capture_output=True, text=True, timeout=30)\n" +
            "    if proc.returncode == 0:\n" +
            "        packages = json.loads(proc.stdout)\n" +
            "        result = json.dumps({'success': True, 'packages': packages})\n" +
            "    else:\n" +
            "        result = json.dumps({'success': False, 'error': proc.stderr})\n" +
            "except Exception as e:\n" +
            "    result = json.dumps({'success': False, 'error': str(e)})";

        try {
            ExecutionResult execResult = restClient.executeCode(pythonCode, new java.util.HashMap<>());
            String resultStr = execResult.getResult();

            JsonObject result = JsonParser.parseString(resultStr).getAsJsonObject();

            if (result.has("success") && result.get("success").getAsBoolean()) {
                // Clear existing rows
                tableModel.setRowCount(0);

                // Parse packages array
                com.inductiveautomation.ignition.common.gson.JsonArray packages =
                    result.getAsJsonArray("packages");

                // Add each package to the table (store package name, not button)
                for (int i = 0; i < packages.size(); i++) {
                    JsonObject pkg = packages.get(i).getAsJsonObject();
                    String name = pkg.has("name") ? pkg.get("name").getAsString() : "Unknown";
                    String version = pkg.has("version") ? pkg.get("version").getAsString() : "Unknown";

                    // Store package name in Actions column (button created by renderer)
                    tableModel.addRow(new Object[]{name, version, name});
                }

                // Update count label
                packageCountLabel.setText("Installed Packages (" + packages.size() + ")");

                // Custom renderer for buttons in Actions column
                packagesTable.getColumn("Actions").setCellRenderer(new TableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value,
                            boolean isSelected, boolean hasFocus, int row, int column) {
                        String packageName = (String) value;
                        JButton btn = ModernButton.createDefault("Uninstall");
                        btn.setPreferredSize(new Dimension(90, 26));
                        return btn;
                    }
                });

                // Custom editor for buttons in Actions column
                packagesTable.getColumn("Actions").setCellEditor(new TableCellEditor() {
                    private String currentPackageName;

                    @Override
                    public Component getTableCellEditorComponent(JTable table, Object value,
                            boolean isSelected, int row, int column) {
                        currentPackageName = (String) value;
                        JButton btn = ModernButton.createDefault("Uninstall");
                        btn.setPreferredSize(new Dimension(90, 26));
                        btn.addActionListener(e -> {
                            uninstallPackage(currentPackageName);
                            fireEditingStopped();
                        });
                        return btn;
                    }

                    @Override
                    public Object getCellEditorValue() {
                        return currentPackageName;
                    }

                    @Override
                    public boolean isCellEditable(java.util.EventObject e) {
                        return true;
                    }

                    @Override
                    public boolean shouldSelectCell(java.util.EventObject e) {
                        return true;
                    }

                    @Override
                    public boolean stopCellEditing() {
                        fireEditingStopped();
                        return true;
                    }

                    @Override
                    public void cancelCellEditing() {
                        fireEditingCanceled();
                    }

                    @Override
                    public void addCellEditorListener(javax.swing.event.CellEditorListener l) {
                        listenerList.add(javax.swing.event.CellEditorListener.class, l);
                    }

                    @Override
                    public void removeCellEditorListener(javax.swing.event.CellEditorListener l) {
                        listenerList.remove(javax.swing.event.CellEditorListener.class, l);
                    }

                    protected void fireEditingStopped() {
                        Object[] listeners = listenerList.getListenerList();
                        for (int i = listeners.length - 2; i >= 0; i -= 2) {
                            if (listeners[i] == javax.swing.event.CellEditorListener.class) {
                                ((javax.swing.event.CellEditorListener) listeners[i + 1])
                                    .editingStopped(new javax.swing.event.ChangeEvent(this));
                            }
                        }
                    }

                    protected void fireEditingCanceled() {
                        Object[] listeners = listenerList.getListenerList();
                        for (int i = listeners.length - 2; i >= 0; i -= 2) {
                            if (listeners[i] == javax.swing.event.CellEditorListener.class) {
                                ((javax.swing.event.CellEditorListener) listeners[i + 1])
                                    .editingCanceled(new javax.swing.event.ChangeEvent(this));
                            }
                        }
                    }

                    private final javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();
                });

                // Force table to refresh
                packagesTable.revalidate();
                packagesTable.repaint();

            } else {
                // Error occurred
                tableModel.setRowCount(0);
                String error = result.has("error") ? result.get("error").getAsString() : "Unknown error";
                tableModel.addRow(new Object[]{"Error: " + error, "", ""});
                packageCountLabel.setText("Installed Packages (Error)");
            }

        } catch (Exception e) {
            tableModel.setRowCount(0);
            tableModel.addRow(new Object[]{"Error loading packages: " + e.getMessage(), "", ""});
            packageCountLabel.setText("Installed Packages (Error)");
        }
    }

    /**
     * Uninstall a package.
     */
    private void uninstallPackage(String packageName) {
        Python3RestClient restClient = idePanel.getRestClient();

        if (restClient == null) {
            DarkDialog.showMessage(this,
                "Not connected to gateway. Please connect first.",
                "Connection Required");
            return;
        }

        // Confirm uninstallation
        boolean confirm = DarkDialog.showConfirm(this,
            "Uninstall package: " + packageName + "\n\n" +
            "This will run 'pip uninstall -y " + packageName + "'\n" +
            "on the gateway. Continue?",
            "Confirm Uninstallation");

        if (!confirm) {
            return;
        }

        // Uninstall package using pip
        String pythonCode = String.format(
            "import subprocess\n" +
            "import json\n" +
            "import os\n" +
            "os.environ['PATH'] = '/usr/local/bin:/usr/bin:/bin:/usr/local/sbin:/usr/sbin:/sbin'\n" +
            "try:\n" +
            "    proc = subprocess.run(['pip3', 'uninstall', '-y', '%s'], " +
            "        capture_output=True, text=True, timeout=60)\n" +
            "    output = proc.stdout\n" +
            "    if proc.stderr:\n" +
            "        output += '\\n' + proc.stderr\n" +
            "    if proc.returncode == 0:\n" +
            "        result = json.dumps({'success': True, 'output': output})\n" +
            "    else:\n" +
            "        result = json.dumps({'success': False, 'error': output})\n" +
            "except Exception as e:\n" +
            "    result = json.dumps({'success': False, 'error': str(e)})\n",
            packageName.replace("'", "'\\''")  // Escape single quotes
        );

        try {
            ExecutionResult result = restClient.executeCode(pythonCode, null);
            String resultStr = result.getResult();

            JsonObject resultJson = JsonParser.parseString(resultStr).getAsJsonObject();

            if (resultJson.has("success") && resultJson.get("success").getAsBoolean()) {
                DarkDialog.showMessage(this,
                    "Package uninstalled successfully: " + packageName + "\n\n" +
                    "Refreshing packages list...",
                    "Uninstallation Successful");

                // Refresh packages list
                refreshPackagesList();
            } else {
                String error = resultJson.has("error")
                    ? resultJson.get("error").getAsString()
                    : "Uninstallation failed. Check gateway logs for details.";

                DarkDialog.showMessage(this,
                    "Failed to uninstall package: " + packageName + "\n\n" +
                    "Error:\n" + error,
                    "Uninstallation Failed");
            }
        } catch (Exception ex) {
            DarkDialog.showMessage(this,
                "Failed to uninstall package:\n\n" + ex.getMessage(),
                "Uninstallation Error");
        }
    }
}
