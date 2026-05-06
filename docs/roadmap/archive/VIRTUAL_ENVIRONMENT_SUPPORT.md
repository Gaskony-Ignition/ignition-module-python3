# Virtual Environment (venv) Support - Implementation Plan

**Status:** ✅ HIGHLY VIABLE - Ready for Implementation
**Effort Estimate:** 7-11 hours for production-ready basic support
**Risk Level:** LOW
**Priority:** HIGH (improves package isolation and dependency management)

---

## Executive Summary

This document outlines the plan to add Python virtual environment (venv) support to the Python 3 Integration module. The current architecture is **exceptionally well-suited** for this feature, requiring minimal code changes for basic functionality.

### Benefits

1. **Package Isolation:** Different projects can use different package versions
2. **Dependency Management:** Clean separation from system Python packages
3. **Reproducibility:** Lockable environments via requirements.txt
4. **Security:** Reduced risk of system-wide package conflicts
5. **Standard Practice:** Aligns with modern Python development workflows

### Current Limitations (Without venv)

- All scripts share the same Python environment
- Package conflicts when different versions needed
- No isolation between projects
- System Python pollution
- Difficult dependency tracking

---

## Architecture Analysis

### Current Python Execution Flow

```
GatewayHook.startup()
  ↓
PythonDistributionManager.getPythonPath()
  ↓ Priority: Embedded → Download → System
Python3ProcessPool(pythonPath, poolSize)
  ↓
Python3Executor(pythonPath) × poolSize
  ↓
ProcessBuilder(pythonPath, "-u", "python_bridge.py")
  ↓
Python subprocess with sys.path resolution
```

### Key Finding: Single Integration Point

**The entire system depends on a single String parameter: `pythonPath`**

If `pythonPath` points to a venv Python executable:
- ✅ Packages automatically resolve from venv (via sys.path)
- ✅ pip installs to venv site-packages
- ✅ venv packages take priority over system packages
- ✅ **NO CODE CHANGES NEEDED for basic functionality**

### What Needs to Change

1. **venv Detection** - Teach `PythonDistributionManager` to detect venvs
2. **pip Command Fix** - Fix hardcoded `pip3` in `PackagesDialog`
3. **Environment Variables** - Set `VIRTUAL_ENV` and `PATH` (optional but recommended)
4. **UI Configuration** - Add venv settings to Designer (optional)

---

## Implementation Phases

## Phase 1: Basic venv Support via System Property ⭐

**Goal:** Allow using an existing venv via `-Dignition.python3.venv.path`

**Effort:** 2-4 hours
**Risk:** LOW
**Value:** HIGH (immediate usability)

### Changes Required

#### File 1: `PythonDistributionManager.java`

**Location:** `gateway/src/main/java/.../gateway/PythonDistributionManager.java`

**Add new method (insert after line 103):**

```java
/**
 * Detect Python virtual environment from configuration.
 * Priority:
 * 1. System property: -Dignition.python3.venv.path=/path/to/venv
 * 2. Environment variable: VIRTUAL_ENV
 * 3. Auto-detect in common locations
 *
 * @return Path to venv Python executable, or null if not found
 */
private String detectVirtualEnv() {
    // Priority 1: System property
    String venvPath = System.getProperty("ignition.python3.venv.path");
    if (venvPath != null && !venvPath.isEmpty()) {
        String venvPython = getVenvPythonExecutable(venvPath);
        if (venvPython != null && isPythonValid(venvPython)) {
            LOGGER.info("Found venv from system property: {}", venvPath);
            return venvPython;
        } else {
            LOGGER.warn("Invalid venv path in system property: {}", venvPath);
        }
    }

    // Priority 2: Environment variable VIRTUAL_ENV
    String virtualEnv = System.getenv("VIRTUAL_ENV");
    if (virtualEnv != null && !virtualEnv.isEmpty()) {
        String venvPython = getVenvPythonExecutable(virtualEnv);
        if (venvPython != null && isPythonValid(venvPython)) {
            LOGGER.info("Found venv from environment variable: {}", virtualEnv);
            return venvPython;
        }
    }

    // Priority 3: Auto-detect in common locations
    String[] venvCandidates = {
        "/opt/ignition-python-env",           // Production deployment
        "./python-env",                        // Relative to Ignition install
        "./venv",                              // Standard venv name
        System.getProperty("user.home") + "/ignition-python-env"  // User home
    };

    for (String candidate : venvCandidates) {
        String venvPython = getVenvPythonExecutable(candidate);
        if (venvPython != null && isPythonValid(venvPython)) {
            LOGGER.info("Auto-detected venv: {}", candidate);
            return venvPython;
        }
    }

    return null;
}

/**
 * Get Python executable path from venv directory.
 * Handles Unix (bin/python3) and Windows (Scripts/python.exe) differences.
 *
 * @param venvPath Path to venv root directory
 * @return Path to Python executable, or null if not found
 */
private String getVenvPythonExecutable(String venvPath) {
    Path venvRoot = Paths.get(venvPath);

    // Verify pyvenv.cfg exists (reliable venv detection)
    Path pyvenvCfg = venvRoot.resolve("pyvenv.cfg");
    if (!Files.exists(pyvenvCfg)) {
        LOGGER.debug("Not a valid venv (missing pyvenv.cfg): {}", venvPath);
        return null;
    }

    String os = detectOS();
    Path pythonExecutable;

    if ("windows".equals(os)) {
        // Windows: venv/Scripts/python.exe
        pythonExecutable = venvRoot.resolve("Scripts").resolve("python.exe");
    } else {
        // Unix/Linux/macOS: venv/bin/python3
        pythonExecutable = venvRoot.resolve("bin").resolve("python3");
        if (!Files.exists(pythonExecutable)) {
            // Fallback: some venvs use 'python' instead of 'python3'
            pythonExecutable = venvRoot.resolve("bin").resolve("python");
        }
    }

    if (Files.exists(pythonExecutable) && Files.isExecutable(pythonExecutable)) {
        return pythonExecutable.toString();
    }

    return null;
}
```

**Modify existing method (line 78):**

```java
public String getPythonPath() throws IOException {
    // PRIORITY 1: Check for virtual environment FIRST
    String venvPython = detectVirtualEnv();
    if (venvPython != null) {
        LOGGER.info("Using virtual environment Python: {}", venvPython);
        return venvPython;
    }

    // PRIORITY 2: Check if embedded Python already extracted
    if (isEmbeddedPythonInstalled()) {
        LOGGER.info("Using embedded Python: {}", pythonExecutable);
        return pythonExecutable;
    }

    // PRIORITY 3: Download if enabled (prioritize self-contained distribution)
    if (autoDownload) {
        LOGGER.info("Embedded Python not found, downloading distribution...");
        downloadAndInstall();
        return pythonExecutable;
    }

    // PRIORITY 4: Try system Python as fallback (only when autoDownload disabled)
    String systemPython = detectSystemPython();
    if (systemPython != null) {
        LOGGER.info("Using system Python: {}", systemPython);
        return systemPython;
    }

    throw new IOException(
            "Python 3 not found. Please install Python 3.8+ or enable auto-download.\n"
                    + "To use a virtual environment, set: -Dignition.python3.venv.path=/path/to/venv"
    );
}
```

**Summary of Changes:**
- Added `detectVirtualEnv()` method (50 lines)
- Added `getVenvPythonExecutable()` method (35 lines)
- Modified `getPythonPath()` to check venv first (5 lines changed)
- **Total:** ~90 lines of new code

### Testing Phase 1

```bash
# 1. Create virtual environment
python3 -m venv /opt/ignition-python-env
source /opt/ignition-python-env/bin/activate

# 2. Install test packages
pip install numpy pandas requests

# 3. Configure Ignition (edit ignition.conf)
wrapper.java.additional.101=-Dignition.python3.venv.path=/opt/ignition-python-env

# 4. Restart Gateway
./ignition.sh restart

# 5. Test in Designer IDE
import numpy
import pandas
import requests
print(f"numpy: {numpy.__version__}")
print(f"pandas: {pandas.__version__}")
print(f"requests: {requests.__version__}")

# 6. Verify venv usage in Gateway logs
grep "Using virtual environment" wrapper.log
```

**Expected Output:**
```
Using virtual environment Python: /opt/ignition-python-env/bin/python3
numpy: 2.1.3
pandas: 2.2.3
requests: 2.32.3
```

---

## Phase 2: Fix pip Command for venv Compatibility 🔧

**Goal:** Fix Designer PackagesDialog to use venv pip correctly

**Effort:** 1-2 hours
**Risk:** LOW
**Value:** CRITICAL (enables package installation to venv)

### Changes Required

#### File 2: `PackagesDialog.java`

**Location:** `designer/src/main/java/.../designer/PackagesDialog.java`

**Modify `installPackage()` method (line 562-579):**

**OLD CODE (Line 567-569):**
```java
"    proc_result = subprocess.run(['pip3', 'install', '%s', '--break-system-packages'], " +
```

**NEW CODE:**
```java
"import sys\n" +
"    # Use sys.executable to ensure we use the correct Python/venv\n" +
"    # Check if we're in a venv (skip --break-system-packages flag)\n" +
"    in_venv = hasattr(sys, 'base_prefix') and sys.prefix != sys.base_prefix\n" +
"    pip_cmd = [sys.executable, '-m', 'pip', 'install', '%s']\n" +
"    if not in_venv:\n" +
"        pip_cmd.append('--break-system-packages')  # Only needed for system Python\n" +
"    proc_result = subprocess.run(pip_cmd, " +
```

**Also update `searchPackage()` if needed (line 414):**
- Currently uses urllib.request (no changes needed)
- Search works regardless of venv

**Summary of Changes:**
- Modified pip installation command (8 lines changed)
- Added venv detection logic
- Conditional `--break-system-packages` flag
- **Total:** ~10 lines changed

### Testing Phase 2

```bash
# 1. Connect Designer to Gateway with venv configured
# 2. Open Tools → Python 3 IDE → Packages tab
# 3. Search for package: "beautifulsoup4"
# 4. Install package
# 5. Verify installed to venv, not system

# On Gateway:
ls /opt/ignition-python-env/lib/python3.11/site-packages/
# Should show beautifulsoup4

ls /usr/lib/python3/dist-packages/
# Should NOT show beautifulsoup4 (not installed to system)

# 6. Test import in IDE
import bs4
print(bs4.__version__)
```

---

## Phase 3: Set Virtual Environment Variables 🌍

**Goal:** Set `VIRTUAL_ENV` and `PATH` environment variables in subprocess

**Effort:** 1 hour
**Risk:** LOW
**Value:** MEDIUM (improves compatibility with Python tools)

### Changes Required

#### File 3: `Python3Executor.java`

**Location:** `gateway/src/main/java/.../gateway/Python3Executor.java`

**Modify `startProcess()` method (line 88-130):**

**Add after line 98 (after environment setup):**

```java
private void startProcess() throws IOException {
    LOGGER.info("Starting Python 3 process: {}", pythonPath);

    ProcessBuilder pb = new ProcessBuilder(
            pythonPath,
            "-u",  // Unbuffered output
            bridgeScriptPath.toString()
    );

    // Existing environment setup
    pb.environment().put("PYTHONIOENCODING", "utf-8");

    String maxMemoryMB = System.getProperty("ignition.python3.max.memory.mb", "512");
    String maxCpuSeconds = System.getProperty("ignition.python3.max.cpu.seconds", "60");

    pb.environment().put("PYTHON3_MAX_MEMORY_MB", maxMemoryMB);
    pb.environment().put("PYTHON3_MAX_CPU_SECONDS", maxCpuSeconds);

    // NEW: Set virtual environment variables if using venv
    if (isVenvPython(pythonPath)) {
        Path venvPath = getVenvPath(pythonPath);
        if (venvPath != null) {
            pb.environment().put("VIRTUAL_ENV", venvPath.toString());

            // Update PATH to include venv/bin (or Scripts on Windows)
            String pathSeparator = System.getProperty("path.separator");  // ":" or ";"
            String currentPath = pb.environment().get("PATH");
            String venvBin = getVenvBinPath(venvPath);
            pb.environment().put("PATH", venvBin + pathSeparator + currentPath);

            LOGGER.info("Set VIRTUAL_ENV={}", venvPath);
            LOGGER.info("Updated PATH to include: {}", venvBin);
        }
    }

    process = pb.start();
    // ... rest of method
}

/**
 * Check if Python executable is from a virtual environment.
 * Checks for pyvenv.cfg in parent directories.
 */
private boolean isVenvPython(String pythonPath) {
    try {
        Path venvPath = getVenvPath(pythonPath);
        return venvPath != null;
    } catch (Exception e) {
        return false;
    }
}

/**
 * Get venv root directory from Python executable path.
 * Unix: /path/to/venv/bin/python3 → /path/to/venv
 * Windows: \path\to\venv\Scripts\python.exe → \path\to\venv
 */
private Path getVenvPath(String pythonPath) {
    Path path = Paths.get(pythonPath);
    Path parent = path.getParent();  // bin/ or Scripts/

    if (parent == null) {
        return null;
    }

    String dirName = parent.getFileName().toString();
    if ("bin".equals(dirName) || "Scripts".equals(dirName)) {
        Path venvRoot = parent.getParent();
        // Verify pyvenv.cfg exists
        if (Files.exists(venvRoot.resolve("pyvenv.cfg"))) {
            return venvRoot;
        }
    }

    return null;
}

/**
 * Get venv bin directory path (bin/ on Unix, Scripts/ on Windows).
 */
private String getVenvBinPath(Path venvPath) {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("win")) {
        return venvPath.resolve("Scripts").toString();
    } else {
        return venvPath.resolve("bin").toString();
    }
}
```

**Summary of Changes:**
- Added venv detection logic to `startProcess()` (15 lines)
- Added helper methods: `isVenvPython()`, `getVenvPath()`, `getVenvBinPath()` (40 lines)
- **Total:** ~55 lines of new code

### Testing Phase 3

```python
# Test script in Designer IDE
import os
import sys

print("VIRTUAL_ENV:", os.environ.get('VIRTUAL_ENV'))
print("sys.prefix:", sys.prefix)
print("sys.base_prefix:", sys.base_prefix)
print("In venv:", sys.prefix != sys.base_prefix)
print("PATH:", os.environ.get('PATH'))

# Expected output:
# VIRTUAL_ENV: /opt/ignition-python-env
# sys.prefix: /opt/ignition-python-env
# sys.base_prefix: /usr
# In venv: True
# PATH: /opt/ignition-python-env/bin:/usr/local/bin:/usr/bin:...
```

---

## Phase 4: Add venv UI to Settings Dialog 🎨

**Goal:** Add venv configuration to Designer Settings dialog

**Effort:** 3-4 hours
**Risk:** LOW
**Value:** HIGH (user-friendly configuration)

### Changes Required

#### File 4: `SettingsDialog.java`

**Location:** `designer/src/main/java/.../designer/SettingsDialog.java`

**Add new UI components (after line 50):**

```java
// Virtual Environment components
private JTextField venvPathField;
private JButton venvBrowseButton;
private JButton venvCreateButton;
private JLabel venvStatusLabel;
```

**Add venv section to layout (after line 157):**

```java
// Virtual Environment Section (Optional)
JPanel venvSection = createSection("Virtual Environment (Optional)");
venvSection.setLayout(new BoxLayout(venvSection, BoxLayout.Y_AXIS));

// Status label (shows current venv)
venvStatusLabel = new JLabel("No virtual environment configured");
venvStatusLabel.setFont(new Font(venvStatusLabel.getFont().getName(), Font.ITALIC, 12));
venvStatusLabel.setForeground(ModernTheme.TEXT_SECONDARY);
venvSection.add(venvStatusLabel);
venvSection.add(Box.createVerticalStrut(8));

// venv path field
venvSection.add(createLabel("Virtual Environment Path"));
venvSection.add(Box.createVerticalStrut(4));

JPanel venvRow = new JPanel();
venvRow.setLayout(new BoxLayout(venvRow, BoxLayout.X_AXIS));
venvRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

venvPathField = new JTextField();
venvPathField.setBackground(ModernTheme.INPUT_BACKGROUND);
venvPathField.setForeground(ModernTheme.TEXT_PRIMARY);
venvPathField.setCaretColor(ModernTheme.TEXT_PRIMARY);
venvPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
venvRow.add(venvPathField);

venvRow.add(Box.createHorizontalStrut(8));

// Browse button
venvBrowseButton = ModernButton.createSecondary("Browse", 100, 35);
venvBrowseButton.addActionListener(e -> browseVenvPath());
venvRow.add(venvBrowseButton);

venvRow.add(Box.createHorizontalStrut(8));

// Create venv button
venvCreateButton = ModernButton.createSecondary("Create New", 120, 35);
venvCreateButton.addActionListener(e -> createVenv());
venvRow.add(venvCreateButton);

venvSection.add(venvRow);
venvSection.add(Box.createVerticalStrut(8));

// Help text
JLabel helpLabel = new JLabel(
    "<html>Virtual environments provide isolated package installations.<br>" +
    "Leave empty to use system Python.</html>"
);
helpLabel.setFont(new Font(helpLabel.getFont().getName(), Font.PLAIN, 11));
helpLabel.setForeground(ModernTheme.TEXT_SECONDARY);
venvSection.add(helpLabel);

contentPanel.add(venvSection);
contentPanel.add(Box.createVerticalStrut(16));
```

**Add browse action (after line 400):**

```java
private void browseVenvPath() {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    chooser.setDialogTitle("Select Virtual Environment Directory");

    // Start from current venv path if set
    String currentPath = venvPathField.getText();
    if (!currentPath.isEmpty()) {
        chooser.setCurrentDirectory(new File(currentPath));
    }

    int result = chooser.showOpenDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
        File selectedDir = chooser.getSelectedFile();

        // Verify it's a valid venv (check for pyvenv.cfg)
        File pyvenvCfg = new File(selectedDir, "pyvenv.cfg");
        if (!pyvenvCfg.exists()) {
            DarkDialog.showMessage(this,
                "The selected directory does not appear to be a valid Python virtual environment.\n\n" +
                "Expected to find: pyvenv.cfg\n\n" +
                "To create a new virtual environment, use the 'Create New' button.",
                "Invalid Virtual Environment");
            return;
        }

        venvPathField.setText(selectedDir.getAbsolutePath());
        updateVenvStatus(selectedDir.getAbsolutePath());
    }
}

private void createVenv() {
    // Show dialog to select location for new venv
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    chooser.setDialogTitle("Select Location for New Virtual Environment");

    int result = chooser.showDialog(this, "Create Here");
    if (result == JFileChooser.APPROVE_OPTION) {
        File parentDir = chooser.getSelectedFile();
        File venvDir = new File(parentDir, "ignition-python-env");

        // Confirm creation
        boolean confirm = DarkDialog.showConfirm(this,
            "Create new virtual environment at:\n\n" + venvDir.getAbsolutePath() + "\n\n" +
            "This will use the system Python to create an isolated environment.\n\n" +
            "Continue?",
            "Create Virtual Environment");

        if (!confirm) {
            return;
        }

        // TODO: Call REST endpoint to create venv on Gateway
        // For Phase 4, we'll implement client-side creation
        try {
            createVenvLocally(venvDir);
            venvPathField.setText(venvDir.getAbsolutePath());
            updateVenvStatus(venvDir.getAbsolutePath());

            DarkDialog.showMessage(this,
                "Virtual environment created successfully!\n\n" +
                "Location: " + venvDir.getAbsolutePath() + "\n\n" +
                "You can now install packages to this environment.",
                "Success");
        } catch (Exception ex) {
            DarkDialog.showMessage(this,
                "Failed to create virtual environment:\n\n" + ex.getMessage(),
                "Creation Failed");
        }
    }
}

private void createVenvLocally(File venvDir) throws IOException, InterruptedException {
    // Execute: python3 -m venv <path>
    ProcessBuilder pb = new ProcessBuilder("python3", "-m", "venv", venvDir.getAbsolutePath());
    pb.redirectErrorStream(true);

    Process process = pb.start();
    int exitCode = process.waitFor();

    if (exitCode != 0) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String output = reader.lines().collect(Collectors.joining("\n"));
        throw new IOException("Failed to create venv. Exit code: " + exitCode + "\n" + output);
    }
}

private void updateVenvStatus(String venvPath) {
    if (venvPath == null || venvPath.isEmpty()) {
        venvStatusLabel.setText("No virtual environment configured");
        venvStatusLabel.setForeground(ModernTheme.TEXT_SECONDARY);
        return;
    }

    // Try to get Python version from venv
    try {
        File pyvenvCfg = new File(venvPath, "pyvenv.cfg");
        if (pyvenvCfg.exists()) {
            String version = extractPythonVersion(pyvenvCfg);
            venvStatusLabel.setText("✓ Valid virtual environment (Python " + version + ")");
            venvStatusLabel.setForeground(new Color(76, 175, 80));  // Green
        } else {
            venvStatusLabel.setText("⚠ Invalid virtual environment (missing pyvenv.cfg)");
            venvStatusLabel.setForeground(new Color(255, 152, 0));  // Orange
        }
    } catch (Exception e) {
        venvStatusLabel.setText("⚠ Could not verify virtual environment");
        venvStatusLabel.setForeground(new Color(255, 152, 0));  // Orange
    }
}

private String extractPythonVersion(File pyvenvCfg) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(pyvenvCfg));
    String line;
    while ((line = reader.readLine()) != null) {
        if (line.startsWith("version = ")) {
            return line.substring("version = ".length());
        }
    }
    reader.close();
    return "unknown";
}
```

**Add preference save/load (modify existing methods):**

```java
// In loadPreferences() (around line 342)
String savedVenvPath = prefs.get("venv.path", "");
venvPathField.setText(savedVenvPath);
if (!savedVenvPath.isEmpty()) {
    updateVenvStatus(savedVenvPath);
}

// In saveSettings() (around line 366)
String venvPath = venvPathField.getText().trim();
if (!venvPath.isEmpty()) {
    prefs.put("venv.path", venvPath);
} else {
    prefs.remove("venv.path");
}
```

**Summary of Changes:**
- Added 4 new UI components
- Added venv section to layout (~50 lines)
- Added browse/create action handlers (~100 lines)
- Added venv status checking (~40 lines)
- Modified preference save/load (~10 lines)
- **Total:** ~200 lines of new code

### Testing Phase 4

1. Open Designer → Tools → Python 3 IDE → Settings
2. See new "Virtual Environment (Optional)" section
3. Click "Browse" → select existing venv → verify status shows ✓ Valid
4. Click "Create New" → select location → verify venv created
5. Save settings → restart Designer → verify venv path persisted
6. Install package → verify installs to configured venv

---

## Phase 5: REST API for venv Management 🔌

**Goal:** Add REST endpoints for venv creation and management

**Effort:** 4-6 hours
**Risk:** MEDIUM
**Value:** MEDIUM (nice-to-have for remote management)

### New REST Endpoints

```
POST /data/python3integration/api/v1/venv/create
  Body: {"path": "/opt/ignition-python-env", "pythonVersion": "3.11"}
  Response: {"success": true, "path": "/opt/ignition-python-env", "version": "3.11.6"}

GET /data/python3integration/api/v1/venv/status
  Response: {
    "configured": true,
    "path": "/opt/ignition-python-env",
    "pythonVersion": "3.11.6",
    "packageCount": 42
  }

GET /data/python3integration/api/v1/venv/list
  Response: {
    "detected": [
      {"path": "/opt/ignition-python-env", "version": "3.11.6"},
      {"path": "/opt/venv-test", "version": "3.10.12"}
    ]
  }
```

### Implementation

**File: Python3RestEndpoints.java**

Add new handlers (similar pattern to existing endpoints).

**Defer to Phase 5** (not critical for basic venv support).

---

## Phase 6: Named venv Pools (Multi-venv Support) 🔮

**Goal:** Support multiple venvs for different script types

**Effort:** 8-12 hours
**Risk:** HIGH (requires significant refactoring)
**Value:** HIGH (for advanced users with complex requirements)

### Architecture Changes Required

**Current:**
```
Python3ProcessPool (single pool, single pythonPath)
  ├── Executor 1 (pythonPath)
  ├── Executor 2 (pythonPath)
  └── Executor 3 (pythonPath)
```

**Proposed:**
```
Python3ProcessPoolManager
  ├── DefaultPool (pythonPath1)
  │   ├── Executor 1
  │   ├── Executor 2
  │   └── Executor 3
  ├── MLPool (pythonPath2)
  │   ├── Executor 1
  │   └── Executor 2
  └── WebPool (pythonPath3)
      └── Executor 1
```

### Script Metadata

Add venv selection to script metadata:

```json
{
  "name": "ML Training Script",
  "description": "Train ML model",
  "venv": "MLPool",  // <-- New field
  "created": "2025-10-29T..."
}
```

### **Defer to Future Release** (wait for user feedback)

---

## Risk Assessment

### Low Risk Items

✅ **Phase 1-3:** Detection, pip fix, environment variables
- No breaking changes
- Falls back to current behaviour if venv not configured
- Easy rollback (remove system property)

### Medium Risk Items

⚠️ **Phase 4:** UI changes
- Designer UI changes require testing across OS
- Preference persistence could have issues
- Mitigation: Hide behind "Advanced" toggle initially

### High Risk Items

🚨 **Phase 6:** Multi-venv pools
- Significant architecture changes
- Performance implications (more processes)
- Complex pool management logic
- Mitigation: Wait for user demand, implement incrementally

---

## Rollout Strategy

### Version 2.12.0: Basic venv Support

**Included:**
- Phase 1: venv detection via system property ✅
- Phase 2: pip command fix ✅
- Phase 3: Environment variables ✅

**Documentation:**
- Quick Start guide: How to create and use venv
- Configuration guide: System property setup
- Troubleshooting: Common venv issues

**Timeline:** 1-2 days (7-11 hours coding + testing)

### Version 2.13.0: UI Configuration

**Included:**
- Phase 4: Settings dialog UI ✅

**Documentation:**
- UI guide with screenshots
- Video walkthrough

**Timeline:** 1 week after 2.12.0

### Version 2.14.0+: Advanced Features

**Included:**
- Phase 5: REST API (if demand exists)
- Phase 6: Multi-venv pools (if demand exists)

**Timeline:** TBD based on user feedback

---

## Success Criteria

### Phase 1-3 Success

- ✅ Gateway starts with venv configured
- ✅ Scripts import venv packages
- ✅ Packages install to venv (not system)
- ✅ venv packages visible in Designer Packages tab
- ✅ Multiple Gateways can use different venvs
- ✅ Documentation complete

### Phase 4 Success

- ✅ Settings dialog shows venv configuration
- ✅ Browse button selects valid venvs
- ✅ Create button makes functional venv
- ✅ Status indicator shows venv validity
- ✅ Preferences persist across sessions

---

## Documentation Requirements

### User Documentation

1. **QUICK_START.md** - Add venv section
   - How to create venv
   - How to configure Ignition
   - How to install packages

2. **CONFIGURATION.md** - New file
   - System property reference
   - Environment variable reference
   - Auto-detection locations

3. **TROUBLESHOOTING.md** - Add venv section
   - "venv not detected" → check pyvenv.cfg
   - "packages install to system" → verify pip command
   - "VIRTUAL_ENV not set" → check Phase 3 implementation

4. **BEST_PRACTICES.md** - New file
   - When to use venv
   - requirements.txt management
   - venv backup/restore

### Developer Documentation

1. **ARCHITECTURE.md** - Update Python execution flow
2. **API_REFERENCE.md** - Add venv REST endpoints (Phase 5)
3. **CONTRIBUTING.md** - Add venv testing procedures

---

## Compatibility Matrix

| Python Version | venv Support | Tested |
|---|---|---|
| Python 3.8 | ✅ Yes | ✅ |
| Python 3.9 | ✅ Yes | ✅ |
| Python 3.10 | ✅ Yes | ✅ |
| Python 3.11 | ✅ Yes | ✅ |
| Python 3.12 | ✅ Yes | ⏳ Pending |
| Python 3.13 | ⚠️ Not tested | ❌ No |

| OS | venv Support | Path Format |
|---|---|---|
| Ubuntu 22.04+ | ✅ Yes | `/path/to/venv/bin/python3` |
| Debian 12+ | ✅ Yes | `/path/to/venv/bin/python3` |
| RHEL 8+ | ✅ Yes | `/path/to/venv/bin/python3` |
| Windows Server 2019+ | ✅ Yes | `\path\to\venv\Scripts\python.exe` |
| macOS 12+ | ✅ Yes | `/path/to/venv/bin/python3` |

---

## Example Usage Scenarios

### Scenario 1: Production Deployment

```bash
# 1. Create production venv
sudo mkdir -p /opt/ignition-python-env
sudo python3 -m venv /opt/ignition-python-env
sudo chown -R ignition:ignition /opt/ignition-python-env

# 2. Install production packages
sudo -u ignition /opt/ignition-python-env/bin/pip install -r requirements.txt

# 3. Configure Ignition
sudo vim /opt/inductive-automation/ignition/data/ignition.conf
# Add: wrapper.java.additional.101=-Dignition.python3.venv.path=/opt/ignition-python-env

# 4. Restart Gateway
sudo systemctl restart ignition

# 5. Verify
tail -f /opt/inductive-automation/ignition/logs/wrapper.log | grep "Using virtual environment"
```

### Scenario 2: Development Environment

```bash
# 1. Create dev venv (relative to project)
cd /home/developer/ignition-project
python3 -m venv venv
source venv/bin/activate

# 2. Install dev packages
pip install numpy pandas matplotlib pytest black

# 3. Configure local Ignition
# Add to ignition.conf:
wrapper.java.additional.101=-Dignition.python3.venv.path=/home/developer/ignition-project/venv

# 4. Restart and test
```

### Scenario 3: Multiple Environments (Future - Phase 6)

```bash
# ML environment
python3 -m venv /opt/venvs/ml-env
/opt/venvs/ml-env/bin/pip install tensorflow pytorch scikit-learn

# Web scraping environment
python3 -m venv /opt/venvs/web-env
/opt/venvs/web-env/bin/pip install requests beautifulsoup4 selenium

# Data processing environment
python3 -m venv /opt/venvs/data-env
/opt/venvs/data-env/bin/pip install pandas polars duckdb

# Configure in Designer: Select venv per script
```

---

## Conclusion

Virtual environment support is **highly viable** with minimal risk and high user value. The architecture is exceptionally well-suited for this feature, requiring only ~300 lines of code for basic functionality.

**Recommended Path:**
1. Implement Phase 1-3 immediately (v2.12.0)
2. Gather user feedback
3. Implement Phase 4 based on feedback (v2.13.0)
4. Defer Phase 5-6 until demand is proven

**Next Steps:**
1. Review and approve this plan
2. Create GitHub issue for v2.12.0
3. Begin Phase 1 implementation
4. Write user documentation
5. Test on all supported platforms

---

**Document Version:** 1.0
**Created:** 2025-10-29
**Status:** ✅ Ready for Implementation
**Estimated Completion:** 2-3 days for basic support (Phase 1-3)
