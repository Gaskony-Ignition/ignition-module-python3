# Python 3 IDE - Modern UI Implementation Plan

**Status:** Phase 2 Complete (3/7 tasks done)
**Started:** October 22, 2025
**Target:** Match Web UI design from python3moduleWEB.png

---

## 🎯 Goals

Transform the Java Swing IDE to match the modern Web UI design:
1. ✅ **Auto-detected Gateway URL** - Automatically use system properties/env vars
2. ✅ **Settings Dialog** - Professional settings panel (Settings.png)
3. ✅ **Info Dialog** - Module/Python version info
4. ⏳ **Python Packages Dialog** - Package management UI (pythonPackages.png)
5. ⏳ **Modern Top Toolbar** - Redesign to match Web UI style (python3moduleWEB.png)
6. ⏳ **REST API Methods** - Package management endpoints
7. ⏳ **Testing** - Verify all features work

---

## ✅ Completed Tasks

### Task 1: Auto-detect Gateway URL ✅
**Files Modified:**
- `Python3IDE.java` - Added gateway auto-detection

**Implementation:**
```java
// New preferences constants
private static final String PREF_GATEWAY_OVERRIDE = "python3ide.gateway.override";
private static final String PREF_AUTO_CONNECT = "python3ide.gateway.autoconnect";
private static final String PREF_POOL_SIZE = "python3ide.pool.size";

// New instance variables
private String detectedGatewayUrl;   // Auto-detected from system properties
private String effectiveGatewayUrl;  // URL actually used for connections

// Auto-detection method
private String detectGatewayUrl() {
    // 1. Try system property: -Dignition.python3.gateway.url=http://localhost:9088
    // 2. Try environment variable: IGNITION_GATEWAY_URL
    // 3. Default to: http://localhost:8088
}
```

**How it works:**
- Constructor calls `detectGatewayUrl()` to auto-detect gateway
- Checks preferences for user override
- Uses override if set, otherwise uses detected URL
- Auto-connects on startup if preference enabled (default: true)

---

### Task 2: Settings Dialog ✅
**Files Created:**
- `SettingsDialog.java` (~360 lines)

**Files Modified:**
- `Python3IDE.java` - Added helper methods:
  - `getDetectedGatewayUrl()` - Returns auto-detected URL
  - `reloadSettingsFromPreferences()` - Reloads after settings change

**UI Components:**
- **Detected Gateway URL** (read-only) - Shows auto-detected value
- **Gateway URL Override** (editable) - Optional manual override
- **Effective URL** (read-only, blue text) - Shows which URL will be used
- **Auto-connect on startup** (checkbox) - Default: true
- **Pool Size** (dropdown 1-20) - Default: 3
- **Connect to Gateway** (primary blue button)
- **Reset to Defaults / Cancel / Save Settings** (buttons)

**Preferences Storage:**
```
python3ide.gateway.override = "" (empty = use auto-detected)
python3ide.gateway.autoconnect = true
python3ide.pool.size = 3
python3ide.theme = "dark"
python3ide.fontsize = 12
```

**Integration:**
- Dialog will be opened by Settings button in toolbar
- Settings persist to Java Preferences
- IDE reloads settings after dialog closes

---

### Task 3: Create InfoDialog.java ✅
**Files Created:**
- `InfoDialog.java` (~340 lines)

**Files Modified:**
- `Python3IDE.java` - Added `getRestClient()` helper method

**UI Components:**
- **Header** - Title "Python 3 IDE" with subtitle
- **Module Information Section:**
  - Module version (from version.properties: major.minor.patch)
  - Python version (from REST API)
- **Gateway Connection Section:**
  - Connection status (Connected/Disconnected with color coding)
- **Process Pool Statistics Section:**
  - Pool size
  - Healthy processes (green if all healthy, yellow if some unhealthy)
  - Available processes
  - In use processes (blue if active, gray if none)
- **Refresh Button** - Reload data from Gateway
- **Close Button** - Close dialog

**Implementation Details:**
```java
// Load module version from version.properties at runtime
InputStream is = getClass().getClassLoader().getResourceAsStream("version.properties");
Properties props = new Properties();
props.load(is);
String version = props.get("version.major") + "." + props.get("version.minor") + "." + props.get("version.patch");

// Fetch data from Gateway via REST client
Python3RestClient restClient = idePanel.getRestClient();
String pythonVersion = restClient.getPythonVersion();
PoolStats poolStats = restClient.getPoolStats();
```

**Color Coding:**
- Connected status → Green (`ModernTheme.SUCCESS`)
- Disconnected status → Red (`ModernTheme.ERROR`)
- Healthy processes → Green if all healthy, orange if degraded (`ModernTheme.WARNING`)
- In use processes → Blue if active (`ModernTheme.ACCENT_PRIMARY`)
- Module/Python versions → Blue (`ModernTheme.ACCENT_PRIMARY`)

**Integration:**
- Dialog will be opened by Info button in toolbar (Task 5)
- Displays real-time data from Gateway
- Refresh button reloads data without closing dialog
- Gracefully handles disconnected state

---

## ⏳ Pending Tasks

### Task 4: Create PackagesDialog.java ⏳
**Reference:** pythonPackages.png screenshot

**Purpose:** Python package management UI

**UI Sections:**

**A. Warning Banner** (if not connected)
```
⚠️ Not connected to Gateway. Please connect first to manage packages.
```

**B. Search PyPI**
- Search field + Search button
- Subtitle: "Search for exact package name to see details and install"

**C. Install from PyPI**
- Package name field (e.g., numpy, pandas, requests)
- Install button
- Subtitle: "Enter a package name from PyPI. Version can be specified (e.g., numpy==1.24.0)"

**D. Upload .whl File (Air-gapped Install)**
- File chooser + Upload button
- Subtitle: "Upload a .whl file for offline/air-gapped installations"

**E. Installed Packages Table**
- Table columns: Package Name | Version | Actions
- Refresh / Deselect All / Uninstall (X) buttons
- Shows count: "Installed Packages (0)"

**Files to Create:**
- `PackagesDialog.java` (~600 lines)

**Backend REST API Endpoints Needed:**
```
GET  /data/python3integration/api/v1/packages/list
POST /data/python3integration/api/v1/packages/search
POST /data/python3integration/api/v1/packages/install
POST /data/python3integration/api/v1/packages/upload
DELETE /data/python3integration/api/v1/packages/uninstall
```

**Note:** Gateway REST endpoints need to be implemented separately (UI calls them, they will 404 until implemented)

---

### Task 5: Redesign Python3IDE Toolbar ⏳
**Reference:** python3moduleWEB.png screenshot

**Current State:**
- Cluttered toolbar with text labels
- Gateway URL text field visible
- Theme dropdown visible
- Connect button visible

**Target State:**
Clean icon-based toolbar matching Web UI:
```
[Gateway URL display] [Execute] [Clear] [Save] [Save As...] [Import...] [Export...]
[A-] [A+] [Warp Emulation ▼] [📦 Packages] [⚙️ Settings] [ℹ️ Info]
```

**Changes Required:**
- **Remove:** Gateway URL text field (move to Settings dialog)
- **Remove:** Theme dropdown (move to Settings dialog - future)
- **Remove:** Connect button (move to Settings dialog)
- **Add:** Packages button (box icon) → opens PackagesDialog
- **Add:** Settings button (gear icon) → opens SettingsDialog
- **Add:** Info button (i icon) → opens InfoDialog
- **Redesign:** All buttons to use icon-based style
- **Add:** Gateway URL display (read-only label, shows effective URL)
- **Add:** Warp Emulation dropdown (execution mode selector)

**Button Style:**
- Icon buttons with tooltips (no text labels except Execute)
- Modern flat design using `ModernButton`
- Hover states with `ModernTheme.BUTTON_HOVER`

**Files to Modify:**
- `Python3IDE.java` - Major toolbar redesign in `initComponents()` and `layoutComponents()`
- `ModernButton.java` - Add `createIconButton(Icon icon, String tooltip)` method

**Implementation Steps:**
1. Create icon resources or use Unicode symbols
2. Add new button fields to Python3IDE class
3. Modify `initComponents()` to create icon buttons
4. Modify `layoutComponents()` to arrange new toolbar
5. Add action listeners:
   - Settings button → `openSettingsDialog()`
   - Packages button → `openPackagesDialog()`
   - Info button → `openInfoDialog()`

---

### Task 6: Add Package Management REST Methods ⏳
**Files to Modify:**
- `Python3RestClient.java`

**Methods to Add:**
```java
// List installed packages
public List<PackageInfo> listPackages() throws IOException
    → GET /data/python3integration/api/v1/packages/list

// Search PyPI for package
public PackageSearchResult searchPackage(String name) throws IOException
    → POST /data/python3integration/api/v1/packages/search
    → Body: {"name": "numpy"}

// Install package from PyPI
public InstallResult installPackage(String name, String version) throws IOException
    → POST /data/python3integration/api/v1/packages/install
    → Body: {"name": "numpy", "version": "1.24.0"}

// Upload .whl file
public InstallResult uploadWheel(File whlFile) throws IOException
    → POST /data/python3integration/api/v1/packages/upload
    → Multipart form data with file

// Uninstall package
public UninstallResult uninstallPackage(String name) throws IOException
    → DELETE /data/python3integration/api/v1/packages/uninstall?name=numpy
```

**Data Classes to Create:**
```java
public class PackageInfo {
    private String name;
    private String version;
    // getters/setters
}

public class PackageSearchResult {
    private boolean found;
    private String name;
    private String latestVersion;
    private String description;
    // getters/setters
}

public class InstallResult {
    private boolean success;
    private String message;
    private String packageName;
    private String version;
    // getters/setters
}

public class UninstallResult {
    private boolean success;
    private String message;
    // getters/setters
}
```

---

### Task 7: Testing & Integration ⏳
**Test Scenarios:**

**Settings Dialog:**
- [ ] Open Settings dialog from toolbar
- [ ] Verify detected URL is correct
- [ ] Set gateway override and save
- [ ] Verify override is used for connections
- [ ] Toggle auto-connect and verify behavior
- [ ] Change pool size and verify saved
- [ ] Click "Reset to Defaults" and verify reset
- [ ] Click "Connect to Gateway" and verify connection

**Info Dialog:**
- [ ] Open Info dialog from toolbar
- [ ] Verify module version displayed correctly
- [ ] Verify Python version from gateway shown
- [ ] Verify connection status accurate
- [ ] Verify pool stats displayed

**Packages Dialog:**
- [ ] Open Packages dialog from toolbar
- [ ] Verify warning shown when not connected
- [ ] Search for package (will 404 until gateway implemented)
- [ ] Install package (will 404 until gateway implemented)
- [ ] Upload .whl file (will 404 until gateway implemented)
- [ ] Refresh installed packages list
- [ ] Uninstall package (will 404 until gateway implemented)

**Toolbar:**
- [ ] Verify all icon buttons display correctly
- [ ] Verify tooltips show on hover
- [ ] Verify Settings button opens SettingsDialog
- [ ] Verify Packages button opens PackagesDialog
- [ ] Verify Info button opens InfoDialog
- [ ] Verify gateway URL display shows effective URL
- [ ] Verify all existing buttons still work (Execute, Save, etc.)

**Preferences Persistence:**
- [ ] Close and reopen IDE
- [ ] Verify gateway override persists
- [ ] Verify auto-connect setting persists
- [ ] Verify pool size persists
- [ ] Verify theme persists
- [ ] Verify font size persists

---

## 🎨 Theme Consistency

All new dialogs follow existing dark theme pattern:
- **Background:** `ModernTheme.BACKGROUND_DARK` (#1E1E1E)
- **Panels:** `ModernTheme.PANEL_BACKGROUND` (#252526)
- **Borders:** `ModernTheme.BORDER_DEFAULT` (#404040)
- **Primary buttons:** Blue `ModernTheme.ACCENT_PRIMARY`
- **Text:** `ModernTheme.FOREGROUND_PRIMARY` (#CCCCCC)
- **Secondary text:** `ModernTheme.FOREGROUND_SECONDARY` (#969696)
- **Warning banner:** Orange background with dark border
- **Input fields:** `ModernTheme.INPUT_BACKGROUND` with proper borders

---

## 📁 File Structure

```
designer/src/.../designer/
├── SettingsDialog.java        ✅ Created (~360 lines)
├── InfoDialog.java            ✅ Created (~340 lines)
├── PackagesDialog.java         ⏳ To create (~600 lines)
├── Python3IDE.java             ✅ Modified (toolbar redesign pending)
├── Python3RestClient.java      ⏳ To modify (add package methods)
├── ModernButton.java           ⏳ To modify (add icon button factory)
└── ModernTheme.java            ✅ Already has all needed colors
```

---

## 🚀 Implementation Order

1. ✅ **Auto-detect gateway** (DONE)
2. ✅ **Create SettingsDialog** (DONE)
3. ✅ **Create InfoDialog** (DONE)
4. ⏳ **Create PackagesDialog** (NEXT)
5. ⏳ **Redesign toolbar** (add buttons for new dialogs)
6. ⏳ **Add REST client methods** (package management stubs)
7. ⏳ **Polish & testing**

---

## 📊 Estimated Effort

- **Phase 1 (Complete):** ~400 lines (auto-detect + SettingsDialog)
- **Phase 2 (Complete):** ~340 lines (InfoDialog)
- **Phase 3 (Pending):** ~800 lines (PackagesDialog + REST methods)
- **Phase 4 (Pending):** ~200 lines (toolbar redesign)
- **Total:** ~1740 lines of new/modified code

---

## 🔧 Technical Notes

### Gateway Auto-Detection Priority
1. System property: `-Dignition.python3.gateway.url=http://localhost:9088`
2. Environment variable: `IGNITION_GATEWAY_URL`
3. Default: `http://localhost:8088`

### Preferences Keys
```java
python3ide.gateway.override     // String (empty = use detected)
python3ide.gateway.autoconnect  // boolean (default: true)
python3ide.pool.size            // int (default: 3, range: 1-20)
python3ide.theme                // String (existing)
python3ide.fontsize             // int (existing)
```

### Dialog Pattern
All dialogs extend `JDialog` and follow this pattern:
```java
public class XyzDialog extends JDialog {
    private final Python3IDE idePanel;
    private final Preferences prefs;

    public XyzDialog(Frame parent, Python3IDE idePanel) {
        super(parent, "Title", true);
        this.idePanel = idePanel;
        this.prefs = Preferences.userNodeForPackage(Python3IDE.class);

        initComponents();
        layoutComponents();
        loadSettings();

        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
}
```

---

## ✅ Success Criteria

- [x] Gateway URL auto-detected from system properties/env vars
- [x] Settings dialog opens and saves preferences correctly
- [x] Info dialog shows module/Python version and connection status
- [ ] Packages dialog UI complete (API calls will 404 until gateway endpoints implemented)
- [ ] Top toolbar matches Web UI screenshot style
- [ ] All buttons have proper icons and tooltips
- [ ] Dark theme consistent across all new dialogs
- [ ] Preferences persist across IDE sessions
- [ ] All existing features still work (backward compatible)

---

## 🐛 Known Issues / Future Work

1. **Gateway REST endpoints not implemented yet** - Package management will return 404 until gateway-side endpoints are added
2. **Icon resources** - May need to create or import icon files for toolbar buttons (currently planning Unicode symbols as fallback)
3. **Theme selector** - Currently in toolbar, should move to Settings dialog in future
4. **Font size controls** - Currently in toolbar (A- / A+ buttons), could move to Settings in future
5. **Pool size configuration** - Settings dialog allows changing, but may need gateway restart to take effect

---

## 📝 Version Planning

- **v2.7.0** - Modern UI overhaul (this plan)
  - Auto-detect gateway URL
  - Settings dialog
  - Info dialog
  - Packages dialog UI (no backend)
  - Toolbar redesign

- **v2.8.0** - Package management backend (future)
  - Gateway REST API endpoints
  - Package installation logic
  - .whl file handling
  - PyPI search integration

---

**Last Updated:** October 22, 2025
**Implementation Status:** 3/7 tasks complete (43%)
