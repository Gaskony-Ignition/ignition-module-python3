# Python3IDE.java - Comprehensive Refactoring Analysis

**File:** `/modules/ignition-module-python3-java/python3-integration/designer/src/main/java/com/inductiveautomation/ignition/examples/python3/designer/Python3IDE.java`

**Current Status:** 4,317 lines, monolithic class
**Class Size:** ~4.3K lines total

---

## Executive Summary

The Python3IDE class is a monolithic 4,317-line UI class that orchestrates the entire Python 3 IDE for the Ignition Designer. It combines UI layout, event handling, theme management, script operations, dialog management, keyboard shortcuts, and auto-save functionality in a single class.

**Key Findings:**
- 8 major functional areas identified
- 125+ methods total (including inner classes and anonymous listeners)
- Moderate tight coupling between themes and UI components
- Several areas can be cleanly extracted with minimal refactoring
- Some circular dependencies through status bar and theme application
- Estimated 60-70% refactoring feasibility

---

## FUNCTIONAL AREA 1: UI Component Initialization

**Line Range:** 207-341
**Type:** Initialization/Setup

### Methods:
- `initComponents()` (lines 207-341) - ~134 lines
  - Creates all UI components (text fields, buttons, editor, tree, etc.)
  - Initializes code editor with syntax highlighting
  - Creates dialog components (lazy-initialized)

### Key Fields Created:
```
gatewayUrlField, connectButton, themeSelector, codeEditor
outputArea, errorArea, statusBar, executeButton
pythonIdeTab, terminalTab, scriptTree, metadataPanel
diagnosticsPanel, currentScriptLabel
```

### Dependencies:
- Direct: ModernTheme, ModernButton, RSyntaxTextArea, UnsavedChangesTracker
- Internal: None (pure initialization)
- External: Java Swing components

### Extraction Complexity: **EASY**

**Rationale:** This is straightforward component creation with no dependencies on other methods. Could be moved to a builder pattern or dedicated factory class.

**Extraction Strategy:**
1. Create `UIComponentFactory` class
2. Move all component creation to static factory methods
3. Return initialized components to caller

**Example Extraction:**
```java
class UIComponentFactory {
    public static RSyntaxTextArea createCodeEditor(int fontSize) { ... }
    public static JTextField createGatewayUrlField(String url) { ... }
    public static ModernStatusBar createStatusBar() { ... }
}
```

---

## FUNCTIONAL AREA 2: UI Layout & Component Assembly

**Line Range:** 346-734
**Type:** Layout/Composition

### Methods:
- `layoutComponents()` (lines 346-451) - ~105 lines
- `createSidebar()` (lines 456-519) - ~63 lines
- `createEditorPanel()` (lines 524-734) - ~210 lines

### Total Lines: ~378 lines

### Key Operations:
1. **layoutComponents():**
   - Creates BorderLayout hierarchy
   - Assembles gateway panel (top)
   - Creates main split pane (sidebar | editor)
   - Adds status bar (bottom)

2. **createSidebar():**
   - Script tree with scroll pane
   - Metadata panel
   - Vertical split pane (tree | metadata)

3. **createEditorPanel():**
   - Code editor with RTextScrollPane
   - Terminal panel
   - Output/Error tabs with CardLayout
   - Diagnostics panel
   - Complex multi-pane layout

### Dependencies:
- Direct: `initComponents()` (requires initialized fields)
- Internal: `createSidebar()`, `createEditorPanel()` called by `layoutComponents()`
- External: JSplitPane, CardLayout, BorderLayout, RTextScrollPane
- Theme: References ModernTheme colors throughout

### Extraction Complexity: **MEDIUM**

**Rationale:** These methods are tightly coupled with initialized fields and theme colors. Can be extracted but require passing initialized components and theme state.

**Extraction Strategy:**
1. Create `LayoutManager` class to handle assembly
2. Pass initialized components via constructor
3. Create separate methods for each panel (already done: `createSidebar()`, `createEditorPanel()`)
4. Extract split pane configuration logic

**Concerns:**
- References to instance variables (mainSplit, sidebarSplit, bottomSplit, centerPanel)
- Theme color dependencies (ModernTheme.*)
- Must maintain field assignments for later access

---

## FUNCTIONAL AREA 3: Keyboard Shortcuts & Input Handling

**Line Range:** 739-937
**Type:** Event Handling

### Methods:
- `attachListeners()` (lines 739-810) - ~71 lines
- `setupKeyboardShortcuts()` (lines 814-937) - ~123 lines

### Total Lines: ~194 lines

### Keyboard Shortcuts Defined:
```
Ctrl+Enter   → executeCode()
Ctrl+S       → saveCurrentScript()
Ctrl+Shift+S → saveScriptAs()
Ctrl+N       → createNewScript()
Ctrl++       → changeFontSize(1)
Ctrl+-       → changeFontSize(-1)
Ctrl+0       → setFontSize(12)
Ctrl+F       → showFindDialog()
Ctrl+H       → showReplaceDialog()
Ctrl+Shift+F → showAdvancedFindReplaceDialog()
Ctrl+Shift+P → showCommandPalette()
Ctrl+B       → toggleSidebar()
```

### Event Listeners:
- Theme selector change → applyTheme()
- Tree selection → onTreeSelectionChanged()
- Tree double-click → loadSelectedScript()
- Tree right-click → showContextMenu()
- Button clicks → various actions
- Gateway URL enter key → connectToGateway()

### Dependencies:
- Direct: None (pure setup, calls other methods)
- Internal: Calls ~15 different action methods
- External: InputMap, ActionMap, KeyStroke

### Extraction Complexity: **EASY**

**Rationale:** Input handling is self-contained and only calls action methods. Can be moved to separate manager with method references.

**Extraction Strategy:**
1. Create `KeyboardShortcutsManager` class
2. Pass action callbacks in constructor (lambda/method references)
3. Provide initialization method: `setupShortcuts(InputMap, ActionMap)`
4. Return shortcut bindings list for reference

**Example Extraction:**
```java
class KeyboardShortcutsManager {
    public void setupKeyboardShortcuts(
        RSyntaxTextArea editor,
        Runnable onExecute,
        Runnable onSave,
        Runnable onFind,
        // ... more callbacks
    ) { ... }
}
```

---

## FUNCTIONAL AREA 4: Script Management (CRUD Operations)

**Line Range:** 1558-2710
**Type:** Business Logic / Script Operations

### Methods:
- `refreshScriptTree()` (lines 1558-1582) - ~24 lines
- `buildScriptTree()` (lines 1588-1636) - ~48 lines
- `getOrCreateFolder()` (lines 1641-1673) - ~32 lines
- `onTreeSelectionChanged()` (lines 1678-1700) - ~22 lines
- `loadSelectedScript()` (lines 1702-1742) - ~40 lines
- `loadScript()` (lines 1743-1784) - ~41 lines
- `saveCurrentScript()` (lines 1785-1819) - ~34 lines
- `saveScriptAs()` (lines 1824-1847) - ~23 lines
- `showSaveDialog()` (lines 1854-1892) - ~38 lines
- `saveScript()` (lines 1897-1936) - ~39 lines
- `createNewFolder()` (lines 1941-1966) - ~25 lines
- `createNewScript()` (lines 1971-1989) - ~18 lines
- `showContextMenu()` (lines 1994-2070) - ~76 lines
- `exportScript()` (lines 2070-2117) - ~47 lines
- `renameScript()` (lines 2117-2201) - ~84 lines
- `editMetadata()` (lines 2201-2422) - ~221 lines (LARGE)
- `renameFolder()` (lines 2422-2514) - ~92 lines
- `getFolderPathForNode()` (lines 2514-2536) - ~22 lines
- `showMoveToFolderDialog()` (lines 2536-2599) - ~63 lines
- `collectFolderPaths()` (lines 2599-2613) - ~14 lines
- `moveScriptToFolder()` (lines 2613-2663) - ~50 lines
- `deleteScript()` (lines 2663-2710) - ~47 lines
- `importScript()` (lines 2710-2794) - ~84 lines
- `exportCurrentScript()` (lines 2794-2843) - ~49 lines

### Total Lines: ~1,152 lines (26.6% of class!)

### Core Script Operations:
```
Load Script
Save Script / Save As
Create Script / Create Folder
Delete Script / Delete Folder
Rename Script / Rename Folder
Move Script to Folder
Import Script from File
Export Script to File
Refresh Script Tree
```

### Dependencies:
- **Gateway:** restClient (for API calls)
- **UI State:** currentScript, codeEditor, changesTracker
- **UI Updates:** updateCurrentScriptLabel(), metadataPanel, statusBar, treeModel
- **Dialogs:** DarkDialog, showSaveDialog(), showMoveToFolderDialog()
- **SwingWorker:** Multiple async operations (loadScript, saveScript, deleteScript, etc.)

### Extraction Complexity: **HARD**

**Rationale:** These operations are tightly coupled to UI state (currentScript, codeEditor content) and require callbacks to update UI. Complex metadata editing dialog with multiple fields. Heavy async operations with SwingWorker.

**Potential Extraction Issues:**
1. **Tight UI Coupling:** Each operation updates currentScript, codeEditor, statusBar, metadataPanel
2. **Circular State:** Changes tracked by changesTracker, updated by saveScript()
3. **Complex Dialog:** editMetadata() is 221 lines with 11 input fields
4. **Async Complexity:** Multiple SwingWorker instances with error handling
5. **Tree Operations:** buildScriptTree(), getOrCreateFolder() have tree-specific logic

**Extraction Strategy:**
1. Extract `ScriptOperationsManager` for API interactions only
2. Keep UI orchestration in Python3IDE (would be "thin" wrapper)
3. Use callback pattern for UI updates
4. Extract editMetadata() dialog to separate class
5. Use Observable pattern for script selection/loading

**Extraction Barriers:**
- currentScript field (shared state)
- codeEditor.setText() calls
- Tree manipulation (treeModel.reload(), treeModel.add())
- Frequent statusBar.setStatus() calls

---

## FUNCTIONAL AREA 5: Code Execution & Terminal

**Line Range:** 1014-1318
**Type:** Execution / Output Handling

### Methods:
- `executeCode()` (lines 1014-1114) - ~100 lines
- `handleSuccess()` (lines 1119-1137) - ~18 lines
- `handleError()` (lines 1142-1150) - ~8 lines
- `onModeTabChanged()` (lines 1158-1222) - ~64 lines
- `clearOutput()` (lines 1227-1230) - ~3 lines
- `executeTerminalCommand()` (lines 1237-1275) - ~38 lines
- `updateTerminalWorkingDirectory()` (lines 1276-1317) - ~41 lines
- `refreshDiagnostics()` (lines 1318-1352) - ~34 lines
- `refreshPythonVersion()` (lines 1353-1397) - ~44 lines
- `updateAutocompleteStatus()` (lines 1398-1423) - ~25 lines
- `handlePoolClicked()` (lines 1424-1505) - ~81 lines

### Total Lines: ~456 lines (10.5% of class)

### Key Operations:
1. **executeCode():** Main execution dispatcher
   - Python mode: Uses Python3ExecutionWorker
   - Terminal mode: Uses interactive shell session
   - Manages progress bar and execution state

2. **onModeTabChanged():** Switches between editor and terminal
   - Creates/destroys interactive shell sessions
   - Updates UI labels and panels

3. **handleSuccess/handleError:** Result handling callbacks

4. **Terminal Operations:**
   - executeTerminalCommand()
   - updateTerminalWorkingDirectory()
   - Terminal history tracking (terminalHistory field)

### Dependencies:
- **Gateway:** restClient for execution
- **UI Output:** outputArea, errorArea, progressBar
- **State:** currentWorker, interactiveShellSessionId, terminalHistory
- **Dialogs:** Via handlePoolClicked() → pool size adjustment dialog
- **Panels:** terminalPanel, diagnosticsPanel, statusBar

### Extraction Complexity: **MEDIUM**

**Rationale:** Execution logic is somewhat self-contained but needs UI update callbacks. Terminal operations create complexity with session management.

**Extraction Strategy:**
1. Create `ExecutionManager` for Python code execution
2. Create separate `TerminalSessionManager` for shell sessions
3. Use callbacks for result handling and status updates
4. Keep pool interaction in separate method

**Concerns:**
- SwingWorker integration (currentWorker field)
- Session management (interactiveShellSessionId)
- Terminal history tracking
- Progress bar management

---

## FUNCTIONAL AREA 6: Theme Management & Styling

**Line Range:** 2904-3500
**Type:** Theme/Styling

### Methods:
- `mapThemeNameToKey()` (lines 2904-2927) - ~23 lines
- `applyTheme()` (lines 2928-3123) - ~195 lines (LARGE)
- `stylePopupMenu()` (lines 3136-3181) - ~45 lines
- `setComponentsDark()` (lines 3181-3212) - ~31 lines
- `updateComponent()` (lines 3212-3232) - ~20 lines
- `updateButtonTheme()` (lines 3232-3248) - ~16 lines
- `createThemedDialogButton()` (lines 3249-3286) - ~37 lines
- `updateTitledBorders()` (lines 3286-3335) - ~49 lines
- `updateScrollPaneTheme()` (lines 3336-3391) - ~55 lines
- `updateSplitPaneDividers()` (lines 3391-3416) - ~25 lines
- `applyTransparentScrollBar()` (lines 4100-4170) - ~70 lines (inner class UI delegate)

### Total Lines: ~566 lines (13.1% of class)

### Theme Names Supported:
```
dark, monokai, eclipse, idea, vs, druid, default/light
```

### Core Styling Operations:
```
Apply RSyntaxTextArea theme
Update text area colors (foreground, background, caret)
Update button colors (primary, success, default)
Update tree colors
Update split pane dividers
Update scroll pane appearance
Update titled borders
Update dialog theme (via DarkDialog)
```

### Dependencies:
- **Internal Fields:** Uses almost all UI component fields
- **UI Components:** codeEditor, outputArea, errorArea, buttons, tree, split panes, scroll panes
- **External:** Theme (RSyntaxTextArea), ModernTheme color constants, DarkDialog
- **Theme State:** useDarkTheme boolean

### Extraction Complexity: **HARD**

**Rationale:** Extremely tightly coupled to UI structure. applyTheme() method (195 lines) touches 20+ different component types. Theme state (useDarkTheme) is referenced throughout code.

**Potential Extraction Issues:**
1. **Pervasive Dependencies:** Updates components created in initComponents()
2. **Theme State:** useDarkTheme boolean used for conditional styling
3. **Component References:** Direct field access to buttons, text areas, tree, panels
4. **Ripple Effects:** Theme changes need to propagate to dialogs, popups, terminal panel
5. **Component Type Variations:** Different update logic for buttons, borders, scroll panes, split panes

**Extraction Strategy:**
1. Create `ThemeManager` class to hold theme state and styling logic
2. Register components with theme manager (observer pattern)
3. Components self-register for theme updates: `themeManager.register(component, ThemeType.BUTTON)`
4. Pass component references in batch (Map<String, Component>)
5. Create component-specific "themers": ButtonThemer, BorderThemer, ScrollPaneThemer

**Extraction Barriers:**
- applyTheme() method is 195 lines with 40+ statements
- SwingUtilities.updateComponentTreeUI(this) needs component hierarchy
- terminalPanel theme update (created in createEditorPanel)
- Dialog theme integration (DarkDialog.setDarkTheme)

---

## FUNCTIONAL AREA 7: Dialogs & User Input

**Line Range:** 3416-4209
**Type:** Dialog Management / User Interaction

### Methods:
- `showFindDialog()` (lines 3416-3428) - ~12 lines
- `showReplaceDialog()` (lines 3429-3442) - ~13 lines
- `showAdvancedFindReplaceDialog()` (lines 3443-3458) - ~15 lines
- `showCommandPalette()` (lines 3459-3471) - ~12 lines
- `toggleSidebar()` (lines 3472-3490) - ~18 lines
- `showInformationDialog()` (lines 4169-4178) - ~9 lines
- `openInfoDialog()` (lines 4179-4188) - ~9 lines
- `openPackagesDialog()` (lines 4189-4198) - ~9 lines
- `openSettingsDialog()` (lines 4199-4208) - ~9 lines
- `showSaveDialog()` (lines 1854-1892) - ~38 lines (analyzed in AREA 4)
- `showMoveToFolderDialog()` (lines 2536-2599) - ~63 lines (analyzed in AREA 4)
- `showInvalidNameError()` (lines 1545-1557) - ~12 lines (analyzed in AREA 4)
- `isValidName()` (lines 1528-1544) - ~16 lines
- `showUnsavedChangesDialog()` (lines 2843-2855) - ~12 lines
- `onDirtyStateChanged()` (lines 2856-2873) - ~17 lines
- `setStatus()` (lines 1506-1527) - ~21 lines

### Total Lines: ~304 lines (7.0% of class)

### Dialog Types:
```
Find/Replace dialogs (lazy-initialized, from RSyntaxTextArea library)
Command Palette (v2.8.0 feature)
Settings Dialog (v2.7.0 feature)
Info Dialog (module/Python version info)
Packages Dialog (pip package management - v2.7.0)
Save Dialog (multi-field input for metadata)
Move Folder Dialog (folder selection)
Unsaved Changes warning
Invalid Name error
```

### Dependencies:
- **Gateway:** Some dialogs trigger connectToGateway()
- **State:** currentScript, codeEditor, changesTracker
- **Components:** findDialog, replaceDialog, advancedFindReplaceDialog, commandPalette
- **External:** FindDialog, ReplaceDialog, FindReplaceDialog, CommandPaletteDialog, SettingsDialog, DarkDialog

### Extraction Complexity: **EASY**

**Rationale:** Most dialog creation is minimal (just instantiation and show()). Dialog content/logic is in separate classes (FindReplaceDialog, SettingsDialog, etc.). Status message setting is simple.

**Extraction Strategy:**
1. Create `DialogManager` to handle dialog instantiation
2. Keep only UI-side callbacks in Python3IDE
3. Move dialog lazy initialization to DialogManager

**Low Risk:** Dialog classes are already separated; main IDE just calls them.

---

## FUNCTIONAL AREA 8: Auto-Save & Settings Management

**Line Range:** 3491-4315
**Type:** Auto-Save / Settings / Configuration

### Methods:
- `initializeAutoSave()` (lines 3491-3495) - ~4 lines
- `performAutoSave()` (lines 3500-3532) - ~32 lines
- `cleanupOldAutosaveFiles()` (lines 3537-3558) - ~21 lines
- `initializeCommandPalette()` (lines 3563-3744) - ~181 lines (LARGE)
- `changeFontSize()` (lines 3786-3788) - ~2 lines
- `setFontSize()` (lines 3793-3806) - ~13 lines
- `detectGatewayUrl()` (lines 4220-4251) - ~31 lines
- `getDetectedGatewayUrl()` (lines 4258-4260) - ~2 lines
- `getEffectiveGatewayUrl()` (lines 4267-4269) - ~2 lines
- `getRestClient()` (lines 4276-4278) - ~2 lines
- `reloadSettingsFromPreferences()` (lines 4284-4315) - ~31 lines

### Total Lines: ~321 lines (7.4% of class)

### Key Features:
```
Auto-save every 30 seconds (if dirty + connected)
Cleanup old autosave files (keep last 5)
Font size management with preferences
Gateway URL detection/override
Command Palette initialization with 20+ commands
Settings reload from preferences
```

### Dependencies:
- **Preferences:** Java Preferences API for persistence
- **File I/O:** Auto-save directory (~/.python3ide/autosave)
- **State:** currentScript, fontSize, currentTheme, changesTracker
- **Components:** codeEditor, statusBar
- **Dialog:** CommandPaletteDialog (initialized separately)
- **Callbacks:** All 20+ command palette commands reference IDE methods

### Extraction Complexity: **MEDIUM**

**Rationale:** Auto-save is self-contained, but Command Palette initialization (181 lines) requires extensive callbacks. Gateway URL detection is pure logic but needs integration.

**Extraction Strategy:**
1. Create `AutoSaveManager` for auto-save functionality
   - Timer management
   - File I/O
   - Cleanup logic

2. Create `CommandPaletteBuilder` for command registration
   - Takes IDE instance and builds command list
   - Returns initialized CommandPaletteDialog

3. Keep font size in `SettingsManager` (with preferences)

4. Move gateway detection to `GatewayDetector` utility class

**Concerns:**
- AutoSave needs reference to currentScript and changesTracker
- Command Palette needs method references (this::executeCode, etc.)
- Settings reload updates multiple fields (currentTheme, fontSize)

---

## FUNCTIONAL AREA 9: Search & Navigation

**Line Range:** 3416-3443 (dialog methods)
**Type:** Find/Replace/Search

### Methods:
- `showFindDialog()` (lines 3416-3428) - ~12 lines
- `showReplaceDialog()` (lines 3429-3442) - ~13 lines
- `showAdvancedFindReplaceDialog()` (lines 3443-3458) - ~15 lines

### Inner Class: SearchListener Implementation (lines 3744-3777)
```
searchEvent() - Handles RSyntaxTextArea search events
getSelectedText() - Called by search dialogs
```

### Total Lines: ~50 lines (1.2% of class)

### Dependencies:
- **Components:** codeEditor (RSyntaxTextArea)
- **Dialogs:** FindDialog, ReplaceDialog, FindReplaceDialog (from RSyntaxTextArea library)
- **External:** SearchListener, SearchEvent

### Extraction Complexity: **EASY**

**Rationale:** Find/Replace functionality is delegated to RSyntaxTextArea library. IDE just creates dialogs and provides search listener. Very minimal custom logic.

**Extraction Strategy:**
1. Move SearchListener inner class to separate file
2. Create simple wrapper for dialog creation
3. Most logic already in RSyntaxTextArea library

---

## Drag & Drop Transfer Handler

**Line Range:** 3841-4071
**Type:** Drag & Drop / Advanced Feature

### Inner Class: ScriptTreeTransferHandler
- `getSourceActions()` (lines 3843-3845)
- `createTransferable()` (lines 3848-3866)
- `canImport()` (lines 3869-3893)
- `importData()` (lines 3893-4046)
  - Contains inner class ScriptTransferable
  - Contains inner methods: moveScript(), moveFolder()

### Total Lines: ~230 lines (5.3% of class)

### Key Operations:
```
Drag scripts / folders from tree
Drop on target folder
Move script to new folder
Move folder under parent
```

### Dependencies:
- **Tree:** scriptTree, scriptTree.getSelectionPath()
- **API:** restClient for moving scripts/folders
- **UI Update:** Callbacks for tree manipulation

### Extraction Complexity: **HARD**

**Rationale:** Transfer handler requires deep tree knowledge and folder path logic. Must coordinate with script movement API calls and UI updates.

**Extraction Strategy:**
1. Create `ScriptTreeTransferHandler` as separate top-level class
2. Pass restClient and tree reference in constructor
3. Provide callback for tree rebuild after drop

**Concerns:**
- Needs folder path logic (getFolderPathForNode, collectFolderPaths)
- Async operations (moveScript, moveFolder via SwingWorker)
- Tree model updates must happen after successful API call

---

## Critical State & Tight Couplings

### Shared State Fields (Class-level):

| Field | Used By | Risk Level |
|-------|---------|-----------|
| `currentScript` | Script ops, execution, UI labels | CRITICAL |
| `codeEditor` | Execution, save, export, theme | CRITICAL |
| `restClient` | All operations needing Gateway | CRITICAL |
| `changesTracker` | Save, load, dirty state | HIGH |
| `currentTheme` | Theme application, preferences | HIGH |
| `fontSize` | Font size ops, preferences, theme | HIGH |
| `useDarkTheme` | Theme application, popup styling | HIGH |
| `interactiveShellSessionId` | Terminal execution, mode switching | MEDIUM |
| `terminalHistory` | Terminal mode, history display | MEDIUM |
| `sidebarCollapsed` | Sidebar toggle, sidebar state | MEDIUM |
| `autoSaveTimer` | Auto-save initialization | LOW |

### Circular Dependencies:

```
applyTheme() modifies currentTheme
  → Calls stylePopupMenu()
    → Checks useDarkTheme
      → Which was set in applyTheme()

saveScript() modifies currentScript
  → Calls refreshScriptTree()
    → Which rebuilds tree from API
      → May reload currentScript
        → Updates metadata panel
          → Which references currentScript

connectToGateway() creates restClient
  → enablementDependency for ALL operations
    → Can be called from multiple places
      → Can cause race conditions if concurrent
```

### Callback Hell Risk:

```
Multiple callbacks chain operations:
saveCurrentScript()
  → saveScript() (SwingWorker)
    → done() calls refreshScriptTree()
      → refreshScriptTree() (SwingWorker)
        → done() calls buildScriptTree()
          → References currentScript
          → Updates metadataPanel
```

---

## Extraction Difficulty Matrix

| Functional Area | Lines | Complexity | Extractable | Risk | Priority |
|-----------------|-------|-----------|-----------|------|----------|
| UI Component Init | 134 | Easy | 95% | Low | Medium |
| UI Layout | 378 | Medium | 60% | Medium | Medium |
| Keyboard Shortcuts | 194 | Easy | 90% | Low | Low |
| Script Management | 1,152 | Hard | 40% | High | High |
| Code Execution | 456 | Medium | 70% | Medium | High |
| Theme Management | 566 | Hard | 50% | High | High |
| Dialogs | 304 | Easy | 85% | Low | Low |
| Auto-Save/Settings | 321 | Medium | 75% | Medium | Medium |
| Find/Replace | 50 | Easy | 95% | Low | Low |
| Drag & Drop | 230 | Hard | 60% | Medium | Low |

**Grand Total: 3,785 lines** (87.6% of class)
**Remaining:** 532 lines (constructor, utility methods, getter methods)

---

## Recommended Extraction Roadmap

### Phase 1: EASY WINS (Low Risk, Quick Wins)
**Estimated: 2-3 days**

1. **UIComponentFactory** (134 lines)
   - Extract: initComponents()
   - Create factory methods for each component type
   - Risk: None (pure creation, no state)

2. **KeyboardShortcutsManager** (194 lines)
   - Extract: setupKeyboardShortcuts() + attachListeners() for keyboard
   - Pass IDE callbacks as method references
   - Risk: Low (isolated input mapping)

3. **DialogManager** (50 lines for simple wrapper)
   - Extract: Dialog creation methods
   - Lazy initialization already in place
   - Risk: Low (thin wrapper)

### Phase 2: MEDIUM EXTRACTION (Medium Risk, Medium Effort)
**Estimated: 4-5 days**

1. **LayoutManager** (378 lines)
   - Extract: layoutComponents(), createSidebar(), createEditorPanel()
   - Requires passing initialized components
   - Return assembled panel hierarchy
   - Risk: Medium (field assignment, theme integration)

2. **ExecutionManager** (300 lines)
   - Extract: executeCode(), handleSuccess/Error, output handling
   - Keep terminal operations separate
   - Use callbacks for result handling
   - Risk: Medium (SwingWorker, session management)

3. **AutoSaveManager** (50 lines)
   - Extract: Auto-save timer and file operations
   - Keep UI callbacks minimal
   - Risk: Low (self-contained)

### Phase 3: HARD EXTRACTION (High Risk, High Effort, High Value)
**Estimated: 6-8 days**

1. **ScriptOperationsManager** (600+ lines)
   - Extract: All CRUD operations on scripts
   - Handle API interaction, async operations
   - Use callback pattern for UI updates
   - Split into: ScriptLoader, ScriptSaver, ScriptDeleter
   - Risk: High (complex state management)

2. **ThemeManager** (566 lines)
   - Extract: Theme application and component styling
   - Use observer/listener pattern
   - Register components for theme updates
   - Risk: High (pervasive UI coupling)

3. **TerminalManager** (200 lines)
   - Extract: Terminal session management, mode switching
   - Keep session lifecycle separate from execution
   - Risk: Medium (session state complexity)

### Phase 4: ARCHITECTURAL REFACTORING (Major Effort, Long-term)
**Estimated: 10+ days**

1. **MVC/MVP Pattern**
   - Python3IDE as View/Presenter (thin)
   - Create Model layer: ScriptRepository, ExecutionService
   - Create separate Controllers: ScriptController, ExecutionController
   - Risk: Very High (requires restructuring)

2. **Observer/Listener Pattern**
   - Replace direct field updates with event notifications
   - Components listen to: script changes, execution results, theme changes
   - Decouple UI from business logic
   - Risk: Very High (architectural change)

3. **Component Tree Refactoring**
   - Extract each major panel to separate class
   - SidebarPanel, EditorPanel, OutputPanel, StatusBar
   - Compose in Python3IDE as container
   - Risk: High (requires interface definitions)

---

## Extraction Sequence (Recommended)

**For Maximum Impact with Minimum Risk:**

1. **Week 1:** UIComponentFactory + KeyboardShortcutsManager (easy wins)
   - Result: ~330 lines extracted, class reduced to 4,000 lines
   - Risk: Minimal
   - Effort: Low

2. **Week 2:** LayoutManager + AutoSaveManager
   - Result: ~430 lines extracted, class reduced to 3,570 lines
   - Risk: Low-Medium
   - Effort: Medium

3. **Week 3:** ExecutionManager + DialogManager
   - Result: ~350 lines extracted, class reduced to 3,220 lines
   - Risk: Medium
   - Effort: Medium

4. **Weeks 4-5:** ThemeManager (requires most refactoring)
   - Result: ~560 lines extracted, class reduced to 2,660 lines
   - Risk: High
   - Effort: High

5. **Weeks 6-7:** ScriptOperationsManager (largest, most complex)
   - Result: ~1,150 lines extracted, class reduced to 1,510 lines
   - Risk: High
   - Effort: High

**Final Result:** Python3IDE reduced from 4,317 to ~1,500-2,000 lines
- Remaining: Constructor, composition logic, thin callbacks, public accessors
- Total effort: 5-6 weeks for complete refactoring

---

## Key Refactoring Patterns to Use

### 1. Callback Pattern for UI Updates
```java
// Manager takes callbacks for UI operations
public class ScriptOperationsManager {
    private final Runnable onScriptLoaded;
    private final Consumer<String> onStatusUpdate;
    
    public ScriptOperationsManager(
        Runnable onScriptLoaded,
        Consumer<String> onStatusUpdate
    ) {
        this.onScriptLoaded = onScriptLoaded;
        this.onStatusUpdate = onStatusUpdate;
    }
    
    public void loadScript(String name) {
        // ... API call
        onStatusUpdate.accept("Script loaded");
        onScriptLoaded.run();
    }
}
```

### 2. Observer Pattern for Theme Changes
```java
// Components register for theme updates
public interface ThemeObserver {
    void onThemeChanged(Theme theme);
}

public class ThemeManager {
    private List<ThemeObserver> observers = new ArrayList<>();
    
    public void register(ThemeObserver observer) {
        observers.add(observer);
    }
    
    public void applyTheme(Theme theme) {
        observers.forEach(obs -> obs.onThemeChanged(theme));
    }
}
```

### 3. Builder Pattern for Complex Dialogs
```java
// Replace large dialog initialization with builder
public class SaveScriptDialogBuilder {
    public SaveScriptDialog build(ScriptMetadata metadata) {
        SaveScriptDialog dialog = new SaveScriptDialog();
        dialog.setName(metadata.getName());
        dialog.setAuthor(metadata.getAuthor());
        // ... other fields
        return dialog;
    }
}
```

### 4. Manager Composition
```java
// Central manager that composes sub-managers
public class IDEManager {
    private final ScriptManager scripts;
    private final ExecutionManager execution;
    private final ThemeManager themes;
    private final KeyboardManager keyboard;
    
    public IDEManager(...) {
        this.scripts = new ScriptManager(...);
        this.execution = new ExecutionManager(...);
        // ...
    }
}
```

---

## Testing Strategy Post-Refactoring

### Unit Tests Required:
1. **KeyboardShortcutsManager** - Test keybinding registration
2. **UIComponentFactory** - Test component creation and defaults
3. **ScriptOperationsManager** - Test CRUD operations (mock API)
4. **ThemeManager** - Test theme application (verify color updates)
5. **ExecutionManager** - Test execution flow (mock restClient)
6. **AutoSaveManager** - Test file I/O and cleanup

### Integration Tests Required:
1. Full IDE initialization
2. Script load → edit → save → execute flow
3. Theme change propagation to all components
4. Keyboard shortcut → action execution
5. Drag & drop script movements
6. Terminal mode switching

### Manual Testing Checklist:
- [ ] All keyboard shortcuts function
- [ ] Theme changes apply to all components
- [ ] Script save/load/delete/export/import work
- [ ] Execution produces correct output
- [ ] Terminal mode creates/closes sessions properly
- [ ] Auto-save files created in temp directory
- [ ] Settings dialog changes persist
- [ ] Drag & drop scripts/folders works

---

## File Organization After Refactoring

```
designer/src/main/java/com/inductiveautomation/ignition/examples/python3/designer/

Current:
├── Python3IDE.java                    (4,317 lines) ← MONOLITH

Post-Refactoring:
├── Python3IDE.java                    (~1,500 lines) ← THIN ORCHESTRATOR
├── managers/
│   ├── ScriptOperationsManager.java   (500+ lines)
│   ├── ExecutionManager.java          (250+ lines)
│   ├── ThemeManager.java              (300+ lines)
│   ├── AutoSaveManager.java           (60 lines)
│   ├── TerminalSessionManager.java    (150 lines)
│   └── SettingsManager.java           (100 lines)
├── ui/
│   ├── UIComponentFactory.java        (150 lines)
│   ├── LayoutManager.java             (250 lines)
│   ├── DialogManager.java             (80 lines)
│   ├── KeyboardShortcutsManager.java  (120 lines)
│   └── ScriptTreeTransferHandler.java (200 lines)
├── dialogs/
│   ├── SaveScriptDialog.java          (200 lines)
│   ├── SettingsDialog.java            (existing)
│   └── EditMetadataDialog.java        (250 lines) ← EXTRACTED from Python3IDE
└── search/
    └── SearchListener.java            (50 lines) ← EXTRACTED from Python3IDE
```

---

## Conclusion

The Python3IDE class is a well-functional but tightly coupled monolith that can be successfully refactored into ~8-10 smaller, specialized manager classes.

**Best approach:**
1. Extract easy wins first (UIComponentFactory, KeyboardShortcutsManager)
2. Progress to medium-difficulty extractions (LayoutManager, ExecutionManager)
3. Reserve hard extractions (ScriptOperationsManager, ThemeManager) for experienced developers
4. Use callback/observer patterns to maintain loose coupling
5. Maintain backward compatibility at the public interface

**Risk mitigation:**
- Maintain Python3IDE as thin orchestrator/facade
- Use Java interfaces for extracted managers
- Comprehensive unit tests before extracting each manager
- Feature flags for gradual rollout of refactored code

**Estimated total effort:** 5-6 weeks for experienced Java developers
**Difficulty:** Medium (high complexity, but well-isolated areas)
**Maintenance impact:** Highly positive (each manager <400 lines, single responsibility)

