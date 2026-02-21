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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CaretEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
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
 * Python 3 Script Console for the Ignition Designer.
 * Redesigned to match the Gateway Web UI appearance with a single combined
 * output panel (no separate error tab), prominent Run button, and dark theme.
 *
 * @since v3.3.0
 * @revised v3.5.0 - Redesigned to match Web GUI, merged output/error panels
 */
public class Python3ScriptConsole extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(Python3ScriptConsole.class);

    private static final String PREF_THEME = "python3console.theme";
    private static final String PREF_SPLIT_ORIENTATION = "python3console.splitOrientation";

    private final Python3RestClient restClient;
    private final ThemeManager themeManager;
    private final Preferences prefs;

    private RSyntaxTextArea codeEditor;
    private JTextPane outputPane;
    private JSplitPane splitPane;
    private ModernStatusBar statusBar;
    private JComboBox<String> versionCombo;
    private JButton runButton;

    // Toolbar references for theme updates
    private JPanel toolbarPanel;
    private final java.util.List<JButton> toolbarButtons = new java.util.ArrayList<>();
    private final java.util.List<JLabel> separatorLabels = new java.util.ArrayList<>();
    private JPanel outputHeaderPanel;
    private JLabel outputHeaderLabel;

    // Script tracking
    private String loadedScriptName;
    private JLabel scriptNameLabel;
    private JPanel scriptNameBar;

    /**
     * Creates a new Python 3 Script Console.
     *
     * @param context the Designer context
     */
    public Python3ScriptConsole(DesignerContext context) {
        setLayout(new BorderLayout());
        setBackground(ModernTheme.BACKGROUND_DARK);
        this.prefs = Preferences.userNodeForPackage(Python3ScriptConsole.class);
        this.restClient = new Python3RestClient(context);
        this.themeManager = new ThemeManager(Python3ScriptConsole.class);

        // Top section: toolbar + optional script name bar
        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setOpaque(false);
        topSection.add(createToolbar(), BorderLayout.NORTH);
        topSection.add(createScriptNameBar(), BorderLayout.SOUTH);
        add(topSection, BorderLayout.NORTH);

        // Code editor
        JPanel editorPanel = createEditorPanel();

        // Output panel (single combined panel, no tabs)
        JPanel outputPanel = createOutputPanel();

        // Split pane (respect saved orientation)
        int savedOrientation = prefs.getInt(PREF_SPLIT_ORIENTATION, JSplitPane.VERTICAL_SPLIT);
        splitPane = new JSplitPane(savedOrientation, editorPanel, outputPanel);
        splitPane.setResizeWeight(0.65);
        splitPane.setDividerSize(3);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setBackground(ModernTheme.BORDER_SUBTLE);
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
    // Toolbar - matches Web GUI layout: Run | Version | spacer | Load | Save | Save As | Clear
    // =========================================================================

    private JPanel createToolbar() {
        toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.setBackground(ModernTheme.BACKGROUND_DARKER);
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ModernTheme.BORDER_SUBTLE),
                new EmptyBorder(8, 12, 8, 12)
        ));

        // Left section: Run button + version selector
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftPanel.setOpaque(false);

        runButton = createRunButton();
        leftPanel.add(runButton);

        // Separator
        JLabel sep1 = new JLabel("|");
        sep1.setForeground(ModernTheme.BORDER_DEFAULT);
        sep1.setBorder(new EmptyBorder(0, 4, 0, 4));
        separatorLabels.add(sep1);
        leftPanel.add(sep1);

        versionCombo = new JComboBox<>();
        versionCombo.addItem("(Default)");
        versionCombo.setPreferredSize(new Dimension(120, 28));
        versionCombo.setBackground(ModernTheme.BACKGROUND_DARKER);
        versionCombo.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        versionCombo.setToolTipText("Select Python version");
        leftPanel.add(versionCombo);

        toolbarPanel.add(leftPanel, BorderLayout.WEST);

        // Right section: Load Script, Save, Save As, Clear
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightPanel.setOpaque(false);

        JButton loadButton = createToolbarButton("Load Script");
        loadButton.setToolTipText("Load a saved script (Ctrl+O)");
        loadButton.addActionListener(e -> loadScript());
        toolbarButtons.add(loadButton);
        rightPanel.add(loadButton);

        JButton saveButton = createToolbarButton("Save");
        saveButton.setToolTipText("Save current script (Ctrl+S)");
        saveButton.addActionListener(e -> saveScript());
        toolbarButtons.add(saveButton);
        rightPanel.add(saveButton);

        JButton saveAsButton = createToolbarButton("Save As");
        saveAsButton.setToolTipText("Save as new script");
        saveAsButton.addActionListener(e -> saveScriptAs());
        toolbarButtons.add(saveAsButton);
        rightPanel.add(saveAsButton);

        JButton clearButton = createToolbarButton("Clear");
        clearButton.setToolTipText("Clear output (Ctrl+L)");
        clearButton.addActionListener(e -> clearOutput());
        toolbarButtons.add(clearButton);
        rightPanel.add(clearButton);

        JButton splitButton = createToolbarButton("Split");
        splitButton.setToolTipText("Toggle split orientation (horizontal/vertical)");
        splitButton.addActionListener(e -> toggleSplitOrientation());
        toolbarButtons.add(splitButton);
        rightPanel.add(splitButton);

        // Separator before theme toggle
        JLabel sep2 = new JLabel("|");
        sep2.setForeground(ModernTheme.BORDER_DEFAULT);
        sep2.setBorder(new EmptyBorder(0, 4, 0, 4));
        separatorLabels.add(sep2);
        rightPanel.add(sep2);

        JButton themeButton = createToolbarButton("Theme");
        themeButton.setToolTipText("Toggle light/dark theme");
        themeButton.addActionListener(e -> toggleTheme());
        toolbarButtons.add(themeButton);
        rightPanel.add(themeButton);

        toolbarPanel.add(rightPanel, BorderLayout.EAST);

        return toolbarPanel;
    }

    // =========================================================================
    // Script name bar (visible only when a script is loaded)
    // =========================================================================

    private JPanel createScriptNameBar() {
        scriptNameBar = new JPanel(new BorderLayout());
        scriptNameBar.setBackground(ModernTheme.BACKGROUND_LIGHT);
        scriptNameBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ModernTheme.BORDER_SUBTLE),
                new EmptyBorder(4, 14, 4, 14)
        ));

        scriptNameLabel = new JLabel("");
        scriptNameLabel.setFont(ModernTheme.FONT_REGULAR);
        scriptNameLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        scriptNameBar.add(scriptNameLabel, BorderLayout.WEST);

        // Initially hidden
        scriptNameBar.setVisible(false);

        return scriptNameBar;
    }

    private void updateScriptNameBar(String name) {
        if (name != null && !name.isEmpty()) {
            loadedScriptName = name;
            scriptNameLabel.setText("Script: " + name);
            scriptNameBar.setVisible(true);
        } else {
            loadedScriptName = null;
            scriptNameLabel.setText("");
            scriptNameBar.setVisible(false);
        }
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

        // Thin modern scrollbar
        JScrollBar editorVsb = editorScrollPane.getVerticalScrollBar();
        editorVsb.setPreferredSize(new Dimension(6, 0));
        editorVsb.setBackground(ModernTheme.BACKGROUND_DARK);
        editorVsb.setUnitIncrement(16);

        panel.add(editorScrollPane, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // Output panel - single combined panel (no tabs)
    // =========================================================================

    private JPanel createOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ModernTheme.BACKGROUND_DARKER);

        // Output header with "Output" label
        outputHeaderPanel = new JPanel(new BorderLayout());
        outputHeaderPanel.setBackground(ModernTheme.BACKGROUND_DARKER);
        outputHeaderPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ModernTheme.BORDER_SUBTLE),
                new EmptyBorder(6, 14, 6, 14)
        ));

        outputHeaderLabel = new JLabel("Output");
        outputHeaderLabel.setFont(ModernTheme.FONT_BOLD);
        outputHeaderLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        outputHeaderPanel.add(outputHeaderLabel, BorderLayout.WEST);

        panel.add(outputHeaderPanel, BorderLayout.NORTH);

        // Single styled output pane (supports colored text for errors)
        outputPane = new JTextPane();
        outputPane.setEditable(false);
        outputPane.setFont(ModernTheme.FONT_MONOSPACE);
        outputPane.setBackground(ModernTheme.BACKGROUND_DARKER);
        outputPane.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        outputPane.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);
        outputPane.setBorder(new EmptyBorder(10, 14, 10, 14));

        // Set default text style
        StyledDocument doc = outputPane.getStyledDocument();
        SimpleAttributeSet defaultStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(defaultStyle, ModernTheme.FOREGROUND_SECONDARY);
        StyleConstants.setFontFamily(defaultStyle, "Monospaced");
        StyleConstants.setFontSize(defaultStyle, 14);
        try {
            doc.insertString(0, "Run a script to see output here. (Ctrl+Enter)", defaultStyle);
        } catch (BadLocationException ignored) {
        }

        JScrollPane outputScroll = new JScrollPane(outputPane,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        outputScroll.setBorder(BorderFactory.createEmptyBorder());
        outputScroll.getViewport().setBackground(ModernTheme.BACKGROUND_DARKER);

        // Thin modern scrollbar styling
        JScrollBar vsb = outputScroll.getVerticalScrollBar();
        vsb.setPreferredSize(new Dimension(6, 0));
        vsb.setBackground(ModernTheme.BACKGROUND_DARKER);
        vsb.setUnitIncrement(16);

        panel.add(outputScroll, BorderLayout.CENTER);
        return panel;
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

        // Show "Executing..." in output pane
        clearOutputPane();
        appendToOutput("Executing...\n", ModernTheme.FOREGROUND_SECONDARY);

        final String version = selectedVersion;
        new SwingWorker<ExecutionResult, Void>() {
            @Override
            protected ExecutionResult doInBackground() throws Exception {
                return restClient.executeCode(code, new HashMap<>(), version);
            }

            @Override
            protected void done() {
                runButton.setEnabled(true);
                clearOutputPane();
                try {
                    ExecutionResult result = get();
                    long timeMs = result.getExecutionTimeMs() != null ? result.getExecutionTimeMs() : 0;

                    if (result.isSuccess()) {
                        String output = result.getResult() != null ? result.getResult() : "";
                        if (!output.isEmpty()) {
                            appendToOutput(output, ModernTheme.FOREGROUND_PRIMARY);
                        } else {
                            appendToOutput("(no output)", ModernTheme.FOREGROUND_SECONDARY);
                        }
                        appendToOutput("\n\nCompleted in " + timeMs + "ms",
                                ModernTheme.SUCCESS);
                        statusBar.setStatus("Executed in " + timeMs + "ms",
                                ModernStatusBar.MessageType.SUCCESS);
                    } else {
                        String error = result.getError() != null ? result.getError() : "Execution failed";
                        // Show any output first
                        String output = result.getResult();
                        if (output != null && !output.isEmpty()) {
                            appendToOutput(output + "\n\n", ModernTheme.FOREGROUND_PRIMARY);
                        }
                        appendToOutput(error, ModernTheme.ERROR);
                        statusBar.setStatus("Execution failed",
                                ModernStatusBar.MessageType.ERROR);
                    }
                } catch (Exception ex) {
                    appendToOutput("Error: " + ex.getMessage(), ModernTheme.ERROR);
                    statusBar.setStatus("Execution error",
                            ModernStatusBar.MessageType.ERROR);
                }
            }
        }.execute();
    }

    private void clearOutput() {
        clearOutputPane();
        appendToOutput("Output cleared.", ModernTheme.FOREGROUND_SECONDARY);
        statusBar.setStatus("Output cleared", ModernStatusBar.MessageType.INFO);
    }

    private void clearOutputPane() {
        outputPane.setText("");
    }

    private void appendToOutput(String text, Color color) {
        StyledDocument doc = outputPane.getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, color);
        StyleConstants.setFontFamily(attrs, "Monospaced");
        StyleConstants.setFontSize(attrs, 14);
        try {
            doc.insertString(doc.getLength(), text, attrs);
        } catch (BadLocationException ignored) {
        }
        // Auto-scroll to bottom
        outputPane.setCaretPosition(doc.getLength());
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
                    updateScriptNameBar(script.getName());
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

    /**
     * Save: if a script is loaded, auto-save to that name. Otherwise fall through to Save As.
     */
    private void saveScript() {
        if (loadedScriptName != null && !loadedScriptName.isEmpty()) {
            // Auto-save to existing name
            String code = codeEditor.getText();
            statusBar.setStatus("Saving...", ModernStatusBar.MessageType.INFO);

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    restClient.saveScript(loadedScriptName, code, "");
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        statusBar.setStatus("Saved: " + loadedScriptName,
                                ModernStatusBar.MessageType.SUCCESS);
                    } catch (Exception ex) {
                        LOGGER.error("Failed to save script", ex);
                        statusBar.setStatus("Save failed",
                                ModernStatusBar.MessageType.ERROR);
                    }
                }
            }.execute();
        } else {
            // No script loaded, fall through to Save As
            saveScriptAs();
        }
    }

    /**
     * Save As: always prompts for a new script name.
     */
    private void saveScriptAs() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Name", "");
        fields.put("Description", "");

        Map<String, String> result = DarkDialog.showMultiInput(this, "Save Script As", fields);
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

        final String trimmedName = name.trim();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                restClient.saveScript(trimmedName, code, description);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    updateScriptNameBar(trimmedName);
                    statusBar.setStatus("Saved: " + trimmedName,
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

    private void applyCurrentTheme() {
        String savedTheme = themeManager.getSavedThemePreference();
        applyThemeByName(savedTheme);
    }

    private void applyThemeByName(String themeName) {
        try {
            boolean isDark = !"default".equals(themeName);
            themeManager.applyTheme(themeName, this, codeEditor, null, null, null);
            DarkDialog.setDarkTheme(isDark);

            // Theme colors for dark vs light
            Color bg = isDark ? ModernTheme.BACKGROUND_DARK : Color.WHITE;
            Color bgDarker = isDark ? ModernTheme.BACKGROUND_DARKER : new Color(245, 245, 245);
            Color bgLight = isDark ? ModernTheme.BACKGROUND_LIGHT : new Color(248, 248, 248);
            Color fgPrimary = isDark ? ModernTheme.FOREGROUND_PRIMARY : new Color(30, 30, 30);
            Color fgSecondary = isDark ? ModernTheme.FOREGROUND_SECONDARY : new Color(100, 100, 100);
            Color borderSubtle = isDark ? ModernTheme.BORDER_SUBTLE : new Color(220, 220, 220);
            Color borderDefault = isDark ? ModernTheme.BORDER_DEFAULT : new Color(200, 200, 200);

            // Root panel
            setBackground(bg);

            // Toolbar
            if (toolbarPanel != null) {
                toolbarPanel.setBackground(bgDarker);
                toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle),
                        new EmptyBorder(8, 12, 8, 12)
                ));
            }

            // Toolbar buttons - update foreground and background for custom paint
            Color buttonBg = isDark ? ModernTheme.BUTTON_BACKGROUND : new Color(230, 230, 230);
            for (JButton btn : toolbarButtons) {
                btn.setForeground(fgPrimary);
                btn.setBackground(buttonBg);
                btn.repaint();
            }

            // Separator labels
            for (JLabel sep : separatorLabels) {
                sep.setForeground(borderDefault);
            }

            // Version combo
            if (versionCombo != null) {
                versionCombo.setBackground(bgDarker);
                versionCombo.setForeground(fgPrimary);
            }

            // Output pane
            if (outputPane != null) {
                outputPane.setBackground(bgDarker);
                outputPane.setForeground(fgPrimary);
                outputPane.setCaretColor(fgPrimary);
            }

            // Output header
            if (outputHeaderPanel != null) {
                outputHeaderPanel.setBackground(bgDarker);
                outputHeaderPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle),
                        new EmptyBorder(6, 14, 6, 14)
                ));
            }
            if (outputHeaderLabel != null) {
                outputHeaderLabel.setForeground(fgSecondary);
            }

            // Script name bar
            if (scriptNameBar != null) {
                scriptNameBar.setBackground(bgLight);
                scriptNameBar.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle),
                        new EmptyBorder(4, 14, 4, 14)
                ));
            }
            if (scriptNameLabel != null) {
                scriptNameLabel.setForeground(fgSecondary);
            }

            // Split pane
            if (splitPane != null) {
                splitPane.setBackground(borderSubtle);
            }

            // Status bar
            if (statusBar != null) {
                statusBar.updateTheme(isDark);
            }

            // Apply theme to the parent JFrame if we're embedded in one
            java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
            if (window instanceof JFrame) {
                JFrame frame = (JFrame) window;
                frame.setBackground(bg);
                frame.getRootPane().setBackground(bg);
                frame.getRootPane().setOpaque(true);
                frame.getLayeredPane().setBackground(bg);
                frame.getContentPane().setBackground(bg);
            }

            // Force full repaint
            revalidate();
            repaint();

            statusBar.setStatus("Theme: " + themeName, ModernStatusBar.MessageType.INFO);
        } catch (IOException e) {
            LOGGER.error("Failed to apply theme: {}", themeName, e);
        }
    }

    private void toggleTheme() {
        String current = themeManager.getCurrentTheme();
        String newTheme = "default".equals(current) ? "dark" : "default";
        applyThemeByName(newTheme);
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
        splitPane.setDividerLocation(0.65);
        splitPane.revalidate();
        splitPane.repaint();
        prefs.putInt(PREF_SPLIT_ORIENTATION, newOrientation);

        String label = (newOrientation == JSplitPane.VERTICAL_SPLIT)
                ? "top/bottom" : "left/right";
        statusBar.setStatus("Split: " + label, ModernStatusBar.MessageType.INFO);
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

    /**
     * Creates the prominent green Run button (matches Web GUI's primary action button).
     */
    private JButton createRunButton() {
        JButton button = new JButton("Run") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bgColor;
                if (!isEnabled()) {
                    bgColor = ModernTheme.darken(ModernTheme.SUCCESS, 0.4);
                } else if (getModel().isPressed()) {
                    bgColor = ModernTheme.darken(ModernTheme.SUCCESS, 0.2);
                } else if (getModel().isRollover()) {
                    bgColor = ModernTheme.lighten(ModernTheme.SUCCESS, 0.15);
                } else {
                    bgColor = ModernTheme.SUCCESS;
                }

                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                        ModernTheme.CORNER_RADIUS, ModernTheme.CORNER_RADIUS);

                // Draw play triangle icon
                g2.setColor(Color.WHITE);
                int iconSize = 10;
                int iconX = 12;
                int iconY = (getHeight() - iconSize) / 2;
                int[] xPoints = {iconX, iconX, iconX + iconSize};
                int[] yPoints = {iconY, iconY + iconSize, iconY + iconSize / 2};
                g2.fillPolygon(xPoints, yPoints, 3);

                // Draw text
                g2.setFont(getFont());
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int textX = iconX + iconSize + 6;
                int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), textX, textY);

                g2.dispose();
            }
        };

        button.setFont(ModernTheme.FONT_BUTTON);
        button.setForeground(Color.WHITE);
        button.setBackground(ModernTheme.SUCCESS);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(90, ModernTheme.BUTTON_HEIGHT_PRIMARY));
        button.setToolTipText("Execute code (Ctrl+Enter)");
        button.addActionListener(e -> executeCode());

        return button;
    }

    private JButton createToolbarButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Use getBackground() so theme changes are reflected
                Color base = getBackground();
                if (getModel().isPressed()) {
                    g2.setColor(ModernTheme.darken(base, 0.15));
                } else if (getModel().isRollover()) {
                    g2.setColor(ModernTheme.lighten(base, 0.1));
                } else {
                    g2.setColor(base);
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

        // Auto-size based on text width
        int textWidth = button.getFontMetrics(ModernTheme.FONT_BOLD).stringWidth(text);
        button.setPreferredSize(new Dimension(textWidth + 24, ModernTheme.BUTTON_HEIGHT_SECONDARY));
        button.setMargin(new Insets(4, 10, 4, 10));

        return button;
    }
}
