package com.gaskony.python3.designer.managers;

import com.gaskony.python3.designer.CustomTabButton;
import com.gaskony.python3.designer.DiagnosticsPanel;
import com.gaskony.python3.designer.ModernButton;
import com.gaskony.python3.designer.ModernStatusBar;
import com.gaskony.python3.designer.ModernTheme;
import com.gaskony.python3.designer.ScriptMetadataPanel;
import com.gaskony.python3.designer.ScriptTreeCellRenderer;
import com.gaskony.python3.designer.ScriptTreeNode;
import com.gaskony.python3.designer.TerminalPanel;
import com.gaskony.python3.designer.ThemedSplitPaneUI;
import com.gaskony.python3.designer.UiComponentFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DropMode;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * Builds the Swing component tree for the Python 3 IDE. Extracted from
 * {@code Python3IDE.initComponents() + layoutComponents() + createSidebar() +
 * createEditorPanel()} in v3.13.
 *
 * <p>The IDE creates a {@link Components} record-style holder, hands it to
 * {@link #buildTopBar} / {@link #buildSidebar} / {@link #buildEditorPanel}, and
 * places the resulting panels in itself. All layout details (gaps, borders,
 * card layouts, custom-painted card wrappers) live here.</p>
 *
 * @since v3.13
 */
public final class Python3IDELayout {

    private Python3IDELayout() {
        // Static-utility class
    }

    /**
     * Mutable holder for the components Python3IDE owns. Populated by
     * {@link #createComponents}; layout helpers below read from it.
     */
    public static final class Components {
        public JTextField gatewayUrlField;
        public ModernButton connectButton;
        public RSyntaxTextArea codeEditor;
        public JLabel currentScriptLabel;
        public JTextArea outputArea;
        public JTextArea errorArea;
        public ModernStatusBar statusBar;
        public JLabel connectionStatusIndicator;
        public ModernButton executeButton;
        public ModernButton saveButton;
        public ModernButton saveAsButton;
        public ModernButton importButton;
        public ModernButton exportButton;
        public ModernButton fontIncreaseButton;
        public ModernButton fontDecreaseButton;
        public JProgressBar progressBar;
        public CustomTabButton pythonIdeTab;
        public CustomTabButton terminalTab;
        public JComboBox<String> versionSelector;
        public JTree scriptTree;
        public DefaultTreeModel treeModel;
        public ScriptTreeNode rootNode;
        public ScriptMetadataPanel metadataPanel;
        public DiagnosticsPanel diagnosticsPanel;
        public ModernButton newFolderBtn;
        public ModernButton newScriptBtn;
        public ModernButton refreshBtn;
        public JPanel editorContainer;
        public TerminalPanel terminalPanel;
        public JPanel centerPanel;
        public JLabel editorTitleLabel;
        public JSplitPane sidebarSplit;
        public JSplitPane bottomSplit;
    }

    /**
     * Constructs all leaf Swing widgets the IDE needs. Returns a {@link Components}
     * holder.
     */
    public static Components createComponents(String effectiveGatewayUrl, int fontSize) {
        Components c = new Components();

        c.gatewayUrlField = new JTextField(effectiveGatewayUrl, 15);
        c.gatewayUrlField.setFont(ModernTheme.FONT_REGULAR);

        c.connectButton = ModernButton.createPrimary("Connect");

        c.codeEditor = new RSyntaxTextArea(20, 80);
        c.codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
        c.codeEditor.setCodeFoldingEnabled(true);
        c.codeEditor.setAutoIndentEnabled(true);
        c.codeEditor.setMarkOccurrences(true);
        c.codeEditor.setPaintTabLines(true);
        c.codeEditor.setTabSize(4);
        c.codeEditor.setFont(ModernTheme.FONT_CODE.deriveFont((float) fontSize));

        c.currentScriptLabel = new JLabel("No script selected");
        c.currentScriptLabel.setFont(ModernTheme.withSize(ModernTheme.FONT_BOLD, 12));
        c.currentScriptLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        c.currentScriptLabel.setBackground(ModernTheme.BACKGROUND_LIGHT);
        c.currentScriptLabel.setOpaque(true);
        c.currentScriptLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ModernTheme.BORDER_DEFAULT),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));

        c.outputArea = UiComponentFactory.createDarkOutputArea();
        c.errorArea = UiComponentFactory.createDarkErrorArea();
        c.statusBar = new ModernStatusBar();

        c.executeButton = ModernButton.createPrimary("▶ Execute");
        c.executeButton.setToolTipText("Execute code on Gateway (Ctrl+Enter)");
        c.saveButton = ModernButton.createSuccess("✓ Save");
        c.saveButton.setToolTipText("Save current script (Ctrl+S)");
        c.saveAsButton = ModernButton.createSecondary("✎ Save As...");
        c.saveAsButton.setToolTipText("Save script with metadata (Ctrl+Shift+S)");
        c.importButton = ModernButton.createSecondary("↓ Import...");
        c.importButton.setToolTipText("Import Python script from file");
        c.exportButton = ModernButton.createSecondary("↑ Export...");
        c.exportButton.setToolTipText("Export script to .py file");
        c.fontIncreaseButton = ModernButton.createSmall("A+");
        c.fontIncreaseButton.setToolTipText("Increase Font Size (Ctrl++)");
        c.fontDecreaseButton = ModernButton.createSmall("A-");
        c.fontDecreaseButton.setToolTipText("Decrease Font Size (Ctrl+-)");

        c.progressBar = new JProgressBar();
        c.progressBar.setIndeterminate(false);
        c.progressBar.setVisible(false);

        c.pythonIdeTab = new CustomTabButton("Python IDE");
        c.terminalTab = new CustomTabButton("Terminal");
        c.pythonIdeTab.setSelected(true);

        c.versionSelector = new JComboBox<>();
        c.versionSelector.setToolTipText("Select Python version for execution");
        c.versionSelector.setPreferredSize(new Dimension(100, 28));
        c.versionSelector.setMaximumSize(new Dimension(120, 28));
        c.versionSelector.setFont(ModernTheme.FONT_REGULAR);

        c.rootNode = new ScriptTreeNode("Scripts");
        c.treeModel = new DefaultTreeModel(c.rootNode);
        c.scriptTree = new JTree(c.treeModel);
        c.scriptTree.setRootVisible(true);
        c.scriptTree.setShowsRootHandles(true);
        c.scriptTree.setCellRenderer(new ScriptTreeCellRenderer());
        c.scriptTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        c.scriptTree.setRowHeight(24);
        c.scriptTree.setDragEnabled(true);
        c.scriptTree.setDropMode(DropMode.ON_OR_INSERT);
        c.scriptTree.setBackground(ModernTheme.TREE_BACKGROUND);
        c.scriptTree.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        c.scriptTree.setFont(ModernTheme.FONT_REGULAR);
        c.scriptTree.putClientProperty("JTree.lineStyle", "None");
        c.scriptTree.setToggleClickCount(1);

        c.metadataPanel = new ScriptMetadataPanel();
        c.diagnosticsPanel = new DiagnosticsPanel();

        return c;
    }

    /**
     * Builds the gateway-connection toolbar that goes at the top of the IDE.
     * Returns the assembled JPanel.
     *
     * @param dialogActions callbacks for Versions / Packages / Settings / Info buttons
     */
    public static JPanel buildTopBar(Components c, DialogActions dialogActions) {
        JPanel gatewayPanel = new JPanel(new BorderLayout(2, 0));
        gatewayPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        gatewayPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ModernTheme.BORDER_SUBTLE, 1),
            BorderFactory.createEmptyBorder(0, 0, 2, 0)
        ));

        JPanel gatewayCardHeader = ModernTheme.createCardHeader(
            "Gateway Connection", "Configure and manage gateway connectivity");
        gatewayPanel.add(gatewayCardHeader, BorderLayout.NORTH);

        // Left: gateway URL + connection status
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        leftPanel.setBorder(new EmptyBorder(2, 5, 2, 5));

        c.gatewayUrlField.setEditable(false);
        c.gatewayUrlField.setBackground(ModernTheme.PANEL_BACKGROUND);
        c.gatewayUrlField.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        c.gatewayUrlField.setAlignmentX(Component.LEFT_ALIGNMENT);

        c.connectionStatusIndicator = new JLabel("[●] Disconnected");
        c.connectionStatusIndicator.setForeground(ModernTheme.ERROR_BRIGHT);
        c.connectionStatusIndicator.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));
        c.connectionStatusIndicator.setToolTipText("Gateway connection status");
        c.connectionStatusIndicator.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.connectionStatusIndicator.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        leftPanel.add(c.gatewayUrlField);
        leftPanel.add(c.connectionStatusIndicator);
        gatewayPanel.add(leftPanel, BorderLayout.WEST);

        // Center: mode tabs + version selector + action buttons
        JPanel toolbarCenterPanel = new JPanel(new FlowLayout(
            FlowLayout.CENTER, ModernTheme.BUTTON_GAP, ModernTheme.TOOLBAR_VPADDING));
        toolbarCenterPanel.setBackground(ModernTheme.PANEL_BACKGROUND);

        JPanel modeTabsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        modeTabsPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        modeTabsPanel.setBorder(null);
        modeTabsPanel.add(c.pythonIdeTab);
        modeTabsPanel.add(c.terminalTab);
        toolbarCenterPanel.add(modeTabsPanel);

        JPanel versionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        versionPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        versionPanel.add(new JLabel("Python:"));
        versionPanel.add(c.versionSelector);
        toolbarCenterPanel.add(versionPanel);
        toolbarCenterPanel.add(c.executeButton);
        toolbarCenterPanel.add(c.saveButton);
        toolbarCenterPanel.add(c.saveAsButton);
        toolbarCenterPanel.add(c.importButton);
        toolbarCenterPanel.add(c.exportButton);
        toolbarCenterPanel.add(c.progressBar);
        gatewayPanel.add(toolbarCenterPanel, BorderLayout.CENTER);

        // Right: Versions / Packages / Settings / Info
        JPanel rightPanel = new JPanel(new FlowLayout(
            FlowLayout.RIGHT, ModernTheme.BUTTON_GAP, ModernTheme.TOOLBAR_VPADDING));
        rightPanel.setBackground(ModernTheme.PANEL_BACKGROUND);

        ModernButton versionsButton = ModernButton.createDefault("Versions");
        versionsButton.setToolTipText("Install/uninstall Python versions");
        versionsButton.addActionListener(e -> dialogActions.openVersionManager());
        rightPanel.add(versionsButton);

        ModernButton packagesButton = ModernButton.createDefault("📦 Packages");
        packagesButton.setToolTipText("Manage Python packages");
        packagesButton.addActionListener(e -> dialogActions.openPackages());
        rightPanel.add(packagesButton);

        ModernButton settingsButton = ModernButton.createDefault("⚙ Settings");
        settingsButton.setToolTipText("Configure IDE settings");
        settingsButton.addActionListener(e -> dialogActions.openSettings());
        rightPanel.add(settingsButton);

        ModernButton infoButton = ModernButton.createDefault("ⓘ Info");
        infoButton.setToolTipText("View module and Python version information");
        infoButton.addActionListener(e -> dialogActions.openInfo());
        rightPanel.add(infoButton);

        gatewayPanel.add(rightPanel, BorderLayout.EAST);
        return gatewayPanel;
    }

    /**
     * Builds the left-side script-browser sidebar. Allocates {@code newFolderBtn},
     * {@code newScriptBtn}, {@code refreshBtn} into {@code c} but does not wire
     * their action listeners; callers attach those later.
     */
    public static JPanel buildSidebar(Components c, boolean useDarkTheme) {
        JPanel sidebar = new JPanel(new BorderLayout(5, 5));
        sidebar.setPreferredSize(new Dimension(250, 600));
        sidebar.setBackground(ModernTheme.BACKGROUND_DARK);

        JScrollPane treeScroll = new JScrollPane(c.scriptTree);
        treeScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        treeScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        treeScroll.setBorder(null);
        treeScroll.setBackground(ModernTheme.TREE_BACKGROUND);
        treeScroll.getViewport().setBackground(ModernTheme.TREE_BACKGROUND);

        JPanel treeToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        treeToolbar.setBackground(ModernTheme.PANEL_BACKGROUND);

        c.newFolderBtn = ModernButton.createSmall("+Folder");
        c.newFolderBtn.setToolTipText("New Folder");
        c.newScriptBtn = ModernButton.createSmall("+Script");
        c.newScriptBtn.setToolTipText("New Script");
        c.refreshBtn = ModernButton.createSmall("Refresh");
        c.refreshBtn.setToolTipText("Refresh Scripts");

        treeToolbar.add(c.newFolderBtn);
        treeToolbar.add(c.newScriptBtn);
        treeToolbar.add(c.refreshBtn);

        JPanel scriptBrowserHeader = ModernTheme.createCardHeader(
            "Script Browser", "Browse and organize Python scripts");

        JPanel treeHeaderAndToolbar = new JPanel(new BorderLayout(0, 0));
        treeHeaderAndToolbar.setBackground(ModernTheme.BACKGROUND_DARK);
        treeHeaderAndToolbar.add(scriptBrowserHeader, BorderLayout.NORTH);
        treeHeaderAndToolbar.add(treeToolbar, BorderLayout.SOUTH);

        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.setBackground(ModernTheme.BACKGROUND_DARK);
        treePanel.add(treeHeaderAndToolbar, BorderLayout.NORTH);

        JPanel treeCardWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                    ModernTheme.CORNER_RADIUS_LARGE, ModernTheme.CORNER_RADIUS_LARGE);
                g2.setColor(ModernTheme.BORDER_DEFAULT);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                    ModernTheme.CORNER_RADIUS_LARGE, ModernTheme.CORNER_RADIUS_LARGE);
                g2.dispose();
            }
        };
        treeCardWrapper.setOpaque(false);
        treeCardWrapper.setBackground(ModernTheme.TREE_BACKGROUND);
        treeCardWrapper.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        treeCardWrapper.add(treeScroll, BorderLayout.CENTER);

        treePanel.add(treeCardWrapper, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(ModernTheme.BACKGROUND_DARK);
        bottomPanel.add(c.metadataPanel, BorderLayout.CENTER);

        c.sidebarSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        c.sidebarSplit.setUI(new ThemedSplitPaneUI(
            useDarkTheme ? ModernTheme.BORDER_DEFAULT : ModernTheme.LIGHT_BORDER));
        c.sidebarSplit.setTopComponent(treePanel);
        c.sidebarSplit.setBottomComponent(bottomPanel);
        c.sidebarSplit.setDividerLocation(400);
        c.sidebarSplit.setResizeWeight(0.6);
        c.sidebarSplit.setBackground(ModernTheme.EDITOR_BACKGROUND);
        c.sidebarSplit.setBorder(null);
        c.sidebarSplit.setDividerSize(4);

        sidebar.add(c.sidebarSplit, BorderLayout.CENTER);
        return sidebar;
    }

    /**
     * Builds the editor panel: code editor (or terminal panel) on top, output
     * tabs at the bottom. Allocates {@code editorContainer}, {@code terminalPanel},
     * {@code centerPanel}, {@code editorTitleLabel}, {@code bottomSplit} into {@code c}.
     *
     * @param onTerminalCommand callback the terminal panel uses for command submission
     * @param onModeTabChanged  callback invoked when user toggles Python/Terminal tabs;
     *                          true = terminal mode, false = python mode
     */
    public static JPanel buildEditorPanel(Components c, boolean useDarkTheme,
                                           java.util.function.Consumer<String> onTerminalCommand,
                                           java.util.function.Consumer<Boolean> onModeTabChanged) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ModernTheme.EDITOR_BACKGROUND);

        RTextScrollPane editorScroll = new RTextScrollPane(c.codeEditor);
        editorScroll.setLineNumbersEnabled(true);
        editorScroll.setBorder(null);
        editorScroll.setViewportBorder(null);
        editorScroll.setOpaque(true);
        editorScroll.setBackground(ModernTheme.EDITOR_BACKGROUND);
        editorScroll.getViewport().setBackground(ModernTheme.EDITOR_BACKGROUND);
        editorScroll.getViewport().setOpaque(true);

        if (editorScroll.getGutter() != null) {
            editorScroll.getGutter().setBorder(null);
            editorScroll.getGutter().setBackground(ModernTheme.EDITOR_BACKGROUND);
            editorScroll.getGutter().setOpaque(true);
            try {
                java.lang.reflect.Method setBorderColorMethod =
                    editorScroll.getGutter().getClass().getMethod("setBorderColor", Color.class);
                setBorderColorMethod.invoke(editorScroll.getGutter(), ModernTheme.EDITOR_BACKGROUND);
            } catch (Exception ignore) {
                // Method doesn't exist on this RSyntaxTextArea version - tolerate
            }
        }

        editorScroll.setFocusable(false);
        c.codeEditor.setBorder(null);
        editorScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        editorScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        editorScroll.addMouseWheelListener(e -> {
            int scrollAmount = e.getUnitsToScroll() * 3;
            javax.swing.JScrollBar vertical = editorScroll.getVerticalScrollBar();
            vertical.setValue(vertical.getValue() + scrollAmount);
        });

        c.codeEditor.setBackground(ModernTheme.EDITOR_BACKGROUND);
        c.codeEditor.setOpaque(true);
        c.codeEditor.setMargin(new Insets(0, 0, 0, 0));

        c.editorContainer = new JPanel(new BorderLayout(0, 0));
        c.editorContainer.setBackground(ModernTheme.EDITOR_BACKGROUND);
        c.editorContainer.setOpaque(true);
        c.editorContainer.setBorder(null);
        c.editorContainer.add(editorScroll, BorderLayout.CENTER);

        c.terminalPanel = new TerminalPanel(onTerminalCommand::accept);

        c.centerPanel = new JPanel(new CardLayout());
        c.centerPanel.setBackground(ModernTheme.EDITOR_BACKGROUND);
        c.centerPanel.setOpaque(true);
        c.centerPanel.setBorder(null);
        c.centerPanel.add(c.editorContainer, "EDITOR");
        c.centerPanel.add(c.terminalPanel, "TERMINAL");

        JPanel editorTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        editorTitlePanel.setBackground(ModernTheme.EDITOR_BACKGROUND);
        editorTitlePanel.setOpaque(true);
        editorTitlePanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        c.editorTitleLabel = new JLabel("Python 3 Code Editor");
        c.editorTitleLabel.setFont(ModernTheme.FONT_REGULAR);
        c.editorTitleLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        editorTitlePanel.add(c.editorTitleLabel);

        JLabel separator = new JLabel(" | ");
        separator.setFont(ModernTheme.FONT_REGULAR);
        separator.setForeground(ModernTheme.FOREGROUND_MUTED);
        editorTitlePanel.add(separator);
        editorTitlePanel.add(c.currentScriptLabel);

        JPanel modeTabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        modeTabPanel.setBackground(ModernTheme.EDITOR_BACKGROUND);
        modeTabPanel.setOpaque(true);
        modeTabPanel.setBorder(null);
        modeTabPanel.add(c.pythonIdeTab);
        modeTabPanel.add(c.terminalTab);

        JPanel topHeaderPanel = new JPanel(new BorderLayout(0, 0));
        topHeaderPanel.setBackground(ModernTheme.EDITOR_BACKGROUND);
        topHeaderPanel.setOpaque(true);
        topHeaderPanel.setBorder(null);
        topHeaderPanel.add(editorTitlePanel, BorderLayout.NORTH);
        topHeaderPanel.add(modeTabPanel, BorderLayout.SOUTH);

        panel.setBorder(null);
        panel.setOpaque(true);
        panel.add(topHeaderPanel, BorderLayout.NORTH);
        panel.add(c.centerPanel, BorderLayout.CENTER);

        ((CardLayout) c.centerPanel.getLayout()).show(c.centerPanel, "EDITOR");

        c.pythonIdeTab.setClickAction(() -> {
            c.pythonIdeTab.setSelected(true);
            c.terminalTab.setSelected(false);
            onModeTabChanged.accept(false);
        });
        c.terminalTab.setClickAction(() -> {
            c.terminalTab.setSelected(true);
            c.pythonIdeTab.setSelected(false);
            onModeTabChanged.accept(true);
        });

        // Output / errors tabs
        JScrollPane outputScroll = wrapInBorderlessScrollPane(c.outputArea);
        JScrollPane errorScroll = wrapInBorderlessScrollPane(c.errorArea);

        CustomTabButton outputTab = new CustomTabButton("Output");
        CustomTabButton errorTab = new CustomTabButton("Errors");
        outputTab.setSelected(true);

        JPanel tabHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabHeaderPanel.setBackground(ModernTheme.BACKGROUND_DARKER);
        tabHeaderPanel.setOpaque(true);
        tabHeaderPanel.setBorder(null);
        tabHeaderPanel.add(outputTab);
        tabHeaderPanel.add(errorTab);

        JPanel tabContentPanel = new JPanel(new CardLayout());
        tabContentPanel.setBackground(ModernTheme.BACKGROUND_DARKER);
        tabContentPanel.setOpaque(true);
        tabContentPanel.setBorder(null);
        tabContentPanel.add(outputScroll, "OUTPUT");
        tabContentPanel.add(errorScroll, "ERRORS");

        outputTab.setClickAction(() -> {
            outputTab.setSelected(true);
            errorTab.setSelected(false);
            ((CardLayout) tabContentPanel.getLayout()).show(tabContentPanel, "OUTPUT");
        });
        errorTab.setClickAction(() -> {
            errorTab.setSelected(true);
            outputTab.setSelected(false);
            ((CardLayout) tabContentPanel.getLayout()).show(tabContentPanel, "ERRORS");
        });

        JPanel outputPanel = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                    ModernTheme.CORNER_RADIUS_LARGE, ModernTheme.CORNER_RADIUS_LARGE);
                g2.setColor(ModernTheme.BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                    ModernTheme.CORNER_RADIUS_LARGE, ModernTheme.CORNER_RADIUS_LARGE);
                g2.dispose();
            }
        };
        outputPanel.setOpaque(false);
        outputPanel.setBackground(ModernTheme.BACKGROUND_DARKER);
        outputPanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        JPanel outputHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        outputHeaderPanel.setBackground(ModernTheme.BACKGROUND_DARKER);
        outputHeaderPanel.setOpaque(true);
        outputHeaderPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 8));

        JLabel outputTitleLabel = new JLabel("Execution Results");
        outputTitleLabel.setFont(ModernTheme.FONT_REGULAR);
        outputTitleLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        outputHeaderPanel.add(outputTitleLabel);

        JPanel outputTopPanel = new JPanel(new BorderLayout(0, 0));
        outputTopPanel.setBackground(ModernTheme.BACKGROUND_DARKER);
        outputTopPanel.setOpaque(true);
        outputTopPanel.add(outputHeaderPanel, BorderLayout.NORTH);
        outputTopPanel.add(tabHeaderPanel, BorderLayout.SOUTH);

        outputPanel.add(outputTopPanel, BorderLayout.NORTH);
        outputPanel.add(tabContentPanel, BorderLayout.CENTER);

        c.bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        c.bottomSplit.setUI(new ThemedSplitPaneUI(
            useDarkTheme ? ModernTheme.BORDER_DEFAULT : ModernTheme.LIGHT_BORDER));
        c.bottomSplit.setLeftComponent(outputPanel);
        c.bottomSplit.setRightComponent(c.diagnosticsPanel);
        c.bottomSplit.setResizeWeight(0.75);
        c.bottomSplit.setBackground(ModernTheme.EDITOR_BACKGROUND);
        c.bottomSplit.setBorder(null);
        c.bottomSplit.setDividerSize(4);
        c.bottomSplit.setPreferredSize(new Dimension(600, 200));

        panel.add(c.bottomSplit, BorderLayout.SOUTH);
        return panel;
    }

    private static JScrollPane wrapInBorderlessScrollPane(JComponent inner) {
        JScrollPane scroll = new JScrollPane(inner);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setViewportBorder(null);
        scroll.setBackground(ModernTheme.BACKGROUND_DARKER);
        scroll.getViewport().setBackground(ModernTheme.BACKGROUND_DARKER);
        scroll.setOpaque(true);
        return scroll;
    }

    /**
     * Callbacks for the four right-side toolbar buttons.
     */
    public interface DialogActions {
        void openVersionManager();
        void openPackages();
        void openSettings();
        void openInfo();
    }
}
