# Python 3 Integration Module for Ignition

**Current Version: v3.5.2** | [Changelog](../CHANGELOG.md) | [GitHub](https://github.com/Gaskony-Ignition/ignition-module-python3)

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

**Latest Release:** v3.5.2 (February 2026)

### Recent Changes

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
