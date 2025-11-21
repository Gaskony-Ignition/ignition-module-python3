# Python 3 Integration - Security Guide

**Version:** v2.15.9
**Last Updated:** October 2025

---

## Overview

The Python 3 Integration module implements a **three-tier security model** that balances security with usability:

1. **DESIGNER_ADMIN** - Full Python capabilities for Designer IDE users (trusted)
2. **ADMIN** - Extended capabilities for API users with admin key
3. **RESTRICTED** - Safe modules only for unauthenticated API access

---

## Security Architecture

### Four-Layer Defense

```
┌─────────────────────────────────────────────────────┐
│ Layer 1: Authentication                             │
│ - Designer: Ignition Designer Auth (built-in)      │
│ - REST API: API Token validation                    │
│ - Gateway: Ignition user context                    │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│ Layer 2: Authorization (Security Mode)              │
│ - DESIGNER_ADMIN: Full capabilities (Designer IDE)  │
│ - ADMIN: os, subprocess, requests (API with key)    │
│ - RESTRICTED: Safe modules only (default API/Script)│
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│ Layer 3: Code Validation (AST-based)                │
│ - Parse code into Abstract Syntax Tree              │
│ - Validate imports against whitelist                │
│ - Block dangerous function calls                    │
│ - Detect evasion techniques                         │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│ Layer 4: Resource Limits & Audit Logging            │
│ - Memory: 512MB (prevent accidents)                 │
│ - CPU: 60s timeout (prevent infinite loops)         │
│ - Code size: 1MB (prevent payload attacks)          │
│ - All actions logged for compliance                 │
└─────────────────────────────────────────────────────┘
```

---

## Security Modes

### DESIGNER_ADMIN Mode

**Who:** Ignition Designer IDE users
**Trust Level:** HIGH (authenticated administrators)
**Access:** Full Python 3 capabilities

**Allowed:**
- ✅ All safe modules (math, json, datetime, etc.)
- ✅ All admin modules (os, sys, subprocess, requests, pandas, numpy, etc.)
- ✅ File I/O operations
- ✅ Network requests
- ✅ Shell command execution
- ✅ Package installation (`pip install`)

**Blocked:**
- ❌ ctypes (can bypass security)
- ❌ multiprocessing (can spawn uncontrolled processes)
- ❌ threading (resource management issues)
- ❌ telnetlib, paramiko (network security concerns)

**Why Safe?**
- Users are already authenticated via Ignition Designer
- They have administrative access to the Ignition system
- All actions are audit logged
- Resource limits prevent accidents (512MB RAM, 60s timeout)

**Example:**
```python
# In Designer IDE - Full capabilities
import os
import sys
import requests
import pandas as pd

# Execute shell command
result = os.system("ls -la")

# Install package
import subprocess
subprocess.run(["pip", "install", "pandas"])

# Use pandas
df = pd.DataFrame({"col1": [1, 2, 3]})
```

---

### ADMIN Mode

**Who:** REST API users with admin API key
**Trust Level:** MEDIUM (external systems with credentials)
**Access:** Extended Python capabilities
**Requires:** HTTPS + Admin API Key (32+ characters)

**Allowed:**
- ✅ All safe modules (math, json, datetime, etc.)
- ✅ All admin modules (os, sys, subprocess, requests, pandas, numpy, etc.)

**Blocked:**
- ❌ Same as DESIGNER_ADMIN (always blocked modules)

**Configuration:**
```bash
# Generate secure admin key (32+ characters)
openssl rand -hex 32

# Configure in ignition.conf
wrapper.java.additional.200=-Dignition.python3.admin.apikey=<generated-key>

# Restart Ignition Gateway
```

**Example:**
```bash
# With admin key - ADMIN mode
curl -X POST https://gateway:8088/data/python3integration/api/v1/exec \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <32-char-admin-key>" \
  -d '{"code": "import os; result = os.getcwd()"}'
# ✅ Works - ADMIN mode granted
```

**Security Requirements:**
- ✅ HTTPS required (enforced by default)
- ✅ Admin key must be 32+ characters
- ✅ Constant-time key comparison (prevents timing attacks)
- ✅ All actions audit logged

---

### RESTRICTED Mode

**Who:** Unauthenticated REST API users
**Trust Level:** LOW (anyone with network access)
**Access:** Safe modules only

**Allowed:**
- ✅ math, json, datetime, itertools, collections
- ✅ decimal, random, re, statistics, time, calendar
- ✅ uuid, hashlib, base64, string, textwrap
- ✅ difflib, enum, functools, operator, copy

**Blocked:**
- ❌ os, sys, subprocess (file system access)
- ❌ socket, urllib, requests (network access)
- ❌ pickle, shelve (arbitrary code execution)
- ❌ All always-blocked modules

**Example:**
```bash
# No auth - RESTRICTED mode
curl -X POST http://gateway:8088/data/python3integration/api/v1/exec \
  -H "Content-Type: application/json" \
  -d '{"code": "import math; result = math.sqrt(16)"}'
# ✅ Works - math is allowed

curl -X POST http://gateway:8088/data/python3integration/api/v1/exec \
  -H "Content-Type: application/json" \
  -d '{"code": "import os; result = os.getcwd()"}'
# ❌ Fails - os requires ADMIN mode
```

---

## AST-Based Code Validation

### How It Works

The module uses **Abstract Syntax Tree (AST) parsing** instead of string matching to validate code:

1. **Parse Code** - Convert Python code into AST
2. **Walk Tree** - Traverse all nodes in the AST
3. **Validate Imports** - Check all `import` and `from...import` statements
4. **Check Function Calls** - Detect dangerous functions (`eval`, `exec`, `__import__`)
5. **Block Attribute Access** - Prevent `os.system()`, `subprocess.call()`, etc.

**Why AST?**
- ✅ Cannot be bypassed with string tricks (`eval("im" + "port os")`)
- ✅ Detects dynamic imports (`__import__("os")`)
- ✅ Catches attribute access (`os.system()`)
- ✅ Industry-standard security practice

### Example Validation

```python
# RESTRICTED mode - All these are blocked by AST validator:

# 1. Direct import
import os  # ❌ Blocked - os not in safe_modules

# 2. From import
from subprocess import call  # ❌ Blocked - subprocess not in safe_modules

# 3. String concatenation trick (bypasses regex, but NOT AST)
exec("imp" + "ort os")  # ❌ Blocked - AST detects exec() call

# 4. Dynamic import
__import__("os")  # ❌ Blocked - AST detects __import__() call

# 5. Attribute access
import os  # Already blocked, but if somehow imported:
os.system("ls")  # ❌ Blocked - AST detects os.system() call
```

---

## Audit Logging

All Python executions are logged for compliance and security monitoring.

### Log Format

```json
{
  "timestamp": "2025-10-20T15:30:45Z",
  "user": "admin",
  "sourceIP": "192.168.1.100",
  "securityMode": "DESIGNER_ADMIN",
  "codeHash": "a1b2c3d4e5f6...",
  "modulesUsed": "os, sys, requests",
  "success": true,
  "durationMs": 125,
  "resultSize": 1024
}
```

### Log Location

Logs are written to two places:

1. **SLF4J Logger** - Standard Ignition logs (`wrapper.log`)
2. **Audit Log Files** - Daily rotated files in `data/python3-integration/audit/`

Example:
```bash
# View audit logs
tail -f data/python3-integration/audit/audit-2025-10-20.log

# Search for specific user
grep "user=admin" data/python3-integration/audit/audit-2025-10-20.log
```

### What's Logged

- ✅ Who executed the code (user ID, IP address)
- ✅ When it was executed (timestamp)
- ✅ What security mode was used (DESIGNER_ADMIN, ADMIN, RESTRICTED)
- ✅ Code hash (SHA-256) for forensics
- ✅ Modules imported (os, sys, requests, etc.)
- ✅ Success/failure status
- ✅ Execution duration (milliseconds)

---

## Rate Limiting

To prevent abuse, the module enforces rate limits:

- **Limit:** 100 requests per minute per user
- **Granularity:** Per IP address
- **Response:** HTTP 429 Too Many Requests

**Example:**
```bash
# After 100 requests in 1 minute:
curl -X POST http://gateway:8088/data/python3integration/api/v1/exec \
  -H "Content-Type: application/json" \
  -d '{"code": "result = 2 + 2"}'

# Response:
{
  "success": false,
  "error": "Rate limit exceeded. Maximum 100 requests per minute."
}
```

---

## Resource Limits

All Python processes are subject to resource limits:

### Memory Limit

- **Default:** 512MB per process
- **Purpose:** Prevent memory exhaustion attacks
- **Override:** Set environment variable `PYTHON3_MAX_MEMORY_MB`

```bash
# In ignition.conf
wrapper.java.additional.300=-DPYTHON3_MAX_MEMORY_MB=1024
```

### CPU Time Limit

- **Default:** 60 seconds per execution
- **Purpose:** Prevent infinite loops
- **Override:** Set environment variable `PYTHON3_MAX_CPU_SECONDS`

```bash
# In ignition.conf
wrapper.java.additional.301=-DPYTHON3_MAX_CPU_SECONDS=120
```

### Code Size Limit

- **Limit:** 1MB per request
- **Purpose:** Prevent payload attacks
- **Non-configurable** (security hardening)

---

## Security Best Practices

### For Administrators

1. **Use Strong Admin Keys**
   ```bash
   # Generate 32+ character key
   openssl rand -hex 32
   ```

2. **Enforce HTTPS for ADMIN Mode**
   ```bash
   # Default is true - DO NOT disable in production
   wrapper.java.additional.400=-Dignition.python3.admin.requirehttps=true
   ```

3. **Monitor Audit Logs**
   ```bash
   # Daily review
   tail -f data/python3-integration/audit/audit-$(date +%Y-%m-%d).log
   ```

4. **Review Rate Limits**
   - Check for repeated failures
   - Investigate suspicious patterns
   - Adjust limits if needed (not recommended)

### For Developers

1. **Use Designer IDE for Development**
   - Full Python capabilities (DESIGNER_ADMIN mode)
   - No need for API keys
   - Audit logged for compliance

2. **Test in RESTRICTED Mode First**
   - Ensure scripts work with safe modules only
   - Add admin modules only if needed
   - Document why admin access is required

3. **Never Hardcode Admin Keys**
   - Use environment variables
   - Rotate keys regularly
   - Revoke unused tokens

### For Security Teams

1. **Regular Audits**
   - Review audit logs weekly
   - Check for unauthorized ADMIN mode usage
   - Verify Designer users have legitimate access

2. **Penetration Testing**
   - Test AST validation bypasses
   - Verify rate limiting effectiveness
   - Check HTTPS enforcement

3. **Incident Response**
   - Revoke compromised API keys immediately
   - Review all executions by compromised user
   - Update admin key after incident

---

## Security Testing

**Test Suite Status:** ✅ 184 tests passing (19% code coverage)

Phase 3 comprehensive testing has been completed with the following test suites:

### Test Suites

1. **Python3SecurityServiceTest** (30 tests)
   - ✅ Designer mode detection (case-insensitive)
   - ✅ Admin API key validation
   - ✅ Token generation and revocation
   - ✅ HTTPS enforcement
   - ✅ Security mode priority

2. **AstValidationSecurityTest** (35+ tests)
   - ✅ String concatenation bypass attempts
   - ✅ Dynamic import detection
   - ✅ Eval/exec bypass prevention
   - ✅ Attribute access validation
   - ✅ Always-blocked module enforcement

3. **SecurityModeTest** (12 tests)
   - ✅ Enum validation and parsing
   - ✅ Mode classification methods
   - ✅ Case-insensitive mode detection

4. **Python3AuditEventTest** (17 tests)
   - ✅ Event creation and validation
   - ✅ Log line formatting
   - ✅ Null handling

5. **Python3SecurityUtilsTest** (16 tests)
   - ✅ Code hashing (SHA-256)
   - ✅ User/IP extraction
   - ✅ Endpoint formatting

### Test DESIGNER_ADMIN Mode

```python
# In Designer IDE - Should succeed
import os
import sys
import requests
result = os.getcwd()
print(f"Current directory: {result}")
```

### Test ADMIN Mode

```bash
# With admin key - Should succeed
curl -X POST https://gateway:8088/data/python3integration/api/v1/exec \
  -H "Authorization: Bearer <admin-key>" \
  -H "Content-Type: application/json" \
  -d '{"code": "import os; result = os.getcwd()"}'
```

### Test RESTRICTED Mode

```bash
# No auth - Should fail
curl -X POST http://gateway:8088/data/python3integration/api/v1/exec \
  -H "Content-Type: application/json" \
  -d '{"code": "import os; result = os.getcwd()"}'

# Expected response:
{
  "success": false,
  "error": "SECURITY ERROR: Module 'os' not allowed in RESTRICTED mode..."
}
```

### Test AST Validation

```bash
# Try to bypass with string tricks - Should fail
curl -X POST http://gateway:8088/data/python3integration/api/v1/exec \
  -H "Content-Type: application/json" \
  -d '{"code": "__import__(\"os\").system(\"ls\")"}'

# Expected response:
{
  "success": false,
  "error": "SECURITY ERROR: Function '__import__()' not allowed in RESTRICTED mode..."
}
```

---

## Threat Model

### Threats Mitigated

✅ **Unauthorized File Access** - RESTRICTED mode blocks os, pathlib
✅ **Network Attacks** - RESTRICTED mode blocks socket, urllib, requests
✅ **Code Injection** - AST validation blocks eval, exec, __import__
✅ **Resource Exhaustion** - Memory/CPU limits prevent DoS
✅ **Timing Attacks** - Constant-time admin key comparison
✅ **Replay Attacks** - Audit logging with timestamps
✅ **Privilege Escalation** - Security mode enforcement

### Residual Risks

⚠️ **Designer Users Can Execute Arbitrary Code**
   - Mitigation: Audit logging, resource limits
   - Justification: Designer users are trusted administrators

⚠️ **Admin Key Compromise**
   - Mitigation: 32+ char requirement, HTTPS enforcement
   - Response: Revoke key, review audit logs

⚠️ **Resource Limit Bypass (Linux Only)**
   - Mitigation: Resource limits not available on Windows
   - Response: Monitor CPU/memory usage, use process monitoring

---

## Compliance

This security model supports compliance with:

- **SOC 2** - Audit logging, access control
- **ISO 27001** - Risk management, security controls
- **NIST Cybersecurity Framework** - Identify, Protect, Detect, Respond

---

## Version History

- **v2.6.0** (Oct 2025) - Added AST validation, Designer IDE integration
- **v2.5.0** (Oct 2025) - Initial security implementation (Phase 1)

---

## Support

For security questions or to report vulnerabilities:
- **GitHub Issues:** https://github.com/inductiveautomation/ignition-module-python3/issues
- **Ignition Forum:** https://forum.inductiveautomation.com/

---

**Remember:** Security is a balance between protection and usability. This module prioritizes usability for trusted Designer users while maintaining strong security for external API access.
