# Python Sandboxing and Authentication Analysis
## Python 3 Integration Module for Ignition

## Executive Summary

After reviewing your use case where **users are admin users with Designer access**, the sandboxing and authentication requirements are different from typical multi-tenant scenarios. Your users need powerful Python capabilities, not restricted execution. The focus should shift from preventing legitimate operations to **protecting against accidents, resource exhaustion, and external attacks**.

## Part 1: Python Sandboxing Strategy

### Understanding Your Use Case

Your module users are:
- **Trusted administrators** with Designer access
- **Power users** who need full Python capabilities
- **Engineers** writing automation scripts
- **Developers** creating integrations

They legitimately need to:
- Import any Python library
- Access file systems for data processing
- Make network calls to APIs
- Execute system commands for automation
- Use full Python features for complex logic

### Recommended Sandboxing Approach: Resource Protection, Not Feature Restriction

Instead of restricting Python features (which would cripple functionality), implement **resource protection and accident prevention**:

#### 1. Resource Limits (Currently Partially Implemented)

**What You Have:**
```java
// From Python3Executor.java
pb.environment().put("PYTHON3_MAX_MEMORY_MB", maxMemoryMB);
pb.environment().put("PYTHON3_MAX_CPU_SECONDS", maxCpuSeconds);
```

**What's Missing - Enforcement:**
```python
# python_bridge.py enhancement needed
import resource
import signal

def setup_resource_limits():
    # Memory limit (prevent accidental memory bombs)
    memory_limit_mb = int(os.environ.get('PYTHON3_MAX_MEMORY_MB', '512'))
    resource.setrlimit(resource.RLIMIT_AS,
        (memory_limit_mb * 1024 * 1024, memory_limit_mb * 1024 * 1024))

    # CPU time limit (prevent infinite loops)
    cpu_limit = int(os.environ.get('PYTHON3_MAX_CPU_SECONDS', '60'))
    resource.setrlimit(resource.RLIMIT_CPU, (cpu_limit, cpu_limit))

    # File descriptor limit (prevent file handle leaks)
    resource.setrlimit(resource.RLIMIT_NOFILE, (1024, 1024))

    # Process limit (prevent fork bombs)
    resource.setrlimit(resource.RLIMIT_NPROC, (32, 32))

    # Core dump size (prevent filling disk)
    resource.setrlimit(resource.RLIMIT_CORE, (0, 0))

def timeout_handler(signum, frame):
    raise TimeoutError("Execution timeout exceeded")

# Set up alarm for wall-clock timeout
signal.signal(signal.SIGALRM, timeout_handler)
```

#### 2. Execution Isolation (Recommended Addition)

**Use Process Namespacing (Linux):**
```java
// Enhanced ProcessBuilder configuration
ProcessBuilder pb = new ProcessBuilder(
    "unshare",           // Use Linux namespaces
    "--pid",             // New PID namespace
    "--net",             // Optional: network isolation
    "--mount",           // New mount namespace
    "--fork",            // Fork before executing
    pythonPath,
    "-u",
    bridgeScriptPath.toString()
);

// Alternative: Use Docker for stronger isolation
ProcessBuilder pb = new ProcessBuilder(
    "docker", "run",
    "--rm",                          // Remove container after execution
    "--memory=" + maxMemoryMB + "m", // Memory limit
    "--cpus=1.0",                    // CPU limit
    "--network=none",                // No network (optional)
    "--read-only",                   // Read-only filesystem
    "-v", scriptsDir + ":/scripts:ro", // Mount scripts read-only
    "python:3.11-slim",
    "python", "/scripts/bridge.py"
);
```

#### 3. File System Protection (Recommended Addition)

**Implement Path Restrictions:**
```python
# In python_bridge.py
import os
import tempfile

class SafeFileSystem:
    def __init__(self):
        # Define allowed directories
        self.allowed_dirs = [
            '/tmp',
            tempfile.gettempdir(),
            os.environ.get('PYTHON3_DATA_DIR', '/data/python3'),
        ]

    def validate_path(self, path):
        """Ensure path is within allowed directories"""
        abs_path = os.path.abspath(path)
        for allowed in self.allowed_dirs:
            if abs_path.startswith(os.path.abspath(allowed)):
                return True
        raise PermissionError(f"Access to {path} is not allowed")

    def safe_open(self, filename, mode='r'):
        """Wrapped file open with path validation"""
        self.validate_path(filename)
        return open(filename, mode)

# Monkey-patch built-in open (optional - may be too restrictive)
_original_open = open
def restricted_open(filename, mode='r', *args, **kwargs):
    SafeFileSystem().validate_path(filename)
    return _original_open(filename, mode, *args, **kwargs)

# Only enable if needed - this might be too restrictive for admin users
# builtins.open = restricted_open
```

#### 4. Network Access Control (Optional)

Since your users are admins who likely need network access:

```python
# In python_bridge.py - Audit instead of block
import socket
import logging

_original_socket = socket.socket

def audited_socket(*args, **kwargs):
    """Log network connections for audit trail"""
    sock = _original_socket(*args, **kwargs)
    original_connect = sock.connect

    def logged_connect(address):
        logging.info(f"Network connection to {address}")
        return original_connect(address)

    sock.connect = logged_connect
    return sock

# Enable network auditing
socket.socket = audited_socket
```

#### 5. Import Controls (Minimal - Audit Only)

For admin users, don't block imports but audit dangerous ones:

```python
# In python_bridge.py
import builtins
import logging

dangerous_modules = {
    'subprocess': 'System command execution',
    'os': 'Operating system interface',
    'ctypes': 'Low-level memory access',
    'multiprocessing': 'Process creation',
}

_original_import = builtins.__import__

def audited_import(name, *args, **kwargs):
    if name in dangerous_modules:
        logging.warning(f"Importing potentially dangerous module: {name} ({dangerous_modules[name]})")
    return _original_import(name, *args, **kwargs)

builtins.__import__ = audited_import
```

### What NOT to Implement (Would Break Functionality)

❌ **RestrictedPython** - Too restrictive for admin users
❌ **AST transformation** - Would prevent legitimate code patterns
❌ **Blocking imports** - Admins need full library access
❌ **Disabling exec/eval** - Required for dynamic code
❌ **Read-only file system** - Need to write temporary files
❌ **No network access** - Need to call APIs

## Part 2: Authentication and Authorization Gaps Explained

### Current Authentication Gaps

Looking at your code, here are the specific authentication/authorization issues:

#### Gap 1: No User Context in Execution

**Current Code (Python3RestEndpoints.java):**
```java
// Line 99: TODO: When SDK exposes user APIs, implement direct user authentication here
private static boolean hasExecutionPermission(RequestContext context) {
    // Currently returns true for all authenticated requests
    return true;
}
```

**The Issue:**
- Can't track WHO executed what code
- No per-user audit trail
- Can't implement user-specific limits
- No way to revoke individual user access

**Recommended Fix:**
```java
private static boolean hasExecutionPermission(RequestContext context) {
    // Extract user from session or API key
    String userId = extractUserId(context);
    if (userId == null) {
        return false;
    }

    // Log user action
    auditLogger.info("User {} executing Python code", userId);

    // Check user permissions (even if all admins, good to verify)
    return userService.hasRole(userId, "python3-executor");
}

private static String extractUserId(RequestContext context) {
    // Try multiple methods to get user identity

    // Method 1: API Key header
    String apiKey = context.getHeader("X-API-Key");
    if (apiKey != null) {
        return apiKeyService.getUserForKey(apiKey);
    }

    // Method 2: Session cookie
    HttpSession session = context.getSession(false);
    if (session != null) {
        return (String) session.getAttribute("userId");
    }

    // Method 3: Basic auth
    String auth = context.getHeader("Authorization");
    if (auth != null && auth.startsWith("Basic ")) {
        return parseBasicAuth(auth);
    }

    return null;
}
```

#### Gap 2: Script Access Control

**Current Issue:**
Any authenticated user can access any script

**Recommended Enhancement:**
```java
public class ScriptAccessControl {

    public boolean canRead(String userId, SavedScript script) {
        // Even for admins, implement logical access control
        if (script.isPublic()) return true;
        if (script.getAuthor().equals(userId)) return true;
        if (script.getSharedWith().contains(userId)) return true;
        if (userService.hasRole(userId, "script-admin")) return true;
        return false;
    }

    public boolean canWrite(String userId, SavedScript script) {
        // Only author or script-admin can modify
        if (script.getAuthor().equals(userId)) return true;
        if (userService.hasRole(userId, "script-admin")) return true;
        return false;
    }

    public boolean canDelete(String userId, SavedScript script) {
        // Only author or script-admin can delete
        return canWrite(userId, script);
    }
}
```

#### Gap 3: Execution Audit Trail

**Current Issue:**
No record of who executed what and when

**Recommended Implementation:**
```java
public class ExecutionAuditLogger {

    public void logExecution(ExecutionContext context) {
        AuditEntry entry = new AuditEntry()
            .withUserId(context.getUserId())
            .withTimestamp(Instant.now())
            .withScriptName(context.getScriptName())
            .withCodeHash(hashCode(context.getCode()))
            .withVariables(sanitizeVariables(context.getVariables()))
            .withResult(context.getResult())
            .withExecutionTime(context.getExecutionTime())
            .withClientIp(context.getClientIp());

        // Store in database
        auditRepository.save(entry);

        // Also log to file for compliance
        auditFileLogger.info(entry.toJson());
    }
}
```

### Why These Gaps Matter (Even for Admin Users)

1. **Compliance**: Many industries require audit trails of who executed what code
2. **Debugging**: When something goes wrong, need to know who did what
3. **Security**: Even admins can have compromised accounts
4. **Accountability**: Track resource usage per user
5. **Forensics**: Investigate issues after they occur

## Part 3: Balanced Security Recommendations

### Implement These Security Measures

#### 1. Resource Protection
✅ Memory limits (prevent OOM)
✅ CPU limits (prevent server lockup)
✅ Disk I/O limits (prevent disk filling)
✅ Process limits (prevent fork bombs)
✅ Execution timeouts (prevent hanging)

#### 2. Audit and Monitoring
✅ Log all executions with user ID
✅ Monitor resource usage
✅ Alert on suspicious patterns
✅ Track script modifications
✅ Record network connections

#### 3. Isolation
✅ Process isolation (separate processes)
✅ Working directory isolation
✅ Environment variable sanitization
✅ Optional: Container/namespace isolation

#### 4. Input Validation
✅ Script name sanitization
✅ Variable type checking
✅ Size limits on inputs
✅ Rate limiting per user

### Don't Implement These (Too Restrictive)

❌ Python feature restrictions
❌ Import blocking
❌ AST transformation
❌ Read-only execution
❌ Network blocking

## Part 4: Practical Implementation Plan

### Phase 1: Critical Security (Week 1)
1. Add resource limit enforcement in python_bridge.py
2. Implement execution timeout handling
3. Add user ID extraction from requests
4. Create audit logging system

### Phase 2: Enhanced Protection (Week 2)
1. Add process isolation (namespaces or containers)
2. Implement execution metrics collection
3. Create alerting for suspicious activity
4. Add rate limiting per user

### Phase 3: Advanced Features (Week 3)
1. Implement script access control
2. Add execution history UI
3. Create resource usage dashboard
4. Add security configuration UI

## Configuration Examples

### Recommended Security Configuration
```yaml
python3:
  security:
    # Resource limits
    max-memory-mb: 1024        # 1GB per execution
    max-cpu-seconds: 300        # 5 minutes
    max-output-size-mb: 10      # 10MB output limit
    max-processes: 10           # Prevent fork bombs

    # Timeouts
    execution-timeout-seconds: 600  # 10 minutes max
    idle-timeout-seconds: 3600      # Kill idle processes after 1 hour

    # Auditing
    audit:
      enabled: true
      log-code: false           # Don't log actual code (privacy)
      log-variables: true       # Log variable names but not values
      log-output: false         # Don't log output (might be sensitive)
      retention-days: 90        # Keep audit logs for 90 days

    # Isolation
    isolation:
      method: process           # process | container | namespace
      working-dir: /tmp/python3-${userId}-${executionId}
      cleanup-on-exit: true

    # Rate limiting
    rate-limit:
      per-user-per-minute: 60
      per-user-per-hour: 1000
      global-per-minute: 500
```

## Summary

For your use case with **admin users who need full Python functionality**:

1. **Don't restrict Python features** - Your users need them
2. **Do implement resource protection** - Prevent accidents and attacks
3. **Do add comprehensive auditing** - Track who does what
4. **Do implement user context** - Even admins need accountability
5. **Consider isolation** - Processes/containers for safety
6. **Monitor everything** - Detect issues before they become problems

The goal is **protecting the system from accidents and resource exhaustion**, not preventing legitimate operations. Your admin users are trusted but still need guardrails to prevent mistakes from taking down the server.

## Next Steps

1. Review and adjust the proposed security measures for your environment
2. Implement resource protection first (highest impact, lowest friction)
3. Add audit logging (required for most enterprises)
4. Consider container isolation if you need stronger boundaries
5. Test with your actual use cases to ensure nothing breaks
