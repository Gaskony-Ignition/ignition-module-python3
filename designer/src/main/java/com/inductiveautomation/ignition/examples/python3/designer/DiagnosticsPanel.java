package com.inductiveautomation.ignition.examples.python3.designer;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
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
 * Shows execution statistics, pool usage, gateway impact, and recent module log entries
 * with level filtering (All/Error/Warn/Info) and module-only toggle.
 *
 * v3.6.8: Added module logs table (replaces removed Logs page)
 * v3.9.0: Combined diagnostics+logs with card headers and filter toolbar
 */
public class DiagnosticsPanel extends JPanel implements Themeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsPanel.class);

    // Metric labels
    private final JLabel impactLevelLabel;
    private final JLabel healthScoreLabel;
    private final JLabel totalExecutionsLabel;
    private final JLabel successRateLabel;
    private final JLabel avgExecutionTimeLabel;
    private final JLabel ramUsageLabel;
    private final JLabel cpuUsageLabel;

    // Module logs table
    private final JTable logsTable;
    private final DefaultTableModel logsTableModel;
    private final JLabel logsCountLabel;

    // Log filter controls
    private final ModernButton filterAllBtn;
    private final ModernButton filterErrorBtn;
    private final ModernButton filterWarnBtn;
    private final ModernButton filterInfoBtn;
    private final ModernButton moduleOnlyBtn;
    private String currentLogFilter = "ALL";
    private boolean moduleOnlyFilter = false;
    private final List<Object[]> allLogEntries = new ArrayList<>();

    // Panels and controls promoted to fields for applyTheme() support
    private JPanel fieldsPanel;
    private JPanel topSection;
    private JPanel logsSection;
    private JPanel filterToolbar;
    private JScrollPane logsScrollPane;
    private ModernButton refreshLogsBtn;
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

        // Create metric labels
        impactLevelLabel = createValueLabel();
        healthScoreLabel = createValueLabel();
        totalExecutionsLabel = createValueLabel();
        successRateLabel = createValueLabel();
        avgExecutionTimeLabel = createValueLabel();
        ramUsageLabel = createValueLabel();
        cpuUsageLabel = createValueLabel();

        // Metrics fields grid
        fieldsPanel = new JPanel(new GridLayout(7, 2, 5, 5));
        fieldsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        fieldsPanel.setBackground(ModernTheme.PANEL_BACKGROUND);

        fieldsPanel.add(createKeyLabel("Total Executions:"));
        fieldsPanel.add(totalExecutionsLabel);

        fieldsPanel.add(createKeyLabel("Success Rate:"));
        fieldsPanel.add(successRateLabel);

        fieldsPanel.add(createKeyLabel("Avg Time (ms):"));
        fieldsPanel.add(avgExecutionTimeLabel);

        fieldsPanel.add(createKeyLabel("RAM (Py3/Gw/Max):"));
        fieldsPanel.add(ramUsageLabel);

        fieldsPanel.add(createKeyLabel("CPU (Py3/Gw/Cores):"));
        fieldsPanel.add(cpuUsageLabel);

        fieldsPanel.add(createKeyLabel("Impact Level:"));
        fieldsPanel.add(impactLevelLabel);

        fieldsPanel.add(createKeyLabel("Health Score:"));
        fieldsPanel.add(healthScoreLabel);

        // Top section: card header + metrics
        topSection = new JPanel(new BorderLayout(0, 4));
        topSection.setBackground(ModernTheme.PANEL_BACKGROUND);

        JPanel cardHeader = ModernTheme.createCardHeader("Performance Diagnostics",
                "Execution stats, resource usage, and module health");
        topSection.add(cardHeader, BorderLayout.NORTH);
        topSection.add(fieldsPanel, BorderLayout.CENTER);

        add(topSection, BorderLayout.NORTH);

        // Logs section: card header + filter toolbar + table
        logsSection = new JPanel(new BorderLayout(0, 0));
        logsSection.setBackground(ModernTheme.PANEL_BACKGROUND);
        logsSection.setBorder(new EmptyBorder(8, 0, 0, 0));

        // Gateway Logs card header
        JPanel logsCardHeader = ModernTheme.createCardHeader("Gateway Logs", "Live gateway log entries");
        logsSection.add(logsCardHeader, BorderLayout.NORTH);

        // Filter toolbar
        filterToolbar = new JPanel(new BorderLayout());
        filterToolbar.setBackground(ModernTheme.PANEL_BACKGROUND);
        filterToolbar.setBorder(new EmptyBorder(6, 5, 6, 5));

        // Left side: filter buttons
        JPanel filterBtnsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        filterBtnsPanel.setBackground(ModernTheme.PANEL_BACKGROUND);

        filterAllBtn = ModernButton.createSmall("All");
        filterErrorBtn = ModernButton.createSmall("Error");
        filterWarnBtn = ModernButton.createSmall("Warn");
        filterInfoBtn = ModernButton.createSmall("Info");

        // Set initial active state for "All"
        setFilterButtonActive(filterAllBtn, true);
        setFilterButtonActive(filterErrorBtn, false);
        setFilterButtonActive(filterWarnBtn, false);
        setFilterButtonActive(filterInfoBtn, false);

        filterAllBtn.addActionListener(e -> setLogFilter("ALL"));
        filterErrorBtn.addActionListener(e -> setLogFilter("ERROR"));
        filterWarnBtn.addActionListener(e -> setLogFilter("WARN"));
        filterInfoBtn.addActionListener(e -> setLogFilter("INFO"));

        filterBtnsPanel.add(filterAllBtn);
        filterBtnsPanel.add(filterErrorBtn);
        filterBtnsPanel.add(filterWarnBtn);
        filterBtnsPanel.add(filterInfoBtn);

        // Separator
        JLabel sep = new JLabel(" | ");
        sep.setForeground(ModernTheme.FOREGROUND_MUTED);
        sep.setFont(ModernTheme.FONT_REGULAR);
        filterBtnsPanel.add(sep);

        // Module Only toggle
        moduleOnlyBtn = ModernButton.createSmall("Module Only");
        setFilterButtonActive(moduleOnlyBtn, false);
        moduleOnlyBtn.addActionListener(e -> {
            moduleOnlyFilter = !moduleOnlyFilter;
            setFilterButtonActive(moduleOnlyBtn, moduleOnlyFilter);
            applyLogFilter();
        });
        filterBtnsPanel.add(moduleOnlyBtn);

        filterToolbar.add(filterBtnsPanel, BorderLayout.WEST);

        // Right side: count + refresh
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightControls.setBackground(ModernTheme.PANEL_BACKGROUND);

        logsCountLabel = new JLabel("0 entries");
        logsCountLabel.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));
        logsCountLabel.setForeground(ModernTheme.FOREGROUND_MUTED);
        rightControls.add(logsCountLabel);

        refreshLogsBtn = ModernButton.createSmall("Refresh");
        refreshLogsBtn.addActionListener(e -> refreshLogs());
        rightControls.add(refreshLogsBtn);

        filterToolbar.add(rightControls, BorderLayout.EAST);

        // Center wrapper for toolbar + table
        JPanel logsCenterPanel = new JPanel(new BorderLayout(0, 0));
        logsCenterPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        logsCenterPanel.add(filterToolbar, BorderLayout.NORTH);

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

        logsCenterPanel.add(logsScrollPane, BorderLayout.CENTER);
        logsSection.add(logsCenterPanel, BorderLayout.CENTER);

        add(logsSection, BorderLayout.CENTER);

        // Initially show "Not connected"
        clear();
    }

    /**
     * Sets the active/inactive visual state of a filter button.
     */
    private void setFilterButtonActive(ModernButton btn, boolean active) {
        if (active) {
            btn.setNormalBackground(ModernTheme.ACCENT_PRIMARY);
            btn.setHoverBackground(ModernTheme.ACCENT_HOVER);
            btn.setPressedBackground(ModernTheme.ACCENT_ACTIVE);
            btn.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        } else {
            btn.setNormalBackground(ModernTheme.BUTTON_BACKGROUND);
            btn.setHoverBackground(ModernTheme.BUTTON_HOVER);
            btn.setPressedBackground(ModernTheme.BUTTON_ACTIVE);
            btn.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        }
        btn.repaint();
    }

    /**
     * Sets the current log level filter and updates button states.
     *
     * @param filter "ALL", "ERROR", "WARN", or "INFO"
     */
    private void setLogFilter(String filter) {
        currentLogFilter = filter;

        setFilterButtonActive(filterAllBtn, "ALL".equals(filter));
        setFilterButtonActive(filterErrorBtn, "ERROR".equals(filter));
        setFilterButtonActive(filterWarnBtn, "WARN".equals(filter));
        setFilterButtonActive(filterInfoBtn, "INFO".equals(filter));

        applyLogFilter();
    }

    /**
     * Rebuilds the visible log table from the cached entries based on current filters.
     */
    private void applyLogFilter() {
        logsTableModel.setRowCount(0);
        int count = 0;

        for (Object[] entry : allLogEntries) {
            String level = entry[1] != null ? entry[1].toString().toUpperCase() : "";
            String message = entry[2] != null ? entry[2].toString() : "";

            // Level filter
            if (!"ALL".equals(currentLogFilter) && !level.equals(currentLogFilter)) {
                continue;
            }

            // Module-only filter: only show entries containing "python3" or "Python3"
            if (moduleOnlyFilter) {
                if (!message.toLowerCase().contains("python3")
                        && !level.toLowerCase().contains("python3")) {
                    continue;
                }
            }

            logsTableModel.addRow(entry);
            count++;
        }

        logsCountLabel.setText(count + " entries");
    }

    /**
     * Sets the REST client for fetching metrics.
     *
     * @param restClient the REST client
     */
    public void setRestClient(Python3RestClient restClient) {
        this.restClient = restClient;

        if (restClient != null) {
            refreshMetrics();
            refreshLogs();
        } else {
            clear();
        }
    }

    /**
     * Refreshes metrics from the Gateway.
     */
    public void refreshMetrics() {
        if (restClient == null) {
            clear();
            return;
        }

        SwingWorker<DiagnosticsData, Void> worker = new SwingWorker<DiagnosticsData, Void>() {
            @Override
            protected DiagnosticsData doInBackground() throws Exception {
                PoolStats poolStats = restClient.getPoolStats();
                GatewayImpact impact = restClient.getGatewayImpact();

                String pythonVersion = null;
                try {
                    pythonVersion = restClient.getPythonVersion();
                } catch (Exception e) {
                    LOGGER.warn("Failed to fetch Python version", e);
                }

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
     * Populates the allLogEntries cache and applies current filter.
     */
    public void refreshLogs() {
        if (restClient == null) {
            allLogEntries.clear();
            logsTableModel.setRowCount(0);
            logsCountLabel.setText("0 entries");
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
                            allLogEntries.clear();

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

                                allLogEntries.add(new Object[]{timestamp, level, message});
                            }

                            applyLogFilter();
                        });
                    }
                } catch (Exception e) {
                    LOGGER.debug("Failed to fetch module logs (non-fatal): {}", e.getMessage());
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        allLogEntries.clear();
                        logsTableModel.setRowCount(0);
                        logsCountLabel.setText("unavailable");
                    });
                }
                return null;
            }
        }.execute();
    }

    /**
     * Displays diagnostics data.
     */
    private void displayDiagnostics(DiagnosticsData data) {
        if (data == null || data.poolStats == null) {
            clear();
            return;
        }

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

        GatewayImpact impact = data.impact;
        if (impact != null) {
            // RAM usage
            if (impact.getPython3MemoryMb() != null || impact.getGatewayMemoryMb() != null) {
                double python3Mb = impact.getPython3MemoryMb() != null ? impact.getPython3MemoryMb() : 0.0;
                double gatewayMb = impact.getGatewayMemoryMb() != null ? impact.getGatewayMemoryMb() : 0.0;
                double maxMb = impact.getMaxMemoryMb() != null ? impact.getMaxMemoryMb() : 0.0;

                if (maxMb > 0) {
                    ramUsageLabel.setText(String.format("%.0f / %.0f / %.0f", python3Mb, gatewayMb, maxMb));
                } else {
                    ramUsageLabel.setText(String.format("%.0f / %.0f", python3Mb, gatewayMb));
                }
                ramUsageLabel.setForeground(getMemoryUsageColor(python3Mb));
            } else {
                ramUsageLabel.setText("\u2014");
                ramUsageLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            }

            // CPU usage
            if (impact.getPython3CpuPercent() != null || impact.getGatewayCpuPercent() != null) {
                double python3Cpu = impact.getPython3CpuPercent() != null ? impact.getPython3CpuPercent() : 0.0;
                double gatewayCpu = impact.getGatewayCpuPercent() != null ? impact.getGatewayCpuPercent() : 0.0;
                int cores = impact.getAvailableCores() != null ? impact.getAvailableCores() : 0;

                if (cores > 0) {
                    cpuUsageLabel.setText(String.format("%.1f%% / %.1f%% / %d", python3Cpu, gatewayCpu, cores));
                } else {
                    cpuUsageLabel.setText(String.format("%.1f%% / %.1f%%", python3Cpu, gatewayCpu));
                }
                cpuUsageLabel.setForeground(getCpuUsageColor(python3Cpu));
            } else {
                cpuUsageLabel.setText("\u2014");
                cpuUsageLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            }

            impactLevelLabel.setText(impact.getImpactLevel());
            impactLevelLabel.setForeground(getImpactLevelColor(impact.getImpactLevel()));

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
     * Clears all diagnostic fields and log entries.
     */
    private void clear() {
        totalExecutionsLabel.setText("\u2014");
        successRateLabel.setText("\u2014");
        avgExecutionTimeLabel.setText("\u2014");
        ramUsageLabel.setText("\u2014");
        cpuUsageLabel.setText("\u2014");
        impactLevelLabel.setText("\u2014");
        healthScoreLabel.setText("\u2014");

        successRateLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        ramUsageLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        cpuUsageLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        impactLevelLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        healthScoreLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);

        allLogEntries.clear();
        logsTableModel.setRowCount(0);
        logsCountLabel.setText("0 entries");
    }

    private Color getMemoryUsageColor(double memoryMb) {
        if (memoryMb <= 100) {
            return ModernTheme.SUCCESS;
        } else if (memoryMb <= 250) {
            return ModernTheme.WARNING;
        } else {
            return ModernTheme.ERROR;
        }
    }

    private Color getCpuUsageColor(double cpuPercent) {
        if (cpuPercent <= 25) {
            return ModernTheme.SUCCESS;
        } else if (cpuPercent <= 50) {
            return ModernTheme.WARNING;
        } else {
            return ModernTheme.ERROR;
        }
    }

    private Color getImpactLevelColor(String level) {
        if (level == null) {
            return ModernTheme.FOREGROUND_PRIMARY;
        }
        switch (level.toUpperCase()) {
            case "LOW":       return ModernTheme.SUCCESS;
            case "MODERATE":  return ModernTheme.WARNING;
            case "HIGH":
            case "CRITICAL":  return ModernTheme.ERROR;
            default:          return ModernTheme.FOREGROUND_PRIMARY;
        }
    }

    private Color getHealthScoreColor(int score) {
        if (score >= 80) return ModernTheme.SUCCESS;
        else if (score >= 60) return ModernTheme.WARNING;
        else return ModernTheme.ERROR;
    }

    private Color getSuccessRateColor(double rate) {
        if (rate >= 95.0) return ModernTheme.SUCCESS;
        else if (rate >= 85.0) return ModernTheme.WARNING;
        else return ModernTheme.ERROR;
    }

    /**
     * Applies the current theme to this panel and its children.
     *
     * @param isDark true for dark theme, false for light theme
     */
    public void applyTheme(boolean isDark) {
        Color bg = isDark ? ModernTheme.PANEL_BACKGROUND : Color.WHITE;
        Color bgDarker = isDark ? ModernTheme.BACKGROUND_DARKER : new Color(245, 245, 248);
        Color fg = isDark ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK;
        Color fgSecondary = isDark ? ModernTheme.FOREGROUND_SECONDARY : new Color(80, 80, 80);
        Color fgMuted = isDark ? ModernTheme.FOREGROUND_MUTED : new Color(100, 100, 100);
        Color border = isDark ? ModernTheme.BORDER_SUBTLE : new Color(208, 208, 216);
        Color buttonBg = isDark ? ModernTheme.BUTTON_BACKGROUND : ModernTheme.LIGHT_BUTTON_BG;
        Color accentPrimary = isDark ? ModernTheme.ACCENT_PRIMARY : ModernTheme.LIGHT_PRIMARY;
        Color accentHover = isDark ? ModernTheme.ACCENT_HOVER : ModernTheme.LIGHT_PRIMARY_HOVER;
        Color accentActive = isDark ? ModernTheme.ACCENT_ACTIVE : ModernTheme.LIGHT_PRIMARY_ACTIVE;
        Color buttonHover = isDark ? ModernTheme.BUTTON_HOVER : ModernTheme.LIGHT_BUTTON_HOVER;
        Color buttonActive = isDark ? ModernTheme.BUTTON_ACTIVE : ModernTheme.LIGHT_BUTTON_ACTIVE;

        // Outer panel
        setBackground(bg);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                BorderFactory.createEmptyBorder(0, 0, 5, 0)
        ));

        // Sub-panels
        if (fieldsPanel != null) fieldsPanel.setBackground(bg);
        if (topSection != null) topSection.setBackground(bg);
        if (logsSection != null) logsSection.setBackground(bg);
        if (filterToolbar != null) filterToolbar.setBackground(bg);

        // Update filter toolbar child panels
        for (Component c : filterToolbar.getComponents()) {
            if (c instanceof JPanel) {
                c.setBackground(bg);
            }
        }

        // Logs count label
        logsCountLabel.setForeground(fgMuted);

        // Key labels
        for (JLabel keyLabel : keyLabels) {
            keyLabel.setForeground(fgSecondary);
        }

        // Value labels
        JLabel[] valueLabels = {totalExecutionsLabel, successRateLabel, avgExecutionTimeLabel,
                ramUsageLabel, cpuUsageLabel, impactLevelLabel, healthScoreLabel};
        for (JLabel label : valueLabels) {
            label.setForeground(fg);
        }

        // Update filter button colors based on current state
        updateFilterButtonTheme(filterAllBtn, "ALL".equals(currentLogFilter),
                accentPrimary, accentHover, accentActive, buttonBg, buttonHover, buttonActive, fg, fgSecondary);
        updateFilterButtonTheme(filterErrorBtn, "ERROR".equals(currentLogFilter),
                accentPrimary, accentHover, accentActive, buttonBg, buttonHover, buttonActive, fg, fgSecondary);
        updateFilterButtonTheme(filterWarnBtn, "WARN".equals(currentLogFilter),
                accentPrimary, accentHover, accentActive, buttonBg, buttonHover, buttonActive, fg, fgSecondary);
        updateFilterButtonTheme(filterInfoBtn, "INFO".equals(currentLogFilter),
                accentPrimary, accentHover, accentActive, buttonBg, buttonHover, buttonActive, fg, fgSecondary);
        updateFilterButtonTheme(moduleOnlyBtn, moduleOnlyFilter,
                accentPrimary, accentHover, accentActive, buttonBg, buttonHover, buttonActive, fg, fgSecondary);

        // Refresh button
        if (refreshLogsBtn != null) {
            refreshLogsBtn.setNormalBackground(buttonBg);
            refreshLogsBtn.setHoverBackground(buttonHover);
            refreshLogsBtn.setPressedBackground(buttonActive);
            refreshLogsBtn.setForeground(fg);
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
     * Updates a filter button's colors based on active state and current theme.
     */
    private void updateFilterButtonTheme(ModernButton btn, boolean active,
            Color accentBg, Color accentHover, Color accentActive,
            Color normalBg, Color normalHover, Color normalActive,
            Color fgPrimary, Color fgSecondary) {
        if (active) {
            btn.setNormalBackground(accentBg);
            btn.setHoverBackground(accentHover);
            btn.setPressedBackground(accentActive);
            btn.setForeground(fgPrimary);
        } else {
            btn.setNormalBackground(normalBg);
            btn.setHoverBackground(normalHover);
            btn.setPressedBackground(normalActive);
            btn.setForeground(fgSecondary);
        }
        btn.repaint();
    }

    private JLabel createKeyLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ModernTheme.withSize(ModernTheme.FONT_BOLD, 12));
        label.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        keyLabels.add(label);
        return label;
    }

    private JLabel createValueLabel() {
        JLabel label = new JLabel("\u2014");
        label.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 12));
        label.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        return label;
    }

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
     */
    public void dispose() {
        // No cleanup needed - auto-refresh timer removed in v2.0.18
    }
}
