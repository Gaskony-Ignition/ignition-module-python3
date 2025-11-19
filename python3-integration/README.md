# Python 3 Integration Module for Ignition

**Current Version: v2.15.8** | [Changelog](../CHANGELOG.md) | [GitHub](https://github.com/nigelgwork/ignition-module-python3)

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

1. **Download** the latest `.modl` file from [Releases](https://github.com/nigelgwork/ignition-module-python3/releases)
2. Open Ignition Gateway web interface (http://localhost:8088)
3. Navigate to **Config → System → Modules**
4. Click **Install or Upgrade a Module**
5. Select the `.modl` file
6. Click **Install**
7. Module status should show **Running**

**Need detailed installation instructions?** See [Installation Guide](docs/getting-started/INSTALLATION.md)

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
- **[Installation Guide](docs/getting-started/INSTALLATION.md)** - Detailed installation and upgrade instructions
- **[Quick Start](docs/getting-started/QUICK_START.md)** - Get up and running in 30 minutes
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
git clone https://github.com/nigelgwork/ignition-module-python3-java.git
cd ignition-module-python3-java/python3-integration

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
- **🐛 [Report Issues](https://github.com/nigelgwork/ignition-module-python3/issues)** - Bug reports and feature requests
- **💬 [Discussions](https://github.com/nigelgwork/ignition-module-python3/discussions)** - Questions and community support
- **📧 Email**: support@example.com

---

## 🤝 Contributing

We welcome contributions! See [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

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

**Latest Release:** v2.15.8 (November 2025)

### Recent Changes

**v2.15.8** - Feature Removal (per user request)
- Completely removed Recent folder feature from script tree
- Simplified codebase by removing RecentScriptsManager
- Tree now shows only actual folder structure without virtual folders
- Removed virtual folder exclusion logic in getFolderPathForNode() and showContextMenu()
- Simplified convertToMetadata() by removing Recent folder path cleanup

**v2.15.4** - Critical Bug Fixes
- Fixed Recent folder persistence issue (cleans up virtual folder paths on script load)
- Fixed script name display not visible in IDE (now shows prominently in title bar)
- Script display format: "Python 3 Code Editor | • ScriptName *"

**v2.15.3** - Bug Fixes
- Fixed Recent folder phantom creation issue (scripts no longer create "📌 Recent" folder)
- Updated Info dialog with correct usage documentation
- Prevented context menu actions on virtual folders

**v2.15.2** - UX Enhancement
- Reorganized Packages dialog layout (side-by-side columns)
- Improved visibility and table optimization

**v2.15.1** - Bugfix
- Fixed installed packages table rendering
- Removed experimental warning banner

**v2.15.0** - Packages Dialog UX
- Scrollable PyPI search results
- Functional installed packages table with uninstall

**v2.12.0** - Virtual Environment Support
- Full venv support with automatic detection
- VIRTUAL_ENV propagation
- UI status display

**v2.11.0** - Code Architecture Refinement
- Extracted 7 manager classes (1,762 lines)
- Reduced Python3IDE.java by 15.1%
- Improved testability and maintainability

**Full changelog:** [CHANGELOG.md](../CHANGELOG.md)

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

**📣 Questions? Open an [issue](https://github.com/nigelgwork/ignition-module-python3/issues) or [discussion](https://github.com/nigelgwork/ignition-module-python3/discussions)!**
