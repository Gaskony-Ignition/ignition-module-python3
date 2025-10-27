package com.inductiveautomation.ignition.examples.python3.designer.managers;

import com.inductiveautomation.ignition.examples.python3.designer.DarkDialog;
import com.inductiveautomation.ignition.examples.python3.designer.ModernStatusBar;
import com.inductiveautomation.ignition.examples.python3.designer.Python3RestClient;
import com.inductiveautomation.ignition.examples.python3.designer.SavedScript;
import com.inductiveautomation.ignition.examples.python3.designer.ScriptMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFileChooser;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages script import and export functionality for the Python 3 IDE.
 * Handles file chooser dialogs, file I/O, and integration with the Gateway REST API.
 *
 * @since v2.8.0
 */
public class ScriptImportExportManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptImportExportManager.class);

    private final Component parentComponent;
    private final ImportExportContext context;
    private final ModernStatusBar statusBar;

    /**
     * Interface to provide context for import/export operations.
     * This allows the manager to access necessary state without tight coupling.
     */
    public interface ImportExportContext {
        /**
         * Gets the REST client for communicating with the Gateway.
         *
         * @return REST client, or null if not connected
         */
        Python3RestClient getRestClient();

        /**
         * Gets the current script being edited.
         *
         * @return current script metadata, or null if no script loaded
         */
        ScriptMetadata getCurrentScript();

        /**
         * Gets the current code from the editor.
         *
         * @return editor text content
         */
        String getCurrentCode();

        /**
         * Saves a script to the Gateway.
         *
         * @param name script name
         * @param code script code
         * @param author author name
         * @param version version string
         * @param description script description
         * @param folderPath folder path
         */
        void saveScript(String name, String code, String author, String version, String description, String folderPath);

        /**
         * Loads a script into the editor.
         *
         * @param scriptName name of script to load
         */
        void loadScript(String scriptName);

        /**
         * Sets the status bar message.
         *
         * @param message status message
         * @param color message color
         */
        void setStatus(String message, Color color);
    }

    /**
     * Creates a new ScriptImportExportManager with dependency injection.
     *
     * @param parentComponent parent component for dialog positioning
     * @param context context provider for accessing IDE state
     * @param statusBar status bar for user feedback
     */
    public ScriptImportExportManager(
        Component parentComponent,
        ImportExportContext context,
        ModernStatusBar statusBar
    ) {
        this.parentComponent = parentComponent;
        this.context = context;
        this.statusBar = statusBar;
    }

    /**
     * Imports a Python script from a .py file.
     * Shows file chooser, reads file, and prompts for script metadata.
     */
    public void importScript() {
        Python3RestClient restClient = context.getRestClient();
        if (restClient == null) {
            DarkDialog.showMessage(
                parentComponent,
                "Please connect to a Gateway first",
                "Not Connected"
            );
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".py");
            }

            @Override
            public String getDescription() {
                return "Python Files (*.py)";
            }
        });

        int result = fileChooser.showOpenDialog(parentComponent);

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
                Map<String, String> fields = new LinkedHashMap<>();
                fields.put("Script Name", scriptName);
                fields.put("Author", System.getProperty("user.name", "Unknown"));
                fields.put("Version", "1.0");
                fields.put("Folder Path", "");
                fields.put("Description", "Imported from " + fileName);

                Map<String, String> inputResult = DarkDialog.showMultiInput(parentComponent, "Import Script", fields);

                if (inputResult != null) {
                    String name = inputResult.get("Script Name").trim();
                    String author = inputResult.get("Author").trim();
                    String version = inputResult.get("Version").trim();
                    String folderPath = inputResult.get("Folder Path").trim();
                    String description = inputResult.get("Description").trim();

                    if (name.isEmpty()) {
                        DarkDialog.showMessage(
                            parentComponent,
                            "Script name cannot be empty",
                            "Invalid Input"
                        );
                        return;
                    }

                    // Save imported script
                    context.saveScript(name, code, author, version, description, folderPath);

                    // Load the imported script
                    context.loadScript(name);

                    statusBar.setStatus("Imported: " + fileName, ModernStatusBar.MessageType.SUCCESS);
                    LOGGER.info("Imported script from: {}", file.getAbsolutePath());
                }

            } catch (IOException e) {
                LOGGER.error("Failed to import script", e);
                DarkDialog.showMessage(
                    parentComponent,
                    "Failed to import script: " + e.getMessage(),
                    "Error"
                );
            }
        }
    }

    /**
     * Exports the current script to a .py file.
     * Shows file chooser and writes current editor content.
     */
    public void exportCurrentScript() {
        String code = context.getCurrentCode().trim();

        if (code.isEmpty()) {
            DarkDialog.showMessage(
                parentComponent,
                "Cannot export empty code",
                "Empty Code"
            );
            return;
        }

        JFileChooser fileChooser = new JFileChooser();

        // Default filename based on current script name or generic
        ScriptMetadata currentScript = context.getCurrentScript();
        String defaultName = (currentScript != null && currentScript.getName() != null) ?
            currentScript.getName() + ".py" : "script.py";
        fileChooser.setSelectedFile(new File(defaultName));

        int result = fileChooser.showSaveDialog(parentComponent);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            // Ensure .py extension
            if (!file.getName().endsWith(".py")) {
                file = new File(file.getAbsolutePath() + ".py");
            }

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(code);
                context.setStatus("Exported: " + file.getName(), new Color(0, 128, 0));
                LOGGER.info("Exported script to: {}", file.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("Failed to export script", e);
                DarkDialog.showMessage(
                    parentComponent,
                    "Failed to export script: " + e.getMessage(),
                    "Error"
                );
            }
        }
    }

    /**
     * Exports a specific script from the Gateway to a .py file.
     * Loads the script from Gateway first, then shows file chooser.
     *
     * @param scriptMetadata metadata of the script to export
     */
    public void exportScript(ScriptMetadata scriptMetadata) {
        Python3RestClient restClient = context.getRestClient();
        if (restClient == null) {
            DarkDialog.showMessage(
                parentComponent,
                "Please connect to a Gateway first",
                "Not Connected"
            );
            return;
        }

        SwingWorker<SavedScript, Void> worker = new SwingWorker<SavedScript, Void>() {
            @Override
            protected SavedScript doInBackground() throws Exception {
                return restClient.loadScript(scriptMetadata.getName());
            }

            @Override
            protected void done() {
                try {
                    SavedScript script = get();

                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setSelectedFile(new File(script.getName() + ".py"));

                    int result = fileChooser.showSaveDialog(parentComponent);

                    if (result == JFileChooser.APPROVE_OPTION) {
                        File file = fileChooser.getSelectedFile();

                        try (FileWriter writer = new FileWriter(file)) {
                            writer.write(script.getCode());
                            context.setStatus("Exported: " + file.getName(), new Color(0, 128, 0));
                        }
                    }

                } catch (Exception e) {
                    LOGGER.error("Failed to export script", e);
                    DarkDialog.showMessage(
                        parentComponent,
                        "Failed to export script: " + e.getMessage(),
                        "Error"
                    );
                }
            }
        };

        worker.execute();
    }
}
