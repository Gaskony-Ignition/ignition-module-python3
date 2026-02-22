# Python 3 Integration Module for Ignition

**Current Version: v3.7.0** | [Changelog](../CHANGELOG.md) | [GitHub](https://github.com/Gaskony-Ignition/ignition-module-python3)

**Status:** ✅ Production Ready - Complete security implementation with comprehensive documentation

This module enables Python 3 scripting functions in Ignition 8.3+, allowing you to use modern Python 3 features and libraries alongside Ignition's built-in Jython 2.7 environment.

---

## ⭐ Key Features

### Core Functionality
- ⚡ **Python 3 Execution** - Execute Python 3 code from Ignition scripts
- 🎨 **Designer IDE** - Interactive Python 3 IDE with code editor, output panel, and diagnostics
- 🔄 **Process Pool** - Efficient subprocess pooling for minimal overhead
- 📦 **Full Library Support** - Use numpy, pandas, requests, and any Python 3 package
- 🌐 **REST API** - HTTP endpoints for external integration
- 🔍 **Auto Health Checking** - Self-healing process pool with automatic restart

### Security & Enterprise (v2.6.0+)
- 🔐 **Three-Tier Security Model** - DESIGNER_ADMIN, ADMIN, and RESTRICTED modes
- ✅ **AST-Based Validation** - Prevents code injection and bypass attempts
- 📊 **Comprehensive Audit Logging** - JSON-formatted logs for compliance
- 🔑 **API Key Authentication** - 32+ character admin key with HTTPS enforcement
- 🧪 **184 Tests Passing** - 19% code coverage with security validation

---

## 🚀 Quick Start

### Prerequisites

1. **Ignition 8.3.0+**
2. **Java 17** (required by Ignition 8.3)

**Note:** The module includes Python 3 bundled - no separate Python installation needed!

### Installation

1. **Download** the latest `.modl` file from [Releases](https://github.com/Gaskony-Ignition/ignition-module-python3/releases)
2. Open Ignition Gateway web interface (http://localhost:8088)
3. Navigate to **Config → System → Modules**
4. Click **Install or Upgrade a Module**
5. Select the `.modl` file
6. Click **Install**
7. Module status should show **Running**

**Need detailed installation instructions?** See [Quick Start Guide](docs/getting-started/QUICK_START.md)

### First Script

**In Ignition Script Console:**
```python
# Execute Python 3 code
result = system.python3.exec("result = 2 + 2")
print(result)  # Prints: 4

# Use Python 3 libraries
code = """
import math
result = math.sqrt(16)
"""
result = system.python3.exec(code)
print(result)  # Prints: 4.0
```

**In Designer IDE** (Tools → Python 3 IDE):
1. Connect to Gateway
2. Write Python code in editor
3. Click **Execute** (or press Ctrl+Enter)
4. View output in Output panel

---

## 📚 Documentation

### Getting Started
- **[Quick Start Guide](docs/getting-started/QUICK_START.md)** - Installation, setup, and getting up and running in 30 minutes
- **[Keyboard Shortcuts](docs/getting-started/KEYBOARD_SHORTCUTS.md)** - Complete keyboard reference for IDE

### Operations
- **[Package Management](docs/operations/PACKAGE_MANAGEMENT.md)** - Installing and managing Python packages
- **[Virtual Environments](docs/operations/VIRTUAL_ENVIRONMENT.md)** - Using Python venvs (v2.12.0+)
- **[Air-Gapped Deployment](docs/operations/AIR_GAPPED_DEPLOYMENT.md)** - Bundling packages for offline environments
- **[Troubleshooting](docs/operations/TROUBLESHOOTING.md)** - Common issues and solutions
- **[Deployment](docs/operations/DEPLOYMENT.md)** - Production deployment guide
- **[Monitoring](docs/operations/MONITORING.md)** - Performance monitoring and metrics
- **[Backup & Recovery](docs/operations/BACKUP.md)** - Backup strategies and disaster recovery

### Development
- **[Testing Guide](docs/development/testing/README.md)** - Running tests and writing new tests
- **[Version Workflow](docs/development/VERSION_WORKFLOW.md)** - Version numbering and release process
- **[E2E Tests](docs/development/E2E_TESTS.md)** - End-to-end testing guide

### Architecture & API
- **[Architecture Overview](docs/architecture/OVERVIEW.md)** - System design and components
- **[Designer IDE Documentation](docs/api/DESIGNER_IDE.md)** - IDE features and usage
- **[REST API Reference](docs/api/REST_API.md)** - HTTP endpoints and examples

### Security
- **[Security Overview](docs/security/SECURITY_OVERVIEW.md)** ⭐ **START HERE** - Security architecture and modes
- **[Security Configuration](docs/security/SECURITY_CONFIG.md)** - Production security settings
- **[Audit Checklist](docs/security/AUDIT_CHECKLIST.md)** - Security audit and compliance

### Roadmap
- **[Consolidated Roadmap](docs/roadmap/CONSOLIDATED_ROADMAP.md)** - Feature roadmap and future plans

---

## 💡 Common Use Cases

### 1. Data Processing with Pandas
```python
code = """
import pandas as pd

# Read CSV from Gateway
df = pd.read_csv('/path/to/data.csv')

# Process data
summary = df.groupby('category')['value'].sum()
result = summary.to_dict()
"""

data = system.python3.exec(code)
print(data)
```

### 2. Web API Calls
```python
code = """
import requests

# Make API call
response = requests.get('https://api.example.com/data')
result = response.json()
"""

api_data = system.python3.exec(code)
print(api_data)
```

### 3. Machine Learning
```python
code = """
import numpy as np
from sklearn.linear_model import LinearRegression

# Train model
X = np.array([[1], [2], [3], [4]])
y = np.array([2, 4, 6, 8])

model = LinearRegression()
model.fit(X, y)

# Predict
prediction = model.predict([[5]])
result = prediction[0]
"""

prediction = system.python3.exec(code)
print(prediction)  # Prints: 10.0
```

---

## 🏗️ Architecture

### Subprocess + Process Pool Approach

```
┌─────────────────────────────────────────┐
│        Ignition Gateway (Java)          │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │   Python3ProcessPool            │   │
│  │   (3-20 warm processes)         │   │
│  │                                 │   │
│  │  ┌──────────┐  ┌──────────┐   │   │
│  │  │ Python3  │  │ Python3  │   │   │
│  │  │ Process  │  │ Process  │   │   │
│  │  │    #1    │  │    #2    │ ... │   │
│  │  └──────────┘  └──────────┘   │   │
│  │                                 │   │
│  └─────────────────────────────────┘   │
│           ↕ JSON via stdin/stdout       │
└─────────────────────────────────────────┘
```

**Benefits**:
- ✅ No complex dependencies (no Py4J, no JEP)
- ✅ Stable and reliable (isolated processes)
- ✅ Easy to debug (standard Python logging)
- ✅ Minimal overhead (process reuse)

---

## 📦 Building from Source

```bash
# Clone repository
git clone https://github.com/Gaskony-Ignition/ignition-module-python3.git
cd ignition-module-python3/python3-integration

# Build with Gradle
./gradlew clean build --no-daemon

# Module output
ls build/libs/Python3Integration-signed.modl
```

**All 184+ tests must pass before commits.**

---

## 🔧 Configuration

### Pool Size

Adjust Python process pool size in `ignition.conf`:
```properties
wrapper.java.additional.X=-Dignition.python3.poolsize=5
```

Default: 3 processes

### Python Path (Optional)

Specify custom Python installation:
```properties
wrapper.java.additional.X=-Dignition.python3.path=/usr/bin/python3.11
```

**Note:** Module includes bundled Python, custom path usually not needed.

---

## 🆘 Support

### Documentation
- **📖 [Complete Documentation](docs/)** - Comprehensive guides and references
- **🔍 [Troubleshooting Guide](docs/operations/TROUBLESHOOTING.md)** - Common issues and solutions
- **❓ [FAQ](docs/operations/TROUBLESHOOTING.md#faq)** - Frequently asked questions

### Community
- **🐛 [Report Issues](https://github.com/Gaskony-Ignition/ignition-module-python3/issues)** - Bug reports and feature requests
- **💬 [Discussions](https://github.com/Gaskony-Ignition/ignition-module-python3/discussions)** - Questions and community support
- **📧 Email**: support@example.com

---

## 🤝 Contributing

We welcome contributions!

### Development Setup
1. Clone repository
2. Build module: `./gradlew build`
3. Run tests: `./gradlew test`
4. Install in local Ignition for testing

### Running Tests
```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests Python3ExecutorTest

# With coverage
./gradlew test jacocoTestReport
```

---

## 📜 License

This project is licensed under the Apache License 2.0 - see [LICENSE](../LICENSE) file for details.

---

## 🙏 Credits

**Developed by:** Nigel Gwork
**Powered by:** [Ignition SDK](https://github.com/inductiveautomation/ignition-sdk-examples)
**Built with:** ❤️ and ☕

**Special Thanks:**
- Inductive Automation for the Ignition platform and SDK
- Python community for excellent libraries
- Contributors and testers

---

## 📈 Changelog

**Latest Release:** v3.7.0 (February 2026)

### Recent Changes

**v3.7.0** - Split Python3RestEndpoints God class into handler companion classes
- **`EndpointContext`** — package-private class holding all 9 service dependencies, passed to each handler class
- **`ExecutionHandlers`** — 11 handlers: exec, eval, call-module, call-script, check-syntax, completions, example, shell session create/exec/close, auth/session
- **`ScriptAndPackageHandlers`** — 12 handlers: script CRUD (save/load/list/delete/available), package catalog/status/install/uninstall/verify, PyPI search/info
- **`MonitoringHandlers`** — 19 handlers: version, pool-stats, pool-size, health, versions, diagnostics, metrics, gateway-impact, script-metrics, historical, alerts, enhanced metrics, circuit-breaker, alert-manager, prometheus, logs, distributions CRUD
- **`Python3RestEndpoints`** shrunk from ~3,140 lines to ~1,338 lines — now contains only routing infrastructure, security, CSRF, IP whitelist, rate limiting, and utility methods
- Handler utility methods made package-private static; companion classes call `Python3RestEndpoints.withHandler/validateCSRFIfSession/parseJsonBody/etc.`
- **GatewayHook.java unchanged** — all 10 static setters preserved with identical signatures

**v3.6.15** - Delete permanently-disabled shell-exec dead code
- **Removed `handleShellExec`** - method deprecated since v2.9.0, only returned an error response; now the route returns 404 cleanly
- **Removed shell-exec route registration** from `mountRoutes()` — `ApiEndpoints.ROUTE_SHELL_EXEC` constant kept in common scope

**v3.6.14** - REST handler wrapper: `withHandler` eliminates boilerplate from all 41 REST endpoints
- **`HandlerLogic` functional interface** - allows checked exceptions in lambda handler bodies
- **`withHandler` method** - single source of truth for entry/exit logging, security headers, and error-to-response conversion; security headers now guaranteed on all endpoints including error paths
- **41 handlers converted** - each `handleXxx` method now a one-liner wrapping the business logic; ~270 lines of repetitive try/catch/logger boilerplate removed
- **2 special-case handlers** - `handleSearchPyPI` (returns partial results on error) and `handleGetPrometheusMetrics` (writes raw text response) keep direct `applySecurityHeaders` calls

**v3.6.13** - Architectural refactoring: single source of truth for constants, utilities, and base classes (Phase A/B/C)
- **Phase A (constants extraction)** - `ApiEndpoints`, `JsonFields`, `PoolConfig` (common scope) and `PreferenceKeys` (designer scope) centralise all hardcoded strings and numbers; 40+ route paths, 50+ JSON field names, pool limits, and preference keys now changed in one place
- **Phase B (utility consolidation)** - `ApiResponse` factory replaces 79 inline `createErrorResponse()` calls in REST endpoints; `ComponentThemeHelper` removes duplicated `updatePanelBackgrounds/updateScrollPaneTheme/updateSplitPaneDividers` from `Python3IDE` and `ThemeManager`; `UiComponentFactory` adds factory methods for themed output areas
- **Phase C (base class infrastructure)** - `Themeable` interface standardises theme-switching contract for `ScriptMetadataPanel` and `DiagnosticsPanel`; `BaseModuleDialog` eliminates boilerplate from `SettingsDialog`, `PackagesDialog`, and `VersionManagerDialog`

**v3.6.12** - Designer theme pollution fix, enriched system.python3 scripting docs
- **Critical Designer fix** - `ThemeManager.applyDarkDialogTheme()` and `applyLightDialogTheme()` removed: these called `UIManager.put()` for 50+ global Swing keys (Panel.background, Label.foreground, Button.background, Menu.background, Tree.background, etc.), causing the entire Ignition Designer to randomly switch colors when the Script Console theme changed
- **Root cause** - Python3IDE.java removed these methods in v2.0.17 but ThemeManager (used by Python3ScriptConsole) still had them; now both are consistent: all theming via direct `setBackground()`/`setForeground()` only
- **Enriched scripting docs** - `Python3ScriptModule.properties` now has detailed descriptions for all `system.python3.*` functions including multi-line examples, parameter notes, and return value documentation

**v3.6.11** - UI style phases 4, 5, 7: SectionPanel card headers, DiagnosticsPanel theme support
- **Phase 7 (DarkDialog consolidation)** - InformationDialog fully delegates to DarkDialog; obsolete `InformationDialog.setDarkTheme()` call removed from Python3IDE
- **Phase 4 (createSection → createCardHeader)** - PackagesDialog, SettingsDialog, InfoDialog all use `SectionPanel` pattern with `ModernTheme.createCardHeader()` for consistent section titling
- **Phase 5 (applyTheme coverage)** - `DiagnosticsPanel.applyTheme(boolean isDark)` added; wired into `Python3IDE.applyTheme()` so diagnostics panel colors update on theme switch

**v3.6.10** - Theme cascade followup: InformationDialog fonts, Python3IDE button color constants
- **InformationDialog fonts** - 3 hardcoded `Font("Segoe UI")` / `Font("Consolas")` replaced with `ModernTheme.FONT_BOLD` / `ModernTheme.FONT_CODE`
- **Python3IDE button colors** - 6 inline `new Color(...)` light-theme button states extracted to `ModernTheme.LIGHT_PRIMARY_*` and `LIGHT_SUCCESS_*` constants
- **ModernTheme expanded** - Added `LIGHT_PRIMARY`, `LIGHT_PRIMARY_HOVER`, `LIGHT_PRIMARY_ACTIVE`, `LIGHT_SUCCESS`, `LIGHT_SUCCESS_HOVER`, `LIGHT_SUCCESS_ACTIVE`

**v3.6.9** - Theme cascade: all hardcoded colors/fonts replaced with ModernTheme constants
- **ModernTheme expanded** - Added 32 new constants: 14 light palette (`LIGHT_BACKGROUND`, `LIGHT_BORDER`, etc.), 5 semantic (`WARNING_*`, `ERROR_LIGHT`, `SUCCESS_LIGHT`), 3 editor colors, 3 spacing constants
- **DarkDialog palette fix** - Removed wrong navy palette (`#14181f`), now correctly delegates to ModernTheme VS Code Dark+ colors
- **InformationDialog cleanup** - Removed 12 private duplicate constants; all colors now cascade from ModernTheme
- **VersionManagerDialog modernized** - All 7 hardcoded fonts replaced with ModernTheme; plain JButtons replaced with ModernButton
- **Font cascade** - All `new Font("Monospaced", ...)` occurrences across 5 files replaced with `ModernTheme.FONT_CODE`
- **Light mode colors cascaded** - All inline `new Color(...)` light-mode values replaced with `LIGHT_*` constants in Python3IDE, Python3ScriptConsole, ThemeManager, ModernStatusBar, TerminalPanel, CollapsiblePanel
- **Run button color** - `ModernButton.createRunButton()` now uses `ModernTheme.SUCCESS` constant instead of inline `new Color(76,175,80)`
- **Warning banner** - PackagesDialog warning uses `WARNING_*` theme constants
- **Scrollbar colors** - ModernScrollBarUI track/thumb use `withAlpha(ModernTheme.*)` pattern
- **Last TitledBorder removed** - FindReplaceDialog now uses LineBorder with ModernTheme colors

**v3.6.8** - Duplicate console fix, theme isolation, logs in diagnostics, floating card headers, CSRF fix
- **Duplicate Console fix** - Designer Tools menu now only registers the Python 3 Script Console item once on startup; guard flag prevents multiple registrations
- **Theme isolation** - FlatLafScope now saves and restores all UIManager properties to prevent theme pollution of other Designer components
- **Module logs in Diagnostics** - Removed standalone Logs page; module log entries now appear as a table below diagnostics metrics
- **Floating card headers** - Consistent card-style headers (with gradient) on all panels: Gateway Connection, Script Browser, Script Information, Diagnostics
- **CSRF package install fix** - Designer REST client now authenticated via X-Source header, bypassing CSRF for API calls
- **Version in menu always current** - Module version now reliably read from version.properties in Designer classloader

**v3.6.7** - Menu version display, PackagesDialog fix, rename popup fix, light mode output
- **Version in Tools menu** - Restored version number: "Python 3 Script Console vX.Y.Z"
- **PackagesDialog fix** - Fixed infinite recursion in `getRestClient()` causing StackOverflowError
- **Rename popup fix** - Dialogs now appear in front of Designer (was creating ownerless dialogs behind window)
- **Light mode output** - Darker text colors (black primary, dark gray secondary), re-colors existing text on theme switch

**v3.6.5** - Theme toggle fix, delete/rename fix, Packages button, version display fix
- **Theme toggle fix** - Restructured error handling so console colors always apply even if RSTA theme fails; fixed toggle state getting stuck by updating `currentTheme` before theme load
- **Delete/Rename fix** - Changed HTTP DELETE to POST for script delete endpoint; Ignition servlet container compatibility
- **Packages button** - Added Packages button to Script Console toolbar; new PackagesDialog constructor works without Python3IDE reference
- **Version display fix** - InformationDialog now reads version dynamically from version.properties instead of hardcoded value

**v3.6.4** - Package install fix, rename/delete fix, Script Console theme fix
- **Package install/uninstall** - Replaced fragile `executeCode()` subprocess workaround with proper REST endpoint calls (`/api/v1/packages/install/:name`, `/api/v1/packages/uninstall/:name`); implemented `installPackage()` and `uninstallPackage()` in REST client
- **Script rename/delete fix** - Added URL decoding in all gateway handlers that extract names from URL paths (`handleLoadScript`, `handleDeleteScript`, `handleInstallPackage`, `handleUninstallPackage`, `handleGetPyPIInfo`); scripts with spaces or special characters now work correctly
- **Script Console theme switching** - Fixed theme toggle to properly update all components (toolbar, buttons, output pane, version combo, script name bar, status bar, separators); buttons now use dynamic `getBackground()` instead of hardcoded dark colors; added `ModernStatusBar.updateTheme()` method

**v3.6.3** - Sidebar cleanup, PyPI install fix, Designer rename fix, Project Browser stability, Script Console theme toggle
- **Sidebar cleanup** - Removed redundant "Python 3" heading from sidebar (PageHeader already shows it)
- **PyPI install fix** - Fixed version format bug (missing `==` separator), increased pip timeout to 5 minutes for large packages
- **Designer rename fix** - Fixed script and folder rename in Project Browser by adding auth token to delete requests
- **Project Browser stability** - Fixed "Python 3 Scripts" tree node collapsing every 30 seconds (now only rebuilds when data changes)
- **Script Console theme toggle** - Added Theme button to toolbar for switching between light and dark mode
- **Dark theme fix** - Removed aggressive `updateComponentTreeUI` call that was resetting dark theme colors on Script Console JFrame

**v3.6.2** - PyPI direct install, Designer dark theme fix, split toggle fix, IDE consolidation
- **PyPI install** - Package install now falls back to direct PyPI download when package not in bundled catalog
- **Designer dark theme** - Fixed white JFrame background on Script Console window; all frame internals now themed dark
- **Split toggle fix** - Fixed split orientation button to properly toggle between top/bottom and left/right layouts
- **IDE consolidated** - Removed legacy Python 3 IDE window; Script Console is now the single Designer entry point
- **Modernized UI** - Increased Script Console default window size, added window cleanup listener

**v3.6.1** - Bug fixes: Designer revert, CSRF fix, Packages fix, Logs improvements, Heading bar
- **Designer fix** - Reverted FlatLaf scoping that broke IDE and Script Console from opening
- **Full-width heading bar** - Added page header matching AI Terminal style across full width
- **CSRF fix** - Fixed CSRF token validation failures for pool-size and package install operations
- **Packages fix** - Fixed ".map is not a function" error when loading package catalog (object-to-array conversion)
- **Logs improvements** - Default filter to module logs (Python3), added pause/resume button for live logs

**v3.6.0** - Designer IDE Visual Redesign (FlatLaf, Web UI Color Alignment)
**v3.5.8** - Package Manager Init Fix, Pool Stats Fix
- **Package manager fix** - Fixed "Package manager not initialized" error by registering package manager with REST endpoints after creation in `startup()` instead of only in `initializeScriptManager()` (which runs before packageManager exists)
- **Pool size display fix** - Fixed pool size always showing 0 by correcting field name mismatch: Java returned `totalSize` but React UI expected `poolSize`
- **Pool stats consistency** - Unified pool stats field names across Java backend and all React components (Dashboard, Diagnostics, GlobalStatusBar)

**v3.5.7** - Auth Screen Matches AI Terminal Layout
- **Unified auth layout** - Auth overlay now uses identical structure, text styles, and button as the AI Terminal module (module identity → divider → lock emoji → heading → description → button)
- **Module identity** - Shows Code2 icon + "Python 3 Integration" name at 18px/600 weight, matching the shared design pattern across all Gaskony modules

**v3.5.6** - Module Branding on Auth Screen & Sidebar
- **Auth screen module badge** - Auth-required overlay now shows a "Python 3 Integration" badge with icon so users can identify which module requires login
- **Sidebar module header** - Navigation sidebar has a branded header with the module icon and name, visible in both expanded and collapsed states

**v3.5.5** - Enhanced Gateway Auth UI, AuthCheck Utility
- **Auth screen redesign** - Gateway Web UI auth-required overlay now shows a lock icon, descriptive text, and a styled login button instead of plain text
- **AuthCheck utility** - New `authCheck.ts` module with `AuthRequiredError` class for consistent auth failure detection across all components
- **GlobalStatusBar auth detection** - Status bar fetch calls now check for auth redirects instead of silently failing
- **useGatewayFetch auth detection** - Gateway fetch hook now detects HTML login redirects and surfaces clear error messages

**v3.5.4** - Designer Connection Fix, Script Console UI, Pool Size Control
- **Designer REST client auth fix** - REST client now uses HMAC `api_token` (not CSRF UUID) for Bearer auth; fixes Script Console and Project Browser "unable to connect" errors
- **Gateway URL detection** - REST client reads saved gateway URL from IDE preferences; Script Console and Project Browser now share the IDE's configured gateway address
- **Script Console UI polish** - Thin 6px scrollbars, softer borders (`BORDER_SUBTLE`), increased padding for a cleaner web UI feel
- **Pool size control fix** - Backend returns proper HTTP 400/403/500 status codes on error; frontend checks response `success` field for reliable error reporting

**v3.5.3** - Designer Script Console Fix, Project Browser Auth & Folders
- **Script Console crash fix** - Fixed NullPointerException (`outputArea is null`) when opening Script Console; ThemeManager now null-checks optional components
- **Project Browser auth fix** - Designer REST client Bearer tokens now accepted by Gateway auth check (v3.5.2 broke Designer access)
- **Real folder creation** - "New Folder" in Project Browser creates a persistent folder with `__init__` script instead of showing an unhelpful message
- **Nested folder support** - Folder context menu now has "New Folder..." for creating sub-folders

**v3.5.2** - CSRF Fix, Gateway Authentication & Logs Rewrite
- **CSRF token fix** - Session endpoint now accepts Gateway Web UI clients and generates proper CSRF tokens for HTTP sessions
- **Gateway authentication** - All REST endpoints require Gateway login; unauthenticated access shows login prompt
- **Logs rewrite** - Reads from Ignition's system_logs.idb SQLite database instead of wrapper.log (works in Docker)
- **PyPI metadata route fix** - `/api/v1/packages/pypi-info/:name` now correctly uses path parameter
- **Pool health status** - Pool stats endpoint includes `healthCheckStatus` field
- **Diagnostics cleanup** - Replaced large health banner with inline status dot
- **Consistent API client** - All frontend fetch calls use `credentials: 'same-origin'` and auth detection

**v3.5.1** - Designer Script Console & Project Browser Fixes
- **REST client resilience** - Designer REST client no longer blocks on auth token failure; Project Browser and Script Console now connect reliably
- **Script Console redesign** - Merged Output/Errors into single combined panel with colored text (output white, errors red, timing green)
- **Script Console toolbar** - Matches Web GUI: Run (green) on left, Load Script/Save/Save As/Clear/Split on right
- **Save/Save As in Script Console** - Save auto-saves to loaded script; Save As always prompts for new name
- **Script name indicator** - Shows loaded script name bar below toolbar
- **Split orientation** - Both Designer Script Console and Web GUI IDE support vertical/horizontal split toggle (persisted)

**v3.5.0** - Gateway Web UI Improvements & New Features
- **Save/Save As split** - Save auto-saves loaded scripts; Save As always prompts for new name
- **PyPI search fix** - Uses PyPI JSON API for exact matches + resilient HTML fallback regex
- **Logs tab** - New sidebar tab showing gateway logs with level filtering and text search
- **CPU/RAM fix** - Accurate metrics using system load average and heap+non-heap memory
- **Pool size control** - Clickable pool size in Diagnostics allows resizing (1-20)
- **Diagnostics cleanup** - Removed Circuit Breaker and Active Alerts panels

**v3.4.0** - Designer Project Browser Integration
- **Python 3 Scripts** node added to Designer's Project Browser sidebar
- Browse, create, rename, delete scripts directly from the Project Browser tree
- Double-click a script to open it in the Script Console
- Right-click context menus on root, folder, and script nodes

**v3.2.3** - IDE Script Loading, Status Bar & Folder Improvements
- IDE now has "Load Script" dropdown to load and run saved scripts directly
- Status bar redesigned: CPU and RAM usage with mini progress bars (left), pool utilization (right)
- Removed connection status icon from status bar (replaced with system metrics)
- New scripts created inside the currently selected folder (folder-aware creation)
- Move-to-folder now shows dropdown of existing folders instead of free-text input
- Fixed terminal auto-create firing multiple times on version selector change
- Added 500ms delay for CSRF token initialization before first terminal session
- TypeScript cleanup: removed unused variables and interfaces

**v3.2.2** - Terminal & Script Rename Fixes
- Fixed terminal sending commands to bash instead of Python (now starts `python3 -u -i`)
- Terminal supports multi-version Python selection via version selector dropdown
- Merged stderr into stdout for Python tracebacks to display in terminal
- Python interactive prompts (`>>>`, `...`) filtered from terminal output
- Fixed script rename causing scripts to disappear (save now verified before delete)
- Fixed script detail panel load/save to handle nested response formats
- Script list refreshes on rename error to restore correct state

**v3.2.1** - Gateway Web UI Bug Fixes
- Fixed CSRF token handling for all POST/DELETE requests (terminal, IDE, scripts, packages)
- Fixed terminal not working (session creation failed silently without CSRF token)
- Added centralized API client (api.ts) with automatic CSRF token management
- Sidebar now shows module version (v3.2.1) instead of Python version
- Status bar pool utilization progress bar with color coding (green/yellow/red)
- Script folder creation, collapsible folder tree view, and folder organization
- Script rename/move action buttons (hover-reveal) replacing right-click context menus
- PyPI package lookup in install modal with live validation and package info display
- Version selector now shows all installed Python versions from distributions endpoint
- Terminal tab renaming (double-click) and activity indicators

**v3.2.0** - Gateway Web UI
- Browser-based Python 3 IDE accessible from Ignition Gateway home page
- React + TypeScript frontend with CodeMirror 6 editor and xterm.js terminal
- 7-tab sidebar: Dashboard, IDE, Terminal, Scripts, Python Versions, Packages, Diagnostics
- Multi-tab terminal sessions using interactive shell backend
- Script management (save, load, delete, import/export .py files)
- Python version install/uninstall UI from Gateway
- Package management with search, install, uninstall
- Real-time diagnostics with pool stats, metrics, circuit breaker status
- Standalone dedicated page at `/res/python3integration/standalone.html`
- Dark theme matching Ignition's modern aesthetic
- All CSS scoped to prevent Gateway UI pollution

**v3.1.0** - Multi-Version Python Management
- Install/uninstall Python versions (3.9-3.13) directly from the Designer IDE
- New Version Manager dialog with install status, size, and pool management
- PoolManager for running multiple Python version pools concurrently
- REST API endpoints for distribution management (`/distributions`, `/distributions/install`, `/distributions/uninstall`)
- Version selector in IDE toolbar for per-execution version choice
- Repository renamed from `ignition-module-python3-java` to `ignition-module-python3`

**v3.0.0** - Major Version Release
- Milestone release marking production maturity
- Complete feature set with comprehensive documentation
- Stable, fully-tested codebase ready for enterprise deployment
- All critical bugs resolved and security hardening complete

**v2.15.10** - Critical Bug Fixes
- Fixed pip3 command errors on systems without pip3 in PATH
- Fixed drag-and-drop bug (scripts replacing folders instead of moving into them)
- Fixed script signature verification errors (made enforcement optional)

**v2.15.9** - Production Readiness Fixes
- Fixed critical bug in python_bridge.py (dead execute_shell handler)
- Updated vulnerable dependencies (commons-compress, SLF4J, Mockito)
- Fixed memory leaks (CSRF tokens, rate limiters, thread pool cleanup)

**v2.12.0** - Virtual Environment Support
- Full venv support with automatic detection and UI status display

**For complete changelog, see [CHANGELOG.md](../CHANGELOG.md)**

---

## 🗺️ Roadmap

See [CONSOLIDATED_ROADMAP.md](docs/roadmap/CONSOLIDATED_ROADMAP.md) for detailed feature roadmap.

### Coming Soon
- Enhanced autocomplete and IntelliSense
- Debugging support with breakpoints
- Performance profiling tools
- Code refactoring utilities
- Multi-cursor editing

---

**⭐ If you find this module useful, please star the repository!**

**📣 Questions? Open an [issue](https://github.com/Gaskony-Ignition/ignition-module-python3/issues) or [discussion](https://github.com/Gaskony-Ignition/ignition-module-python3/discussions)!**
