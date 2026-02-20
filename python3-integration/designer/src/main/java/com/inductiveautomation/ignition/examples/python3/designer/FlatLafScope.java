package com.inductiveautomation.ignition.examples.python3.designer;

import com.formdev.flatlaf.FlatDarkLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;

/**
 * Scoping utility for FlatLaf Look and Feel.
 *
 * Ensures FlatLaf only affects Python 3 module windows, not the Ignition Designer itself.
 * Components created during a FlatLaf scope retain modern styling after L&amp;F is restored.
 *
 * @since v3.6.0
 */
public final class FlatLafScope {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlatLafScope.class);

    private FlatLafScope() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * Executes an action within a FlatDarkLaf scope.
     *
     * Saves the current L&amp;F, sets FlatDarkLaf with custom defaults,
     * runs the action, then restores the original L&amp;F.
     * Components created during the scope retain FlatLaf styling.
     *
     * @param action the action to run under FlatDarkLaf
     */
    public static void withFlatLafDark(Runnable action) {
        LookAndFeel originalLaf = UIManager.getLookAndFeel();
        try {
            configureFlatLafDefaults();
            FlatDarkLaf.setup();
            action.run();
        } catch (Exception e) {
            LOGGER.warn("Error during FlatLaf scope, running action with current L&F", e);
            try {
                action.run();
            } catch (Exception inner) {
                LOGGER.error("Action also failed without FlatLaf", inner);
            }
        } finally {
            try {
                UIManager.setLookAndFeel(originalLaf);
            } catch (Exception e) {
                LOGGER.warn("Failed to restore original Look and Feel", e);
            }
        }
    }

    /**
     * Configures FlatLaf UIManager properties to match the web UI color palette.
     * Called before FlatDarkLaf.setup() to customize defaults.
     */
    private static void configureFlatLafDefaults() {
        // Arc radii for rounded components
        UIManager.put("Button.arc", 6);
        UIManager.put("Component.arc", 6);
        UIManager.put("TextComponent.arc", 6);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);

        // Scrollbar sizing
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.showButtons", false);

        // Backgrounds matching web palette
        UIManager.put("Panel.background", ModernTheme.PANEL_BACKGROUND);
        UIManager.put("TextField.background", ModernTheme.INPUT_BACKGROUND);
        UIManager.put("ComboBox.background", ModernTheme.INPUT_BACKGROUND);
        UIManager.put("Button.background", ModernTheme.BUTTON_BACKGROUND);
        UIManager.put("SplitPane.dividerSize", 5);

        // Selection colors
        UIManager.put("List.selectionBackground", ModernTheme.ACCENT_PRIMARY);
        UIManager.put("Tree.selectionBackground", ModernTheme.TREE_SELECTION);
        UIManager.put("Table.selectionBackground", ModernTheme.ACCENT_PRIMARY);

        // Focus indicator
        UIManager.put("Component.focusColor", ModernTheme.ACCENT_PRIMARY);
        UIManager.put("Component.focusWidth", 1);

        LOGGER.debug("FlatLaf defaults configured for Python 3 module");
    }
}
