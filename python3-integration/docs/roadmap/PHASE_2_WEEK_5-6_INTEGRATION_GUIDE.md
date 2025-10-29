# Phase 2 Week 5-6: Security Enhancements - Integration Guide

**Date:** October 29, 2025
**Version Target:** v2.14.0
**Status:** ✅ CORE CLASSES COMPLETE (awaiting integration)

---

## ✅ Completed: Security Infrastructure Classes

### 1. ResourceLimits.java ✅
**Purpose:** Configurable resource limits for Python execution
**Lines:** 348
**Location:** `gateway/src/main/java/.../gateway/ResourceLimits.java`

**Features:**
- Memory limit: 512 MB (configurable via `ignition.python3.limit.memory.mb`)
- CPU time limit: 60 seconds (configurable via `ignition.python3.limit.cputime.ms`)
- Execution timeout: 30 seconds (configurable via `ignition.python3.limit.timeout.ms`)
- Code size limit: 1 MB (configurable via `ignition.python3.limit.code.size`)
- Variable count limit: 100 (configurable via `ignition.python3.limit.variables.count`)
- Variable size limit: 1 MB per variable (configurable via `ignition.python3.limit.variable.size`)
- Individual enforcement flags for each limit type

**Methods:**
- `validateCodeSize(String code)` - Throws ResourceLimitException if exceeded
- `validateVariables(Map<String, Object> variables)` - Validates count and sizes
- `validateMemoryUsage(long memoryUsedMB)` - Validates memory consumption
- `validateCpuTime(long cpuTimeUsedMs)` - Validates CPU usage
- Configurable via system properties or programmatically

### 2. UserContext.java ✅
**Purpose:** User context for Python execution tracking
**Lines:** 228
**Location:** `gateway/src/main/java/.../gateway/UserContext.java`

**Features:**
- User identification (username, userId, sessionId, IP address)
- Execution source tracking (DESIGNER, REST_API, GATEWAY_SCRIPT, PERSPECTIVE, VISION, UNKNOWN)
- Factory methods for different contexts
- Header-based context extraction from REST requests

**Factory Methods:**
- `UserContext.fromDesigner(String username, String ipAddress)`
- `UserContext.fromRestApi(String username, String sessionId, String ipAddress, String endpoint)`
- `UserContext.fromGatewayScript(String scriptName)`
- `UserContext.fromRequestHeaders(Map<String, String> headers, String ipAddress)`
- `UserContext.anonymous(ExecutionSource source)`

**Key Methods:**
- `getDisplayName()` - Returns "username (userId)" or just username
- `getFullDescription()` - Returns full context string for logging
- `isAnonymous()`, `isFromDesigner()`, `isFromRestApi()`, `isFromGateway()`

### 3. EnhancedAuditLogger.java ✅
**Purpose:** Structured JSON audit logging for SIEM integration
**Lines:** 338
**Location:** `gateway/src/main/java/.../gateway/EnhancedAuditLogger.java`

**Features:**
- Asynchronous logging (non-blocking via BlockingQueue)
- Structured JSON format (.jsonl files)
- Automatic log rotation (10,000 entries per file, configurable)
- Configurable retention (90 days default)
- SHA-256 code hashing for deduplication
- Logs username, timestamp, code hash, execution time, resource usage, errors

**Methods:**
- `logExecution(UserContext userContext, String code, long executionTimeMs, boolean success, String error, long memoryUsedMB, long cpuTimeMs, String securityMode, String result)`
- `shutdown()` - Gracefully processes remaining queue entries
- `getTotalEntries()`, `getQueueSize()`, `getAuditLogDir()`

**Configuration:**
```java
EnhancedAuditLogger logger = new EnhancedAuditLogger(
    Paths.get("/path/to/audit"),  // Audit log directory
    10000,                         // Max entries per file
    90,                            // Retention days
    true                           // Enable rotation
);
```

### 4. InputValidator.java ✅
**Purpose:** Input validation for code and variables
**Lines:** 410
**Location:** `gateway/src/main/java/.../gateway/InputValidator.java`

**Features:**
- Code length validation (prevent DoS)
- Variable count and size validation
- Malicious pattern detection (file access, subprocess, network, eval/exec)
- Variable name validation (prevent injection, reserved keywords)
- Configurable security policies

**Configurable Policies:**
- `ignition.python3.validation.max.code.length` (default: 1MB)
- `ignition.python3.validation.max.variables` (default: 100)
- `ignition.python3.validation.max.variable.size` (default: 1MB)
- `ignition.python3.validation.allow.file.access` (default: false)
- `ignition.python3.validation.allow.subprocess` (default: false)
- `ignition.python3.validation.allow.network` (default: false)
- `ignition.python3.validation.enforce.patterns` (default: true)

**Methods:**
- `validateCode(String code)` - Throws ValidationException if malicious patterns detected
- `validateVariables(Map<String, Object> variables)` - Validates count, size, and names
- `validateExecutionRequest(String code, Map<String, Object> variables)` - Combined validation

**Pattern Detection:**
- BLOCK level: `eval()`, `exec()`, file access (if disabled), subprocess (if disabled), network (if disabled)
- WARN level: `compile()`, `pickle`, infinite loops, `ctypes`
- INFO level: Other suspicious patterns

### 5. RateLimiter.java ✅
**Purpose:** Token bucket rate limiting per user and globally
**Lines:** 359
**Location:** `gateway/src/main/java/.../gateway/RateLimiter.java`

**Features:**
- Token bucket algorithm with automatic refill
- Per-user rate limits (60 req/min default)
- Global rate limits (300 req/min default)
- Configurable window duration (60 seconds default)
- Statistics tracking (total requests, rejections, per-user stats)

**Configuration:**
- `ignition.python3.ratelimit.user.requests` (default: 60 req/min)
- `ignition.python3.ratelimit.global.requests` (default: 300 req/min)
- `ignition.python3.ratelimit.window.ms` (default: 60000ms)

**Methods:**
- `allowRequest(UserContext userContext)` - Returns true if allowed, false if rate limit exceeded
- `getUserStats(String username)` - Returns per-user statistics
- `reset()` - Reset all rate limits
- `resetUser(String username)` - Reset specific user's rate limit
- `cleanupInactiveUsers()` - Remove inactive user buckets

---

## ✅ Integration Complete: Python3Executor

The `Python3Executor` class has been updated with:

**New Constructor:**
```java
public Python3Executor(String pythonPath, ResourceLimits resourceLimits,
                      InputValidator inputValidator, EnhancedAuditLogger auditLogger)
```

**New Execution Methods with UserContext:**
```java
public Python3Result executeWithContext(String code, Map<String, Object> variables,
                                       String securityMode, UserContext userContext)

public Python3Result evaluateWithContext(String expression, Map<String, Object> variables,
                                        String securityMode, UserContext userContext)
```

**Execution Flow:**
1. Validate input if `inputValidator` configured
2. Check resource limits if `resourceLimits` configured
3. Execute code via subprocess
4. Audit log if `auditLogger` and `userContext` provided

**Backward Compatibility:**
- Original `execute()` and `evaluate()` methods still work
- Security components are optional (can be null)
- Existing code continues to function unchanged

---

## ⏳ Pending: Integration Tasks

### 1. Python3ProcessPool Integration

**What needs to be done:**

1. **Add security components to pool initialization:**
   ```java
   // In GatewayHook.startup()
   ResourceLimits resourceLimits = new ResourceLimits();
   InputValidator inputValidator = new InputValidator();
   EnhancedAuditLogger auditLogger = new EnhancedAuditLogger(
       Paths.get(gatewayContext.getSystemManager().getDataDir(), "python3-audit")
   );
   RateLimiter rateLimiter = new RateLimiter();

   // Pass to pool
   Python3ProcessPool pool = new Python3ProcessPool(
       pythonPath, poolSize, resourceLimits, inputValidator, auditLogger
   );
   ```

2. **Update Python3ProcessPool constructor:**
   ```java
   public Python3ProcessPool(String pythonPath, int poolSize,
                            ResourceLimits resourceLimits,
                            InputValidator inputValidator,
                            EnhancedAuditLogger auditLogger) {
       // Store components
       this.resourceLimits = resourceLimits;
       this.inputValidator = inputValidator;
       this.auditLogger = auditLogger;

       // Create executors with security components
       for (int i = 0; i < poolSize; i++) {
           Python3Executor executor = new Python3Executor(
               pythonPath, resourceLimits, inputValidator, auditLogger
           );
           availableExecutors.offer(executor);
       }
   }
   ```

3. **Add getters for security components:**
   ```java
   public ResourceLimits getResourceLimits() { return resourceLimits; }
   public InputValidator getInputValidator() { return inputValidator; }
   public EnhancedAuditLogger getAuditLogger() { return auditLogger; }
   ```

### 2. Python3RestEndpoints Integration

**What needs to be done:**

1. **Add rate limiter to REST endpoints:**
   ```java
   // In Python3RestEndpoints.java (static fields)
   private static RateLimiter rateLimiter;

   // In mountRoutes() method
   public static void mountRoutes(RouteGroup routes, Python3ScriptModule scriptModule,
                                  Python3ProcessPool processPool) {
       Python3RestEndpoints.scriptModule = scriptModule;
       Python3RestEndpoints.rateLimiter = processPool.getRateLimiter(); // Get from pool

       // Existing route mounting code...
   }
   ```

2. **Create UserContext from request in execution handlers:**
   ```java
   private static JsonObject handleExec(RequestContext req, HttpServletResponse res) {
       try {
           // Extract user context from request
           HttpServletRequest httpReq = req.getRequest();
           String username = extractUsername(httpReq);  // From session or headers
           String sessionId = httpReq.getSession(false) != null ?
                             httpReq.getSession(false).getId() : null;
           String ipAddress = httpReq.getRemoteAddr();
           String endpoint = httpReq.getRequestURI();

           UserContext userContext = UserContext.fromRestApi(
               username, sessionId, ipAddress, endpoint
           );

           // Rate limit check
           if (!rateLimiter.allowRequest(userContext)) {
               JsonObject error = new JsonObject();
               error.addProperty("success", false);
               error.addProperty("error", "Rate limit exceeded");
               res.setStatus(429); // Too Many Requests
               return error;
           }

           // Parse request body
           JsonObject requestBody = parseJsonBody(req);
           String code = requestBody.get("code").getAsString();
           Map<String, Object> variables = jsonToMap(requestBody.getAsJsonObject("variables"));

           // Execute with user context
           Python3Result result = scriptModule.executeWithContext(code, variables, userContext);

           // Return result
           return resultToJson(result);

       } catch (Exception e) {
           return createErrorResponse(e.getMessage());
       }
   }
   ```

3. **Helper method to extract username:**
   ```java
   private static String extractUsername(HttpServletRequest req) {
       // Try session attribute first
       if (req.getSession(false) != null) {
           Object userAttr = req.getSession(false).getAttribute("username");
           if (userAttr != null) return userAttr.toString();
       }

       // Try X-Username header
       String headerUser = req.getHeader("X-Username");
       if (headerUser != null && !headerUser.trim().isEmpty()) {
           return headerUser;
       }

       // Try remote user (if authenticated via container)
       String remoteUser = req.getRemoteUser();
       if (remoteUser != null && !remoteUser.trim().isEmpty()) {
           return remoteUser;
       }

       // Default to anonymous
       return "anonymous";
   }
   ```

4. **Update all execution endpoints:**
   - `/api/v1/exec` - Execute code
   - `/api/v1/eval` - Evaluate expression
   - `/api/v1/call-module` - Call module function

   Each should:
   - Create UserContext from request
   - Check rate limit
   - Execute with user context
   - Audit logging happens automatically in Python3Executor

### 3. Python3ScriptModule Integration

**What needs to be done:**

1. **Add security components to script module:**
   ```java
   // In Python3ScriptModule.java constructor
   public Python3ScriptModule(Python3ProcessPool processPool) {
       this.processPool = processPool;
       this.resourceLimits = processPool.getResourceLimits();
       this.inputValidator = processPool.getInputValidator();
       this.rateLimiter = processPool.getRateLimiter();
   }
   ```

2. **Add executeWithContext method:**
   ```java
   @ScriptFunction(docBundlePrefix = "Python3ScriptModule")
   public Python3Result executeWithContext(String code,
                                          @KeywordArgs Map<String, Object> variables,
                                          UserContext userContext) throws Python3Exception {
       // Rate limit check
       if (rateLimiter != null && !rateLimiter.allowRequest(userContext)) {
           throw new Python3Exception("Rate limit exceeded");
       }

       // Borrow executor and execute with context
       Python3Executor executor = processPool.borrowExecutor(30, TimeUnit.SECONDS);
       try {
           return executor.executeWithContext(code, variables, "RESTRICTED", userContext);
       } finally {
           processPool.returnExecutor(executor);
       }
   }
   ```

3. **Backward compatibility:**
   - Keep existing `exec()` and `eval()` methods
   - These call new methods with null UserContext
   - Audit logging only happens when UserContext provided

### 4. Designer IDE Integration

**What needs to be done:**

Update `Python3IDE.java` to create UserContext for executions:

```java
// In Python3IDE.java execution handler
private void executeCode() {
    try {
        // Get current user from Designer context
        String username = getCurrentDesignerUsername();  // From DesignerContext
        String ipAddress = "127.0.0.1";  // Local Designer execution

        UserContext userContext = UserContext.fromDesigner(username, ipAddress);

        // Execute with user context (enables audit logging)
        String code = editorPanel.getCode();
        Map<String, Object> variables = new HashMap<>();

        // Call REST API with user context headers
        httpRequest.setHeader("X-Username", username);
        httpRequest.setHeader("X-Source", "DESIGNER");

        // Existing execution code...

    } catch (Exception e) {
        LOGGER.error("Execution failed", e);
    }
}
```

---

## 📊 Testing Requirements

### Unit Tests Needed

Create test file: `gateway/src/test/java/.../gateway/SecurityEnhancementsTest.java`

```java
@Test
public void testResourceLimitsEnforcement() {
    ResourceLimits limits = new ResourceLimits(1, 1000, 1000, 100, 10, 1000);

    // Test code size limit
    String largeCode = "x = 'A' * 2000000";  // 2MB code
    assertThrows(ResourceLimitException.class, () -> {
        limits.validateCodeSize(largeCode);
    });

    // Test variable count limit
    Map<String, Object> manyVars = new HashMap<>();
    for (int i = 0; i < 200; i++) {
        manyVars.put("var" + i, i);
    }
    assertThrows(ResourceLimitException.class, () -> {
        limits.validateVariables(manyVars);
    });
}

@Test
public void testInputValidation() {
    InputValidator validator = new InputValidator();

    // Test eval() detection
    String maliciousCode = "eval('print(\"hacked\")')";
    assertThrows(ValidationException.class, () -> {
        validator.validateCode(maliciousCode);
    });

    // Test file access detection
    String fileCode = "open('/etc/passwd', 'r').read()";
    assertThrows(ValidationException.class, () -> {
        validator.validateCode(fileCode);
    });
}

@Test
public void testUserContextTracking() {
    UserContext context = UserContext.fromDesigner("testuser", "192.168.1.100");

    assertEquals("testuser", context.getUsername());
    assertEquals("192.168.1.100", context.getIpAddress());
    assertEquals(ExecutionSource.DESIGNER, context.getSource());
    assertTrue(context.isFromDesigner());
    assertFalse(context.isAnonymous());
}

@Test
public void testAuditLogging() throws Exception {
    Path tempDir = Files.createTempDirectory("audit-test");
    EnhancedAuditLogger logger = new EnhancedAuditLogger(tempDir);

    UserContext context = UserContext.fromDesigner("testuser", "127.0.0.1");

    logger.logExecution(
        context,
        "print('test')",
        150,
        true,
        null,
        10,
        50,
        "RESTRICTED",
        "test"
    );

    // Wait for async processing
    Thread.sleep(500);

    // Check log file created
    assertTrue(Files.list(tempDir).anyMatch(p -> p.toString().endsWith(".jsonl")));

    logger.shutdown();
}

@Test
public void testRateLimiting() {
    RateLimiter limiter = new RateLimiter(5, 100, 60000);  // 5 per user, 100 global
    UserContext context = UserContext.fromDesigner("testuser", "127.0.0.1");

    // First 5 should pass
    for (int i = 0; i < 5; i++) {
        assertTrue(limiter.allowRequest(context));
    }

    // 6th should fail
    assertFalse(limiter.allowRequest(context));

    // Different user should still work
    UserContext context2 = UserContext.fromDesigner("user2", "127.0.0.1");
    assertTrue(limiter.allowRequest(context2));
}

@Test
public void testExecutorWithSecurityComponents() throws Exception {
    ResourceLimits limits = new ResourceLimits();
    InputValidator validator = new InputValidator();
    Path tempDir = Files.createTempDirectory("audit-test");
    EnhancedAuditLogger logger = new EnhancedAuditLogger(tempDir);

    Python3Executor executor = new Python3Executor(
        "python3", limits, validator, logger
    );

    UserContext context = UserContext.fromDesigner("testuser", "127.0.0.1");

    // Valid code should work
    Python3Result result = executor.executeWithContext(
        "result = 2 + 2", new HashMap<>(), "RESTRICTED", context
    );
    assertTrue(result.isSuccess());

    // Malicious code should fail
    assertThrows(Python3Exception.class, () -> {
        executor.executeWithContext(
            "eval('print(\"hacked\")')", new HashMap<>(), "RESTRICTED", context
        );
    });

    executor.shutdown();
    logger.shutdown();
}
```

---

## 🚀 Next Steps

**To Complete Phase 2 Week 5-6:**

1. ✅ Create security infrastructure classes (DONE)
2. ✅ Update Python3Executor with security support (DONE)
3. ⏳ Update Python3ProcessPool initialization (IN PROGRESS)
4. ⏳ Update Python3RestEndpoints with rate limiting and user context (IN PROGRESS)
5. ⏳ Update Python3ScriptModule with security methods (IN PROGRESS)
6. ⏳ Update Designer IDE to pass user context (IN PROGRESS)
7. ⏳ Add unit tests (IN PROGRESS)
8. ⏳ Build and verify (IN PROGRESS)
9. ⏳ Increment version to v2.14.0 (IN PROGRESS)
10. ⏳ Commit and push (IN PROGRESS)

**Estimated Time to Complete:**
- Python3ProcessPool integration: 30 minutes
- REST endpoints integration: 1 hour
- ScriptModule integration: 30 minutes
- Designer IDE integration: 30 minutes
- Unit tests: 2 hours
- Testing and verification: 1 hour
- **Total: ~5-6 hours**

---

## 📝 Code Quality

All security classes follow best practices:
- ✅ Thread-safe implementations (AtomicLong, ConcurrentHashMap, synchronized)
- ✅ SLF4J logging integration
- ✅ Comprehensive Javadoc documentation
- ✅ Configurable via system properties
- ✅ Clear separation of concerns
- ✅ No external dependencies (pure JDK + Gson)
- ✅ Backward compatible (optional components)

---

**Status:** Infrastructure complete, Python3Executor updated, integration guide provided.
**Next Phase:** Integrate security components into process pool, REST API, and script module.
