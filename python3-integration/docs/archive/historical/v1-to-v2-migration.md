# Python3IDE v1.9 to v2.0 Migration Guide

**Version:** 2.0.0+
**Last Updated:** 2025-10-17
**Author:** Claude Code

## Overview

This guide helps developers migrate from Python3IDE v1.9 (2,676-line monolith) to Python3IDE v2.0 (refactored architecture).

### Migration Philosophy

**Gradual, Not Big Bang**
- v1.9 remains fully functional with all features
- v2.0 demonstrates new architecture with core features
- Both versions coexist in the codebase
- Migrate features incrementally as needed

---

## Why Migrate?

### v1.9 Challenges

1. **Token Limit Issues**
   - Single file: 2,676 lines, ~25,000 tokens
   - Exceeds AI tool processing limits
   - Difficult to review and modify

2. **Maintainability**
   - Mixed concerns (UI + logic + events)
   - Hard to find specific functionality
   - Risky to make changes

3. **Testability**
   - Cannot unit test business logic independently
   - UI and logic tightly coupled
   - Mocking is difficult

### v2.0 Advantages

1. **Modularity**
   - Files: 95-440 lines each
   - Token usage: 1K-4.5K per file
   - 10x reduction in complexity

2. **Maintainability**
   - Clear separation of concerns
   - Each class has single responsibility
   - Easy to find and modify features

3. **Testability**
   - Managers can be unit tested
   - UI panels can be tested in isolation
   - Easy to mock dependencies

---

## Architecture Comparison

### v1.9 Structure
```
Python3IDE_v1_9.java (2,676 lines)
    ├── UI Components (mixed throughout)
    ├── Gateway Connection Logic (mixed)
    ├── Script Management (mixed)
    ├── Theme Management (mixed)
    ├── Event Handlers (mixed)
    └── Utility Methods (mixed)
```

**Everything in one file!**

### v2.0 Structure
```
Python3IDE_v2.java (440 lines)
    │
    ├── managers/
    │   ├── GatewayConnectionManager.java (110 lines)
    │   ├── ScriptManager.java (105 lines)
    │   └── ThemeManager.java (220 lines)
    │
    └── ui/
        ├── EditorPanel.java (95 lines)
        └── ScriptTreePanel.java (135 lines)
```

**Clear separation of concerns!**

---

## Feature Parity Status

### ✅ Implemented in v2.0.0

| Feature | v1.9 | v2.0 | Notes |
|---------|------|------|-------|
| Gateway Connection | ✅ | ✅ | Via `GatewayConnectionManager` |
| Code Execution | ✅ | ✅ | Via `EditorPanel` + REST API |
| Script Save | ✅ | ✅ | Via `ScriptManager.saveScript()` |
| Script Load | ✅ | ✅ | Via `ScriptManager.loadScript()` |
| Script List | ✅ | ✅ | Via `ScriptManager.listScripts()` |
| Script Delete | ✅ | ✅ | Via `ScriptManager.deleteScript()` |
| Script Tree Browser | ✅ | ✅ | Via `ScriptTreePanel` |
| Folder Hierarchy | ✅ | ✅ | Automatic from `folderPath` |
| Metadata Display | ✅ | ✅ | Via `ScriptMetadataPanel` |
| Theme Switching | ✅ | ✅ | Via `ThemeManager` |
| Dark/Light Themes | ✅ | ✅ | Dark, Monokai, VS Code Dark+ |
| Status Bar | ✅ | ✅ | Via `ModernStatusBar` |
| Diagnostics Panel | ✅ | ✅ | Via `DiagnosticsPanel` |

### ⏳ TODO in v2.x

| Feature | v1.9 | v2.0 | Target Version |
|---------|------|------|----------------|
| Script Rename | ✅ | ❌ | v2.1.0 |
| Folder Create | ✅ | ❌ | v2.1.0 |
| Folder Rename | ✅ | ❌ | v2.1.0 |
| Folder Delete | ✅ | ❌ | v2.1.0 |
| Import Scripts | ✅ | ❌ | v2.1.0 |
| Export Scripts | ✅ | ❌ | v2.1.0 |
| Find/Replace | ✅ | ❌ | v2.1.0 |
| Auto-Completion | ✅ | ❌ | v2.1.0 |
| Syntax Checking | ✅ | ❌ | v2.2.0 |
| Code Profiling | ✅ | ❌ | v2.2.0 |
| Drag-and-Drop | ✅ | ❌ | v2.2.0 |

---

## Migration Strategies

### Strategy 1: Parallel Development (Recommended)

**When:** Adding new features

**Approach:**
1. Implement new features in v2.0 architecture
2. Keep v1.9 for existing users
3. Gradually deprecate v1.9 when v2.x reaches full parity

**Benefits:**
- No disruption to existing users
- Clean implementation of new features
- Natural migration path

**Example:**
```java
// Add find/replace feature
// 1. Create FindReplaceManager.java (new)
// 2. Create FindReplacePanel.java (new)
// 3. Integrate into Python3IDE_v2.java (modify)
// 4. v1.9 keeps old implementation
```

---

### Strategy 2: Extract and Refactor

**When:** Fixing bugs in v1.9

**Approach:**
1. Identify problematic code in v1.9
2. Extract to new manager in v2.0
3. Fix bug in clean manager class
4. Backport fix to v1.9 if critical

**Benefits:**
- Bug fix becomes part of migration
- Clean code in v2.0
- Incremental progress

**Example:**
```java
// Bug: Script save doesn't handle special characters
// 1. Extract script validation to ScriptManager.validateScriptName()
// 2. Fix validation logic in clean method
// 3. Use in v2.0
// 4. Optionally backport to v1.9
```

---

### Strategy 3: Feature Freeze v1.9

**When:** v2.x reaches 80%+ parity

**Approach:**
1. Announce v1.9 feature freeze
2. Complete remaining v2.x features
3. Mark v1.9 as legacy
4. Deprecate v1.9 in next major version

**Benefits:**
- Focus development on v2.x
- Clear migration timeline
- Users have time to transition

---

## Code Migration Examples

### Example 1: Moving Gateway Connection Logic

**v1.9 (lines 150-250 in Python3IDE_v1_9.java):**
```java
// Mixed with other code
private void connectToGateway() {
    String url = gatewayUrlField.getText().trim();
    // ... 50+ lines of connection logic
    // ... mixed with UI updates
    // ... mixed with error handling
}
```

**v2.0 (managers/GatewayConnectionManager.java):**
```java
public class GatewayConnectionManager {
    public boolean connect(String url) {
        // Clean, testable logic
        // No UI dependencies
        // Clear responsibility
    }
}

// Python3IDE_v2.java
private void connectToGateway() {
    String url = gatewayUrlField.getText().trim();
    boolean connected = connectionManager.connect(url);  // Clean call

    if (connected) {
        // UI updates separated
    }
}
```

**Migration Steps:**
1. Create `GatewayConnectionManager.java`
2. Copy connection logic from v1.9
3. Remove UI dependencies
4. Add unit tests
5. Use in v2.0

---

### Example 2: Script Management

**v1.9 (scattered throughout Python3IDE_v1_9.java):**
```java
// Save script (lines 500-550)
private void saveScript() {
    // ... REST API call
    // ... UI updates
    // ... error handling
}

// Load script (lines 600-650)
private void loadScript(String name) {
    // ... REST API call
    // ... metadata extraction
    // ... UI updates
}

// Delete script (lines 700-750)
private void deleteScript(String name) {
    // ... confirmation dialog
    // ... REST API call
    // ... tree refresh
}
```

**v2.0 (managers/ScriptManager.java):**
```java
public class ScriptManager {
    private final Python3RestClient restClient;

    public SavedScript saveScript(String name, String code, ...) throws IOException {
        return restClient.saveScript(name, code, ...);
    }

    public SavedScript loadScript(String name) throws IOException {
        SavedScript script = restClient.loadScript(name);
        this.currentScript = script;
        return script;
    }

    public boolean deleteScript(String name) throws IOException {
        return restClient.deleteScript(name);
    }
}

// Python3IDE_v2.java
private void saveScript() {
    String name = JOptionPane.showInputDialog(this, "Enter script name:");
    if (name != null && !name.trim().isEmpty()) {
        try {
            scriptManager.saveScript(name.trim(), editorPanel.getCode(), ...);
            refreshScriptTree();
        } catch (Exception e) {
            handleError(e);
        }
    }
}
```

**Benefits:**
- Business logic separated from UI
- ScriptManager is testable
- REST API calls centralized
- Exception handling clear

---

### Example 3: Theme Management

**v1.9 (lines 1000-1200 in Python3IDE_v1_9.java):**
```java
private void applyTheme(String themeName) {
    // ... 200+ lines of theme logic
    // ... mixed with component traversal
    // ... hard to understand and modify
}
```

**v2.0 (managers/ThemeManager.java):**
```java
public class ThemeManager {
    public void applyTheme(String themeName, Component rootComponent,
                           RSyntaxTextArea codeEditor, JTextArea outputArea,
                           JTextArea errorArea, JTree scriptTree) throws IOException {
        // Clean, organized theme logic
        Theme theme = loadTheme(themeName);
        theme.apply(codeEditor);

        boolean isDarkTheme = themeName.toLowerCase().contains("dark");
        if (isDarkTheme) {
            applyDarkTheme(outputArea, errorArea, scriptTree, rootComponent);
        } else {
            applyLightTheme(outputArea, errorArea, scriptTree, rootComponent);
        }

        // ... rest of clean implementation
    }
}
```

**Benefits:**
- Self-contained theme management
- Easy to add new themes
- No dependencies on Python3IDE_v2 internal state
- Can be tested independently

---

## Dependency Management

### v1.9 Dependencies
All dependencies bundled in one class:
- DesignerContext
- Python3RestClient
- UI components
- Event handlers
- Utility classes

**Problem:** Everything depends on everything!

### v2.0 Dependencies

**Managers depend on:**
- Python3RestClient (injected via constructor)
- No UI dependencies
- No DesignerContext dependencies

**UI Panels depend on:**
- Nothing! (self-contained presentation)
- Communicate via callbacks

**Python3IDE_v2 depends on:**
- Managers (owns them)
- UI Panels (owns them)
- DesignerContext (constructor parameter)

**Benefit:** Clear, unidirectional dependency flow!

---

## Testing Migration

### v1.9 Testing Challenges

```java
// Cannot unit test gateway connection without entire IDE
@Test
public void testConnect() {
    // Must create full Python3IDE_v1_9 instance
    // Must mock DesignerContext
    // Must mock all UI components
    // Nearly impossible!
}
```

### v2.0 Testing Strategy

```java
// Unit test GatewayConnectionManager independently
@Test
public void testConnect() {
    GatewayConnectionManager manager = new GatewayConnectionManager();

    // Mock REST client response
    // Test connection logic in isolation
    // Easy and fast!
}

// Unit test ScriptManager
@Test
public void testSaveScript() {
    Python3RestClient mockClient = mock(Python3RestClient.class);
    when(mockClient.saveScript(...)).thenReturn(mockSavedScript);

    ScriptManager manager = new ScriptManager(mockClient);
    SavedScript result = manager.saveScript("test", "code", ...);

    assertNotNull(result);
    verify(mockClient).saveScript(...);
}

// Integration test UI panels
@Test
public void testEditorPanel() {
    EditorPanel panel = new EditorPanel();
    panel.setCode("print('test')");

    assertEquals("print('test')", panel.getCode());
}
```

---

## Migration Checklist

### Phase 1: Understand v2.0 Architecture
- [ ] Read [V2_ARCHITECTURE_GUIDE.md](V2_ARCHITECTURE_GUIDE.md)
- [ ] Review `Python3IDE_v2.java` code
- [ ] Explore `managers/` package
- [ ] Explore `ui/` package
- [ ] Understand data flow diagrams

### Phase 2: Identify Feature to Migrate
- [ ] Pick a feature from TODO list (e.g., find/replace)
- [ ] Locate equivalent code in v1.9
- [ ] Understand dependencies and interactions

### Phase 3: Extract Business Logic
- [ ] Create new manager class (if needed)
- [ ] Extract logic from v1.9
- [ ] Remove UI dependencies
- [ ] Add unit tests
- [ ] Document manager API

### Phase 4: Create UI Component
- [ ] Create new UI panel (if needed)
- [ ] Design component structure
- [ ] Implement presentation logic only
- [ ] Add integration tests
- [ ] Document component API

### Phase 5: Integrate into v2.0
- [ ] Add manager to `Python3IDE_v2` constructor
- [ ] Add UI panel to layout
- [ ] Wire events via `wireUpEvents()`
- [ ] Test end-to-end functionality
- [ ] Update documentation

### Phase 6: Verify and Deploy
- [ ] Build module: `./gradlew clean build`
- [ ] Test in Ignition Gateway
- [ ] Verify feature works as expected
- [ ] Update version in `version.properties`
- [ ] Commit and push changes

---

## Common Migration Pitfalls

### Pitfall 1: Direct v1.9 Copy-Paste

**Problem:**
```java
// Copying v1.9 code directly
public class MyManager {
    private JButton someButton;  // ❌ UI dependency!
    private Python3IDE_v1_9 ide;  // ❌ Circular dependency!

    public void doSomething() {
        someButton.setEnabled(false);  // ❌ Manager shouldn't touch UI!
    }
}
```

**Solution:**
```java
// Clean manager
public class MyManager {
    private final Python3RestClient restClient;  // ✅ Business dependency only

    public Result doSomething() {
        // ✅ Return data, let caller handle UI
        return restClient.callSomeAPI();
    }
}

// Python3IDE_v2 handles UI
private void handleButtonClick() {
    Result result = myManager.doSomething();
    someButton.setEnabled(false);  // ✅ UI updates in main class
}
```

---

### Pitfall 2: Not Using Dependency Injection

**Problem:**
```java
// Manager creates its own dependencies
public class MyManager {
    public MyManager() {
        this.restClient = new Python3RestClient();  // ❌ Hard to test!
    }
}
```

**Solution:**
```java
// Inject dependencies
public class MyManager {
    private final Python3RestClient restClient;

    public MyManager(Python3RestClient restClient) {  // ✅ Injectable
        this.restClient = restClient;
    }
}

// Easy to mock in tests
@Test
public void testMyManager() {
    Python3RestClient mockClient = mock(Python3RestClient.class);
    MyManager manager = new MyManager(mockClient);  // ✅ Testable!
}
```

---

### Pitfall 3: Mixed Threading Models

**Problem:**
```java
// Mixing EDT and background threads
public void executeCode() {
    // ❌ Long-running operation on EDT - freezes UI!
    ExecutionResult result = connectionManager.executeCode(code, vars);
    editorPanel.setOutput(result.getResult());
}
```

**Solution:**
```java
// Use SwingWorker for background operations
public void executeCode() {
    SwingWorker<ExecutionResult, Void> worker = new SwingWorker<>() {
        @Override
        protected ExecutionResult doInBackground() {
            // ✅ Background thread
            return connectionManager.executeCode(code, vars);
        }

        @Override
        protected void done() {
            try {
                ExecutionResult result = get();
                // ✅ UI update on EDT
                editorPanel.setOutput(result.getResult());
            } catch (Exception e) {
                handleError(e);
            }
        }
    };

    worker.execute();
}
```

---

## Migration Timeline Recommendation

### Month 1: Core Features
- Stabilize v2.0.0 with existing features
- Write comprehensive documentation (✅ Done)
- Set up testing framework

### Month 2: Feature Parity
- Implement v2.1.0 features:
  - Script rename
  - Folder management
  - Import/export
  - Find/replace

### Month 3: Advanced Features
- Implement v2.2.0 features:
  - Auto-completion improvements
  - Code profiling
  - Historical metrics

### Month 4: Deprecation
- Announce v1.9 feature freeze
- Encourage users to switch to v2.x
- Plan v1.9 removal for v3.0.0

---

## Resources

### Documentation
- [V2_ARCHITECTURE_GUIDE.md](V2_ARCHITECTURE_GUIDE.md) - Detailed architecture guide
- [DEVELOPER_EXTENSION_GUIDE.md](DEVELOPER_EXTENSION_GUIDE.md) - How to extend v2.0

### Code References
- v2.0 Main: [Python3IDE_v2.java](../designer/src/main/java/com/inductiveautomation/ignition/examples/python3/designer/Python3IDE_v2.java)
- v1.9 Legacy: [Python3IDE_v1_9.java](../designer/src/main/java/com/inductiveautomation/ignition/examples/python3/designer/Python3IDE_v1_9.java)

### Discussions
- GitHub Issues: https://github.com/nigelgwork/ignition-module-python3/issues
- Module Roadmap: [../ROADMAP.md](../ROADMAP.md)

---

## Getting Help

### Questions?
1. Review this migration guide
2. Check [V2_ARCHITECTURE_GUIDE.md](V2_ARCHITECTURE_GUIDE.md)
3. Search existing GitHub issues
4. Open new GitHub issue with `[v2-migration]` tag

### Contributing
1. Pick a feature from TODO list
2. Follow migration checklist
3. Submit pull request
4. Update documentation

---

**Document Version:** 1.0
**Module Version:** 2.0.2
**Generated:** 2025-10-17 by Claude Code
