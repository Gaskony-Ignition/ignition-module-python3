package com.inductiveautomation.ignition.examples.python3.designer;

import com.formdev.flatlaf.FlatDarkLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Scoping utility for FlatLaf Look and Feel.
 *
 * Ensures FlatLaf only affects Python 3 module windows, not the Ignition Designer itself.
 * Components created during a FlatLaf scope retain modern styling after L&amp;F is restored.
 *
 * v3.6.8: Fixed theme pollution - all UIManager property changes are now saved and restored
 * to prevent affecting other Designer components.
 *
 * @since v3.6.0
 */
public final class FlatLafScope {

    private static final Logger logger = LoggerFactory.getLogger(FlatLafScope.class);

    /** All UIManager keys that this scope modifies - must be saved/restored. */
    private static final String[] MANAGED_KEYS = {
        "Button.arc", "Component.arc", "TextComponent.arc",
        "ScrollBar.thumbArc", "ScrollBar.trackArc",
        "ScrollBar.width", "ScrollBar.thumbInsets", "ScrollBar.showButtons",
        "Panel.background", "TextField.background", "ComboBox.background",
        "Button.background", "SplitPane.dividerSize",
        "List.selectionBackground", "Tree.selectionBackground", "Table.selectionBackground",
        "Component.focusColor", "Component.focusWidth"
    };

    private FlatLafScope() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * Executes an action within a FlatDarkLaf scope.
     *
     * Saves the current L&amp;F and all UIManager properties this scope modifies,
     * sets FlatDarkLaf with custom defaults, runs the action, then restores
     * the original L&amp;F and all saved properties.
     * Components created during the scope retain FlatLaf styling.
     *
     * @param action the action to run under FlatDarkLaf
     */
    public static void withFlatLafDark(Runnable action) {
        LookAndFeel originalLaf = UIManager.getLookAndFeel();
        Map<String, Object> savedProperties = saveUIManagerProperties();

        try {
            configureFlatLafDefaults();
            FlatDarkLaf.setup();
            action.run();
        } catch (Exception e) {
            logger.warn("Error during FlatLaf scope, running action with current L&F", e);
            try {
                action.run();
            } catch (Exception inner) {
                logger.error("Action also failed without FlatLaf", inner);
            }
        } finally {
            try {
                UIManager.setLookAndFeel(originalLaf);
            } catch (Exception e) {
                logger.warn("Failed to restore original Look and Feel", e);
            }
            // Restore all UIManager properties that were modified
            restoreUIManagerProperties(savedProperties);
        }
    }

    /**
     * Saves the current values of all UIManager properties that will be modified.
     *
     * @return map of property key to saved value (null if property was not set)
     */
    private static Map<String, Object> saveUIManagerProperties() {
        Map<String, Object> saved = new HashMap<>();
        for (String key : MANAGED_KEYS) {
            saved.put(key, UIManager.get(key));
        }
        return saved;
    }

    /**
     * Restores previously saved UIManager properties.
     *
     * @param savedProperties the properties to restore
     */
    private static void restoreUIManagerProperties(Map<String, Object> savedProperties) {
        for (Map.Entry<String, Object> entry : savedProperties.entrySet()) {
            if (entry.getValue() == null) {
                // Property was not set before - remove it
                UIManager.put(entry.getKey(), null);
            } else {
                UIManager.put(entry.getKey(), entry.getValue());
            }
        }
        logger.debug("Restored {} UIManager properties after FlatLaf scope", savedProperties.size());
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

        logger.debug("FlatLaf defaults configured for Python 3 module");
    }
}
