# Testing Documentation

**Module:** Python 3 Integration for Ignition 8.3+
**Version:** v2.15.9
**Last Updated:** 2025-11-21

Complete testing documentation for the Python 3 Integration module.

---

## 📊 Current Test Status

**As of v2.15.9:**
- **Total Tests:** 184+
- **Status:** ✅ All passing
- **Coverage:** Gateway module coverage implemented
- **Test Types:** Unit tests, Integration tests, Manager smoke tests

---

## 📖 Testing Guides

### 1. [TESTING.md](../TESTING.md) ⭐ START HERE
**Audience:** All developers
**Purpose:** Overview of testing approach and running tests
**Topics:**
- How to run tests locally
- Test organization
- Writing new tests
- Test best practices

---

### 2. [UNIT_TESTING_GUIDE.md](../UNIT_TESTING_GUIDE.md)
**Audience:** Developers writing unit tests
**Purpose:** Detailed guide for unit testing
**Topics:**
- Unit test patterns
- Mocking strategies
- Testing individual components
- Test fixtures and utilities

---

### 3. [E2E_TESTS.md](../E2E_TESTS.md)
**Audience:** QA engineers, Integration testers
**Purpose:** End-to-end testing guide
**Topics:**
- E2E test scenarios
- Testing full workflows
- Integration with Ignition Gateway
- Designer IDE testing

---

## 🚀 Quick Start

### Running All Tests
```bash
cd python3-integration
./gradlew clean test
```

### Running Specific Test Classes
```bash
./gradlew test --tests Python3ExecutorTest
./gradlew test --tests Python3ProcessPoolTest
```

### Running Tests with Coverage
```bash
./gradlew clean test jacocoTestReport
open gateway/build/reports/jacoco/test/html/index.html
```

---

## 📈 Test Milestones

| Version | Test Count | Status | Notes |
|---------|------------|--------|-------|
| v2.15.9 | 184+ | ✅ All passing | Manager smoke tests added |
| v2.11.1 | 184 | ✅ All passing | Manager smoke tests (v2.11.1) |
| v2.11.0 | 177 | ✅ All passing | Manager refactoring complete |
| v2.5.26 | 38 | ✅ All passing | Phase 2 complete |

---

## 🧪 Test Categories

### Gateway Tests
- **Python3ExecutorTest** - Python subprocess execution
- **Python3ProcessPoolTest** - Process pool management
- **PythonDistributionManagerTest** - Python distribution detection
- **Python3ScriptModuleTest** - Scripting function tests
- **Python3RestEndpointsTest** - REST API endpoint tests

### Designer Tests
- **ManagerSmokeTest** - Smoke tests for all 7 manager classes
- **ScriptManagerTest** - Script CRUD operations
- **GatewayConnectionManagerTest** - Gateway connection management
- **ThemeManagerTest** - Theme management
- **ExecutionManagerTest** - Code execution
- **SearchManagerTest** - Find/Replace functionality
- **ScriptImportExportManagerTest** - Import/export operations
- **KeyboardShortcutsManagerTest** - Keyboard shortcuts
- **ScriptTransferManagerTest** - Drag-and-drop operations
- **CommandPaletteManagerTest** - Command palette
- **AutoSaveManagerTest** - Auto-save functionality

---

## 📝 Writing Tests

### Test Structure
```java
@Test
@DisplayName("Should execute simple Python code successfully")
void testSimpleExecution() {
    // Arrange
    String code = "result = 2 + 2";

    // Act
    ExecutionResult result = executor.execute(code, Map.of(), "RESTRICTED");

    // Assert
    assertTrue(result.isSuccess());
    assertEquals("4", result.getResult());
}
```

### Best Practices
1. ✅ Use descriptive test names with `@DisplayName`
2. ✅ Follow Arrange-Act-Assert pattern
3. ✅ Test both success and failure cases
4. ✅ Use meaningful assertions
5. ✅ Clean up resources in `@AfterEach`
6. ✅ Mock external dependencies
7. ✅ Keep tests fast (< 1 second each)

---

## 🔧 Troubleshooting

### Tests Failing Locally
1. Check Python 3 is installed: `python3 --version`
2. Verify Gradle wrapper: `./gradlew --version`
3. Clean build directory: `./gradlew clean`
4. Check Java version: `java -version` (need Java 17+)

### Coverage Not Generating
1. Run with coverage: `./gradlew jacocoTestReport`
2. Check reports: `gateway/build/reports/jacoco/test/html/index.html`
3. Ensure tests passed first

---

## 📚 Additional Resources

### Historical Test Documents (Archived)
- [2025-10-19 Test Results (v2.5.26)](../../archive/sessions/2025-10-19-test-results-v2.5.26.md)
- [2025-10-19 Test Fixes Needed](../../archive/sessions/2025-10-19-test-fixes-needed.md)
- [2025-10-19 Testing Status](../../archive/sessions/2025-10-19-testing-status.md)

### External Resources
- **JUnit 5 Docs**: https://junit.org/junit5/docs/current/user-guide/
- **Mockito Docs**: https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
- **JaCoCo Coverage**: https://www.jacoco.org/jacoco/

---

**Need help?** See [TROUBLESHOOTING.md](../../operations/TROUBLESHOOTING.md) or open an issue on GitHub.
