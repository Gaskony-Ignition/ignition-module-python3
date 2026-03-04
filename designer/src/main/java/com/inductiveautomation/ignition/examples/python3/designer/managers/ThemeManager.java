package com.inductiveautomation.ignition.examples.python3.designer.managers;

import com.inductiveautomation.ignition.examples.python3.designer.ComponentThemeHelper;
import com.inductiveautomation.ignition.examples.python3.designer.ModernTheme;
import com.inductiveautomation.ignition.examples.python3.designer.PreferenceKeys;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JTextArea;
import javax.swing.JTree;
import java.awt.Color;
import java.awt.Component;
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
    private static final String PREF_THEME = PreferenceKeys.IDE_THEME;

    private String currentTheme;
    private final Preferences prefs;

    public ThemeManager(Class<?> prefsClass) {
        this.prefs = Preferences.userNodeForPackage(prefsClass);
        this.currentTheme = prefs.get(PREF_THEME, "dark");
    }

    /**
     * Applies theme to IDE components.
     * Always updates currentTheme and preferences, even if RSTA theme loading fails.
     */
    public void applyTheme(String themeName, Component rootComponent, RSyntaxTextArea codeEditor,
                           JTextArea outputArea, JTextArea errorArea, JTree scriptTree) throws IOException {
        // Always update the tracked theme state first so toggle works correctly
        currentTheme = themeName;
        prefs.put(PREF_THEME, themeName);

        boolean isDarkTheme = false;

        switch (themeName.toLowerCase()) {
            case "dark":
            case "monokai":
            case "vs":
                isDarkTheme = true;
                break;
            default:
                isDarkTheme = false;
                break;
        }

        // Try to load and apply RSTA syntax theme (may fail due to classloader issues)
        try {
            java.io.InputStream themeStream;
            if (isDarkTheme) {
                themeStream = getClass().getResourceAsStream("/themes/python3-dark.xml");
            } else {
                themeStream = getClass().getResourceAsStream(
                        "/org/fife/ui/rsyntaxtextarea/themes/default.xml");
            }
            if (themeStream != null) {
                Theme theme = Theme.load(themeStream);
                theme.apply(codeEditor);
            } else {
                LOGGER.warn("Theme resource stream is null for '{}', skipping RSTA theme", themeName);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load RSTA theme '{}': {}", themeName, e.getMessage());
        }

        if (isDarkTheme) {
            applyDarkTheme(outputArea, errorArea, scriptTree, rootComponent);
        } else {
            applyLightTheme(outputArea, errorArea, scriptTree, rootComponent);
        }

        ComponentThemeHelper.updateScrollPaneTheme(rootComponent, isDarkTheme);
        ComponentThemeHelper.updateSplitPaneDividers(rootComponent, isDarkTheme);

        LOGGER.info("Applied theme: {}", themeName);
    }

    private void applyDarkTheme(JTextArea outputArea, JTextArea errorArea, JTree scriptTree, Component root) {
        if (outputArea != null) {
            outputArea.setBackground(ModernTheme.BACKGROUND_DARKER);
            outputArea.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            outputArea.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);
        }

        if (errorArea != null) {
            errorArea.setBackground(ModernTheme.BACKGROUND_DARKER);
            errorArea.setForeground(ModernTheme.ERROR);
            errorArea.setCaretColor(ModernTheme.ERROR);
        }

        if (scriptTree != null) {
            scriptTree.setBackground(ModernTheme.TREE_BACKGROUND);
            scriptTree.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        }

        ComponentThemeHelper.updatePanelBackgrounds(root, ModernTheme.BACKGROUND_DARK);
        // NOTE: Do NOT call UIManager.put() here - that sets GLOBAL Swing defaults which
        // affects the entire Ignition Designer, not just our Script Console window.
        // All theming is done via direct component setBackground()/setForeground() calls.
    }

    private void applyLightTheme(JTextArea outputArea, JTextArea errorArea, JTree scriptTree, Component root) {
        if (outputArea != null) {
            outputArea.setBackground(Color.WHITE);
            outputArea.setForeground(Color.BLACK);
            outputArea.setCaretColor(Color.BLACK);
        }

        if (errorArea != null) {
            errorArea.setBackground(Color.WHITE);
            errorArea.setForeground(ModernTheme.ERROR_LIGHT);
            errorArea.setCaretColor(ModernTheme.ERROR_LIGHT);
        }

        if (scriptTree != null) {
            scriptTree.setBackground(Color.WHITE);
            scriptTree.setForeground(Color.BLACK);
        }

        ComponentThemeHelper.updatePanelBackgrounds(root, Color.WHITE);
        // NOTE: Do NOT call UIManager.put() here - that sets GLOBAL Swing defaults which
        // affects the entire Ignition Designer, not just our Script Console window.
        // All theming is done via direct component setBackground()/setForeground() calls.
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
