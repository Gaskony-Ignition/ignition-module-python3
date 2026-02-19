# CLAUDE.md - Python 3 Integration Module

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 🎯 Repository Identity

**Repository:** `ignition-module-python3` - Production-ready v3.3.0
- **Module Name:** Python 3 Integration
- **Module ID:** com.gaskony.python3.swing
- **IDE Implementation:** Java Swing with RSyntaxTextArea
- **Status:** Stable, fully functional, production-ready
- **Last Release:** v3.3.0 (Feb 2026)
- **GitHub:** https://github.com/Gaskony-Ignition/ignition-module-python3

**Separated from Web UI repository on:** Oct 22, 2025
- For Web UI (JCEF + React) development, see: `/modules/ignition-module-python3-web/`

## ⚠️ CRITICAL: File Cleanup Rules

**ALWAYS DELETE Zone.Identifier files immediately:**
- **NEVER** ignore or skip Zone.Identifier files
- **ALWAYS** delete them using: `find . -name "*Zone.Identifier*" -type f -delete`
- Run this check at the start of ANY code cleanup, documentation update, or file organization task
- These are Windows WSL metadata files that should never be committed

**Remember:** DELETE, not ignore!

## ⚠️ CRITICAL: Version Management and Build Process

**ALWAYS follow this complete workflow for EVERY build:**

### 1. Pre-Build Cleanup
- Delete Zone.Identifier files: `find . -name "*Zone.Identifier*" -type f -delete`
- Review and tidy up code (remove commented code, fix formatting)
- Update documentation if needed (README, TESTING_GUIDE, etc.)

### 2. Version Increment
Version file: `python3-integration/version.properties`

**Current Version: v3.3.0** (February 2026)

**NOTE:** This is the Java Swing IDE repository. The Web UI (JCEF) version is in a separate repository.

**Versioning Rules:**
- **MAJOR** (x.0.0): Breaking changes, major new features, architectural changes
- **MINOR** (1.x.0): New features, significant fixes, scope changes, API additions
- **PATCH** (1.0.x): Bug fixes, documentation updates, minor tweaks

**Examples:**
- Added new feature (folders, find/replace) → **MINOR** (2.0.9 → 2.1.0)
- Fixed UI bugs (scrollbars, themes) → **PATCH** (2.0.8 → 2.0.9)
- Rewrote entire architecture (v1 → v2 refactor) → **MAJOR** (1.17.2 → 2.0.0)

**Version Locations to Update:**
When incrementing version, update ALL of these files:
- [ ] `python3-integration/version.properties` - Primary version source (REQUIRED)
- [ ] `python3-integration/designer/src/main/java/.../DesignerHook.java` - Fallback version (line 183) **CRITICAL: Update EVERY release**
- [ ] `README.md` (repository root) - Main README version references
- [ ] `python3-integration/README.md` - Module README version references + Changelog entry
- [ ] `CLAUDE.md` - Current version reference (line 27)
- [ ] Update Changelog sections in both README files

**CRITICAL: DesignerHook.java Fallback Version (line 183)**
This version appears in the IDE window title bar. It MUST be updated with EVERY release or the header will show the wrong version.
```java
return "X.Y.Z";  // ALWAYS UPDATE THIS WITH NEW RELEASES
```

**Release Checklist:**
- [ ] All tests passing (`./gradlew clean build`)
- [ ] Zone.Identifier files deleted
- [ ] Code cleanup complete (no commented code, proper formatting)
- [ ] Documentation updated (README, version references)
- [ ] Version bumped in all locations above
- [ ] Git commit with proper format
- [ ] Git push to GitHub
- [ ] Build artifacts verified (*.modl file in build/libs/)

**Recent Releases:**
- v3.3.0 (Feb 2026) - Gateway Web UI improvements, PyPI search, Designer Script Console
- v3.2.3 (Feb 2026) - IDE script loading, status bar CPU/RAM, folder-aware scripts, terminal fix
- v3.2.2 (Feb 2026) - Terminal Python shell fix, script rename safety fix
- v3.2.1 (Feb 2026) - Gateway Web UI bug fixes (CSRF, terminal, folders, PyPI search, status bar)
- v3.2.0 (Feb 2026) - Gateway Web UI with React-based IDE, terminal, script/package/version management
- v3.1.0 (Feb 2026) - Multi-version Python management with install/uninstall UI
- v3.0.0 (Nov 2025) - Major version release marking production maturity
- v2.15.10 (Nov 2025) - Critical bug fixes (pip3, drag-drop, signatures)
- v2.15.9 (Nov 2025) - Production security & memory leak fixes
- v2.15.8 (Nov 2025) - Removed Recent Scripts folder feature
- v2.12.0 (Oct 2025) - Virtual environment support

**For complete version history, see [CHANGELOG.md](CHANGELOG.md)**

### 3. Build Module
```bash
cd /modules/ignition-module-python3/python3-integration
./gradlew clean build --no-daemon
```

### 4. Git Commit and Push
**ALWAYS commit and push after successful build:**
```bash
git add -A
git commit -m "Release vX.Y.Z - [description]

Version: X.Y.Z-1 → X.Y.Z (MAJOR/MINOR/PATCH)

Changes:
- [List key changes]
- [...]

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
git push
```

### Complete Build Workflow Summary
1. **Clean**: Delete Zone.Identifier files
2. **Tidy**: Code cleanup and documentation updates
3. **Version**: Increment version.properties
4. **Update READMEs**: Update BOTH README.md files with new version and changelog
   - `/README.md` (repository root) - Update version references and latest release section
   - `/python3-integration/README.md` (module) - Update version references and add changelog entry
5. **Build**: Run ./gradlew clean build
6. **Commit**: Add all changes (code + READMEs + version.properties)
7. **Push**: Push to GitHub

**Remember:** NEVER build without incrementing version, updating BOTH READMEs, and pushing to git!

## ⚠️ CRITICAL: Pre-Push Requirements (User Request)

**BEFORE EVERY PUSH TO GITHUB, YOU MUST:**

1. **Update ALL Documentation** (ESPECIALLY README FILES):
   - `/README.md` (repository root) - Update version (lines 3, 11, 76, 135) and latest release section
   - `/python3-integration/README.md` (module) - Update version (lines 3, 136, 471, 514) and add changelog entry
   - ARCHITECTURE.md - update if architecture changed
   - CHANGELOG.md - add release notes if exists
   - Any other .md files that reference version numbers or features
   - Version references in comments and docstrings

2. **Clean Working Folder**:
   - Delete Zone.Identifier files: `find . -name "*Zone.Identifier*" -type f -delete`
   - Remove commented-out code
   - Remove temporary files
   - Fix formatting issues
   - Remove debug statements

3. **Verify Git Status**:
   - Run `git status` to check for untracked or modified files
   - Ensure no unexpected files are being committed
   - Review changes with `git diff`

**This is a MANDATORY workflow requirement requested by the user. Do NOT skip these steps.**

## Repository Purpose

This is a **Python 3 Integration module** for Ignition 8.3 SDK. The repository focuses exclusively on the working Python 3 Integration module implementation.

## Repository Structure

**Current Version: v3.3.0** (February 2026)

```
ignition-module-python3/
├── README.md                        # Repository landing page (update with each release)
├── CLAUDE.md                        # This file - AI guidance
├── .gitignore                       # Git ignore rules
│
└── python3-integration/             # ⭐ THE MODULE (v2.11.2)
    ├── build.gradle.kts            # Root build configuration
    ├── settings.gradle.kts         # Gradle settings
    ├── version.properties          # Current version: 2.11.0
    ├── README.md                   # Module documentation (comprehensive)
    │
    ├── common/                     # Common scope (shared code)
    ├── gateway/                    # Gateway scope (Python bridge, REST API)
    │   ├── build.gradle.kts
    │   └── src/main/java/.../gateway/
    │       ├── GatewayHook.java
    │       ├── Python3ProcessPool.java
    │       ├── Python3Executor.java
    │       ├── Python3ScriptModule.java
    │       ├── Python3RestEndpoints.java
    │       └── resources/python_bridge.py
    │
    ├── designer/                   # Designer scope (Python 3 IDE - v2.0.0+)
    │   ├── build.gradle.kts
    │   └── src/main/java/.../designer/
    │       ├── DesignerHook.java
    │       ├── Python3IDE.java            # Main IDE class (refactored v2.0.0)
    │       ├── managers/                  # Business logic layer
    │       │   ├── GatewayConnectionManager.java
    │       │   ├── ScriptManager.java
    │       │   └── ThemeManager.java
    │       └── ui/                        # Presentation layer
    │           ├── EditorPanel.java
    │           ├── ScriptTreePanel.java
    │           ├── MetadataPanel.java
    │           └── DiagnosticsPanel.java
    │
    └── docs/                        # Module-specific v2.0 documentation
        ├── V2_ARCHITECTURE_GUIDE.md
        ├── V2_STATUS_SUMMARY.md
        ├── V2_FEATURE_COMPARISON_AND_ROADMAP.md
        ├── V2_MIGRATION_GUIDE.md
        ├── TESTING_GUIDE.md
        └── VERSION_UPDATE_WORKFLOW.md
```

**Note:** Repository cleaned up Dec 2024 to focus exclusively on the Python 3 Integration module. General SDK documentation and examples removed (available from official sources - see External SDK Resources section below).

## Working with the Active Module

The `python3-integration/` directory contains a complete, working module implementation. Key aspects:

### Architecture Overview

The module uses a **subprocess process pool** approach to bridge Ignition's Jython 2.7 with Python 3:

**Gateway Scope:**
1. **GatewayHook** - Module lifecycle, initializes process pool during startup()
2. **Python3ProcessPool** - Manages 3-20 warm Python processes, thread-safe borrowing/returning
3. **Python3Executor** - Wraps single Python subprocess, handles JSON communication via stdin/stdout
4. **Python3ScriptModule** - Exposes scripting functions like `system.python3.exec()`, `system.python3.eval()`
5. **Python3RestEndpoints** - REST API for remote execution (v1.6.0+, enhanced v2.0.0+)
6. **python_bridge.py** - Python-side request handler running in each subprocess

**Designer Scope (v2.0.0+):**
1. **Python3IDE_v2.java** - Main IDE orchestration class (refactored v2.0.0)
2. **Managers/** - Business logic (GatewayConnectionManager, ScriptManager, ThemeManager)
3. **UI/** - Presentation layer (EditorPanel, ScriptTreePanel, MetadataPanel, DiagnosticsPanel)

See `python3-integration/docs/V2_ARCHITECTURE_GUIDE.md` for detailed component interactions and data flow.

### Build Commands

```bash
# Build the module (from python3-integration/ directory)
cd python3-integration
./gradlew clean build

# Output location
ls -lh build/libs/*.modl

# Build from repository root
cd /modules/ignition-module-python3
./gradlew -p python3-integration clean build
```

### Testing the Module

```bash
# Install in local Ignition Gateway
# 1. Navigate to http://localhost:8088
# 2. Config → System → Modules → Install or Upgrade a Module
# 3. Upload: python3-integration/build/libs/python3-integration-signed.modl

# Test in Script Console (once installed)
# system.python3.example()
# system.python3.getVersion()
# system.python3.getPoolStats()

# Test Designer IDE (v2.0.0+)
# Open Designer → Tools → Python 3 IDE
# Connect to Gateway, write Python 3 code, click Execute
```

### Key Implementation Files

When modifying module functionality, focus on these files:

- **GatewayHook.java** - Module lifecycle (setup, startup, shutdown), REST API mounting
- **Python3ProcessPool.java** - Pool management, health checking, borrow/return logic
- **Python3Executor.java** - Single process communication, timeout handling
- **Python3ScriptModule.java** - Scripting function definitions with @ScriptFunction annotations
- **Python3RestEndpoints.java** - REST API endpoints (Ignition 8.3 OpenAPI compliant)
- **python_bridge.py** - Python-side command processing (execute, evaluate, call_module)

### External SDK Resources

When learning Ignition SDK patterns or troubleshooting module development issues, reference these official resources:

**Official Documentation:**
- **SDK Documentation**: https://www.sdk-docs.inductiveautomation.com/
  - Getting Started, Module Architecture, Scopes, Hooks, Lifecycle
  - Scripting Functions, RPC Communication, REST APIs
  - Perspective Components, Vision Components, OPC-UA Drivers

**Example Code:**
- **Official SDK Examples**: https://github.com/inductiveautomation/ignition-sdk-examples
  - 17+ example modules with complete source code
  - scripting-function/ - Most similar pattern to this module
  - perspective-component/ - UI component examples
  - opc-ua-device/ - Device driver examples

**Community Resources:**
- **Forum**: https://forum.inductiveautomation.com/c/module-development/7
  - Module development discussions, troubleshooting, best practices
- **Gradle Plugin**: https://github.com/inductiveautomation/ignition-module-tools
  - Build tool documentation and examples

## Module Development Patterns

### Module Lifecycle Critical Phases

Every GatewayHook goes through three phases (see `GatewayHook.java` for implementation):

1. **setup(GatewayContext)** - Early initialization
   - Load configuration (system properties, environment variables)
   - Register extension points
   - **DO NOT** start threads or access database
   - Current module: Loads Python path configuration, initializes logger

2. **startup(LicenseState)** - Main initialization
   - Platform services now available
   - Start background threads, initialize process pools
   - Register script managers
   - Current module: Creates Python3ProcessPool, registers Python3ScriptModule

3. **shutdown()** - Clean shutdown
   - Stop all threads, close connections
   - Release resources to prevent memory leaks
   - Current module: Shuts down process pool, terminates all Python subprocesses

### Scripting Function Registration

To expose functions to Ignition scripts (pattern from `Python3ScriptModule.java`):

```java
@ScriptFunction(docBundlePrefix = "Python3ScriptModule")
public Object exec(String code, @KeywordArgs Map<String, Object> variables) {
    // Implementation
}
```

Then register in GatewayHook.startup():
```java
context.getScriptManager().addScriptModule(
    "system.python3",
    new Python3ScriptModule(processPool),
    new ScriptModuleDocProvider()
);
```

Documentation properties file: `src/main/resources/Python3ScriptModule.properties`

### Build Configuration

This module uses Gradle with the Ignition SDK plugin (`io.ia.sdk.modl`):

- **Root build.gradle.kts**: Defines module metadata, scopes, hooks
- **Scope build.gradle.kts**: Dependencies for each scope (common, gateway, designer)
- **settings.gradle.kts**: Declares subprojects

Key configuration in root `build.gradle.kts`:
```kotlin
ignitionModule {
    projectScopes.putAll(mapOf(
        ":gateway" to "G",      // Gateway scope
        ":common" to "GC"       // Common scope (Gateway + Client)
    ))

    hooks.putAll(mapOf(
        "com.inductiveautomation.ignition.examples.python3.gateway.GatewayHook" to "G"
    ))
}
```

## Thread Safety and Concurrency

The module handles concurrent script execution through process pooling:

### Process Pool Pattern

```java
// From Python3ProcessPool.java
private final BlockingQueue<Python3Executor> availableExecutors;

// Borrow (blocks if pool exhausted)
Python3Executor executor = pool.borrowExecutor(30, TimeUnit.SECONDS);

// Execute with borrowed executor
try {
    result = executor.execute(code, variables);
} finally {
    pool.returnExecutor(executor);  // CRITICAL: Always return
}
```

### Concurrency Constraints

- Pool size (default: 3) = max concurrent Python executions
- Each executor handles one request at a time (synchronized)
- Requests beyond pool size wait up to 30 seconds
- Health checker runs every 30 seconds (separate thread)

### Thread-Safe Patterns Used

1. **BlockingQueue** for executor availability
2. **synchronized** blocks in Python3Executor for command execution
3. **AtomicInteger** for pool statistics
4. **ExecutorService** for health checking

## Configuration System

Configuration is loaded in GatewayHook.setup() via system properties and environment variables:

### Python Path Detection (Priority Order)

1. System property: `-Dignition.python3.path=/path/to/python3`
2. Environment variable: `IGNITION_PYTHON3_PATH`
3. Auto-detection (OS-specific paths in GatewayHook.java)
4. Fallback: `python3`

### Pool Size Configuration

System property: `-Dignition.python3.poolsize=5` (default: 3)

To add to Ignition, edit `ignition.conf`:
```properties
wrapper.java.additional.100=-Dignition.allowunsignedmodules=true
wrapper.java.additional.101=-Dignition.python3.path=/usr/bin/python3.11
wrapper.java.additional.102=-Dignition.python3.poolsize=5
```

## Subprocess Communication Protocol

The module uses **line-based JSON** protocol between Java and Python:

### Request Format (Java → Python via stdin)

```json
{"command": "execute", "code": "result = 2 + 2", "variables": {}}
{"command": "evaluate", "expression": "x + y", "variables": {"x": 10, "y": 20}}
{"command": "call_module", "module": "math", "function": "sqrt", "args": [16]}
```

### Response Format (Python → Java via stdout)

```json
{"success": true, "result": 4}
{"success": false, "error": "NameError: name 'x' is not defined", "traceback": "..."}
```

### Critical Implementation Details

1. **Line-based protocol**: Each request/response is a single line (no pretty-printing)
2. **Unbuffered I/O**: Python started with `-u` flag
3. **Timeout handling**: Java reads with 30s timeout
4. **stderr ignored**: Only stdout used for responses (stderr logged separately)

See `Python3Executor.java` and `python_bridge.py` for full protocol implementation.

## Common Development Tasks

### Adding a New Scripting Function

1. Add method to `Python3ScriptModule.java` with `@ScriptFunction` annotation
2. Add documentation to `Python3ScriptModule.properties`
3. Rebuild: `./gradlew -p python3-integration clean build`
4. Reinstall module in Gateway
5. Test in Script Console

### Extending Python Bridge

1. Add new command handler to `python_bridge.py`:
   ```python
   def process_request(self, request):
       if request['command'] == 'my_command':
           return self.handle_my_command(request)
   ```
2. Add Java method to `Python3Executor.java` to send the new command
3. Update `Python3ScriptModule.java` to expose to scripts

### Debugging

**Gateway logs location**: `<ignition-install>/logs/wrapper.log`

**Check module status**:
```bash
tail -f wrapper.log | grep Python3
```

**Common issues**:
- "Python process is not alive" → Check Python path, verify `python3 --version` works
- "Timeout waiting for executor" → Pool exhausted, increase pool size
- "Failed to parse response" → Check python_bridge.py for print statements (breaks JSON protocol)

## Module Package Structure

Current module uses:
- **Module ID**: `com.inductiveautomation.ignition.examples.python3`
- **Package**: `com.inductiveautomation.ignition.examples.python3.gateway`
- **Hook**: `com.inductiveautomation.ignition.examples.python3.gateway.GatewayHook`

Follows reverse domain notation (Inductive Automation convention for examples).

## Resource Files

Resources in `src/main/resources/` are bundled into the .modl file:

- **python_bridge.py**: Extracted to temp file at runtime, executed by subprocess
- **Python3ScriptModule.properties**: Scripting function documentation

Access at runtime:
```java
InputStream is = getClass().getResourceAsStream("/python_bridge.py");
```

## Critical Best Practices

### Subprocess Management

- **Always** terminate processes in shutdown() to prevent orphaned processes
- **Never** use process.waitFor() without timeout (can hang forever)
- **Always** return executors to pool in finally blocks
- **Monitor** process health continuously (already implemented)

### JSON Communication

- **Never** use print() in python_bridge.py (breaks protocol)
- **Always** use single-line JSON (no newlines in JSON strings)
- **Handle** serialization failures gracefully (complex objects → str)

### Ignition Module Development

- **Use** SLF4J logger, never System.out.println
- **Test** module install/uninstall cycles (check for memory leaks)
- **Version** carefully: SNAPSHOT vs release versions
- **Document** configuration properties

### Certificate Management

**Decision: Certificates are KEPT in git** (not regenerated each time)

**Rationale:**
1. **CI/CD Compatibility** - GitHub Actions workflows work out-of-box without extra setup
2. **Consistency** - Same certificate signature across all environments (dev, test, prod)
3. **Reproducible Builds** - Same inputs produce same outputs, verifiable
4. **Simplicity** - Clone and build works immediately, no extra steps required
5. **Minimal Size** - Only 4.9KB total (certificate.der + keystore.jks + gaskony-cert.pem)

**Files in Repository:**
- `python3-integration/certificate.der` (883 bytes) - Public certificate
- `python3-integration/keystore.jks` (2.7KB) - Private keystore
- `python3-integration/gaskony-cert.pem` (1.3KB) - PEM format certificate
- `python3-integration/sign.props` - Signing configuration with development passwords

**Security Notes:**
- These are **development-only self-signed certificates**
- Passwords (`***REDACTED***`) are public in repository
- Intended for testing and development environments only
- For production distribution, generate new certificates with private keys

**Alternative (Not Recommended):**
- Regenerating certificates each build creates inconsistency
- Different environments get different signatures
- CI/CD breaks without extra configuration
- Script (`generate-signing-certs.sh`) creates `gradle.properties` but build uses `sign.props` (mismatch)
- No practical security benefit for open-source development module

**Current Approach is Correct** for this open-source development project.

## Module Documentation Resources

**In This Repository:**
- **Active module code**: `python3-integration/` (v2.11.2)
- **V2 Architecture Guide**: `python3-integration/docs/V2_ARCHITECTURE_GUIDE.md` ⭐
- **V2 Status Summary**: `python3-integration/docs/V2_STATUS_SUMMARY.md`
- **Testing Guide**: `python3-integration/docs/TESTING_GUIDE.md`
- **Version Workflow**: `python3-integration/docs/VERSION_UPDATE_WORKFLOW.md`
- **Future Roadmap**: `python3-integration/docs/roadmap/README.md`

**External Resources:**
- **Official SDK Docs**: https://www.sdk-docs.inductiveautomation.com/
- **SDK Examples**: https://github.com/inductiveautomation/ignition-sdk-examples
- **Module Development Forum**: https://forum.inductiveautomation.com/c/module-development/7

## Python 3 IDE (v2.0.0+ - IMPLEMENTED)

**STATUS**: ✅ Fully implemented and refactored in v2.0.0 (Oct 2024)

The Python 3 IDE is a **Designer-scoped feature** that provides an IDE-type interface for Python 3 development:

### Implemented Features (v2.0.23)

**Core IDE:**
- ✅ Code editor with Python syntax highlighting (RSyntaxTextArea)
- ✅ Gateway execution via REST API (non-blocking, async)
- ✅ Separate output/error tabs with color coding
- ✅ Execution timing and performance metrics
- ✅ Connection management (multi-Gateway support)

**Script Management:**
- ✅ Save scripts with names and descriptions
- ✅ Load scripts from tree browser
- ✅ Delete scripts with confirmation
- ✅ Rename scripts (v2.0.5)
- ✅ Folder organization (create, rename folders - v2.0.5)
- ✅ Import/Export scripts to .py files (v2.0.7)

**Advanced Features:**
- ✅ Find/Replace toolbar (v2.0.6)
- ✅ Enhanced diagnostics panel (v2.0.8)
- ✅ Theme support (Dark, Light, VS Code Dark+ - v1.11.0+)
- ✅ Real-time Python version detection (v2.0.9)
- ✅ Modular architecture (v2.0.0 refactor)

**v2.0.0 Refactoring:**
- Reduced main class from 2,676 lines → 490 lines (82% reduction)
- Separated concerns: Managers (business logic) + UI Panels (presentation)
- Improved maintainability: 95-490 lines per file (vs 25K tokens before)

### Architecture (v2.0.0+)

**Main Class:**
- `Python3IDE_v2.java` - Orchestration, menu registration, panel assembly

**Managers (Business Logic):**
- `GatewayConnectionManager.java` - Gateway URL management, REST client lifecycle
- `ScriptManager.java` - Script CRUD operations, file I/O
- `ThemeManager.java` - Theme application, RSyntaxTextArea styling

**UI Panels (Presentation):**
- `EditorPanel.java` - Code editor, Execute button, output/error display
- `ScriptTreePanel.java` - Script browser tree, right-click menu
- `MetadataPanel.java` - Script name, description, save button
- `DiagnosticsPanel.java` - Execution time, pool stats, health indicators

### Documentation

- **Full Architecture**: `python3-integration/docs/V2_ARCHITECTURE_GUIDE.md`
- **Feature Comparison**: `python3-integration/docs/V2_FEATURE_COMPARISON_AND_ROADMAP.md`
- **Status Summary**: `python3-integration/docs/V2_STATUS_SUMMARY.md`

### Historical Context

The original IDE plan (`python3-integration/docs/PYTHON_IDE_PLAN.md`) outlined v1.7.0-v1.8.0 implementation phases. This has been fully implemented and later refactored to v2.0.0 architecture for better maintainability.

## REST API Endpoints (v1.6.0+)

The module exposes a **REST API** following Ignition 8.3 OpenAPI standards.

### API Endpoint Pattern

All endpoints follow the Ignition 8.3 convention:
```
/data/python3integration/api/v1/{endpoint}
```

This ensures:
- **OpenAPI compliance** - Endpoints appear in `/openapi.json`
- **Discoverability** via Ignition's API documentation
- **API versioning** for future compatibility
- **Standard authentication** via API tokens or session

### Available Endpoints

**POST Endpoints** (Execute Python code):
- `/data/python3integration/api/v1/exec` - Execute Python statements
- `/data/python3integration/api/v1/eval` - Evaluate Python expressions
- `/data/python3integration/api/v1/call-module` - Call Python module functions

**GET Endpoints** (Status & Info):
- `/data/python3integration/api/v1/version` - Python version information
- `/data/python3integration/api/v1/pool-stats` - Process pool statistics
- `/data/python3integration/api/v1/health` - Health check endpoint
- `/data/python3integration/api/v1/diagnostics` - Performance diagnostics
- `/data/python3integration/api/v1/example` - Example test endpoint

### Authentication

REST API endpoints use `RouteAccess.GRANTED` for public access. They can be secured at the gateway level using:
- **API Tokens**: Generate in Gateway → Security → API Keys
- **Session Auth**: Login via `/data/app/login`

Bearer token authentication:
```bash
curl -H "Authorization: Bearer <token>" http://localhost:8088/data/python3integration/api/v1/health
```

### Example Usage

```bash
# Health check (no auth for public endpoints)
curl http://localhost:8088/data/python3integration/api/v1/health

# Execute Python code
curl -X POST http://localhost:8088/data/python3integration/api/v1/exec \
  -H "Content-Type: application/json" \
  -d '{"code": "result = 2 + 2", "variables": {}}'

# Evaluate expression
curl -X POST http://localhost:8088/data/python3integration/api/v1/eval \
  -H "Content-Type": "application/json" \
  -d '{"expression": "x + y", "variables": {"x": 10, "y": 20}}'

# Call Python module
curl -X POST http://localhost:8088/data/python3integration/api/v1/call-module \
  -H "Content-Type: application/json" \
  -d '{"module": "math", "function": "sqrt", "args": [16]}'
```

### Implementation Details

REST endpoints are implemented in `Python3RestEndpoints.java`:
- All routes use `.accessControl(req -> RouteAccess.GRANTED)` for access control
- All routes use `.type(RouteGroup.TYPE_JSON)` for JSON handling
- Routes are mounted in `GatewayHook.mountRouteHandlers()`
- Requires Perspective gateway dependencies for access control API

### Adding New REST Endpoints

1. Add handler method to `Python3RestEndpoints.java`:
   ```java
   private static JsonObject handleMyEndpoint(RequestContext req, HttpServletResponse res) {
       try {
           // Implementation
           JsonObject response = new JsonObject();
           response.addProperty("success", true);
           return response;
       } catch (Exception e) {
           return createErrorResponse(e.getMessage());
       }
   }
   ```

2. Mount route in `mountRoutes()`:
   ```java
   routes.newRoute("/api/v1/my-endpoint")
       .handler(Python3RestEndpoints::handleMyEndpoint)
       .method(HttpMethod.GET)
       .type(RouteGroup.TYPE_JSON)
       .accessControl(req -> RouteAccess.GRANTED)
       .mount();
   ```

3. Rebuild and reinstall module

