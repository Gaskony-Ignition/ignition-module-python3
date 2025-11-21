# Security Guide - Python 3 Integration Module

**Version:** 2.15.9
**Last Updated:** 2025-11-21
**Status:** Production Ready with Security Hardening

This document provides comprehensive security guidance for deploying the Python 3 Integration module in production environments.

---

## Quick Security Checklist

### Pre-Production Deployment

- [ ] HTTPS enabled on Gateway (SSL certificates configured)
- [ ] Admin API key configured (64+ characters, high entropy)
- [ ] IP whitelist configured for ADMIN mode (restrict to known IPs)
- [ ] Firewall rules updated (block unauthorized Gateway access)
- [ ] Audit logging verified (monitor `wrapper.log`)
- [ ] Test all security controls (auth, CSRF, IP whitelist, rate limiting)

---

## Security Architecture

### Multi-Layer Defense

The module implements defense-in-depth with 7 security layers:

1. **Network Security** - HTTPS, IP whitelisting
2. **Authentication** - API keys, session tokens  
3. **Authorization** - Security modes (RESTRICTED/ADMIN/DESIGNER_ADMIN)
4. **Input Validation** - AST + string-based validation
5. **Sandboxing** - Restricted builtins, import controls
6. **Resource Limits** - CPU time, memory, process pool
7. **Audit Logging** - All operations logged

### Security Modes

| Mode | Authentication | Python Capabilities | Use Case |
|------|---------------|-------------------|----------|
| RESTRICTED | ❌ None | Safe modules only | Public REST API |
| ADMIN | ✅ API Key + IP Whitelist | Extended capabilities | Trusted automation |
| DESIGNER_ADMIN | ✅ Session Token | Full capabilities | Designer IDE users |

---

## Critical Security Features (v2.15.9)

### 1. CSRF Protection

All state-changing endpoints validate CSRF tokens to prevent cross-site request forgery attacks.

**How it works:**
- Session-based requests must include `X-CSRF-Token` header
- Stateless API requests (Bearer tokens) exempt from CSRF validation  
- Tokens expire after 30 minutes of inactivity

### 2. IP Whitelisting (ADMIN Mode)

Restrict ADMIN mode access to specific IP addresses or CIDR ranges.

**Configuration:**
```properties
# ignition.conf
wrapper.java.additional.201=-Dignition.python3.admin.ip.whitelist=192.168.1.0/24,10.0.0.100
```

**Behavior:**
- ADMIN mode: Only whitelisted IPs allowed
- DESIGNER_ADMIN/RESTRICTED: All IPs allowed

### 3. Persistent Session Tokens

HMAC signing key persisted to prevent token invalidation on Gateway restart.

**Storage location:** `~/.ignition-python3/hmac-signing.key`

**Security:**
- 256-bit cryptographically secure key
- Owner-only file permissions (Unix)
- Backed up on Gateway restart

---

## Authentication Configuration

### Admin API Key (ADMIN Mode)

**Generate secure key:**
```bash
openssl rand -hex 32  # Outputs 64 hex characters
```

**Configure in ignition.conf:**
```properties
wrapper.java.additional.200=-Dignition.python3.admin.apikey=<YOUR_64_CHAR_KEY_HERE>
```

**Requirements (v2.15.9):**
- Minimum 64 characters
- Must contain 3+ character types (uppercase, lowercase, numbers, symbols)

**Security notes:**
- ⚠️ HTTPS REQUIRED in production
- 🔄 Rotate every 90 days
- 🔐 Never commit to version control

### Session Tokens (DESIGNER_ADMIN Mode)

**How it works:**
1. Designer authenticates via Ignition's auth system
2. Calls `/auth/session` to obtain token
3. Uses token in `Authorization: Bearer <token>` header
4. Token expires after 8 hours

**Features:**
- HMAC-SHA256 signed (tamper-proof)
- Persistent across Gateway restarts
- Revocable via REST API

---

## Production Deployment Best Practices

### HTTPS Configuration

**HTTPS is MANDATORY for ADMIN mode.**

1. Configure SSL certificate in Gateway: Config → Security → SSL/TLS
2. Enable HTTPS on port 8043
3. Redirect HTTP → HTTPS

**Disable HTTPS requirement (DEV ONLY):**
```properties
# INSECURE - Never use in production
wrapper.java.additional.202=-Dignition.python3.admin.requirehttps=false
```

### Network Hardening

**Recommended firewall rules:**
```bash
# Allow HTTPS from trusted networks only
iptables -A INPUT -p tcp --dport 8043 -s 192.168.1.0/24 -j ACCEPT
iptables -A INPUT -p tcp --dport 8043 -j DROP

# Block direct HTTP access
iptables -A INPUT -p tcp --dport 8088 ! -s 127.0.0.1 -j DROP
```

### Monitoring & Logging

**Monitor `wrapper.log` for:**
- Failed authentication: `Security check failed`
- IP whitelist violations: `ADMIN mode request rejected from non-whitelisted IP`
- CSRF failures: `CSRF token validation failed`
- Rate limiting: `Rate limit exceeded`

**Example audit log entries:**
```
2025-11-21 10:30:15 INFO  Python3RestEndpoints - AUDIT: PYTHON_EXEC - Code: result = 2 + 2
2025-11-21 10:30:25 WARN  Python3RestEndpoints - SECURITY: ADMIN mode request rejected from non-whitelisted IP: 203.0.113.5
```

---

## Threat Model & Mitigations

### HIGH: Arbitrary Code Execution

**Mitigations:**
- ✅ Security modes restrict unauthenticated users to safe modules
- ✅ AST validation checks syntax before execution
- ✅ String blacklisting blocks dangerous patterns
- ✅ Process isolation (separate subprocess per execution)
- ✅ Resource limits (CPU time, memory)

### HIGH: Authentication Bypass

**Mitigations:**
- ✅ 64+ character API keys with entropy validation
- ✅ Constant-time comparison (prevents timing attacks)
- ✅ HMAC-signed session tokens (tamper-proof)
- ✅ IP whitelisting (network-layer access control)
- ✅ HTTPS enforcement (prevents credential interception)

### MEDIUM: CSRF Attacks

**Mitigations:**
- ✅ CSRF tokens for all state-changing endpoints (v2.15.9)
- ✅ Session-based validation only (Bearer tokens exempt)
- ✅ 30-minute token expiration

### MEDIUM: Denial of Service

**Mitigations:**
- ✅ Rate limiting (100 requests/minute per client)
- ✅ Process pool limits (max concurrent executions)
- ✅ Request size limits (1MB max code size)
- ✅ Execution timeouts (30 seconds max)
- ✅ Memory leak protection (bounded data structures)

---

## Security Changelog

### v2.15.9 (2025-11-21) - Security Hardening

**Added:**
- CSRF protection for all state-changing endpoints
- IP whitelisting for ADMIN mode
- Persistent HMAC signing key
- Strengthened API key validation (64+ chars with entropy)
- Timing attack protection
- Memory leak fixes (CSRF tokens, rate limiters, thread pools)

**Fixed:**
- CVE fixes: commons-compress (1.24.0 → 1.27.1), slf4j (1.7.36 → 2.0.16)
- Timing attack in secureEquals()
- Memory leaks in token storage and rate limiters
- Dead code removal (shell-exec endpoint)

**Security Impact:** HIGH - Multiple critical and high-severity fixes

---

## Incident Response

### If Security Incident Detected:

1. **Immediate:** Disable module or revoke API keys
2. **Isolate:** Block attacker IP at firewall
3. **Investigate:** Review logs, identify scope
4. **Remediate:** Rotate credentials, patch vulnerabilities
5. **Prevent:** Strengthen controls, update monitoring

### Reporting Vulnerabilities

**DO NOT** create public GitHub issues for security bugs.

Contact module maintainer privately with:
- Steps to reproduce
- Impact assessment
- Suggested fixes (if any)

Allow 90 days for patch before public disclosure.

---

## References

- OWASP Top 10: https://owasp.org/Top10/
- CWE Top 25: https://cwe.mitre.org/top25/
- NIST Cybersecurity Framework: https://www.nist.gov/cyberframework

---

**Last Review:** 2025-11-21  
**Next Review:** 2026-02-21 (Quarterly)
