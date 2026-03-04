package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.inductiveautomation.ignition.examples.python3.designer.ModernTheme;
import com.inductiveautomation.ignition.examples.python3.designer.ModernButton;

/**
 * Dialog for managing Python version installations.
 * Allows users to install/uninstall Python versions from the module.
 *
 * @since v3.1.0
 */
public class VersionManagerDialog extends BaseModuleDialog {
    private static final Logger LOGGER = LoggerFactory.getLogger(VersionManagerDialog.class);

    private static final int DIALOG_WIDTH = 700;
    private static final int DIALOG_HEIGHT = 450;

    private final Python3RestClient restClient;

    private JTable versionsTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    /**
     * Creates a new Version Manager dialog.
     *
     * @param parent the parent frame
     * @param restClient the REST client for Gateway communication
     */
    public VersionManagerDialog(Frame parent, Python3RestClient restClient) {
        super(parent, "Python Version Manager", DIALOG_WIDTH, DIALOG_HEIGHT);
        this.restClient = restClient;

        initComponents();
        layoutComponents();
        loadDistributions();
    }

    private void initComponents() {
        // Table model: Version, Full Version, Status, Size, Pool, Action
        tableModel = new DefaultTableModel(
                new String[]{"Version", "Full Version", "Status", "Size (MB)", "Pool Active", ""},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only action column is "editable" (for button)
            }
        };

        versionsTable = new JTable(tableModel);
        versionsTable.setRowHeight(36);
        versionsTable.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 13));
        versionsTable.getTableHeader().setFont(ModernTheme.withSize(ModernTheme.FONT_BOLD, 13));
        versionsTable.setSelectionBackground(ModernTheme.TREE_SELECTION);
        versionsTable.setSelectionForeground(ModernTheme.FOREGROUND_PRIMARY);
        versionsTable.setGridColor(ModernTheme.BORDER_DEFAULT);
        versionsTable.setBackground(ModernTheme.EDITOR_BACKGROUND);
        versionsTable.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        versionsTable.getTableHeader().setBackground(ModernTheme.BACKGROUND_LIGHT);
        versionsTable.getTableHeader().setForeground(ModernTheme.FOREGROUND_SECONDARY);

        // Column widths
        versionsTable.getColumnModel().getColumn(0).setPreferredWidth(80);   // Version
        versionsTable.getColumnModel().getColumn(1).setPreferredWidth(100);  // Full Version
        versionsTable.getColumnModel().getColumn(2).setPreferredWidth(100);  // Status
        versionsTable.getColumnModel().getColumn(3).setPreferredWidth(80);   // Size
        versionsTable.getColumnModel().getColumn(4).setPreferredWidth(80);   // Pool
        versionsTable.getColumnModel().getColumn(5).setPreferredWidth(140);  // Action

        // Custom renderer for the Status column
        versionsTable.getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());

        // Custom renderer and editor for the Action column
        versionsTable.getColumnModel().getColumn(5).setCellRenderer(new ActionButtonRenderer());
        versionsTable.getColumnModel().getColumn(5).setCellEditor(new ActionButtonEditor());

        statusLabel = new JLabel("Loading...");
        statusLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        statusLabel.setFont(ModernTheme.FONT_REGULAR);

        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(200, 16));
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 8));
        mainPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ModernTheme.PANEL_BACKGROUND);

        JLabel titleLabel = new JLabel("Python Version Manager");
        titleLabel.setFont(ModernTheme.withSize(ModernTheme.FONT_BOLD, 16));
        titleLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JLabel descLabel = new JLabel("Select Python versions to install for use with the module");
        descLabel.setFont(ModernTheme.FONT_REGULAR);
        descLabel.setForeground(ModernTheme.FOREGROUND_MUTED);
        descLabel.setBorder(new EmptyBorder(4, 0, 0, 0));

        JPanel headerLeft = new JPanel();
        headerLeft.setLayout(new BoxLayout(headerLeft, BoxLayout.Y_AXIS));
        headerLeft.setBackground(ModernTheme.PANEL_BACKGROUND);
        headerLeft.add(titleLabel);
        headerLeft.add(descLabel);
        headerPanel.add(headerLeft, BorderLayout.CENTER);

        // Refresh button
        ModernButton refreshButton = ModernButton.createSmall("Refresh");
        refreshButton.addActionListener(e -> loadDistributions());
        headerPanel.add(refreshButton, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Table in scroll pane
        JScrollPane scrollPane = new JScrollPane(versionsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT));
        scrollPane.getViewport().setBackground(ModernTheme.EDITOR_BACKGROUND);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom: Status and close
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 0));
        bottomPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        bottomPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statusPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        statusPanel.add(statusLabel);
        statusPanel.add(progressBar);
        bottomPanel.add(statusPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        ModernButton closeButton = ModernButton.createDefault("Close");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    /**
     * Loads distribution info from the Gateway.
     */
    private void loadDistributions() {
        if (restClient == null) {
            statusLabel.setText("Not connected to Gateway");
            statusLabel.setForeground(ModernTheme.ERROR);
            return;
        }

        statusLabel.setText("Loading...");
        statusLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        progressBar.setVisible(true);

        new SwingWorker<List<Python3RestClient.DistributionInfo>, Void>() {
            @Override
            protected List<Python3RestClient.DistributionInfo> doInBackground() throws Exception {
                return restClient.getDistributions();
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    List<Python3RestClient.DistributionInfo> distributions = get();
                    populateTable(distributions);

                    long installed = distributions.stream().filter(d -> d.installed).count();
                    statusLabel.setText(String.format("%d version(s) installed, %d available",
                            installed, distributions.size()));
                    statusLabel.setForeground(ModernTheme.SUCCESS);

                } catch (Exception e) {
                    LOGGER.error("Failed to load distributions", e);
                    statusLabel.setText("Failed to load: " + e.getMessage());
                    statusLabel.setForeground(ModernTheme.ERROR);
                }
            }
        }.execute();
    }

    private void populateTable(List<Python3RestClient.DistributionInfo> distributions) {
        tableModel.setRowCount(0);

        for (Python3RestClient.DistributionInfo dist : distributions) {
            String status = dist.installed ? "Installed" : (dist.available ? "Available" : "Not Available");
            String size = dist.installed && dist.installSizeMB > 0 ? String.valueOf(dist.installSizeMB) : "-";
            String pool = dist.installed ? (dist.poolActive ? "Active" : "Inactive") : "-";
            String action = dist.installed ? "Uninstall" : (dist.available ? "Install" : "");

            tableModel.addRow(new Object[]{
                    "Python " + dist.version,
                    dist.fullVersion,
                    status,
                    size,
                    pool,
                    action
            });
        }
    }

    /**
     * Install a Python version.
     */
    private void installVersion(String version, int row) {
        statusLabel.setText("Installing Python " + version + "... (this may take a few minutes)");
        statusLabel.setForeground(ModernTheme.ACCENT_PRIMARY);
        progressBar.setVisible(true);

        // Disable the action column while installing
        tableModel.setValueAt("Installing...", row, 5);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return restClient.installPythonVersion(version);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    boolean success = get();
                    if (success) {
                        statusLabel.setText("Python " + version + " installed successfully!");
                        statusLabel.setForeground(ModernTheme.SUCCESS);
                    } else {
                        statusLabel.setText("Failed to install Python " + version);
                        statusLabel.setForeground(ModernTheme.ERROR);
                    }
                } catch (Exception e) {
                    LOGGER.error("Install failed for Python {}", version, e);
                    statusLabel.setText("Install failed: " + e.getMessage());
                    statusLabel.setForeground(ModernTheme.ERROR);
                }

                // Refresh the table
                loadDistributions();
            }
        }.execute();
    }

    /**
     * Uninstall a Python version.
     */
    private void uninstallVersion(String version, int row) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Uninstall Python " + version + "?\n\n"
                        + "This will remove the downloaded Python distribution.\n"
                        + "The module will need to be restarted to update active pools.",
                "Confirm Uninstall",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        statusLabel.setText("Uninstalling Python " + version + "...");
        statusLabel.setForeground(ModernTheme.WARNING);
        progressBar.setVisible(true);

        tableModel.setValueAt("Removing...", row, 5);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return restClient.uninstallPythonVersion(version);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    boolean success = get();
                    if (success) {
                        statusLabel.setText("Python " + version + " uninstalled. Restart module to update pools.");
                        statusLabel.setForeground(ModernTheme.SUCCESS);
                    } else {
                        statusLabel.setText("Failed to uninstall Python " + version);
                        statusLabel.setForeground(ModernTheme.ERROR);
                    }
                } catch (Exception e) {
                    LOGGER.error("Uninstall failed for Python {}", version, e);
                    statusLabel.setText("Uninstall failed: " + e.getMessage());
                    statusLabel.setForeground(ModernTheme.ERROR);
                }

                loadDistributions();
            }
        }.execute();
    }

    /**
     * Extract the version number from the display string (e.g., "Python 3.11" -> "3.11").
     */
    private String extractVersion(String displayString) {
        if (displayString != null && displayString.startsWith("Python ")) {
            return displayString.substring("Python ".length());
        }
        return displayString;
    }

    // =========================================================================
    // Custom Table Renderers
    // =========================================================================

    /**
     * Renders the Status column with color coding.
     */
    private class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                String status = value != null ? value.toString() : "";
                if ("Installed".equals(status)) {
                    setForeground(ModernTheme.SUCCESS);
                } else if ("Available".equals(status)) {
                    setForeground(ModernTheme.ACCENT_PRIMARY);
                } else {
                    setForeground(ModernTheme.FOREGROUND_MUTED);
                }
                setBackground(ModernTheme.EDITOR_BACKGROUND);
            }

            return c;
        }
    }

    /**
     * Renders the Action column as a button-like label.
     */
    private class ActionButtonRenderer extends JButton implements TableCellRenderer {
        public ActionButtonRenderer() {
            setOpaque(true);
            setFont(ModernTheme.FONT_REGULAR);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            String text = value != null ? value.toString() : "";
            setText(text);

            if ("Install".equals(text)) {
                setBackground(ModernTheme.darken(ModernTheme.SUCCESS, 0.5));
                setForeground(Color.WHITE);
            } else if ("Uninstall".equals(text)) {
                setBackground(ModernTheme.darken(ModernTheme.ERROR, 0.5));
                setForeground(Color.WHITE);
            } else if ("Installing...".equals(text) || "Removing...".equals(text)) {
                setBackground(ModernTheme.darken(ModernTheme.WARNING, 0.5));
                setForeground(Color.WHITE);
                setEnabled(false);
            } else {
                setBackground(ModernTheme.BORDER_DEFAULT);
                setForeground(ModernTheme.FOREGROUND_MUTED);
                setEnabled(false);
            }

            return this;
        }
    }

    /**
     * Handles clicks on the Action column buttons.
     */
    private class ActionButtonEditor extends javax.swing.AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private JButton button;
        private String action;
        private int currentRow;

        public ActionButtonEditor() {
            button = ModernButton.createSmall("Install");
            button.addActionListener(e -> {
                fireEditingStopped();
                String version = extractVersion((String) tableModel.getValueAt(currentRow, 0));

                if ("Install".equals(action)) {
                    installVersion(version, currentRow);
                } else if ("Uninstall".equals(action)) {
                    uninstallVersion(version, currentRow);
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            action = value != null ? value.toString() : "";
            currentRow = row;
            button.setText(action);

            if ("Install".equals(action)) {
                button.setBackground(ModernTheme.darken(ModernTheme.SUCCESS, 0.5));
                button.setForeground(Color.WHITE);
                button.setEnabled(true);
            } else if ("Uninstall".equals(action)) {
                button.setBackground(ModernTheme.darken(ModernTheme.ERROR, 0.5));
                button.setForeground(Color.WHITE);
                button.setEnabled(true);
            } else {
                button.setEnabled(false);
            }

            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return action;
        }
    }
}
