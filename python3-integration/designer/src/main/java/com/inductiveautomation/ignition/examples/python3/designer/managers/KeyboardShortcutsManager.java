package com.inductiveautomation.ignition.examples.python3.designer.managers;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;

/**
 * Manages keyboard shortcuts for the Python 3 IDE.
 * Registers all keyboard shortcuts and maps them to actions.
 *
 * @since v2.8.0
 */
public class KeyboardShortcutsManager {
    private final JComponent component;
    private final ShortcutActions actions;

    /**
     * Interface defining all actions that can be triggered by keyboard shortcuts.
     */
    public interface ShortcutActions {
        void executeCode();
        void saveCurrentScript();
        void saveScriptAs();
        void createNewScript();
        void changeFontSize(int delta);
        void setFontSize(int size);
        void showFindDialog();
        void showReplaceDialog();
        void showAdvancedFindReplaceDialog();
        void showCommandPalette();
        void toggleSidebar();
    }

    /**
     * Creates a new KeyboardShortcutsManager.
     *
     * @param component component to attach keyboard shortcuts to
     * @param actions actions to be triggered by shortcuts
     */
    public KeyboardShortcutsManager(JComponent component, ShortcutActions actions) {
        this.component = component;
        this.actions = actions;
    }

    /**
     * Registers all keyboard shortcuts.
     */
    public void setupKeyboardShortcuts() {
        InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = component.getActionMap();

        // Ctrl+Enter: Execute
        registerShortcut(inputMap, actionMap, "control ENTER", "execute",
            e -> actions.executeCode());

        // Ctrl+S: Save
        registerShortcut(inputMap, actionMap, "control S", "save",
            e -> actions.saveCurrentScript());

        // Ctrl+Shift+S: Save As
        registerShortcut(inputMap, actionMap, "control shift S", "saveAs",
            e -> actions.saveScriptAs());

        // Ctrl+N: New Script
        registerShortcut(inputMap, actionMap, "control N", "newScript",
            e -> actions.createNewScript());

        // Ctrl++: Increase font
        registerShortcut(inputMap, actionMap, "control PLUS", "increaseFontSize",
            e -> actions.changeFontSize(1));

        // Ctrl+-: Decrease font
        registerShortcut(inputMap, actionMap, "control MINUS", "decreaseFontSize",
            e -> actions.changeFontSize(-1));

        // Ctrl+0: Reset font
        registerShortcut(inputMap, actionMap, "control 0", "resetFontSize",
            e -> actions.setFontSize(12));

        // Ctrl+F: Find
        registerShortcut(inputMap, actionMap, "control F", "find",
            e -> actions.showFindDialog());

        // Ctrl+H: Replace
        registerShortcut(inputMap, actionMap, "control H", "replace",
            e -> actions.showReplaceDialog());

        // Ctrl+Shift+F: Advanced Find/Replace
        registerShortcut(inputMap, actionMap, "control shift F", "advancedFindReplace",
            e -> actions.showAdvancedFindReplaceDialog());

        // Ctrl+Shift+P: Command Palette (v2.8.0)
        registerShortcut(inputMap, actionMap, "control shift P", "commandPalette",
            e -> actions.showCommandPalette());

        // Ctrl+B: Toggle Sidebar (v2.8.0)
        registerShortcut(inputMap, actionMap, "control B", "toggleSidebar",
            e -> actions.toggleSidebar());
    }

    /**
     * Registers a keyboard shortcut.
     *
     * @param inputMap input map to register keystroke
     * @param actionMap action map to register action
     * @param keyStrokeString keystroke string (e.g., "control ENTER")
     * @param actionKey action key identifier
     * @param action action to perform
     */
    private void registerShortcut(
        InputMap inputMap,
        ActionMap actionMap,
        String keyStrokeString,
        String actionKey,
        ActionPerformer action
    ) {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyStrokeString);
        inputMap.put(keyStroke, actionKey);
        actionMap.put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.perform(e);
            }
        });
    }

    /**
     * Functional interface for action performers.
     */
    @FunctionalInterface
    private interface ActionPerformer {
        void perform(ActionEvent e);
    }

    /**
     * Gets all registered shortcuts as a description.
     *
     * @return array of shortcut descriptions
     */
    public String[] getShortcutDescriptions() {
        return new String[] {
            "Ctrl+Enter - Execute code",
            "Ctrl+S - Save script",
            "Ctrl+Shift+S - Save script as",
            "Ctrl+N - New script",
            "Ctrl++ - Increase font size",
            "Ctrl+- - Decrease font size",
            "Ctrl+0 - Reset font size",
            "Ctrl+F - Find",
            "Ctrl+H - Replace",
            "Ctrl+Shift+F - Advanced Find/Replace",
            "Ctrl+Shift+P - Command Palette",
            "Ctrl+B - Toggle Sidebar"
        };
    }
}
