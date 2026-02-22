# Python 3 Integration for Ignition

<!-- Version and Status -->
![Version](https://img.shields.io/badge/version-3.7.1-blue.svg)
![Status](https://img.shields.io/badge/status-stable-brightgreen.svg)
![Tests](https://img.shields.io/badge/tests-184%20passing-brightgreen.svg)
![Coverage](https://img.shields.io/badge/coverage-19%25-orange.svg)

<!-- Platform Requirements -->
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://adoptium.net/)
[![Python](https://img.shields.io/badge/Python-3.9%20%7C%203.11%20%7C%203.12-blue.svg)](https://www.python.org/)
[![Ignition](https://img.shields.io/badge/Ignition-8.3+-red.svg)](https://inductiveautomation.com/)
[![Gradle](https://img.shields.io/badge/Gradle-8.10.2-green.svg)](https://gradle.org/)

<!-- Quality Metrics -->
![Lines of Code](https://img.shields.io/badge/lines%20of%20code-15K+-blue.svg)
![Manager Classes](https://img.shields.io/badge/managers-7%20classes-blue.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

<!-- GitHub Actions Status (Disabled but badges retained for future use) -->
<!-- [![Tests](https://github.com/Gaskony-Ignition/ignition-module-python3/actions/workflows/test.yml/badge.svg)](https://github.com/Gaskony-Ignition/ignition-module-python3/actions/workflows/test.yml) -->
<!-- [![Build](https://github.com/Gaskony-Ignition/ignition-module-python3/actions/workflows/build.yml/badge.svg)](https://github.com/Gaskony-Ignition/ignition-module-python3/actions/workflows/build.yml) -->

**🎯 Repository:** `ignition-module-python3` | **Version:** v3.7.1 | **Module ID:** `com.gaskony.python3.swing`

[Full Documentation →](python3-integration/README.md)

---

## 🔄 Repository Split Notice

**Separated Oct 22, 2025**

- **This repository:** Python 3 Integration Module (v3.7.1) - Production-ready
- **Web UI repository:** Modern JCEF + React IDE (v3.3.4) - In development
- **Purpose:** Maintain stable version while developing modern web UI separately

---

A production-ready Ignition module that enables Python 3 scripting alongside Jython 2.7, with a modern Designer IDE.

---

## 🚀 Quick Start

```bash
cd python3-integration
./gradlew clean build --no-daemon
# Install build/libs/python3-integration-signed.modl in Ignition Gateway
```

**Key Features:**
- 🌐 **Gateway Web UI** - Browser-based IDE, PyPI search, and management
- 🖥️ **Designer Script Console** - Lightweight script execution from Tools menu
- 🗂️ **NEW: Project Browser Integration** - Python 3 Scripts node in Designer sidebar (v3.5.1)
- 🎨 Modern Designer IDE with dark theme
- 🏗️ Modular Architecture (v2.0.0+)
- 📊 Enhanced Diagnostics with real-time metrics
- ✨ Script Management - Save, load, organize in folders
- ⌨️ Keyboard Shortcuts - Ctrl+Enter, Ctrl+S, Ctrl+N, Ctrl+F
- 🖱️ Context Menus - Right-click scripts (Load, Export, Rename, Delete, Move)
- 🎯 Power User Features - Font controls, move to folder, drag-and-drop
- 🔄 REST API for remote execution and script autocomplete
- 🔒 Production Security - Script signing, CSRF protection

👉 **[See Full Documentation](python3-integration/README.md)** for features, API reference, and examples.

---

## 📖 Documentation

- **Module Documentation**: [python3-integration/README.md](python3-integration/README.md) - Complete user guide
- **Architecture**: [python3-integration/docs/architecture/OVERVIEW.md](python3-integration/docs/architecture/OVERVIEW.md)
- **Development Guide**: [CLAUDE.md](CLAUDE.md) - For contributors

---

## 🔧 For Developers

### Build & Test
```bash
# Build module
cd python3-integration && ./gradlew clean build --no-daemon

# Test with Docker
docker-compose up -d
# Access at http://localhost:9088
```

### Key Resources
- **Version Workflow**: [python3-integration/docs/development/VERSION_WORKFLOW.md](python3-integration/docs/development/VERSION_WORKFLOW.md)
- **Testing Guide**: [python3-integration/docs/development/TESTING.md](python3-integration/docs/development/TESTING.md)
- **Roadmap**: [python3-integration/docs/roadmap/README.md](python3-integration/docs/roadmap/README.md)

---

## 📚 External Resources

- **Official SDK Docs**: https://www.sdk-docs.inductiveautomation.com/
- **SDK Examples**: https://github.com/inductiveautomation/ignition-sdk-examples
- **Forum**: https://forum.inductiveautomation.com/c/module-development/7
- **Gradle Plugin**: https://github.com/inductiveautomation/ignition-module-tools

---

## 📜 Credits

**Python 3 Integration Module** developed by Gaskony with assistance from Claude Code (Anthropic).

Built using the Ignition 8.3 SDK from Inductive Automation.

---

## 📄 License

See individual module source files for licensing information.
