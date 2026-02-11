# Python3IDE.java Extraction - Quick Reference

## At a Glance
- **File Size:** 4,390 lines
- **Extraction Candidates:** 8 managers
- **Potential Reduction:** 1,500+ lines (36%)
- **Estimated Effort:** 6-8 hours over 2-3 sprints

## Recommended Extraction Sequence

### ✅ QUICK WINS (Start Here) - Week 1
| Manager | Lines | Time | Risk | Status |
|---------|-------|------|------|--------|
| **AutoSaveManager** | 120 | 30min | ⭐ VERY LOW | Ready to start |
| **CommandPaletteManager** | 280 | 20min | ⭐ VERY LOW | Ready to start |
| **SearchManager** | 100 | 20min | ⭐ LOW | Ready to start |

### 🔥 HIGH VALUE (Next) - Week 2
| Manager | Lines | Time | Risk | Dependencies |
|---------|-------|------|------|--------------|
| **ExecutionManager** | 250 | 60min | ⭐⭐ MEDIUM | RestClient, UI components |
| **ScriptImportExportManager** | 180 | 45min | ⭐ LOW | File I/O, dialogs |

### 🎯 CONSOLIDATION (Later) - Week 3
| Manager | Lines | Time | Risk | Dependencies |
|---------|-------|------|------|--------------|
| **KeyboardShortcutsManager** | 150 | 40min | ⭐ LOW | All action methods |
| **ScriptTransferManager** | 280 | 90min | ⭐⭐ MEDIUM-HIGH | Recursive operations |

### ⚠️ CONDITIONAL (Review First)
| Manager | Lines | Time | Risk | Note |
|---------|-------|------|------|------|
| **UIPreferencesManager** | 200 | 60min | ⭐⭐ MEDIUM | Coordinate with ThemeManager |

## Manager Profiles

### AutoSaveManager ⭐⭐⭐⭐⭐
**What it does:** Auto-saves scripts every 30 seconds
**Extract:** `initializeAutoSave()`, `performAutoSave()`, `cleanupOldAutosaveFiles()`
**Why first:** Smallest, safest, zero dependencies
**Test impact:** Testable with mock Timer
```java
// After extraction
autoSaveManager = new AutoSaveManager(changesTracker, codeEditor, restClient);
autoSaveManager.initialize();
```

### CommandPaletteManager ⭐⭐⭐⭐⭐
**What it does:** Registers 20+ commands (keyboard + palette)
**Extract:** `initializeCommandPalette()` (174 lines of command registration)
**Why quick:** Pure data configuration, no business logic
**Test impact:** Easy to unit test - just verify commands exist
```java
// After extraction
commandPalette = CommandPaletteManager.create(
    (Frame) SwingUtilities.getWindowAncestor(this),
    this  // pass reference to IDE for callbacks
);
```

### SearchManager ⭐⭐⭐⭐
**What it does:** Manages Find/Replace dialogs
**Extract:** `showFindDialog()`, `showReplaceDialog()`, `showAdvancedFindReplaceDialog()`
**Why second:** Lazy initialization pattern, no state mutations
**Test impact:** Works with RSyntaxTextArea API
```java
// After extraction
searchManager = new SearchManager(codeEditor, statusBar);
searchManager.showFindDialog();
```

### ExecutionManager ⭐⭐⭐
**What it does:** Core IDE execution logic (IDE mode + Terminal mode)
**Extract:** `executeCode()`, `handleSuccess()`, `handleError()`, `onModeTabChanged()`
**Why high value:** 250 lines of core business logic
**Complexity:** Terminal/IDE mode switching, async execution
**Test impact:** Needs async testing strategy
```java
// After extraction
executionManager = new ExecutionManager(
    restClient, codeEditor, outputArea, errorArea,
    this::handleSuccess, this::handleError
);
executionManager.setExecutionMode(isTerminalMode);
executionManager.execute(code);
```

### ScriptImportExportManager ⭐⭐⭐⭐
**What it does:** Import scripts from .py files, export to .py files
**Extract:** `importScript()`, `exportScript()`, `exportCurrentScript()`
**Why medium priority:** File I/O is testable, dialogs are standard
**Test impact:** Can mock file system
```java
// After extraction
importExportManager = new ScriptImportExportManager(
    restClient, codeEditor, statusBar
);
importExportManager.importScript();
importExportManager.exportCurrentScript();
```

### KeyboardShortcutsManager ⭐⭐⭐
**What it does:** Registers 13 keyboard shortcuts
**Extract:** `setupKeyboardShortcuts()` (150 lines)
**Why later:** Needs method references to all actions (more dependencies)
**Test impact:** Can simulate key events in tests
```java
// After extraction
keyboardManager = new KeyboardShortcutsManager(codeEditor, this);
keyboardManager.setupShortcuts();
```

### ScriptTransferManager ⭐⭐
**What it does:** Move scripts between folders, drag & drop
**Extract:** `moveScriptToFolder()`, `moveScript()`, `moveFolder()` (inner class)
**Why last:** Most complex - recursive folder operations
**Test impact:** Needs comprehensive testing for folder path logic
**Risk:** Folder move affects all nested scripts
```java
// After extraction
transferManager = new ScriptTransferManager(
    restClient, rootNode, scriptTree, statusBar
);
transferManager.moveScriptToFolder("ScriptName", "Folder/Path");
```

## Dependency Summary

### Most Critical Fields to Extract
```java
// These move to multiple managers
- Python3RestClient restClient       // ExecutionManager, ScriptImportExportManager, ScriptTransferManager
- RSyntaxTextArea codeEditor         // ExecutionManager, SearchManager, KeyboardShortcutsManager
- ModernStatusBar statusBar          // All managers (status callbacks)
- JTextArea outputArea, errorArea    // ExecutionManager
- ScriptMetadata currentScript       // AutoSaveManager, ExecutionManager
```

### Fields That Stay in Python3IDE
```java
// These are UI layout concerns
- All JPanel, JSplitPane, JTree components
- scriptTree, rootNode (tree structure)
- UI assembly and initialization code
```

## Testing Strategy

### Unit Tests Needed
```java
// AutoSaveManager_Test.java
- testPerformAutoSave_WithDirtyScript()
- testAutoSaveCleanup_KeepLast5()
- testAutoSave_SkipsWhenNotConnected()

// CommandPaletteManager_Test.java
- testCommandsRegistered()
- testCommandsCategorized()

// ExecutionManager_Test.java
- testExecute_IDEMode()
- testExecute_TerminalMode()
- testExecutionTimeout()

// ScriptTransferManager_Test.java
- testMoveScriptToFolder()
- testMoveFolder_UpdatesNestedScripts()
- testFolderPathCalculation()
```

### Integration Tests Needed
```java
// KeyboardShortcuts + Execution
- testCtrlEnter_ExecutesCode()
- testCtrlS_SavesScript()

// Drag & Drop + ScriptTransfer
- testDragDrop_MovesScriptFolder()
```

## Implementation Checklist

### For Each Extraction
- [ ] Create new manager class with dependency injection
- [ ] Move methods from Python3IDE to manager
- [ ] Leave delegating methods in Python3IDE (calling manager)
- [ ] Add unit tests for manager
- [ ] Test keyboard shortcuts still work
- [ ] Test command palette still works
- [ ] Verify no new compiler warnings
- [ ] Commit with message: "Extract [ManagerName]: [Brief description]"

### Quality Gates
- [ ] All tests pass
- [ ] Code review approved
- [ ] No functionality regression
- [ ] Cyclomatic complexity of Python3IDE decreases
- [ ] No new dependencies introduced

## Risk Mitigation

### If Extraction Fails
1. Revert commit: `git revert <commit-hash>`
2. Review failing tests
3. Identify missing dependencies
4. Try alternative extraction approach

### Per-Manager Risks
| Manager | Risk | Mitigation |
|---------|------|-----------|
| AutoSaveManager | Timer not stopping | Test shutdown() method |
| CommandPaletteManager | Commands not callable | Mock Python3IDE for tests |
| ExecutionManager | Async deadlocks | Use CountDownLatch in tests |
| ScriptTransferManager | Folder path bugs | Comprehensive path unit tests |

## Success Metrics

### After All Extractions
- Python3IDE.java: 4,390 → 2,800 lines (36% reduction)
- Methods per class: 80+ → 5-8
- Cyclomatic complexity: HIGH → MEDIUM-HIGH
- Testable methods: 20% → 70%
- Dependencies between classes: Clear, acyclic

## Questions to Answer Before Starting

1. **Should KeyboardShortcutsManager + CommandPaletteManager merge?**
   - Could create unified CommandRegistry for central command management
   - Pros: Single source of truth
   - Cons: Larger class, mixed concerns

2. **How to handle UIPreferencesManager + ThemeManager overlap?**
   - Review ThemeManager.java first
   - Determine if it should expand or stay focused
   - Avoid code duplication

3. **Should ExecutionManager support strategy pattern?**
   - ExecutionStrategy interface for IDE vs Terminal modes
   - Makes testing easier
   - Adds abstraction layer

4. **How to test async operations (ExecutionManager)?**
   - SwingUtilities.invokeAndWait() in tests?
   - Mock ExecutorService?
   - Separate logic from threading?

## Reference Files
- Full Analysis: `/modules/ignition-module-python3/python3-integration/PYTHON3IDE_PHASE2B_REFACTORING_ANALYSIS.md`
- Current File: `/modules/ignition-module-python3/python3-integration/designer/src/main/java/com/inductiveautomation/ignition/examples/python3/designer/Python3IDE.java`
- Existing Managers: `/modules/ignition-module-python3/python3-integration/designer/src/main/java/com/inductiveautomation/ignition/examples/python3/designer/managers/`

