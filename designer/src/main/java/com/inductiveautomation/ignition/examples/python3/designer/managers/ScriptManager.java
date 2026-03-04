package com.inductiveautomation.ignition.examples.python3.designer.managers;

import com.inductiveautomation.ignition.examples.python3.designer.Python3RestClient;
import com.inductiveautomation.ignition.examples.python3.designer.SavedScript;
import com.inductiveautomation.ignition.examples.python3.designer.ScriptMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Manages script operations using existing Python3RestClient API.
 * Simplified version for v2.0.0 architecture demonstration.
 *
 * v2.0.0: Extracted from Python3IDE.java monolith
 */
public class ScriptManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptManager.class);

    private final Python3RestClient restClient;
    private SavedScript currentScript;

    public ScriptManager(Python3RestClient restClient) {
        this.restClient = restClient;
        this.currentScript = null;
    }

    /**
     * Saves a script using the existing 6-parameter saveScript method.
     */
    public void saveScript(String name, String code, String description,
                          String author, String folderPath, String version) throws IOException {
        if (restClient == null) {
            throw new IllegalStateException("Not connected to Gateway");
        }
        restClient.saveScript(name, code, description, author, folderPath, version);
        LOGGER.info("Script saved: {}", name);
    }

    /**
     * Loads a script by name.
     */
    public SavedScript loadScript(String name) throws IOException {
        if (restClient == null) {
            throw new IllegalStateException("Not connected to Gateway");
        }
        SavedScript script = restClient.loadScript(name);
        this.currentScript = script;
        LOGGER.info("Script loaded: {}", name);
        return script;
    }

    /**
     * Deletes a script by name.
     */
    public void deleteScript(String name) throws IOException {
        if (restClient == null) {
            throw new IllegalStateException("Not connected to Gateway");
        }
        restClient.deleteScript(name);
        if (currentScript != null && name.equals(currentScript.getName())) {
            currentScript = null;
        }
        LOGGER.info("Script deleted: {}", name);
    }

    /**
     * Renames a script by loading, deleting old, and saving with new name.
     */
    public void renameScript(String oldName, String newName) throws IOException {
        if (restClient == null) {
            throw new IllegalStateException("Not connected to Gateway");
        }

        // Load the script
        SavedScript script = restClient.loadScript(oldName);

        // Delete the old script
        restClient.deleteScript(oldName);

        // Save with new name
        restClient.saveScript(
                newName,
                script.getCode(),
                script.getDescription(),
                script.getAuthor(),
                script.getFolderPath(),
                script.getVersion()
        );

        if (currentScript != null && oldName.equals(currentScript.getName())) {
            currentScript = null;
        }

        LOGGER.info("Script renamed: {} → {}", oldName, newName);
    }

    /**
     * Lists all scripts.
     */
    public List<ScriptMetadata> listScripts() throws IOException {
        if (restClient == null) {
            throw new IllegalStateException("Not connected to Gateway");
        }
        return restClient.listScripts();
    }

    /**
     * Gets the current script.
     */
    public SavedScript getCurrentScript() {
        return currentScript;
    }

    /**
     * Clears the current script.
     */
    public void clearCurrentScript() {
        this.currentScript = null;
    }
}
