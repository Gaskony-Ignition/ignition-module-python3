# Python3IDE.java Phase 2B Refactoring Analysis

## Executive Summary

**File:** `Python3IDE.java`
**Total Lines:** 4,390
**Total Methods:** 80+ (including nested classes)
**Refactoring Opportunity:** ~1,500+ lines can be extracted into focused manager classes

This analysis identifies business logic that can be extracted into 5-6 focused manager classes, reducing Python3IDE.java from 4,390 to approximately 2,800 lines (36% reduction).

---

## 1. File Statistics

### Current Metrics
- **Total Lines of Code:** 4,390
- **Estimated Method Count:** 80+ (including anonymous inner classes)
- **UI Component Fields:** 25+ (buttons, panels, text areas, dialogs)
- **Manager Fields:** 4 (GatewayConnectionManager, ScriptManager, ThemeManager, RecentScriptsManager)
- **State Fields:** 15+ (theme, fontSize, currentScript, etc.)
- **Service Dependencies:** Python3RestClient, PythonSyntaxChecker, AutoCompletion

### Class Structure
```
Python3IDE (JPanel) - 4,390 lines
├── Field Declarations (250 lines)
├── Constructor (50 lines)
├── Component Initialization (150 lines)
├── Component Layout (400 lines)
├── UI Setup & Listeners (150 lines)
├── Keyboard Shortcuts (120 lines)
├── Gateway Connection (80 lines)
├── Execution Logic (100 lines) ← EXTRACT TO ExecutionManager
├── Terminal Mode (180 lines) ← EXTRACT TO ExecutionManager
├── Script Management (600 lines) ← USE ScriptManager
├── Auto-Save (100 lines) ← EXTRACT TO AutoSaveManager
├── Command Palette (300 lines) ← EXTRACT TO CommandPaletteManager
├── Search/Find/Replace (100 lines) ← EXTRACT TO SearchManager
├── Import/Export (150 lines) ← EXTRACT TO ScriptImportExportManager
├── Theme/Display (400 lines) ← PARTIALLY extract to UIPreferencesManager
├── Drag & Drop (200 lines) ← EXTRACT TO ScriptTransferManager
└── Utility Methods (200 lines)
```

---

## 2. Extraction Candidates Analysis

### CANDIDATE 1: ExecutionManager
**Lines:** ~250 lines to extract | **Complexity:** MEDIUM
**Current Location:** Lines 1067-1280, 1290-1360

#### Methods to Extract
```java
private void executeCode()              // Lines 1067-1167 (100 lines)
private void handleSuccess(...)         // Lines 1172-1190 (18 lines)
private void handleError(...)           // Lines 1195-1203 (8 lines)
private void onModeTabChanged(...)      // Lines 1211-1290 (79 lines)
private void clearOutput()              // ~10 lines
private void executeTerminalCommand(..) // ~50 lines
private void updateTerminalWorkingDir() // ~30 lines
```

#### Dependencies (Fields needed)
```java
- Python3RestClient restClient
- RSyntaxTextArea codeEditor
- JTextArea outputArea
- JTextArea errorArea
- ModernStatusBar statusBar
- JButton executeButton
- JProgressBar progressBar
- CustomTabButton pythonIdeTab
- CustomTabButton terminalTab
- TerminalPanel terminalPanel
- JPanel centerPanel
- String interactiveShellSessionId
- StringBuilder terminalHistory
- SwingWorker currentWorker
- UnsavedChangesTracker changesTracker
```

#### Complexity Assessment: MEDIUM
- Straightforward execution flow
- Clear separation between terminal and IDE modes
- Callback-based success/error handling

#### Risk Assessment: LOW
- No UI layout code
- No thread creation beyond SwingWorker
- Uses existing callback pattern

---

### CANDIDATE 2: AutoSaveManager
**Lines:** ~120 lines to extract | **Complexity:** SIMPLE
**Current Location:** Lines 3561-3628

#### Methods to Extract
```java
private void initializeAutoSave()       // Lines 3561-3565 (4 lines)
private void performAutoSave()          // Lines 3570-3602 (32 lines)
private void cleanupOldAutosaveFiles()  // Lines 3607-3628 (21 lines)
```

#### Dependencies (Fields needed)
```java
- Timer autoSaveTimer
- static final int AUTO_SAVE_INTERVAL_MS = 30000
- UnsavedChangesTracker changesTracker
- ScriptMetadata currentScript
- RSyntaxTextArea codeEditor
- Python3RestClient restClient
- ModernStatusBar statusBar
```

#### Complexity Assessment: SIMPLE
- Self-contained timer logic
- Minimal external dependencies
- File I/O is straightforward

#### Risk Assessment: VERY LOW
- No UI modifications
- No complex state management
- Can be fully encapsulated

#### Additional Considerations
- Could be further abstracted into AutoSaveStrategy pattern
- Consider configurable interval via preferences
- Status bar is only used for notifications (could be via callback)

---

### CANDIDATE 3: CommandPaletteManager
**Lines:** ~280 lines to extract | **Complexity:** SIMPLE
**Current Location:** Lines 3529-3807

#### Methods to Extract
```java
private void showCommandPalette()       // Lines 3529-3536 (7 lines)
private void initializeCommandPalette() // Lines 3633-3807 (174 lines)
```

#### Dependencies (Fields needed)
```java
- CommandPaletteDialog commandPalette
- Frame (from SwingUtilities.getWindowAncestor)
```

#### Command Groups (Registered Methods)
- **Execution:** executeCode(), clearOutput()
- **File:** saveCurrentScript(), saveScriptAs(), createNewScript(), 
           importButton.doClick(), exportButton.doClick(), 
           refreshScriptTree()
- **Search:** showFindDialog(), showReplaceDialog(), showAdvancedFindReplaceDialog()
- **View:** toggleSidebar(), changeFontSize(+1), changeFontSize(-1), 
           setFontSize(12)
- **Theme:** applyTheme("dark"), applyTheme("light"), applyTheme("vscode")
- **Gateway:** connectToGateway()
- **Settings:** openSettingsDialog()
- **Help:** showInformationDialog()
- **Tools:** openPackagesDialog()

#### Complexity Assessment: SIMPLE
- Pure data configuration (command registration)
- No complex logic
- Works with existing command palette dialog

#### Risk Assessment: VERY LOW
- Completely isolated from other logic
- Only calls existing public methods
- Can be tested independently

#### Note
This is almost a factory/builder pattern - could be even simpler if CommandPaletteDialog supports fluent API registration.

---

### CANDIDATE 4: ScriptImportExportManager
**Lines:** ~180 lines to extract | **Complexity:** SIMPLE-MEDIUM
**Current Location:** Lines 2125-2167, 2780-2908

#### Methods to Extract
```java
private void importScript()             // Lines 2780-2859 (79 lines)
private void exportScript(...)          // Lines 2125-2167 (42 lines)
private void exportCurrentScript()      // Lines 2864-2908 (44 lines)
private void saveScript(...)            // Already exists in ScriptManager
```

#### Dependencies (Fields needed)
```java
- Python3RestClient restClient
- ScriptMetadata currentScript
- RSyntaxTextArea codeEditor
- ModernStatusBar statusBar (for status messages)
```

#### Methods Already in ScriptManager
```java
saveScript(name, code, description, author, folder, version)
```

#### Complexity Assessment: SIMPLE-MEDIUM
- JFileChooser UI interaction is straightforward
- File I/O operations are standard
- Some Dialog usage for metadata input
- Validation logic for script names

#### Risk Assessment: LOW
- Focused on file operations
- Can be unit tested with mock files
- No complex state mutations

#### Note
Could be combined with script movement operations (see next candidate) into a broader ScriptOperationsManager, but import/export is distinct enough to warrant separation.

---

### CANDIDATE 5: ScriptTransferManager (Drag & Drop + Move)
**Lines:** ~280 lines to extract | **Complexity:** MEDIUM-COMPLEX
**Current Location:** Lines 2606-2728, 4042-4160 (nested in ScriptTreeTransferHandler)

#### Methods to Extract
```java
private void showMoveToFolderDialog(..) // Lines 2606-2664 (58 lines)
private void moveScriptToFolder(...)    // Lines 2683-2728 (45 lines)
private void collectFolderPaths(...)    // Lines 2669-2678 (9 lines)
// Inner class:
private void moveScript(...)            // Lines 4042-4087 (45 lines)
private void moveFolder(...)            // Lines 4092-4160 (68 lines)
private String getFolderPath(...)       // ~10 lines
```

#### Dependencies (Fields needed)
```java
- Python3RestClient restClient
- ScriptTreeNode rootNode
- JTree scriptTree
- ModernStatusBar statusBar
- JPanel (this)
- ScriptMetadata currentScript (for context)
```

#### Complexity Assessment: MEDIUM-COMPLEX
- Handles both drag-drop and UI-based movement
- Complex folder path calculations
- Nested folder support (recursive operations)
- Manages folder and script moves differently

#### Risk Assessment: MEDIUM
- Folder recursive operations require careful testing
- Path manipulation is error-prone
- Multiple SwingWorker patterns

#### Note
This is a good candidate for extraction BUT requires careful testing. The moveFolder() method updates ALL scripts under a folder subtree, which is complex. Could benefit from unit tests.

---

### CANDIDATE 6: UIPreferencesManager
**Lines:** ~200 lines to extract | **Complexity:** MEDIUM
**Current Location:** Lines 2998-3160, 3860-3890

#### Methods to Extract
```java
private void setFontSize(int newSize)           // Lines 3867-3890 (23 lines)
private void changeFontSize(int delta)          // Lines 3860-3862 (2 lines)
private String mapThemeNameToKey(...)           // ~10 lines
private void applyTheme(String themeName)       // Lines 2998-3100 (102 lines)
    ├── updateComponent(Component, Color)       // ~20 lines
    ├── updateButtonTheme(JButton, Color...)    // ~15 lines
    ├── updateTitledBorders(...)                // ~15 lines
    ├── updateScrollPaneTheme(...)              // ~15 lines
    ├── updateSplitPaneDividers(...)            // ~15 lines
```

#### Dependencies (Fields needed)
```java
- String currentTheme
- int fontSize
- boolean useDarkTheme
- RSyntaxTextArea codeEditor
- Preferences prefs
- All UI components (buttons, panels, text areas)
- ModernStatusBar statusBar
```

#### Complexity Assessment: MEDIUM
- Theme application spreads across many components
- Requires recursive component traversal
- Multiple UI update strategies

#### Risk Assessment: MEDIUM
- Already partially handled by ThemeManager (which applies RSyntaxTextArea themes)
- UIPreferencesManager would handle font and generic component theming
- Potential overlap with existing ThemeManager

#### Note
**IMPORTANT:** ThemeManager already exists and handles RSyntaxTextArea themes. Need to clarify:
1. Does ThemeManager handle the full component tree or just editor?
2. Should UIPreferencesManager focus only on font and dialog theming?
3. Or should UIPreferencesManager completely replace ThemeManager's responsibilities?

Recommendation: Review existing ThemeManager before extracting this.

---

### CANDIDATE 7: KeyboardShortcutsManager
**Lines:** ~150 lines to extract | **Complexity:** SIMPLE
**Current Location:** Lines 867-1000+

#### Methods to Extract
```java
private void setupKeyboardShortcuts()  // Lines 867-1000+ (~150 lines)
```

#### Shortcuts Currently Registered
```java
- Ctrl+Enter   → executeCode()
- Ctrl+S       → saveCurrentScript()
- Ctrl+Shift+S → saveScriptAs()
- Ctrl+N       → createNewScript()
- Ctrl++       → changeFontSize(+1)
- Ctrl+-       → changeFontSize(-1)
- Ctrl+0       → setFontSize(12)
- Ctrl+F       → showFindDialog()
- Ctrl+H       → showReplaceDialog()
- Ctrl+Shift+F → showAdvancedFindReplaceDialog()
- Ctrl+Shift+P → showCommandPalette()
- Ctrl+B       → toggleSidebar()
- Ctrl+/       → (likely comment toggle - search for this)
```

#### Dependencies (Fields needed)
```java
- RSyntaxTextArea codeEditor
- Lambda/method references to all action methods
```

#### Complexity Assessment: SIMPLE
- Pure keyboard binding registration
- No logic beyond InputMap/ActionMap setup
- Clear callback-based architecture

#### Risk Assessment: VERY LOW
- Completely isolated
- Easy to test (bind and trigger)
- No state mutations

#### Note
This could be combined with CommandPaletteManager as a unified "CommandRegistry" that handles both palette commands and keyboard shortcuts centrally. That might be better design than separate managers.

---

### CANDIDATE 8: SearchManager
**Lines:** ~100 lines to extract | **Complexity:** SIMPLE
**Current Location:** Lines 3486-3523, plus SearchListenerImpl class

#### Methods to Extract
```java
private void showFindDialog()           // Lines 3486-3494 (8 lines)
private void showReplaceDialog()        // Lines 3499-3507 (8 lines)
private void showAdvancedFindReplaceDialog()  // Lines 3513-3523 (10 lines)
private class SearchListenerImpl implements SearchListener  // ~40 lines
```

#### Dependencies (Fields needed)
```java
- FindDialog findDialog
- ReplaceDialog replaceDialog
- FindReplaceDialog advancedFindReplaceDialog
- RSyntaxTextArea codeEditor
- Frame (from SwingUtilities.getWindowAncestor)
- ModernStatusBar statusBar (for status messages)
```

#### Complexity Assessment: SIMPLE
- Mostly lazy initialization
- SearchListener callbacks are straightforward

#### Risk Assessment: VERY LOW
- Completely isolated
- Works with mature RSyntaxTextArea API
- No state management needed

#### Note
Small enough that it might be combined with KeyboardShortcutsManager or kept separate depending on complexity growth plans.

---

## 3. Extraction Difficulty Assessment

### Easiest to Extract (QUICK WINS)
1. **AutoSaveManager** ⭐⭐⭐⭐⭐
   - Only 3 methods
   - Self-contained
   - No UI layout involved
   - Can be extracted in <30 minutes
   
2. **CommandPaletteManager** ⭐⭐⭐⭐⭐
   - Pure configuration/registration
   - No state management
   - No UI layout
   - Can be extracted in <20 minutes

3. **SearchManager** ⭐⭐⭐⭐
   - Lazy initialization pattern
   - Minimal dependencies
   - Already well-isolated
   - Can be extracted in <20 minutes

### Medium Difficulty
4. **ScriptImportExportManager** ⭐⭐⭐⭐
   - File I/O is straightforward
   - Dialog handling is standard
   - Minor validation logic
   - Can be extracted in <45 minutes

5. **KeyboardShortcutsManager** ⭐⭐⭐
   - Straightforward binding setup
   - Many callbacks (method references)
   - Need to ensure all target methods are accessible
   - Can be extracted in <40 minutes

6. **ExecutionManager** ⭐⭐⭐
   - Core execution logic
   - SwingWorker patterns
   - Terminal and IDE modes
   - Can be extracted in <60 minutes

### Most Complex
7. **ScriptTransferManager** ⭐⭐
   - Recursive folder operations
   - Complex path calculations
   - Multiple movement strategies
   - Need careful testing
   - Can be extracted in <90 minutes

8. **UIPreferencesManager** ⭐⭐
   - Deep component tree traversal
   - Multiple update strategies
   - Potential overlap with existing ThemeManager
   - Can be extracted in <60 minutes (after clarifying ThemeManager role)

---

## 4. Dependencies and Interactions

### Dependency Graph
```
Python3IDE
├── ExecutionManager
│   └── Depends on: restClient, codeEditor, outputArea, statusBar, progressBar
│   └── Called by: Keyboard shortcuts, Command Palette
│
├── AutoSaveManager
│   └── Depends on: restClient, codeEditor, changesTracker, currentScript, statusBar
│   └── Called by: Timer (internal), no external calls during normal operation
│
├── CommandPaletteManager
│   └── Depends on: All action methods (calls into Python3IDE)
│   └── Called by: Keyboard shortcut (Ctrl+Shift+P)
│
├── ScriptImportExportManager
│   └── Depends on: restClient, currentScript, codeEditor, statusBar
│   └── Called by: Toolbar buttons, Command Palette, Keyboard shortcuts
│
├── ScriptTransferManager
│   └── Depends on: restClient, rootNode, scriptTree, statusBar
│   └── Called by: Right-click context menu, Drag & Drop handler
│
├── KeyboardShortcutsManager
│   └── Depends on: All action methods (calls into Python3IDE)
│   └── Called by: Swing framework (key events)
│
├── SearchManager
│   └── Depends on: codeEditor, statusBar, Dialogs (FindDialog, ReplaceDialog)
│   └── Called by: Toolbar, Command Palette, Keyboard shortcuts
│
└── UIPreferencesManager
    └── Depends on: ALL UI components, preferences, ThemeManager
    └── Called by: Theme selection, Font size controls, initialization
    └── ⚠️ OVERLAP with existing ThemeManager
```

### Circular Dependencies
- ✅ None detected - all dependencies flow from Python3IDE to managers
- Managers call back into Python3IDE via method references/callbacks

### Tight Couplings
- ExecutionManager ↔ TerminalPanel (needs refactoring opportunity)
- SearchManager ↔ Find/Replace Dialogs (acceptable - focused responsibility)
- UIPreferencesManager ↔ ThemeManager (potential conflict - needs review)

---

## 5. Recommended Extraction Order

### Phase 1: Quick Wins (2-3 hours total)
1. **AutoSaveManager** ← START HERE
   - Smallest, most isolated
   - Zero risk
   - Quick confidence builder
   - Validates extraction pattern

2. **CommandPaletteManager**
   - Pure configuration
   - Works with AutoSaveManager tests
   - Builds on extraction pattern

3. **SearchManager**
   - Small, well-isolated
   - Completes the "easy wins"

### Phase 2: Core Logic (3-4 hours total)
4. **ScriptImportExportManager**
   - Medium complexity
   - File I/O is testable
   - Reduces Python3IDE by 180 lines

5. **ExecutionManager**
   - Heart of the IDE
   - More complex, needs testing
   - Will reveal state management patterns

### Phase 3: Advanced Features (3-4 hours total)
6. **KeyboardShortcutsManager**
   - Depends on all previous extractions
   - Consolidates keyboard logic
   - Medium complexity

7. **ScriptTransferManager**
   - Most complex logic
   - Needs unit tests
   - Handle last due to complexity

### Phase 4: Polish (2-3 hours total)
8. **UIPreferencesManager** (if ThemeManager is reviewed)
   - Requires careful coordination with ThemeManager
   - Potential refactoring of existing manager
   - Can wait until other managers are stable

---

## 6. Code Metrics: Before and After

### Current State (Python3IDE.java alone)
```
Lines of Code: 4,390
Methods: 80+
Fields: 40+
Classes: 8 (including nested classes)
Cyclomatic Complexity: HIGH (many conditionals)
```

### After Full Extraction (Estimate)
```
Python3IDE.java: ~2,800 lines (-36%)
├── Initialization code: 200 lines
├── UI layout: 400 lines
├── Component assembly: 300 lines
├── Listener registration: 150 lines
├── Orchestration/delegation: 200 lines
├── Utility methods: 100 lines
└── Public API: 50 lines

New Manager Classes: 8
├── AutoSaveManager.java: 100 lines
├── CommandPaletteManager.java: 150 lines
├── SearchManager.java: 80 lines
├── ScriptImportExportManager.java: 200 lines
├── ExecutionManager.java: 280 lines
├── KeyboardShortcutsManager.java: 160 lines
├── ScriptTransferManager.java: 300 lines
└── UIPreferencesManager.java: 250 lines

Total Manager Lines: 1,520 lines
Total Overall Lines: 4,320 lines (similar, but much better organized)
Methods per Class (After): 4-8 (down from 80+ in single class)
```

---

## 7. Risk Analysis and Mitigation

### Risk Level by Extraction

| Manager | Risk | Mitigation |
|---------|------|-----------|
| AutoSaveManager | VERY LOW | Extract first, use as pattern |
| CommandPaletteManager | VERY LOW | Pure configuration, isolated |
| SearchManager | LOW | Well-established API, isolated |
| ScriptImportExportManager | LOW | File I/O is testable with mocks |
| KeyboardShortcutsManager | LOW | Method references are type-safe |
| ExecutionManager | MEDIUM | Needs async testing, terminal complexity |
| ScriptTransferManager | MEDIUM-HIGH | Recursive operations, path calculations |
| UIPreferencesManager | MEDIUM | Potential ThemeManager overlap |

### Testing Strategy
1. **Unit Tests** for managers (especially ScriptTransferManager)
2. **Integration Tests** for keyboard shortcuts + command palette
3. **Manual Testing** for execution (async behavior hard to test)
4. **UI Testing** for drag & drop transfer operations

### Rollback Plan
- Each manager extraction should be a separate git commit
- Can revert individual extractons if issues arise
- Keep original methods in Python3IDE until manager is fully tested

---

## 8. Implementation Patterns to Follow

### Pattern 1: Dependency Injection
```java
// Example: AutoSaveManager
public class AutoSaveManager {
    private final Timer autoSaveTimer;
    private final UnsavedChangesTracker changesTracker;
    private final ScriptMetadata currentScript;
    private final RSyntaxTextArea codeEditor;
    private final Python3RestClient restClient;
    private final Consumer<String> statusCallback;
    
    public AutoSaveManager(
        UnsavedChangesTracker changesTracker,
        RSyntaxTextArea codeEditor,
        Python3RestClient restClient,
        Consumer<String> statusCallback
    ) {
        this.changesTracker = changesTracker;
        this.codeEditor = codeEditor;
        this.restClient = restClient;
        this.statusCallback = statusCallback;
        this.autoSaveTimer = new Timer(AUTO_SAVE_INTERVAL_MS, e -> performAutoSave());
    }
    
    public void initialize() {
        autoSaveTimer.start();
    }
    
    public void shutdown() {
        autoSaveTimer.stop();
    }
}
```

### Pattern 2: Callback Pattern
```java
// Example: ExecutionManager
public class ExecutionManager {
    private final Consumer<ExecutionResult> onSuccess;
    private final Consumer<Exception> onError;
    
    public ExecutionManager(
        Python3RestClient restClient,
        Consumer<ExecutionResult> onSuccess,
        Consumer<Exception> onError
    ) {
        this.onSuccess = onSuccess;
        this.onError = onError;
    }
    
    public void execute(String code) {
        // ... execution logic ...
        onSuccess.accept(result);  // Callback instead of direct UI update
    }
}
```

### Pattern 3: Factory Pattern
```java
// Example: CommandPaletteManager as factory
public class CommandPaletteManager {
    public static CommandPaletteDialog createAndInitialize(
        Frame owner,
        CommandRegistry commandRegistry
    ) {
        CommandPaletteDialog palette = new CommandPaletteDialog(owner);
        commandRegistry.registerAll(palette);
        return palette;
    }
}
```

---

## 9. Specific Method Details

### AutoSaveManager - Complete Method List
```java
- public AutoSaveManager(...)          // Constructor with DI
- public void initialize()             // Start timer
- public void shutdown()               // Stop timer
- private void performAutoSave()       // Main logic (32 lines)
- private void cleanupOldAutosaveFiles()  // Housekeeping (21 lines)
- public void setCurrentScript(ScriptMetadata)  // Notify of script changes
```

### ExecutionManager - Complete Method List
```java
- public ExecutionManager(...)         // Constructor with DI
- public void executeCode(String code) // Execute in IDE or Terminal mode
- public void executeTerminalCommand(String command)  // Terminal-specific
- private void handleSuccess(ExecutionResult)  // Callback
- private void handleError(Exception)   // Error callback
- public void setExecutionMode(boolean isTerminal)  // Switch modes
- public void clearOutput()            // Clear output areas
```

### CommandPaletteManager - Complete Method List
```java
- public static CommandPaletteDialog create(Frame owner, CommandRegistry registry)
- private static void registerExecutionCommands(CommandPaletteDialog)
- private static void registerFileCommands(CommandPaletteDialog)
- private static void registerSearchCommands(CommandPaletteDialog)
- private static void registerViewCommands(CommandPaletteDialog)
- private static void registerThemeCommands(CommandPaletteDialog)
- private static void registerGatewayCommands(CommandPaletteDialog)
- private static void registerSettingsCommands(CommandPaletteDialog)
```

---

## 10. Actionable Recommendations

### Immediate Actions (Next Sprint)
1. ✅ **Extract AutoSaveManager**
   - Smallest, safest
   - Establishes extraction pattern
   - Estimated: 30 minutes

2. ✅ **Extract CommandPaletteManager**
   - Pure configuration
   - Zero business logic risk
   - Estimated: 20 minutes

3. ✅ **Extract SearchManager**
   - Consolidates search UIs
   - Minimal complexity
   - Estimated: 20 minutes

### Short-term Actions (Following Sprint)
4. ✅ **Extract ScriptImportExportManager**
   - Medium complexity
   - Good learning opportunity
   - Estimated: 45 minutes

5. ✅ **Extract ExecutionManager** (Highest Value)
   - Core business logic
   - Reduces Python3IDE complexity significantly
   - Estimated: 60 minutes

### Medium-term Actions
6. ✅ **Extract KeyboardShortcutsManager**
   - Consolidates all keyboard shortcuts
   - Easier once others extracted
   - Estimated: 40 minutes

7. ✅ **Extract ScriptTransferManager** (Highest Risk)
   - Most complex
   - Needs comprehensive testing
   - Estimated: 90 minutes

### Long-term/Conditional
8. ⚠️ **Review and potentially extract UIPreferencesManager**
   - Requires coordination with existing ThemeManager
   - Needs architecture decision first
   - Estimated: 60 minutes (after review)

### Architecture Improvements
- **Consider:** Combining KeyboardShortcutsManager + CommandPaletteManager into unified CommandRegistry
- **Consider:** Creating ExecutionStrategy pattern to handle IDE vs Terminal mode
- **Consider:** Interface-based design for managers to enable mocking in tests

---

## 11. Validation Checklist for Each Extraction

- [ ] All method dependencies identified
- [ ] All field dependencies identified
- [ ] New manager created with dependency injection constructor
- [ ] Old methods marked as delegating to manager
- [ ] Tests added for new manager
- [ ] Keyboard shortcuts still work
- [ ] Command palette still works
- [ ] UI behavior unchanged
- [ ] No new compiler warnings
- [ ] Code review completed
- [ ] Git commit message follows style guide

---

## Summary

**Best Starting Point:** AutoSaveManager
- Smallest, most isolated
- Zero risk of breaking other features
- Validates the extraction pattern
- Quick win to build momentum

**Highest Value:** ExecutionManager
- Core business logic (~250 lines)
- Will significantly reduce Python3IDE complexity
- Unlocks async testing patterns
- Foundation for future terminal enhancements

**Should Not Extract (Yet):** UIPreferencesManager
- Needs clarification with existing ThemeManager
- Review ThemeManager responsibilities first
- Risk of duplicate code

**Estimated Total Refactoring Time:** 6-8 hours spread across 2-3 sprints

