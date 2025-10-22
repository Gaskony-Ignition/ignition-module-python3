# Python 3 Integration Module - Documentation

**Current Version**: v2.0.22
**Last Updated**: 2025-10-17

Documentation for the Python 3 Integration module for Ignition 8.3+.

---

## 📋 Essential Documentation

### V2 Architecture (Current - v2.0.0+)
- **[V2_ARCHITECTURE_GUIDE.md](V2_ARCHITECTURE_GUIDE.md)** - Complete v2.0.0+ modular architecture overview
- **[V2_STATUS_SUMMARY.md](V2_STATUS_SUMMARY.md)** - Current implementation status and completion metrics
- **[V2_FEATURE_COMPARISON_AND_ROADMAP.md](V2_FEATURE_COMPARISON_AND_ROADMAP.md)** - v1 vs v2 comparison with detailed roadmap
- **[V2_MIGRATION_GUIDE.md](V2_MIGRATION_GUIDE.md)** - Migrating from v1.x to v2.0+ (for developers)

### Development Workflow
- **[VERSION_UPDATE_WORKFLOW.md](VERSION_UPDATE_WORKFLOW.md)** - **MANDATORY** version release checklist
- **[TESTING_GUIDE.md](TESTING_GUIDE.md)** - How to test the module (manual and automated)

---

## 🏗️ v2.0 Architecture Overview

**Refactored in v2.0.0** from 2,676-line monolith to modular design:

### Module Structure
```
designer/src/main/java/.../designer/
├── Python3IDE.java             # Main IDE class (490 lines, was 2,676)
├── managers/                   # Business logic layer
│   ├── GatewayConnectionManager.java
│   ├── ScriptManager.java
│   └── ThemeManager.java
└── ui/                         # Presentation layer
    ├── EditorPanel.java
    ├── ScriptTreePanel.java
    ├── MetadataPanel.java
    └── DiagnosticsPanel.java
```

**Benefits:**
- 82% reduction in main class size (2,676 → 490 lines)
- Clear separation of concerns (Managers + UI Panels + Orchestration)
- Each file 95-490 lines (highly maintainable)
- Easy to test, extend, and debug

---

## 📊 Current Module Status (v2.0.22)

### ✅ Implemented Features
- Modern Designer IDE with VS Code-inspired dark theme
- Script management (save, load, delete, rename, organize in folders)
- Find/Replace toolbar
- Import/Export scripts to .py files
- Enhanced diagnostics panel with real-time metrics
- Theme support (Dark, Light, VS Code Dark+)
- REST API for remote execution
- Theme-aware split pane dividers (v2.0.22)

### 🎯 Next Priorities
See [V2_FEATURE_COMPARISON_AND_ROADMAP.md](V2_FEATURE_COMPARISON_AND_ROADMAP.md) for the complete roadmap.

**Immediate Next Steps:**
- Clear Output button
- New Script button
- Keyboard shortcuts (Ctrl+Enter, Ctrl+S)
- Context menu (right-click operations)

---

## 🔗 REST API Endpoints (v2.0.22)

```
POST /data/python3integration/api/v1/exec              - Execute code
POST /data/python3integration/api/v1/eval              - Evaluate expression
POST /data/python3integration/api/v1/call-module       - Call Python function
GET  /data/python3integration/api/v1/version           - Python version
GET  /data/python3integration/api/v1/pool-stats        - Pool statistics
GET  /data/python3integration/api/v1/health            - Health check
GET  /data/python3integration/api/v1/diagnostics       - Performance metrics
GET  /data/python3integration/api/v1/scripts           - List scripts
POST /data/python3integration/api/v1/scripts/save      - Save script
GET  /data/python3integration/api/v1/scripts/{name}    - Load script
DELETE /data/python3integration/api/v1/scripts/{name}  - Delete script
```

---

## 📚 External Resources

- **Ignition SDK Documentation**: https://www.sdk-docs.inductiveautomation.com/
- **SDK Examples**: https://github.com/inductiveautomation/ignition-sdk-examples
- **RSyntaxTextArea API**: https://bobbylight.github.io/RSyntaxTextArea/
- **Ignition Forum**: https://forum.inductiveautomation.com/

---

## 📞 Quick Reference

**For Users:**
- Open IDE: Tools → Python 3 IDE
- Execute code: Ctrl+Enter (coming soon)
- Save script: Save button (Ctrl+S coming soon)
- Find text: Find toolbar

**For Developers:**
- **Build**: `./gradlew clean build --no-daemon`
- **Version**: `python3-integration/version.properties`
- **Main IDE**: `designer/src/main/java/.../Python3IDE.java`
- **Gateway API**: `gateway/src/main/java/.../Python3RestEndpoints.java`
- **Version workflow**: [VERSION_UPDATE_WORKFLOW.md](VERSION_UPDATE_WORKFLOW.md)

---

**Last Updated**: 2025-10-17 | **Version**: v2.0.22
