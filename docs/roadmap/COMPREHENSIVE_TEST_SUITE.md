# Comprehensive Test Suite Implementation Guide
## Python 3 Integration Module for Ignition

### Overview
This document provides a complete specification for implementing a comprehensive test suite for the Python 3 Integration module. The test suite should achieve minimum 80% code coverage and ensure production readiness.

## Test Framework Setup

### Dependencies to Add
Add to `python3-integration/build.gradle.kts`:
```gradle
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito:mockito-inline:5.5.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("com.github.tomakehurst:wiremock:3.0.1")
    testImplementation("org.slf4j:slf4j-simple:2.0.9")
}
```

## 1. Unit Tests

### A. Gateway Components

#### Test: Python3ProcessPoolTest
**Location:** `gateway/src/test/java/.../gateway/Python3ProcessPoolTest.java`

**Test Cases:**
```java
1. testPoolInitialization()
   - Verify pool creates correct number of processes
   - Assert all executors are healthy after initialization
   - Verify available executors equals pool size

2. testBorrowAndReturnExecutor()
   - Borrow executor from pool
   - Verify pool size decreases
   - Return executor
   - Verify pool size restores
   - Assert same executor is reusable

3. testConcurrentBorrowing()
   - Create pool with 3 executors
   - Spawn 10 threads each trying to borrow
   - Verify proper queuing behavior
   - Assert no deadlocks occur
   - Verify all threads eventually get an executor

4. testBorrowTimeout()
   - Create pool with 1 executor
   - Borrow the executor
   - Try to borrow another with 1-second timeout
   - Assert TimeoutException is thrown
   - Return first executor
   - Verify second borrow succeeds

5. testHealthCheckAndRecovery()
   - Mock an unhealthy executor
   - Trigger health check
   - Verify unhealthy executor is replaced
   - Assert new executor is healthy
   - Verify pool size remains constant

6. testPoolShutdown()
   - Create pool with multiple executors
   - Execute some tasks
   - Shutdown pool
   - Verify all processes are terminated
   - Assert no resource leaks

7. testDynamicPoolResize()
   - Start with pool size 3
   - Resize to 5
   - Verify 2 new executors added
   - Resize to 2
   - Verify 3 executors removed safely
   - Assert in-use executors are not killed

8. testProcessCrashRecovery()
   - Simulate process crash (kill subprocess)
   - Verify pool detects crash
   - Assert automatic replacement occurs
   - Verify pool remains functional
```

#### Test: Python3ExecutorTest
**Location:** `gateway/src/test/java/.../gateway/Python3ExecutorTest.java`

**Test Cases:**
```java
1. testSimpleCodeExecution()
   - Execute "print('hello')"
   - Verify output equals "hello\n"
   - Assert no errors

2. testVariableInjection()
   - Inject variables: x=5, y=10
   - Execute "print(x + y)"
   - Verify output equals "15\n"

3. testErrorHandling()
   - Execute code with syntax error
   - Verify error is captured
   - Assert error message contains line number
   - Verify executor remains healthy after error

4. testTimeout()
   - Execute infinite loop with 2-second timeout
   - Verify TimeoutException thrown
   - Assert process is killed
   - Verify executor is marked unhealthy

5. testLargeOutput()
   - Execute code generating 10MB output
   - Verify all output is captured
   - Assert no buffer overflow
   - Test output truncation at limit

6. testUnicodeHandling()
   - Execute code with unicode: "print('Hello 世界 🌍')"
   - Verify correct encoding/decoding
   - Test various character sets

7. testResourceLimits()
   - Test memory limit enforcement
   - Test CPU time limit enforcement
   - Verify limits are applied correctly

8. testConcurrentExecution()
   - Verify executor properly serializes requests
   - Assert no race conditions
   - Test execution queue behavior

9. testProcessRestart()
   - Execute code successfully
   - Force process restart
   - Execute code again
   - Verify both executions succeed
```

#### Test: Python3RestEndpointsTest
**Location:** `gateway/src/test/java/.../gateway/Python3RestEndpointsTest.java`

**Test Cases:**
```java
1. testExecuteEndpoint()
   - Mock HTTP POST to /execute
   - Send valid JSON payload
   - Verify 200 response
   - Assert response contains output
   - Test with various code samples

2. testRateLimiting()
   - Send 100 requests rapidly
   - Send 101st request
   - Verify rate limit response (429)
   - Wait 1 minute
   - Verify requests work again

3. testInputValidation()
   - Test oversized code (>1MB)
   - Test invalid JSON
   - Test missing required fields
   - Test SQL injection attempts
   - Test path traversal attempts
   - Verify all return 400 with appropriate errors

4. testHealthEndpoint()
   - GET /health
   - Verify 200 response
   - Assert response contains pool stats
   - Test during various pool states

5. testDiagnosticsEndpoint()
   - GET /diagnostics
   - Verify metrics present
   - Test calculation accuracy
   - Verify sensitive data not exposed

6. testScriptCRUD()
   - Test POST /scripts (create)
   - Test GET /scripts/{id} (read)
   - Test PUT /scripts/{id} (update)
   - Test DELETE /scripts/{id} (delete)
   - Test GET /scripts (list)
   - Verify proper error handling for missing scripts

7. testScriptSigning()
   - Save signed script
   - Attempt to tamper with script
   - Verify signature validation fails
   - Test signature regeneration

8. testCORSHeaders()
   - Verify CORS headers present
   - Test preflight requests
   - Verify allowed origins configuration

9. testSecurityHeaders()
   - Verify X-Content-Type-Options
   - Verify X-Frame-Options
   - Verify CSP headers
   - Test CSRF token validation

10. testAsyncExecution()
    - Test /execute/async endpoint
    - Verify job ID returned
    - Test status polling
    - Test result retrieval
    - Test job cancellation
```

#### Test: Python3ScriptRepositoryTest
**Location:** `gateway/src/test/java/.../gateway/Python3ScriptRepositoryTest.java`

**Test Cases:**
```java
1. testSaveAndLoadScript()
2. testScriptVersioning()
3. testFolderOrganization()
4. testScriptSearch()
5. testScriptMetadata()
6. testConcurrentAccess()
7. testDataPersistence()
8. testScriptSizeLimit()
```

### B. Designer Components

#### Test: GatewayConnectionManagerTest
**Location:** `designer/src/test/java/.../managers/GatewayConnectionManagerTest.java`

**Test Cases:**
```java
1. testSuccessfulConnection()
2. testConnectionFailure()
3. testConnectionRetry()
4. testExecuteCode()
5. testPoolStatsRetrieval()
6. testPythonVersionDetection()
7. testConnectionTimeout()
8. testSSLConnection()
```

#### Test: ScriptManagerTest
**Location:** `designer/src/test/java/.../managers/ScriptManagerTest.java`

**Test Cases:**
```java
1. testSaveScript()
2. testLoadScript()
3. testDeleteScript()
4. testRenameScript()
5. testMoveScript()
6. testListScripts()
7. testScriptValidation()
8. testFolderOperations()
```

### C. UI Component Tests

#### Test: EditorPanelTest
**Location:** `designer/src/test/java/.../ui/EditorPanelTest.java`

**Test Cases:**
```java
1. testCodeHighlighting()
2. testAutoIndentation()
3. testBraceMatching()
4. testUndoRedo()
5. testFindReplace()
6. testKeyboardShortcuts()
7. testThemeSwitch()
8. testFontSizeChange()
```

#### Test: ScriptTreePanelTest
**Location:** `designer/src/test/java/.../ui/ScriptTreePanelTest.java`

**Test Cases:**
```java
1. testTreePopulation()
2. testDragAndDrop()
3. testContextMenu()
4. testFolderExpansion()
5. testScriptSelection()
6. testRename()
7. testDelete()
8. testSearch()
```

## 2. Integration Tests

### A. End-to-End Process Tests

#### Test: ProcessPoolIntegrationTest
**Location:** `gateway/src/test/java/.../integration/ProcessPoolIntegrationTest.java`

**Test Cases:**
```java
1. testFullExecutionFlow()
   - Start process pool
   - Execute multiple scripts
   - Verify outputs
   - Test error scenarios
   - Shutdown cleanly

2. testStressTest()
   - Execute 1000 scripts concurrently
   - Monitor memory usage
   - Verify no process leaks
   - Test pool recovery under load

3. testLongRunningScripts()
   - Execute scripts running 30+ seconds
   - Test timeout handling
   - Verify cleanup

4. testMemoryIntensiveScripts()
   - Execute memory-heavy operations
   - Verify memory limits enforced
   - Test OOM handling

5. testPythonLibraryUsage()
   - Test numpy operations
   - Test pandas DataFrames
   - Test file I/O
   - Test network requests
   - Verify isolation between executions
```

### B. REST API Integration Tests

#### Test: RestAPIIntegrationTest
**Location:** `gateway/src/test/java/.../integration/RestAPIIntegrationTest.java`

**Test Cases:**
```java
1. testCompleteScriptLifecycle()
   - Create script via API
   - Execute script
   - Update script
   - List scripts
   - Delete script
   - Verify each step

2. testConcurrentAPIRequests()
   - 50 concurrent execute requests
   - Different scripts/parameters
   - Verify all complete successfully
   - Test request queuing

3. testAPIAuthentication()
   - Test with valid API key
   - Test with invalid API key
   - Test with expired key
   - Verify proper rejection

4. testWebSocketConnection()
   - Connect to execution stream
   - Execute long-running script
   - Verify real-time output
   - Test connection recovery
```

## 3. Performance Tests

### Test: PerformanceBenchmarkTest
**Location:** `gateway/src/test/java/.../performance/PerformanceBenchmarkTest.java`

**Test Cases:**
```java
1. testExecutionLatency()
   - Measure simple script execution time
   - Target: <100ms for "print('hello')"
   - Test percentiles: p50, p95, p99

2. testThroughput()
   - Measure executions per second
   - Target: >50 executions/second
   - Test with various pool sizes

3. testMemoryUsage()
   - Monitor heap usage over 1000 executions
   - Verify no memory leaks
   - Test garbage collection impact

4. testStartupTime()
   - Measure module startup time
   - Target: <5 seconds
   - Test with various pool sizes

5. testScriptStoragePerformance()
   - Test with 10,000 stored scripts
   - Measure list/search performance
   - Target: <500ms for listing
```

## 4. Security Tests

### Test: SecurityValidationTest
**Location:** `gateway/src/test/java/.../security/SecurityValidationTest.java`

**Test Cases:**
```java
1. testCodeInjection()
   - Test OS command injection attempts
   - Test import restrictions
   - Test file system access attempts
   - Verify all are blocked/sanitized

2. testResourceExhaustion()
   - Test infinite loops
   - Test memory bombs
   - Test fork bombs
   - Verify limits enforced

3. testPathTraversal()
   - Test ../../../ in script names
   - Test absolute paths
   - Test symbolic links
   - Verify proper sanitization

4. testXSS()
   - Test script output with HTML/JS
   - Verify proper escaping
   - Test in all output contexts

5. testSQLInjection()
   - Test script names with SQL
   - Test in search parameters
   - Verify parameterized queries

6. testRateLimitBypass()
   - Attempt various bypass techniques
   - Different IPs, headers, etc.
   - Verify rate limit holds
```

## 5. Mock/Stub Implementations

### MockPython3Executor
```java
public class MockPython3Executor extends Python3Executor {
    private String mockOutput = "";
    private String mockError = "";
    private boolean shouldTimeout = false;
    private boolean shouldCrash = false;

    public void setMockOutput(String output) { ... }
    public void setMockError(String error) { ... }
    public void setShouldTimeout(boolean timeout) { ... }
    public void setShouldCrash(boolean crash) { ... }

    @Override
    public ExecutionResult execute(String code, Map<String, Object> vars, long timeout) {
        if (shouldTimeout) throw new TimeoutException();
        if (shouldCrash) throw new RuntimeException("Process crashed");
        return new ExecutionResult(mockOutput, mockError, 0);
    }
}
```

## 6. Test Data

### TestDataProvider
```java
public class TestDataProvider {
    public static String getSimpleScript() {
        return "print('Hello, World!')";
    }

    public static String getComplexScript() {
        return """
            import json
            data = {'name': 'test', 'value': 42}
            print(json.dumps(data))
            """;
    }

    public static String getErrorScript() {
        return "print(undefined_variable)";
    }

    public static String getInfiniteLoop() {
        return "while True: pass";
    }

    public static String getMemoryIntensive() {
        return "x = [0] * (10**9)";
    }

    public static Map<String, Object> getSampleVariables() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("temperature", 25.5);
        vars.put("pressure", 101.325);
        vars.put("tags", Arrays.asList("tag1", "tag2"));
        return vars;
    }
}
```

## 7. Test Configuration

### test-application.properties
```properties
# Test Configuration
ignition.python3.poolsize=2
ignition.python3.timeout.default=5000
ignition.python3.max.memory.mb=256
ignition.python3.max.cpu.seconds=10
ignition.python3.test.mode=true

# Disable rate limiting for tests
api.rate.limit.enabled=false

# Use in-memory storage for tests
script.repository.type=memory

# Test logging
logging.level.root=WARN
logging.level.com.inductiveautomation.ignition.examples.python3=DEBUG
```

## 8. Continuous Integration

### GitHub Actions Workflow
Create `.github/workflows/test.yml`:
```yaml
name: Test Suite

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest

    strategy:
      matrix:
        java: [11, 17]
        python: [3.8, 3.9, 3.10, 3.11]

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK
      uses: actions/setup-java@v3
      with:
        java-version: ${{ matrix.java }}
        distribution: 'temurin'

    - name: Set up Python
      uses: actions/setup-python@v4
      with:
        python-version: ${{ matrix.python }}

    - name: Install Python dependencies
      run: |
        pip install numpy pandas requests
        pip install pyflakes jedi

    - name: Run tests
      run: |
        cd python3-integration
        ./gradlew test --info

    - name: Generate coverage report
      run: |
        cd python3-integration
        ./gradlew jacocoTestReport

    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: ./python3-integration/build/reports/jacoco/test/jacocoTestReport.xml

    - name: Archive test results
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: test-results-${{ matrix.java }}-${{ matrix.python }}
        path: python3-integration/build/test-results/
```

## 9. Test Execution Commands

### Run All Tests
```bash
cd python3-integration
./gradlew test
```

### Run Specific Test Class
```bash
./gradlew test --tests "*.Python3ProcessPoolTest"
```

### Run with Coverage
```bash
./gradlew test jacocoTestReport
# Report at: build/reports/jacoco/test/html/index.html
```

### Run Integration Tests Only
```bash
./gradlew test --tests "*.integration.*"
```

### Run Performance Tests
```bash
./gradlew test --tests "*.performance.*" -Dperformance.tests.enabled=true
```

## 10. Coverage Goals

### Minimum Coverage Requirements
- **Overall:** 80%
- **Gateway Module:** 85%
- **Process Pool:** 90%
- **REST Endpoints:** 85%
- **Designer Components:** 75%
- **UI Components:** 70%

### Critical Paths (100% Coverage Required)
- Python3Executor.execute()
- Python3ProcessPool.borrowExecutor()
- Python3RestEndpoints security validation
- Script signing and verification
- Error handling and recovery

## 11. Test Documentation

Each test class should include:
1. Class-level Javadoc explaining purpose
2. Method-level comments for complex tests
3. Clear test names following pattern: `test<Feature>_<Scenario>_<ExpectedResult>()`
4. Assertions with descriptive messages
5. Proper test isolation (no shared state)

## 12. Test Maintenance

### Regular Tasks
1. **Weekly:** Review flaky tests
2. **Monthly:** Update test data
3. **Quarterly:** Performance baseline update
4. **Per Release:** Full regression suite
5. **Continuous:** Add tests for bug fixes

### Test Review Checklist
- [ ] Tests are independent and can run in any order
- [ ] No hardcoded paths or environment assumptions
- [ ] Proper cleanup in @After methods
- [ ] Meaningful assertion messages
- [ ] Both positive and negative test cases
- [ ] Edge cases covered
- [ ] Performance tests have baselines
- [ ] Security tests updated with new threats

## Implementation Priority

### Phase 1 (Week 1)
1. Set up test framework
2. Implement Python3ExecutorTest
3. Implement Python3ProcessPoolTest
4. Basic integration tests

### Phase 2 (Week 2)
1. REST endpoint tests
2. Security tests
3. Designer component tests
4. Performance benchmarks

### Phase 3 (Week 3)
1. UI component tests
2. End-to-end tests
3. CI/CD setup
4. Documentation

## Success Metrics

- **All tests passing** in CI/CD pipeline
- **80%+ code coverage** achieved
- **Performance baselines** established
- **Security vulnerabilities** identified and fixed
- **Flaky test rate** <1%
- **Test execution time** <5 minutes for unit tests
- **Clear documentation** for test maintenance
