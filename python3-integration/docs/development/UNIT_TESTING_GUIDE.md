# Unit Testing Guide

**Version:** 2.11.3
**Last Updated:** 2025-10-28

## Overview

This guide provides instructions for writing and running unit tests for the Python 3 Integration module.

## Current Test Coverage

**Status:** Basic smoke tests implemented (v2.11.1)
- **Test Count:** 9 smoke tests
- **Coverage:** Manager class existence and structure validation
- **Framework:** JUnit 5
- **Location:** `designer/src/test/java/`

### Existing Tests

**ManagerSmokeTest.java** - Validates all 7 manager classes:
- Class loading verification
- Public constructor validation
- Package structure validation

## Test Categories

### 1. Smoke Tests (✅ Implemented)
**Purpose:** Verify basic class structure and compilation

**Example:**
```java
@Test
void testAutoSaveManagerClassExists() {
    assertDoesNotThrow(() -> {
        Class<?> clazz = Class.forName(
            "com.inductiveautomation.ignition.examples.python3.designer.managers.AutoSaveManager"
        );
        assertNotNull(clazz);
    });
}
```

### 2. Unit Tests (⏳ To Be Implemented)
**Purpose:** Test individual methods in isolation

**Priority Components:**
1. **ThemeManager** - Theme application logic
2. **ScriptManager** - Script CRUD operations (requires mocking)
3. **AutoSaveManager** - File management logic
4. **ExecutionManager** - Execution flow (requires mocking)

**Example Template:**
```java
package com.inductiveautomation.ignition.examples.python3.designer.managers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class ThemeManagerTest {

    private ThemeManager themeManager;

    @BeforeEach
    void setUp() {
        themeManager = new ThemeManager();
    }

    @Test
    @DisplayName("Should return available themes")
    void testGetAvailableThemes() {
        List<String> themes = themeManager.getAvailableThemes();
        assertNotNull(themes);
        assertTrue(themes.contains("Dark"));
        assertTrue(themes.contains("Light"));
    }

    @Test
    @DisplayName("Should map theme name to key correctly")
    void testMapThemeNameToKey() {
        assertEquals("dark", themeManager.mapThemeNameToKey("Dark"));
        assertEquals("light", themeManager.mapThemeNameToKey("Light"));
    }
}
```

### 3. Integration Tests (⏳ To Be Implemented)
**Purpose:** Test interactions between components

**Example Scenarios:**
- Script save → Load → Verify content
- Connection → Execute code → Verify result
- Theme change → Apply → Verify UI update

### 4. UI Tests (⏳ To Be Implemented)
**Purpose:** Test Swing components

**Challenges:**
- Requires headless testing setup
- Mock DesignerContext needed
- EDT thread handling

**Framework Options:**
- AssertJ Swing (recommended)
- Fest-Swing (deprecated but stable)

## Running Tests

### Command Line

**Run all tests:**
```bash
cd python3-integration
./gradlew test
```

**Run specific test class:**
```bash
./gradlew test --tests ManagerSmokeTest
```

**Run with detailed output:**
```bash
./gradlew test --info
```

**Generate coverage report:**
```bash
./gradlew test jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html
```

### IDE Integration

**IntelliJ IDEA:**
1. Right-click on test class → Run 'TestClassName'
2. View → Tool Windows → Run to see results
3. Enable coverage: Run → Run with Coverage

**Eclipse:**
1. Right-click on test class → Run As → JUnit Test
2. Window → Show View → JUnit for results

## Test Structure

### Directory Layout
```
designer/src/
├── main/java/
│   └── com/inductiveautomation/.../designer/
│       ├── managers/              # Production code
│       └── Python3IDE.java
└── test/java/
    └── com/inductiveautomation/.../designer/
        └── managers/              # Test code
            ├── ManagerSmokeTest.java       (✅ Exists)
            ├── ThemeManagerTest.java       (⏳ TODO)
            ├── AutoSaveManagerTest.java    (⏳ TODO)
            └── ExecutionManagerTest.java   (⏳ TODO)
```

### Naming Conventions

- **Test class:** `<ClassName>Test.java`
- **Test method:** `test<MethodName>_<Scenario>_<ExpectedResult>()`
- **Display name:** Use `@DisplayName` for readable descriptions

**Examples:**
```java
@Test
@DisplayName("Should throw exception when file not found")
void testLoadScript_FileNotFound_ThrowsException() {
    // Test implementation
}

@Test
@DisplayName("Should return empty list when no scripts exist")
void testListScripts_NoScripts_ReturnsEmptyList() {
    // Test implementation
}
```

## Mocking Dependencies

Many components require mocking for isolation testing.

### Common Dependencies to Mock

1. **Python3RestClient** - Network calls
2. **DesignerContext** - IDE context
3. **File I/O** - File system operations
4. **SwingWorker** - Background operations

### Mockito Setup

Add to `designer/build.gradle.kts`:
```kotlin
dependencies {
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.3.1")
}
```

**Example with Mockito:**
```java
import static org.mockito.Mockito.*;

class ScriptManagerTest {

    @Mock
    private Python3RestClient mockClient;

    private ScriptManager scriptManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scriptManager = new ScriptManager(mockClient);
    }

    @Test
    void testSaveScript_Success() throws Exception {
        // Arrange
        when(mockClient.saveScript(any())).thenReturn(true);

        // Act
        boolean result = scriptManager.saveScript(
            "test", "print('hello')", "desc", "author", "/", "1.0"
        );

        // Assert
        assertTrue(result);
        verify(mockClient, times(1)).saveScript(any());
    }
}
```

## Test Data Management

### Test Fixtures
Create reusable test data:

```java
class TestFixtures {
    public static SavedScript createTestScript() {
        return new SavedScript(
            "test_script",
            "print('Hello, World!')",
            "Test script",
            "test_author",
            "/test",
            "1.0",
            System.currentTimeMillis()
        );
    }
}
```

### Temporary Files
Use JUnit's temporary directory:

```java
@Test
void testAutoSave(@TempDir Path tempDir) {
    AutoSaveManager manager = new AutoSaveManager(tempDir.toString());
    // Test with temporary directory
}
```

## Best Practices

### 1. Test Independence
- Each test should be independent
- Use `@BeforeEach` for setup
- Use `@AfterEach` for cleanup
- No shared state between tests

### 2. Arrange-Act-Assert Pattern
```java
@Test
void testExample() {
    // Arrange - Set up test data and mocks
    ScriptManager manager = new ScriptManager(mockClient);
    String testCode = "print('test')";

    // Act - Execute the method being tested
    boolean result = manager.saveScript("name", testCode, ...);

    // Assert - Verify the results
    assertTrue(result);
    verify(mockClient).saveScript(any());
}
```

### 3. Test One Thing
Each test should verify one specific behavior:

```java
// Good - Tests one behavior
@Test
void testSaveScript_ValidInput_ReturnsTrue() { ... }

// Bad - Tests multiple behaviors
@Test
void testScriptOperations() {
    // Saves script
    // Loads script
    // Deletes script
}
```

### 4. Use Descriptive Names
```java
// Good
@Test
@DisplayName("Should retry connection 3 times before failing")
void testConnect_NetworkFailure_RetriesThreeTimes() { ... }

// Bad
@Test
void test1() { ... }
```

## Continuous Integration

### Local Pre-commit
Run tests before committing:
```bash
./gradlew test
```

### GitHub Actions (Disabled)
**Note:** GitHub Actions currently disabled due to free tier limits. Tests run locally before each commit.

**Previous workflow** (`.github/workflows/test.yml.disabled`):
```yaml
name: Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: cd python3-integration && ./gradlew test
```

## Coverage Goals

**Current:** ~19% (mostly gateway code)

**Target Goals:**
- **Phase 1:** 40% - Core manager logic
- **Phase 2:** 60% - Include UI components
- **Phase 3:** 80% - Comprehensive coverage

**Priority Coverage:**
1. Business logic (managers) - 80%+
2. Data models (records) - 60%+
3. UI components (panels) - 40%+
4. Utilities - 70%+

## Troubleshooting

### Tests Not Running
```bash
# Clean and rebuild
./gradlew clean test

# Check test discovery
./gradlew test --info | grep "Test"
```

### EDT Thread Issues
For Swing tests, ensure operations run on EDT:
```java
SwingUtilities.invokeAndWait(() -> {
    // UI operations here
});
```

### Mock Not Working
Verify MockitoAnnotations initialization:
```java
@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
}
```

## Resources

### Documentation
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ Documentation](https://assertj.github.io/doc/)

### Examples
- `ManagerSmokeTest.java` - Basic structure validation
- Official SDK examples: https://github.com/inductiveautomation/ignition-sdk-examples

## Next Steps

### Short Term
1. ✅ Smoke tests for all managers (v2.11.1)
2. ⏳ Unit tests for ThemeManager (no dependencies)
3. ⏳ Unit tests for AutoSaveManager (file I/O)

### Medium Term
1. ⏳ Mock Python3RestClient for network tests
2. ⏳ Integration tests for full workflows
3. ⏳ UI component tests with AssertJ Swing

### Long Term
1. ⏳ Achieve 60%+ code coverage
2. ⏳ Performance/load testing
3. ⏳ Re-enable GitHub Actions when resources allow

---

**Maintained by:** Development Team
**Last Review:** 2025-10-28
