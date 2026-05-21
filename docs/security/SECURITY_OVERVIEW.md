# Python 3 Integration - Security Guide

**Version:** v2.15.10
**Last Updated:** October 2025

---

## Overview

The Python 3 Integration module implements a **two-tier security model** (as of v4.0.0; the previous `RESTRICTED` mode was removed — see "What changed in v4.0.0" below):

1. **DESIGNER_ADMIN** - Full Python capabilities for Designer IDE users (trusted)
2. **ADMIN** - Full Python capabilities for API callers authenticated via the admin key path (equivalent to DESIGNER_ADMIN, retained only for audit-log clarity)

Unauthenticated REST calls are now **rejected**, not silently demoted. Customers who relied on the previous `RESTRICTED` mode for unauthenticated callers must add an admin key or authenticate via Ignition session.

### What changed in v4.0.0

- `RESTRICTED` mode is gone. The AST-based filter that purported to confine untrusted callers to a whitelist of "safe" Python modules was trivially bypassable (see review item C13). Keeping it would have misled customers about its security guarantees.
- The previous "fall through to RESTRICTED" branch on unauthenticated requests is replaced with a `403 Forbidden`. Callers that depended on the silent demotion must now handle the explicit auth failure.
- For real isolation between callers and the Gateway host, run the Gateway inside a container or VM. OS-level isolation is the only effective sandbox; the in-process filter never was.

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
│ - ADMIN: Full capabilities (API with key)           │
│ - Unauthenticated: 403 Forbidden                    │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│ Layer 3: Resource Limits & Audit Logging            │
│ - Memory: 512MB (prevent accidents)                 │
│ - CPU: 60s timeout (prevent infinite loops)         │
│ - Code size: 1MB (prevent payload attacks)          │
│ - All actions logged for compliance                 │
└─────────────────────────────────────────────────────┘

> **Note (v4.0.0):** The previous "Layer 3: AST-based code validation"
> was removed alongside the RESTRICTED mode it served. AST filtering
> against an import whitelist proved bypassable in C13 and gave callers
> a false sense of containment. The remaining layers — authentication +
> authorization + resource limits + audit logs — are now the only
> in-process defences; rely on OS-level isolation for trust boundaries.
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

### Unauthenticated REST callers (v4.0.0)

**Behaviour:** `403 Forbidden`. The previous `RESTRICTED` mode that purported to grant a "safe-modules-only" capability to anonymous callers is gone.

**Migration:**
- If you previously consumed the public REST API from unauthenticated clients, mint an admin key from the Gateway and send it as `Authorization: Bearer …`. The ADMIN mode now grants full capabilities (same as DESIGNER_ADMIN) — there is no longer a reduced-capability tier.
- If you relied on the RESTRICTED-mode whitelist as a defence against malicious payloads, that defence was illusory. Replace it with a real isolation boundary (per-tenant container, network segmentation, or Gateway-per-customer deployment).

**Example:**
```bash
# v4.0.0 — auth required
curl -X POST http://gateway:8088/data/python3integration/api/v1/exec \
  -H "Authorization: Bearer <32-char-admin-key>" \
  -H "Content-Type: application/json" \
  -d '{"code": "import math; result = math.sqrt(16)"}'
# ✅ Works — ADMIN mode

curl -X POST http://gateway:8088/data/python3integration/api/v1/exec \
  -H "Content-Type: application/json" \
  -d '{"code": "import math; result = math.sqrt(16)"}'
# ❌ 403 Forbidden — no Authorization header
```

---

## AST-based code validation — removed in v4.0.0

The module previously walked the AST of incoming Python source to validate imports and detect dangerous function calls. That layer was removed in v4.0.0 because:

- The whitelist could be evaded via attribute lookups, dynamic imports, `exec()` of constructed strings, or simply spelling a banned identifier through `__import__("o" + "s")` — every static-validation approach has a documented bypass.
- Keeping the layer encouraged customers to treat anonymous callers as containable. They weren't.
- The real trust boundary — authentication — was being undermined by the existence of an "anonymous but safe" tier. v4.0.0 makes auth load-bearing.

Customers who want defence-in-depth against accidental misuse (vs. malicious) should still configure `python_bridge.py` deny-lists; those continue to work as a guardrail but are explicitly not a security boundary.
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
