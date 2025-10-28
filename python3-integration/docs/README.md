# Python 3 Integration Module - Documentation

**Current Version**: v2.11.0
**Last Updated**: 2025-10-28

Complete documentation for the Python 3 Integration module for Ignition 8.3+.

---

## 📋 Documentation Index

### 🚀 Getting Started
Perfect for new users and developers.

- **[Quick Start Guide](getting-started/QUICK_START.md)** ⭐
  - 5-minute quick start
  - Prerequisites and installation
  - First Python 3 script
  - Basic troubleshooting

### 🏗️ Architecture
System design and component details.

- **[Architecture Overview](architecture/OVERVIEW.md)** - Complete v2.0.0+ modular architecture
- **[Current Status](architecture/STATUS.md)** - Implementation status and completion metrics

### 💻 Development
For contributors and module developers.

- **[Testing Guide](development/TESTING.md)** - Manual and automated testing
- **[Version Workflow](development/VERSION_WORKFLOW.md)** ⚠️ **MANDATORY** - Version release checklist
- **[E2E Test Suite](development/E2E_TESTS.md)** - End-to-end testing documentation

### 🔧 Operations
For administrators and operators.

- **[Deployment Guide](operations/DEPLOYMENT.md)** - Production deployment checklist
- **[Monitoring Guide](operations/MONITORING.md)** - Monitoring and metrics setup
- **[Backup & Restore](operations/BACKUP.md)** - Backup and disaster recovery
- **[Performance Guide](operations/PERFORMANCE.md)** - Performance benchmarks and optimization
- **[Troubleshooting](operations/TROUBLESHOOTING.md)** ⭐ - Common issues and solutions

### 📡 API
API documentation and examples.

- **[REST API](api/REST_API.md)** - Complete REST API reference
- **[Designer IDE](api/DESIGNER_IDE.md)** - Python 3 IDE user guide

### 🔒 Security
Security configuration and best practices.

- **[Security Overview](security/SECURITY_OVERVIEW.md)** - Security model and architecture
- **[Security Configuration](security/SECURITY_CONFIG.md)** - Detailed configuration guide
- **[Audit Checklist](security/AUDIT_CHECKLIST.md)** - Security audit procedures

### 📦 Archive
Historical documentation preserved for reference.

- **[Refactoring Logs](archive/refactoring/)** - v2.9.0-v2.11.0 refactoring details
- **[Session Notes](archive/sessions/)** - Historical development session notes
- **[Migration Guide](archive/historical/v1-to-v2-migration.md)** - v1.x to v2.0+ migration

---

## 🎯 Quick Navigation

### For New Developers
1. Start here: [Quick Start Guide](getting-started/QUICK_START.md)
2. Understand the system: [Architecture Overview](architecture/OVERVIEW.md)
3. Learn to test: [Testing Guide](development/TESTING.md)

### For Users
1. Installation: [Quick Start Guide](getting-started/QUICK_START.md)
2. Using the IDE: [Designer IDE Guide](api/DESIGNER_IDE.md)
3. Problems?: [Troubleshooting Guide](operations/TROUBLESHOOTING.md)

### For Administrators
1. Deploy: [Deployment Guide](operations/DEPLOYMENT.md)
2. Monitor: [Monitoring Guide](operations/MONITORING.md)
3. Secure: [Security Configuration](security/SECURITY_CONFIG.md)

### For Contributors
1. **MUST READ**: [Version Workflow](development/VERSION_WORKFLOW.md)
2. Testing: [Testing Guide](development/TESTING.md)
3. Architecture: [Architecture Overview](architecture/OVERVIEW.md)

---

## 📊 Current Module Status (v2.11.0)

### ✅ Implemented Features
- Modern Designer IDE with VS Code-inspired dark theme
- Script management (save, load, delete, rename, organize in folders)
- Command Palette (Ctrl+Shift+P) for keyboard-driven workflow
- Recent Scripts quick access (last 10 scripts)
- Collapsible sidebar (Ctrl+B) for more code space
- Find/Replace toolbar
- Import/Export scripts to .py files
- Enhanced diagnostics panel with real-time metrics
- Theme support (Dark, Light, VS Code Dark+)
- Smart auto-save (30-second interval)
- Inline error markers (real-time syntax validation)
- REST API for remote execution
- 7 focused manager classes (1,762 lines of organized code)

### 🎯 Architecture Highlights
- **Python3IDE.java**: Reduced from 4,390 → 3,727 lines (15.1% reduction)
- **Manager Pattern**: 7 specialized managers with dependency injection
- **Clean Separation**: Business logic separated from UI
- **Testability**: Each component independently testable

---

## 🔗 REST API Endpoints (v2.11.0)

**Base URL**: `http://localhost:8088/data/python3integration/api/v1/`

### Execution Endpoints
```
POST /exec              - Execute Python statements
POST /eval              - Evaluate Python expressions
POST /call-module       - Call Python module functions
```

### Information Endpoints
```
GET /version           - Python version information
GET /pool-stats        - Process pool statistics
GET /health            - Health check
GET /diagnostics       - Performance metrics
```

### Script Management Endpoints
```
GET    /scripts        - List all scripts
POST   /scripts/save   - Save script
GET    /scripts/{name} - Load script
DELETE /scripts/{name} - Delete script
```

**Full documentation**: [REST API Guide](api/REST_API.md)

---

## 📚 External Resources

- **Ignition SDK Documentation**: https://www.sdk-docs.inductiveautomation.com/
- **SDK Examples**: https://github.com/inductiveautomation/ignition-sdk-examples
- **RSyntaxTextArea API**: https://bobbylight.github.io/RSyntaxTextArea/
- **Ignition Forum**: https://forum.inductiveautomation.com/

---

## 📞 Quick Reference

### For Users
- **Open IDE**: Tools → Python 3 IDE
- **Execute code**: Ctrl+Enter
- **Save script**: Ctrl+S
- **Find text**: Ctrl+F
- **Command palette**: Ctrl+Shift+P
- **Toggle sidebar**: Ctrl+B

### For Developers
- **Build**: `./gradlew clean build --no-daemon`
- **Test**: `./gradlew test`
- **Version file**: `python3-integration/version.properties`
- **Main IDE**: `designer/src/main/java/.../Python3IDE.java`
- **Gateway API**: `gateway/src/main/java/.../Python3RestEndpoints.java`
- **Version workflow**: [development/VERSION_WORKFLOW.md](development/VERSION_WORKFLOW.md)

### For Administrators
- **Install module**: Gateway → Config → System → Modules
- **Check logs**: `<ignition-install>/logs/wrapper.log | grep Python3`
- **Configure path**: `ignition.conf` → `wrapper.java.additional.101=-Dignition.python3.path=/path/to/python3`
- **Pool size**: `ignition.conf` → `wrapper.java.additional.102=-Dignition.python3.poolsize=5`

---

## 📝 Changelog

See [CHANGELOG.md](../../CHANGELOG.md) in repository root for complete version history.

**Latest Release**: v2.11.0 (2025-10-28)
- 7 manager classes extracted from Python3IDE.java
- Code reduction: 4,390 → 3,727 lines (15.1%)
- Improved architecture with dependency injection
- Disabled GitHub Actions (local testing workflow)

---

## 🎓 Learning Path

### Beginner Path (30 minutes)
1. [Quick Start Guide](getting-started/QUICK_START.md) - 20 min
2. [Designer IDE Guide](api/DESIGNER_IDE.md) - 10 min

### Intermediate Path (2 hours)
1. [Quick Start Guide](getting-started/QUICK_START.md) - 20 min
2. [Architecture Overview](architecture/OVERVIEW.md) - 40 min
3. [REST API Guide](api/REST_API.md) - 30 min
4. [Testing Guide](development/TESTING.md) - 30 min

### Advanced Path (4 hours)
1. Quick Start Guide - 20 min
2. Architecture Overview - 40 min
3. REST API Guide - 30 min
4. Testing Guide - 30 min
5. [Deployment Guide](operations/DEPLOYMENT.md) - 40 min
6. [Security Configuration](security/SECURITY_CONFIG.md) - 30 min
7. [Version Workflow](development/VERSION_WORKFLOW.md) - 30 min
8. [Monitoring Guide](operations/MONITORING.md) - 20 min

---

## 🆘 Getting Help

### Documentation Issues
1. Check [Troubleshooting Guide](operations/TROUBLESHOOTING.md)
2. Search documentation (Ctrl+F in browser)
3. Check [Quick Start Guide](getting-started/QUICK_START.md)

### Technical Issues
1. Check Gateway logs: `<ignition-install>/logs/wrapper.log`
2. Review [Troubleshooting Guide](operations/TROUBLESHOOTING.md)
3. Test REST API health: `curl http://localhost:8088/data/python3integration/api/v1/health`

### Community Support
- **Ignition Forum**: https://forum.inductiveautomation.com/
- **GitHub Issues**: https://github.com/nigelgwork/ignition-module-python3-java/issues
- **SDK Documentation**: https://www.sdk-docs.inductiveautomation.com/

---

## 📈 Documentation Statistics

- **Total Guides**: 20+ comprehensive guides
- **Total Lines**: 8,000+ lines of documentation
- **Coverage**: Installation, Architecture, API, Operations, Security
- **Last Major Update**: v2.11.0 (2025-10-28)
- **Documentation Structure**: Organized by audience (Getting Started, Development, Operations, Security)

---

**Documentation Version**: 2.0
**Module Version**: v2.11.0
**Last Updated**: 2025-10-28
**Maintained By**: Development Team
