# Implementation Specification - UX Improvements

**⚠️ STATUS: COMPLETED - All 5 tasks implemented in v2.11.4-v2.11.5**
**Archived:** 2025-10-30
**This document is obsolete and kept for historical reference only.**

**Date:** 2025-10-28
**Version:** 2.11.4 (MINOR - New Features)
**Tasks:** 5 UX improvements and bug fixes

## Implementation Summary

All 5 tasks from this specification have been successfully implemented:

1. ✅ **Theme Selector to Settings** - Implemented in v2.11.4 (SettingsDialog.java)
2. ✅ **Auto-Connect to Gateway** - Implemented (PREF_AUTO_CONNECT in Python3IDE.java)
3. ✅ **Terminal Mode PATH Fix** - Implemented in v2.11.4 (Python3IDE.java:1344)
4. ✅ **PyPI Search Functionality** - Implemented in v2.11.4 (PackagesDialog.java)
5. ✅ **PyPI Install Functionality** - Implemented in v2.11.4-v2.11.5 (PackagesDialog.java)

---

# Original Specification (For Historical Reference)

---

## Task 1: Move Theme Selector to Settings Dialog

### Current State
- Theme selector (JComboBox) located in main toolbar
- Takes up horizontal space in the toolbar
- Located at line ~675 in Python3IDE.java
- Theme selection triggers immediate UI update

### Target State
- Theme selector moved to Settings Dialog → "Editor Appearance" section
- Main toolbar space freed up
- Settings dialog remains non-scrollable
- Theme persisted to preferences

### Files to Modify

#### 1. `SettingsDialog.java`

**Add field (after line 58):**
```java
private JComboBox<String> themeSelector;
```

**Add to initComponents() method (after line 120):**
```java
// Theme selector
String[] themes = {"Dark", "Light", "IntelliJ", "Darcula", "Eclipse", "VS Code Dark+"};
themeSelector = new JComboBox<>(themes);
themeSelector.setBackground(ModernTheme.INPUT_BACKGROUND);
themeSelector.setForeground(ModernTheme.FOREGROUND_PRIMARY);
themeSelector.setFont(ModernTheme.FONT_REGULAR);
themeSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
```

**Add to appearanceSection (after line 226, before fontHint):**
```java
// Theme selection
appearanceSection.add(Box.createVerticalStrut(16));
appearanceSection.add(createLabel("Editor Theme"));
appearanceSection.add(Box.createVerticalStrut(8));
themeSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
appearanceSection.add(themeSelector);
appearanceSection.add(Box.createVerticalStrut(4));
JLabel themeHint = createLabel("Code editor color theme. Changes apply on restart.");
themeHint.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));
themeHint.setForeground(ModernTheme.FOREGROUND_SECONDARY);
appearanceSection.add(themeHint);
```

**Add to loadSettings() method (after poolSize load):**
```java
// Load theme preference
String savedTheme = prefs.get("theme", "Dark");
themeSelector.setSelectedItem(savedTheme);
```

**Add to saveSettings() method (after poolSize save):**
```java
// Save theme preference
String theme = (String) themeSelector.getSelectedItem();
if (theme != null) {
    prefs.put("theme", theme);
    // Apply theme immediately
    idePanel.applyTheme(theme);
}
```

#### 2. `Python3IDE.java`

**Remove from class fields (line ~136):**
```java
// DELETE THIS LINE:
private JComboBox<String> themeSelector;
```

**Remove from createTopPanel() method (lines ~480-485):**
```java
// DELETE THESE LINES:
themeSelector = new JComboBox<>(themes);
themeSelector.setSelectedItem("Dark");
themeSelector.setFont(ModernTheme.FONT_REGULAR);
themeSelector.setBackground(ModernTheme.PANEL_BACKGROUND);
themeSelector.setForeground(ModernTheme.FOREGROUND_PRIMARY);
themeSelector.setPreferredSize(new Dimension(153, 28));
```

**Remove from rightPanel.add() (line ~675):**
```java
// DELETE THIS LINE:
rightPanel.add(themeSelector);
```

**Remove theme label (around line ~670):**
```java
// DELETE THESE LINES:
JLabel themeLabel = new JLabel("Theme:");
themeLabel.setFont(ModernTheme.FONT_REGULAR);
themeLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
rightPanel.add(themeLabel);
```

**Remove event listener (around line ~1031):**
```java
// DELETE THIS ENTIRE BLOCK:
themeSelector.addActionListener(e -> {
    String selected = (String) themeSelector.getSelectedItem();
    if (selected != null) {
        applyTheme(themeManager.mapThemeNameToKey(selected));
    }
});
```

**Add public method for Settings dialog to call:**
```java
/**
 * Applies theme from name (called by SettingsDialog).
 * @param themeName Theme name (e.g., "Dark", "Light")
 */
public void applyTheme(String themeName) {
    String themeKey = themeManager.mapThemeNameToKey(themeName);
    applyTheme(themeKey);
}
```

**Update initialize() method to load theme from preferences:**
```java
// After UI setup, load and apply saved theme
Preferences prefs = Preferences.userNodeForPackage(Python3IDE.class);
String savedTheme = prefs.get("theme", "Dark");
applyTheme(savedTheme);
```

---

## Task 2: Auto-Connect to Gateway on Startup

### Current State
- Manual connection required via "Connect" button
- Gateway URL must be entered manually
- No auto-detection of Designer's gateway

### Target State
- Auto-connect on startup to Designer's gateway
- Get gateway URL from DesignerContext
- Fallback to localhost:8088 if unavailable
- Non-blocking (uses SwingWorker)
- Shows connection status in status bar

### Files to Modify

#### 1. `Python3IDE.java`

**Add import:**
```java
import com.inductiveautomation.ignition.client.gateway_interface.GatewayConnectionManager;
```

**Add method to get Designer's gateway URL:**
```java
/**
 * Gets the Gateway URL from the Designer context.
 * @return Gateway URL or default "http://localhost:8088"
 */
private String getDesignerGatewayUrl() {
    try {
        // Try to get the gateway URL from Designer context
        if (designerContext != null) {
            GatewayConnectionManager gcm = designerContext.getGatewayConnectionManager();
            if (gcm != null) {
                String address = gcm.getGatewayAddress();
                if (address != null && !address.isEmpty()) {
                    // Ensure it starts with http://
                    if (!address.startsWith("http://") && !address.startsWith("https://")) {
                        address = "http://" + address;
                    }
                    return address;
                }
            }
        }
    } catch (Exception e) {
        LOGGER.warn("Could not get Designer gateway URL, using default", e);
    }

    // Fallback to localhost
    return "http://localhost:8088";
}
```

**Add auto-connect method:**
```java
/**
 * Auto-connects to the Designer's gateway in the background.
 * Called on startup.
 */
private void autoConnectToGateway() {
    // Check if auto-connect is enabled
    Preferences prefs = Preferences.userNodeForPackage(Python3IDE.class);
    boolean autoConnect = prefs.getBoolean("autoConnect", true); // Default: enabled

    if (!autoConnect) {
        LOGGER.info("Auto-connect disabled in preferences");
        return;
    }

    String gatewayUrl = getDesignerGatewayUrl();
    LOGGER.info("Auto-connecting to gateway: {}", gatewayUrl);

    // Set the URL in the field
    gatewayUrlField.setText(gatewayUrl);

    // Connect in background
    SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
        @Override
        protected Boolean doInBackground() {
            // Small delay to ensure UI is fully initialized
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return connectionManager.connect(gatewayUrl);
        }

        @Override
        protected void done() {
            try {
                Boolean connected = get();
                if (connected) {
                    onConnectionSuccess();
                    LOGGER.info("Auto-connect successful");
                } else {
                    statusBar.setText("Auto-connect failed - click Connect to retry");
                    LOGGER.warn("Auto-connect failed");
                }
            } catch (Exception e) {
                LOGGER.error("Auto-connect error", e);
                statusBar.setText("Auto-connect error: " + e.getMessage());
            }
        }
    };

    worker.execute();
}
```

**Update initialize() method (add at end, after UI setup):**
```java
// Auto-connect to gateway
SwingUtilities.invokeLater(() -> {
    autoConnectToGateway();
});
```

---

## Task 3: Fix Terminal Mode - PATH and Environment

### Current State
- Terminal commands execute without proper shell environment
- `sudo` command not found (not in PATH)
- `pip install` fails with externally-managed-environment error
- Commands don't have access to standard Linux tools

### Root Cause
- Commands executed directly without shell login
- No PATH environment variable set
- Python virtual environment restrictions

### Target State
- Commands execute with full shell environment
- PATH includes /usr/bin, /bin, /usr/local/bin
- pip commands use --break-system-packages flag
- Better error messages

### Files to Modify

#### 1. `Python3IDE.java` - executeTerminalCommand() method

**Replace entire method (~line 1334):**
```java
/**
 * Executes a terminal command on the Gateway.
 * Commands are sent to the Gateway's Python environment via REST API.
 *
 * @param command Command to execute
 */
private void executeTerminalCommand(String command) {
    if (command == null || command.trim().isEmpty()) {
        return;
    }

    command = command.trim();

    // Handle pip install specially - add --break-system-packages flag
    if (command.startsWith("pip install ") || command.startsWith("pip3 install ")) {
        if (!command.contains("--break-system-packages")) {
            command = command + " --break-system-packages";
            terminalPanel.appendOutput("Note: Added --break-system-packages flag\n",
                new Color(255, 200, 100)); // Orange
        }
    }

    // Prepare shell command with full environment
    String shellCommand = prepareShellCommand(command);

    // Execute via REST API (which has proper Python environment)
    final String finalCommand = command;
    SwingWorker<String, Void> worker = new SwingWorker<>() {
        @Override
        protected String doInBackground() {
            try {
                // Use Python subprocess to execute with proper shell environment
                String pythonCode = String.format(
                    "import subprocess\n" +
                    "import os\n" +
                    "# Set up PATH\n" +
                    "os.environ['PATH'] = '/usr/local/bin:/usr/bin:/bin:/usr/local/sbin:/usr/sbin:/sbin'\n" +
                    "result = subprocess.run(%s, shell=True, capture_output=True, text=True)\n" +
                    "output = result.stdout\n" +
                    "if result.stderr:\n" +
                    "    output += '\\nERROR:\\n' + result.stderr\n" +
                    "result = output",
                    escapeForPython(shellCommand)
                );

                Map<String, Object> variables = new HashMap<>();
                ExecutionResult result = connectionManager.executeCode(pythonCode, variables);

                if (result.success()) {
                    return result.result() != null ? result.result().toString() : "";
                } else {
                    return "ERROR: " + result.error();
                }
            } catch (Exception e) {
                return "ERROR: " + e.getMessage();
            }
        }

        @Override
        protected void done() {
            try {
                String output = get();

                // Determine output color
                Color outputColor = ModernTheme.FOREGROUND_PRIMARY;
                if (output.contains("ERROR:")) {
                    outputColor = new Color(255, 100, 100); // Red
                }

                terminalPanel.appendOutput(output + "\n", outputColor);

                // Update working directory if command was cd
                if (finalCommand.startsWith("cd ")) {
                    updateTerminalWorkingDirectory();
                }

            } catch (Exception e) {
                terminalPanel.appendOutput("ERROR: " + e.getMessage() + "\n",
                    new Color(255, 100, 100));
                LOGGER.error("Terminal command execution failed", e);
            }
        }
    };

    worker.execute();
}

/**
 * Prepares shell command with proper environment.
 */
private String prepareShellCommand(String command) {
    // If command uses sudo, provide helpful message
    if (command.trim().startsWith("sudo ")) {
        // Remove sudo and warn
        command = command.replaceFirst("sudo\\s+", "");
        terminalPanel.appendOutput("Note: 'sudo' not available, executing without elevation\n",
            new Color(255, 200, 100)); // Orange
    }

    return command;
}

/**
 * Escapes a string for use in Python code.
 */
private String escapeForPython(String str) {
    return "'" + str.replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r") + "'";
}
```

---

## Task 4: Implement PyPI Search Functionality

### Current State
- No search functionality in Packages dialog
- Cannot discover available packages

### Target State
- Search field in Packages dialog
- Queries PyPI JSON API
- Displays search results in table
- Shows package name, version, description

### Files to Modify

#### 1. Create new file: `PyPISearcher.java`

**Location:** `designer/src/main/java/com/inductiveautomation/ignition/examples/python3/designer/`

**Content:**
```java
package com.inductiveautomation.ignition.examples.python3.designer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Searches PyPI for Python packages.
 */
public class PyPISearcher {

    private static final String PYPI_SEARCH_URL = "https://pypi.org/pypi/%s/json";
    private final OkHttpClient client;

    public PyPISearcher() {
        this.client = new OkHttpClient();
    }

    /**
     * Searches for a package on PyPI.
     * @param query Package name to search
     * @return Package info or null if not found
     */
    public PackageInfo searchPackage(String query) throws IOException {
        String url = String.format(PYPI_SEARCH_URL, query);
        Request request = new Request.Builder()
            .url(url)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }

            String jsonData = response.body().string();
            JsonObject json = JsonParser.parseString(jsonData).getAsJsonObject();

            JsonObject info = json.getAsJsonObject("info");
            String name = info.get("name").getAsString();
            String version = info.get("version").getAsString();
            String summary = info.has("summary") ? info.get("summary").getAsString() : "";

            return new PackageInfo(name, version, summary);
        }
    }

    /**
     * Package information from PyPI.
     */
    public static class PackageInfo {
        public final String name;
        public final String version;
        public final String description;

        public PackageInfo(String name, String version, String description) {
            this.name = name;
            this.version = version;
            this.description = description;
        }
    }
}
```

#### 2. Modify `PackagesDialog.java`

**Add search field after title:**
```java
// Search panel
JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
searchPanel.setBackground(ModernTheme.BACKGROUND_DARK);
searchPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

JTextField searchField = new JTextField();
searchField.setBackground(ModernTheme.INPUT_BACKGROUND);
searchField.setForeground(ModernTheme.FOREGROUND_PRIMARY);
searchField.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);
searchField.setFont(ModernTheme.FONT_REGULAR);
searchField.setBorder(BorderFactory.createCompoundBorder(
    BorderFactory.createLineBorder(ModernTheme.INPUT_BORDER),
    new EmptyBorder(8, 10, 8, 10)
));

JButton searchButton = ModernButton.createPrimary("Search PyPI");
searchButton.setPreferredSize(new Dimension(150, 35));

searchPanel.add(searchField, BorderLayout.CENTER);
searchPanel.add(searchButton, BorderLayout.EAST);

contentPanel.add(searchPanel);
```

**Add search button action:**
```java
searchButton.addActionListener(e -> {
    String query = searchField.getText().trim();
    if (query.isEmpty()) {
        return;
    }

    // Search in background
    SwingWorker<PyPISearcher.PackageInfo, Void> worker = new SwingWorker<>() {
        @Override
        protected PyPISearcher.PackageInfo doInBackground() {
            try {
                PyPISearcher searcher = new PyPISearcher();
                return searcher.searchPackage(query);
            } catch (Exception ex) {
                return null;
            }
        }

        @Override
        protected void done() {
            try {
                PyPISearcher.PackageInfo pkg = get();
                if (pkg != null) {
                    JOptionPane.showMessageDialog(PackagesDialog.this,
                        String.format("Package: %s\nVersion: %s\n\n%s",
                            pkg.name, pkg.version, pkg.description),
                        "Package Found",
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(PackagesDialog.this,
                        "Package not found on PyPI",
                        "Not Found",
                        JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(PackagesDialog.this,
                    "Search error: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    };

    worker.execute();
});
```

---

## Task 5: Implement PyPI Install Functionality

### Current State
- Cannot install packages from PyPI
- Manual terminal commands required

### Target State
- Install button in Packages dialog
- Prompts for package name
- Installs via REST API
- Shows progress
- Refreshes package list

### Files to Modify

#### 1. `PackagesDialog.java`

**Add Install button to button panel:**
```java
JButton installButton = ModernButton.createPrimary("Install from PyPI");
installButton.setPreferredSize(new Dimension(160, 35));

installButton.addActionListener(e -> {
    String packageName = JOptionPane.showInputDialog(this,
        "Enter package name to install:",
        "Install Package",
        JOptionPane.QUESTION_MESSAGE);

    if (packageName != null && !packageName.trim().isEmpty()) {
        installPackage(packageName.trim());
    }
});

buttonPanel.add(installButton);
```

**Add install method:**
```java
/**
 * Installs a package from PyPI.
 */
private void installPackage(String packageName) {
    // Show progress
    JOptionPane.showMessageDialog(this,
        "Installing " + packageName + "...\nThis may take a moment.",
        "Installing",
        JOptionPane.INFORMATION_MESSAGE);

    SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
        String errorMessage = null;

        @Override
        protected Boolean doInBackground() {
            try {
                // Execute pip install via REST API
                String pythonCode = String.format(
                    "import subprocess\\n" +
                    "import os\\n" +
                    "os.environ['PATH'] = '/usr/local/bin:/usr/bin:/bin'\\n" +
                    "result = subprocess.run(['pip3', 'install', '%s', '--break-system-packages'], " +
                    "capture_output=True, text=True)\\n" +
                    "if result.returncode != 0:\\n" +
                    "    raise Exception(result.stderr)\\n" +
                    "result = 'success'",
                    packageName
                );

                Map<String, Object> variables = new HashMap<>();
                ExecutionResult result = connectionManager.executeCode(pythonCode, variables);

                if (!result.success()) {
                    errorMessage = result.error();
                    return false;
                }

                return true;
            } catch (Exception e) {
                errorMessage = e.getMessage();
                return false;
            }
        }

        @Override
        protected void done() {
            try {
                Boolean success = get();
                if (success) {
                    JOptionPane.showMessageDialog(PackagesDialog.this,
                        packageName + " installed successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);

                    // Refresh package list
                    refreshPackages();
                } else {
                    JOptionPane.showMessageDialog(PackagesDialog.this,
                        "Installation failed:\\n" + errorMessage,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(PackagesDialog.this,
                    "Installation error: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    };

    worker.execute();
}
```

---

## Build and Test Plan

### 1. Build
```bash
cd python3-integration
./gradlew clean build --no-daemon
```

### 2. Test Checklist
- [ ] Settings dialog shows theme selector in Editor Appearance
- [ ] Theme changes apply immediately
- [ ] Theme persists across IDE restarts
- [ ] IDE auto-connects on startup to Designer's gateway
- [ ] Terminal mode executes commands with proper PATH
- [ ] `ls` command works in terminal
- [ ] `pip install ansible --break-system-packages` works
- [ ] PyPI search finds packages
- [ ] PyPI install functionality works
- [ ] Packages dialog refreshes after install

### 3. Version Update
Update to v2.11.4 in:
- version.properties
- DesignerHook.java (fallback)
- README.md files
- CHANGELOG.md

---

## Implementation Order

1. Task 1: Move theme selector (easiest, visible change)
2. Task 2: Auto-connect (improves UX significantly)
3. Task 3: Fix terminal mode (fixes broken functionality)
4. Task 4: PyPI search (new feature)
5. Task 5: PyPI install (completes PyPI integration)

---

**End of Specification**
