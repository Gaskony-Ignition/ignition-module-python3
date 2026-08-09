# Security Policy

## Supported Versions

We actively support and provide security updates for the following versions:

| Version | Supported          | Status |
| ------- | ------------------ | ------ |
| 4.3.x   | :white_check_mark: | Active development (current) |
| 4.2.x   | :white_check_mark: | Security fixes only |
| 4.1.x   | :warning:          | End of life soon |
| < 4.1   | :x:                | No longer supported |

**Recommendation:** Always use the latest 4.3.x release for best security and features.

---

## Trust model

The Python 3 Integration module exposes scripting functions like
`system.python3.exec(...)` and a REST API at `/data/python3integration/...`
that run **arbitrary Python 3 code on the Gateway host**.

**The security model mirrors Ignition's own: authoring is the boundary**
(see `docs/PROJECT_CHARTER.md` &sect;2, decided 2026-07-02). A person with
Designer access can already execute arbitrary Jython — and therefore
arbitrary code — on the Gateway; Python 3 is neither more nor less gated
than that. Runtime `system.python3.*` calls from project Jython are
therefore **allowed by default**, matching Jython's own trust level. REST
execution remains locked behind Administrator/Designer token issuance
regardless of the runtime setting (see below).

The previous "RESTRICTED" mode that purported to filter Python source
was removed in May 2026 because the AST/string-match checks in
`python_bridge.py` were trivially bypassable (see security review C13;
classic CPython escape vectors such as
`[].__class__.__mro__[1].__subclasses__()` and
`getattr(__builtins__, 'ev'+'al')` defeated every layer). Continuing to
ship the mode would have advertised a security boundary that did not
exist.

Current state (v4.3.0):

- **REST endpoints** (`/exec`, `/eval`, `/call-module`, `/call-script`,
  `/packages/install`, `/packages/uninstall`, `/distributions/install`,
  `/distributions/uninstall`) require
  an authenticated caller via Bearer session token or admin API key. The
  `/auth/session` endpoint mints `DESIGNER_ADMIN` tokens only for callers
  whose Ignition session reports the `Administrator` or `Designer` role
  (fix C14). Unauthenticated callers receive a 401/403; the previous
  silent demotion to "RESTRICTED" was removed. This path is unaffected by
  the runtime opt-out described below.
- **The Designer** communicates with the Gateway over the platform's
  authenticated module-RPC channel (v4.2.0+). Its script authoring/exec/eval
  path requires an authenticated **Designer** session
  (`ClientReqSession.isDesigner()`), independent of the runtime scripting
  opt-out below — an authenticated Designer session must always be able to
  develop and test scripts, matching the "authoring is the boundary"
  principle. Vision/Perspective runtime client sessions are rejected by this
  check even though they hold a valid Gateway session.
- **Scripting bindings** (`system.python3.exec`, `system.python3.eval`,
  `system.python3.callModule`, `system.python3.callScript`), called from
  project Jython (tag scripts, gateway events, Perspective bindings), check
  `RoleResolver.requireScriptingAllowed()` at the Jython entry-point. The
  default policy is **allow** (opt-out), matching Jython's own trust level.
  A Gateway administrator can disable `system.python3.*` fleet-wide by
  setting the system property
  `ignition.python3.scriptingFunctions.allowed=false` (or the equivalent
  `IGNITION_PYTHON3_SCRIPTING_ALLOWED` environment variable) — for example
  to shrink the Python supply-chain surface, not because Jython itself is
  any safer without it.
- **`python_bridge.py`** no longer attempts to validate Python source.
  All Python execution runs with full Python 3 capabilities, gated only
  on the Java side.

**The real runtime threat is injection, not access:** a Perspective page
must never feed end-user input into `exec`/`eval`. The documented pattern is
to author a saved script and call it with typed arguments
(`system.python3.callScript`) — the same discipline as SQL-injection
guidance.

For real isolation between users and Gateway-host privilege, deploy the
Gateway in a container or VM whose blast radius matches your trust
requirements. OS-level isolation (separate UID, seccomp, AppArmor,
container, VM) is the only meaningful security boundary for
arbitrary-code execution; the module relies on it for that boundary.

## Security Features

### Built-in Security Measures

1. **Role-Based Access Control**
   - Designer IDE integrates with Ignition security roles
   - REST execution endpoints require an Administrator/Designer-issued
     session token (C14 fix)
   - `system.python3.*` scripting functions are allowed by default
     (opt-out); an Administrator can disable them Gateway-wide (C13,
     flipped to opt-out in v4.3.0 — see Trust model above)

2. **Process Isolation**
   - Python code executes in isolated subprocess pool
   - Limited process lifetime and resource allocation
   - No direct access to Java heap or Ignition runtime classes

3. **Module Signing**
   - All releases are cryptographically signed
   - Signature verification by Ignition Gateway
   - Prevents tampering and ensures authenticity

4. **Network Security**
   - REST API access control via Ignition's security layer
   - API token authentication required
   - Session-based authentication for Designer IDE
   - CSRF protection on browser-issued requests

> **Note on the previous "Code Validation" feature:** earlier versions of
> this document listed AST-based Python syntax validation as a security
> feature. The validator (and its companion string-match filter) were
> removed in May 2026 — they could not be made non-bypassable and their
> presence implied a security guarantee the module could not honour.
> Syntax checking still runs in the Designer IDE for developer
> convenience, but it is a UX feature, not a security control.

---

## Reporting a Vulnerability

**We take security seriously.** If you discover a security vulnerability, please help us protect our users by reporting it responsibly.

### How to Report

#### Option 1: GitHub Security Advisories (Preferred)
1. Go to: https://github.com/Gaskony-Ignition/ignition-module-python3/security/advisories
2. Click **"New draft security advisory"**
3. Provide detailed information about the vulnerability
4. Submit privately - **DO NOT** create a public issue

#### Option 2: Email (Alternative)
- **Email:** [Your security contact email - to be added]
- **Subject:** `[SECURITY] Python 3 Integration Module - [Brief Description]`
- **Encryption:** PGP key available upon request

### What to Include

Please provide as much information as possible:

1. **Vulnerability Description**
   - Clear description of the security issue
   - Impact assessment (severity, scope)
   - Affected versions

2. **Reproduction Steps**
   - Step-by-step instructions to reproduce
   - Proof of concept (if available)
   - Environment details (OS, Java version, Ignition version)

3. **Potential Impact**
   - Who is affected?
   - What can an attacker do?
   - Required privileges or conditions

4. **Suggested Fix** (Optional)
   - Your recommendation for fixing the issue
   - Code patches or configuration changes

### Example Report

```markdown
**Vulnerability:** Code Injection in REST API Endpoint

**Affected Versions:** 2.10.0 - 2.11.0

**Description:**
The /api/v1/exec endpoint does not properly sanitize user input,
allowing arbitrary code execution when [specific condition].

**Steps to Reproduce:**
1. Send POST request to /api/v1/exec with payload: [example]
2. Observe that [malicious behaviour occurs]

**Impact:**
Authenticated users with Designer role can execute arbitrary Python
code on the Gateway server, potentially accessing system resources.

**Suggested Fix:**
Add input validation before execution in Python3RestEndpoints.java
[specific code suggestion]
```

---

## Response Timeline

We are committed to responding promptly to security reports:

| Stage | Timeline |
|-------|----------|
| **Initial Response** | Within 48 hours |
| **Validation** | Within 1 week |
| **Fix Development** | 2-4 weeks (depending on severity) |
| **Patch Release** | Within 4 weeks for critical issues |
| **Public Disclosure** | After patch is released and users notified |

### Severity Levels

**Critical:** Remote code execution, privilege escalation, data breach
- **Response:** Immediate (24-48 hours)
- **Fix:** Emergency patch within 1 week

**High:** Authentication bypass, significant security weakness
- **Response:** Within 1 week
- **Fix:** Patch within 2-3 weeks

**Medium:** Information disclosure, minor security issues
- **Response:** Within 2 weeks
- **Fix:** Included in next regular release

**Low:** Best practice violations, hardening opportunities
- **Response:** Within 1 month
- **Fix:** Included in future release

---

## Disclosure Policy

### Coordinated Disclosure

We follow **responsible disclosure** practices:

1. **Private Reporting:** Security issues reported privately
2. **Fix Development:** Patch developed in private repository
3. **User Notification:** Security advisory sent to users before public disclosure
4. **Public Disclosure:** After 90 days OR after patch release (whichever is sooner)
5. **Credit:** Security researchers credited (if desired) in release notes

### What We Ask From You

- **Give us time to fix:** Please allow at least 90 days before public disclosure
- **Don't exploit:** Do not exploit the vulnerability beyond proof-of-concept
- **Keep it private:** Do not share details with others until we release a fix
- **Cooperate:** Work with us to understand and verify the issue

### What We Promise

- **Timely response:** We will acknowledge your report within 48 hours
- **Regular updates:** We will keep you informed of progress
- **Credit:** We will credit you in the security advisory (if you wish)
- **No legal action:** We will not pursue legal action against security researchers acting in good faith

---

## Security Best Practices

### For Module Users

1. **Keep Updated**
   - Always use the latest version of the module
   - Subscribe to security advisories
   - Test updates in non-production first

2. **Access Control**
   - Limit Designer IDE access to trusted users
   - The previous `RESTRICTED` mode is no longer available as of v4.0.0 — its AST-based filter was bypassable. Use OS-level isolation (container, VM, jail) when running Python from untrusted callers
   - Regularly audit user permissions

3. **Network Security**
   - Use HTTPS for Gateway connections
   - Restrict REST API access to trusted networks
   - Enable Ignition's built-in authentication

4. **Monitor Activity**
   - Review Gateway logs regularly
   - Monitor for unusual Python execution patterns
   - Set up alerts for security events

5. **Configuration Hardening**
   - Disable unused REST endpoints if possible
   - Limit process pool size based on needs
   - Set appropriate execution timeouts

### For Module Developers

1. **Code Review**
   - All code changes must be reviewed
   - Security-critical changes require extra scrutiny
   - Use static analysis tools (Checkstyle, SpotBugs)

2. **Testing**
   - Write security-focused unit tests
   - Test authentication and authorization
   - Validate input sanitization

3. **Dependencies**
   - Keep dependencies updated (Dependabot enabled)
   - Review dependency security advisories
   - Audit transitive dependencies

4. **Secure Coding**
   - Validate all user input
   - Sanitize output before display
   - Use parameterized queries (if applicable)
   - Follow OWASP guidelines

---

## Known Security Considerations

### Design Limitations

1. **Python Code Execution**
   - The module executes arbitrary Python code by design
   - Users with the Administrator/Designer role can execute any Python
     code on the Gateway host
   - Recommendation: limit Administrator/Designer roles to trusted users
     and isolate the Gateway host (container/VM) so that "execute any
     Python" maps to "compromise this Gateway host" and nothing more

2. **Process Isolation**
   - Python subprocesses run with Gateway user privileges
   - Subprocesses can access Gateway file system
   - Recommendation: use a dedicated service account with limited
     permissions; deploy the Gateway in a container/VM

3. **REST API Access**
   - REST API allows remote code execution
   - Now requires an authenticated session token; unauthenticated
     callers receive 401/403 (C13 / C14 fixes)
   - Recommendation: rotate session tokens regularly; restrict admin API
     key access to trusted operators

4. **Audit log does not capture caller identity (found 07/08/2026, README screenshot review)**
   - `Python3SecurityUtils.getCurrentUser()` and `getSourceIP()` are unimplemented
     stubs (`gateway/src/main/java/com/gaskony/python3/gateway/Python3SecurityUtils.java`)
     that always return `null`. Every `Python3AuditEvent` — REST, Designer RPC,
     and Gateway web UI alike — is therefore written with `user=UNAUTHENTICATED`,
     `sourceIP=LOCAL`, regardless of who actually authenticated.
   - This is **not** an authorisation bypass: the real gates
     (`Python3RpcHandler.requireDesignerSession()`, role-based `SecurityMode`
     mapping, and REST's 401/403 checks) are separate code and were confirmed
     independently to be enforcing (anonymous `GET /health` returns 401 on the
     v4.6.1 test gateway). It is an audit-**attribution** gap: the trail proves
     *that* privileged code ran and *what* ran, but not *which* authenticated
     user ran it.
   - Recommendation until fixed: correlate `python3-audit-*.log` entries with the
     Gateway's own access/session logs by timestamp if per-user attribution is
     needed for compliance.

### Mitigations Implemented

- Java-side **Administrator role gate** on every REST and scripting
  entry-point that executes Python source (C13/C14)
- Process pool limits prevent resource exhaustion
- Execution timeouts prevent infinite loops
- Module signing prevents tampering
- Role-based access control integration (Ignition `WebUiSession`)
- Pip argument-injection hardening (PEP-503 regex + `--` separator) on
  package install/uninstall (B2)

> **Removed:** the AST-based "RESTRICTED" sandbox and the legacy
> `system.python3.execShell` shell-injection sink (C13/C16). Both
> claimed boundaries that were trivially bypassable; their presence
> implied guarantees the module could not honour.

---

## Security Updates

Security updates are released as patch versions and announced via:

1. **GitHub Security Advisories:** https://github.com/Gaskony-Ignition/ignition-module-python3/security/advisories
2. **Release Notes:** [CHANGELOG.md](CHANGELOG.md)
3. **Email Notifications:** (If you have starred/watched the repository)

### Subscribing to Security Updates

To receive security notifications:
1. Go to: https://github.com/Gaskony-Ignition/ignition-module-python3
2. Click **"Watch"** → **"Custom"**
3. Check **"Security alerts"**
4. Click **"Apply"**

---

## Security Audit History

| Date | Auditor | Scope | Findings |
|------|---------|-------|----------|
| 2025-10-28 | Internal | Code review (v2.11.0) | No critical issues |
| 2025-10-22 | Internal | Security fixes (v2.9.0) | Issues addressed |
| - | - | External audit pending | - |

**Note:** Comprehensive third-party security audit recommended for production deployments.

---

## Security Resources

### Internal Documentation
- [Security Guide](docs/security/SECURITY_OVERVIEW.md)
- [Security Configuration](docs/security/SECURITY_CONFIG.md)
- [Security Audit Checklist](docs/security/AUDIT_CHECKLIST.md)

### External Resources
- **OWASP Top 10:** https://owasp.org/www-project-top-ten/
- **Ignition Security:** https://docs.inductiveautomation.com/docs/8.3/platform/security
- **CVE Database:** https://cve.mitre.org/

---

## Contact

For non-security issues, please use:
- **GitHub Issues:** https://github.com/Gaskony-Ignition/ignition-module-python3/issues
- **Discussions:** https://github.com/Gaskony-Ignition/ignition-module-python3/discussions

For security vulnerabilities, use the methods described in [Reporting a Vulnerability](#reporting-a-vulnerability).

---

**Security Policy Version:** 1.0
**Last Updated:** 2025-10-28
**Next Review:** 2026-01-28 (Quarterly review)
