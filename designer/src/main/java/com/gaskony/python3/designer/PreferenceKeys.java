package com.gaskony.python3.designer;

/**
 * Centralised user-preference key strings for the Python 3 Designer components.
 *
 * <p>Single source of truth for all {@link java.util.prefs.Preferences} key strings.
 * The IDE components ({@link Python3IDE}) and the Script Console ({@link Python3ScriptConsole})
 * each use a different Preferences node, but the key strings are defined here.</p>
 *
 * <p>IDE preferences: {@code Preferences.userNodeForPackage(Python3IDE.class)}<br>
 * Console preferences: {@code Preferences.userNodeForPackage(Python3ScriptConsole.class)}</p>
 */
public final class PreferenceKeys {

    private PreferenceKeys() {
        // Utility class - no instances
    }

    // =========================================================================
    // Python3IDE preference keys  (node: userNodeForPackage(Python3IDE.class))
    // =========================================================================

    /** Key for the IDE colour theme name ("dark", "light", "vscode"). */
    public static final String IDE_THEME = "python3ide.theme";

    /** Key for the code-editor font size (integer). */
    public static final String IDE_FONT_SIZE = "python3ide.fontsize";

    /** Key for the user-configured Gateway URL override (empty = auto-detect). */
    public static final String IDE_GATEWAY_OVERRIDE = "python3ide.gateway.override";

    /** Key for the auto-connect-on-startup flag (boolean). */
    public static final String IDE_AUTO_CONNECT = "python3ide.gateway.autoconnect";

    /** Key for the desired process-pool size (integer). */
    public static final String IDE_POOL_SIZE = "python3ide.pool.size";

    // =========================================================================
    // Python3ScriptConsole preference keys  (node: userNodeForPackage(Python3ScriptConsole.class))
    // =========================================================================

    /** Key for the Script Console colour theme name. */
    public static final String CONSOLE_THEME = "python3console.theme";

    /** Key for the Script Console split-pane orientation (JSplitPane constant). */
    public static final String CONSOLE_SPLIT_ORIENTATION = "python3console.splitOrientation";
}
