package com.gaskony.python3.designer.managers;

import com.gaskony.python3.designer.ui.CommandPaletteDialog;

import javax.swing.JButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Frame;

/**
 * Manages the Command Palette (Ctrl+Shift+P) for the Python 3 IDE.
 * Provides VS Code-style keyboard-driven command access with fuzzy search,
 * keyboard navigation, and categorized commands.
 *
 * @since v2.9.0
 */
public class CommandPaletteManager {
    private final Component parentComponent;
    private final CommandActions actions;
    private final JButton importButton;
    private final JButton exportButton;

    private CommandPaletteDialog commandPalette;

    /**
     * Interface defining all actions that can be triggered from the command palette.
     */
    public interface CommandActions {
        void executeCode();
        void clearOutput();
        void saveCurrentScript();
        void saveScriptAs();
        void createNewScript();
        void refreshScriptTree();
        void showFindDialog();
        void showReplaceDialog();
        void showAdvancedFindReplaceDialog();
        void toggleSidebar();
        void changeFontSize(int delta);
        void setFontSize(int size);
        void applyTheme(String themeKey);
        void connectToGateway();
        void openSettingsDialog();
        void showInformationDialog();
        void openPackagesDialog();
    }

    /**
     * Creates a new CommandPaletteManager.
     *
     * @param parentComponent parent component for dialog positioning
     * @param actions actions to be triggered by commands
     * @param importButton import button for triggering import action
     * @param exportButton export button for triggering export action
     */
    public CommandPaletteManager(
        Component parentComponent,
        CommandActions actions,
        JButton importButton,
        JButton exportButton
    ) {
        this.parentComponent = parentComponent;
        this.actions = actions;
        this.importButton = importButton;
        this.exportButton = exportButton;
    }

    /**
     * Shows the command palette dialog.
     * Initializes the palette on first use (lazy initialization).
     */
    public void showCommandPalette() {
        if (commandPalette == null) {
            initializeCommandPalette();
        }
        commandPalette.showPalette();
    }

    /**
     * Initializes the Command Palette with all available commands.
     */
    private void initializeCommandPalette() {
        commandPalette = new CommandPaletteDialog((Frame) SwingUtilities.getWindowAncestor(parentComponent));

        // Script Execution Commands
        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Execute Script",
            "Run the current Python script on the Gateway",
            "Execution",
            actions::executeCode,
            KeyStroke.getKeyStroke("control ENTER")
        ));

        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Clear Output",
            "Clear the output and error panels",
            "Execution",
            actions::clearOutput
        ));

        // Script Management Commands
        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Save Script",
            "Save the current script",
            "File",
            actions::saveCurrentScript,
            KeyStroke.getKeyStroke("control S")
        ));

        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Save Script As...",
            "Save the current script with a new name",
            "File",
            actions::saveScriptAs,
            KeyStroke.getKeyStroke("control shift S")
        ));

        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "New Script",
            "Create a new script",
            "File",
            actions::createNewScript,
            KeyStroke.getKeyStroke("control N")
        ));

        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Import Script...",
            "Import a script from a .py file",
            "File",
            () -> importButton.doClick()
        ));

        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Export Script...",
            "Export the current script to a .py file",
            "File",
            () -> exportButton.doClick()
        ));

        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Refresh Script Tree",
            "Reload all scripts from the Gateway",
            "File",
            actions::refreshScriptTree
        ));

        // Search Commands
        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Find...",
            "Find text in the current script",
            "Search",
            actions::showFindDialog,
            KeyStroke.getKeyStroke("control F")
        ));

        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Replace...",
            "Find and replace text in the current script",
            "Search",
            actions::showReplaceDialog,
            KeyStroke.getKeyStroke("control H")
        ));

        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Advanced Find/Replace...",
            "Find and replace with regex support",
            "Search",
            actions::showAdvancedFindReplaceDialog,
            KeyStroke.getKeyStroke("control shift F")
        ));

        // View Commands
        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Toggle Sidebar",
            "Show/hide the script tree and metadata panels",
            "View",
            actions::toggleSidebar,
            KeyStroke.getKeyStroke("control B")
        ));

        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Increase Font Size",
            "Make the editor font larger",
            "View",
            () -> actions.changeFontSize(1),
            KeyStroke.getKeyStroke("control PLUS")
        ));

        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Decrease Font Size",
            "Make the editor font smaller",
            "View",
            () -> actions.changeFontSize(-1),
            KeyStroke.getKeyStroke("control MINUS")
        ));

        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Reset Font Size",
            "Reset editor font to default size (12pt)",
            "View",
            () -> actions.setFontSize(12),
            KeyStroke.getKeyStroke("control 0")
        ));

        // Theme Commands
        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Switch to Dark Theme",
            "Change editor theme to dark",
            "Theme",
            () -> actions.applyTheme("dark")
        ));

        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Switch to Light Theme",
            "Change editor theme to light",
            "Theme",
            () -> actions.applyTheme("light")
        ));

        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Switch to VS Code Dark+",
            "Change editor theme to VS Code Dark+",
            "Theme",
            () -> actions.applyTheme("vscode")
        ));

        // Gateway Commands
        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Connect to Gateway",
            "Connect to the Ignition Gateway",
            "Gateway",
            actions::connectToGateway
        ));

        // Settings Commands
        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Open Settings",
            "Open IDE settings dialog",
            "Settings",
            actions::openSettingsDialog
        ));

        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Show IDE Info",
            "Display IDE information and version",
            "Help",
            actions::showInformationDialog
        ));

        commandPalette.addCommand(new CommandPaletteDialog.Command(
            "Manage Packages",
            "Install or remove Python packages",
            "Tools",
            actions::openPackagesDialog
        ));
    }
}
