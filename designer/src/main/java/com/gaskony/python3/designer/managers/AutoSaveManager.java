package com.gaskony.python3.designer.managers;

import com.gaskony.python3.designer.ModernStatusBar;
import com.gaskony.python3.designer.ScriptMetadata;
import com.gaskony.python3.designer.UnsavedChangesTracker;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Timer;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * Manages auto-save functionality for the Python 3 IDE.
 * Automatically saves scripts every 30 seconds to prevent data loss.
 * Keeps the last 5 autosave files per script.
 *
 * @since v2.8.0
 */
public class AutoSaveManager {
    private static final Logger logger = LoggerFactory.getLogger(AutoSaveManager.class);
    private static final int AUTO_SAVE_INTERVAL_MS = 30000;  // 30 seconds
    private static final int MAX_AUTOSAVE_FILES = 5;
    private static final String AUTOSAVE_DIR_NAME = ".python3ide/autosave";

    private final RSyntaxTextArea codeEditor;
    private final UnsavedChangesTracker changesTracker;
    private final ModernStatusBar statusBar;
    private final AutoSaveContext context;

    private Timer autoSaveTimer;

    /**
     * Interface to provide context for auto-save operations.
     * This allows the manager to check current script and connection state without tight coupling.
     */
    public interface AutoSaveContext {
        /**
         * Gets the current script being edited.
         *
         * @return current script metadata, or null if no script loaded
         */
        ScriptMetadata getCurrentScript();

        /**
         * Checks if connected to Gateway.
         *
         * @return true if REST client is connected
         */
        boolean isConnectedToGateway();
    }

    /**
     * Creates a new AutoSaveManager with dependency injection.
     *
     * @param codeEditor code editor to save from
     * @param changesTracker tracker for unsaved changes
     * @param statusBar status bar for user feedback
     * @param context context provider for script and connection state
     */
    public AutoSaveManager(
        RSyntaxTextArea codeEditor,
        UnsavedChangesTracker changesTracker,
        ModernStatusBar statusBar,
        AutoSaveContext context
    ) {
        this.codeEditor = codeEditor;
        this.changesTracker = changesTracker;
        this.statusBar = statusBar;
        this.context = context;
    }

    /**
     * Initializes the auto-save timer.
     * Starts automatic saves every 30 seconds if there are unsaved changes.
     */
    public void initialize() {
        autoSaveTimer = new Timer(AUTO_SAVE_INTERVAL_MS, e -> performAutoSave());
        autoSaveTimer.start();
        logger.info("Auto-save initialized (interval: {}ms)", AUTO_SAVE_INTERVAL_MS);
    }

    /**
     * Shuts down the auto-save timer.
     * Call this when closing the IDE to prevent memory leaks.
     */
    public void shutdown() {
        if (autoSaveTimer != null) {
            autoSaveTimer.stop();
            autoSaveTimer = null;
            logger.info("Auto-save shutdown complete");
        }
    }

    /**
     * Performs auto-save if there are unsaved changes.
     * Saves to ~/.python3ide/autosave/ directory with timestamp.
     */
    private void performAutoSave() {
        ScriptMetadata currentScript = context.getCurrentScript();

        // Only auto-save if we have unsaved changes and a current script
        if (!changesTracker.isDirty() || currentScript == null) {
            return;
        }

        // Only auto-save if connected to Gateway
        if (!context.isConnectedToGateway()) {
            logger.debug("Auto-save skipped: not connected to Gateway");
            return;
        }

        try {
            // Save to temp file as backup
            File tempDir = new File(System.getProperty("user.home"), AUTOSAVE_DIR_NAME);
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            String fileName = currentScript.getName() + "_autosave_" + System.currentTimeMillis() + ".py";
            File tempFile = new File(tempDir, fileName);
            Files.write(tempFile.toPath(), codeEditor.getText().getBytes());

            // Clean up old autosave files (keep only last 5)
            cleanupOldAutosaveFiles(tempDir, currentScript.getName());

            statusBar.setStatus("Auto-saved to " + tempFile.getName(), ModernStatusBar.MessageType.INFO);
            logger.debug("Auto-saved to: {}", tempFile.getAbsolutePath());

        } catch (Exception e) {
            logger.error("Auto-save failed", e);
        }
    }

    /**
     * Cleans up old autosave files, keeping only the most recent ones.
     *
     * @param dir directory containing autosave files
     * @param scriptName name of the script to clean up
     */
    private void cleanupOldAutosaveFiles(File dir, String scriptName) {
        try {
            File[] autosaveFiles = dir.listFiles((d, name) ->
                name.startsWith(scriptName + "_autosave_") && name.endsWith(".py")
            );

            if (autosaveFiles != null && autosaveFiles.length > MAX_AUTOSAVE_FILES) {
                // Sort by last modified (oldest first)
                Arrays.sort(autosaveFiles, (a, b) ->
                    Long.compare(a.lastModified(), b.lastModified())
                );

                // Delete all but the last 5
                for (int i = 0; i < autosaveFiles.length - MAX_AUTOSAVE_FILES; i++) {
                    autosaveFiles[i].delete();
                    logger.debug("Deleted old autosave file: {}", autosaveFiles[i].getName());
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to cleanup old autosave files", e);
        }
    }

    /**
     * Gets the auto-save interval in milliseconds.
     *
     * @return auto-save interval
     */
    public int getAutoSaveInterval() {
        return AUTO_SAVE_INTERVAL_MS;
    }

    /**
     * Checks if auto-save is running.
     *
     * @return true if auto-save timer is active
     */
    public boolean isRunning() {
        return autoSaveTimer != null && autoSaveTimer.isRunning();
    }
}
