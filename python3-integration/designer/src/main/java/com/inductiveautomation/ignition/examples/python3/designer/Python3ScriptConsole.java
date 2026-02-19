package com.inductiveautomation.ignition.examples.python3.designer;

import com.inductiveautomation.ignition.designer.model.DesignerContext;
import com.inductiveautomation.ignition.examples.python3.designer.managers.ThemeManager;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CaretEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Lightweight Python 3 Script Console for the Ignition Designer.
 * Provides code execution, load/save, theme toggle, and basic IDE features
 * without the full tree browser, terminal, or package management.
 *
 * @since v3.3.0
 */
public class Python3ScriptConsole extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(Python3ScriptConsole.class);

    private static final String PREF_THEME = "python3console.theme";
    private static final String PREF_SPLIT_ORIENTATION = "python3console.splitOrientation";

    private final Python3RestClient restClient;
    private final ThemeManager themeManager;
    private final Preferences prefs;

    private RSyntaxTextArea codeEditor;
    private JTextArea outputArea;
    private JTextArea errorArea;
    private JTabbedPane outputTabs;
    private JSplitPane splitPane;
    private ModernStatusBar statusBar;
    private JComboBox<String> versionCombo;
    private JButton themeToggleButton;
    private JButton runButton;

    /**
     * Creates a new Python 3 Script Console.
     *
     * @param context the Designer context
     */
    public Python3ScriptConsole(DesignerContext context) {
        setLayout(new BorderLayout());
        this.prefs = Preferences.userNodeForPackage(Python3ScriptConsole.class);
        this.restClient = new Python3RestClient(context);
        this.themeManager = new ThemeManager(Python3ScriptConsole.class);

        // Build UI components
        add(createToolbar(), BorderLayout.NORTH);

        // Code editor
        JPanel editorPanel = createEditorPanel();

        // Output tabs
        JPanel outputPanel = createOutputPanel();

        // Split pane
        int savedOrientation = prefs.getInt(PREF_SPLIT_ORIENTATION, JSplitPane.VERTICAL_SPLIT);
        splitPane = new JSplitPane(savedOrientation, editorPanel, outputPanel);
        splitPane.setResizeWeight(0.65);
        splitPane.setDividerSize(5);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        add(splitPane, BorderLayout.CENTER);

        // Status bar
        statusBar = new ModernStatusBar();
        add(statusBar, BorderLayout.SOUTH);

        // Apply saved theme
        applyCurrentTheme();

        // Populate version combo in background
        populateVersionCombo();

        // Update status bar in background
        updateStatusBarAsync();

        // Setup keyboard shortcuts
        setupKeyboardShortcuts();

        // Caret listener for cursor position
        codeEditor.addCaretListener(this::updateCursorPosition);

        LOGGER.info("Python 3 Script Console initialized");
    }

    /**
     * Loads a named script into the editor. Called from Project Browser nav tree.
     *
     * @param scriptName the name of the script to load
     */
    public void openScript(String scriptName) {
        if (scriptName == null || scriptName.isEmpty()) {
            return;
        }
        loadScriptByName(scriptName);
    }

    // =========================================================================
    // Toolbar
    // =========================================================================

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(ModernTheme.BACKGROUND_DARKER);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ModernTheme.BORDER_DEFAULT),
                new EmptyBorder(6, 8, 6, 8)
        ));

        // Left section
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftPanel.setOpaque(false);

        themeToggleButton = createToolbarButton("Dark");
        themeToggleButton.setToolTipText("Toggle dark/light theme");
        themeToggleButton.addActionListener(e -> toggleTheme());
        leftPanel.add(themeToggleButton);

        versionCombo = new JComboBox<>();
        versionCombo.addItem("(Default)");
        versionCombo.setPreferredSize(new Dimension(120, 28));
        versionCombo.setBackground(ModernTheme.BACKGROUND_DARKER);
        versionCombo.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        versionCombo.setToolTipText("Select Python version");
        leftPanel.add(versionCombo);

        JButton loadButton = createToolbarButton("Load");
        loadButton.setToolTipText("Load a saved script (Ctrl+O)");
        loadButton.addActionListener(e -> loadScript());
        leftPanel.add(loadButton);

        JButton saveButton = createToolbarButton("Save");
        saveButton.setToolTipText("Save current script (Ctrl+S)");
        saveButton.addActionListener(e -> saveScript());
        leftPanel.add(saveButton);

        toolbar.add(leftPanel, BorderLayout.WEST);

        // Right section
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightPanel.setOpaque(false);

        runButton = createAccentButton("Run");
        runButton.setToolTipText("Execute code (Ctrl+Enter)");
        runButton.addActionListener(e -> executeCode());
        rightPanel.add(runButton);

        JButton clearButton = createToolbarButton("Clear");
        clearButton.setToolTipText("Clear output (Ctrl+L)");
        clearButton.addActionListener(e -> clearOutput());
        rightPanel.add(clearButton);

        JButton splitToggleButton = createToolbarButton("Split");
        splitToggleButton.setToolTipText("Toggle split orientation");
        splitToggleButton.addActionListener(e -> toggleSplitOrientation());
        rightPanel.add(splitToggleButton);

        toolbar.add(rightPanel, BorderLayout.EAST);

        return toolbar;
    }

    // =========================================================================
    // Editor panel
    // =========================================================================

    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ModernTheme.BACKGROUND_DARK);

        codeEditor = new RSyntaxTextArea(20, 80);
        codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
        codeEditor.setCodeFoldingEnabled(true);
        codeEditor.setAntiAliasingEnabled(true);
        codeEditor.setTabSize(4);
        codeEditor.setTabsEmulated(true);
        codeEditor.setAutoIndentEnabled(true);
        codeEditor.setBracketMatchingEnabled(true);
        codeEditor.setAnimateBracketMatching(true);
        codeEditor.setFont(ModernTheme.FONT_MONOSPACE);
        codeEditor.setText("# Python 3 Script Console\n# Press Ctrl+Enter to run\n\nprint('Hello, World!')\n");

        // Attach syntax checker
        PythonSyntaxChecker syntaxChecker = new PythonSyntaxChecker(codeEditor, restClient);
        codeEditor.addParser(syntaxChecker);

        // Setup autocomplete
        try {
            Python3CompletionProvider completionProvider = new Python3CompletionProvider(restClient);
            AutoCompletion autoCompletion = new AutoCompletion(completionProvider);
            autoCompletion.setAutoActivationEnabled(true);
            autoCompletion.setAutoActivationDelay(300);
            autoCompletion.setShowDescWindow(true);
            autoCompletion.install(codeEditor);
        } catch (Exception e) {
            LOGGER.warn("Failed to setup autocomplete: {}", e.getMessage());
        }

        RTextScrollPane editorScrollPane = new RTextScrollPane(codeEditor);
        editorScrollPane.setFoldIndicatorEnabled(true);
        editorScrollPane.setLineNumbersEnabled(true);
        editorScrollPane.setBorder(BorderFactory.createEmptyBorder());
        editorScrollPane.getGutter().setBackground(ModernTheme.BACKGROUND_DARKER);

        panel.add(editorScrollPane, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // Output panel
    // =========================================================================

    private JPanel createOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ModernTheme.BACKGROUND_DARKER);

        outputTabs = new JTabbedPane();
        outputTabs.setBackground(ModernTheme.BACKGROUND_DARKER);
        outputTabs.setForeground(ModernTheme.FOREGROUND_PRIMARY);

        outputArea = createOutputTextArea();
        errorArea = createOutputTextArea();
        errorArea.setForeground(ModernTheme.ERROR);

        outputTabs.addTab("Output", new javax.swing.JScrollPane(outputArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        outputTabs.addTab("Errors", new javax.swing.JScrollPane(errorArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));

        panel.add(outputTabs, BorderLayout.CENTER);
        return panel;
    }

    private JTextArea createOutputTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(ModernTheme.FONT_MONOSPACE);
        area.setBackground(ModernTheme.BACKGROUND_DARKER);
        area.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        area.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);
        area.setBorder(new EmptyBorder(4, 8, 4, 8));
        return area;
    }

    // =========================================================================
    // Actions
    // =========================================================================

    private void executeCode() {
        String code = codeEditor.getText();
        if (code == null || code.trim().isEmpty()) {
            statusBar.setStatus("Nothing to execute", ModernStatusBar.MessageType.WARNING);
            return;
        }

        // Get selected version (null if default)
        String selectedVersion = null;
        if (versionCombo.getSelectedIndex() > 0) {
            selectedVersion = (String) versionCombo.getSelectedItem();
        }

        statusBar.setStatus("Executing...", ModernStatusBar.MessageType.INFO);
        runButton.setEnabled(false);

        final String version = selectedVersion;
        new SwingWorker<ExecutionResult, Void>() {
            @Override
            protected ExecutionResult doInBackground() throws Exception {
                return restClient.executeCode(code, new HashMap<>(), version);
            }

            @Override
            protected void done() {
                runButton.setEnabled(true);
                try {
                    ExecutionResult result = get();
                    if (result.isSuccess()) {
                        outputArea.setText(result.getResult() != null ? result.getResult() : "");
                        outputTabs.setSelectedIndex(0);
                        long timeMs = result.getExecutionTimeMs() != null ? result.getExecutionTimeMs() : 0;
                        statusBar.setStatus("Executed in " + timeMs + "ms",
                                ModernStatusBar.MessageType.SUCCESS);
                    } else {
                        errorArea.setText(result.getError() != null ? result.getError() : "Execution failed");
                        outputTabs.setSelectedIndex(1);
                        statusBar.setStatus("Execution failed",
                                ModernStatusBar.MessageType.ERROR);
                    }
                } catch (Exception ex) {
                    errorArea.setText("Error: " + ex.getMessage());
                    outputTabs.setSelectedIndex(1);
                    statusBar.setStatus("Execution error",
                            ModernStatusBar.MessageType.ERROR);
                }
            }
        }.execute();
    }

    private void clearOutput() {
        outputArea.setText("");
        errorArea.setText("");
        statusBar.setStatus("Output cleared", ModernStatusBar.MessageType.INFO);
    }

    private void loadScript() {
        new SwingWorker<List<ScriptMetadata>, Void>() {
            @Override
            protected List<ScriptMetadata> doInBackground() throws Exception {
                return restClient.listScripts();
            }

            @Override
            protected void done() {
                try {
                    List<ScriptMetadata> scripts = get();
                    if (scripts.isEmpty()) {
                        DarkDialog.showMessage(Python3ScriptConsole.this,
                                "No saved scripts found.", "Load Script");
                        return;
                    }

                    // Build list of script names
                    String[] scriptNames = scripts.stream()
                            .map(ScriptMetadata::getName)
                            .toArray(String[]::new);

                    String selected = (String) javax.swing.JOptionPane.showInputDialog(
                            Python3ScriptConsole.this,
                            "Select a script to load:",
                            "Load Script",
                            javax.swing.JOptionPane.PLAIN_MESSAGE,
                            null,
                            scriptNames,
                            scriptNames[0]
                    );

                    if (selected != null) {
                        loadScriptByName(selected);
                    }
                } catch (Exception ex) {
                    LOGGER.error("Failed to list scripts", ex);
                    DarkDialog.showMessage(Python3ScriptConsole.this,
                            "Failed to load script list: " + ex.getMessage(), "Error");
                }
            }
        }.execute();
    }

    private void loadScriptByName(String name) {
        new SwingWorker<SavedScript, Void>() {
            @Override
            protected SavedScript doInBackground() throws Exception {
                return restClient.loadScript(name);
            }

            @Override
            protected void done() {
                try {
                    SavedScript script = get();
                    codeEditor.setText(script.getCode());
                    codeEditor.setCaretPosition(0);
                    statusBar.setStatus("Loaded: " + script.getName(),
                            ModernStatusBar.MessageType.SUCCESS);
                } catch (Exception ex) {
                    LOGGER.error("Failed to load script: {}", name, ex);
                    DarkDialog.showMessage(Python3ScriptConsole.this,
                            "Failed to load script: " + ex.getMessage(), "Error");
                }
            }
        }.execute();
    }

    private void saveScript() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Name", "");
        fields.put("Description", "");

        Map<String, String> result = DarkDialog.showMultiInput(this, "Save Script", fields);
        if (result == null) {
            return;
        }

        String name = result.get("Name");
        String description = result.get("Description");

        if (name == null || name.trim().isEmpty()) {
            DarkDialog.showMessage(this, "Script name is required.", "Save Script");
            return;
        }

        String code = codeEditor.getText();
        statusBar.setStatus("Saving...", ModernStatusBar.MessageType.INFO);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                restClient.saveScript(name.trim(), code, description);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusBar.setStatus("Saved: " + name.trim(),
                            ModernStatusBar.MessageType.SUCCESS);
                } catch (Exception ex) {
                    LOGGER.error("Failed to save script", ex);
                    statusBar.setStatus("Save failed",
                            ModernStatusBar.MessageType.ERROR);
                    DarkDialog.showMessage(Python3ScriptConsole.this,
                            "Failed to save script: " + ex.getMessage(), "Error");
                }
            }
        }.execute();
    }

    // =========================================================================
    // Theme management
    // =========================================================================

    private void toggleTheme() {
        String current = themeManager.getCurrentTheme();
        String newTheme = "dark".equals(current) ? "default" : "dark";
        try {
            themeManager.applyTheme(newTheme, this, codeEditor, outputArea, errorArea, null);
            DarkDialog.setDarkTheme(!"default".equals(newTheme));
            themeToggleButton.setText("dark".equals(newTheme) ? "Dark" : "Light");
            prefs.put(PREF_THEME, newTheme);
        } catch (IOException e) {
            LOGGER.error("Failed to apply theme: {}", newTheme, e);
        }
    }

    private void applyCurrentTheme() {
        String savedTheme = themeManager.getSavedThemePreference();
        try {
            themeManager.applyTheme(savedTheme, this, codeEditor, outputArea, errorArea, null);
            DarkDialog.setDarkTheme(!"default".equals(savedTheme));
            themeToggleButton.setText("dark".equals(savedTheme) || "vs".equals(savedTheme)
                    || "monokai".equals(savedTheme) ? "Dark" : "Light");
        } catch (IOException e) {
            LOGGER.error("Failed to apply saved theme: {}", savedTheme, e);
        }
    }

    // =========================================================================
    // Split orientation
    // =========================================================================

    private void toggleSplitOrientation() {
        int current = splitPane.getOrientation();
        int newOrientation = (current == JSplitPane.VERTICAL_SPLIT)
                ? JSplitPane.HORIZONTAL_SPLIT
                : JSplitPane.VERTICAL_SPLIT;
        splitPane.setOrientation(newOrientation);
        splitPane.setResizeWeight(0.65);
        prefs.putInt(PREF_SPLIT_ORIENTATION, newOrientation);
    }

    // =========================================================================
    // Version combo population
    // =========================================================================

    private void populateVersionCombo() {
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return restClient.getAvailableVersions();
            }

            @Override
            protected void done() {
                try {
                    List<String> versions = get();
                    for (String version : versions) {
                        versionCombo.addItem(version);
                    }
                } catch (Exception ex) {
                    LOGGER.warn("Failed to populate version combo: {}", ex.getMessage());
                }
            }
        }.execute();
    }

    // =========================================================================
    // Status bar updates
    // =========================================================================

    private void updateStatusBarAsync() {
        new SwingWorker<Void, Void>() {
            private String pythonVersion;
            private PoolStats poolStats;
            private boolean healthy;

            @Override
            protected Void doInBackground() throws Exception {
                try {
                    pythonVersion = restClient.getPythonVersion();
                } catch (Exception e) {
                    pythonVersion = "Unknown";
                }
                try {
                    poolStats = restClient.getPoolStats();
                } catch (Exception e) {
                    poolStats = null;
                }
                try {
                    healthy = restClient.isHealthy();
                } catch (Exception e) {
                    healthy = false;
                }
                return null;
            }

            @Override
            protected void done() {
                statusBar.setPythonVersion("Python " + pythonVersion);
                if (poolStats != null) {
                    statusBar.updatePoolStats(poolStats);
                }
                if (healthy) {
                    statusBar.setConnection("Connected", ModernTheme.SUCCESS);
                } else {
                    statusBar.setConnection("Disconnected", ModernTheme.ERROR);
                }
                statusBar.setStatus("Ready", ModernStatusBar.MessageType.INFO);
            }
        }.execute();
    }

    private void updateCursorPosition(CaretEvent e) {
        try {
            int caretPos = codeEditor.getCaretPosition();
            int line = codeEditor.getLineOfOffset(caretPos) + 1;
            int col = caretPos - codeEditor.getLineStartOffset(line - 1) + 1;
            statusBar.setCursorPosition(line, col);
        } catch (Exception ex) {
            // Ignore bad location
        }
    }

    // =========================================================================
    // Keyboard shortcuts
    // =========================================================================

    private void setupKeyboardShortcuts() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        // Ctrl+Enter -> Execute
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "execute");
        actionMap.put("execute", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeCode();
            }
        });

        // Ctrl+S -> Save
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "save");
        actionMap.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveScript();
            }
        });

        // Ctrl+O -> Load
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK), "load");
        actionMap.put("load", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadScript();
            }
        });

        // Ctrl+L -> Clear
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK), "clear");
        actionMap.put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearOutput();
            }
        });
    }

    // =========================================================================
    // Button factory methods
    // =========================================================================

    private JButton createToolbarButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2.setColor(ModernTheme.BUTTON_ACTIVE);
                } else if (getModel().isRollover()) {
                    g2.setColor(ModernTheme.BUTTON_HOVER);
                } else {
                    g2.setColor(ModernTheme.BUTTON_BACKGROUND);
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), ModernTheme.CORNER_RADIUS, ModernTheme.CORNER_RADIUS);

                g2.setColor(getForeground());
                g2.setFont(getFont());
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(getText())) / 2;
                int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), textX, textY);

                g2.dispose();
            }
        };

        button.setFont(ModernTheme.FONT_BOLD);
        button.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        button.setBackground(ModernTheme.BUTTON_BACKGROUND);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(70, ModernTheme.BUTTON_HEIGHT_SECONDARY));
        button.setMargin(new Insets(4, 10, 4, 10));

        return button;
    }

    private JButton createAccentButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2.setColor(ModernTheme.ACCENT_ACTIVE);
                } else if (getModel().isRollover()) {
                    g2.setColor(ModernTheme.ACCENT_HOVER);
                } else {
                    g2.setColor(ModernTheme.ACCENT_PRIMARY);
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), ModernTheme.CORNER_RADIUS, ModernTheme.CORNER_RADIUS);

                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(getText())) / 2;
                int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), textX, textY);

                g2.dispose();
            }
        };

        button.setFont(ModernTheme.FONT_BUTTON);
        button.setForeground(Color.WHITE);
        button.setBackground(ModernTheme.ACCENT_PRIMARY);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(80, ModernTheme.BUTTON_HEIGHT_PRIMARY));
        button.setMargin(new Insets(4, 12, 4, 12));

        return button;
    }
}
