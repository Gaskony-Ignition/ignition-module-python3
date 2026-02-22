package com.inductiveautomation.ignition.examples.python3.designer;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel displaying real-time performance diagnostics, metrics, and module logs.
 * Shows execution statistics, pool usage, gateway impact, and recent module log entries.
 *
 * v3.6.8: Added module logs table (replaces removed Logs page)
 */
public class DiagnosticsPanel extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsPanel.class);

    // v2.5.19: Removed duplicate labels (poolSize, healthy, available, inUse, pythonVersion)
    // These are now only shown in the bottom status bar
    private final JLabel impactLevelLabel;
    private final JLabel healthScoreLabel;
    private final JLabel totalExecutionsLabel;
    private final JLabel successRateLabel;
    private final JLabel avgExecutionTimeLabel;
    private final JLabel ramUsageLabel;        // v2.5.19: NEW - RAM usage
    private final JLabel cpuUsageLabel;        // v2.5.19: NEW - CPU usage

    // v3.6.8: Module logs table
    private final JTable logsTable;
    private final DefaultTableModel logsTableModel;
    private final JLabel logsCountLabel;

    // Panels and controls promoted to fields for applyTheme() support
    private JPanel fieldsPanel;
    private JPanel topSection;
    private JPanel logsSection;
    private JPanel logsHeader;
    private JPanel btnPanel;
    private JScrollPane logsScrollPane;
    private JButton refreshLogsBtn;
    private final List<JLabel> keyLabels = new ArrayList<>();

    private Python3RestClient restClient;
    private Timer refreshTimer;

    /**
     * Creates a new diagnostics panel.
     */
    public DiagnosticsPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernTheme.BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(0, 0, 5, 0)
        ));
        setBackground(ModernTheme.PANEL_BACKGROUND);

        // v2.5.19: Create labels (removed duplicates: poolSize, healthy, available, inUse, pythonVersion)
        impactLevelLabel = createValueLabel();
        healthScoreLabel = createValueLabel();
        totalExecutionsLabel = createValueLabel();
        successRateLabel = createValueLabel();
        avgExecutionTimeLabel = createValueLabel();
        ramUsageLabel = createValueLabel();
        cpuUsageLabel = createValueLabel();

        // v2.5.19: Layout - reduced to 7 rows (was 10), larger font for better readability
        fieldsPanel = new JPanel(new GridLayout(7, 2, 5, 5));  // 5px vertical spacing (was 3)
        fieldsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        fieldsPanel.setBackground(ModernTheme.PANEL_BACKGROUND);

        // v2.5.19: Removed Python Version, Pool Size, Healthy, Available, In Use (shown in bottom status bar)
        fieldsPanel.add(createKeyLabel("Total Executions:"));
        fieldsPanel.add(totalExecutionsLabel);

        fieldsPanel.add(createKeyLabel("Success Rate:"));
        fieldsPanel.add(successRateLabel);

        fieldsPanel.add(createKeyLabel("Avg Time (ms):"));
        fieldsPanel.add(avgExecutionTimeLabel);

        fieldsPanel.add(createKeyLabel("RAM (Py3/Gw/Max):"));  // v2.15.5: Python3/Gateway/Max format
        fieldsPanel.add(ramUsageLabel);

        fieldsPanel.add(createKeyLabel("CPU (Py3/Gw/Cores):"));   // v2.15.5: Python3/Gateway/Cores format
        fieldsPanel.add(cpuUsageLabel);

        fieldsPanel.add(createKeyLabel("Impact Level:"));
        fieldsPanel.add(impactLevelLabel);

        fieldsPanel.add(createKeyLabel("Health Score:"));
        fieldsPanel.add(healthScoreLabel);

        // v3.6.8: Floating card header + metrics in top section
        topSection = new JPanel(new BorderLayout(0, 4));
        topSection.setBackground(ModernTheme.PANEL_BACKGROUND);

        JPanel cardHeader = ModernTheme.createCardHeader("Performance Diagnostics",
                "Execution stats, resource usage, and module health");
        topSection.add(cardHeader, BorderLayout.NORTH);
        topSection.add(fieldsPanel, BorderLayout.CENTER);

        add(topSection, BorderLayout.NORTH);

        // v3.6.8: Module logs section
        logsSection = new JPanel(new BorderLayout(0, 4));
        logsSection.setBackground(ModernTheme.PANEL_BACKGROUND);
        logsSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ModernTheme.BORDER_DEFAULT),
                new EmptyBorder(8, 5, 5, 5)
        ));

        // Logs header with count and refresh button
        logsHeader = new JPanel(new BorderLayout());
        logsHeader.setBackground(ModernTheme.PANEL_BACKGROUND);

        logsCountLabel = new JLabel("Module Logs (0)");
        logsCountLabel.setFont(ModernTheme.FONT_BOLD);
        logsCountLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        logsHeader.add(logsCountLabel, BorderLayout.WEST);

        refreshLogsBtn = new JButton("Refresh");
        refreshLogsBtn.setFont(ModernTheme.withSize(ModernTheme.FONT_BOLD, 10));
        refreshLogsBtn.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        refreshLogsBtn.setBackground(ModernTheme.BUTTON_BACKGROUND);
        refreshLogsBtn.setFocusPainted(false);
        refreshLogsBtn.addActionListener(e -> refreshLogs());
        btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        btnPanel.add(refreshLogsBtn);
        logsHeader.add(btnPanel, BorderLayout.EAST);

        logsSection.add(logsHeader, BorderLayout.NORTH);

        // Logs table
        String[] logColumns = {"Time", "Level", "Message"};
        logsTableModel = new DefaultTableModel(logColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        logsTable = new JTable(logsTableModel);
        logsTable.setBackground(ModernTheme.BACKGROUND_DARKER);
        logsTable.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        logsTable.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));
        logsTable.setGridColor(ModernTheme.BORDER_SUBTLE);
        logsTable.setRowHeight(22);
        logsTable.setShowHorizontalLines(true);
        logsTable.setShowVerticalLines(false);
        logsTable.getTableHeader().setBackground(ModernTheme.PANEL_BACKGROUND);
        logsTable.getTableHeader().setForeground(ModernTheme.FOREGROUND_PRIMARY);
        logsTable.getTableHeader().setFont(ModernTheme.withSize(ModernTheme.FONT_BOLD, 11));

        // Column widths
        logsTable.getColumnModel().getColumn(0).setPreferredWidth(70);  // Time
        logsTable.getColumnModel().getColumn(0).setMaxWidth(90);
        logsTable.getColumnModel().getColumn(1).setPreferredWidth(45);  // Level
        logsTable.getColumnModel().getColumn(1).setMaxWidth(55);
        logsTable.getColumnModel().getColumn(2).setPreferredWidth(400); // Message

        // Color-code log level column
        logsTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected && value != null) {
                    String level = value.toString().toUpperCase();
                    switch (level) {
                        case "ERROR": c.setForeground(ModernTheme.ERROR); break;
                        case "WARN":  c.setForeground(ModernTheme.WARNING); break;
                        case "INFO":  c.setForeground(ModernTheme.SUCCESS); break;
                        case "DEBUG": c.setForeground(ModernTheme.FOREGROUND_MUTED); break;
                        default:      c.setForeground(ModernTheme.FOREGROUND_PRIMARY); break;
                    }
                    c.setBackground(ModernTheme.BACKGROUND_DARKER);
                }
                return c;
            }
        });

        logsScrollPane = new JScrollPane(logsTable);
        logsScrollPane.setBackground(ModernTheme.BACKGROUND_DARKER);
        logsScrollPane.setBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_SUBTLE));
        logsScrollPane.getViewport().setBackground(ModernTheme.BACKGROUND_DARKER);

        logsSection.add(logsScrollPane, BorderLayout.CENTER);

        add(logsSection, BorderLayout.CENTER);

        // Initially show "Not connected"
        clear();
    }

    /**
     * Sets the REST client for fetching metrics.
     *
     * @param restClient the REST client
     */
    public void setRestClient(Python3RestClient restClient) {
        this.restClient = restClient;

        if (restClient != null) {
            // v2.0.18: Only refresh once on connection, no auto-refresh timer
            refreshMetrics();
            refreshLogs();
        } else {
            // Clear display when disconnected
            clear();
        }
    }

    /**
     * Refreshes metrics from the Gateway.
     *
     * v2.0.18: Made public for manual refresh (e.g., after code execution)
     */
    public void refreshMetrics() {
        if (restClient == null) {
            clear();
            return;
        }

        SwingWorker<DiagnosticsData, Void> worker = new SwingWorker<DiagnosticsData, Void>() {
            @Override
            protected DiagnosticsData doInBackground() throws Exception {
                // Fetch metrics and pool stats
                PoolStats poolStats = restClient.getPoolStats();

                // Fetch gateway impact (from v1.14.0 endpoint)
                GatewayImpact impact = restClient.getGatewayImpact();

                // Fetch Python version
                String pythonVersion = null;
                try {
                    pythonVersion = restClient.getPythonVersion();
                } catch (Exception e) {
                    LOGGER.warn("Failed to fetch Python version", e);
                }

                // Fetch diagnostics metrics
                ExecutionMetrics metrics = null;
                try {
                    String diagnosticsJson = restClient.getDiagnostics();
                    metrics = ExecutionMetrics.fromJson(diagnosticsJson);
                } catch (Exception e) {
                    LOGGER.warn("Failed to fetch execution metrics", e);
                }

                return new DiagnosticsData(poolStats, impact, pythonVersion, metrics);
            }

            @Override
            protected void done() {
                try {
                    DiagnosticsData data = get();
                    displayDiagnostics(data);
                } catch (Exception e) {
                    LOGGER.warn("Failed to fetch diagnostics", e);
                    clear();
                }
            }
        };

        worker.execute();
    }

    /**
     * Refreshes module logs from the Gateway.
     *
     * @since v3.6.8
     */
    public void refreshLogs() {
        if (restClient == null) {
            logsTableModel.setRowCount(0);
            logsCountLabel.setText("Module Logs (0)");
            return;
        }

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    String response = restClient.getModuleLogs(50);
                    JsonObject json = JsonParser.parseString(response).getAsJsonObject();

                    if (json.has("entries") && json.get("entries").isJsonArray()) {
                        JsonArray entries = json.getAsJsonArray("entries");

                        javax.swing.SwingUtilities.invokeLater(() -> {
                            logsTableModel.setRowCount(0);

                            for (int i = 0; i < entries.size(); i++) {
                                JsonObject entry = entries.get(i).getAsJsonObject();
                                String timestamp = entry.has("timestamp") ? entry.get("timestamp").getAsString() : "";
                                String level = entry.has("level") ? entry.get("level").getAsString() : "";
                                String message = entry.has("message") ? entry.get("message").getAsString() : "";

                                // Shorten timestamp to time only (HH:mm:ss) for display
                                if (timestamp.length() > 8 && timestamp.contains(" ")) {
                                    String[] parts = timestamp.split(" ");
                                    if (parts.length >= 2) {
                                        timestamp = parts[1];
                                        if (timestamp.length() > 8) {
                                            timestamp = timestamp.substring(0, 8);
                                        }
                                    }
                                }

                                logsTableModel.addRow(new Object[]{timestamp, level, message});
                            }

                            logsCountLabel.setText("Module Logs (" + entries.size() + ")");
                        });
                    }
                } catch (Exception e) {
                    LOGGER.debug("Failed to fetch module logs (non-fatal): {}", e.getMessage());
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        logsTableModel.setRowCount(0);
                        logsCountLabel.setText("Module Logs (unavailable)");
                    });
                }
                return null;
            }
        }.execute();
    }

    /**
     * Displays diagnostics data.
     *
     * @param data the diagnostics data
     */
    private void displayDiagnostics(DiagnosticsData data) {
        if (data == null || data.poolStats == null) {
            clear();
            return;
        }

        // v2.5.19: Removed duplicate pool stats display (pythonVersion, poolSize, healthy, available, inUse)
        // These are shown in bottom status bar

        // Execution metrics
        if (data.metrics != null) {
            totalExecutionsLabel.setText(String.valueOf(data.metrics.getTotalExecutions()));

            double successRate = data.metrics.getSuccessRate();
            successRateLabel.setText(String.format("%.1f%%", successRate));
            successRateLabel.setForeground(getSuccessRateColor(successRate));

            avgExecutionTimeLabel.setText(String.format("%.1f", data.metrics.getAverageExecutionTime()));
        } else {
            totalExecutionsLabel.setText("\u2014");
            successRateLabel.setText("\u2014");
            successRateLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            avgExecutionTimeLabel.setText("\u2014");
        }

        // v2.15.5: RAM and CPU usage with Python3/System/Max format
        GatewayImpact impact = data.impact;
        if (impact != null) {
            // RAM usage - Python3/Gateway/Max format
            if (impact.getPython3MemoryMb() != null || impact.getGatewayMemoryMb() != null) {
                double python3Mb = impact.getPython3MemoryMb() != null ? impact.getPython3MemoryMb() : 0.0;
                double gatewayMb = impact.getGatewayMemoryMb() != null ? impact.getGatewayMemoryMb() : 0.0;
                double maxMb = impact.getMaxMemoryMb() != null ? impact.getMaxMemoryMb() : 0.0;

                if (maxMb > 0) {
                    ramUsageLabel.setText(String.format("%.0f / %.0f / %.0f", python3Mb, gatewayMb, maxMb));
                } else {
                    ramUsageLabel.setText(String.format("%.0f / %.0f", python3Mb, gatewayMb));
                }
                // Color based on Python3 usage
                ramUsageLabel.setForeground(getMemoryUsageColor(python3Mb));
            } else {
                ramUsageLabel.setText("\u2014");
                ramUsageLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            }

            // CPU usage - Python3/Gateway/Cores format
            if (impact.getPython3CpuPercent() != null || impact.getGatewayCpuPercent() != null) {
                double python3Cpu = impact.getPython3CpuPercent() != null ? impact.getPython3CpuPercent() : 0.0;
                double gatewayCpu = impact.getGatewayCpuPercent() != null ? impact.getGatewayCpuPercent() : 0.0;
                int cores = impact.getAvailableCores() != null ? impact.getAvailableCores() : 0;

                if (cores > 0) {
                    cpuUsageLabel.setText(String.format("%.1f%% / %.1f%% / %d", python3Cpu, gatewayCpu, cores));
                } else {
                    cpuUsageLabel.setText(String.format("%.1f%% / %.1f%%", python3Cpu, gatewayCpu));
                }
                // Color based on Python3 CPU usage
                cpuUsageLabel.setForeground(getCpuUsageColor(python3Cpu));
            } else {
                cpuUsageLabel.setText("\u2014");
                cpuUsageLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            }

            // Impact level
            impactLevelLabel.setText(impact.getImpactLevel());
            impactLevelLabel.setForeground(getImpactLevelColor(impact.getImpactLevel()));

            // Health score
            healthScoreLabel.setText(String.valueOf(impact.getHealthScore()));
            healthScoreLabel.setForeground(getHealthScoreColor(impact.getHealthScore()));
        } else {
            ramUsageLabel.setText("\u2014");
            ramUsageLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            cpuUsageLabel.setText("\u2014");
            cpuUsageLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            impactLevelLabel.setText("\u2014");
            impactLevelLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            healthScoreLabel.setText("\u2014");
            healthScoreLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        }
    }

    /**
     * Clears all diagnostic fields.
     */
    private void clear() {
        // v2.5.19: Updated to reflect removed duplicate fields
        totalExecutionsLabel.setText("\u2014");
        successRateLabel.setText("\u2014");
        avgExecutionTimeLabel.setText("\u2014");
        ramUsageLabel.setText("\u2014");
        cpuUsageLabel.setText("\u2014");
        impactLevelLabel.setText("\u2014");
        healthScoreLabel.setText("\u2014");

        // Reset colors
        successRateLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        ramUsageLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        cpuUsageLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        impactLevelLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        healthScoreLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);

        // Clear logs table
        logsTableModel.setRowCount(0);
        logsCountLabel.setText("Module Logs (0)");
    }

    /**
     * Gets color for memory usage display (v2.5.19).
     *
     * @param memoryMb memory usage in MB
     * @return color based on usage level
     */
    private Color getMemoryUsageColor(double memoryMb) {
        if (memoryMb <= 100) {
            return ModernTheme.SUCCESS;      // Low usage (< 100 MB)
        } else if (memoryMb <= 250) {
            return ModernTheme.WARNING;      // Moderate usage (100-250 MB)
        } else {
            return ModernTheme.ERROR;        // High usage (> 250 MB)
        }
    }

    /**
     * Gets color for CPU usage display (v2.5.21).
     *
     * @param cpuPercent CPU usage percentage (0-100)
     * @return color based on CPU usage level
     */
    private Color getCpuUsageColor(double cpuPercent) {
        if (cpuPercent <= 25) {
            return ModernTheme.SUCCESS;      // Low usage (< 25%)
        } else if (cpuPercent <= 50) {
            return ModernTheme.WARNING;      // Moderate usage (25-50%)
        } else {
            return ModernTheme.ERROR;        // High usage (> 50%)
        }
    }

    /**
     * Gets color for impact level display.
     */
    private Color getImpactLevelColor(String level) {
        if (level == null) {
            return ModernTheme.FOREGROUND_PRIMARY;
        }

        switch (level.toUpperCase()) {
            case "LOW":
                return ModernTheme.SUCCESS;
            case "MODERATE":
                return ModernTheme.WARNING;
            case "HIGH":
            case "CRITICAL":
                return ModernTheme.ERROR;
            default:
                return ModernTheme.FOREGROUND_PRIMARY;
        }
    }

    /**
     * Gets color for health score display.
     */
    private Color getHealthScoreColor(int score) {
        if (score >= 80) {
            return ModernTheme.SUCCESS;
        } else if (score >= 60) {
            return ModernTheme.WARNING;
        } else {
            return ModernTheme.ERROR;
        }
    }

    /**
     * Gets color for success rate display.
     */
    private Color getSuccessRateColor(double rate) {
        if (rate >= 95.0) {
            return ModernTheme.SUCCESS;
        } else if (rate >= 85.0) {
            return ModernTheme.WARNING;
        } else {
            return ModernTheme.ERROR;
        }
    }

    /**
     * Applies the current theme to this panel and its children.
     * Call this when the IDE theme changes.
     *
     * @param isDark true for dark theme, false for light theme
     */
    public void applyTheme(boolean isDark) {
        Color bg = isDark ? ModernTheme.PANEL_BACKGROUND : Color.WHITE;
        Color bgDarker = isDark ? ModernTheme.BACKGROUND_DARKER : new Color(245, 245, 248);
        Color fg = isDark ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK;
        Color fgSecondary = isDark ? ModernTheme.FOREGROUND_SECONDARY : new Color(80, 80, 80);
        Color border = isDark ? ModernTheme.BORDER_SUBTLE : new Color(208, 208, 216);
        Color panelBorder = isDark ? ModernTheme.BORDER_DEFAULT : new Color(180, 180, 190);
        Color buttonBg = isDark ? ModernTheme.BUTTON_BACKGROUND : ModernTheme.LIGHT_BUTTON_BG;

        // Outer panel
        setBackground(bg);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                BorderFactory.createEmptyBorder(0, 0, 5, 0)
        ));

        // Sub-panels
        if (fieldsPanel != null) fieldsPanel.setBackground(bg);
        if (topSection != null) topSection.setBackground(bg);
        if (logsSection != null) {
            logsSection.setBackground(bg);
            logsSection.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, panelBorder),
                    new EmptyBorder(8, 5, 5, 5)
            ));
        }
        if (logsHeader != null) logsHeader.setBackground(bg);
        if (btnPanel != null) btnPanel.setBackground(bg);

        // Refresh button
        if (refreshLogsBtn != null) {
            refreshLogsBtn.setBackground(buttonBg);
            refreshLogsBtn.setForeground(fg);
        }

        // Logs count label
        logsCountLabel.setForeground(fg);

        // Key labels
        for (JLabel keyLabel : keyLabels) {
            keyLabel.setForeground(fgSecondary);
        }

        // Value labels — reset to default foreground (dynamic colors set by displayDiagnostics)
        JLabel[] valueLabels = {totalExecutionsLabel, successRateLabel, avgExecutionTimeLabel,
                ramUsageLabel, cpuUsageLabel, impactLevelLabel, healthScoreLabel};
        for (JLabel label : valueLabels) {
            label.setForeground(fg);
        }

        // Logs table
        logsTable.setBackground(bgDarker);
        logsTable.setForeground(fg);
        logsTable.setGridColor(border);
        logsTable.getTableHeader().setBackground(bg);
        logsTable.getTableHeader().setForeground(fg);

        // Scroll pane
        if (logsScrollPane != null) {
            logsScrollPane.setBackground(bgDarker);
            logsScrollPane.getViewport().setBackground(bgDarker);
            logsScrollPane.setBorder(BorderFactory.createLineBorder(border));
        }

        repaint();
    }

    /**
     * Creates a key label (e.g., "Total Executions:", "Success Rate:").
     *
     * @param text the label text
     * @return configured label
     */
    private JLabel createKeyLabel(String text) {
        JLabel label = new JLabel(text);
        // v2.5.19: Increased font size from 10 to 12 for better readability
        label.setFont(ModernTheme.withSize(ModernTheme.FONT_BOLD, 12));
        label.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        keyLabels.add(label);  // Track for applyTheme()
        return label;
    }

    /**
     * Creates a value label (displays actual metric values).
     *
     * @return configured label
     */
    private JLabel createValueLabel() {
        JLabel label = new JLabel("\u2014");
        // v2.5.19: Increased font size from 10 to 12 for better readability
        label.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 12));
        label.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        return label;
    }

    /**
     * Container for diagnostics data.
     */
    private static class DiagnosticsData {
        final PoolStats poolStats;
        final GatewayImpact impact;
        final String pythonVersion;
        final ExecutionMetrics metrics;

        DiagnosticsData(PoolStats poolStats, GatewayImpact impact, String pythonVersion, ExecutionMetrics metrics) {
            this.poolStats = poolStats;
            this.impact = impact;
            this.pythonVersion = pythonVersion;
            this.metrics = metrics;
        }
    }

    /**
     * Cleanup method.
     * Call this when the panel is no longer visible.
     *
     * v2.0.18: No timer to stop anymore (auto-refresh removed)
     */
    public void dispose() {
        // No cleanup needed - auto-refresh timer removed in v2.0.18
    }
}
