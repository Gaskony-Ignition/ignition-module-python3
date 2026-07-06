# Python 3 Integration for Ignition

<!-- Version and Status -->
![Version](https://img.shields.io/badge/version-4.5.2-blue.svg)
![Status](https://img.shields.io/badge/status-stable-brightgreen.svg)
![Tests](https://img.shields.io/badge/tests-649%20passing-brightgreen.svg)
![Coverage](https://img.shields.io/badge/coverage-51.7%25-yellow.svg)

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

**Repository:** `ignition-module-python3` | **Module ID:** `com.gaskony.python3`

---

## Why this exists

Ignition is built on Jython 2.7 and realistically always will be — but nearly
every programmer today works in Python 3, and the packages that matter
(`requests`, `pandas`, `numpy`, …) are Python 3 only. **This module makes
Python 3 feel like a native part of any Ignition 8.3 gateway**: write and test
a Python 3 script in a first-class Designer editor, save it into a
Project-Library-like tree, and call it from anywhere Jython runs — Perspective
pages, tag scripts, gateway events — via `system.python3.*`.

The full purpose, definition of done, and permanent won't-do list live in
[docs/PROJECT_CHARTER.md](docs/PROJECT_CHARTER.md) — the charter drives every
release decision.

---

## 🚀 Quick Start

```bash
./gradlew clean build --no-daemon
# Install build/Python3-*.modl in Ignition Gateway
```

**Key Features:**

- 🌐 **Gateway Web UI** - Browser-based IDE, PyPI search, and management
- 🖥️ **Designer Script Console** - Lightweight script execution from Tools menu
- 🗂️ **NEW: Project Browser Integration** - Python 3 Scripts node in Designer sidebar (v3.5.1)
- 🎨 Modern Designer IDE with dark theme
- 🏗️ Modular Architecture (v2.0.0+)
- 📊 Enhanced Diagnostics with real-time metrics
- ✨ Script Management - Save, load, organise in folders
- ⌨️ Keyboard Shortcuts - Ctrl+Enter, Ctrl+S, Ctrl+N, Ctrl+F
- 🖱️ Context Menus - Right-click scripts (Load, Export, Rename, Delete, Move)
- 🎯 Power User Features - Font controls, move to folder, drag-and-drop
- 🔄 REST API for remote execution and script autocomplete
- 🔒 Production Security - Script signing, CSRF protection

👉 **See [CHANGELOG.md](CHANGELOG.md)** for the full release history and feature list.

---

## 📖 Documentation

- **Architecture**: [docs/architecture/OVERVIEW.md](docs/architecture/OVERVIEW.md)
- **Development Guide**: [CLAUDE.md](CLAUDE.md) - For contributors
- **Changelog**: [CHANGELOG.md](CHANGELOG.md) - Release history

---

## 🔧 For Developers

### Build & Test

```bash
# Build module
./gradlew clean build --no-daemon

# Test with Docker
docker-compose up -d
# Access at http://localhost:9088
```

### Key Resources

- **Version Workflow**: [docs/development/VERSION_WORKFLOW.md](docs/development/VERSION_WORKFLOW.md)
- **Testing Guide**: [docs/development/TESTING.md](docs/development/TESTING.md)
- **Roadmap**: [docs/roadmap/README.md](docs/roadmap/README.md)

---

## 📚 External Resources

- **Official SDK Docs**: <https://www.sdk-docs.inductiveautomation.com/>
- **SDK Examples**: <https://github.com/inductiveautomation/ignition-sdk-examples>
- **Forum**: <https://forum.inductiveautomation.com/c/module-development/7>
- **Gradle Plugin**: <https://github.com/inductiveautomation/ignition-module-tools>

---

## 📜 Credits

**Python 3 Integration Module** developed by Gaskony with assistance from Claude Code (Anthropic).

Built using the Ignition 8.3 SDK from Inductive Automation.

---

## 📄 License

See individual module source files for licensing information.
