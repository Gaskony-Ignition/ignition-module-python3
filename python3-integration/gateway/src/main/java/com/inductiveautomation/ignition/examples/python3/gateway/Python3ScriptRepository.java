package com.inductiveautomation.ignition.examples.python3.gateway;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages saved Python scripts for the Python 3 Integration module.
 * Scripts are stored as JSON files in the Gateway data directory.
 */
public class Python3ScriptRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(Python3ScriptRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path scriptsDirectory;
    private final Path scriptsIndexFile;
    private final Map<String, SavedScript> scriptIndex;

    /**
     * Creates a new script repository.
     *
     * @param baseDirectory the base directory for script storage
     * @throws IOException if the directory cannot be created
     */
    public Python3ScriptRepository(Path baseDirectory) throws IOException {
        this.scriptsDirectory = baseDirectory.resolve("scripts");
        this.scriptsIndexFile = scriptsDirectory.resolve("index.json");
        this.scriptIndex = new HashMap<>();

        // Create directories if they don't exist
        Files.createDirectories(scriptsDirectory);

        // Load existing scripts
        loadIndex();

        LOGGER.info("Python3ScriptRepository initialized at: {}", scriptsDirectory);
    }

    /**
     * Saves a Python script.
     *
     * @param name the script name (unique identifier)
     * @param code the Python code
     * @param description optional description
     * @param author the script author
     * @param folderPath the folder path (e.g., "My Scripts/Utils")
     * @param version optional version string
     * @return the saved script
     * @throws IOException if save fails
     *
     * v1.17.0: Now generates HMAC signature for tamper protection
     */
    public SavedScript saveScript(String name, String code, String description, String author, String folderPath, String version) throws IOException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Script name cannot be empty");
        }

        // Sanitize name for filesystem
        String sanitizedName = sanitizeName(name);
        String now = Instant.now().toString();

        // Check if updating existing script
        SavedScript existing = scriptIndex.get(sanitizedName);
        String createdDate = existing != null ? existing.getCreatedDate() : now;

        // Generate HMAC signature for tamper protection (v1.17.0)
        String signature = Python3ScriptSigner.signScript(code);
        LOGGER.debug("Generated signature for script: {}, signature hash: {}...",
                name, signature.substring(0, Math.min(16, signature.length())));

        // Create script object
        SavedScript script = new SavedScript(
                sanitizedName,
                name,
                code,
                description,
                author != null ? author : "Unknown",
                createdDate,
                now,
                folderPath != null ? folderPath : "",
                version != null ? version : "1.0",
                signature  // v1.17.0: Store signature
        );

        // Save to index
        scriptIndex.put(sanitizedName, script);

        // Persist index
        saveIndex();

        LOGGER.info("Script saved: {} in folder: {} (signed)", name, folderPath);
        return script;
    }

    /**
     * Saves a Python script (simplified overload for backward compatibility).
     */
    public SavedScript saveScript(String name, String code, String description) throws IOException {
        return saveScript(name, code, description, "Unknown", "", "1.0");
    }

    /**
     * Loads a saved script by name.
     *
     * @param name the script name
     * @return the saved script, or null if not found
     * @throws SecurityException if script signature verification fails (tampered)
     *
     * v1.17.0: Now verifies HMAC signature to detect tampering
     */
    public SavedScript loadScript(String name) {
        String sanitizedName = sanitizeName(name);
        SavedScript script = scriptIndex.get(sanitizedName);

        if (script == null) {
            LOGGER.warn("Script not found: {}", name);
            return null;
        }

        // Verify signature (v1.17.0)
        if (script.getSignature() != null) {
            boolean valid = Python3ScriptSigner.verifyScript(script.getCode(), script.getSignature());

            if (!valid) {
                LOGGER.error("SECURITY: Script signature verification FAILED for: {} - possible tampering detected!", name);
                throw new SecurityException(
                        "Script signature verification failed for: " + name +
                        ". The script may have been tampered with. Please re-save the script."
                );
            }

            LOGGER.debug("Script signature verified for: {}", name);
        } else {
            // Legacy script without signature - log warning
            LOGGER.warn("Script loaded without signature verification (legacy): {}. " +
                    "Re-save to add tamper protection.", name);
        }

        return script;
    }

    /**
     * Loads a saved script by path (supports folder hierarchy).
     * Supports formats like:
     * - "My Script" (root level)
     * - "Folder/My Script" (in folder)
     * - "Folder/Subfolder/My Script" (nested folders)
     * - "/Folder/My Script" (leading slash optional)
     *
     * @param scriptPath the script path
     * @return the saved script, or null if not found
     */
    public SavedScript loadScriptByPath(String scriptPath) {
        if (scriptPath == null || scriptPath.trim().isEmpty()) {
            LOGGER.warn("Script path is empty");
            return null;
        }

        // Normalize path: remove leading/trailing slashes
        String normalizedPath = scriptPath.replaceAll("^/+|/+$", "").trim();

        // Split into folder path and script name
        String folderPath;
        String scriptName;

        int lastSlash = normalizedPath.lastIndexOf('/');
        if (lastSlash == -1) {
            // No folder path, script is at root
            folderPath = "";
            scriptName = normalizedPath;
        } else {
            // Extract folder path and script name
            folderPath = normalizedPath.substring(0, lastSlash);
            scriptName = normalizedPath.substring(lastSlash + 1);
        }

        // Search for script with matching name and folder path
        for (SavedScript script : scriptIndex.values()) {
            String scriptFolderPath = script.getFolderPath() != null ? script.getFolderPath() : "";

            // Match script name and folder path
            if (script.getName().equals(scriptName) && scriptFolderPath.equals(folderPath)) {
                LOGGER.debug("Found script by path: {} in folder: {}", scriptName, folderPath);
                return script;
            }
        }

        // Try case-insensitive match
        for (SavedScript script : scriptIndex.values()) {
            String scriptFolderPath = script.getFolderPath() != null ? script.getFolderPath() : "";

            if (script.getName().equalsIgnoreCase(scriptName) &&
                scriptFolderPath.equalsIgnoreCase(folderPath)) {
                LOGGER.debug("Found script by path (case-insensitive): {} in folder: {}", scriptName, folderPath);
                return script;
            }
        }

        LOGGER.warn("Script not found by path: {}", scriptPath);
        return null;
    }

    /**
     * Lists all saved scripts.
     *
     * @return list of saved scripts (metadata only, no code)
     */
    public List<ScriptMetadata> listScripts() {
        return scriptIndex.values().stream()
                .map(script -> new ScriptMetadata(
                        script.getId(),
                        script.getName(),
                        script.getDescription(),
                        script.getAuthor(),
                        script.getCreatedDate(),
                        script.getLastModified(),
                        script.getFolderPath(),
                        script.getVersion()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Deletes a saved script.
     *
     * @param name the script name
     * @return true if deleted, false if not found
     * @throws IOException if delete fails
     */
    public boolean deleteScript(String name) throws IOException {
        String sanitizedName = sanitizeName(name);

        if (scriptIndex.remove(sanitizedName) != null) {
            saveIndex();
            LOGGER.info("Script deleted: {}", name);
            return true;
        }

        LOGGER.warn("Script not found for deletion: {}", name);
        return false;
    }

    /**
     * Checks if a script exists.
     *
     * @param name the script name
     * @return true if exists
     */
    public boolean exists(String name) {
        return scriptIndex.containsKey(sanitizeName(name));
    }

    /**
     * Gets the number of saved scripts.
     *
     * @return count of scripts
     */
    public int getScriptCount() {
        return scriptIndex.size();
    }

    /**
     * Loads the script index from disk.
     */
    private void loadIndex() throws IOException {
        if (!Files.exists(scriptsIndexFile)) {
            LOGGER.info("No existing script index found, starting fresh");
            return;
        }

        try {
            String json = Files.readString(scriptsIndexFile);
            Map<String, SavedScript> loaded = GSON.fromJson(
                    json,
                    new TypeToken<Map<String, SavedScript>>() {}.getType()
            );

            if (loaded != null) {
                scriptIndex.putAll(loaded);
                LOGGER.info("Loaded {} saved scripts", scriptIndex.size());
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load script index, starting fresh", e);
        }
    }

    /**
     * Saves the script index to disk.
     */
    private void saveIndex() throws IOException {
        String json = GSON.toJson(scriptIndex);
        Files.writeString(
                scriptsIndexFile,
                json,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    /**
     * Sanitizes a script name for use as a filesystem identifier.
     *
     * @param name the original name
     * @return sanitized name
     */
    private String sanitizeName(String name) {
        if (name == null) {
            return "unnamed";
        }

        // Replace spaces with underscores, remove special characters
        return name.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9_-]", "_")
                .replaceAll("_+", "_");
    }

    /**
     * Represents a saved Python script.
     *
     * v1.17.0: Added signature field for tamper protection
     */
    public static class SavedScript {
        private final String id;
        private final String name;
        private final String code;
        private final String description;
        private final String author;
        private final String createdDate;
        private final String lastModified;
        private final String folderPath;
        private final String version;
        private final String signature;  // v1.17.0: HMAC signature for tamper detection

        public SavedScript(String id, String name, String code, String description,
                          String author, String createdDate, String lastModified,
                          String folderPath, String version, String signature) {
            this.id = id;
            this.name = name;
            this.code = code;
            this.description = description;
            this.author = author;
            this.createdDate = createdDate;
            this.lastModified = lastModified;
            this.folderPath = folderPath;
            this.version = version;
            this.signature = signature;  // Can be null for backward compatibility
        }

        // Constructor for backward compatibility (without signature)
        public SavedScript(String id, String name, String code, String description,
                          String author, String createdDate, String lastModified,
                          String folderPath, String version) {
            this(id, name, code, description, author, createdDate, lastModified,
                 folderPath, version, null);
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public String getAuthor() {
            return author;
        }

        public String getCreatedDate() {
            return createdDate;
        }

        public String getLastModified() {
            return lastModified;
        }

        public String getFolderPath() {
            return folderPath;
        }

        public String getVersion() {
            return version;
        }

        public String getSignature() {
            return signature;
        }
    }

    /**
     * Script metadata (without code for listing).
     */
    public static class ScriptMetadata {
        private final String id;
        private final String name;
        private final String description;
        private final String author;
        private final String createdDate;
        private final String lastModified;
        private final String folderPath;
        private final String version;

        public ScriptMetadata(String id, String name, String description,
                            String author, String createdDate, String lastModified,
                            String folderPath, String version) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.author = author;
            this.createdDate = createdDate;
            this.lastModified = lastModified;
            this.folderPath = folderPath;
            this.version = version;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getAuthor() {
            return author;
        }

        public String getCreatedDate() {
            return createdDate;
        }

        public String getLastModified() {
            return lastModified;
        }

        public String getFolderPath() {
            return folderPath;
        }

        public String getVersion() {
            return version;
        }
    }
}
