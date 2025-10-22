package com.inductiveautomation.ignition.examples.python3.designer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Panel displaying real-time performance diagnostics and metrics.
 * Shows execution statistics, pool usage, and gateway impact.
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

    private Python3RestClient restClient;
    private Timer refreshTimer;

    /**
     * Creates a new diagnostics panel.
     */
    public DiagnosticsPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT),
                        "Performance Diagnostics",
                        TitledBorder.DEFAULT_JUSTIFICATION,
                        TitledBorder.DEFAULT_POSITION,
                        ModernTheme.FONT_REGULAR,
                        ModernTheme.FOREGROUND_PRIMARY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        setPreferredSize(new Dimension(250, 200));
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
        JPanel fieldsPanel = new JPanel(new GridLayout(7, 2, 5, 5));  // 5px vertical spacing (was 3)
        fieldsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        fieldsPanel.setBackground(ModernTheme.PANEL_BACKGROUND);

        // v2.5.19: Removed Python Version, Pool Size, Healthy, Available, In Use (shown in bottom status bar)
        fieldsPanel.add(createKeyLabel("Total Executions:"));
        fieldsPanel.add(totalExecutionsLabel);

        fieldsPanel.add(createKeyLabel("Success Rate:"));
        fieldsPanel.add(successRateLabel);

        fieldsPanel.add(createKeyLabel("Avg Time (ms):"));
        fieldsPanel.add(avgExecutionTimeLabel);

        fieldsPanel.add(createKeyLabel("RAM Usage (MB):"));  // v2.5.19: NEW
        fieldsPanel.add(ramUsageLabel);

        fieldsPanel.add(createKeyLabel("CPU Usage (%):"));   // v2.5.21: Changed from ms to %
        fieldsPanel.add(cpuUsageLabel);

        fieldsPanel.add(createKeyLabel("Impact Level:"));
        fieldsPanel.add(impactLevelLabel);

        fieldsPanel.add(createKeyLabel("Health Score:"));
        fieldsPanel.add(healthScoreLabel);

        add(fieldsPanel, BorderLayout.CENTER);

        // Initially show "Not connected"
        clear();

        // v2.0.18: Removed auto-refresh timer to stop log spam
        // Diagnostics now only refresh manually (on demand or after execution)
        // refreshTimer = new Timer(5000, e -> refreshMetrics());
        // refreshTimer.setInitialDelay(1000);
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
            totalExecutionsLabel.setText("—");
            successRateLabel.setText("—");
            successRateLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            avgExecutionTimeLabel.setText("—");
        }

        // v2.5.19: RAM and CPU usage from impact data
        GatewayImpact impact = data.impact;
        if (impact != null) {
            // RAM usage - from impact.getMemoryUsageMb() if available
            if (impact.getMemoryUsageMb() != null && impact.getMemoryUsageMb() > 0) {
                ramUsageLabel.setText(String.format("%.1f", impact.getMemoryUsageMb()));
                ramUsageLabel.setForeground(getMemoryUsageColor(impact.getMemoryUsageMb()));
            } else {
                ramUsageLabel.setText("—");
                ramUsageLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            }

            // v2.5.21: CPU usage percentage - from impact.getCpuUsagePercent()
            if (impact.getCpuUsagePercent() != null) {
                cpuUsageLabel.setText(String.format("%.2f%%", impact.getCpuUsagePercent()));
                cpuUsageLabel.setForeground(getCpuUsageColor(impact.getCpuUsagePercent()));
            } else {
                cpuUsageLabel.setText("0.00%");
                cpuUsageLabel.setForeground(ModernTheme.SUCCESS);
            }

            // Impact level
            impactLevelLabel.setText(impact.getImpactLevel());
            impactLevelLabel.setForeground(getImpactLevelColor(impact.getImpactLevel()));

            // Health score
            healthScoreLabel.setText(String.valueOf(impact.getHealthScore()));
            healthScoreLabel.setForeground(getHealthScoreColor(impact.getHealthScore()));
        } else {
            ramUsageLabel.setText("—");
            ramUsageLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            cpuUsageLabel.setText("—");
            cpuUsageLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            impactLevelLabel.setText("—");
            impactLevelLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            healthScoreLabel.setText("—");
            healthScoreLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        }
    }

    /**
     * Clears all diagnostic fields.
     */
    private void clear() {
        // v2.5.19: Updated to reflect removed duplicate fields
        totalExecutionsLabel.setText("—");
        successRateLabel.setText("—");
        avgExecutionTimeLabel.setText("—");
        ramUsageLabel.setText("—");
        cpuUsageLabel.setText("—");
        impactLevelLabel.setText("—");
        healthScoreLabel.setText("—");

        // Reset colors
        successRateLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        ramUsageLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        cpuUsageLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        impactLevelLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        healthScoreLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
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
        return label;
    }

    /**
     * Creates a value label (displays actual metric values).
     *
     * @return configured label
     */
    private JLabel createValueLabel() {
        JLabel label = new JLabel("—");
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
