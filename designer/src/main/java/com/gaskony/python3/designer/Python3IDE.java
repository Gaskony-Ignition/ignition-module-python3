package com.gaskony.python3.designer;

import com.inductiveautomation.ignition.designer.model.DesignerContext;
import com.gaskony.python3.designer.managers.AutoSaveManager;
import com.gaskony.python3.designer.managers.CommandPaletteManager;
import com.gaskony.python3.designer.managers.ExecutionManager;
import com.gaskony.python3.designer.managers.KeyboardShortcutsManager;
import com.gaskony.python3.designer.managers.Python3IDEConnectionController;
import com.gaskony.python3.designer.managers.Python3IDELayout;
import com.gaskony.python3.designer.managers.Python3IDEScriptOps;
import com.gaskony.python3.designer.managers.Python3IDETerminalController;
import com.gaskony.python3.designer.managers.Python3IDETheme;
import com.gaskony.python3.designer.managers.ScriptImportExportManager;
import com.gaskony.python3.designer.managers.ScriptTransferManager;
import com.gaskony.python3.designer.managers.SearchManager;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.prefs.Preferences;

/**
 * Python 3 IDE panel for the Ignition Designer.
 *
 * <p>Slim orchestrator (v3.13 refactor &mdash; was 3 854 lines before). Wires
 * focused collaborators in {@code managers/} together; the Swing component
 * tree is built by {@link Python3IDELayout}. Each collaborator owns one bucket
 * of responsibility:</p>
 *
 * <ul>
 *   <li>{@link Python3IDELayout} &mdash; component construction, top toolbar,
 *       sidebar, editor / output split assembly</li>
 *   <li>{@link Python3IDETheme} &mdash; theme switching, popup-menu styling,
 *       dialog buttons, titled-border colour cascade</li>
 *   <li>{@link Python3IDEConnectionController} &mdash; gateway URL detection,
 *       connect, refresh diagnostics / Python version / available versions /
 *       autocomplete status, pool resize</li>
 *   <li>{@link Python3IDETerminalController} &mdash; terminal/editor card
 *       switching and shell-command execution</li>
 *   <li>{@link Python3IDEScriptOps} &mdash; script CRUD: refresh / load /
 *       save / save-as / rename / edit-metadata / move / delete, folder
 *       create-rename, context menu, name validation</li>
 *   <li>{@link SearchManager}, {@link CommandPaletteManager},
 *       {@link KeyboardShortcutsManager}, {@link ExecutionManager},
 *       {@link AutoSaveManager}, {@link ScriptImportExportManager},
 *       {@link ScriptTransferManager} &mdash; pre-existing collaborators</li>
 * </ul>
 *
 * <p>Public API preserved verbatim from v3.12.x: {@link #applyThemeByName(String)},
 * {@link #getDetectedGatewayUrl()}, {@link #getEffectiveGatewayUrl()},
 * {@link #getRestClient()}, {@link #reloadSettingsFromPreferences()},
 * {@link #getCodeEditor()}, {@link #getOutputArea()},
 * {@link #getErrorArea()}.</p>
 */
public class Python3IDE extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(Python3IDE.class);

    private final DesignerContext context;
    private String detectedGatewayUrl;
    private String effectiveGatewayUrl;
    private Python3RestClient restClient;
    private PythonSyntaxChecker syntaxChecker;
    private AutoCompletion autoCompletion;
    private Python3CompletionProvider completionProvider;

    /** Holder for all Swing widgets the IDE owns. Built by {@link Python3IDELayout}. */
    private final Python3IDELayout.Components ui;

    private JSplitPane mainSplit;
    private boolean sidebarCollapsed = false;
    private int sidebarDividerLocation = 250;

    private int fontSize;
    private boolean useDarkTheme = true;

    private SearchManager searchManager;
    private CommandPaletteManager commandPaletteManager;
    private UnsavedChangesTracker changesTracker;
    private ScriptMetadata currentScript;
    private AutoSaveManager autoSaveManager;
    private ScriptImportExportManager importExportManager;
    private ExecutionManager executionManager;
    private KeyboardShortcutsManager keyboardShortcutsManager;
    private ScriptTransferManager scriptTransferManager;

    private Python3IDETheme themeController;
    private Python3IDEConnectionController connectionController;
    private Python3IDETerminalController terminalController;
    private Python3IDEScriptOps scriptOps;

    /**
     * Creates a new Python 3 IDE panel.
     *
     * @param context the Designer context
     */
    public Python3IDE(DesignerContext context) {
        this.context = context;
        this.restClient = null;

        this.detectedGatewayUrl = Python3IDEConnectionController.detectGatewayUrl();
        logger.info("Auto-detected Gateway URL: {}", this.detectedGatewayUrl);

        Preferences prefs = Preferences.userNodeForPackage(Python3IDE.class);
        String currentTheme = prefs.get(PreferenceKeys.IDE_THEME, "dark");
        this.fontSize = prefs.getInt(PreferenceKeys.IDE_FONT_SIZE, 12);

        String gatewayOverride = prefs.get(PreferenceKeys.IDE_GATEWAY_OVERRIDE, "");
        if (gatewayOverride != null && !gatewayOverride.trim().isEmpty()) {
            this.effectiveGatewayUrl = gatewayOverride.trim();
            logger.info("Using Gateway URL override from settings: {}", this.effectiveGatewayUrl);
        } else {
            this.effectiveGatewayUrl = this.detectedGatewayUrl;
            logger.info("Using auto-detected Gateway URL: {}", this.effectiveGatewayUrl);
        }

        this.ui = Python3IDELayout.createComponents(this.effectiveGatewayUrl, this.fontSize);

        changesTracker = new UnsavedChangesTracker(ui.codeEditor);
        changesTracker.addChangeListener(this::onDirtyStateChanged);

        ui.statusBar.setPoolClickListener(this::handlePoolClicked);

        layoutPanels();
        initControllers();
        attachListeners();
        themeController.applyTheme(currentTheme);

        boolean autoConnect = prefs.getBoolean(PreferenceKeys.IDE_AUTO_CONNECT, true);
        if (autoConnect) {
            logger.info("Auto-connect enabled, connecting to Gateway...");
            connectionController.connectToGateway();
        } else {
            logger.info("Auto-connect disabled in settings");
        }
    }

    private void layoutPanels() {
        setLayout(new BorderLayout(0, 0));
        setBorder(null);
        setBackground(ModernTheme.EDITOR_BACKGROUND);

        Python3IDELayout.DialogActions dialogActions = new Python3IDELayout.DialogActions() {
            @Override public void openVersionManager() { Python3IDE.this.openVersionManagerDialog(); }
            @Override public void openPackages() { Python3IDE.this.openPackagesDialog(); }
            @Override public void openSettings() { Python3IDE.this.openSettingsDialog(); }
            @Override public void openInfo() { Python3IDE.this.openInfoDialog(); }
        };

        JPanel topBar = Python3IDELayout.buildTopBar(ui, dialogActions);
        add(topBar, BorderLayout.NORTH);

        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setUI(new ThemedSplitPaneUI(
            useDarkTheme ? ModernTheme.BORDER_DEFAULT : ModernTheme.LIGHT_BORDER));
        mainSplit.setDividerLocation(250);
        mainSplit.setBackground(ModernTheme.EDITOR_BACKGROUND);
        mainSplit.setBorder(null);
        mainSplit.setDividerSize(4);

        mainSplit.setLeftComponent(Python3IDELayout.buildSidebar(ui, useDarkTheme));
        mainSplit.setRightComponent(Python3IDELayout.buildEditorPanel(
            ui, useDarkTheme,
            this::executeTerminalCommand,
            isTerminal -> terminalController.onModeTabChanged(isTerminal)));

        add(mainSplit, BorderLayout.CENTER);
        add(ui.statusBar, BorderLayout.SOUTH);
    }

    private void initControllers() {
        themeController = new Python3IDETheme(new ThemeIdeContext(), "dark");
        connectionController = new Python3IDEConnectionController(new ConnCtx());

        executionManager = new ExecutionManager(
            new ExecManagerCtx(), ui.executeButton, ui.progressBar);

        terminalController = new Python3IDETerminalController(new TerminalCtx(), executionManager);

        scriptOps = new Python3IDEScriptOps(new ScriptOpsCtx());

        searchManager = new SearchManager(this, ui.codeEditor, new SearchListenerImpl());

        commandPaletteManager = new CommandPaletteManager(
            this,
            new CommandPaletteManager.CommandActions() {
                @Override public void executeCode() { executionManager.executeCode(); }
                @Override public void clearOutput() { Python3IDE.this.clearOutput(); }
                @Override public void saveCurrentScript() { scriptOps.saveCurrentScript(); }
                @Override public void saveScriptAs() { scriptOps.saveScriptAs(); }
                @Override public void createNewScript() { scriptOps.createNewScript(); }
                @Override public void refreshScriptTree() { scriptOps.refreshScriptTree(); }
                @Override public void showFindDialog() { searchManager.showFindDialog(); }
                @Override public void showReplaceDialog() { searchManager.showReplaceDialog(); }
                @Override public void showAdvancedFindReplaceDialog() { searchManager.showAdvancedFindReplaceDialog(); }
                @Override public void toggleSidebar() { Python3IDE.this.toggleSidebar(); }
                @Override public void changeFontSize(int delta) { Python3IDE.this.changeFontSize(delta); }
                @Override public void setFontSize(int size) { Python3IDE.this.setFontSize(size); }
                @Override public void applyTheme(String themeKey) { themeController.applyTheme(themeKey); }
                @Override public void connectToGateway() { connectionController.connectToGateway(); }
                @Override public void openSettingsDialog() { Python3IDE.this.openSettingsDialog(); }
                @Override public void showInformationDialog() { Python3IDE.this.showInformationDialog(); }
                @Override public void openPackagesDialog() { Python3IDE.this.openPackagesDialog(); }
            },
            ui.importButton,
            ui.exportButton
        );

        importExportManager = new ScriptImportExportManager(
            this,
            new ScriptImportExportManager.ImportExportContext() {
                @Override public Python3RestClient getRestClient() { return restClient; }
                @Override public ScriptMetadata getCurrentScript() { return currentScript; }
                @Override public String getCurrentCode() { return ui.codeEditor.getText(); }
                @Override
                public void saveScript(String name, String code, String author, String version,
                                       String description, String folderPath) {
                    scriptOps.saveScript(name, code, description, author, folderPath, version);
                }
                @Override public void loadScript(String scriptName) { scriptOps.loadScript(scriptName); }
                @Override public void setStatus(String message, Color color) {
                    Python3IDE.this.setStatus(message, color);
                }
            },
            ui.statusBar
        );

        autoSaveManager = new AutoSaveManager(
            ui.codeEditor,
            changesTracker,
            ui.statusBar,
            new AutoSaveManager.AutoSaveContext() {
                @Override public ScriptMetadata getCurrentScript() { return currentScript; }
                @Override public boolean isConnectedToGateway() { return restClient != null; }
            }
        );
        autoSaveManager.initialize();
    }

    private void attachListeners() {
        ui.connectButton.addActionListener(e -> connectionController.connectToGateway());
        ui.executeButton.addActionListener(e -> executionManager.executeCode());
        ui.saveButton.addActionListener(e -> scriptOps.saveCurrentScript());
        ui.saveAsButton.addActionListener(e -> scriptOps.saveScriptAs());
        ui.importButton.addActionListener(e -> importExportManager.importScript());
        ui.exportButton.addActionListener(e -> importExportManager.exportCurrentScript());
        ui.fontIncreaseButton.addActionListener(e -> changeFontSize(1));
        ui.fontDecreaseButton.addActionListener(e -> changeFontSize(-1));
        ui.newFolderBtn.addActionListener(e -> scriptOps.createNewFolder());
        ui.newScriptBtn.addActionListener(e -> scriptOps.createNewScript());
        ui.refreshBtn.addActionListener(e -> scriptOps.refreshScriptTree());

        keyboardShortcutsManager = new KeyboardShortcutsManager(
            ui.codeEditor,
            new KeyboardShortcutsManager.ShortcutActions() {
                @Override public void executeCode() { executionManager.executeCode(); }
                @Override public void saveCurrentScript() { scriptOps.saveCurrentScript(); }
                @Override public void saveScriptAs() { scriptOps.saveScriptAs(); }
                @Override public void createNewScript() { scriptOps.createNewScript(); }
                @Override public void changeFontSize(int delta) { Python3IDE.this.changeFontSize(delta); }
                @Override public void setFontSize(int size) { Python3IDE.this.setFontSize(size); }
                @Override public void showFindDialog() { searchManager.showFindDialog(); }
                @Override public void showReplaceDialog() { searchManager.showReplaceDialog(); }
                @Override public void showAdvancedFindReplaceDialog() { searchManager.showAdvancedFindReplaceDialog(); }
                @Override public void showCommandPalette() { commandPaletteManager.showCommandPalette(); }
                @Override public void toggleSidebar() { Python3IDE.this.toggleSidebar(); }
            }
        );
        keyboardShortcutsManager.setupKeyboardShortcuts();

        scriptTransferManager = new ScriptTransferManager(
            new ScriptTransferManager.TransferContext() {
                @Override public Python3RestClient getRestClient() { return restClient; }
                @Override public ScriptTreeNode getRootNode() { return ui.rootNode; }
                @Override public void refreshScriptTree() { scriptOps.refreshScriptTree(); }
                @Override public void setStatus(String message, Color color) {
                    Python3IDE.this.setStatus(message, color);
                }
                @Override public Component getParentComponent() { return Python3IDE.this; }
            },
            ui.rootNode
        );
        ui.scriptTree.setTransferHandler(scriptTransferManager.createTransferHandler());

        ui.scriptTree.addTreeSelectionListener(e -> scriptOps.onTreeSelectionChanged());

        ui.scriptTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    scriptOps.loadSelectedScript();
                }
            }
        });
        ui.scriptTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    scriptOps.showContextMenu(e,
                        node -> importExportManager.exportScript(node.getScriptMetadata()));
                }
            }
        });

        ui.gatewayUrlField.addActionListener(e -> connectionController.connectToGateway());

        ui.codeEditor.addCaretListener(e -> {
            int line = ui.codeEditor.getCaretLineNumber() + 1;
            int col = ui.codeEditor.getCaretOffsetFromLineStart() + 1;
            ui.statusBar.setCursorPosition(line, col);
        });
    }

    // ===== Local methods =====================================================

    private void clearOutput() {
        ui.outputArea.setText("");
        ui.errorArea.setText("");
    }

    private void executeTerminalCommand(String command) {
        terminalController.executeTerminalCommand(command);
    }

    private void handlePoolClicked() {
        connectionController.handlePoolClicked(null);
    }

    private void setStatus(String message, Color color) {
        ModernStatusBar.MessageType type = ModernStatusBar.MessageType.INFO;
        if (color.equals(Color.RED) || color.equals(ModernTheme.ERROR)) {
            type = ModernStatusBar.MessageType.ERROR;
        } else if (color.equals(Color.ORANGE) || color.equals(ModernTheme.WARNING)) {
            type = ModernStatusBar.MessageType.WARNING;
        } else if (color.equals(ModernTheme.SUCCESS)) {
            type = ModernStatusBar.MessageType.SUCCESS;
        }
        ui.statusBar.setStatus(message, type);
    }

    private void onDirtyStateChanged(boolean isDirty) {
        updateCurrentScriptLabel();
        setStatus(isDirty ? "Unsaved changes" : "All changes saved",
            isDirty ? Color.ORANGE : ModernTheme.SUCCESS);
    }

    private void updateCurrentScriptLabel() {
        if (currentScript == null || currentScript.getName() == null || currentScript.getName().isEmpty()) {
            ui.currentScriptLabel.setText("  No script selected");
            ui.currentScriptLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        } else {
            StringBuilder labelText = new StringBuilder();
            labelText.append("• ");
            if (currentScript.getFolderPath() != null && !currentScript.getFolderPath().isEmpty()) {
                labelText.append(currentScript.getFolderPath()).append(" / ");
            }
            labelText.append(currentScript.getName());
            if (changesTracker.isDirty()) {
                labelText.append(" *");
            }
            ui.currentScriptLabel.setText(labelText.toString());
            ui.currentScriptLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        }
    }

    private void toggleSidebar() {
        if (sidebarCollapsed) {
            mainSplit.setDividerLocation(sidebarDividerLocation);
            sidebarCollapsed = false;
            ui.statusBar.setStatus("Sidebar expanded", ModernStatusBar.MessageType.INFO);
        } else {
            sidebarDividerLocation = mainSplit.getDividerLocation();
            mainSplit.setDividerLocation(0);
            sidebarCollapsed = true;
            ui.statusBar.setStatus("Sidebar collapsed (Ctrl+B to toggle)", ModernStatusBar.MessageType.INFO);
        }
    }

    private void changeFontSize(int delta) {
        setFontSize(fontSize + delta);
    }

    private void setFontSize(int newSize) {
        if (newSize < 8 || newSize > 24) {
            return;
        }
        fontSize = newSize;
        ui.codeEditor.setFont(ModernTheme.FONT_CODE.deriveFont((float) fontSize));
        Preferences prefs = Preferences.userNodeForPackage(Python3IDE.class);
        prefs.putInt(PreferenceKeys.IDE_FONT_SIZE, fontSize);
        logger.info("Font size: {}", fontSize);
    }

    private void showInformationDialog() {
        InformationDialog.show(this);
    }

    private void openInfoDialog() {
        java.awt.Frame parentFrame = (java.awt.Frame) SwingUtilities.getWindowAncestor(this);
        new InfoDialog(parentFrame, this).setVisible(true);
    }

    private void openVersionManagerDialog() {
        java.awt.Frame parentFrame = (java.awt.Frame) SwingUtilities.getWindowAncestor(this);
        VersionManagerDialog dialog = new VersionManagerDialog(parentFrame, restClient);
        dialog.setVisible(true);
        connectionController.refreshAvailableVersions();
    }

    private void openPackagesDialog() {
        java.awt.Frame parentFrame = (java.awt.Frame) SwingUtilities.getWindowAncestor(this);
        new PackagesDialog(parentFrame, this).setVisible(true);
    }

    private void openSettingsDialog() {
        java.awt.Frame parentFrame = (java.awt.Frame) SwingUtilities.getWindowAncestor(this);
        SettingsDialog dialog = new SettingsDialog(parentFrame, this);
        dialog.setVisible(true);
        if (dialog.isConnectRequested()) {
            connectionController.connectToGateway();
        }
    }

    // ===== Public API (preserved verbatim from v3.12.x) ======================

    public void applyThemeByName(String displayName) {
        themeController.applyThemeByName(displayName);
    }

    public String getDetectedGatewayUrl() {
        return detectedGatewayUrl;
    }

    public String getEffectiveGatewayUrl() {
        return effectiveGatewayUrl;
    }

    public Python3RestClient getRestClient() {
        return restClient;
    }

    public RSyntaxTextArea getCodeEditor() {
        return ui.codeEditor;
    }

    public JTextArea getOutputArea() {
        return ui.outputArea;
    }

    public JTextArea getErrorArea() {
        return ui.errorArea;
    }

    public void reloadSettingsFromPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(Python3IDE.class);

        String gatewayOverride = prefs.get(PreferenceKeys.IDE_GATEWAY_OVERRIDE, "");
        if (gatewayOverride != null && !gatewayOverride.trim().isEmpty()) {
            effectiveGatewayUrl = gatewayOverride.trim();
            logger.info("Reloaded Gateway URL override: {}", effectiveGatewayUrl);
        } else {
            effectiveGatewayUrl = detectedGatewayUrl;
            logger.info("Reloaded auto-detected Gateway URL: {}", effectiveGatewayUrl);
        }

        ui.gatewayUrlField.setText(effectiveGatewayUrl);

        String newTheme = prefs.get(PreferenceKeys.IDE_THEME, "dark");
        if (!newTheme.equals(themeController.getCurrentTheme())) {
            themeController.applyTheme(newTheme);
        }

        int newFontSize = prefs.getInt(PreferenceKeys.IDE_FONT_SIZE, 12);
        if (newFontSize != fontSize) {
            fontSize = newFontSize;
            ui.codeEditor.setFont(ModernTheme.FONT_CODE.deriveFont((float) fontSize));
        }

        logger.info("Settings reloaded from preferences");
    }

    /**
     * Returns the {@link DesignerContext} this IDE was constructed with.
     */
    public DesignerContext getDesignerContext() {
        return context;
    }

    // ===== Search listener (uses RSTA SearchEngine in-place) =================

    private final class SearchListenerImpl implements SearchListener {
        @Override
        public void searchEvent(org.fife.rsta.ui.search.SearchEvent e) {
            org.fife.rsta.ui.search.SearchEvent.Type type = e.getType();
            org.fife.ui.rtextarea.SearchContext sc = e.getSearchContext();
            switch (type) {
                case FIND: {
                    org.fife.ui.rtextarea.SearchResult result = SearchEngine.find(ui.codeEditor, sc);
                    if (!result.wasFound()) {
                        setStatus("Text not found: " + sc.getSearchFor(), Color.ORANGE);
                    }
                    break;
                }
                case REPLACE: {
                    org.fife.ui.rtextarea.SearchResult result = SearchEngine.replace(ui.codeEditor, sc);
                    if (!result.wasFound()) {
                        setStatus("Text not found: " + sc.getSearchFor(), Color.ORANGE);
                    }
                    break;
                }
                case REPLACE_ALL: {
                    org.fife.ui.rtextarea.SearchResult result = SearchEngine.replaceAll(ui.codeEditor, sc);
                    setStatus("Replaced " + result.getCount() + " occurrences", ModernTheme.SUCCESS);
                    break;
                }
                case MARK_ALL: {
                    org.fife.ui.rtextarea.SearchResult result = SearchEngine.markAll(ui.codeEditor, sc);
                    if (!result.wasFound()) {
                        setStatus("Text not found: " + sc.getSearchFor(), Color.ORANGE);
                    }
                    break;
                }
                default:
                    break;
            }
        }

        @Override
        public String getSelectedText() {
            return ui.codeEditor.getSelectedText();
        }
    }

    // ===== Inner contexts (bridges to manager interfaces) ====================

    private final class ThemeIdeContext implements Python3IDETheme.IdeContext {
        @Override public JComponent getRoot() { return Python3IDE.this; }
        @Override public RSyntaxTextArea getCodeEditor() { return ui.codeEditor; }
        @Override public JTextArea getOutputArea() { return ui.outputArea; }
        @Override public JTextArea getErrorArea() { return ui.errorArea; }
        @Override public JTree getScriptTree() { return ui.scriptTree; }
        @Override public JTextField getGatewayUrlField() { return ui.gatewayUrlField; }
        @Override public JLabel getCurrentScriptLabel() { return ui.currentScriptLabel; }
        @Override public JButton getConnectButton() { return ui.connectButton; }
        @Override public JButton getExecuteButton() { return ui.executeButton; }
        @Override public JButton getSaveButton() { return ui.saveButton; }
        @Override public JButton getSaveAsButton() { return ui.saveAsButton; }
        @Override public JButton getImportButton() { return ui.importButton; }
        @Override public JButton getExportButton() { return ui.exportButton; }
        @Override public JButton getNewFolderBtn() { return ui.newFolderBtn; }
        @Override public JButton getNewScriptBtn() { return ui.newScriptBtn; }
        @Override public JButton getRefreshBtn() { return ui.refreshBtn; }
        @Override public ScriptMetadataPanel getMetadataPanel() { return ui.metadataPanel; }
        @Override public DiagnosticsPanel getDiagnosticsPanel() { return ui.diagnosticsPanel; }
        @Override public TerminalPanel getTerminalPanel() { return ui.terminalPanel; }
        @Override public void setStatus(String message, Color color) { Python3IDE.this.setStatus(message, color); }
        @Override public void setUseDarkTheme(boolean isDark) { useDarkTheme = isDark; }
    }

    private final class ConnCtx implements Python3IDEConnectionController.ConnectionContext {
        @Override public JTextField getGatewayUrlField() { return ui.gatewayUrlField; }
        @Override public JLabel getConnectionStatusIndicator() { return ui.connectionStatusIndicator; }
        @Override public ModernStatusBar getStatusBar() { return ui.statusBar; }
        @Override public DiagnosticsPanel getDiagnosticsPanel() { return ui.diagnosticsPanel; }
        @Override public RSyntaxTextArea getCodeEditor() { return ui.codeEditor; }
        @Override public JComboBox<String> getVersionSelector() { return ui.versionSelector; }
        @Override public JComponent getParent() { return Python3IDE.this; }
        @Override public Python3RestClient getRestClient() { return restClient; }
        @Override public void setRestClient(Python3RestClient client) { restClient = client; }
        @Override public PythonSyntaxChecker getSyntaxChecker() { return syntaxChecker; }
        @Override public void setSyntaxChecker(PythonSyntaxChecker checker) { syntaxChecker = checker; }
        @Override public AutoCompletion getAutoCompletion() { return autoCompletion; }
        @Override public void setAutoCompletion(AutoCompletion ac) { autoCompletion = ac; }
        @Override public Python3CompletionProvider getCompletionProvider() { return completionProvider; }
        @Override public void setCompletionProvider(Python3CompletionProvider p) { completionProvider = p; }
        @Override public String getEffectiveGatewayUrl() { return effectiveGatewayUrl; }
        @Override public void setEffectiveGatewayUrl(String url) { effectiveGatewayUrl = url; }
        @Override public void refreshScriptTree() { scriptOps.refreshScriptTree(); }
        @Override public void setStatus(String message, Color color) { Python3IDE.this.setStatus(message, color); }
    }

    private final class ExecManagerCtx implements ExecutionManager.ExecutionContext {
        @Override public Python3RestClient getRestClient() { return restClient; }
        @Override public String getCurrentCode() { return ui.codeEditor.getText(); }
        @Override public boolean isTerminalMode() { return ui.terminalTab.isSelected(); }
        @Override public void clearOutput() { Python3IDE.this.clearOutput(); }
        @Override public void setOutputText(String text) { ui.outputArea.setText(text); }
        @Override public void setErrorText(String text) { ui.errorArea.setText(text); }
        @Override public void setStatus(String message, Color color) { Python3IDE.this.setStatus(message, color); }
        @Override public void clearEditor() { ui.codeEditor.setText(""); }
        @Override public void refreshDiagnostics() { connectionController.refreshDiagnostics(); }
        @Override public String getPythonVersion() {
            Object selected = ui.versionSelector.getSelectedItem();
            return selected != null ? selected.toString() : null;
        }
    }

    private final class TerminalCtx implements Python3IDETerminalController.TerminalContext {
        @Override public TerminalPanel getTerminalPanel() { return ui.terminalPanel; }
        @Override public JPanel getCenterPanel() { return ui.centerPanel; }
        @Override public RSyntaxTextArea getCodeEditor() { return ui.codeEditor; }
        @Override public JLabel getEditorTitleLabel() { return ui.editorTitleLabel; }
        @Override public JLabel getCurrentScriptLabel() { return ui.currentScriptLabel; }
        @Override public Python3RestClient getRestClient() { return restClient; }
        @Override public void setStatus(String message, Color color) { Python3IDE.this.setStatus(message, color); }
        @Override public void updateCurrentScriptLabel() { Python3IDE.this.updateCurrentScriptLabel(); }
    }

    private final class ScriptOpsCtx implements Python3IDEScriptOps.ScriptOpsContext {
        @Override public JComponent getParent() { return Python3IDE.this; }
        @Override public RSyntaxTextArea getCodeEditor() { return ui.codeEditor; }
        @Override public JTree getScriptTree() { return ui.scriptTree; }
        @Override public DefaultTreeModel getTreeModel() { return ui.treeModel; }
        @Override public ScriptTreeNode getRootNode() { return ui.rootNode; }
        @Override public ScriptMetadataPanel getMetadataPanel() { return ui.metadataPanel; }
        @Override public Python3RestClient getRestClient() { return restClient; }
        @Override public UnsavedChangesTracker getChangesTracker() { return changesTracker; }
        @Override public ScriptMetadata getCurrentScript() { return currentScript; }
        @Override public void setCurrentScript(ScriptMetadata metadata) { currentScript = metadata; }
        @Override public boolean isUseDarkTheme() { return useDarkTheme; }
        @Override public void setStatus(String message, Color color) { Python3IDE.this.setStatus(message, color); }
        @Override public void updateCurrentScriptLabel() { Python3IDE.this.updateCurrentScriptLabel(); }
        @Override public void stylePopupMenu(JPopupMenu menu) {
            themeController.stylePopupMenu(menu, useDarkTheme);
        }
    }
}
