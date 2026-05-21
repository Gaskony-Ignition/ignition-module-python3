# Python 3 Integration - Security Configuration Guide

**Version:** v2.15.10
**Last Updated:** October 2025
**Audience:** System Administrators, Security Teams

---

## Table of Contents

1. [Overview](#overview)
2. [Security Architecture](#security-architecture)
3. [Initial Configuration](#initial-configuration)
4. [Admin API Key Configuration](#admin-api-key-configuration)
5. [HTTPS Configuration](#https-configuration)
6. [Process Pool Configuration](#process-pool-configuration)
7. [Resource Limits](#resource-limits)
8. [Audit Logging](#audit-logging)
9. [Access Control](#access-control)
10. [Security Hardening](#security-hardening)
11. [Compliance & Monitoring](#compliance--monitoring)

---

## Overview

The Python 3 Integration module implements a **defense-in-depth** security model with multiple layers of protection:

### Security Layers

```
┌──────────────────────────────────────────────────────────┐
│ Layer 1: Authentication                                  │
│ - Designer: Ignition Designer auth (built-in)           │
│ - REST API: API token validation                        │
│ - Gateway: Ignition user context                        │
└──────────────────────────────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────────────┐
│ Layer 2: Authorization (Security Modes)                 │
│ - DESIGNER_ADMIN: Full capabilities (Designer users)    │
│ - ADMIN: Extended capabilities (API with key)           │
│ - Unauthenticated: 403 Forbidden (v4.0.0+)              │
└──────────────────────────────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────────────┐
│ Layer 3: Code Validation (AST-based)                    │
│ - Parse code into Abstract Syntax Tree                  │
│ - Validate imports against whitelist                    │
│ - Block dangerous function calls                        │
│ - Detect evasion techniques                             │
└──────────────────────────────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────────────┐
│ Layer 4: Resource Limits & Audit Logging                │
│ - Memory: 512MB limit (prevent accidents)               │
│ - CPU: 60s timeout (prevent infinite loops)             │
│ - Code size: 1MB limit (prevent payload attacks)        │
│ - All actions logged for compliance                     │
└──────────────────────────────────────────────────────────┘
```

---

## Security Architecture

### Three-Tier Security Model

| Mode | Users | Authentication | Capabilities | Use Case |
|------|-------|----------------|--------------|----------|
| **DESIGNER_ADMIN** | Designer users | Ignition Designer login | Full Python access | Development & admin tasks |
| **ADMIN** | REST API with key | API key + HTTPS | Extended Python access | Trusted automation |

###Decision Flow

```
Request → Authentication Check
             │
             ├─ Designer User-Agent? → DESIGNER_ADMIN mode
             │
             ├─ Valid Admin API Key? → ADMIN mode
             │
             └─ No Authentication → 403 Forbidden (v4.0.0+)
                                        │
                                        ↓
                            Code Validation (AST)
                                        │
                                        ↓
                                Execute Python Code
                                        │
                                        ↓
                                  Audit Logging
```

---

## Initial Configuration

### Step 1: Verify Module Installation

After installing the module, verify it's running:

```bash
# Check Gateway logs
tail -f <ignition>/logs/wrapper.log | grep Python3

# Expected output:
# INFO  [Python3] Python3 Integration module startup
# INFO  [Python3] Python version: 3.11.2
# INFO  [Python3] Process pool initialized: 3 processes
```

### Step 2: Check Default Security Mode

By default, all requests use **RESTRICTED** mode:

```bash
# Test default security (no auth)
curl -X POST http://localhost:8088/data/python3integration/api/v1/exec \
  -H "Content-Type: application/json" \
  -d '{"code": "import math; result = math.sqrt(16)"}'

# ✅ Works - math is a safe module

curl -X POST http://localhost:8088/data/python3integration/api/v1/exec \
  -H "Content-Type: application/json" \
  -d '{"code": "import os; result = os.getcwd()"}'

# ❌ Fails - os requires ADMIN mode
```

### Step 3: Test Designer Access

Open Ignition Designer and access **Tools → Python 3 IDE**:

1. Connect to Gateway
2. Write code: `import os; print(os.getcwd())`
3. Click Execute

**Expected:** Works (Designer users have DESIGNER_ADMIN mode)

---

## Admin API Key Configuration

### Generating Secure API Key

The admin API key must be **32+ characters** for security.

**Method 1: OpenSSL (Linux/Mac)**
```bash
openssl rand -hex 32
```

**Method 2: Python**
```python
import secrets
print(secrets.token_hex(32))
```

**Method 3: PowerShell (Windows)**
```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[BitConverter]::ToString($bytes) -replace '-',''
```

**Example Output:**
```
a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2
```

### Configuring in Ignition

**Step 1: Edit ignition.conf**

Location: `<ignition>/data/ignition.conf`

Add the following line:
```properties
wrapper.java.additional.200=-Dignition.python3.admin.apikey=a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2
```

**Note:** Use an available number (200, 201, etc.). Check existing `wrapper.java.additional.N` entries.

**Step 2: Restart Gateway**

```bash
# Linux/Mac
cd <ignition>/
./gwcmd.sh -r

# Windows
cd "C:\Program Files\Inductive Automation\Ignition"
gwcmd.bat -r
```

**Step 3: Verify Configuration**

Check Gateway logs for confirmation:

```bash
tail -f logs/wrapper.log | grep "Admin API key"

# Expected output:
# INFO  [Python3SecurityService] Admin API key configured (length: 64 chars)
# INFO  [Python3SecurityService] ADMIN mode available via: Authorization: Bearer <admin-key>
# WARN  [Python3SecurityService] ADMIN mode should ONLY be used over HTTPS!
```

**Step 4: Test Admin Access**

```bash
curl -X POST https://localhost:8088/data/python3integration/api/v1/exec \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2" \
  -d '{"code": "import os; result = os.getcwd()"}'

# ✅ Should work with ADMIN mode
```

### Key Rotation

**Best Practice:** Rotate admin API keys every 90 days.

```bash
# 1. Generate new key
NEW_KEY=$(openssl rand -hex 32)

# 2. Update ignition.conf
sed -i 's/ignition.python3.admin.apikey=.*/ignition.python3.admin.apikey='$NEW_KEY'/' data/ignition.conf

# 3. Restart Gateway
./gwcmd.sh -r

# 4. Update API clients with new key
```

### Key Security Requirements

✅ **DO:**
- Use 32+ character keys
- Store keys in environment variables or secrets manager
- Rotate keys regularly
- Use HTTPS for all ADMIN mode requests
- Log all ADMIN mode usage

❌ **DON'T:**
- Hardcode keys in scripts
- Share keys via email or Slack
- Use short keys (<32 chars)
- Use HTTP for ADMIN mode (unless dev)
- Reuse keys across environments

---

## HTTPS Configuration

### Why HTTPS is Required for ADMIN Mode

ADMIN mode grants powerful capabilities (os, sys, subprocess access). HTTPS encryption ensures:

1. **Confidentiality:** API keys cannot be intercepted
2. **Integrity:** Requests cannot be tampered with
3. **Authentication:** Server identity is verified

### Enabling HTTPS in Ignition

**Step 1: Generate SSL Certificate**

**Self-Signed (Development Only):**
```bash
cd <ignition>/data/

# Generate private key
openssl genrsa -out server.key 2048

# Generate certificate
openssl req -new -x509 -key server.key -out server.crt -days 365 \
  -subj "/C=US/ST=State/L=City/O=Organization/CN=localhost"
```

**Production:** Use a certificate from a trusted Certificate Authority (CA) like:
- Let's Encrypt (free)
- DigiCert, Sectigo, etc.

**Step 2: Configure Ignition**

1. Open Gateway: `http://localhost:8088`
2. Navigate to **Config → Security → SSL/TLS**
3. Upload certificate and private key
4. Enable HTTPS on port 8043 (or custom port)
5. Restart Gateway

**Step 3: Test HTTPS**

```bash
curl https://localhost:8043/StatusPing
```

**Step 4: Update API Clients**

Update all REST API clients to use `https://` instead of `http://`:

```python
# Before
url = "http://localhost:8088/data/python3integration/api/v1/exec"

# After
url = "https://localhost:8043/data/python3integration/api/v1/exec"
```

### Disabling HTTPS Requirement (Development Only)

For development environments, you can disable the HTTPS requirement:

**In ignition.conf:**
```properties
wrapper.java.additional.201=-Dignition.python3.admin.requirehttps=false
```

**⚠️ WARNING:** Only use this in isolated development environments. NEVER in production.

---

## Process Pool Configuration

### Understanding the Process Pool

The module uses a **pool of Python processes** to handle concurrent requests:

- **Default Size:** 3 processes
- **Min Size:** 1 process
- **Max Size:** 20 processes
- **Purpose:** Isolation, concurrency, stability

### Configuring Pool Size

**Step 1: Determine Optimal Size**

Consider:
- **Concurrent Users:** How many users/systems will execute Python code simultaneously?
- **Memory:** Each process uses ~50-200MB
- **CPU Cores:** Pool size should not exceed CPU core count

**Recommended Sizes:**
- **Small System (2-4 cores):** 3-5 processes
- **Medium System (8-16 cores):** 5-10 processes
- **Large System (32+ cores):** 10-20 processes

**Step 2: Configure in ignition.conf**

```properties
wrapper.java.additional.202=-Dignition.python3.poolsize=10
```

**Step 3: Restart Gateway**

```bash
./gwcmd.sh -r
```

**Step 4: Verify Pool Size**

```bash
curl http://localhost:8088/data/python3integration/api/v1/pool-stats

# Expected output:
{
  "totalSize": 10,
  "healthy": 10,
  "available": 10,
  "inUse": 0
}
```

### Monitoring Pool Health

**Check Pool Stats via API:**
```bash
curl http://localhost:8088/data/python3integration/api/v1/pool-stats | jq
```

**Check via Gateway Logs:**
```bash
tail -f logs/wrapper.log | grep "Pool stats"
```

**Healthy Pool Indicators:**
- `healthy` == `totalSize` (all processes healthy)
- `available` > 0 (processes available for requests)
- `inUse` < `totalSize` (not all processes busy)

**Unhealthy Pool Indicators:**
- `healthy` < `totalSize` (some processes dead)
- `available` == 0 (all processes busy)
- Frequent process restarts in logs

### Troubleshooting Pool Issues

**Problem:** Pool exhausted (`available` = 0)

**Solutions:**
1. Increase pool size
2. Optimize slow Python scripts
3. Check for infinite loops
4. Review execution times in audit logs

**Problem:** Unhealthy processes (`healthy` < `totalSize`)

**Solutions:**
1. Check Gateway logs for Python errors
2. Verify Python installation is valid
3. Check system resources (memory, disk)
4. Restart Gateway to recreate pool

---

## Resource Limits

### Memory Limits

**Default:** 512MB per Python process

**Purpose:** Prevent memory exhaustion attacks

**Configuration:**
```properties
wrapper.java.additional.203=-DPYTHON3_MAX_MEMORY_MB=1024
```

**Monitoring:**
```bash
# Check memory usage
curl http://localhost:8088/data/python3integration/api/v1/diagnostics | jq '.memoryUsageMb'
```

### CPU Time Limits

**Default:** 60 seconds per execution

**Purpose:** Prevent infinite loops and CPU exhaustion

**Configuration:**
```properties
wrapper.java.additional.204=-DPYTHON3_MAX_CPU_SECONDS=120
```

**Monitoring:**
Check audit logs for execution times:
```bash
tail -f data/python3-integration/audit/audit-*.log | grep "durationMs"
```

### Code Size Limits

**Default:** 1MB per request

**Purpose:** Prevent payload attacks

**Non-configurable** (security hardening)

If exceeded:
```json
{
  "success": false,
  "error": "Code size exceeds maximum allowed (1MB)"
}
```

---

## Audit Logging

### Log Location

**File Location:**
```
<ignition>/data/python3-integration/audit/audit-YYYY-MM-DD.log
```

**Example:**
```
<ignition>/data/python3-integration/audit/audit-2025-10-20.log
```

### Log Format

```json
{
  "timestamp": "2025-10-20T15:30:45Z",
  "user": "admin",
  "sourceIP": "192.168.1.100",
  "securityMode": "DESIGNER_ADMIN",
  "codeHash": "a1b2c3d4e5f6...",
  "success": true,
  "durationMs": 125,
  "endpoint": "REST:/api/v1/exec"
}
```

### Configuring Audit Logging

**Enable/Disable:**
```properties
# Audit logging is always enabled (non-configurable for compliance)
```

**Log Retention:**
```bash
# Keep logs for 90 days
find <ignition>/data/python3-integration/audit/ -name "audit-*.log" -mtime +90 -delete
```

**Log Rotation:**
Logs automatically rotate daily. Old logs remain until manually deleted.

### Reviewing Audit Logs

**View recent executions:**
```bash
tail -n 100 data/python3-integration/audit/audit-$(date +%Y-%m-%d).log | jq
```

**Search for failed executions:**
```bash
grep '"success":false' data/python3-integration/audit/audit-*.log
```

**Search by user:**
```bash
grep '"user":"admin"' data/python3-integration/audit/audit-*.log
```

**Search by security mode:**
```bash
grep '"securityMode":"ADMIN"' data/python3-integration/audit/audit-*.log
```

**Count executions today:**
```bash
cat data/python3-integration/audit/audit-$(date +%Y-%m-%d).log | wc -l
```

### Forwarding Logs to SIEM

**Splunk Example:**
```bash
# Add to Splunk inputs.conf
[monitor://<ignition>/data/python3-integration/audit/*.log]
sourcetype = python3_audit
index = security
```

**ELK Stack (Elasticsearch) Example:**
```bash
# Filebeat configuration
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /path/to/ignition/data/python3-integration/audit/*.log
    json.keys_under_root: true
```

---

## Access Control

### Designer Users (DESIGNER_ADMIN Mode)

**Who Can Access:**
Any user who can open the Ignition Designer.

**Controlling Access:**
1. **Ignition User Roles:** Restrict Designer access via Ignition user management
2. **Network Firewall:** Limit who can connect to Gateway
3. **VPN:** Require VPN for Designer access

**Best Practice:**
Only grant Designer access to trusted administrators.

### REST API Users (ADMIN/RESTRICTED Mode)

**ADMIN Mode Access Control:**
1. **API Key:** Only share with trusted systems
2. **HTTPS:** Enforce HTTPS for all ADMIN requests
3. **IP Whitelist:** Restrict API access by IP (firewall level)
4. **Rate Limiting:** 100 requests/min per IP (built-in)

**RESTRICTED Mode Access Control:**
- Default mode for all unauthenticated requests
- Safe modules only (cannot harm system)
- Suitable for public-facing APIs

### Gateway Script Users

**Script Console:**
Scripts executed via Ignition Script Console use **RESTRICTED** mode by default.

**To grant ADMIN access:**
This requires custom role-based logic (not currently implemented).

---

## Security Hardening

### 1. Principle of Least Privilege

**Recommendation:**
- Use RESTRICTED mode by default
- Only grant ADMIN mode when absolutely necessary
- Regularly review who has ADMIN access

### 2. Network Segmentation

**Recommendation:**
- Place Gateway on internal network
- Use firewall to restrict external access
- Require VPN for remote access

### 3. Regular Security Audits

**Monthly Tasks:**
1. Review audit logs for suspicious activity
2. Check for failed authentication attempts
3. Verify API key rotation schedule
4. Review pool stats for anomalies

**Example:**
```bash
# Count failed executions this month
grep '"success":false' data/python3-integration/audit/audit-2025-10-*.log | wc -l

# If abnormally high, investigate
```

### 4. Keep Software Updated

**Recommendation:**
- Update Python 3 regularly (`pip install --upgrade pip`)
- Update Ignition Gateway to latest version
- Update Python 3 Integration module when new versions release

### 5. Disable Unused Features

**If not using REST API:**
```properties
# Disable REST API (custom firewall rule)
# Block port 8088 for external access
```

**If not using Designer IDE:**
- Don't grant Designer access to untrusted users

---

## Compliance & Monitoring

### SOC 2 Compliance

**Type 2 Controls Supported:**
- ✅ **CC6.1:** Audit logging of all executions
- ✅ **CC6.2:** Authentication and authorization
- ✅ **CC6.6:** Code validation and sandboxing
- ✅ **CC7.2:** HTTPS encryption for sensitive data

**Evidence Collection:**
```bash
# Export audit logs for compliance review
tar -czf audit-logs-$(date +%Y-%m).tar.gz \
  data/python3-integration/audit/audit-2025-10-*.log
```

### NIST Cybersecurity Framework

**Identify:**
- ✅ Asset inventory: Python processes tracked in pool
- ✅ Risk assessment: Threat model documented

**Protect:**
- ✅ Access control: Three-tier security model
- ✅ Data security: HTTPS encryption

**Detect:**
- ✅ Anomaly detection: Monitor failed executions
- ✅ Security monitoring: Audit logs

**Respond:**
- ✅ Incident response: Revoke API keys immediately
- ✅ Forensics: Audit logs provide full execution history

**Recover:**
- ✅ Backup: Script repository stored on disk
- ✅ Recovery: Standard Gateway backup/restore

### Monitoring Dashboard

**Key Metrics to Monitor:**

1. **Execution Success Rate**
   ```bash
   # Calculate success rate
   TOTAL=$(cat audit-*.log | wc -l)
   SUCCESS=$(grep '"success":true' audit-*.log | wc -l)
   RATE=$((SUCCESS * 100 / TOTAL))
   echo "Success rate: $RATE%"
   ```

2. **Average Execution Time**
   ```bash
   # Extract execution times
   grep '"durationMs"' audit-*.log | \
     sed 's/.*"durationMs":\([0-9]*\).*/\1/' | \
     awk '{sum+=$1; count++} END {print "Average:", sum/count, "ms"}'
   ```

3. **Security Mode Usage**
   ```bash
   # Count by security mode
   echo "DESIGNER_ADMIN:"
   grep '"securityMode":"DESIGNER_ADMIN"' audit-*.log | wc -l
   echo "ADMIN:"
   grep '"securityMode":"ADMIN"' audit-*.log | wc -l
   echo "RESTRICTED:"
   grep '"securityMode":"RESTRICTED"' audit-*.log | wc -l
   ```

4. **Pool Health**
   ```bash
   # Monitor pool stats every minute
   watch -n 60 "curl -s http://localhost:8088/data/python3integration/api/v1/pool-stats | jq"
   ```

---

## Configuration Summary

### Complete ignition.conf Example

```properties
# Python 3 Integration Module Configuration

# Admin API Key (32+ characters required)
wrapper.java.additional.200=-Dignition.python3.admin.apikey=a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2

# HTTPS Enforcement (true = enforce, false = disable)
# WARNING: Only set to false in isolated development environments
wrapper.java.additional.201=-Dignition.python3.admin.requirehttps=true

# Process Pool Size (1-20)
wrapper.java.additional.202=-Dignition.python3.poolsize=5

# Resource Limits
wrapper.java.additional.203=-DPYTHON3_MAX_MEMORY_MB=512
wrapper.java.additional.204=-DPYTHON3_MAX_CPU_SECONDS=60

# Python Path (optional, auto-detected if not set)
# wrapper.java.additional.205=-Dignition.python3.path=/usr/bin/python3.11
```

### Production vs Development Configuration

**Production:**
```properties
# Production Configuration (SECURE)
wrapper.java.additional.200=-Dignition.python3.admin.apikey=<64-char-key>
wrapper.java.additional.201=-Dignition.python3.admin.requirehttps=true
wrapper.java.additional.202=-Dignition.python3.poolsize=10
wrapper.java.additional.203=-DPYTHON3_MAX_MEMORY_MB=512
wrapper.java.additional.204=-DPYTHON3_MAX_CPU_SECONDS=60
```

**Development:**
```properties
# Development Configuration (PERMISSIVE)
wrapper.java.additional.200=-Dignition.python3.admin.apikey=dev-key-at-least-32-characters-long
wrapper.java.additional.201=-Dignition.python3.admin.requirehttps=false
wrapper.java.additional.202=-Dignition.python3.poolsize=3
wrapper.java.additional.203=-DPYTHON3_MAX_MEMORY_MB=1024
wrapper.java.additional.204=-DPYTHON3_MAX_CPU_SECONDS=120
```

---

## Additional Resources

- **SECURITY_GUIDE.md** - Technical security details
- **REST_API_GUIDE.md** - REST API usage and authentication
- **DESIGNER_USER_GUIDE.md** - Designer IDE user guide
- **DEPLOYMENT_CHECKLIST.md** - Pre-production deployment checklist

---

**Questions?** Contact your security team or open an issue on GitHub.

*This guide was created for Python 3 Integration v2.15.10 - Last updated November 2025*
