package com.inductiveautomation.ignition.examples.python3.designer;

import com.inductiveautomation.ignition.designer.model.DesignerContext;
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
import org.fife.ui.autocomplete.CompletionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.KeyEvent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.*;
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

    private final DesignerContext context;
    private Python3RestClient restClient;
    private PythonSyntaxChecker syntaxChecker;
    private AutoCompletion autoCompletion;
    private Python3CompletionProvider completionProvider;  // v2.4.0: Track for status updates

    // v2.5.8: Interactive shell session tracking
    private String interactiveShellSessionId = null;
    private StringBuilder terminalHistory = new StringBuilder();

    // UI Components
    private JTextField gatewayUrlField;
    private JButton connectButton;
    private JComboBox<String> themeSelector;
    private RSyntaxTextArea codeEditor;
    private JLabel currentScriptLabel;
    private JTextArea outputArea;
    private JTextArea errorArea;
    private ModernStatusBar statusBar;
    private JButton executeButton;
    private JButton clearButton;
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

    // Theme and Settings
    private String currentTheme;
    private int fontSize;
    private boolean useDarkTheme = true;  // Track current theme for popup menu styling (v2.0.12)

    // Find/Replace Dialogs
    private FindDialog findDialog;
    private ReplaceDialog replaceDialog;
    private FindReplaceDialog advancedFindReplaceDialog;

    // Unsaved Changes Tracking
    private UnsavedChangesTracker changesTracker;
    private ScriptMetadata currentScript;

    private SwingWorker<ExecutionResult, Void> currentWorker;  // v2.5.8: Changed from Python3ExecutionWorker to support both types

    /**
     * Creates a new Python 3 IDE panel.
     *
     * @param context the Designer context
     */
    public Python3IDE(DesignerContext context) {
        this.context = context;
        this.restClient = null;

        // Load preferences
        Preferences prefs = Preferences.userNodeForPackage(Python3IDE.class);
        this.currentTheme = prefs.get(PREF_THEME, "dark");
        this.fontSize = prefs.getInt(PREF_FONT_SIZE, 12);

        initComponents();
        layoutComponents();
        attachListeners();
        applyTheme(currentTheme);

        // Auto-connect to default Gateway on startup
        connectToGateway();
    }

    /**
     * Initializes all UI components.
     */
    private void initComponents() {
        // Gateway URL input
        gatewayUrlField = new JTextField("http://localhost:9088", 15);  // v2.5.4: Reduced by 40% (25 → 15) to make room for Save buttons
        gatewayUrlField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        connectButton = ModernButton.createPrimary("Connect");

        // Theme selector dropdown
        String[] themes = {"Dark", "VS Code Dark+", "Monokai", "Dracula", "Default (Light)", "IntelliJ Light", "Eclipse"};
        themeSelector = new JComboBox<>(themes);
        themeSelector.setSelectedItem("Dark");  // Match default theme
        themeSelector.setFont(ModernTheme.FONT_REGULAR);
        themeSelector.setBackground(ModernTheme.PANEL_BACKGROUND);
        themeSelector.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        themeSelector.setPreferredSize(new Dimension(153, 28));  // Reduced by 15% to give more space for "Theme:" label (v2.3.1)

        // Code editor with RSyntaxTextArea
        codeEditor = new RSyntaxTextArea(20, 80);
        codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
        codeEditor.setCodeFoldingEnabled(true);
        codeEditor.setAutoIndentEnabled(true);
        codeEditor.setMarkOccurrences(true);
        codeEditor.setPaintTabLines(true);
        codeEditor.setTabSize(4);
        codeEditor.setFont(new Font("Monospaced", Font.PLAIN, fontSize));
        // v2.5.8: Removed hint text - editor starts empty

        // Enable parser notifications for real-time error checking
        codeEditor.setMarkOccurrences(true);

        // Unsaved changes tracker
        changesTracker = new UnsavedChangesTracker(codeEditor);
        changesTracker.addChangeListener(this::onDirtyStateChanged);

        // Current script indicator label
        currentScriptLabel = new JLabel("No script selected");
        currentScriptLabel.setFont(ModernTheme.withSize(ModernTheme.FONT_BOLD, 11));
        currentScriptLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        currentScriptLabel.setBackground(new Color(30, 30, 30));  // v2.5.13: Match editor background
        currentScriptLabel.setOpaque(true);  // v2.5.13: Make background visible
        currentScriptLabel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));

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

        // Buttons (v2.5.6: Removed keyboard shortcuts from labels - use Info button instead)
        executeButton = ModernButton.createPrimary("Execute");
        clearButton = ModernButton.createDefault("Clear");
        saveButton = ModernButton.createSuccess("Save");
        saveAsButton = ModernButton.createDefault("Save As...");
        importButton = ModernButton.createDefault("Import...");
        exportButton = ModernButton.createDefault("Export...");

        // Font size buttons (v2.0.28)
        fontIncreaseButton = ModernButton.createDefault("A+");
        fontIncreaseButton.setToolTipText("Increase Font Size (Ctrl++)");
        fontDecreaseButton = ModernButton.createDefault("A-");
        fontDecreaseButton.setToolTipText("Decrease Font Size (Ctrl+-)");

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);

        // v2.5.21: Create execution mode tabs (Python IDE / Terminal)
        pythonIdeTab = new CustomTabButton("Python IDE");
        terminalTab = new CustomTabButton("Terminal");
        pythonIdeTab.setSelected(true);  // Python IDE mode selected by default

        // Script Browser Tree (Ignition Tag Browser style)
        rootNode = new ScriptTreeNode("Scripts");
        treeModel = new DefaultTreeModel(rootNode);
        scriptTree = new JTree(treeModel);
        scriptTree.setRootVisible(true);
        scriptTree.setShowsRootHandles(true);
        scriptTree.setCellRenderer(new ScriptTreeCellRenderer());
        scriptTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        scriptTree.setRowHeight(20);  // Compact 20px rows (16px icon + 4px padding)
        scriptTree.setDragEnabled(true);
        scriptTree.setDropMode(DropMode.ON_OR_INSERT);
        scriptTree.setTransferHandler(new ScriptTreeTransferHandler());

        // Ignition Tag Browser-style tree appearance
        scriptTree.setBackground(ModernTheme.TREE_BACKGROUND);
        scriptTree.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        scriptTree.setFont(new Font("SansSerif", Font.PLAIN, 12));
        scriptTree.putClientProperty("JTree.lineStyle", "None");  // Clean look without connecting lines
        scriptTree.setToggleClickCount(1);  // Single-click to expand (like Tag Browser)

        // Metadata Panel
        metadataPanel = new ScriptMetadataPanel();

        // Diagnostics Panel (v1.15.0 - displays performance metrics)
        diagnosticsPanel = new DiagnosticsPanel();

        // Find/Replace Dialogs (lazy initialized when first used)
        findDialog = null;
        replaceDialog = null;
    }

    /**
     * Lays out all components in the panel.
     */
    private void layoutComponents() {
        // v2.5.22: NUCLEAR FIX - Remove ALL gaps and borders
        setLayout(new BorderLayout(0, 0));  // Zero gaps (was 5,5)
        setBorder(new EmptyBorder(5, 5, 5, 5));  // Minimal padding (was 10,10,10,10)
        setBackground(new Color(30, 30, 30));  // Match all child panels exactly

        // Top area: Gateway Connection with theme selector
        JPanel gatewayPanel = new JPanel(new BorderLayout(10, 0));
        gatewayPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        // v2.5.10: Removed empty border to eliminate white padding inside panel
        gatewayPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT),
                "Gateway Connection",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                ModernTheme.FONT_REGULAR,
                ModernTheme.FOREGROUND_PRIMARY));

        // Left side: URL and Connect button
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 1));  // Reduced vertical gap for 25% height reduction (Issue 5 - v1.15.1)
        leftPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        JLabel urlLabel = new JLabel("URL:");
        urlLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        leftPanel.add(urlLabel);
        leftPanel.add(gatewayUrlField);
        leftPanel.add(connectButton);
        gatewayPanel.add(leftPanel, BorderLayout.WEST);

        // Center: Execution mode tabs and action buttons - v2.5.21 UX improvement (tabs instead of dropdown)
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 1));
        centerPanel.setBackground(ModernTheme.PANEL_BACKGROUND);

        // v2.5.21: Mode tabs panel (like Output/Errors tabs)
        JPanel modeTabsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        modeTabsPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        modeTabsPanel.setBorder(null);
        modeTabsPanel.add(pythonIdeTab);
        modeTabsPanel.add(terminalTab);

        centerPanel.add(modeTabsPanel);
        centerPanel.add(executeButton);
        centerPanel.add(clearButton);
        centerPanel.add(saveButton);
        centerPanel.add(saveAsButton);
        centerPanel.add(importButton);
        centerPanel.add(exportButton);
        centerPanel.add(progressBar);
        gatewayPanel.add(centerPanel, BorderLayout.CENTER);

        // Right side: Font size controls, Theme selector, and Information button (v2.5.15: Info moved to right)
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 1));  // Horizontal gap adjusted (v2.3.3)
        rightPanel.setBackground(ModernTheme.PANEL_BACKGROUND);

        // Font size controls
        JLabel fontLabel = new JLabel("Font:");
        fontLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        fontLabel.setFont(ModernTheme.FONT_REGULAR);
        rightPanel.add(fontLabel);
        rightPanel.add(fontDecreaseButton);
        rightPanel.add(fontIncreaseButton);

        // Theme selector with fixed label size to prevent cutoff (v2.3.3)
        JLabel themeLabel = new JLabel("Theme:");
        themeLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        themeLabel.setFont(ModernTheme.FONT_REGULAR);
        themeLabel.setPreferredSize(new Dimension(55, 28));  // Fixed width to prevent cutoff on initial load
        themeLabel.setMinimumSize(new Dimension(55, 28));
        rightPanel.add(themeLabel);
        rightPanel.add(themeSelector);

        // Information button (v2.5.1, v2.5.4: Updated icon, v2.5.15: Moved to right of Theme)
        ModernButton infoButton = ModernButton.createDefault("ⓘ Info");
        infoButton.setToolTipText("View keyboard shortcuts and user guide");
        infoButton.addActionListener(e -> showInformationDialog());
        rightPanel.add(infoButton);

        gatewayPanel.add(rightPanel, BorderLayout.EAST);

        add(gatewayPanel, BorderLayout.NORTH);

        // Create main split pane (sidebar | editor) with themed UI (v2.3.3 - direct paint approach)
        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setUI(new ThemedSplitPaneUI(useDarkTheme ? ModernTheme.BORDER_DEFAULT : new Color(200, 200, 200)));  // v2.5.7: Use BORDER_DEFAULT instead of BACKGROUND_DARKER
        mainSplit.setDividerLocation(250);
        mainSplit.setBackground(new Color(30, 30, 30));  // v2.5.23: Match editor background exactly
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
        // v2.5.10: Removed empty border to eliminate white padding inside panel
        treeScroll.setBorder(new TitledBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT),
                "Script Browser",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                ModernTheme.FONT_REGULAR,
                ModernTheme.FOREGROUND_PRIMARY));
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

        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.setBackground(ModernTheme.BACKGROUND_DARK);
        treePanel.add(treeToolbar, BorderLayout.NORTH);
        treePanel.add(treeScroll, BorderLayout.CENTER);

        // Bottom panel: metadata only (diagnostics moved to execution results panel - v1.17.2)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(ModernTheme.BACKGROUND_DARK);
        bottomPanel.add(metadataPanel, BorderLayout.CENTER);

        // Split tree and bottom panel (metadata only) with themed UI (v2.3.3)
        sidebarSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        sidebarSplit.setUI(new ThemedSplitPaneUI(useDarkTheme ? ModernTheme.BORDER_DEFAULT : new Color(200, 200, 200)));  // v2.5.7: Use BORDER_DEFAULT instead of BACKGROUND_DARKER
        sidebarSplit.setTopComponent(treePanel);
        sidebarSplit.setBottomComponent(bottomPanel);
        sidebarSplit.setDividerLocation(400);  // More space for tree since diagnostics moved (v1.17.2)
        sidebarSplit.setResizeWeight(0.6);  // More weight to tree
        sidebarSplit.setBackground(new Color(30, 30, 30));  // v2.5.23: Match editor background exactly
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
        panel.setBackground(new Color(30, 30, 30));  // v2.5.23: Match ALL child components exactly

        // Editor with line numbers
        RTextScrollPane editorScroll = new RTextScrollPane(codeEditor);
        editorScroll.setLineNumbersEnabled(true);

        // v2.5.20: CRITICAL - Remove ALL borders and set backgrounds to eliminate white rectangle
        editorScroll.setBorder(null);
        editorScroll.setViewportBorder(null);
        editorScroll.setOpaque(true);
        editorScroll.setBackground(new Color(30, 30, 30));
        editorScroll.getViewport().setBackground(new Color(30, 30, 30));
        editorScroll.getViewport().setOpaque(true);

        // v2.5.20: Fix gutter (line numbers) border and background
        if (editorScroll.getGutter() != null) {
            editorScroll.getGutter().setBorder(null);
            editorScroll.getGutter().setBackground(new Color(30, 30, 30));
            editorScroll.getGutter().setOpaque(true);

            // v2.5.26: CRITICAL - Set gutter border color to match background (eliminates white rectangle)
            // RTextScrollPane draws a border around the entire component using gutter.getBorderColor()
            try {
                // Try to set border color if the method exists
                java.lang.reflect.Method setBorderColorMethod =
                    editorScroll.getGutter().getClass().getMethod("setBorderColor", java.awt.Color.class);
                setBorderColorMethod.invoke(editorScroll.getGutter(), new Color(30, 30, 30));
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

        // v2.5.22: NUCLEAR FIX - Ensure code editor has zero margin and dark background
        codeEditor.setBackground(new Color(30, 30, 30));
        codeEditor.setOpaque(true);
        codeEditor.setMargin(new java.awt.Insets(0, 0, 0, 0));  // ZERO internal margin

        // v2.5.22: Create simple editor container (just the scroll pane, no header)
        editorContainer = new JPanel(new BorderLayout(0, 0));
        editorContainer.setBackground(new Color(30, 30, 30));
        editorContainer.setOpaque(true);
        editorContainer.setBorder(null);
        editorContainer.add(editorScroll, BorderLayout.CENTER);

        // v2.5.9: Create terminal panel for true terminal UX
        terminalPanel = new TerminalPanel(this::executeTerminalCommand);

        // v2.5.22: Create centerPanel with CardLayout to switch between editor and terminal
        centerPanel = new JPanel(new CardLayout());
        centerPanel.setBackground(new Color(30, 30, 30));
        centerPanel.setOpaque(true);  // v2.5.22: Ensure opaque
        centerPanel.setBorder(null);  // v2.5.22: No border
        centerPanel.add(editorContainer, "EDITOR");
        centerPanel.add(terminalPanel, "TERMINAL");

        // v2.5.22: Create title panel (for "Python 3 Code Editor / Terminal" text)
        JPanel editorTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        editorTitlePanel.setBackground(new Color(30, 30, 30));
        editorTitlePanel.setOpaque(true);
        editorTitlePanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));  // Padding for text only

        editorTitleLabel = new JLabel("Python 3 Code Editor");
        editorTitleLabel.setFont(ModernTheme.FONT_REGULAR);
        editorTitleLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        editorTitlePanel.add(editorTitleLabel);

        // v2.5.22: Create execution mode tab panel (Python IDE / Terminal tabs) - OUTSIDE CardLayout
        JPanel modeTabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        modeTabPanel.setBackground(new Color(30, 30, 30));
        modeTabPanel.setOpaque(true);
        modeTabPanel.setBorder(null);
        modeTabPanel.add(pythonIdeTab);
        modeTabPanel.add(terminalTab);

        // v2.5.22: Create header panel (title + tabs) - stays visible in both modes
        JPanel topHeaderPanel = new JPanel(new BorderLayout(0, 0));
        topHeaderPanel.setBackground(new Color(30, 30, 30));
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
        bottomSplit.setUI(new ThemedSplitPaneUI(useDarkTheme ? ModernTheme.BORDER_DEFAULT : new Color(200, 200, 200)));  // v2.5.7: Use BORDER_DEFAULT instead of BACKGROUND_DARKER
        bottomSplit.setLeftComponent(outputPanel);
        bottomSplit.setRightComponent(diagnosticsPanel);
        bottomSplit.setResizeWeight(0.75);  // 75% for execution results, 25% for diagnostics
        bottomSplit.setBackground(new Color(30, 30, 30));  // v2.5.23: Match editor background exactly
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

        // Clear button
        clearButton.addActionListener(e -> clearOutput());

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

        // Theme selector
        themeSelector.addActionListener(e -> {
            String selected = (String) themeSelector.getSelectedItem();
            if (selected != null) {
                String themeKey = mapThemeNameToKey(selected);
                applyTheme(themeKey);
            }
        });

        // Keyboard shortcuts
        setupKeyboardShortcuts();

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
     * Sets up keyboard shortcuts.
     */
    private void setupKeyboardShortcuts() {
        InputMap inputMap = codeEditor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = codeEditor.getActionMap();

        // Ctrl+Enter: Execute
        KeyStroke ctrlEnter = KeyStroke.getKeyStroke("control ENTER");
        inputMap.put(ctrlEnter, "execute");
        actionMap.put("execute", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeCode();
            }
        });

        // Ctrl+S: Save
        KeyStroke ctrlS = KeyStroke.getKeyStroke("control S");
        inputMap.put(ctrlS, "save");
        actionMap.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveCurrentScript();
            }
        });

        // Ctrl+Shift+S: Save As
        KeyStroke ctrlShiftS = KeyStroke.getKeyStroke("control shift S");
        inputMap.put(ctrlShiftS, "saveAs");
        actionMap.put("saveAs", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveScriptAs();
            }
        });

        // Ctrl+N: New Script
        KeyStroke ctrlN = KeyStroke.getKeyStroke("control N");
        inputMap.put(ctrlN, "newScript");
        actionMap.put("newScript", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createNewScript();
            }
        });

        // Ctrl++: Increase font
        KeyStroke ctrlPlus = KeyStroke.getKeyStroke("control PLUS");
        inputMap.put(ctrlPlus, "increaseFontSize");
        actionMap.put("increaseFontSize", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeFontSize(1);
            }
        });

        // Ctrl+-: Decrease font
        KeyStroke ctrlMinus = KeyStroke.getKeyStroke("control MINUS");
        inputMap.put(ctrlMinus, "decreaseFontSize");
        actionMap.put("decreaseFontSize", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeFontSize(-1);
            }
        });

        // Ctrl+0: Reset font
        KeyStroke ctrl0 = KeyStroke.getKeyStroke("control 0");
        inputMap.put(ctrl0, "resetFontSize");
        actionMap.put("resetFontSize", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setFontSize(12);
            }
        });

        // Ctrl+F: Find
        KeyStroke ctrlF = KeyStroke.getKeyStroke("control F");
        inputMap.put(ctrlF, "find");
        actionMap.put("find", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showFindDialog();
            }
        });

        // Ctrl+H: Replace
        KeyStroke ctrlH = KeyStroke.getKeyStroke("control H");
        inputMap.put(ctrlH, "replace");
        actionMap.put("replace", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showReplaceDialog();
            }
        });

        // Ctrl+Shift+F: Advanced Find/Replace
        KeyStroke ctrlShiftF = KeyStroke.getKeyStroke("control shift F");
        inputMap.put(ctrlShiftF, "advancedFindReplace");
        actionMap.put("advancedFindReplace", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAdvancedFindReplaceDialog();
            }
        });
    }

    /**
     * Connects to the Gateway.
     */
    private void connectToGateway() {
        String url = gatewayUrlField.getText().trim();

        if (url.isEmpty()) {
            setStatus("Please enter a Gateway URL", Color.RED);
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
            gatewayUrlField.setText(url);
        }

        try {
            restClient = new Python3RestClient(url);
            statusBar.setStatus("Connected to " + url, ModernStatusBar.MessageType.SUCCESS);
            statusBar.setConnection("Connected", ModernTheme.SUCCESS);
            statusBar.setPoolStats("Pool: Checking...", ModernTheme.INFO);

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
            refreshScriptTree();

        } catch (Exception e) {
            statusBar.setStatus("Connection failed: " + e.getMessage(), ModernStatusBar.MessageType.ERROR);
            statusBar.setConnection("Not Connected", ModernTheme.ERROR);
            statusBar.setPoolStats("Pool: Not Connected", ModernTheme.ERROR);
            statusBar.setPythonVersion("Python: --");
            LOGGER.error("Failed to connect to Gateway: {}", url, e);
        }
    }

    /**
     * Executes the Python code in the editor or shell command.
     * v2.5.0: Added support for Shell Command mode
     */
    private void executeCode() {
        if (restClient == null) {
            setStatus("Not connected to Gateway", Color.RED);
            return;
        }

        String code = codeEditor.getText().trim();

        if (code.isEmpty()) {
            setStatus("No code to execute", Color.ORANGE);
            return;
        }

        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }

        // Check execution mode (v2.5.21: Use tab selected state instead of dropdown)
        boolean isShellMode = terminalTab.isSelected();

        clearOutput();

        executeButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        if (isShellMode) {
            // v2.5.8: Use interactive shell for Terminal mode
            setStatus("Executing terminal command (interactive shell)...", Color.BLUE);

            // Create session if not exists
            if (interactiveShellSessionId == null) {
                try {
                    interactiveShellSessionId = restClient.createInteractiveShellSession();
                    LOGGER.info("Created interactive shell session: {}", interactiveShellSessionId);
                } catch (IOException e) {
                    handleError(e);
                    return;
                }
            }

            // Execute command in interactive session
            final String sessionId = interactiveShellSessionId;
            currentWorker = new SwingWorker<ExecutionResult, Void>() {
                @Override
                protected ExecutionResult doInBackground() throws Exception {
                    long startTime = System.currentTimeMillis();
                    ExecutionResult result = restClient.executeInteractiveShellCommand(sessionId, code);

                    // Add execution time
                    long execTime = System.currentTimeMillis() - startTime;
                    return new ExecutionResult(result.isSuccess(), result.getResult(), result.getError(), execTime, System.currentTimeMillis());
                }

                @Override
                protected void done() {
                    try {
                        ExecutionResult result = get();

                        // Append to terminal history
                        terminalHistory.append("$ ").append(code).append("\n");
                        if (result.getResult() != null && !result.getResult().isEmpty()) {
                            terminalHistory.append(result.getResult()).append("\n");
                        }

                        // Show accumulated history
                        outputArea.setText(terminalHistory.toString());

                        executeButton.setEnabled(true);
                        progressBar.setVisible(false);

                        long time = result.getExecutionTimeMs() != null ? result.getExecutionTimeMs() : 0;
                        setStatus(String.format("Command executed in %d ms", time), new Color(0, 128, 0));

                        // Clear editor for next command
                        codeEditor.setText("");

                        refreshDiagnostics();
                    } catch (Exception e) {
                        handleError(e);
                    }
                }
            };

            currentWorker.execute();
        } else {
            setStatus("Executing...", Color.BLUE);

            currentWorker = new Python3ExecutionWorker(
                    restClient,
                    code,
                    new HashMap<>(),
                    false,  // not evaluation
                    false,  // v2.5.8: Not shell mode (interactive shell handled above)
                    this::handleSuccess,
                    this::handleError
            );

            currentWorker.execute();
        }
    }

    /**
     * Handles successful execution.
     */
    private void handleSuccess(ExecutionResult result) {
        executeButton.setEnabled(true);
        progressBar.setVisible(false);

        if (result.isSuccess()) {
            String output = result.getResult() != null ? result.getResult() : "(no output)";
            outputArea.setText(output);

            long time = result.getExecutionTimeMs() != null ? result.getExecutionTimeMs() : 0;
            setStatus(String.format("Execution completed in %d ms", time), new Color(0, 128, 0));

        } else {
            String error = result.getError() != null ? result.getError() : "Unknown error";
            errorArea.setText(error);
            setStatus("Execution failed", Color.RED);
        }

        refreshDiagnostics();
    }

    /**
     * Handles execution errors.
     */
    private void handleError(Exception error) {
        executeButton.setEnabled(true);
        progressBar.setVisible(false);

        errorArea.setText("Connection error: " + error.getMessage());
        setStatus("Execution failed", Color.RED);

        LOGGER.error("Execution error", error);
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
            if (interactiveShellSessionId == null && restClient != null) {
                try {
                    interactiveShellSessionId = restClient.createInteractiveShellSession();
                    LOGGER.info("Created interactive shell session: {}", interactiveShellSessionId);

                    // Get initial working directory
                    updateTerminalWorkingDirectory();
                } catch (IOException e) {
                    LOGGER.error("Failed to create interactive shell session", e);
                }
            }

            // Focus the terminal command input
            terminalPanel.focusCommandInput();

            // v2.5.22: Update title label for Terminal mode
            editorTitleLabel.setText("Terminal");
            currentScriptLabel.setVisible(false);  // Hide script label in terminal mode

            setStatus("Terminal mode: Interactive shell (type commands and press Enter)", new Color(100, 149, 237));
        } else {
            // v2.5.9: Switch back to editor panel view
            ((CardLayout) centerPanel.getLayout()).show(centerPanel, "EDITOR");

            // Switching from Terminal to Python - close shell session
            if (interactiveShellSessionId != null && restClient != null) {
                try {
                    restClient.closeInteractiveShellSession(interactiveShellSessionId);
                    LOGGER.info("Closed interactive shell session on mode switch");
                } catch (IOException e) {
                    LOGGER.error("Failed to close interactive shell session", e);
                }
                interactiveShellSessionId = null;
                terminalHistory.setLength(0);  // Clear history
            }

            // Python Code mode: Restore Python syntax highlighting
            codeEditor.setSyntaxEditingStyle(org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_PYTHON);
            codeEditor.setBackground(new Color(30, 30, 30));  // Standard background
            codeEditor.setCurrentLineHighlightColor(new Color(50, 50, 50));
            codeEditor.setFont(ModernTheme.FONT_MONOSPACE);  // Standard monospace

            // v2.5.18: Restore editor panel title and script indicator (using label instead of TitledBorder)
            editorTitleLabel.setText("Python 3 Code Editor");
            currentScriptLabel.setVisible(true);
            // v2.5.6: Restore script label to current script or default
            if (currentScript != null) {
                currentScriptLabel.setText("Current script: " + currentScript.getName());
            } else {
                currentScriptLabel.setText("No script selected");
            }

            setStatus("Python Code mode: Write Python 3 code", new Color(100, 149, 237));
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
        if (restClient == null || interactiveShellSessionId == null) {
            terminalPanel.appendOutput("ERROR: Not connected or no session");
            return;
        }

        // Execute command in background
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                ExecutionResult result = restClient.executeInteractiveShellCommand(interactiveShellSessionId, command);
                return result.getResult();
            }

            @Override
            protected void done() {
                try {
                    String output = get();
                    terminalPanel.appendOutput(output != null ? output : "");

                    // Update working directory if command was cd
                    if (command.trim().startsWith("cd ")) {
                        updateTerminalWorkingDirectory();
                    }
                } catch (Exception e) {
                    terminalPanel.appendOutput("ERROR: " + e.getMessage());
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
        if (restClient == null || interactiveShellSessionId == null) {
            return;
        }

        // Execute pwd command to get current directory
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                // Detect OS
                String os = System.getProperty("os.name").toLowerCase();
                String pwdCommand = os.contains("win") ? "cd" : "pwd";

                ExecutionResult result = restClient.executeInteractiveShellCommand(interactiveShellSessionId, pwdCommand);
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
                        setStatus("Pool size changed to " + newSize, new Color(0, 128, 0));
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
        } else if (color.equals(new Color(0, 128, 0)) || color.equals(ModernTheme.SUCCESS)) {
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

        treeModel.reload();
        scriptTree.expandRow(0);  // Expand root
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
                    setStatus("Loaded: " + script.getName(), new Color(0, 128, 0));
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
     * If script metadata exists (already saved), does a quick save.
     * Otherwise, shows the save dialog.
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

        // If script already has metadata, do a quick save
        if (currentScript != null && currentScript.getName() != null && !currentScript.getName().isEmpty()) {
            String name = currentScript.getName();
            String author = currentScript.getAuthor() != null ? currentScript.getAuthor() : "Unknown";
            String version = currentScript.getVersion() != null ? currentScript.getVersion() : "1.0";
            String folder = currentScript.getFolderPath() != null ? currentScript.getFolderPath() : "";
            String description = currentScript.getDescription() != null ? currentScript.getDescription() : "";

            saveScript(name, code, description, author, folder, version);
        } else {
            // New script - show save dialog
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
        showSaveDialog();
    }

    /**
     * Shows the save script dialog using custom dark-themed dialog.
     *
     * v2.0.11: Replaced JOptionPane with DarkDialog for proper dark theme support
     */
    private void showSaveDialog() {
        // Prepare fields with current values
        Map<String, String> fields = new java.util.LinkedHashMap<>();
        fields.put("Script Name", currentScript != null ? currentScript.getName() : "");
        fields.put("Author", currentScript != null ? currentScript.getAuthor() : "Unknown");
        fields.put("Version", currentScript != null ? currentScript.getVersion() : "1.0");
        fields.put("Folder Path", currentScript != null ? currentScript.getFolderPath() : "");
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
                    if (currentScript == null) {
                        currentScript = new ScriptMetadata();
                    }
                    currentScript.setName(name);
                    currentScript.setDescription(description);
                    currentScript.setAuthor(author);
                    currentScript.setFolderPath(folderPath);
                    currentScript.setVersion(version);

                    setStatus("Script saved: " + name, new Color(0, 128, 0));
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
     */
    private void createNewScript() {
        // Check for unsaved changes
        if (changesTracker.isDirty()) {
            int choice = showUnsavedChangesDialog();

            if (choice == JOptionPane.YES_OPTION) {
                saveCurrentScript();
            } else if (choice == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }

        // Clear editor for new script
        changesTracker.loadContent("# New Python Script\n\n");
        currentScript = null;
        updateCurrentScriptLabel();
        metadataPanel.clear();
        setStatus("New script", Color.BLUE);
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
            newScriptItem.addActionListener(ev -> createNewScript());
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

        SwingWorker<SavedScript, Void> worker = new SwingWorker<SavedScript, Void>() {
            @Override
            protected SavedScript doInBackground() throws Exception {
                return restClient.loadScript(metadata.getName());
            }

            @Override
            protected void done() {
                try {
                    SavedScript script = get();

                    // v2.0.17: Removed applyFileChooserTheme() - used global UIManager.put()

                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setSelectedFile(new File(script.getName() + ".py"));

                    int result = fileChooser.showSaveDialog(Python3IDE.this);

                    if (result == JFileChooser.APPROVE_OPTION) {
                        File file = fileChooser.getSelectedFile();

                        try (FileWriter writer = new FileWriter(file)) {
                            writer.write(script.getCode());
                            setStatus("Exported: " + file.getName(), new Color(0, 128, 0));
                        }
                    }

                } catch (Exception e) {
                    LOGGER.error("Failed to export script", e);
                    DarkDialog.showMessage(
                            Python3IDE.this,
                            "Failed to export script: " + e.getMessage(),
                            "Error"
                    );
                }
            }
        };

        worker.execute();
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
                    setStatus("Renamed '" + oldName + "' to '" + finalNewName + "'", new Color(0, 128, 0));
                    refreshScriptTree();

                    // Update current script metadata if this is the currently loaded script
                    if (currentScript != null && currentScript.getName().equals(oldName)) {
                        currentScript.setName(finalNewName);
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
        contentPanel.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARK : Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Fields panel
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARK : Color.WHITE);
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
        nameField.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARKER : new Color(245, 245, 245));
        nameField.setForeground(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        nameField.setCaretColor(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        nameField.setFont(ModernTheme.FONT_REGULAR);
        nameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(useDarkTheme ? ModernTheme.BORDER_DEFAULT : new Color(200, 200, 200), 1),
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
        descArea.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARKER : new Color(245, 245, 245));
        descArea.setForeground(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        descArea.setCaretColor(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        descArea.setFont(ModernTheme.FONT_REGULAR);
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setPreferredSize(new Dimension(300, 80));
        descScroll.setBorder(BorderFactory.createLineBorder(useDarkTheme ? ModernTheme.BORDER_DEFAULT : new Color(200, 200, 200), 1));
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
        authorField.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARKER : new Color(245, 245, 245));
        authorField.setForeground(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        authorField.setCaretColor(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        authorField.setFont(ModernTheme.FONT_REGULAR);
        authorField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(useDarkTheme ? ModernTheme.BORDER_DEFAULT : new Color(200, 200, 200), 1),
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
        versionField.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARKER : new Color(245, 245, 245));
        versionField.setForeground(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        versionField.setCaretColor(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        versionField.setFont(ModernTheme.FONT_REGULAR);
        versionField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(useDarkTheme ? ModernTheme.BORDER_DEFAULT : new Color(200, 200, 200), 1),
            new EmptyBorder(5, 5, 5, 5)
        ));
        fieldsPanel.add(versionField, gbc);

        contentPanel.add(fieldsPanel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARK : Color.WHITE);

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
                    setStatus("Updated metadata for '" + newName + "'", new Color(0, 128, 0));
                    refreshScriptTree();

                    // Update current script metadata if this is the currently loaded script
                    if (currentScript != null && currentScript.getName().equals(oldName)) {
                        currentScript.setName(newName);
                        currentScript.setDescription(newDescription);
                        currentScript.setAuthor(newAuthor);
                        currentScript.setVersion(newVersion);

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
                    setStatus("Renamed folder '" + oldName + "' to '" + finalNewName + "'", new Color(0, 128, 0));
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
     */
    private String getFolderPathForNode(ScriptTreeNode folderNode) {
        if (folderNode == rootNode) {
            return "";
        }

        StringBuilder path = new StringBuilder();
        Object[] pathArray = folderNode.getPath();

        // Skip root node (index 0)
        for (int i = 1; i < pathArray.length; i++) {
            if (path.length() > 0) {
                path.append("/");
            }
            path.append(pathArray[i].toString());
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
                            (newFolderPath.isEmpty() ? "root" : newFolderPath), new Color(0, 128, 0));
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
                    setStatus("Deleted: " + metadata.getName(), new Color(0, 128, 0));
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
        if (restClient == null) {
            DarkDialog.showMessage(
                    this,
                    "Please connect to a Gateway first",
                    "Not Connected"
            );
            return;
        }

        // v2.0.17: Removed applyFileChooserTheme() - used global UIManager.put()

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".py");
            }

            @Override
            public String getDescription() {
                return "Python Files (*.py)";
            }
        });

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            try {
                // Read file content
                String code = new String(Files.readAllBytes(file.toPath()));

                // Get script name without extension
                String fileName = file.getName();
                String scriptName = fileName.endsWith(".py") ?
                    fileName.substring(0, fileName.length() - 3) : fileName;

                // Show save dialog with imported content
                java.util.Map<String, String> fields = new java.util.LinkedHashMap<>();
                fields.put("Script Name", scriptName);
                fields.put("Author", System.getProperty("user.name", "Unknown"));
                fields.put("Version", "1.0");
                fields.put("Folder Path", "");
                fields.put("Description", "Imported from " + fileName);

                java.util.Map<String, String> inputResult = DarkDialog.showMultiInput(this, "Import Script", fields);

                if (inputResult != null) {
                    String name = inputResult.get("Script Name").trim();
                    String author = inputResult.get("Author").trim();
                    String version = inputResult.get("Version").trim();
                    String folder = inputResult.get("Folder Path").trim();
                    String description = inputResult.get("Description").trim();

                    if (name.isEmpty()) {
                        DarkDialog.showMessage(
                                this,
                                "Script name cannot be empty",
                                "Invalid Name"
                        );
                        return;
                    }

                    // Save imported script
                    saveScript(name, code, description, author, folder, version);
                    setStatus("Imported: " + fileName, new Color(0, 128, 0));
                }

            } catch (IOException e) {
                LOGGER.error("Failed to import script", e);
                DarkDialog.showMessage(
                        this,
                        "Failed to import script: " + e.getMessage(),
                        "Error"
                );
            }
        }
    }

    /**
     * Exports the current code in the editor to a .py file.
     */
    private void exportCurrentScript() {
        String code = codeEditor.getText().trim();

        if (code.isEmpty()) {
            DarkDialog.showMessage(
                    this,
                    "Cannot export empty code",
                    "Empty Code"
            );
            return;
        }

        // v2.0.17: Removed applyFileChooserTheme() - used global UIManager.put()

        JFileChooser fileChooser = new JFileChooser();

        // Default filename based on current script name or generic
        String defaultName = (currentScript != null && currentScript.getName() != null) ?
                currentScript.getName() + ".py" : "script.py";
        fileChooser.setSelectedFile(new File(defaultName));

        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            // Ensure .py extension
            if (!file.getName().endsWith(".py")) {
                file = new File(file.getAbsolutePath() + ".py");
            }

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(code);
                setStatus("Exported: " + file.getName(), new Color(0, 128, 0));
                LOGGER.info("Exported script to: {}", file.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("Failed to export script", e);
                DarkDialog.showMessage(
                        this,
                        "Failed to export script: " + e.getMessage(),
                        "Error"
                );
            }
        }
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
                  isDirty ? Color.ORANGE : new Color(0, 128, 0));
    }

    /**
     * Updates the current script label to show the selected script name and folder path.
     */
    private void updateCurrentScriptLabel() {
        if (currentScript == null || currentScript.getName() == null || currentScript.getName().isEmpty()) {
            currentScriptLabel.setText("No script selected");
            currentScriptLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        } else {
            StringBuilder labelText = new StringBuilder();

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

                // Update UI components for dark theme (v2.0.14)
                gatewayUrlField.setBackground(ModernTheme.BACKGROUND_DARKER);
                gatewayUrlField.setForeground(ModernTheme.FOREGROUND_PRIMARY);
                gatewayUrlField.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);

                themeSelector.setBackground(ModernTheme.PANEL_BACKGROUND);
                themeSelector.setForeground(ModernTheme.FOREGROUND_PRIMARY);

                currentScriptLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);

                // Update ModernButton instances for dark theme
                updateButtonTheme(connectButton, ModernTheme.ACCENT_PRIMARY, ModernTheme.ACCENT_HOVER, ModernTheme.ACCENT_ACTIVE);
                updateButtonTheme(executeButton, ModernTheme.ACCENT_PRIMARY, ModernTheme.ACCENT_HOVER, ModernTheme.ACCENT_ACTIVE);
                updateButtonTheme(saveButton, ModernTheme.SUCCESS, ModernTheme.lighten(ModernTheme.SUCCESS, 0.1), ModernTheme.darken(ModernTheme.SUCCESS, 0.1));
                updateButtonTheme(clearButton, ModernTheme.BUTTON_BACKGROUND, ModernTheme.BUTTON_HOVER, ModernTheme.BUTTON_ACTIVE);
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
                outputArea.setBackground(Color.WHITE);
                outputArea.setForeground(Color.BLACK);
                outputArea.setCaretColor(Color.BLACK);

                errorArea.setBackground(Color.WHITE);
                errorArea.setForeground(new Color(180, 0, 0));
                errorArea.setCaretColor(new Color(180, 0, 0));

                // Tree
                scriptTree.setBackground(Color.WHITE);
                scriptTree.setForeground(Color.BLACK);

                // Update UI components for light theme (v2.0.14)
                gatewayUrlField.setBackground(Color.WHITE);
                gatewayUrlField.setForeground(Color.BLACK);
                gatewayUrlField.setCaretColor(Color.BLACK);

                themeSelector.setBackground(Color.WHITE);
                themeSelector.setForeground(Color.BLACK);

                currentScriptLabel.setForeground(new Color(100, 100, 100));  // Light gray for secondary text

                // Update ModernButton instances for light theme (lighter, pastel colors)
                Color lightPrimary = new Color(33, 118, 255);  // Lighter blue
                Color lightPrimaryHover = new Color(23, 108, 245);
                Color lightPrimaryActive = new Color(13, 98, 235);
                Color lightSuccess = new Color(40, 167, 69);  // Lighter green
                Color lightSuccessHover = new Color(30, 157, 59);
                Color lightSuccessActive = new Color(20, 147, 49);
                Color lightDefault = new Color(240, 240, 240);  // Light gray
                Color lightDefaultHover = new Color(230, 230, 230);
                Color lightDefaultActive = new Color(220, 220, 220);

                updateButtonTheme(connectButton, lightPrimary, lightPrimaryHover, lightPrimaryActive);
                updateButtonTheme(executeButton, lightPrimary, lightPrimaryHover, lightPrimaryActive);
                updateButtonTheme(saveButton, lightSuccess, lightSuccessHover, lightSuccessActive);
                updateButtonTheme(clearButton, lightDefault, lightDefaultHover, lightDefaultActive);
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
                updateComponent(this, Color.WHITE);

                // v2.0.17: Removed applyLightDialogTheme() - used global UIManager.put()
            }

            // Force repaint of all components
            SwingUtilities.updateComponentTreeUI(this);

            // Force update of all scrollbar UI delegates
            updateScrollPaneTheme(this, isDarkTheme);

            // Force update of all JSplitPane dividers (Issue 8 - v1.17.1)
            updateSplitPaneDividers(this, isDarkTheme);

            currentTheme = themeName;

            // Save preference
            Preferences prefs = Preferences.userNodeForPackage(Python3IDE.class);
            prefs.put(PREF_THEME, themeName);

            setStatus("Theme changed: " + themeName, new Color(0, 128, 0));
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

            // Style each menu item
            for (Component comp : menu.getComponents()) {
                if (comp instanceof JMenuItem) {
                    JMenuItem item = (JMenuItem) comp;
                    item.setBackground(ModernTheme.BACKGROUND_DARK);
                    item.setForeground(ModernTheme.FOREGROUND_PRIMARY);
                    item.setFont(ModernTheme.FONT_REGULAR);
                }
            }
        } else {
            menu.setBackground(Color.WHITE);
            menu.setForeground(Color.BLACK);
            menu.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));

            // Style each menu item
            for (Component comp : menu.getComponents()) {
                if (comp instanceof JMenuItem) {
                    JMenuItem item = (JMenuItem) comp;
                    item.setBackground(Color.WHITE);
                    item.setForeground(Color.BLACK);
                    item.setFont(ModernTheme.FONT_REGULAR);
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
        container.setBackground(new Color(43, 43, 43));
        Component[] components = container.getComponents();
        for (Component component : components) {
            if (component instanceof JTextField || component instanceof JTextArea) {
                component.setBackground(new Color(30, 30, 30));
                component.setForeground(new Color(224, 224, 224));
                if (component instanceof JTextField) {
                    ((JTextField) component).setCaretColor(new Color(224, 224, 224));
                }
            } else if (component instanceof JLabel) {
                component.setForeground(new Color(224, 224, 224));
            } else if (component instanceof JPanel) {
                component.setBackground(new Color(43, 43, 43));
            } else if (component instanceof JButton) {
                component.setBackground(new Color(60, 63, 65));
                component.setForeground(new Color(224, 224, 224));
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
        Color buttonBg = useDarkTheme ? new Color(60, 63, 65) : new Color(238, 238, 238);
        Color foreground = useDarkTheme ? new Color(224, 224, 224) : Color.BLACK;
        Color borderColor = useDarkTheme ? new Color(60, 63, 65) : new Color(200, 200, 200);

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
        Color hoverBg = useDarkTheme ? new Color(75, 80, 85) : new Color(220, 220, 220);
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
                        titledBorder.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
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
                    titledBorder.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
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
                    verticalBar.setBackground(Color.WHITE);
                    verticalBar.setForeground(Color.BLACK);
                }
            }

            if (horizontalBar != null) {
                horizontalBar.updateUI();
                if (isDarkTheme) {
                    horizontalBar.setBackground(ModernTheme.BACKGROUND_DARK);
                    horizontalBar.setForeground(ModernTheme.FOREGROUND_PRIMARY);
                } else {
                    horizontalBar.setBackground(Color.WHITE);
                    horizontalBar.setForeground(Color.BLACK);
                }
            }

            // Update viewport background
            scrollPane.getViewport().setBackground(isDarkTheme ? ModernTheme.BACKGROUND_DARK : Color.WHITE);
            scrollPane.setBackground(isDarkTheme ? ModernTheme.BACKGROUND_DARK : Color.WHITE);
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
            Color dividerColor = isDarkTheme ? ModernTheme.BORDER_DEFAULT : new Color(200, 200, 200);
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

    // Find/Replace Management

    /**
     * Shows the Find dialog.
     */
    private void showFindDialog() {
        if (findDialog == null) {
            // Lazy initialization
            findDialog = new FindDialog((Frame) SwingUtilities.getWindowAncestor(this), new SearchListenerImpl());
            findDialog.setSearchContext(new SearchContext());
        }

        findDialog.setVisible(true);
    }

    /**
     * Shows the Replace dialog.
     */
    private void showReplaceDialog() {
        if (replaceDialog == null) {
            // Lazy initialization
            replaceDialog = new ReplaceDialog((Frame) SwingUtilities.getWindowAncestor(this), new SearchListenerImpl());
            replaceDialog.setSearchContext(new SearchContext());
        }

        replaceDialog.setVisible(true);
    }

    /**
     * Shows the advanced Find/Replace dialog (v2.1.0).
     * This dialog includes regex support, whole word matching, and search history.
     */
    private void showAdvancedFindReplaceDialog() {
        if (advancedFindReplaceDialog == null) {
            // Lazy initialization
            advancedFindReplaceDialog = new FindReplaceDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                codeEditor
            );
        }

        advancedFindReplaceDialog.showDialog();
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
                    setStatus("Replaced " + replaceAllResult.getCount() + " occurrences", new Color(0, 128, 0));
                    break;

                case MARK_ALL:
                    org.fife.ui.rtextarea.SearchResult markResult = SearchEngine.markAll(codeEditor, context);
                    if (!markResult.wasFound()) {
                        setStatus("Text not found: " + context.getSearchFor(), Color.ORANGE);
                    }
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
        codeEditor.setFont(new Font("Monospaced", Font.PLAIN, fontSize));

        // Save preference
        Preferences prefs = Preferences.userNodeForPackage(Python3IDE.class);
        prefs.putInt(PREF_FONT_SIZE, fontSize);

        LOGGER.info("Font size: {}", fontSize);
    }

    /**
     * Converts SavedScript to ScriptMetadata.
     */
    private ScriptMetadata convertToMetadata(SavedScript script) {
        ScriptMetadata metadata = new ScriptMetadata();
        metadata.setId(script.getId());
        metadata.setName(script.getName());
        metadata.setDescription(script.getDescription());
        metadata.setAuthor(script.getAuthor());
        metadata.setCreatedDate(script.getCreatedDate());
        metadata.setLastModified(script.getLastModified());
        metadata.setFolderPath(script.getFolderPath());
        metadata.setVersion(script.getVersion());
        return metadata;
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
     * Transfer handler for drag and drop in the script tree.
     */
    private class ScriptTreeTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            if (c instanceof JTree) {
                JTree tree = (JTree) c;
                TreePath path = tree.getSelectionPath();
                if (path != null) {
                    Object node = path.getLastPathComponent();
                    if (node instanceof ScriptTreeNode) {
                        ScriptTreeNode scriptNode = (ScriptTreeNode) node;
                        // Allow dragging scripts and folders (but not root)
                        if (scriptNode.isScript()) {
                            return new StringSelection("SCRIPT:" + scriptNode.getScriptMetadata().getName());
                        } else if (scriptNode.isFolder() && scriptNode != rootNode) {
                            return new StringSelection("FOLDER:" + getFolderPathForNode(scriptNode));
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop()) {
                return false;
            }

            // Check if we're dropping on a folder
            JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
            TreePath path = dropLocation.getPath();

            if (path == null) {
                return false;
            }

            Object targetNode = path.getLastPathComponent();
            if (!(targetNode instanceof ScriptTreeNode)) {
                return false;
            }

            ScriptTreeNode target = (ScriptTreeNode) targetNode;
            // Can only drop on folders or root
            return target.isFolder() || target == rootNode;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            try {
                // Get the data being dragged
                String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);

                // Get the target folder
                JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
                TreePath path = dropLocation.getPath();
                ScriptTreeNode targetFolder = (ScriptTreeNode) path.getLastPathComponent();

                // Calculate new folder path
                String newFolderPath;
                if (targetFolder == rootNode) {
                    newFolderPath = "";  // Root level
                } else {
                    newFolderPath = getFolderPath(targetFolder);
                }

                // Determine if we're moving a script or folder
                if (data.startsWith("SCRIPT:")) {
                    String scriptName = data.substring(7);  // Remove "SCRIPT:" prefix
                    moveScript(scriptName, newFolderPath);
                } else if (data.startsWith("FOLDER:")) {
                    String folderPath = data.substring(7);  // Remove "FOLDER:" prefix

                    // Prevent moving folder into itself or its own subfolder
                    if (newFolderPath.equals(folderPath) || newFolderPath.startsWith(folderPath + "/")) {
                        setStatus("Cannot move folder into itself", Color.ORANGE);
                        return false;
                    }

                    moveFolder(folderPath, newFolderPath);
                } else {
                    // Backward compatibility - assume it's a script name without prefix
                    moveScript(data, newFolderPath);
                }

                return true;

            } catch (Exception e) {
                LOGGER.error("Failed to import", e);
                setStatus("Failed to move: " + e.getMessage(), Color.RED);
                return false;
            }
        }

        /**
         * Gets the full folder path for a folder node.
         */
        private String getFolderPath(ScriptTreeNode folderNode) {
            if (folderNode == rootNode) {
                return "";
            }

            StringBuilder path = new StringBuilder();
            Object[] pathArray = folderNode.getPath();

            // Skip root node (index 0)
            for (int i = 1; i < pathArray.length; i++) {
                if (path.length() > 0) {
                    path.append("/");
                }
                path.append(pathArray[i].toString());
            }

            return path.toString();
        }

        /**
         * Moves a script to a new folder.
         */
        private void moveScript(String scriptName, String newFolderPath) {
            if (restClient == null) {
                return;
            }

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                private SavedScript script;

                @Override
                protected Void doInBackground() throws Exception {
                    // Load the script
                    script = restClient.loadScript(scriptName);

                    // Update folder path
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
                                (newFolderPath.isEmpty() ? "root" : newFolderPath), new Color(0, 128, 0));
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
         * Moves a folder to a new parent folder.
         */
        private void moveFolder(String oldFolderPath, String newParentPath) {
            if (restClient == null) {
                return;
            }

            // Calculate the new folder path
            // Extract the folder name from the old path
            String folderName;
            int lastSlash = oldFolderPath.lastIndexOf('/');
            if (lastSlash >= 0) {
                folderName = oldFolderPath.substring(lastSlash + 1);
            } else {
                folderName = oldFolderPath;
            }

            // Combine with new parent path
            final String newFolderPath = newParentPath.isEmpty() ? folderName : newParentPath + "/" + folderName;

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
                            if (scriptFolder.equals(oldFolderPath) || scriptFolder.startsWith(oldFolderPath + "/")) {
                                // Update the folder path
                                String updatedPath = scriptFolder.equals(oldFolderPath) ?
                                        newFolderPath :
                                        newFolderPath + scriptFolder.substring(oldFolderPath.length());

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
                        setStatus("Moved folder '" + folderName + "' to " +
                                (newParentPath.isEmpty() ? "root" : newParentPath), new Color(0, 128, 0));
                        refreshScriptTree();
                    } catch (Exception e) {
                        LOGGER.error("Failed to move folder", e);
                        DarkDialog.showMessage(
                                Python3IDE.this,
                                "Failed to move folder: " + e.getMessage(),
                                "Error"
                        );
                    }
                }
            };

            worker.execute();
        }
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
}
