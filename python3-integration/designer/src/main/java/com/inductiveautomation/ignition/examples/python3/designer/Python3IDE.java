package com.inductiveautomation.ignition.examples.python3.designer;

import com.inductiveautomation.ignition.designer.model.DesignerContext;
import com.inductiveautomation.ignition.examples.python3.designer.managers.AutoSaveManager;
import com.inductiveautomation.ignition.examples.python3.designer.managers.CommandPaletteManager;
import com.inductiveautomation.ignition.examples.python3.designer.managers.ExecutionManager;
import com.inductiveautomation.ignition.examples.python3.designer.managers.KeyboardShortcutsManager;
import com.inductiveautomation.ignition.examples.python3.designer.managers.RecentScriptsManager;
import com.inductiveautomation.ignition.examples.python3.designer.managers.ScriptImportExportManager;
import com.inductiveautomation.ignition.examples.python3.designer.managers.SearchManager;
import com.inductiveautomation.ignition.examples.python3.designer.managers.ScriptTransferManager;
import com.inductiveautomation.ignition.examples.python3.designer.ui.FindReplaceDialog;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.autocomplete.AutoCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DropMode;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Python 3 IDE panel for the Ignition Designer.
 *
 * <p>Professional IDE with comprehensive features:</p>
 * <ul>
 *   <li>RSyntaxTextArea with Python syntax highlighting</li>
 *   <li>Left sidebar with folder tree for script organization</li>
 *   <li>Metadata panel showing script information</li>
 *   <li>Theme system (light and dark themes)</li>
 *   <li>Theme-aware dialogs and context menus (v2.0.12+)</li>
 *   <li>Find/Replace functionality</li>
 *   <li>Enhanced keyboard shortcuts</li>
 *   <li>Unsaved changes detection</li>
 *   <li>Export/import functionality</li>
 *   <li>Auto-completion support</li>
 * </ul>
 */
public class Python3IDE extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(Python3IDE.class);
    private static final String PREF_THEME = "python3ide.theme";
    private static final String PREF_FONT_SIZE = "python3ide.fontsize";
    private static final String PREF_GATEWAY_OVERRIDE = "python3ide.gateway.override";
    private static final String PREF_AUTO_CONNECT = "python3ide.gateway.autoconnect";
    private static final String PREF_POOL_SIZE = "python3ide.pool.size";

    private final DesignerContext context;
    private String detectedGatewayUrl;   // Auto-detected from Designer
    private String effectiveGatewayUrl;  // URL actually used for connections
    private Python3RestClient restClient;
    private PythonSyntaxChecker syntaxChecker;
    private AutoCompletion autoCompletion;
    private Python3CompletionProvider completionProvider;  // v2.4.0: Track for status updates

    // UI Components
    private JTextField gatewayUrlField;
    private JButton connectButton;
    private RSyntaxTextArea codeEditor;
    private JLabel currentScriptLabel;
    private JTextArea outputArea;
    private JTextArea errorArea;
    private ModernStatusBar statusBar;
    private JLabel connectionStatusIndicator;  // v2.11.1: Connection status with colored dot
    private JButton executeButton;
    private JButton saveButton;
    private JButton saveAsButton;
    private JButton importButton;
    private JButton exportButton;
    private JButton fontIncreaseButton;
    private JButton fontDecreaseButton;
    private JProgressBar progressBar;

    // v2.5.21: Execution mode tabs (replaced dropdown with tabs like Output/Errors)
    private CustomTabButton pythonIdeTab;
    private CustomTabButton terminalTab;

    // v3.1.0: Python version selector
    private javax.swing.JComboBox<String> versionSelector;

    // Script Browser Components
    private JTree scriptTree;
    private DefaultTreeModel treeModel;
    private ScriptTreeNode rootNode;
    private ScriptMetadataPanel metadataPanel;
    private DiagnosticsPanel diagnosticsPanel;

    // Toolbar buttons for script tree (v2.0.15 - made instance vars for theme updates)
    private ModernButton newFolderBtn;
    private ModernButton newScriptBtn;
    private ModernButton refreshBtn;

    // v2.5.17: Removed JTabbedPane, replaced with custom tab solution

    // Editor container (v2.5.5 - made instance var for dynamic title updates)
    private JPanel editorContainer;
    private TerminalPanel terminalPanel;  // v2.5.9: True terminal UI
    private JPanel centerPanel;  // v2.5.9: Container that switches between editor and terminal
    private JLabel editorTitleLabel;  // v2.5.18: Replaced TitledBorder with simple label

    // Split panes (v2.0.22 - made instance vars for theme updates)
    private JSplitPane mainSplit;
    private JSplitPane sidebarSplit;
    private JSplitPane bottomSplit;

    // Collapsible panels state (v2.8.0)
    private boolean sidebarCollapsed = false;
    private int sidebarDividerLocation = 250;  // Remember location when collapsed

    // Theme and Settings
    private String currentTheme;
    private int fontSize;
    private boolean useDarkTheme = true;  // Track current theme for popup menu styling (v2.0.12)

    // Search Manager (v2.8.0)
    private SearchManager searchManager;

    // Command Palette (v2.9.0)
    private CommandPaletteManager commandPaletteManager;

    // Unsaved Changes Tracking
    private UnsavedChangesTracker changesTracker;
    private ScriptMetadata currentScript;

    // Recent Scripts Tracking (v2.8.0) - REMOVED in v2.15.8 per user request
    // private RecentScriptsManager recentScriptsManager;

    // Auto-Save (v2.8.0)
    private AutoSaveManager autoSaveManager;

    // Script Import/Export (v2.8.0)
    private ScriptImportExportManager importExportManager;

    // Execution Management (v2.8.0)
    private ExecutionManager executionManager;

    // Keyboard Shortcuts (v2.8.0)
    private KeyboardShortcutsManager keyboardShortcutsManager;

    // Script Transfer (v2.8.0)
    private ScriptTransferManager scriptTransferManager;

    /**
     * Creates a new Python 3 IDE panel.
     *
     * @param context the Designer context
     */
    public Python3IDE(DesignerContext context) {
        this.context = context;
        this.restClient = null;

        // Auto-detect Gateway URL (from system properties, env vars, or default)
        this.detectedGatewayUrl = detectGatewayUrl();
        LOGGER.info("Auto-detected Gateway URL: {}", this.detectedGatewayUrl);

        // Load preferences
        Preferences prefs = Preferences.userNodeForPackage(Python3IDE.class);
        this.currentTheme = prefs.get(PREF_THEME, "dark");
        this.fontSize = prefs.getInt(PREF_FONT_SIZE, 12);

        // Check for Gateway URL override in preferences
        String gatewayOverride = prefs.get(PREF_GATEWAY_OVERRIDE, "");
        if (gatewayOverride != null && !gatewayOverride.trim().isEmpty()) {
            this.effectiveGatewayUrl = gatewayOverride.trim();
            LOGGER.info("Using Gateway URL override from settings: {}", this.effectiveGatewayUrl);
        } else {
            this.effectiveGatewayUrl = this.detectedGatewayUrl;
            LOGGER.info("Using auto-detected Gateway URL: {}", this.effectiveGatewayUrl);
        }

        // Initialize recent scripts manager (v2.8.0) - REMOVED in v2.15.8 per user request
        // this.recentScriptsManager = new RecentScriptsManager();

        initComponents();
        layoutComponents();
        attachListeners();
        applyTheme(currentTheme);

        // Initialize search manager (v2.8.0)
        searchManager = new SearchManager(this, codeEditor, new SearchListenerImpl());

        // Initialize command palette manager (v2.9.0)
        commandPaletteManager = new CommandPaletteManager(
            this,
            new CommandPaletteManager.CommandActions() {
                @Override
                public void executeCode() {
                    Python3IDE.this.executeCode();
                }

                @Override
                public void clearOutput() {
                    Python3IDE.this.clearOutput();
                }

                @Override
                public void saveCurrentScript() {
                    Python3IDE.this.saveCurrentScript();
                }

                @Override
                public void saveScriptAs() {
                    Python3IDE.this.saveScriptAs();
                }

                @Override
                public void createNewScript() {
                    Python3IDE.this.createNewScript();
                }

                @Override
                public void refreshScriptTree() {
                    Python3IDE.this.refreshScriptTree();
                }

                @Override
                public void showFindDialog() {
                    Python3IDE.this.showFindDialog();
                }

                @Override
                public void showReplaceDialog() {
                    Python3IDE.this.showReplaceDialog();
                }

                @Override
                public void showAdvancedFindReplaceDialog() {
                    Python3IDE.this.showAdvancedFindReplaceDialog();
                }

                @Override
                public void toggleSidebar() {
                    Python3IDE.this.toggleSidebar();
                }

                @Override
                public void changeFontSize(int delta) {
                    Python3IDE.this.changeFontSize(delta);
                }

                @Override
                public void setFontSize(int size) {
                    Python3IDE.this.setFontSize(size);
                }

                @Override
                public void applyTheme(String themeKey) {
                    Python3IDE.this.applyTheme(themeKey);
                }

                @Override
                public void connectToGateway() {
                    Python3IDE.this.connectToGateway();
                }

                @Override
                public void openSettingsDialog() {
                    Python3IDE.this.openSettingsDialog();
                }

                @Override
                public void showInformationDialog() {
                    Python3IDE.this.showInformationDialog();
                }

                @Override
                public void openPackagesDialog() {
                    Python3IDE.this.openPackagesDialog();
                }
            },
            importButton,
            exportButton
        );

        // Initialize import/export manager (v2.8.0)
        importExportManager = new ScriptImportExportManager(
            this,
            new ScriptImportExportManager.ImportExportContext() {
                @Override
                public Python3RestClient getRestClient() {
                    return restClient;
                }

                @Override
                public ScriptMetadata getCurrentScript() {
                    return currentScript;
                }

                @Override
                public String getCurrentCode() {
                    return codeEditor.getText();
                }

                @Override
                public void saveScript(String name, String code, String author, String version, String description, String folderPath) {
                    Python3IDE.this.saveScript(name, code, author, version, description, folderPath);
                }

                @Override
                public void loadScript(String scriptName) {
                    Python3IDE.this.loadScript(scriptName);
                }

                @Override
                public void setStatus(String message, Color color) {
                    Python3IDE.this.setStatus(message, color);
                }
            },
            statusBar
        );

        // Initialize auto-save (v2.8.0)
        autoSaveManager = new AutoSaveManager(
            codeEditor,
            changesTracker,
            statusBar,
            new AutoSaveManager.AutoSaveContext() {
                @Override
                public ScriptMetadata getCurrentScript() {
                    return currentScript;
                }

                @Override
                public boolean isConnectedToGateway() {
                    return restClient != null;
                }
            }
        );
        autoSaveManager.initialize();

        // Initialize execution manager (v2.8.0)
        executionManager = new ExecutionManager(
            new ExecutionManager.ExecutionContext() {
                @Override
                public Python3RestClient getRestClient() {
                    return restClient;
                }

                @Override
                public String getCurrentCode() {
                    return codeEditor.getText();
                }

                @Override
                public boolean isTerminalMode() {
                    return terminalTab.isSelected();
                }

                @Override
                public void clearOutput() {
                    Python3IDE.this.clearOutput();
                }

                @Override
                public void setOutputText(String text) {
                    outputArea.setText(text);
                }

                @Override
                public void setErrorText(String text) {
                    errorArea.setText(text);
                }

                @Override
                public void setStatus(String message, Color color) {
                    Python3IDE.this.setStatus(message, color);
                }

                @Override
                public void clearEditor() {
                    codeEditor.setText("");
                }

                @Override
                public void refreshDiagnostics() {
                    Python3IDE.this.refreshDiagnostics();
                }

                @Override
                public String getPythonVersion() {
                    Object selected = versionSelector.getSelectedItem();
                    return selected != null ? selected.toString() : null;
                }
            },
            executeButton,
            progressBar
        );

        // Auto-connect to Gateway on startup if enabled (default: true)
        boolean autoConnect = prefs.getBoolean(PREF_AUTO_CONNECT, true);
        if (autoConnect) {
            LOGGER.info("Auto-connect enabled, connecting to Gateway...");
            connectToGateway();
        } else {
            LOGGER.info("Auto-connect disabled in settings");
        }
    }

    /**
     * Initializes all UI components.
     */
    private void initComponents() {
        // Gateway URL input (pre-populated with effective URL from auto-detection or override)
        gatewayUrlField = new JTextField(effectiveGatewayUrl, 15);  // v2.5.4: Reduced by 40% (25 → 15) to make room for Save buttons
        gatewayUrlField.setFont(ModernTheme.FONT_REGULAR);

        connectButton = ModernButton.createPrimary("Connect");

        // Code editor with RSyntaxTextArea
        codeEditor = new RSyntaxTextArea(20, 80);
        codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
        codeEditor.setCodeFoldingEnabled(true);
        codeEditor.setAutoIndentEnabled(true);
        codeEditor.setMarkOccurrences(true);
        codeEditor.setPaintTabLines(true);
        codeEditor.setTabSize(4);
        codeEditor.setFont(ModernTheme.FONT_CODE.deriveFont((float) fontSize));
        // v2.8.1: Line spacing improved via larger font size (14pt, up from 12pt in ModernTheme.FONT_MONOSPACE)
        // v2.5.8: Removed hint text - editor starts empty

        // Enable parser notifications for real-time error checking
        codeEditor.setMarkOccurrences(true);

        // Unsaved changes tracker
        changesTracker = new UnsavedChangesTracker(codeEditor);
        changesTracker.addChangeListener(this::onDirtyStateChanged);

        // Current script indicator label - Enhanced visibility (v2.15.3)
        currentScriptLabel = new JLabel("No script selected");
        currentScriptLabel.setFont(ModernTheme.withSize(ModernTheme.FONT_BOLD, 12));  // Increased from 11 to 12
        currentScriptLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        currentScriptLabel.setBackground(ModernTheme.BACKGROUND_LIGHT);  // Slightly lighter than editor background for contrast
        currentScriptLabel.setOpaque(true);  // v2.5.13: Make background visible
        currentScriptLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ModernTheme.BORDER_DEFAULT),  // Bottom border for separation
            BorderFactory.createEmptyBorder(5, 8, 5, 8)  // Increased padding for prominence
        ));

        // Output area
        outputArea = new JTextArea(8, 80);
        outputArea.setFont(ModernTheme.FONT_MONOSPACE);
        outputArea.setEditable(false);
        outputArea.setBackground(ModernTheme.BACKGROUND_DARKER);
        outputArea.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        outputArea.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);
        outputArea.setBorder(null);  // v2.5.3: Remove default border

        // Error area
        errorArea = new JTextArea(8, 80);
        errorArea.setFont(ModernTheme.FONT_MONOSPACE);
        errorArea.setEditable(false);
        errorArea.setBackground(ModernTheme.BACKGROUND_DARKER);
        errorArea.setForeground(ModernTheme.ERROR);
        errorArea.setCaretColor(ModernTheme.ERROR);
        errorArea.setBorder(null);  // v2.5.3: Remove default border

        // Status bar
        statusBar = new ModernStatusBar();

        // Pool click listener for adjusting pool size (v1.17.2)
        statusBar.setPoolClickListener(this::handlePoolClicked);

        // Buttons (v2.8.1: Matched to Styling.png - Proper visual hierarchy)
        // PRIMARY: Execute button - Blue, 32px height, bold (main action)
        executeButton = ModernButton.createPrimary("▶ Execute");
        executeButton.setToolTipText("Execute code on Gateway (Ctrl+Enter)");

        // SUCCESS: Save button - GREEN, 32px height, bold (safe action)
        saveButton = ModernButton.createSuccess("✓ Save");
        saveButton.setToolTipText("Save current script (Ctrl+S)");

        // SECONDARY: Other action buttons - Gray, 32px height, regular weight
        saveAsButton = ModernButton.createSecondary("✎ Save As...");
        saveAsButton.setToolTipText("Save script with metadata (Ctrl+Shift+S)");

        importButton = ModernButton.createSecondary("↓ Import...");
        importButton.setToolTipText("Import Python script from file");

        exportButton = ModernButton.createSecondary("↑ Export...");
        exportButton.setToolTipText("Export script to .py file");

        // UTILITY: Font size buttons - Small, 24px height (v2.8.1)
        fontIncreaseButton = ModernButton.createSmall("A+");
        fontIncreaseButton.setToolTipText("Increase Font Size (Ctrl++)");

        fontDecreaseButton = ModernButton.createSmall("A-");
        fontDecreaseButton.setToolTipText("Decrease Font Size (Ctrl+-)");

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);

        // v2.5.21: Create execution mode tabs (Python IDE / Terminal)
        pythonIdeTab = new CustomTabButton("Python IDE");
        terminalTab = new CustomTabButton("Terminal");
        pythonIdeTab.setSelected(true);  // Python IDE mode selected by default

        // v3.1.0: Python version selector
        versionSelector = new javax.swing.JComboBox<>();
        versionSelector.setToolTipText("Select Python version for execution");
        versionSelector.setPreferredSize(new java.awt.Dimension(100, 28));
        versionSelector.setMaximumSize(new java.awt.Dimension(120, 28));
        versionSelector.setFont(ModernTheme.FONT_REGULAR);

        // Script Browser Tree (Ignition Tag Browser style)
        rootNode = new ScriptTreeNode("Scripts");
        treeModel = new DefaultTreeModel(rootNode);
        scriptTree = new JTree(treeModel);
        scriptTree.setRootVisible(true);
        scriptTree.setShowsRootHandles(true);
        scriptTree.setCellRenderer(new ScriptTreeCellRenderer());
        scriptTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        scriptTree.setRowHeight(24);  // v2.8.1: Increased from 20px to 24px for better readability
        scriptTree.setDragEnabled(true);
        scriptTree.setDropMode(DropMode.ON_OR_INSERT);
        // Note: TransferHandler is set in attachListeners() after scriptTransferManager initialization

        // Ignition Tag Browser-style tree appearance
        scriptTree.setBackground(ModernTheme.TREE_BACKGROUND);
        scriptTree.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        scriptTree.setFont(ModernTheme.FONT_REGULAR);
        scriptTree.putClientProperty("JTree.lineStyle", "None");  // Clean look without connecting lines
        scriptTree.setToggleClickCount(1);  // Single-click to expand (like Tag Browser)

        // Metadata Panel
        metadataPanel = new ScriptMetadataPanel();

        // Diagnostics Panel (v1.15.0 - displays performance metrics)
        diagnosticsPanel = new DiagnosticsPanel();
    }

    /**
     * Lays out all components in the panel.
     */
    private void layoutComponents() {
        // v2.5.22: NUCLEAR FIX - Remove ALL gaps and borders
        setLayout(new BorderLayout(0, 0));  // Zero gaps (was 5,5)
        setBorder(null);  // v2.11.1: Remove border to eliminate white line around IDE window
        setBackground(ModernTheme.EDITOR_BACKGROUND);  // Match all child panels exactly

        // Top area: Gateway Connection with theme selector (v2.11.1: Reduced gap from 10 to 2)
        // v3.6.8: Replaced TitledBorder with floating card header for consistency
        JPanel gatewayPanel = new JPanel(new BorderLayout(2, 0));
        gatewayPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        gatewayPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernTheme.BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(0, 0, 2, 0)
        ));

        // v3.6.8: Add floating card header at top of gateway panel
        JPanel gatewayCardHeader = ModernTheme.createCardHeader("Gateway Connection", null);
        gatewayPanel.add(gatewayCardHeader, BorderLayout.NORTH);

        // Left side: Gateway URL display with status below (v2.11.1 - Minimal padding to save space)
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        leftPanel.setBorder(new EmptyBorder(2, 5, 2, 5));  // Reduced from 10 to 5

        // Make URL field read-only and styled as display
        gatewayUrlField.setEditable(false);
        gatewayUrlField.setBackground(ModernTheme.PANEL_BACKGROUND);
        gatewayUrlField.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        gatewayUrlField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Connection status indicator (v2.11.1) - placed below URL to save horizontal space
        connectionStatusIndicator = new JLabel("[●] Disconnected");
        connectionStatusIndicator.setForeground(ModernTheme.ERROR_BRIGHT);
        connectionStatusIndicator.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));  // Slightly smaller
        connectionStatusIndicator.setToolTipText("Gateway connection status");
        connectionStatusIndicator.setAlignmentX(Component.LEFT_ALIGNMENT);
        connectionStatusIndicator.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        leftPanel.add(gatewayUrlField);
        leftPanel.add(connectionStatusIndicator);
        gatewayPanel.add(leftPanel, BorderLayout.WEST);

        // Center: Execution mode tabs and action buttons (v2.8.1: Increased spacing to match Styling.png)
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, ModernTheme.BUTTON_GAP, ModernTheme.TOOLBAR_VPADDING));
        centerPanel.setBackground(ModernTheme.PANEL_BACKGROUND);

        // v2.5.21: Mode tabs panel (like Output/Errors tabs)
        JPanel modeTabsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        modeTabsPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        modeTabsPanel.setBorder(null);
        modeTabsPanel.add(pythonIdeTab);
        modeTabsPanel.add(terminalTab);

        centerPanel.add(modeTabsPanel);
        // v3.1.0: Version selector between tabs and execute button
        JPanel versionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        versionPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        versionPanel.add(new javax.swing.JLabel("Python:"));
        versionPanel.add(versionSelector);
        centerPanel.add(versionPanel);
        centerPanel.add(executeButton);
        centerPanel.add(saveButton);
        centerPanel.add(saveAsButton);
        centerPanel.add(importButton);
        centerPanel.add(exportButton);
        centerPanel.add(progressBar);
        gatewayPanel.add(centerPanel, BorderLayout.CENTER);

        // Right side: Action buttons (v2.7.0: Font controls moved to Settings, v2.11.4: Theme selector moved to Settings)
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, ModernTheme.BUTTON_GAP, ModernTheme.TOOLBAR_VPADDING));
        rightPanel.setBackground(ModernTheme.PANEL_BACKGROUND);

        // Versions button (v3.1.0: Python version installation management)
        ModernButton versionsButton = ModernButton.createDefault("Versions");
        versionsButton.setToolTipText("Install/uninstall Python versions");
        versionsButton.addActionListener(e -> openVersionManagerDialog());
        rightPanel.add(versionsButton);

        // Packages button (v2.7.0: Package management)
        ModernButton packagesButton = ModernButton.createDefault("📦 Packages");
        packagesButton.setToolTipText("Manage Python packages");
        packagesButton.addActionListener(e -> openPackagesDialog());
        rightPanel.add(packagesButton);

        // Settings button (v2.7.0: IDE settings)
        ModernButton settingsButton = ModernButton.createDefault("⚙ Settings");
        settingsButton.setToolTipText("Configure IDE settings");
        settingsButton.addActionListener(e -> openSettingsDialog());
        rightPanel.add(settingsButton);

        // Information button (v2.5.1, v2.5.4: Updated icon, v2.7.0: Moved to far right)
        ModernButton infoButton = ModernButton.createDefault("ⓘ Info");
        infoButton.setToolTipText("View module and Python version information");
        infoButton.addActionListener(e -> openInfoDialog());
        rightPanel.add(infoButton);

        gatewayPanel.add(rightPanel, BorderLayout.EAST);

        add(gatewayPanel, BorderLayout.NORTH);

        // Create main split pane (sidebar | editor) with themed UI (v2.3.3 - direct paint approach)
        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setUI(new ThemedSplitPaneUI(useDarkTheme ? ModernTheme.BORDER_DEFAULT : ModernTheme.LIGHT_BORDER));  // v2.5.7: Use BORDER_DEFAULT instead of BACKGROUND_DARKER
        mainSplit.setDividerLocation(250);
        mainSplit.setBackground(ModernTheme.EDITOR_BACKGROUND);  // v2.5.23: Match editor background exactly
        mainSplit.setBorder(null);
        mainSplit.setDividerSize(4);  // Reduced from 8 to 4 for cleaner look

        // Left sidebar: Script browser + metadata
        JPanel sidebar = createSidebar();
        mainSplit.setLeftComponent(sidebar);

        // Right side: Editor + output
        JPanel editorPanel = createEditorPanel();
        mainSplit.setRightComponent(editorPanel);

        add(mainSplit, BorderLayout.CENTER);

        // Status bar at the bottom
        add(statusBar, BorderLayout.SOUTH);
    }

    /**
     * Creates the left sidebar with script tree and metadata.
     */
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(5, 5));
        sidebar.setPreferredSize(new Dimension(250, 600));
        sidebar.setBackground(ModernTheme.BACKGROUND_DARK);

        // Script tree
        JScrollPane treeScroll = new JScrollPane(scriptTree);
        treeScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);  // Hide when not needed (Issue 4 - v1.15.1)
        treeScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        // v3.6.8: Replaced TitledBorder with card header for consistency
        treeScroll.setBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_SUBTLE, 1));
        treeScroll.setBackground(ModernTheme.TREE_BACKGROUND);
        treeScroll.getViewport().setBackground(ModernTheme.TREE_BACKGROUND);

        // Toolbar above tree - spread buttons to fill width nicely (v2.0.16 UX improvement)
        JPanel treeToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        treeToolbar.setBackground(ModernTheme.PANEL_BACKGROUND);

        newFolderBtn = ModernButton.createSmall("+Folder");
        newFolderBtn.setToolTipText("New Folder");
        newFolderBtn.addActionListener(e -> createNewFolder());

        newScriptBtn = ModernButton.createSmall("+Script");
        newScriptBtn.setToolTipText("New Script");
        newScriptBtn.addActionListener(e -> createNewScript());

        refreshBtn = ModernButton.createSmall("Refresh");
        refreshBtn.setToolTipText("Refresh Scripts");
        refreshBtn.addActionListener(e -> refreshScriptTree());

        treeToolbar.add(newFolderBtn);
        treeToolbar.add(newScriptBtn);
        treeToolbar.add(refreshBtn);

        // v3.6.8: Card header + toolbar + tree scroll in a wrapper panel
        JPanel scriptBrowserHeader = ModernTheme.createCardHeader("Script Browser", null);

        JPanel treeHeaderAndToolbar = new JPanel(new BorderLayout(0, 0));
        treeHeaderAndToolbar.setBackground(ModernTheme.BACKGROUND_DARK);
        treeHeaderAndToolbar.add(scriptBrowserHeader, BorderLayout.NORTH);
        treeHeaderAndToolbar.add(treeToolbar, BorderLayout.SOUTH);

        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.setBackground(ModernTheme.BACKGROUND_DARK);
        treePanel.add(treeHeaderAndToolbar, BorderLayout.NORTH);
        treePanel.add(treeScroll, BorderLayout.CENTER);

        // Bottom panel: metadata only (diagnostics moved to execution results panel - v1.17.2)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(ModernTheme.BACKGROUND_DARK);
        bottomPanel.add(metadataPanel, BorderLayout.CENTER);

        // Split tree and bottom panel (metadata only) with themed UI (v2.3.3)
        sidebarSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        sidebarSplit.setUI(new ThemedSplitPaneUI(useDarkTheme ? ModernTheme.BORDER_DEFAULT : ModernTheme.LIGHT_BORDER));  // v2.5.7: Use BORDER_DEFAULT instead of BACKGROUND_DARKER
        sidebarSplit.setTopComponent(treePanel);
        sidebarSplit.setBottomComponent(bottomPanel);
        sidebarSplit.setDividerLocation(400);  // More space for tree since diagnostics moved (v1.17.2)
        sidebarSplit.setResizeWeight(0.6);  // More weight to tree
        sidebarSplit.setBackground(ModernTheme.EDITOR_BACKGROUND);  // v2.5.23: Match editor background exactly
        sidebarSplit.setBorder(null);
        sidebarSplit.setDividerSize(4);  // Reduced from 8 to 4 for cleaner look

        sidebar.add(sidebarSplit, BorderLayout.CENTER);

        return sidebar;
    }

    /**
     * Creates the editor panel.
     */
    private JPanel createEditorPanel() {
        // v2.5.23: CRITICAL - Match background exactly to eliminate white border
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ModernTheme.EDITOR_BACKGROUND);  // v2.5.23: Match ALL child components exactly

        // Editor with line numbers
        RTextScrollPane editorScroll = new RTextScrollPane(codeEditor);
        editorScroll.setLineNumbersEnabled(true);

        // v2.5.20: CRITICAL - Remove ALL borders and set backgrounds to eliminate white rectangle
        editorScroll.setBorder(null);
        editorScroll.setViewportBorder(null);
        editorScroll.setOpaque(true);
        editorScroll.setBackground(ModernTheme.EDITOR_BACKGROUND);
        editorScroll.getViewport().setBackground(ModernTheme.EDITOR_BACKGROUND);
        editorScroll.getViewport().setOpaque(true);

        // v2.5.20: Fix gutter (line numbers) border and background
        if (editorScroll.getGutter() != null) {
            editorScroll.getGutter().setBorder(null);
            editorScroll.getGutter().setBackground(ModernTheme.EDITOR_BACKGROUND);
            editorScroll.getGutter().setOpaque(true);

            // v2.5.26: CRITICAL - Set gutter border color to match background (eliminates white rectangle)
            // RTextScrollPane draws a border around the entire component using gutter.getBorderColor()
            try {
                // Try to set border color if the method exists
                java.lang.reflect.Method setBorderColorMethod =
                    editorScroll.getGutter().getClass().getMethod("setBorderColor", java.awt.Color.class);
                setBorderColorMethod.invoke(editorScroll.getGutter(), ModernTheme.EDITOR_BACKGROUND);
            } catch (Exception e) {
                // If method doesn't exist, that's okay
            }
        }

        // v2.5.24: CRITICAL - Disable focus border that creates white rectangle
        editorScroll.setFocusable(false);  // Prevents focus border on scroll pane
        codeEditor.setBorder(null);  // No border on text area itself

        // v2.5.8: Hide scrollbars completely (Option A - invisible scrolling)
        editorScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        editorScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // v2.11.6: Enable mouse wheel scrolling without visible scrollbar (user request)
        editorScroll.addMouseWheelListener(e -> {
            int scrollAmount = e.getUnitsToScroll() * 3;  // Multiply by 3 for faster scrolling
            javax.swing.JScrollBar vertical = editorScroll.getVerticalScrollBar();
            vertical.setValue(vertical.getValue() + scrollAmount);
        });

        // v2.5.22: NUCLEAR FIX - Ensure code editor has zero margin and dark background
        codeEditor.setBackground(ModernTheme.EDITOR_BACKGROUND);
        codeEditor.setOpaque(true);
        codeEditor.setMargin(new java.awt.Insets(0, 0, 0, 0));  // ZERO internal margin

        // v2.5.22: Create simple editor container (just the scroll pane, no header)
        editorContainer = new JPanel(new BorderLayout(0, 0));
        editorContainer.setBackground(ModernTheme.EDITOR_BACKGROUND);
        editorContainer.setOpaque(true);
        editorContainer.setBorder(null);
        editorContainer.add(editorScroll, BorderLayout.CENTER);

        // v2.5.9: Create terminal panel for true terminal UX
        terminalPanel = new TerminalPanel(this::executeTerminalCommand);

        // v2.5.22: Create centerPanel with CardLayout to switch between editor and terminal
        centerPanel = new JPanel(new CardLayout());
        centerPanel.setBackground(ModernTheme.EDITOR_BACKGROUND);
        centerPanel.setOpaque(true);  // v2.5.22: Ensure opaque
        centerPanel.setBorder(null);  // v2.5.22: No border
        centerPanel.add(editorContainer, "EDITOR");
        centerPanel.add(terminalPanel, "TERMINAL");

        // v2.5.22: Create title panel (for "Python 3 Code Editor / Terminal" text)
        JPanel editorTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        editorTitlePanel.setBackground(ModernTheme.EDITOR_BACKGROUND);
        editorTitlePanel.setOpaque(true);
        editorTitlePanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));  // Padding for text only

        editorTitleLabel = new JLabel("Python 3 Code Editor");
        editorTitleLabel.setFont(ModernTheme.FONT_REGULAR);
        editorTitleLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        editorTitlePanel.add(editorTitleLabel);

        // v2.15.3: Add separator and current script label for better visibility
        JLabel separator = new JLabel(" | ");
        separator.setFont(ModernTheme.FONT_REGULAR);
        separator.setForeground(ModernTheme.FOREGROUND_MUTED);
        editorTitlePanel.add(separator);
        editorTitlePanel.add(currentScriptLabel);

        // v2.5.22: Create execution mode tab panel (Python IDE / Terminal tabs) - OUTSIDE CardLayout
        JPanel modeTabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        modeTabPanel.setBackground(ModernTheme.EDITOR_BACKGROUND);
        modeTabPanel.setOpaque(true);
        modeTabPanel.setBorder(null);
        modeTabPanel.add(pythonIdeTab);
        modeTabPanel.add(terminalTab);

        // v2.5.22: Create header panel (title + tabs) - stays visible in both modes
        JPanel topHeaderPanel = new JPanel(new BorderLayout(0, 0));
        topHeaderPanel.setBackground(ModernTheme.EDITOR_BACKGROUND);
        topHeaderPanel.setOpaque(true);
        topHeaderPanel.setBorder(null);  // v2.5.22: NO border
        topHeaderPanel.add(editorTitlePanel, BorderLayout.NORTH);
        topHeaderPanel.add(modeTabPanel, BorderLayout.SOUTH);

        // v2.5.22: Assemble panel with header (NORTH) + centerPanel with CardLayout (CENTER)
        panel.setBorder(null);  // v2.5.22: NO border on main panel
        panel.setOpaque(true);  // v2.5.22: Ensure opaque
        panel.add(topHeaderPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);

        // Start with editor view
        ((CardLayout) centerPanel.getLayout()).show(centerPanel, "EDITOR");

        // v2.5.22: Set tab click actions for execution mode switching
        pythonIdeTab.setClickAction(() -> {
            pythonIdeTab.setSelected(true);
            terminalTab.setSelected(false);
            onModeTabChanged(false);  // false = Python IDE mode
        });

        terminalTab.setClickAction(() -> {
            terminalTab.setSelected(true);
            pythonIdeTab.setSelected(false);
            onModeTabChanged(true);  // true = Terminal mode
        });

        // v2.5.17: Custom tab solution to eliminate white rectangles
        // Create scroll panes for output and error
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        outputScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outputScroll.setBorder(null);
        outputScroll.setViewportBorder(null);
        outputScroll.setBackground(ModernTheme.BACKGROUND_DARKER);
        outputScroll.getViewport().setBackground(ModernTheme.BACKGROUND_DARKER);
        outputScroll.setOpaque(true);

        JScrollPane errorScroll = new JScrollPane(errorArea);
        errorScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        errorScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        errorScroll.setBorder(null);
        errorScroll.setViewportBorder(null);
        errorScroll.setBackground(ModernTheme.BACKGROUND_DARKER);
        errorScroll.getViewport().setBackground(ModernTheme.BACKGROUND_DARKER);
        errorScroll.setOpaque(true);

        // Create custom tab buttons
        CustomTabButton outputTab = new CustomTabButton("Output");
        CustomTabButton errorTab = new CustomTabButton("Errors");
        outputTab.setSelected(true);  // Output selected by default

        // Create tab header panel
        JPanel tabHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabHeaderPanel.setBackground(ModernTheme.BACKGROUND_DARKER);
        tabHeaderPanel.setOpaque(true);
        tabHeaderPanel.setBorder(null);
        tabHeaderPanel.add(outputTab);
        tabHeaderPanel.add(errorTab);

        // Create content panel with CardLayout
        JPanel tabContentPanel = new JPanel(new CardLayout());
        tabContentPanel.setBackground(ModernTheme.BACKGROUND_DARKER);
        tabContentPanel.setOpaque(true);
        tabContentPanel.setBorder(null);
        tabContentPanel.add(outputScroll, "OUTPUT");
        tabContentPanel.add(errorScroll, "ERRORS");

        // Set tab click actions
        outputTab.setClickAction(() -> {
            outputTab.setSelected(true);
            errorTab.setSelected(false);
            ((CardLayout) tabContentPanel.getLayout()).show(tabContentPanel, "OUTPUT");
        });

        errorTab.setClickAction(() -> {
            errorTab.setSelected(true);  // v2.5.18: Fixed - was backwards
            outputTab.setSelected(false);
            ((CardLayout) tabContentPanel.getLayout()).show(tabContentPanel, "ERRORS");
        });

        // v2.5.23: CRITICAL - Remove line border to eliminate white rectangle around editor
        JPanel outputPanel = new JPanel(new BorderLayout(0, 0));
        outputPanel.setBackground(ModernTheme.BACKGROUND_DARKER);
        outputPanel.setOpaque(true);
        outputPanel.setBorder(null);  // v2.5.23: NO border - was creating white line above output panel

        // Create header for "Execution Results" title
        JPanel outputHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        outputHeaderPanel.setBackground(ModernTheme.BACKGROUND_DARKER);
        outputHeaderPanel.setOpaque(true);
        outputHeaderPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 8));  // Small padding for text

        JLabel outputTitleLabel = new JLabel("Execution Results");
        outputTitleLabel.setFont(ModernTheme.FONT_REGULAR);
        outputTitleLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        outputHeaderPanel.add(outputTitleLabel);

        // Container for title + tabs
        JPanel outputTopPanel = new JPanel(new BorderLayout(0, 0));
        outputTopPanel.setBackground(ModernTheme.BACKGROUND_DARKER);
        outputTopPanel.setOpaque(true);
        outputTopPanel.add(outputHeaderPanel, BorderLayout.NORTH);
        outputTopPanel.add(tabHeaderPanel, BorderLayout.SOUTH);

        outputPanel.add(outputTopPanel, BorderLayout.NORTH);
        outputPanel.add(tabContentPanel, BorderLayout.CENTER);

        // Split execution results (left 75%) and diagnostics (right 25%) with themed UI (v2.3.3)
        bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        bottomSplit.setUI(new ThemedSplitPaneUI(useDarkTheme ? ModernTheme.BORDER_DEFAULT : ModernTheme.LIGHT_BORDER));  // v2.5.7: Use BORDER_DEFAULT instead of BACKGROUND_DARKER
        bottomSplit.setLeftComponent(outputPanel);
        bottomSplit.setRightComponent(diagnosticsPanel);
        bottomSplit.setResizeWeight(0.75);  // 75% for execution results, 25% for diagnostics
        bottomSplit.setBackground(ModernTheme.EDITOR_BACKGROUND);  // v2.5.23: Match editor background exactly
        bottomSplit.setBorder(null);
        bottomSplit.setDividerSize(4);  // Reduced from 8 to 4 for cleaner look
        bottomSplit.setPreferredSize(new Dimension(600, 200));

        panel.add(bottomSplit, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Attaches event listeners to UI components.
     */
    private void attachListeners() {
        // Connect button
        connectButton.addActionListener(e -> connectToGateway());

        // Execute button
        executeButton.addActionListener(e -> executeCode());

        // Save button
        saveButton.addActionListener(e -> saveCurrentScript());

        // Save As button
        saveAsButton.addActionListener(e -> saveScriptAs());

        // Import button
        importButton.addActionListener(e -> importScript());

        // Export button
        exportButton.addActionListener(e -> exportCurrentScript());

        // Font size buttons (v2.0.28)
        fontIncreaseButton.addActionListener(e -> changeFontSize(1));
        fontDecreaseButton.addActionListener(e -> changeFontSize(-1));

        // Initialize keyboard shortcuts manager (v2.8.0)
        keyboardShortcutsManager = new KeyboardShortcutsManager(
            codeEditor,
            new KeyboardShortcutsManager.ShortcutActions() {
                @Override
                public void executeCode() {
                    Python3IDE.this.executeCode();
                }

                @Override
                public void saveCurrentScript() {
                    Python3IDE.this.saveCurrentScript();
                }

                @Override
                public void saveScriptAs() {
                    Python3IDE.this.saveScriptAs();
                }

                @Override
                public void createNewScript() {
                    Python3IDE.this.createNewScript();
                }

                @Override
                public void changeFontSize(int delta) {
                    Python3IDE.this.changeFontSize(delta);
                }

                @Override
                public void setFontSize(int size) {
                    Python3IDE.this.setFontSize(size);
                }

                @Override
                public void showFindDialog() {
                    Python3IDE.this.showFindDialog();
                }

                @Override
                public void showReplaceDialog() {
                    Python3IDE.this.showReplaceDialog();
                }

                @Override
                public void showAdvancedFindReplaceDialog() {
                    Python3IDE.this.showAdvancedFindReplaceDialog();
                }

                @Override
                public void showCommandPalette() {
                    Python3IDE.this.showCommandPalette();
                }

                @Override
                public void toggleSidebar() {
                    Python3IDE.this.toggleSidebar();
                }
            }
        );
        keyboardShortcutsManager.setupKeyboardShortcuts();

        // Initialize script transfer manager (v2.8.0)
        scriptTransferManager = new ScriptTransferManager(
            new ScriptTransferManager.TransferContext() {
                @Override
                public Python3RestClient getRestClient() {
                    return restClient;
                }

                @Override
                public ScriptTreeNode getRootNode() {
                    return rootNode;
                }

                @Override
                public void refreshScriptTree() {
                    Python3IDE.this.refreshScriptTree();
                }

                @Override
                public void setStatus(String message, Color color) {
                    Python3IDE.this.setStatus(message, color);
                }

                @Override
                public Component getParentComponent() {
                    return Python3IDE.this;
                }
            },
            rootNode
        );
        scriptTree.setTransferHandler(scriptTransferManager.createTransferHandler());

        // Tree selection
        scriptTree.addTreeSelectionListener(e -> onTreeSelectionChanged());

        // Tree double-click
        scriptTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    loadSelectedScript();
                }
            }
        });

        // Tree right-click
        scriptTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showContextMenu(e);
                }
            }
        });

        // Gateway URL enter key
        gatewayUrlField.addActionListener(e -> connectToGateway());

        // Cursor position tracking
        codeEditor.addCaretListener(e -> {
            int line = codeEditor.getCaretLineNumber() + 1;  // Convert to 1-based
            int col = codeEditor.getCaretOffsetFromLineStart() + 1;  // Convert to 1-based
            statusBar.setCursorPosition(line, col);
        });
    }


    /**
     * Connects to the Gateway.
     */
    private void connectToGateway() {
        // Use the URL from the text field (allows manual changes before clicking Connect)
        String url = gatewayUrlField.getText().trim();

        if (url.isEmpty()) {
            // If field is empty, use effective URL
            url = effectiveGatewayUrl;
            gatewayUrlField.setText(url);
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
            gatewayUrlField.setText(url);
        }

        // Update effective URL to reflect user's choice
        effectiveGatewayUrl = url;

        try {
            // Update connection status to "Connecting" (orange dot)
            connectionStatusIndicator.setText("[●] Connecting...");
            connectionStatusIndicator.setForeground(ModernTheme.WARNING);
            connectionStatusIndicator.setToolTipText("Connecting to " + url);

            restClient = new Python3RestClient(url);
            statusBar.setStatus("Connected to " + url, ModernStatusBar.MessageType.SUCCESS);
            statusBar.setConnection("Connected", ModernTheme.SUCCESS);
            statusBar.setPoolStats("Pool: Checking...", ModernTheme.INFO);

            // Update connection status to "Connected" (green dot)
            connectionStatusIndicator.setText("[●] Connected");
            connectionStatusIndicator.setForeground(ModernTheme.SUCCESS);
            connectionStatusIndicator.setToolTipText("Connected to " + url);

            // Initialize diagnostics panel with REST client (v1.15.0)
            diagnosticsPanel.setRestClient(restClient);

            LOGGER.info("Connected to Gateway: {}", url);

            // Initialize syntax checker for real-time error checking
            if (syntaxChecker != null) {
                syntaxChecker.dispose();
                codeEditor.removeParser(syntaxChecker);
            }
            syntaxChecker = new PythonSyntaxChecker(codeEditor, restClient);
            codeEditor.addParser(syntaxChecker);
            LOGGER.info("Real-time syntax checking enabled");

            // Initialize auto-completion with Jedi-powered completions
            if (autoCompletion != null) {
                autoCompletion.uninstall();
            }
            completionProvider = new Python3CompletionProvider(restClient);
            autoCompletion = new AutoCompletion(completionProvider);
            autoCompletion.setAutoActivationEnabled(true);
            autoCompletion.setAutoCompleteSingleChoices(false);
            autoCompletion.setAutoActivationDelay(500);  // 500ms delay after typing
            autoCompletion.setShowDescWindow(true);
            autoCompletion.setTriggerKey(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK));  // Ctrl+Space
            autoCompletion.install(codeEditor);
            LOGGER.info("Auto-completion enabled: Ctrl+Space to trigger, auto-activates on typing");

            // v2.4.0: Update autocomplete status indicator
            updateAutocompleteStatus();

            refreshDiagnostics();
            refreshPythonVersion();
            refreshAvailableVersions();
            refreshScriptTree();

        } catch (Exception e) {
            statusBar.setStatus("Connection failed: " + e.getMessage(), ModernStatusBar.MessageType.ERROR);
            statusBar.setConnection("Not Connected", ModernTheme.ERROR_BRIGHT);  // v2.8.1: Bright red for visibility
            statusBar.setPoolStats("Pool: Not Connected", ModernTheme.ERROR_BRIGHT);
            statusBar.setPythonVersion("Python: --");

            // Update connection status to "Disconnected" (red dot)
            connectionStatusIndicator.setText("[●] Disconnected");
            connectionStatusIndicator.setForeground(ModernTheme.ERROR_BRIGHT);
            connectionStatusIndicator.setToolTipText("Connection failed: " + e.getMessage());

            LOGGER.error("Failed to connect to Gateway: {}", url, e);
        }
    }

    /**
     * Executes the Python code in the editor or shell command.
     * v2.5.0: Added support for Shell Command mode
     */
    private void executeCode() {
        executionManager.executeCode();
    }


    /**
     * Handles execution mode change between Python Code and Terminal.
     * v2.5.21: Changed from dropdown to tabs, now accepts boolean parameter
     *
     * @param isTerminalMode true for Terminal mode, false for Python IDE mode
     */
    private void onModeTabChanged(boolean isTerminalMode) {

        if (isTerminalMode) {
            // v2.5.9: Switch to terminal panel view
            ((CardLayout) centerPanel.getLayout()).show(centerPanel, "TERMINAL");

            // Create shell session if needed
            if (!executionManager.hasActiveShellSession() && restClient != null) {
                executionManager.createShellSession();
                // Get initial working directory
                updateTerminalWorkingDirectory();
            }

            // Focus the terminal command input
            terminalPanel.focusCommandInput();

            // v2.5.22: Update title label for Terminal mode
            editorTitleLabel.setText("Terminal");
            currentScriptLabel.setVisible(false);  // Hide script label in terminal mode

            setStatus("Terminal mode: Interactive shell (type commands and press Enter)", ModernTheme.ACCENT_PRIMARY);
        } else {
            // v2.5.9: Switch back to editor panel view
            ((CardLayout) centerPanel.getLayout()).show(centerPanel, "EDITOR");

            // Switching from Terminal to Python - close shell session
            executionManager.closeShellSession();

            // Python Code mode: Restore Python syntax highlighting
            codeEditor.setSyntaxEditingStyle(org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_PYTHON);
            codeEditor.setBackground(ModernTheme.EDITOR_BACKGROUND);  // Standard background
            codeEditor.setCurrentLineHighlightColor(ModernTheme.BACKGROUND_LIGHT);
            codeEditor.setFont(ModernTheme.FONT_MONOSPACE);  // Standard monospace

            // v2.5.18: Restore editor panel title and script indicator (using label instead of TitledBorder)
            editorTitleLabel.setText("Python 3 Code Editor");
            currentScriptLabel.setVisible(true);
            // v2.15.3: Use updateCurrentScriptLabel() for consistent formatting
            updateCurrentScriptLabel();

            setStatus("Python Code mode: Write Python 3 code", ModernTheme.ACCENT_PRIMARY);
        }

        centerPanel.revalidate();
        centerPanel.repaint();
    }

    /**
     * Clears output areas.
     */
    private void clearOutput() {
        outputArea.setText("");
        errorArea.setText("");
    }

    /**
     * Executes a terminal command (called from TerminalPanel).
     *
     * v2.5.9: Terminal command execution with inline output
     */
    private void executeTerminalCommand(String command) {
        if (restClient == null) {
            terminalPanel.appendOutput("ERROR: Not connected to gateway");
            return;
        }

        // v2.11.4: Check for sudo and warn (not available in Python subprocess)
        if (command.trim().startsWith("sudo ")) {
            terminalPanel.appendOutput("WARNING: sudo is not available. Command will run without elevated privileges.\n");
            command = command.trim().substring(5).trim();  // Remove "sudo " prefix
        }

        // v2.11.4: Auto-add --break-system-packages to pip install commands
        if (command.trim().matches("pip3?\\s+install\\s+.*") && !command.contains("--break-system-packages")) {
            command = command.trim() + " --break-system-packages";
            LOGGER.info("Auto-added --break-system-packages to pip install command");
        }

        final String finalCommand = command;

        // v2.11.4: Execute via Python subprocess with proper PATH environment
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                // Escape single quotes in command for Python string literal
                String escapedCommand = finalCommand.replace("'", "'\\''");

                // Build Python code to execute shell command with proper PATH
                String pythonCode = String.format(
                    "import subprocess\n" +
                    "import os\n" +
                    "os.environ['PATH'] = '/usr/local/bin:/usr/bin:/bin:/usr/local/sbin:/usr/sbin:/sbin'\n" +
                    "result = subprocess.run('%s', shell=True, capture_output=True, text=True, timeout=30)\n" +
                    "output = result.stdout\n" +
                    "if result.stderr:\n" +
                    "    output += result.stderr\n" +  // Combine stdout and stderr
                    "result = output if output else '(no output)'",
                    escapedCommand
                );

                ExecutionResult result = restClient.executeCode(pythonCode, null);
                return result.getResult();
            }

            @Override
            protected void done() {
                try {
                    String output = get();
                    if (output != null && !output.isEmpty()) {
                        // v2.11.4: Prefix errors with ERROR: for visibility
                        if (output.toLowerCase().contains("error")) {
                            terminalPanel.appendOutput("ERROR: " + output + "\n");
                        } else {
                            terminalPanel.appendOutput(output + "\n");
                        }
                    } else {
                        terminalPanel.appendOutput("(no output)\n");
                    }

                    // Update working directory if command was cd
                    if (finalCommand.trim().startsWith("cd ")) {
                        updateTerminalWorkingDirectory();
                    }
                } catch (Exception e) {
                    String errorMsg = "ERROR: " + e.getMessage();
                    terminalPanel.appendOutput(errorMsg + "\n");
                    LOGGER.error("Terminal command execution failed", e);
                }
            }
        };

        worker.execute();
    }

    /**
     * Updates the terminal prompt with current working directory.
     *
     * v2.5.9: Fetch pwd and update terminal prompt
     */
    private void updateTerminalWorkingDirectory() {
        String sessionId = executionManager.getShellSessionId();
        if (restClient == null || sessionId == null) {
            return;
        }

        // Execute pwd command to get current directory
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                // Detect OS
                String os = System.getProperty("os.name").toLowerCase();
                String pwdCommand = os.contains("win") ? "cd" : "pwd";

                ExecutionResult result = restClient.executeInteractiveShellCommand(sessionId, pwdCommand);
                return result.getResult();
            }

            @Override
            protected void done() {
                try {
                    String pwd = get();
                    if (pwd != null && !pwd.isEmpty()) {
                        // Clean up output (remove trailing newlines, etc.)
                        pwd = pwd.trim();
                        if (!pwd.isEmpty()) {
                            terminalPanel.updateWorkingDirectory(pwd);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to get working directory", e);
                }
            }
        };

        worker.execute();
    }

    /**
     * Refreshes diagnostics.
     *
     * v2.0.18: Also refresh diagnostics panel (manual refresh after auto-polling removal)
     */
    private void refreshDiagnostics() {
        if (restClient == null) {
            return;
        }

        SwingWorker<PoolStats, Void> worker = new SwingWorker<PoolStats, Void>() {
            @Override
            protected PoolStats doInBackground() throws Exception {
                return restClient.getPoolStats();
            }

            @Override
            protected void done() {
                try {
                    PoolStats stats = get();
                    statusBar.updatePoolStats(stats);
                } catch (Exception e) {
                    statusBar.setPoolStats("Pool: Unavailable", ModernTheme.ERROR);
                }
            }
        };

        worker.execute();

        // v2.0.18: Manually refresh diagnostics panel (no auto-refresh timer anymore)
        if (diagnosticsPanel != null) {
            diagnosticsPanel.refreshMetrics();
        }
    }

    /**
     * Refreshes Python version display in status bar.
     *
     * v2.0.15: Completely rebuilt for reliability - now synchronous since called from background thread
     */
    private void refreshPythonVersion() {
        LOGGER.info("refreshPythonVersion() - START");

        if (restClient == null) {
            LOGGER.warn("refreshPythonVersion() - restClient is null");
            SwingUtilities.invokeLater(() -> statusBar.setPythonVersion("Python: --"));
            return;
        }

        // Since we're already in a SwingWorker from connectToGateway(), we can call synchronously
        try {
            LOGGER.info("refreshPythonVersion() - Calling restClient.getPythonVersion()");
            String version = restClient.getPythonVersion();

            if (version == null || version.trim().isEmpty()) {
                LOGGER.warn("refreshPythonVersion() - Received null or empty version");
                SwingUtilities.invokeLater(() -> statusBar.setPythonVersion("Python: --"));
                return;
            }

            LOGGER.info("refreshPythonVersion() - Successfully retrieved version: {}", version);

            // Update UI on EDT
            final String finalVersion = version;
            SwingUtilities.invokeLater(() -> {
                statusBar.setPythonVersion("Python: " + finalVersion);
                LOGGER.info("refreshPythonVersion() - Status bar updated with: {}", finalVersion);
            });

        } catch (IOException e) {
            LOGGER.error("refreshPythonVersion() - IOException occurred: {}", e.getMessage(), e);
            SwingUtilities.invokeLater(() -> statusBar.setPythonVersion("Python: Connection Error"));
        } catch (Exception e) {
            LOGGER.error("refreshPythonVersion() - Unexpected exception: {}", e.getMessage(), e);
            SwingUtilities.invokeLater(() -> statusBar.setPythonVersion("Python: Error"));
        }

        LOGGER.info("refreshPythonVersion() - END");
    }

    /**
     * Loads available Python versions from the Gateway and populates the version selector.
     *
     * v3.1.0: Multi-version support
     */
    private void refreshAvailableVersions() {
        LOGGER.info("refreshAvailableVersions() - Loading available Python versions");

        if (restClient == null) {
            LOGGER.warn("refreshAvailableVersions() - restClient is null");
            return;
        }

        try {
            java.util.List<String> versions = restClient.getAvailableVersions();
            String defaultVersion = restClient.getDefaultPythonVersion();

            SwingUtilities.invokeLater(() -> {
                String previousSelection = (String) versionSelector.getSelectedItem();
                versionSelector.removeAllItems();

                if (versions.isEmpty()) {
                    versionSelector.addItem("default");
                    versionSelector.setEnabled(false);
                } else {
                    for (String v : versions) {
                        versionSelector.addItem(v);
                    }
                    versionSelector.setEnabled(versions.size() > 1);

                    // Restore previous selection or set default
                    if (previousSelection != null && versions.contains(previousSelection)) {
                        versionSelector.setSelectedItem(previousSelection);
                    } else if (defaultVersion != null && versions.contains(defaultVersion)) {
                        versionSelector.setSelectedItem(defaultVersion);
                    }
                }

                LOGGER.info("Version selector populated with {} version(s), default: {}",
                    versions.size(), defaultVersion);
            });

        } catch (Exception e) {
            LOGGER.warn("Failed to load available versions: {}", e.getMessage());
            SwingUtilities.invokeLater(() -> {
                versionSelector.removeAllItems();
                versionSelector.addItem("default");
                versionSelector.setEnabled(false);
            });
        }
    }

    /**
     * Updates the autocomplete status indicator in the status bar.
     *
     * v2.4.0: New method for autocomplete diagnostics
     */
    private void updateAutocompleteStatus() {
        if (completionProvider == null) {
            statusBar.setAutocomplete("AC: --", ModernTheme.FOREGROUND_SECONDARY);
            return;
        }

        // Check autocomplete availability
        if (completionProvider.isAvailable()) {
            statusBar.setAutocomplete("AC: Ready", ModernTheme.SUCCESS);
            statusBar.setStatus(completionProvider.getStatusMessage(), ModernStatusBar.MessageType.SUCCESS);
        } else {
            String status = completionProvider.getStatusMessage();
            if (status.contains("Jedi not installed")) {
                statusBar.setAutocomplete("AC: No Jedi", ModernTheme.WARNING);
                statusBar.setStatus("Autocomplete unavailable - Install Jedi: pip install jedi", ModernStatusBar.MessageType.WARNING);
            } else {
                statusBar.setAutocomplete("AC: Cooldown", ModernTheme.INFO);
            }
        }
    }

    /**
     * Handles pool stats click event to adjust pool size.
     *
     * v1.17.2: Allow user to adjust pool size (1-20)
     */
    private void handlePoolClicked() {
        if (restClient == null) {
            DarkDialog.showMessage(
                    this,
                    "Please connect to a Gateway first",
                    "Not Connected"
            );
            return;
        }

        // Get current pool size
        int currentSize = 3;  // Default
        try {
            PoolStats stats = restClient.getPoolStats();
            currentSize = stats.getTotalSize();
        } catch (Exception e) {
            LOGGER.warn("Failed to get current pool size", e);
        }

        // Show input dialog to adjust pool size
        String input = DarkDialog.showInput(
                this,
                "Enter new pool size (1-20):",
                "Adjust Pool Size",
                String.valueOf(currentSize)
        );

        if (input == null || input.trim().isEmpty()) {
            return;  // User cancelled
        }

        try {
            int newSize = Integer.parseInt(input.trim());

            if (newSize < 1 || newSize > 20) {
                DarkDialog.showMessage(
                        this,
                        "Pool size must be between 1 and 20",
                        "Invalid Pool Size"
                );
                return;
            }

            // Set the new pool size via REST API
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    restClient.setPoolSize(newSize);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        setStatus("Pool size changed to " + newSize, ModernTheme.SUCCESS);
                        refreshDiagnostics();  // Refresh to show new pool size
                    } catch (Exception e) {
                        LOGGER.error("Failed to set pool size", e);
                        DarkDialog.showMessage(
                                Python3IDE.this,
                                "Failed to set pool size: " + e.getMessage(),
                                "Error"
                        );
                    }
                }
            };

            worker.execute();

        } catch (NumberFormatException e) {
            DarkDialog.showMessage(
                    this,
                    "Please enter a valid number",
                    "Invalid Input"
            );
        }
    }

    /**
     * Sets status message with color.
     */
    private void setStatus(String message, Color color) {
        // Map Color to MessageType
        ModernStatusBar.MessageType type = ModernStatusBar.MessageType.INFO;
        if (color.equals(Color.RED) || color.equals(ModernTheme.ERROR)) {
            type = ModernStatusBar.MessageType.ERROR;
        } else if (color.equals(Color.ORANGE) || color.equals(ModernTheme.WARNING)) {
            type = ModernStatusBar.MessageType.WARNING;
        } else if (color.equals(ModernTheme.SUCCESS) || color.equals(ModernTheme.SUCCESS)) {
            type = ModernStatusBar.MessageType.SUCCESS;
        }
        statusBar.setStatus(message, type);
    }

    // Script Management Methods

    /**
     * Validates a name for illegal characters.
     * Script and folder names cannot contain: / \ : * ? " < > |
     *
     * @param name the name to validate
     * @return true if valid, false if contains illegal characters
     */
    private boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        // Check for illegal characters
        String illegalChars = "/\\:*?\"<>|";
        for (char c : illegalChars.toCharArray()) {
            if (name.indexOf(c) >= 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Shows an error dialog for invalid name.
     */
    private void showInvalidNameError(String name) {
        DarkDialog.showMessage(
                this,
                "Invalid name: '" + name + "'\n\n" +
                "Names cannot contain the following characters:\n" +
                "/ \\ : * ? \" < > |",
                "Invalid Name"
        );
    }

    /**
     * Refreshes the script tree from the Gateway.
     */
    private void refreshScriptTree() {
        if (restClient == null) {
            return;
        }

        SwingWorker<List<ScriptMetadata>, Void> worker = new SwingWorker<List<ScriptMetadata>, Void>() {
            @Override
            protected List<ScriptMetadata> doInBackground() throws Exception {
                return restClient.listScripts();
            }

            @Override
            protected void done() {
                try {
                    List<ScriptMetadata> scripts = get();
                    buildScriptTree(scripts);
                    LOGGER.info("Loaded {} scripts", scripts.size());
                } catch (Exception e) {
                    LOGGER.error("Failed to load scripts", e);
                }
            }
        };

        worker.execute();
    }

    /**
     * Builds the script tree from a list of scripts.
     * v2.15.8: Removed "Recent" folder feature per user request.
     */
    private void buildScriptTree(List<ScriptMetadata> scripts) {
        rootNode.removeAllChildren();

        // Build folder structure
        Map<String, ScriptTreeNode> folders = new HashMap<>();

        for (ScriptMetadata script : scripts) {
            String folderPath = script.getFolderPath();

            if (folderPath == null || folderPath.isEmpty()) {
                // Script at root level
                rootNode.add(new ScriptTreeNode(script));
            } else {
                // Create folder hierarchy
                ScriptTreeNode parent = getOrCreateFolder(folderPath, folders);
                parent.add(new ScriptTreeNode(script));
            }
        }

        // v2.15.6: Preserve current script metadata before reload to prevent clearing on double-click
        ScriptMetadata preservedMetadata = currentScript;

        treeModel.reload();
        scriptTree.expandRow(0);  // Expand root

        // v2.15.6: Restore metadata panel if script was loaded (prevents clearing on double-click)
        if (preservedMetadata != null && preservedMetadata.getName() != null) {
            metadataPanel.displayMetadata(preservedMetadata);
        }
    }

    /**
     * Gets or creates a folder node at the specified path.
     */
    private ScriptTreeNode getOrCreateFolder(String folderPath, Map<String, ScriptTreeNode> folders) {
        if (folders.containsKey(folderPath)) {
            return folders.get(folderPath);
        }

        String[] parts = folderPath.split("/");
        ScriptTreeNode currentParent = rootNode;
        StringBuilder currentPath = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (currentPath.length() > 0) {
                currentPath.append("/");
            }
            currentPath.append(part);

            String pathStr = currentPath.toString();

            if (folders.containsKey(pathStr)) {
                currentParent = folders.get(pathStr);
            } else {
                ScriptTreeNode folderNode = new ScriptTreeNode(part);
                currentParent.add(folderNode);
                folders.put(pathStr, folderNode);
                currentParent = folderNode;
            }
        }

        return currentParent;
    }

    /**
     * Called when tree selection changes.
     */
    private void onTreeSelectionChanged() {
        TreePath path = scriptTree.getSelectionPath();

        if (path == null) {
            metadataPanel.clear();
            return;
        }

        Object node = path.getLastPathComponent();

        if (node instanceof ScriptTreeNode) {
            ScriptTreeNode scriptNode = (ScriptTreeNode) node;

            if (scriptNode.isScript()) {
                metadataPanel.displayMetadata(scriptNode.getScriptMetadata());
            } else {
                metadataPanel.clear();
            }
        }
    }

    /**
     * Loads the selected script into the editor.
     */
    private void loadSelectedScript() {
        TreePath path = scriptTree.getSelectionPath();

        if (path == null) {
            return;
        }

        Object node = path.getLastPathComponent();

        if (!(node instanceof ScriptTreeNode)) {
            return;
        }

        ScriptTreeNode scriptNode = (ScriptTreeNode) node;

        if (!scriptNode.isScript()) {
            return;  // Can't load a folder
        }

        // Check for unsaved changes
        if (changesTracker.isDirty()) {
            int choice = showUnsavedChangesDialog();

            if (choice == JOptionPane.YES_OPTION) {
                // Save current script
                saveCurrentScript();
            } else if (choice == JOptionPane.CANCEL_OPTION) {
                // Cancel loading
                return;
            }
            // NO_OPTION falls through to discard changes
        }

        // Load the script
        ScriptMetadata metadata = scriptNode.getScriptMetadata();
        loadScript(metadata.getName());
    }

    /**
     * Loads a script by name.
     */
    private void loadScript(String name) {
        if (restClient == null) {
            return;
        }

        SwingWorker<SavedScript, Void> worker = new SwingWorker<SavedScript, Void>() {
            @Override
            protected SavedScript doInBackground() throws Exception {
                return restClient.loadScript(name);
            }

            @Override
            protected void done() {
                try {
                    SavedScript script = get();
                    changesTracker.loadContent(script.getCode());
                    currentScript = convertToMetadata(script);
                    updateCurrentScriptLabel();
                    setStatus("Loaded: " + script.getName(), ModernTheme.SUCCESS);

                    // v2.15.8: Recent folder feature removed per user request
                } catch (Exception e) {
                    LOGGER.error("Failed to load script", e);
                    DarkDialog.showMessage(
                            Python3IDE.this,
                            "Failed to load script: " + e.getMessage(),
                            "Error"
                    );
                }
            }
        };

        worker.execute();
    }

    /**
     * Saves the current script.
     * If script metadata exists (already saved), does a quick save WITHOUT prompting.
     * Otherwise, shows the save dialog.
     * v2.15.3: Enhanced to ensure Save button never prompts for existing scripts
     */
    private void saveCurrentScript() {
        if (restClient == null) {
            DarkDialog.showMessage(
                    this,
                    "Please connect to a Gateway first",
                    "Not Connected"
            );
            return;
        }

        String code = codeEditor.getText().trim();

        if (code.isEmpty()) {
            DarkDialog.showMessage(
                    this,
                    "Cannot save empty script",
                    "Empty Script"
            );
            return;
        }

        // If script already has metadata, do a quick save WITHOUT prompting
        if (currentScript != null && currentScript.getName() != null && !currentScript.getName().isEmpty()) {
            String name = currentScript.getName();
            String author = currentScript.getAuthor() != null ? currentScript.getAuthor() : System.getProperty("user.name", "Unknown");
            String version = currentScript.getVersion() != null ? currentScript.getVersion() : "1.0";
            String folder = currentScript.getFolderPath() != null ? currentScript.getFolderPath() : "";
            String description = currentScript.getDescription() != null ? currentScript.getDescription() : "";

            // Quick save - no dialog
            LOGGER.info("Quick save (no prompt) for existing script: {}", name);
            setStatus("Saving " + name + "...", Color.BLUE);
            saveScript(name, code, description, author, folder, version);
        } else {
            // New script - show save dialog
            LOGGER.info("New script - showing save dialog");
            saveScriptAs();
        }
    }

    /**
     * Shows the Save As dialog to save script with a new name.
     */
    private void saveScriptAs() {
        if (restClient == null) {
            DarkDialog.showMessage(
                    this,
                    "Please connect to a Gateway first",
                    "Not Connected"
            );
            return;
        }

        String code = codeEditor.getText().trim();

        if (code.isEmpty()) {
            DarkDialog.showMessage(
                    this,
                    "Cannot save empty script",
                    "Empty Script"
            );
            return;
        }

        // Show save dialog
        showSaveDialog(null);
    }

    /**
     * Shows the save script dialog using custom dark-themed dialog.
     *
     * v2.0.11: Replaced JOptionPane with DarkDialog for proper dark theme support
     */
    private void showSaveDialog() {
        showSaveDialog(null);
    }

    /**
     * Shows the save script dialog with optional pre-populated folder path.
     *
     * @param folderPath the folder path to pre-populate, or null to use current script folder
     */
    private void showSaveDialog(String folderPath) {
        // Auto-detect current user (OS username) for new scripts
        String defaultAuthor = currentScript != null ? currentScript.getAuthor() : System.getProperty("user.name", "Unknown");

        // Use provided folder path, or current script folder, or empty string
        String defaultFolder = folderPath != null ? folderPath :
                              (currentScript != null ? currentScript.getFolderPath() : "");

        // Prepare fields with current values
        Map<String, String> fields = new java.util.LinkedHashMap<>();
        fields.put("Script Name", currentScript != null ? currentScript.getName() : "");
        fields.put("Author", defaultAuthor);
        fields.put("Version", currentScript != null ? currentScript.getVersion() : "1.0");
        fields.put("Folder Path", defaultFolder != null ? defaultFolder : "");
        fields.put("Description", currentScript != null ? currentScript.getDescription() : "");

        // Show custom dark dialog
        Map<String, String> result = DarkDialog.showMultiInput(this, "Save Script", fields);

        if (result == null) {
            return;  // User cancelled
        }

        String name = result.get("Script Name").trim();
        String author = result.get("Author").trim();
        String version = result.get("Version").trim();
        String folder = result.get("Folder Path").trim();
        String description = result.get("Description").trim();

        if (name.isEmpty()) {
            DarkDialog.showMessage(
                    this,
                    "Script name cannot be empty",
                    "Invalid Name"
            );
            return;
        }

        // Validate script name
        if (!isValidName(name)) {
            showInvalidNameError(name);
            return;
        }

        saveScript(name, codeEditor.getText(), description, author, folder, version);
    }

    /**
     * Saves a script to the Gateway.
     */
    private void saveScript(String name, String code, String description,
                           String author, String folderPath, String version) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                restClient.saveScript(name, code, description, author, folderPath, version);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    changesTracker.markSaved();

                    // Update currentScript metadata for future quick saves
                    currentScript = new ScriptMetadata(
                        currentScript != null ? currentScript.getId() : null,
                        name,
                        description,
                        author,
                        currentScript != null ? currentScript.getCreatedDate() : null,
                        currentScript != null ? currentScript.getLastModified() : null,
                        folderPath,
                        version
                    );

                    setStatus("Script saved: " + name, ModernTheme.SUCCESS);
                    refreshScriptTree();
                } catch (Exception e) {
                    LOGGER.error("Failed to save script", e);
                    DarkDialog.showMessage(
                            Python3IDE.this,
                            "Failed to save script: " + e.getMessage(),
                            "Error"
                    );
                }
            }
        };

        worker.execute();
    }

    /**
     * Creates a new folder.
     */
    private void createNewFolder() {
        String folderName = DarkDialog.showInput(
                this,
                "Enter folder name:",
                "New Folder",
                ""
        );

        if (folderName == null || folderName.trim().isEmpty()) {
            return;
        }

        String trimmedName = folderName.trim();

        // Validate folder name
        if (!isValidName(trimmedName)) {
            showInvalidNameError(trimmedName);
            return;
        }

        // Create folder in tree
        ScriptTreeNode newFolder = new ScriptTreeNode(trimmedName);
        rootNode.add(newFolder);
        treeModel.reload();
        scriptTree.expandPath(new TreePath(rootNode.getPath()));
    }

    /**
     * Creates a new script.
     * v2.11.2: Now shows metadata dialog before creating script
     */
    private void createNewScript() {
        createNewScript(null);
    }

    /**
     * Creates a new script with an optional folder path.
     * @param folderPath the folder path to place the script in, or null for root
     */
    private void createNewScript(String folderPath) {
        // Check for unsaved changes
        if (changesTracker.isDirty()) {
            int choice = showUnsavedChangesDialog();

            if (choice == JOptionPane.YES_OPTION) {
                saveCurrentScript();
            } else if (choice == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }

        // Check Gateway connection
        if (restClient == null) {
            DarkDialog.showMessage(
                    this,
                    "Please connect to a Gateway first",
                    "Not Connected"
            );
            return;
        }

        // Clear editor for new script
        changesTracker.loadContent("# New Python Script\n\n");
        currentScript = null;
        updateCurrentScriptLabel();
        metadataPanel.clear();
        setStatus("New script - enter details to save", Color.BLUE);

        // Show save dialog immediately so user can enter metadata
        showSaveDialog(folderPath);
    }

    /**
     * Shows context menu for tree items.
     */
    private void showContextMenu(MouseEvent e) {
        TreePath path = scriptTree.getPathForLocation(e.getX(), e.getY());

        if (path == null) {
            return;
        }

        scriptTree.setSelectionPath(path);
        Object node = path.getLastPathComponent();

        if (!(node instanceof ScriptTreeNode)) {
            return;
        }

        ScriptTreeNode scriptNode = (ScriptTreeNode) node;
        JPopupMenu menu = new JPopupMenu();

        if (scriptNode.isScript()) {
            // Script context menu
            JMenuItem loadItem = new JMenuItem("Load");
            loadItem.addActionListener(ev -> loadSelectedScript());
            menu.add(loadItem);

            JMenuItem exportItem = new JMenuItem("Export...");
            exportItem.addActionListener(ev -> exportScript(scriptNode));
            menu.add(exportItem);

            JMenuItem renameItem = new JMenuItem("Rename...");
            renameItem.addActionListener(ev -> renameScript(scriptNode));
            menu.add(renameItem);

            // v2.3.1: Edit Metadata
            JMenuItem editMetadataItem = new JMenuItem("Edit Metadata...");
            editMetadataItem.addActionListener(ev -> editMetadata(scriptNode));
            menu.add(editMetadataItem);

            // v2.0.29: Move to Folder
            JMenuItem moveItem = new JMenuItem("Move to Folder...");
            moveItem.addActionListener(ev -> showMoveToFolderDialog(scriptNode));
            menu.add(moveItem);

            menu.addSeparator();

            JMenuItem deleteItem = new JMenuItem("Delete");
            deleteItem.addActionListener(ev -> deleteScript(scriptNode));
            menu.add(deleteItem);

        } else {
            // Folder context menu
            JMenuItem newScriptItem = new JMenuItem("New Script Here");
            // Get the folder path for this node
            final String folderPath = getFolderPathForNode(scriptNode);
            newScriptItem.addActionListener(ev -> createNewScript(folderPath));
            menu.add(newScriptItem);

            JMenuItem newFolderItem = new JMenuItem("New Subfolder");
            newFolderItem.addActionListener(ev -> createNewFolder());
            menu.add(newFolderItem);

            // Only allow renaming non-root folders
            if (scriptNode != rootNode) {
                menu.addSeparator();

                JMenuItem renameFolderItem = new JMenuItem("Rename...");
                renameFolderItem.addActionListener(ev -> renameFolder(scriptNode));
                menu.add(renameFolderItem);
            }
        }

        // v2.0.19: Apply theme AFTER menu items are added (was called too early before)
        stylePopupMenu(menu);

        menu.show(scriptTree, e.getX(), e.getY());
    }

    /**
     * Exports a script to a .py file.
     */
    private void exportScript(ScriptTreeNode scriptNode) {
        ScriptMetadata metadata = scriptNode.getScriptMetadata();
        importExportManager.exportScript(metadata);
    }

    /**
     * Renames a script.
     */
    private void renameScript(ScriptTreeNode scriptNode) {
        ScriptMetadata metadata = scriptNode.getScriptMetadata();
        String oldName = metadata.getName();

        String newName = DarkDialog.showInput(
                this,
                "Enter new name for script:",
                "Rename Script",
                oldName
        );

        if (newName == null || newName.trim().isEmpty()) {
            return;  // User cancelled or entered empty name
        }

        final String finalNewName = newName.trim();

        // Validate new name
        if (!isValidName(finalNewName)) {
            showInvalidNameError(finalNewName);
            return;
        }

        if (finalNewName.equals(oldName)) {
            return;  // No change
        }

        // Rename by: load script -> delete old -> save with new name
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private SavedScript script;

            @Override
            protected Void doInBackground() throws Exception {
                // Load the script
                script = restClient.loadScript(oldName);

                // Delete old script
                restClient.deleteScript(oldName);

                // Save with new name
                restClient.saveScript(
                        finalNewName,  // New name
                        script.getCode(),
                        script.getDescription(),
                        script.getAuthor(),
                        script.getFolderPath(),
                        script.getVersion()
                );

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Renamed '" + oldName + "' to '" + finalNewName + "'", ModernTheme.SUCCESS);
                    refreshScriptTree();

                    // Update current script metadata if this is the currently loaded script
                    if (currentScript != null && currentScript.getName().equals(oldName)) {
                        currentScript = new ScriptMetadata(
                            currentScript.getId(),
                            finalNewName,
                            currentScript.getDescription(),
                            currentScript.getAuthor(),
                            currentScript.getCreatedDate(),
                            currentScript.getLastModified(),
                            currentScript.getFolderPath(),
                            currentScript.getVersion()
                        );
                    }

                } catch (Exception e) {
                    LOGGER.error("Failed to rename script", e);
                    DarkDialog.showMessage(
                            Python3IDE.this,
                            "Failed to rename script: " + e.getMessage(),
                            "Error"
                    );
                }
            }
        };

        worker.execute();
    }

    /**
     * Edit metadata for a script (v2.3.3 - properly themed dialog).
     * Allows editing: name, description, author, version.
     *
     * @param scriptNode the script node to edit
     */
    private void editMetadata(ScriptTreeNode scriptNode) {
        ScriptMetadata metadata = scriptNode.getScriptMetadata();
        String oldName = metadata.getName();

        // Create custom themed dialog (v2.3.3 - matches Save Script popup)
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Edit Metadata", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Main content panel
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARK : ModernTheme.LIGHT_BACKGROUND);
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Fields panel
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARK : ModernTheme.LIGHT_BACKGROUND);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Name field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        nameLabel.setFont(ModernTheme.FONT_REGULAR);
        fieldsPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JTextField nameField = new JTextField(oldName, 30);
        nameField.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARKER : ModernTheme.LIGHT_BACKGROUND_DARKER);
        nameField.setForeground(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        nameField.setCaretColor(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        nameField.setFont(ModernTheme.FONT_REGULAR);
        nameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(useDarkTheme ? ModernTheme.BORDER_DEFAULT : ModernTheme.LIGHT_BORDER, 1),
            new EmptyBorder(5, 5, 5, 5)
        ));
        fieldsPanel.add(nameField, gbc);

        // Description field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel descLabel = new JLabel("Description:");
        descLabel.setForeground(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        descLabel.setFont(ModernTheme.FONT_REGULAR);
        fieldsPanel.add(descLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JTextArea descArea = new JTextArea(metadata.getDescription() != null ? metadata.getDescription() : "", 4, 30);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARKER : ModernTheme.LIGHT_BACKGROUND_DARKER);
        descArea.setForeground(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        descArea.setCaretColor(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        descArea.setFont(ModernTheme.FONT_REGULAR);
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setPreferredSize(new Dimension(300, 80));
        descScroll.setBorder(BorderFactory.createLineBorder(useDarkTheme ? ModernTheme.BORDER_DEFAULT : ModernTheme.LIGHT_BORDER, 1));
        fieldsPanel.add(descScroll, gbc);

        // Author field
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        JLabel authorLabel = new JLabel("Author:");
        authorLabel.setForeground(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        authorLabel.setFont(ModernTheme.FONT_REGULAR);
        fieldsPanel.add(authorLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JTextField authorField = new JTextField(metadata.getAuthor() != null ? metadata.getAuthor() : "", 30);
        authorField.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARKER : ModernTheme.LIGHT_BACKGROUND_DARKER);
        authorField.setForeground(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        authorField.setCaretColor(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        authorField.setFont(ModernTheme.FONT_REGULAR);
        authorField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(useDarkTheme ? ModernTheme.BORDER_DEFAULT : ModernTheme.LIGHT_BORDER, 1),
            new EmptyBorder(5, 5, 5, 5)
        ));
        fieldsPanel.add(authorField, gbc);

        // Version field
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        JLabel versionLabel = new JLabel("Version:");
        versionLabel.setForeground(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        versionLabel.setFont(ModernTheme.FONT_REGULAR);
        fieldsPanel.add(versionLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JTextField versionField = new JTextField(metadata.getVersion() != null ? metadata.getVersion() : "1.0", 30);
        versionField.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARKER : ModernTheme.LIGHT_BACKGROUND_DARKER);
        versionField.setForeground(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        versionField.setCaretColor(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        versionField.setFont(ModernTheme.FONT_REGULAR);
        versionField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(useDarkTheme ? ModernTheme.BORDER_DEFAULT : ModernTheme.LIGHT_BORDER, 1),
            new EmptyBorder(5, 5, 5, 5)
        ));
        fieldsPanel.add(versionField, gbc);

        contentPanel.add(fieldsPanel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARK : ModernTheme.LIGHT_BACKGROUND);

        final boolean[] okClicked = {false};

        JButton okButton = createThemedDialogButton("OK");
        okButton.addActionListener(e -> {
            okClicked[0] = true;
            dialog.dispose();
        });

        JButton cancelButton = createThemedDialogButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        if (!okClicked[0]) {
            return;  // User cancelled
        }

        // Get values
        String newName = nameField.getText().trim();
        String newDescription = descArea.getText().trim();
        String newAuthor = authorField.getText().trim();
        String newVersion = versionField.getText().trim();

        // Validate new name
        if (newName.isEmpty()) {
            DarkDialog.showMessage(this, "Name cannot be empty", "Error");
            return;
        }

        if (!isValidName(newName)) {
            showInvalidNameError(newName);
            return;
        }

        // Update metadata by: load script -> delete old -> save with new metadata
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private SavedScript script;

            @Override
            protected Void doInBackground() throws Exception {
                // Load the script
                script = restClient.loadScript(oldName);

                // Delete old script if name changed
                if (!newName.equals(oldName)) {
                    restClient.deleteScript(oldName);
                }

                // Save with new metadata
                restClient.saveScript(
                        newName,
                        script.getCode(),
                        newDescription,
                        newAuthor,
                        script.getFolderPath(),
                        newVersion
                );

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Updated metadata for '" + newName + "'", ModernTheme.SUCCESS);
                    refreshScriptTree();

                    // Update current script metadata if this is the currently loaded script
                    if (currentScript != null && currentScript.getName().equals(oldName)) {
                        currentScript = new ScriptMetadata(
                            currentScript.getId(),
                            newName,
                            newDescription,
                            newAuthor,
                            currentScript.getCreatedDate(),
                            currentScript.getLastModified(),
                            currentScript.getFolderPath(),
                            newVersion
                        );

                        // Refresh metadata panel
                        if (metadataPanel != null) {
                            metadataPanel.displayMetadata(currentScript);
                        }
                    }

                } catch (Exception e) {
                    LOGGER.error("Failed to update metadata", e);
                    DarkDialog.showMessage(
                            Python3IDE.this,
                            "Failed to update metadata: " + e.getMessage(),
                            "Error"
                    );
                }
            }
        };

        worker.execute();
    }

    /**
     * Renames a folder.
     */
    private void renameFolder(ScriptTreeNode folderNode) {
        String oldName = folderNode.toString();
        String oldPath = getFolderPathForNode(folderNode);

        String newName = DarkDialog.showInput(
                this,
                "Enter new name for folder:",
                "Rename Folder",
                oldName
        );

        if (newName == null || newName.trim().isEmpty()) {
            return;  // User cancelled or entered empty name
        }

        final String finalNewName = newName.trim();

        // Validate new name
        if (!isValidName(finalNewName)) {
            showInvalidNameError(finalNewName);
            return;
        }

        if (finalNewName.equals(oldName)) {
            return;  // No change
        }

        // Calculate new path
        String parentPath = "";
        if (folderNode.getParent() != rootNode && folderNode.getParent() != null) {
            parentPath = getFolderPathForNode((ScriptTreeNode) folderNode.getParent());
        }
        final String newPath = parentPath.isEmpty() ? finalNewName : parentPath + "/" + finalNewName;

        // Rename by updating all scripts in this folder (and subfolders)
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Get all scripts
                List<ScriptMetadata> allScripts = restClient.listScripts();

                // Find scripts that need to be moved
                for (ScriptMetadata script : allScripts) {
                    String scriptFolder = script.getFolderPath();
                    if (scriptFolder != null) {
                        // Check if script is in this folder or subfolder
                        if (scriptFolder.equals(oldPath) || scriptFolder.startsWith(oldPath + "/")) {
                            // Update the folder path
                            String updatedPath = scriptFolder.equals(oldPath) ?
                                    newPath :
                                    newPath + scriptFolder.substring(oldPath.length());

                            // Load full script and save with new path
                            SavedScript fullScript = restClient.loadScript(script.getName());
                            restClient.saveScript(
                                    fullScript.getName(),
                                    fullScript.getCode(),
                                    fullScript.getDescription(),
                                    fullScript.getAuthor(),
                                    updatedPath,
                                    fullScript.getVersion()
                            );
                        }
                    }
                }

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Renamed folder '" + oldName + "' to '" + finalNewName + "'", ModernTheme.SUCCESS);
                    refreshScriptTree();
                } catch (Exception e) {
                    LOGGER.error("Failed to rename folder", e);
                    DarkDialog.showMessage(
                            Python3IDE.this,
                            "Failed to rename folder: " + e.getMessage(),
                            "Error"
                    );
                }
            }
        };

        worker.execute();
    }

    /**
     * Gets the full folder path for a folder node.
     * v2.15.8: Simplified after removing Recent folder feature.
     */
    private String getFolderPathForNode(ScriptTreeNode folderNode) {
        if (folderNode == rootNode) {
            return "";
        }

        StringBuilder path = new StringBuilder();
        Object[] pathArray = folderNode.getPath();

        // Skip root node (index 0)
        for (int i = 1; i < pathArray.length; i++) {
            String nodeName = pathArray[i].toString();

            if (path.length() > 0) {
                path.append("/");
            }
            path.append(nodeName);
        }

        return path.toString();
    }

    /**
     * Shows dialog to move a script to a different folder (v2.0.29).
     */
    private void showMoveToFolderDialog(ScriptTreeNode scriptNode) {
        ScriptMetadata metadata = scriptNode.getScriptMetadata();
        String scriptName = metadata.getName();
        String currentFolderPath = metadata.getFolderPath() != null ? metadata.getFolderPath() : "";

        // Get all available folders
        java.util.List<String> folders = new java.util.ArrayList<>();
        folders.add("[Root]");  // Root folder option
        collectFolderPaths(rootNode, "", folders);

        if (folders.size() == 1) {
            DarkDialog.showMessage(
                    this,
                    "No other folders available. Create folders first.",
                    "Move to Folder"
            );
            return;
        }

        // Create combo box for folder selection
        JComboBox<String> folderCombo = new JComboBox<>(folders.toArray(new String[0]));
        folderCombo.setFont(ModernTheme.FONT_REGULAR);
        folderCombo.setBackground(ModernTheme.BACKGROUND_DARKER);
        folderCombo.setForeground(ModernTheme.FOREGROUND_PRIMARY);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(ModernTheme.BACKGROUND_DARKER);
        JLabel label = new JLabel("Select destination folder for '" + scriptName + "':");
        label.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        label.setFont(ModernTheme.FONT_REGULAR);
        panel.add(label, BorderLayout.NORTH);
        panel.add(folderCombo, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Move to Folder",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;  // User cancelled
        }

        String selected = (String) folderCombo.getSelectedItem();
        if (selected == null) {
            return;
        }

        String newFolderPath = selected.equals("[Root]") ? "" : selected;

        if (newFolderPath.equals(currentFolderPath)) {
            return;  // No change
        }

        // Move the script
        moveScriptToFolder(scriptName, newFolderPath);
    }

    /**
     * Recursively collects all folder paths from the tree (v2.0.29).
     */
    private void collectFolderPaths(ScriptTreeNode node, String currentPath, java.util.List<String> folders) {
        for (int i = 0; i < node.getChildCount(); i++) {
            ScriptTreeNode child = (ScriptTreeNode) node.getChildAt(i);
            if (!child.isScript()) {
                String childPath = currentPath.isEmpty() ? child.toString() : currentPath + "/" + child.toString();
                folders.add(childPath);
                collectFolderPaths(child, childPath, folders);
            }
        }
    }

    /**
     * Moves a script to a new folder (v2.0.29).
     */
    private void moveScriptToFolder(String scriptName, String newFolderPath) {
        if (restClient == null) {
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private SavedScript script;

            @Override
            protected Void doInBackground() throws Exception {
                // Load the script
                script = restClient.loadScript(scriptName);

                // Save with new folder path
                restClient.saveScript(
                        script.getName(),
                        script.getCode(),
                        script.getDescription(),
                        script.getAuthor(),
                        newFolderPath,  // New folder path
                        script.getVersion()
                );

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Moved '" + scriptName + "' to " +
                            (newFolderPath.isEmpty() ? "root" : newFolderPath), ModernTheme.SUCCESS);
                    refreshScriptTree();
                } catch (Exception e) {
                    LOGGER.error("Failed to move script", e);
                    DarkDialog.showMessage(
                            Python3IDE.this,
                            "Failed to move script: " + e.getMessage(),
                            "Error"
                    );
                }
            }
        };

        worker.execute();
    }

    /**
     * Deletes a script.
     */
    private void deleteScript(ScriptTreeNode scriptNode) {
        ScriptMetadata metadata = scriptNode.getScriptMetadata();

        boolean confirm = DarkDialog.showConfirm(
                this,
                "Are you sure you want to delete '" + metadata.getName() + "'?",
                "Confirm Delete"
        );

        if (!confirm) {
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                restClient.deleteScript(metadata.getName());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Deleted: " + metadata.getName(), ModernTheme.SUCCESS);

                    // v2.8.0: Remove from recent scripts - REMOVED in v2.15.8 per user request
                    // recentScriptsManager.removeRecent(metadata.getName());

                    refreshScriptTree();
                } catch (Exception e) {
                    LOGGER.error("Failed to delete script", e);
                    DarkDialog.showMessage(
                            Python3IDE.this,
                            "Failed to delete script: " + e.getMessage(),
                            "Error"
                    );
                }
            }
        };

        worker.execute();
    }

    /**
     * Imports a .py file into the script library.
     */
    private void importScript() {
        importExportManager.importScript();
    }

    /**
     * Exports the current code in the editor to a .py file.
     */
    private void exportCurrentScript() {
        importExportManager.exportCurrentScript();
    }

    /**
     * Shows unsaved changes dialog.
     */
    private int showUnsavedChangesDialog() {
        return JOptionPane.showConfirmDialog(
                this,
                "You have unsaved changes. Do you want to save them?",
                "Unsaved Changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
    }

    /**
     * Called when dirty state changes.
     */
    private void onDirtyStateChanged(boolean isDirty) {
        String title = "Python 3 IDE";

        if (isDirty) {
            title += " *";  // Indicate unsaved changes
        }

        // Update current script label to show unsaved changes indicator
        updateCurrentScriptLabel();

        // Could update window title or status here
        setStatus(isDirty ? "Unsaved changes" : "All changes saved",
                  isDirty ? Color.ORANGE : ModernTheme.SUCCESS);
    }

    /**
     * Updates the current script label to show the selected script name and folder path.
     * v2.15.3: Enhanced visual indication with icon prefix
     */
    private void updateCurrentScriptLabel() {
        if (currentScript == null || currentScript.getName() == null || currentScript.getName().isEmpty()) {
            currentScriptLabel.setText("  No script selected");
            currentScriptLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        } else {
            StringBuilder labelText = new StringBuilder();

            // Add file icon prefix for better visibility
            labelText.append("\u2022 ");  // Bullet point or use "\uD83D\uDCC4 " for document icon

            // Add folder path if exists
            if (currentScript.getFolderPath() != null && !currentScript.getFolderPath().isEmpty()) {
                labelText.append(currentScript.getFolderPath()).append(" / ");
            }

            // Add script name
            labelText.append(currentScript.getName());

            // Add unsaved changes indicator
            if (changesTracker.isDirty()) {
                labelText.append(" *");
            }

            currentScriptLabel.setText(labelText.toString());
            currentScriptLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        }
    }

    // Theme Management

    /**
     * Public method to apply theme by display name (v2.11.4: Called from SettingsDialog)
     * @param displayName User-friendly theme name like "Dark", "VS Code Dark+", etc.
     */
    public void applyThemeByName(String displayName) {
        String themeKey = mapThemeNameToKey(displayName);
        applyTheme(themeKey);
    }

    /**
     * Maps user-friendly theme names to internal theme keys.
     */
    private String mapThemeNameToKey(String displayName) {
        switch (displayName) {
            case "Dark":
                return "dark";
            case "VS Code Dark+":
                return "vs";
            case "Monokai":
                return "monokai";
            case "Dracula":
                return "druid";
            case "Default (Light)":
                return "default";
            case "IntelliJ Light":
                return "idea";
            case "Eclipse":
                return "eclipse";
            default:
                return "dark";
        }
    }

    /**
     * Applies a theme to the editor and entire IDE.
     */
    private void applyTheme(String themeName) {
        try {
            Theme theme;
            boolean isDarkTheme = false;

            switch (themeName.toLowerCase()) {
                case "dark":
                    theme = Theme.load(getClass().getResourceAsStream(
                            "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
                    isDarkTheme = true;
                    break;
                case "monokai":
                    theme = Theme.load(getClass().getResourceAsStream(
                            "/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"));
                    isDarkTheme = true;
                    break;
                case "eclipse":
                    theme = Theme.load(getClass().getResourceAsStream(
                            "/org/fife/ui/rsyntaxtextarea/themes/eclipse.xml"));
                    isDarkTheme = false;
                    break;
                case "idea":
                    theme = Theme.load(getClass().getResourceAsStream(
                            "/org/fife/ui/rsyntaxtextarea/themes/idea.xml"));
                    isDarkTheme = false;
                    break;
                case "vs":
                    theme = Theme.load(getClass().getResourceAsStream(
                            "/org/fife/ui/rsyntaxtextarea/themes/vs.xml"));
                    isDarkTheme = true;
                    break;
                case "druid":  // Dracula-like theme
                    theme = Theme.load(getClass().getResourceAsStream(
                            "/org/fife/ui/rsyntaxtextarea/themes/druid.xml"));
                    isDarkTheme = true;
                    break;
                default:  // "default" or "light"
                    theme = Theme.load(getClass().getResourceAsStream(
                            "/org/fife/ui/rsyntaxtextarea/themes/default.xml"));
                    isDarkTheme = false;
                    break;
            }

            // Apply theme to code editor
            theme.apply(codeEditor);

            // Store theme state for popup menu styling (v2.0.12)
            this.useDarkTheme = isDarkTheme;

            // Apply theme colors to output and error areas
            if (isDarkTheme) {
                // v2.0.17: Removed applyDarkScrollbarTheme() - used global UIManager.put()

                // Set DarkDialog theme (v2.0.12)
                DarkDialog.setDarkTheme(true);

                // Output/Error areas
                outputArea.setBackground(ModernTheme.BACKGROUND_DARKER);
                outputArea.setForeground(ModernTheme.FOREGROUND_PRIMARY);
                outputArea.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);

                errorArea.setBackground(ModernTheme.BACKGROUND_DARKER);
                errorArea.setForeground(ModernTheme.ERROR);
                errorArea.setCaretColor(ModernTheme.ERROR);

                // Tree
                scriptTree.setBackground(ModernTheme.TREE_BACKGROUND);
                scriptTree.setForeground(ModernTheme.FOREGROUND_PRIMARY);

                // Update UI components for dark theme (v2.0.14, v2.11.4: theme selector moved to settings)
                gatewayUrlField.setBackground(ModernTheme.BACKGROUND_DARKER);
                gatewayUrlField.setForeground(ModernTheme.FOREGROUND_PRIMARY);
                gatewayUrlField.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);

                currentScriptLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);

                // Update ModernButton instances for dark theme
                updateButtonTheme(connectButton, ModernTheme.ACCENT_PRIMARY, ModernTheme.ACCENT_HOVER, ModernTheme.ACCENT_ACTIVE);
                updateButtonTheme(executeButton, ModernTheme.ACCENT_PRIMARY, ModernTheme.ACCENT_HOVER, ModernTheme.ACCENT_ACTIVE);
                updateButtonTheme(saveButton, ModernTheme.SUCCESS, ModernTheme.lighten(ModernTheme.SUCCESS, 0.1), ModernTheme.darken(ModernTheme.SUCCESS, 0.1));
                updateButtonTheme(saveAsButton, ModernTheme.BUTTON_BACKGROUND, ModernTheme.BUTTON_HOVER, ModernTheme.BUTTON_ACTIVE);
                updateButtonTheme(importButton, ModernTheme.BUTTON_BACKGROUND, ModernTheme.BUTTON_HOVER, ModernTheme.BUTTON_ACTIVE);
                updateButtonTheme(exportButton, ModernTheme.BUTTON_BACKGROUND, ModernTheme.BUTTON_HOVER, ModernTheme.BUTTON_ACTIVE);
                updateButtonTheme(newFolderBtn, ModernTheme.BUTTON_BACKGROUND, ModernTheme.BUTTON_HOVER, ModernTheme.BUTTON_ACTIVE);
                updateButtonTheme(newScriptBtn, ModernTheme.BUTTON_BACKGROUND, ModernTheme.BUTTON_HOVER, ModernTheme.BUTTON_ACTIVE);
                updateButtonTheme(refreshBtn, ModernTheme.BUTTON_BACKGROUND, ModernTheme.BUTTON_HOVER, ModernTheme.BUTTON_ACTIVE);

                // v2.5.17: outputTabs removed, replaced with custom tab solution

                // Update metadata panel theme
                metadataPanel.applyTheme(true);

                // Update all TitledBorder components for dark theme
                updateTitledBorders(this, true);

                // Panels
                updateComponent(this, ModernTheme.BACKGROUND_DARK);

                // v2.0.17: Removed applyDarkDialogTheme() - used global UIManager.put()
            } else {
                // v2.0.17: Removed applyLightScrollbarTheme() - used global UIManager.put()

                // Set DarkDialog theme (v2.0.12)
                DarkDialog.setDarkTheme(false);

                // Output/Error areas
                outputArea.setBackground(ModernTheme.LIGHT_BACKGROUND);
                outputArea.setForeground(Color.BLACK);
                outputArea.setCaretColor(Color.BLACK);

                errorArea.setBackground(ModernTheme.LIGHT_BACKGROUND);
                errorArea.setForeground(ModernTheme.ERROR);
                errorArea.setCaretColor(ModernTheme.ERROR);

                // Tree
                scriptTree.setBackground(ModernTheme.LIGHT_TREE_BG);
                scriptTree.setForeground(Color.BLACK);

                // Update UI components for light theme (v2.0.14)
                gatewayUrlField.setBackground(ModernTheme.LIGHT_BACKGROUND);
                gatewayUrlField.setForeground(Color.BLACK);
                gatewayUrlField.setCaretColor(Color.BLACK);

                currentScriptLabel.setForeground(ModernTheme.FOREGROUND_MUTED);  // Light gray for secondary text

                // Update ModernButton instances for light theme (lighter, pastel colors)
                Color lightPrimary = new Color(33, 118, 255);  // Lighter blue
                Color lightPrimaryHover = new Color(23, 108, 245);
                Color lightPrimaryActive = new Color(13, 98, 235);
                Color lightSuccess = new Color(40, 167, 69);  // Lighter green
                Color lightSuccessHover = new Color(30, 157, 59);
                Color lightSuccessActive = new Color(20, 147, 49);
                Color lightDefault = ModernTheme.LIGHT_BACKGROUND_LIGHT;  // Light gray
                Color lightDefaultHover = ModernTheme.LIGHT_BUTTON_BG;
                Color lightDefaultActive = ModernTheme.LIGHT_BUTTON_HOVER;

                updateButtonTheme(connectButton, lightPrimary, lightPrimaryHover, lightPrimaryActive);
                updateButtonTheme(executeButton, lightPrimary, lightPrimaryHover, lightPrimaryActive);
                updateButtonTheme(saveButton, lightSuccess, lightSuccessHover, lightSuccessActive);
                updateButtonTheme(saveAsButton, lightDefault, lightDefaultHover, lightDefaultActive);
                updateButtonTheme(importButton, lightDefault, lightDefaultHover, lightDefaultActive);
                updateButtonTheme(exportButton, lightDefault, lightDefaultHover, lightDefaultActive);
                updateButtonTheme(newFolderBtn, lightDefault, lightDefaultHover, lightDefaultActive);
                updateButtonTheme(newScriptBtn, lightDefault, lightDefaultHover, lightDefaultActive);
                updateButtonTheme(refreshBtn, lightDefault, lightDefaultHover, lightDefaultActive);

                // v2.5.17: outputTabs removed, replaced with custom tab solution

                // Update metadata panel theme
                metadataPanel.applyTheme(false);

                // Update all TitledBorder components for light theme
                updateTitledBorders(this, false);

                // Panels
                updateComponent(this, ModernTheme.LIGHT_BACKGROUND);

                // v2.0.17: Removed applyLightDialogTheme() - used global UIManager.put()
            }

            // Force repaint of all components
            SwingUtilities.updateComponentTreeUI(this);

            // Force update of all scrollbar UI delegates
            updateScrollPaneTheme(this, isDarkTheme);

            // Force update of all JSplitPane dividers (Issue 8 - v1.17.1)
            updateSplitPaneDividers(this, isDarkTheme);

            // Update terminal theme (v2.8.1)
            if (terminalPanel != null) {
                terminalPanel.setTheme(isDarkTheme);
            }

            currentTheme = themeName;

            // Save preference
            Preferences prefs = Preferences.userNodeForPackage(Python3IDE.class);
            prefs.put(PREF_THEME, themeName);

            setStatus("Theme changed: " + themeName, ModernTheme.SUCCESS);
            LOGGER.info("Applied theme: {}", themeName);

        } catch (IOException e) {
            LOGGER.error("Failed to apply theme: {}", themeName, e);
            setStatus("Failed to apply theme: " + themeName, Color.RED);
        }
    }

    // v2.0.17: REMOVED applyDarkDialogTheme(), applyLightDialogTheme(),
    // applyDarkScrollbarTheme(), applyLightScrollbarTheme() methods.
    // These methods used UIManager.put() which sets GLOBAL Swing defaults
    // affecting the entire Ignition Designer, not just our IDE.
    // Solution: Use DarkDialog (already implemented v2.0.12) and direct component styling only.

    /**
     * Styles a popup menu to match the current theme.
     *
     * @param menu the popup menu to style
     */
    private void stylePopupMenu(JPopupMenu menu) {
        // Determine current theme
        boolean isDark = useDarkTheme;

        if (isDark) {
            menu.setBackground(ModernTheme.BACKGROUND_DARK);
            menu.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            menu.setBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT, 1));

            // Style each menu item (v2.11.2: Added UI properties for better hover contrast)
            for (Component comp : menu.getComponents()) {
                if (comp instanceof JMenuItem) {
                    JMenuItem item = (JMenuItem) comp;
                    item.setBackground(ModernTheme.BACKGROUND_DARK);
                    item.setForeground(ModernTheme.FOREGROUND_PRIMARY);
                    item.setFont(ModernTheme.FONT_REGULAR);

                    // Set selection (hover) colors using UI properties
                    item.putClientProperty("MenuItem.selectionBackground", ModernTheme.BUTTON_HOVER);
                    item.putClientProperty("MenuItem.selectionForeground", Color.WHITE);
                }
            }
        } else {
            menu.setBackground(ModernTheme.LIGHT_BACKGROUND);
            menu.setForeground(Color.BLACK);
            menu.setBorder(BorderFactory.createLineBorder(ModernTheme.LIGHT_BORDER, 1));

            // Style each menu item (v2.11.2: Added UI properties for better hover contrast)
            for (Component comp : menu.getComponents()) {
                if (comp instanceof JMenuItem) {
                    JMenuItem item = (JMenuItem) comp;
                    item.setBackground(ModernTheme.LIGHT_BACKGROUND);
                    item.setForeground(Color.BLACK);
                    item.setFont(ModernTheme.FONT_REGULAR);

                    // Set selection (hover) colors using UI properties
                    item.putClientProperty("MenuItem.selectionBackground", ModernTheme.LIGHT_SELECTION);
                    item.putClientProperty("MenuItem.selectionForeground", Color.BLACK);
                }
            }
        }
    }

    // v2.0.17: REMOVED applyFileChooserTheme() and applyDialogDarkTheme() methods.
    // These used UIManager.put() affecting GLOBAL Swing UI (entire Ignition Designer).
    // JFileChooser will use system defaults - acceptable tradeoff for isolation.

    /**
     * Recursively applies dark theme colors to all components in a container.
     * This ensures dialog panels, labels, and text fields have proper dark styling.
     *
     * @param container the container to apply styling to
     */
    private static void setComponentsDark(Container container) {
        container.setBackground(ModernTheme.PANEL_BACKGROUND);
        Component[] components = container.getComponents();
        for (Component component : components) {
            if (component instanceof JTextField || component instanceof JTextArea) {
                component.setBackground(ModernTheme.EDITOR_BACKGROUND);
                component.setForeground(ModernTheme.FOREGROUND_PRIMARY);
                if (component instanceof JTextField) {
                    ((JTextField) component).setCaretColor(ModernTheme.FOREGROUND_PRIMARY);
                }
            } else if (component instanceof JLabel) {
                component.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            } else if (component instanceof JPanel) {
                component.setBackground(ModernTheme.PANEL_BACKGROUND);
            } else if (component instanceof JButton) {
                component.setBackground(ModernTheme.BUTTON_BACKGROUND);
                component.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            }
            if (component instanceof Container) {
                setComponentsDark((Container) component);
            }
        }
    }

    /**
     * Recursively updates component backgrounds.
     * Traverses the component tree and applies theme colors to all panels.
     *
     * @param comp the component to update
     * @param background the background color to apply
     */
    private void updateComponent(Component comp, Color background) {
        if (comp instanceof JPanel) {
            comp.setBackground(background);
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                updateComponent(child, background);
            }
        }
    }

    /**
     * Updates a ModernButton's color scheme.
     * Helper method to update button colors when theme changes.
     *
     * @param button the button to update (must be a ModernButton)
     * @param normal the normal background color
     * @param hover the hover background color
     * @param pressed the pressed background color
     */
    private void updateButtonTheme(JButton button, Color normal, Color hover, Color pressed) {
        if (button instanceof ModernButton) {
            ModernButton modernButton = (ModernButton) button;
            modernButton.setNormalBackground(normal);
            modernButton.setHoverBackground(hover);
            modernButton.setPressedBackground(pressed);
            modernButton.repaint();
        }
    }

    /**
     * Creates a themed button for dialogs (v2.3.3).
     * Matches the style of DarkDialog buttons.
     *
     * @param text button text
     * @return themed button
     */
    private JButton createThemedDialogButton(String text) {
        Color buttonBg = useDarkTheme ? ModernTheme.BUTTON_BACKGROUND : ModernTheme.LIGHT_BACKGROUND_LIGHT;
        Color foreground = useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK;
        Color borderColor = useDarkTheme ? ModernTheme.BUTTON_BACKGROUND : ModernTheme.LIGHT_BORDER;

        JButton button = new JButton(text);
        button.setBackground(buttonBg);
        button.setForeground(foreground);
        button.setFont(ModernTheme.FONT_REGULAR);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            new EmptyBorder(5, 15, 5, 15)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover effect
        Color hoverBg = useDarkTheme ? ModernTheme.BUTTON_HOVER : ModernTheme.LIGHT_BUTTON_HOVER;
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverBg);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(buttonBg);
            }
        });

        return button;
    }

    /**
     * Recursively updates all TitledBorder components to match the current theme.
     * This updates the border color and title text color for all panels with titled borders.
     *
     * @param comp the component to traverse
     * @param isDarkTheme true if dark theme, false if light theme
     */
    private void updateTitledBorders(Component comp, boolean isDarkTheme) {
        if (comp instanceof JComponent) {
            JComponent jcomp = (JComponent) comp;
            javax.swing.border.Border border = jcomp.getBorder();

            if (border instanceof javax.swing.border.CompoundBorder) {
                javax.swing.border.CompoundBorder compoundBorder = (javax.swing.border.CompoundBorder) border;
                javax.swing.border.Border outerBorder = compoundBorder.getOutsideBorder();

                if (outerBorder instanceof TitledBorder) {
                    TitledBorder titledBorder = (TitledBorder) outerBorder;

                    // Update title text color and border line color
                    if (isDarkTheme) {
                        titledBorder.setTitleColor(ModernTheme.FOREGROUND_PRIMARY);
                        titledBorder.setBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT));
                    } else {
                        titledBorder.setTitleColor(Color.BLACK);
                        titledBorder.setBorder(BorderFactory.createLineBorder(ModernTheme.LIGHT_BORDER));
                    }
                }
            } else if (border instanceof TitledBorder) {
                TitledBorder titledBorder = (TitledBorder) border;

                // Update title text color and border line color
                if (isDarkTheme) {
                    titledBorder.setTitleColor(ModernTheme.FOREGROUND_PRIMARY);
                    titledBorder.setBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT));
                } else {
                    titledBorder.setTitleColor(Color.BLACK);
                    titledBorder.setBorder(BorderFactory.createLineBorder(ModernTheme.LIGHT_BORDER));
                }
            }
        }

        // Recursively traverse children
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                updateTitledBorders(child, isDarkTheme);
            }
        }
    }

    /**
     * Recursively updates all scrollbar UI delegates to match the current theme.
     * Forces scrollbars to pick up the theme changes by updating their UI components.
     *
     * @param comp the component to traverse
     * @param isDarkTheme true if dark theme, false if light theme
     */
    private void updateScrollPaneTheme(Component comp, boolean isDarkTheme) {
        if (comp instanceof JScrollPane) {
            JScrollPane scrollPane = (JScrollPane) comp;

            // Force update scrollbar UI delegates
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            JScrollBar horizontalBar = scrollPane.getHorizontalScrollBar();

            if (verticalBar != null) {
                verticalBar.updateUI();
                if (isDarkTheme) {
                    verticalBar.setBackground(ModernTheme.BACKGROUND_DARK);
                    verticalBar.setForeground(ModernTheme.FOREGROUND_PRIMARY);
                } else {
                    verticalBar.setBackground(ModernTheme.LIGHT_BACKGROUND);
                    verticalBar.setForeground(Color.BLACK);
                }
            }

            if (horizontalBar != null) {
                horizontalBar.updateUI();
                if (isDarkTheme) {
                    horizontalBar.setBackground(ModernTheme.BACKGROUND_DARK);
                    horizontalBar.setForeground(ModernTheme.FOREGROUND_PRIMARY);
                } else {
                    horizontalBar.setBackground(ModernTheme.LIGHT_BACKGROUND);
                    horizontalBar.setForeground(Color.BLACK);
                }
            }

            // Update viewport background
            scrollPane.getViewport().setBackground(isDarkTheme ? ModernTheme.BACKGROUND_DARK : ModernTheme.LIGHT_BACKGROUND);
            scrollPane.setBackground(isDarkTheme ? ModernTheme.BACKGROUND_DARK : ModernTheme.LIGHT_BACKGROUND);
        }

        // Recursively traverse child components
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                updateScrollPaneTheme(child, isDarkTheme);
            }
        }
    }

    /**
     * Recursively updates all JSplitPane dividers to match the current theme.
     * Uses ThemedSplitPaneUI for direct paint control (v2.3.3).
     *
     * Previous approaches using setBackground() failed - this uses custom paint instead.
     *
     * @param comp the component to traverse
     * @param isDarkTheme true if dark theme, false if light theme
     *
     * v1.17.1: Fix for Issue 8 - ensure dividers match theme
     * v2.3.3: Replaced background approach with custom UI paint approach
     */
    private void updateSplitPaneDividers(Component comp, boolean isDarkTheme) {
        if (comp instanceof JSplitPane) {
            JSplitPane splitPane = (JSplitPane) comp;

            // Set custom UI with direct paint control (v2.3.3/v2.5.7)
            // v2.5.7: Changed from BACKGROUND_DARKER to BORDER_DEFAULT for subtle grey dividers
            Color dividerColor = isDarkTheme ? ModernTheme.BORDER_DEFAULT : ModernTheme.LIGHT_BORDER;
            splitPane.setUI(new ThemedSplitPaneUI(dividerColor));
            splitPane.setBorder(null);
            splitPane.setDividerSize(4);  // Maintain consistent size
        }

        // Recursively traverse child components
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                updateSplitPaneDividers(child, isDarkTheme);
            }
        }
    }

    // Find/Replace Management (delegated to SearchManager v2.8.0)

    /**
     * Shows the Find dialog.
     */
    private void showFindDialog() {
        searchManager.showFindDialog();
    }

    /**
     * Shows the Replace dialog.
     */
    private void showReplaceDialog() {
        searchManager.showReplaceDialog();
    }

    /**
     * Shows the advanced Find/Replace dialog (v2.1.0).
     * This dialog includes regex support, whole word matching, and search history.
     */
    private void showAdvancedFindReplaceDialog() {
        searchManager.showAdvancedFindReplaceDialog();
    }

    /**
     * Shows the Command Palette (v2.8.0).
     * Keyboard-driven command access like VS Code (Ctrl+Shift+P).
     */
    private void showCommandPalette() {
        commandPaletteManager.showCommandPalette();
    }

    /**
     * Toggles the sidebar visibility (v2.8.0).
     * Keyboard shortcut: Ctrl+B
     */
    private void toggleSidebar() {
        if (sidebarCollapsed) {
            // Expand sidebar - restore previous divider location
            mainSplit.setDividerLocation(sidebarDividerLocation);
            sidebarCollapsed = false;
            statusBar.setStatus("Sidebar expanded", ModernStatusBar.MessageType.INFO);
        } else {
            // Collapse sidebar - remember current location and collapse to 0
            sidebarDividerLocation = mainSplit.getDividerLocation();
            mainSplit.setDividerLocation(0);
            sidebarCollapsed = true;
            statusBar.setStatus("Sidebar collapsed (Ctrl+B to toggle)", ModernStatusBar.MessageType.INFO);
        }
    }


    /**
     * Search listener implementation for Find/Replace dialogs.
     */
    private class SearchListenerImpl implements SearchListener {
        @Override
        public void searchEvent(org.fife.rsta.ui.search.SearchEvent e) {
            org.fife.rsta.ui.search.SearchEvent.Type type = e.getType();
            SearchContext context = e.getSearchContext();

            switch (type) {
                case FIND:
                    org.fife.ui.rtextarea.SearchResult result = SearchEngine.find(codeEditor, context);
                    if (!result.wasFound()) {
                        setStatus("Text not found: " + context.getSearchFor(), Color.ORANGE);
                    }
                    break;

                case REPLACE:
                    org.fife.ui.rtextarea.SearchResult replaceResult = SearchEngine.replace(codeEditor, context);
                    if (!replaceResult.wasFound()) {
                        setStatus("Text not found: " + context.getSearchFor(), Color.ORANGE);
                    }
                    break;

                case REPLACE_ALL:
                    org.fife.ui.rtextarea.SearchResult replaceAllResult = SearchEngine.replaceAll(codeEditor, context);
                    setStatus("Replaced " + replaceAllResult.getCount() + " occurrences", ModernTheme.SUCCESS);
                    break;

                case MARK_ALL:
                    org.fife.ui.rtextarea.SearchResult markResult = SearchEngine.markAll(codeEditor, context);
                    if (!markResult.wasFound()) {
                        setStatus("Text not found: " + context.getSearchFor(), Color.ORANGE);
                    }
                    break;

                default:
                    // Unknown search event type - no action needed
                    break;
            }
        }

        @Override
        public String getSelectedText() {
            return codeEditor.getSelectedText();
        }
    }

    /**
     * Changes font size.
     */
    private void changeFontSize(int delta) {
        setFontSize(fontSize + delta);
    }

    /**
     * Sets font size.
     */
    private void setFontSize(int newSize) {
        if (newSize < 8 || newSize > 24) {
            return;  // Reasonable bounds
        }

        fontSize = newSize;
        codeEditor.setFont(ModernTheme.FONT_CODE.deriveFont((float) fontSize));

        // Save preference
        Preferences prefs = Preferences.userNodeForPackage(Python3IDE.class);
        prefs.putInt(PREF_FONT_SIZE, fontSize);

        LOGGER.info("Font size: {}", fontSize);
    }

    /**
     * Converts SavedScript to ScriptMetadata.
     * v2.15.8: Simplified after removing Recent folder feature.
     */
    private ScriptMetadata convertToMetadata(SavedScript script) {
        return new ScriptMetadata(
            script.getId(),
            script.getName(),
            script.getDescription(),
            script.getAuthor(),
            script.getCreatedDate(),
            script.getLastModified(),
            script.getFolderPath(),
            script.getVersion()
        );
    }

    // Public accessor methods (for external access if needed)

    public RSyntaxTextArea getCodeEditor() {
        return codeEditor;
    }

    public JTextArea getOutputArea() {
        return outputArea;
    }

    public JTextArea getErrorArea() {
        return errorArea;
    }

    /**
     * Applies completely transparent scrollbar with small grey rounded thumb only.
     *
     * v2.5.3: User-requested ultra-minimal scrollbars
     * - Completely transparent track (no visible background)
     * - No arrow buttons
     * - Small grey rounded slider only
     */
    private void applyTransparentScrollBar(JScrollBar scrollBar) {
        if (scrollBar == null) {
            return;
        }

        // Make scrollbar background completely transparent
        scrollBar.setOpaque(false);
        scrollBar.setBackground(new Color(0, 0, 0, 0));  // Fully transparent
        scrollBar.setUnitIncrement(16);  // Smooth scrolling

        // Custom UI: transparent everything except small grey thumb
        scrollBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected JButton createDecreaseButton(int orientation) {
                // No arrow buttons - return invisible button
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                // No arrow buttons - return invisible button
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }

            @Override
            protected void configureScrollBarColors() {
                // Make track completely transparent
                this.trackColor = new Color(0, 0, 0, 0);
                this.thumbColor = new Color(120, 120, 120);  // Small grey thumb
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                // Completely transparent track - paint nothing
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                    return;
                }

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                   RenderingHints.VALUE_ANTIALIAS_ON);

                // Small grey rounded thumb
                g2.setColor(new Color(120, 120, 120));
                g2.fillRoundRect(thumbBounds.x, thumbBounds.y,
                               thumbBounds.width, thumbBounds.height, 6, 6);

                g2.dispose();
            }
        });
    }

    /**
     * Shows the information dialog with comprehensive user guide.
     *
     * v2.5.1: Added to provide in-app help for users
     */
    private void showInformationDialog() {
        // Update theme in dialog before showing
        InformationDialog.setDarkTheme(useDarkTheme);
        InformationDialog.show(this);
    }

    /**
     * Opens the Info dialog showing module and Python version information.
     * v2.7.0: New modern UI dialog
     */
    private void openInfoDialog() {
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        InfoDialog dialog = new InfoDialog(parentFrame, this);
        dialog.setVisible(true);
    }

    /**
     * Opens the Version Manager dialog for Python version installation.
     * v3.1.0: Install/uninstall Python versions from the module
     */
    private void openVersionManagerDialog() {
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        VersionManagerDialog dialog = new VersionManagerDialog(parentFrame, restClient);
        dialog.setVisible(true);

        // Refresh the version selector after the dialog closes
        refreshAvailableVersions();
    }

    /**
     * Opens the Packages dialog for Python package management.
     * v2.7.0: New modern UI dialog
     */
    private void openPackagesDialog() {
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        PackagesDialog dialog = new PackagesDialog(parentFrame, this);
        dialog.setVisible(true);
    }

    /**
     * Opens the Settings dialog for IDE configuration.
     * v2.7.0: New modern UI dialog
     */
    private void openSettingsDialog() {
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        SettingsDialog dialog = new SettingsDialog(parentFrame, this);
        dialog.setVisible(true);

        // If user clicked "Connect to Gateway" in settings, connect now
        if (dialog.isConnectRequested()) {
            connectToGateway();
        }
    }

    // ============================================================================
    // Settings Dialog Support Methods (v2.7.0)
    // ============================================================================

    /**
     * Auto-detects Gateway URL from system properties, environment variables, or uses default.
     * Note: The Settings dialog allows manual override for non-standard Gateway ports/addresses.
     *
     * @return Detected Gateway URL
     */
    private String detectGatewayUrl() {
        try {
            // Priority 1: Try system property (allows manual override)
            // Set via: -Dignition.python3.gateway.url=http://localhost:9088
            String url = System.getProperty("ignition.python3.gateway.url");

            // Priority 2: Try environment variable
            if (url == null || url.trim().isEmpty()) {
                url = System.getenv("IGNITION_GATEWAY_URL");
            }

            // Priority 3: Default to localhost:8088 (standard Ignition port)
            // Note: The Designer is already connected to this gateway when running from the Designer
            if (url == null || url.trim().isEmpty()) {
                url = "http://localhost:8088";
            } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
                // If URL doesn't have protocol, add http://
                url = "http://" + url;
            }

            // Remove trailing slash if present
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }

            LOGGER.info("Gateway URL detected/defaulted to: {}", url);
            return url;

        } catch (Exception e) {
            LOGGER.error("Failed to detect Gateway URL, using default", e);
            return "http://localhost:8088";  // Fallback default
        }
    }

    /**
     * Gets the auto-detected Gateway URL.
     *
     * @return Auto-detected Gateway URL
     */
    public String getDetectedGatewayUrl() {
        return detectedGatewayUrl;
    }

    /**
     * Gets the effective Gateway URL (either override or detected).
     *
     * @return Effective Gateway URL currently in use
     */
    public String getEffectiveGatewayUrl() {
        return effectiveGatewayUrl;
    }

    /**
     * Gets the REST client for making Gateway API calls.
     *
     * @return REST client instance, or null if not connected
     */
    public Python3RestClient getRestClient() {
        return restClient;
    }

    /**
     * Reloads settings from preferences after Settings dialog changes.
     * Updates the gateway URL field and reconnects if necessary.
     */
    public void reloadSettingsFromPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(Python3IDE.class);

        // Reload gateway override
        String gatewayOverride = prefs.get(PREF_GATEWAY_OVERRIDE, "");
        if (gatewayOverride != null && !gatewayOverride.trim().isEmpty()) {
            effectiveGatewayUrl = gatewayOverride.trim();
            LOGGER.info("Reloaded Gateway URL override: {}", effectiveGatewayUrl);
        } else {
            effectiveGatewayUrl = detectedGatewayUrl;
            LOGGER.info("Reloaded auto-detected Gateway URL: {}", effectiveGatewayUrl);
        }

        // Update gateway URL field
        gatewayUrlField.setText(effectiveGatewayUrl);

        // Reload theme (in case it changed in Settings - future feature)
        String newTheme = prefs.get(PREF_THEME, "dark");
        if (!newTheme.equals(currentTheme)) {
            currentTheme = newTheme;
            applyTheme(currentTheme);
        }

        // Reload font size (in case it changed in Settings - future feature)
        int newFontSize = prefs.getInt(PREF_FONT_SIZE, 12);
        if (newFontSize != fontSize) {
            fontSize = newFontSize;
            codeEditor.setFont(ModernTheme.FONT_CODE.deriveFont((float) fontSize));
        }

        LOGGER.info("Settings reloaded from preferences");
    }
}
