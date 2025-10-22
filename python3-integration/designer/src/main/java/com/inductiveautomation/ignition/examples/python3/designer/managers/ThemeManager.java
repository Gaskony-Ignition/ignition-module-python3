package com.inductiveautomation.ignition.examples.python3.designer.managers;

import com.inductiveautomation.ignition.examples.python3.designer.ModernTheme;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.util.prefs.Preferences;

/**
 * Manages IDE theme application.
 * Self-contained theme management extracted from Python3IDE.
 *
 * v2.0.0: Extracted from Python3IDE.java monolith
 */
public class ThemeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeManager.class);
    private static final String PREF_THEME = "python3ide.theme";

    private String currentTheme;
    private final Preferences prefs;

    public ThemeManager(Class<?> prefsClass) {
        this.prefs = Preferences.userNodeForPackage(prefsClass);
        this.currentTheme = prefs.get(PREF_THEME, "dark");
    }

    /**
     * Applies theme to IDE components.
     */
    public void applyTheme(String themeName, Component rootComponent, RSyntaxTextArea codeEditor,
                           JTextArea outputArea, JTextArea errorArea, JTree scriptTree) throws IOException {
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
            case "vs":
                theme = Theme.load(getClass().getResourceAsStream(
                        "/org/fife/ui/rsyntaxtextarea/themes/vs.xml"));
                isDarkTheme = true;
                break;
            default:
                theme = Theme.load(getClass().getResourceAsStream(
                        "/org/fife/ui/rsyntaxtextarea/themes/default.xml"));
                isDarkTheme = false;
                break;
        }

        theme.apply(codeEditor);

        if (isDarkTheme) {
            applyDarkTheme(outputArea, errorArea, scriptTree, rootComponent);
        } else {
            applyLightTheme(outputArea, errorArea, scriptTree, rootComponent);
        }

        SwingUtilities.updateComponentTreeUI(rootComponent);
        updateScrollPaneTheme(rootComponent, isDarkTheme);
        updateSplitPaneDividers(rootComponent, isDarkTheme);

        currentTheme = themeName;
        prefs.put(PREF_THEME, themeName);
        LOGGER.info("Applied theme: {}", themeName);
    }

    private void applyDarkTheme(JTextArea outputArea, JTextArea errorArea, JTree scriptTree, Component root) {
        outputArea.setBackground(ModernTheme.BACKGROUND_DARKER);
        outputArea.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        outputArea.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);

        errorArea.setBackground(ModernTheme.BACKGROUND_DARKER);
        errorArea.setForeground(ModernTheme.ERROR);
        errorArea.setCaretColor(ModernTheme.ERROR);

        scriptTree.setBackground(ModernTheme.TREE_BACKGROUND);
        scriptTree.setForeground(ModernTheme.FOREGROUND_PRIMARY);

        updateComponent(root, ModernTheme.BACKGROUND_DARK);
        applyDarkDialogTheme();
    }

    private void applyLightTheme(JTextArea outputArea, JTextArea errorArea, JTree scriptTree, Component root) {
        outputArea.setBackground(Color.WHITE);
        outputArea.setForeground(Color.BLACK);
        outputArea.setCaretColor(Color.BLACK);

        errorArea.setBackground(Color.WHITE);
        errorArea.setForeground(new Color(180, 0, 0));
        errorArea.setCaretColor(new Color(180, 0, 0));

        scriptTree.setBackground(Color.WHITE);
        scriptTree.setForeground(Color.BLACK);

        updateComponent(root, Color.WHITE);
        applyLightDialogTheme();
    }

    private void applyDarkDialogTheme() {
        // UX Fix v2.0.9: Enhanced dialog theming for consistent dark theme
        // UX Fix v2.3.2: Comprehensive dark theme for ALL Swing components

        // Dialog components
        UIManager.put("OptionPane.background", ModernTheme.PANEL_BACKGROUND);
        UIManager.put("OptionPane.messageForeground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("OptionPane.messagebackground", ModernTheme.PANEL_BACKGROUND);

        // Panel and container backgrounds
        UIManager.put("Panel.background", ModernTheme.PANEL_BACKGROUND);
        UIManager.put("control", ModernTheme.PANEL_BACKGROUND);

        // Text fields
        UIManager.put("TextField.background", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("TextField.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("TextField.caretForeground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("TextField.selectionBackground", ModernTheme.ACCENT_PRIMARY);
        UIManager.put("TextField.selectionForeground", Color.WHITE);
        UIManager.put("TextField.border", BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT));
        UIManager.put("TextField.inactiveForeground", ModernTheme.FOREGROUND_SECONDARY);

        // Text areas
        UIManager.put("TextArea.background", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("TextArea.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("TextArea.caretForeground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("TextArea.selectionBackground", ModernTheme.ACCENT_PRIMARY);
        UIManager.put("TextArea.selectionForeground", Color.WHITE);

        // Labels
        UIManager.put("Label.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("Label.background", ModernTheme.PANEL_BACKGROUND);
        UIManager.put("Label.disabledForeground", ModernTheme.FOREGROUND_SECONDARY);

        // Buttons
        UIManager.put("Button.background", ModernTheme.BUTTON_BACKGROUND);
        UIManager.put("Button.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("Button.select", ModernTheme.BUTTON_HOVER);
        UIManager.put("Button.focus", ModernTheme.ACCENT_PRIMARY);

        // ComboBox
        UIManager.put("ComboBox.background", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("ComboBox.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", ModernTheme.ACCENT_PRIMARY);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("ComboBox.buttonBackground", ModernTheme.BUTTON_BACKGROUND);
        UIManager.put("ComboBox.buttonDarkShadow", ModernTheme.BORDER_DEFAULT);

        // Tree
        UIManager.put("Tree.background", ModernTheme.TREE_BACKGROUND);
        UIManager.put("Tree.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("Tree.textBackground", ModernTheme.TREE_BACKGROUND);
        UIManager.put("Tree.textForeground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("Tree.selectionBackground", ModernTheme.ACCENT_PRIMARY);
        UIManager.put("Tree.selectionForeground", Color.WHITE);
        UIManager.put("Tree.selectionBorderColor", ModernTheme.ACCENT_PRIMARY);

        // ScrollPane - CRITICAL FIX for light gray backgrounds
        UIManager.put("ScrollPane.background", ModernTheme.BACKGROUND_DARK);
        UIManager.put("ScrollPane.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT));

        // ScrollBar
        UIManager.put("ScrollBar.background", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("ScrollBar.foreground", ModernTheme.FOREGROUND_SECONDARY);
        UIManager.put("ScrollBar.thumb", ModernTheme.FOREGROUND_SECONDARY);
        UIManager.put("ScrollBar.thumbDarkShadow", ModernTheme.BORDER_DEFAULT);
        UIManager.put("ScrollBar.thumbHighlight", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("ScrollBar.thumbShadow", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("ScrollBar.track", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("ScrollBar.trackHighlight", ModernTheme.BACKGROUND_DARK);

        // SplitPane - CRITICAL FIX for light gray dividers
        UIManager.put("SplitPane.background", ModernTheme.BACKGROUND_DARK);
        UIManager.put("SplitPane.dividerSize", 5);
        UIManager.put("SplitPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("SplitPaneDivider.border", BorderFactory.createEmptyBorder());
        UIManager.put("SplitPane.dividerFocusColor", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("SplitPane.highlight", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("SplitPane.shadow", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("SplitPane.darkShadow", ModernTheme.BACKGROUND_DARKER);

        // JTabbedPane - v2.5.16: Comprehensive dark theme to eliminate white rectangle
        UIManager.put("TabbedPane.background", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("TabbedPane.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("TabbedPane.shadow", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("TabbedPane.darkShadow", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("TabbedPane.light", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("TabbedPane.highlight", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("TabbedPane.focus", ModernTheme.ACCENT_PRIMARY);
        UIManager.put("TabbedPane.selected", ModernTheme.BACKGROUND_DARK);
        UIManager.put("TabbedPane.selectHighlight", ModernTheme.ACCENT_PRIMARY);
        UIManager.put("TabbedPane.tabAreaBackground", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("TabbedPane.contentAreaColor", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("TabbedPane.borderHighlightColor", ModernTheme.BORDER_DEFAULT);
        UIManager.put("TabbedPane.contentOpaque", Boolean.TRUE);
        UIManager.put("TabbedPane.tabsOpaque", Boolean.TRUE);

        // Borders - CRITICAL FIX for light borders
        UIManager.put("TitledBorder.border", BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT));
        UIManager.put("TitledBorder.titleColor", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("Border.color", ModernTheme.BORDER_DEFAULT);

        // Viewport (scroll pane content area)
        UIManager.put("Viewport.background", ModernTheme.BACKGROUND_DARK);
        UIManager.put("Viewport.foreground", ModernTheme.FOREGROUND_PRIMARY);

        // Menu components (if any)
        UIManager.put("Menu.background", ModernTheme.PANEL_BACKGROUND);
        UIManager.put("Menu.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("MenuItem.background", ModernTheme.PANEL_BACKGROUND);
        UIManager.put("MenuItem.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("MenuItem.selectionBackground", ModernTheme.ACCENT_PRIMARY);
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
        UIManager.put("PopupMenu.background", ModernTheme.PANEL_BACKGROUND);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT));

        // List components
        UIManager.put("List.background", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("List.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("List.selectionBackground", ModernTheme.ACCENT_PRIMARY);
        UIManager.put("List.selectionForeground", Color.WHITE);

        // Table components (if any)
        UIManager.put("Table.background", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("Table.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("Table.selectionBackground", ModernTheme.ACCENT_PRIMARY);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Table.gridColor", ModernTheme.BORDER_DEFAULT);
        UIManager.put("TableHeader.background", ModernTheme.PANEL_BACKGROUND);
        UIManager.put("TableHeader.foreground", ModernTheme.FOREGROUND_PRIMARY);
    }

    private void applyLightDialogTheme() {
        // Light theme - comprehensive UIManager settings (v2.3.2)
        Color lightGray = new Color(238, 238, 238);
        Color mediumGray = new Color(200, 200, 200);
        Color accentBlue = new Color(51, 153, 255);

        // Dialog components
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("OptionPane.messageForeground", Color.BLACK);
        UIManager.put("OptionPane.messagebackground", Color.WHITE);

        // Panel and container backgrounds
        UIManager.put("Panel.background", Color.WHITE);
        UIManager.put("control", Color.WHITE);

        // Text fields
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.foreground", Color.BLACK);
        UIManager.put("TextField.caretForeground", Color.BLACK);
        UIManager.put("TextField.selectionBackground", accentBlue);
        UIManager.put("TextField.selectionForeground", Color.WHITE);

        // Text areas
        UIManager.put("TextArea.background", Color.WHITE);
        UIManager.put("TextArea.foreground", Color.BLACK);
        UIManager.put("TextArea.caretForeground", Color.BLACK);
        UIManager.put("TextArea.selectionBackground", accentBlue);
        UIManager.put("TextArea.selectionForeground", Color.WHITE);

        // Labels
        UIManager.put("Label.foreground", Color.BLACK);
        UIManager.put("Label.background", Color.WHITE);

        // Buttons
        UIManager.put("Button.background", lightGray);
        UIManager.put("Button.foreground", Color.BLACK);
        UIManager.put("Button.select", mediumGray);

        // ComboBox
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.foreground", Color.BLACK);
        UIManager.put("ComboBox.selectionBackground", accentBlue);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);

        // Tree
        UIManager.put("Tree.background", Color.WHITE);
        UIManager.put("Tree.foreground", Color.BLACK);
        UIManager.put("Tree.selectionBackground", accentBlue);
        UIManager.put("Tree.selectionForeground", Color.WHITE);

        // ScrollPane
        UIManager.put("ScrollPane.background", Color.WHITE);
        UIManager.put("ScrollPane.foreground", Color.BLACK);

        // ScrollBar
        UIManager.put("ScrollBar.background", lightGray);
        UIManager.put("ScrollBar.track", lightGray);

        // SplitPane
        UIManager.put("SplitPane.background", Color.WHITE);
        UIManager.put("SplitPane.dividerSize", 5);

        // Viewport
        UIManager.put("Viewport.background", Color.WHITE);
        UIManager.put("Viewport.foreground", Color.BLACK);

        // Menu components
        UIManager.put("Menu.background", Color.WHITE);
        UIManager.put("Menu.foreground", Color.BLACK);
        UIManager.put("MenuItem.background", Color.WHITE);
        UIManager.put("MenuItem.foreground", Color.BLACK);
        UIManager.put("MenuItem.selectionBackground", accentBlue);
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
        UIManager.put("PopupMenu.background", Color.WHITE);

        // List components
        UIManager.put("List.background", Color.WHITE);
        UIManager.put("List.foreground", Color.BLACK);
        UIManager.put("List.selectionBackground", accentBlue);
        UIManager.put("List.selectionForeground", Color.WHITE);

        // Table components
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.foreground", Color.BLACK);
        UIManager.put("Table.selectionBackground", accentBlue);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("TableHeader.background", lightGray);
        UIManager.put("TableHeader.foreground", Color.BLACK);
    }

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

    private void updateScrollPaneTheme(Component comp, boolean isDarkTheme) {
        if (comp instanceof JScrollPane) {
            JScrollPane scrollPane = (JScrollPane) comp;
            scrollPane.getViewport().setBackground(isDarkTheme ? ModernTheme.BACKGROUND_DARK : Color.WHITE);
            scrollPane.setBackground(isDarkTheme ? ModernTheme.BACKGROUND_DARK : Color.WHITE);
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                updateScrollPaneTheme(child, isDarkTheme);
            }
        }
    }

    private void updateSplitPaneDividers(Component comp, boolean isDarkTheme) {
        if (comp instanceof JSplitPane) {
            JSplitPane splitPane = (JSplitPane) comp;
            if (splitPane.getUI() instanceof BasicSplitPaneUI) {
                // v2.5.7: Changed from BACKGROUND_DARKER to BORDER_DEFAULT for subtle grey dividers
                Color dividerColor = isDarkTheme ? ModernTheme.BORDER_DEFAULT : new Color(200, 200, 200);
                ((BasicSplitPaneUI) splitPane.getUI()).getDivider().setBackground(dividerColor);
            }
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                updateSplitPaneDividers(child, isDarkTheme);
            }
        }
    }

    public String mapThemeNameToKey(String displayName) {
        switch (displayName) {
            case "Dark":
                return "dark";
            case "VS Code Dark+":
                return "vs";
            case "Monokai":
                return "monokai";
            default:
                return "dark";
        }
    }

    public String getCurrentTheme() {
        return currentTheme;
    }

    public String getSavedThemePreference() {
        return prefs.get(PREF_THEME, "dark");
    }
}
