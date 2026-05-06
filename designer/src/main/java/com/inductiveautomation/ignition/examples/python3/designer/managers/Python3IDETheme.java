package com.inductiveautomation.ignition.examples.python3.designer.managers;

import com.inductiveautomation.ignition.examples.python3.designer.ComponentThemeHelper;
import com.inductiveautomation.ignition.examples.python3.designer.DarkDialog;
import com.inductiveautomation.ignition.examples.python3.designer.DiagnosticsPanel;
import com.inductiveautomation.ignition.examples.python3.designer.ModernButton;
import com.inductiveautomation.ignition.examples.python3.designer.ModernTheme;
import com.inductiveautomation.ignition.examples.python3.designer.PreferenceKeys;
import com.inductiveautomation.ignition.examples.python3.designer.ScriptMetadataPanel;
import com.inductiveautomation.ignition.examples.python3.designer.TerminalPanel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.io.IOException;
import java.util.prefs.Preferences;

/**
 * Manages theme application for the Python 3 IDE.
 *
 * <p>Extracted from {@code Python3IDE} in v3.13 to keep the IDE class
 * focused on orchestration. This class owns:</p>
 * <ul>
 *   <li>RSyntaxTextArea theme loading (dark/monokai/eclipse/idea/vs/druid/default)</li>
 *   <li>Wholesale dark/light recolouring of all major components (editor, output,
 *       error, tree, gateway URL field, buttons, panels, splits)</li>
 *   <li>Popup-menu styling that respects the current theme</li>
 *   <li>TitledBorder colour updates (recursive)</li>
 *   <li>Persisting the active theme in {@code Preferences}</li>
 * </ul>
 *
 * <p>Pure structural refactor &mdash; behaviour identical to the inlined version
 * pre-v3.13.</p>
 *
 * @since v3.13
 */
public class Python3IDETheme {
    private static final Logger logger = LoggerFactory.getLogger(Python3IDETheme.class);

    /**
     * Callback the IDE supplies so the theme manager can read &amp; mutate the
     * specific UI fields without holding a reference to the whole panel.
     * Each accessor is a focused, side-effect-free read.
     */
    public interface IdeContext {
        JComponent getRoot();
        RSyntaxTextArea getCodeEditor();
        JTextArea getOutputArea();
        JTextArea getErrorArea();
        JTree getScriptTree();
        JTextField getGatewayUrlField();
        JLabel getCurrentScriptLabel();
        JButton getConnectButton();
        JButton getExecuteButton();
        JButton getSaveButton();
        JButton getSaveAsButton();
        JButton getImportButton();
        JButton getExportButton();
        JButton getNewFolderBtn();
        JButton getNewScriptBtn();
        JButton getRefreshBtn();
        ScriptMetadataPanel getMetadataPanel();
        DiagnosticsPanel getDiagnosticsPanel();
        TerminalPanel getTerminalPanel();
        void setStatus(String message, Color color);
        void setUseDarkTheme(boolean isDark);
    }

    private final IdeContext ctx;
    private String currentTheme;

    public Python3IDETheme(IdeContext ctx, String initialTheme) {
        this.ctx = ctx;
        this.currentTheme = initialTheme;
    }

    public String getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Public method to apply theme by display name (called from SettingsDialog).
     *
     * @param displayName user-friendly theme name like "Dark", "VS Code Dark+"
     */
    public void applyThemeByName(String displayName) {
        applyTheme(mapThemeNameToKey(displayName));
    }

    /**
     * Maps user-friendly theme names to internal theme keys.
     */
    public static String mapThemeNameToKey(String displayName) {
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
    public void applyTheme(String themeName) {
        try {
            Theme theme;
            boolean isDarkTheme;

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
                case "druid":
                    theme = Theme.load(getClass().getResourceAsStream(
                        "/org/fife/ui/rsyntaxtextarea/themes/druid.xml"));
                    isDarkTheme = true;
                    break;
                default:
                    theme = Theme.load(getClass().getResourceAsStream(
                        "/org/fife/ui/rsyntaxtextarea/themes/default.xml"));
                    isDarkTheme = false;
                    break;
            }

            theme.apply(ctx.getCodeEditor());
            ctx.setUseDarkTheme(isDarkTheme);

            if (isDarkTheme) {
                applyDarkComponentColours();
            } else {
                applyLightComponentColours();
            }

            JComponent root = ctx.getRoot();
            SwingUtilities.updateComponentTreeUI(root);
            ComponentThemeHelper.updateScrollPaneTheme(root, isDarkTheme);
            ComponentThemeHelper.updateSplitPaneDividers(root, isDarkTheme);

            TerminalPanel terminalPanel = ctx.getTerminalPanel();
            if (terminalPanel != null) {
                terminalPanel.setTheme(isDarkTheme);
            }

            currentTheme = themeName;
            Preferences prefs = Preferences.userNodeForPackage(
                com.inductiveautomation.ignition.examples.python3.designer.Python3IDE.class);
            prefs.put(PreferenceKeys.IDE_THEME, themeName);

            ctx.setStatus("Theme changed: " + themeName, ModernTheme.SUCCESS);
            logger.info("Applied theme: {}", themeName);
        } catch (IOException e) {
            logger.error("Failed to apply theme: {}", themeName, e);
            ctx.setStatus("Failed to apply theme: " + themeName, Color.RED);
        }
    }

    private void applyDarkComponentColours() {
        DarkDialog.setDarkTheme(true);

        JTextArea outputArea = ctx.getOutputArea();
        outputArea.setBackground(ModernTheme.BACKGROUND_DARKER);
        outputArea.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        outputArea.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);

        JTextArea errorArea = ctx.getErrorArea();
        errorArea.setBackground(ModernTheme.BACKGROUND_DARKER);
        errorArea.setForeground(ModernTheme.ERROR);
        errorArea.setCaretColor(ModernTheme.ERROR);

        JTree scriptTree = ctx.getScriptTree();
        scriptTree.setBackground(ModernTheme.TREE_BACKGROUND);
        scriptTree.setForeground(ModernTheme.FOREGROUND_PRIMARY);

        JTextField gatewayUrlField = ctx.getGatewayUrlField();
        gatewayUrlField.setBackground(ModernTheme.BACKGROUND_DARKER);
        gatewayUrlField.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        gatewayUrlField.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);

        ctx.getCurrentScriptLabel().setForeground(ModernTheme.FOREGROUND_SECONDARY);

        updateButtonTheme(ctx.getConnectButton(), ModernTheme.ACCENT_PRIMARY,
            ModernTheme.ACCENT_HOVER, ModernTheme.ACCENT_ACTIVE);
        updateButtonTheme(ctx.getExecuteButton(), ModernTheme.ACCENT_PRIMARY,
            ModernTheme.ACCENT_HOVER, ModernTheme.ACCENT_ACTIVE);
        updateButtonTheme(ctx.getSaveButton(), ModernTheme.SUCCESS,
            ModernTheme.lighten(ModernTheme.SUCCESS, 0.1),
            ModernTheme.darken(ModernTheme.SUCCESS, 0.1));
        updateButtonTheme(ctx.getSaveAsButton(), ModernTheme.BUTTON_BACKGROUND,
            ModernTheme.BUTTON_HOVER, ModernTheme.BUTTON_ACTIVE);
        updateButtonTheme(ctx.getImportButton(), ModernTheme.BUTTON_BACKGROUND,
            ModernTheme.BUTTON_HOVER, ModernTheme.BUTTON_ACTIVE);
        updateButtonTheme(ctx.getExportButton(), ModernTheme.BUTTON_BACKGROUND,
            ModernTheme.BUTTON_HOVER, ModernTheme.BUTTON_ACTIVE);
        updateButtonTheme(ctx.getNewFolderBtn(), ModernTheme.BUTTON_BACKGROUND,
            ModernTheme.BUTTON_HOVER, ModernTheme.BUTTON_ACTIVE);
        updateButtonTheme(ctx.getNewScriptBtn(), ModernTheme.BUTTON_BACKGROUND,
            ModernTheme.BUTTON_HOVER, ModernTheme.BUTTON_ACTIVE);
        updateButtonTheme(ctx.getRefreshBtn(), ModernTheme.BUTTON_BACKGROUND,
            ModernTheme.BUTTON_HOVER, ModernTheme.BUTTON_ACTIVE);

        ctx.getMetadataPanel().applyTheme(true);
        DiagnosticsPanel diagnostics = ctx.getDiagnosticsPanel();
        if (diagnostics != null) {
            diagnostics.applyTheme(true);
        }

        updateTitledBorders(ctx.getRoot(), true);
        ComponentThemeHelper.updatePanelBackgrounds(ctx.getRoot(), ModernTheme.BACKGROUND_DARK);
    }

    private void applyLightComponentColours() {
        DarkDialog.setDarkTheme(false);

        JTextArea outputArea = ctx.getOutputArea();
        outputArea.setBackground(ModernTheme.LIGHT_BACKGROUND);
        outputArea.setForeground(Color.BLACK);
        outputArea.setCaretColor(Color.BLACK);

        JTextArea errorArea = ctx.getErrorArea();
        errorArea.setBackground(ModernTheme.LIGHT_BACKGROUND);
        errorArea.setForeground(ModernTheme.ERROR);
        errorArea.setCaretColor(ModernTheme.ERROR);

        JTree scriptTree = ctx.getScriptTree();
        scriptTree.setBackground(ModernTheme.LIGHT_TREE_BG);
        scriptTree.setForeground(Color.BLACK);

        JTextField gatewayUrlField = ctx.getGatewayUrlField();
        gatewayUrlField.setBackground(ModernTheme.LIGHT_BACKGROUND);
        gatewayUrlField.setForeground(Color.BLACK);
        gatewayUrlField.setCaretColor(Color.BLACK);

        ctx.getCurrentScriptLabel().setForeground(ModernTheme.FOREGROUND_MUTED);

        Color lightDefault = ModernTheme.LIGHT_BACKGROUND_LIGHT;
        Color lightDefaultHover = ModernTheme.LIGHT_BUTTON_BG;
        Color lightDefaultActive = ModernTheme.LIGHT_BUTTON_HOVER;

        updateButtonTheme(ctx.getConnectButton(), ModernTheme.LIGHT_PRIMARY,
            ModernTheme.LIGHT_PRIMARY_HOVER, ModernTheme.LIGHT_PRIMARY_ACTIVE);
        updateButtonTheme(ctx.getExecuteButton(), ModernTheme.LIGHT_PRIMARY,
            ModernTheme.LIGHT_PRIMARY_HOVER, ModernTheme.LIGHT_PRIMARY_ACTIVE);
        updateButtonTheme(ctx.getSaveButton(), ModernTheme.LIGHT_SUCCESS,
            ModernTheme.LIGHT_SUCCESS_HOVER, ModernTheme.LIGHT_SUCCESS_ACTIVE);
        updateButtonTheme(ctx.getSaveAsButton(), lightDefault, lightDefaultHover, lightDefaultActive);
        updateButtonTheme(ctx.getImportButton(), lightDefault, lightDefaultHover, lightDefaultActive);
        updateButtonTheme(ctx.getExportButton(), lightDefault, lightDefaultHover, lightDefaultActive);
        updateButtonTheme(ctx.getNewFolderBtn(), lightDefault, lightDefaultHover, lightDefaultActive);
        updateButtonTheme(ctx.getNewScriptBtn(), lightDefault, lightDefaultHover, lightDefaultActive);
        updateButtonTheme(ctx.getRefreshBtn(), lightDefault, lightDefaultHover, lightDefaultActive);

        ctx.getMetadataPanel().applyTheme(false);
        DiagnosticsPanel diagnostics = ctx.getDiagnosticsPanel();
        if (diagnostics != null) {
            diagnostics.applyTheme(false);
        }

        updateTitledBorders(ctx.getRoot(), false);
        ComponentThemeHelper.updatePanelBackgrounds(ctx.getRoot(), ModernTheme.LIGHT_BACKGROUND);
    }

    /**
     * Styles a popup menu to match the current theme.
     */
    public void stylePopupMenu(JPopupMenu menu, boolean isDark) {
        if (isDark) {
            menu.setBackground(ModernTheme.BACKGROUND_DARK);
            menu.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            menu.setBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT, 1));

            for (Component comp : menu.getComponents()) {
                if (comp instanceof JMenuItem) {
                    JMenuItem item = (JMenuItem) comp;
                    item.setBackground(ModernTheme.BACKGROUND_DARK);
                    item.setForeground(ModernTheme.FOREGROUND_PRIMARY);
                    item.setFont(ModernTheme.FONT_REGULAR);
                    item.putClientProperty("MenuItem.selectionBackground", ModernTheme.BUTTON_HOVER);
                    item.putClientProperty("MenuItem.selectionForeground", Color.WHITE);
                }
            }
        } else {
            menu.setBackground(ModernTheme.LIGHT_BACKGROUND);
            menu.setForeground(Color.BLACK);
            menu.setBorder(BorderFactory.createLineBorder(ModernTheme.LIGHT_BORDER, 1));

            for (Component comp : menu.getComponents()) {
                if (comp instanceof JMenuItem) {
                    JMenuItem item = (JMenuItem) comp;
                    item.setBackground(ModernTheme.LIGHT_BACKGROUND);
                    item.setForeground(Color.BLACK);
                    item.setFont(ModernTheme.FONT_REGULAR);
                    item.putClientProperty("MenuItem.selectionBackground", ModernTheme.LIGHT_SELECTION);
                    item.putClientProperty("MenuItem.selectionForeground", Color.BLACK);
                }
            }
        }
    }

    /**
     * Creates a themed button for dialogs.
     */
    public static JButton createThemedDialogButton(String text, boolean useDarkTheme) {
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

        Color hoverBg = useDarkTheme ? ModernTheme.BUTTON_HOVER : ModernTheme.LIGHT_BUTTON_HOVER;
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverBg);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(buttonBg);
            }
        });

        return button;
    }

    /**
     * Updates a ModernButton's color scheme.
     */
    public static void updateButtonTheme(JButton button, Color normal, Color hover, Color pressed) {
        if (button instanceof ModernButton) {
            ModernButton modernButton = (ModernButton) button;
            modernButton.setNormalBackground(normal);
            modernButton.setHoverBackground(hover);
            modernButton.setPressedBackground(pressed);
            modernButton.repaint();
        }
    }

    /**
     * Recursively updates all TitledBorder components to match the current theme.
     */
    public static void updateTitledBorders(Component comp, boolean isDarkTheme) {
        if (comp instanceof JComponent) {
            JComponent jcomp = (JComponent) comp;
            Border border = jcomp.getBorder();

            if (border instanceof CompoundBorder) {
                CompoundBorder compoundBorder = (CompoundBorder) border;
                Border outerBorder = compoundBorder.getOutsideBorder();

                if (outerBorder instanceof TitledBorder) {
                    TitledBorder titledBorder = (TitledBorder) outerBorder;
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
                if (isDarkTheme) {
                    titledBorder.setTitleColor(ModernTheme.FOREGROUND_PRIMARY);
                    titledBorder.setBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT));
                } else {
                    titledBorder.setTitleColor(Color.BLACK);
                    titledBorder.setBorder(BorderFactory.createLineBorder(ModernTheme.LIGHT_BORDER));
                }
            }
        }

        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                updateTitledBorders(child, isDarkTheme);
            }
        }
    }

    /**
     * Recursively applies dark theme colours to all components in a container.
     * Kept public-static for any historical caller; the IDE no longer calls it
     * directly post-extraction but the helper is available for future use.
     */
    public static void setComponentsDark(Container container) {
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
}
