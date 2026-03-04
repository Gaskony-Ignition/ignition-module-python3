package com.inductiveautomation.ignition.examples.python3.designer.managers;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages recently opened scripts for quick access.
 * Tracks the last 10 scripts opened and persists them to user preferences.
 *
 * @since v2.8.0
 */
public class RecentScriptsManager {
    private static final int MAX_RECENT = 10;
    private static final String PREFS_KEY = "recent_scripts";
    private static final String DELIMITER = "|||";

    private final LinkedList<String> recentScripts;
    private final Preferences prefs;

    /**
     * Creates a new RecentScriptsManager and loads recent scripts from preferences.
     */
    public RecentScriptsManager() {
        this.prefs = Preferences.userNodeForPackage(RecentScriptsManager.class);
        this.recentScripts = new LinkedList<>();
        loadFromPreferences();
    }

    /**
     * Adds a script to the recent list. If the script already exists, it's moved to the top.
     * v2.15.6: Returns whether the list changed (to avoid unnecessary tree refreshes).
     *
     * @param scriptName the name of the script to add
     * @return true if the script was already at the top (no tree refresh needed), false otherwise
     */
    public boolean addRecent(String scriptName) {
        if (scriptName == null || scriptName.trim().isEmpty()) {
            return true;  // No change
        }

        // v2.15.6: Check if script is already at the top (optimization to avoid unnecessary refresh)
        if (!recentScripts.isEmpty() && recentScripts.getFirst().equals(scriptName)) {
            return true;  // Script already at top, no need to refresh tree
        }

        // Remove if already exists (to avoid duplicates)
        recentScripts.remove(scriptName);

        // Add to the top
        recentScripts.addFirst(scriptName);

        // Trim to max size
        while (recentScripts.size() > MAX_RECENT) {
            recentScripts.removeLast();
        }

        // Persist to preferences
        saveToPreferences();

        return false;  // List changed, tree refresh needed
    }

    /**
     * Gets the list of recent scripts (most recent first).
     *
     * @return list of recent script names
     */
    public List<String> getRecentScripts() {
        return new ArrayList<>(recentScripts);
    }

    /**
     * Removes a script from the recent list (e.g., if it was deleted).
     *
     * @param scriptName the name of the script to remove
     */
    public void removeRecent(String scriptName) {
        if (recentScripts.remove(scriptName)) {
            saveToPreferences();
        }
    }

    /**
     * Clears all recent scripts.
     */
    public void clearRecent() {
        recentScripts.clear();
        saveToPreferences();
    }

    /**
     * Checks if there are any recent scripts.
     *
     * @return true if recent scripts exist
     */
    public boolean hasRecentScripts() {
        return !recentScripts.isEmpty();
    }

    /**
     * Loads recent scripts from user preferences.
     */
    private void loadFromPreferences() {
        String saved = prefs.get(PREFS_KEY, "");
        if (!saved.isEmpty()) {
            String[] scripts = saved.split(DELIMITER);
            for (String script : scripts) {
                if (!script.trim().isEmpty()) {
                    recentScripts.add(script);
                }
            }
        }
    }

    /**
     * Saves recent scripts to user preferences.
     */
    private void saveToPreferences() {
        String joined = String.join(DELIMITER, recentScripts);
        prefs.put(PREFS_KEY, joined);
    }
}
