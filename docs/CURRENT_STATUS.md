# Current Status - Python 3 Integration Module

**Date:** 2026-07-02
**Version:** v4.2.0 (built + signed; awaiting install verification)
**Status:** ⚠️ Nearly done — governed by the [Project Charter](PROJECT_CHARTER.md)

This document tracks what's working, what's broken, and the remaining distance to
**Done** as defined by the charter's nine-workflow Acceptance Contract.

---

## ✅ What's Working

### Core Functionality
- ✅ Python 3 execution via subprocess pool (line-based JSON over stdin/stdout)
- ✅ Process pool management (3–20 warm processes, health checking, async startup)
- ✅ `system.python3.*` scripting functions (deny-by-default; Administrator opt-in via `ignition.python3.scriptingFunctions.allowed=true`)
- ✅ REST API (42 routes, Ignition 8.3 OpenAPI convention)
- ✅ Gateway web UI (React 18 UMD): browser IDE, script storage, PyPI package management, Python version management, diagnostics
- ✅ Multi-version Python support (install/uninstall distributions, per-version pools)
- ✅ Virtual environment support
- ✅ **Designer ↔ Gateway module RPC (new in v4.2.0)** — Project Browser and Script Console now authenticate over the Designer's own Gateway channel

### Security (post-C13/C14 model — see SECURITY.md)
- ✅ OS-level isolation is the declared boundary (no in-process sandbox claims)
- ✅ REST execution requires Administrator/Designer role (token issuance bound to real Ignition roles, C14)
- ✅ Scripting functions deny-by-default with explicit Administrator opt-in (C13)
- ✅ CSRF protection (browser sessions), IP whitelisting, per-IP rate limiting (inner limiter in `Python3RestEndpoints`)
- ✅ Structured audit logging via `Python3AuditLogger` (scripting + REST paths)
- ✅ Signed modules only; pip argument-injection hardening; tar-slip/symlink guards

> **Removed by design (do not re-add):** the `RESTRICTED` sandbox mode, AST/string
> code "validation", `InputValidator`, `ResourceLimits`, the standalone
> `RateLimiter`, and `EnhancedAuditLogger` — all removed in v4.0.0–v4.1.0 as
> bypassable, dead, or redundant. Earlier versions of this document incorrectly
> listed several of these as active features.

### Testing
- ✅ 639 tests passing (583 gateway + 56 designer), 0 failing
- ✅ Gateway instruction coverage ≥ 50% (JaCoCo gate enforced in the build — this is a floor, not a target to climb)

---

## ⚠️ Known Issues / Remaining Work to reach Done

Tracked authoritatively in [PROJECT_CHARTER.md §6](PROJECT_CHARTER.md). Summary:

1. **v4.2.0 verified in the Designer (2026-07-02):** Project Browser round-trip works (list/load/folders), version displays 4.2.0. **Found:** Script Console execution is blocked by the C13 deny-by-default property gate — the RPC path routes through `Python3ScriptModule.exec()`, which demands `ignition.python3.scriptingFunctions.allowed=true` even for an authenticated Designer session. Workaround until v4.3.0: set that property (or `IGNITION_PYTHON3_SCRIPTING_ALLOWED=true`) on the test gateway.
2. **v4.3.0 — "Native Designer" release (charter §6):** Designer exec/eval trusts the authenticated Designer session (no gateway flag); runtime `system.python3.*` default flips to allow with an admin **opt-out** property; Designer diagnostics migrate to RPC and stay; new read-only environment view (versions/packages); package/version *write* management removed from Designer; editor quality bar (styling parity, Jedi autocomplete, syntax squiggles).
3. **Integrator docs:** author-in-Designer → call-from-Perspective guide + the exec/eval injection anti-pattern warning.
4. **Docs accuracy sweep** — `docs/operations/` and `docs/security/` may still reference removed components; SECURITY.md must be updated when the runtime default flips.
5. **Full ten-workflow Acceptance Contract run** on a clean gateway, recorded in the charter.

### Standing limitations (documented, accepted)
- **CI/CD disabled** (free-tier limits) — all builds/tests run locally; `release.sh` handles signing + publishing.
- **macOS packages not bundled** (won't-do per charter §7) — macOS users run `pip install` post-install.
- **Designer IDE conveniences** (autocomplete, linting) — on the charter's candidate list (§6), zero obligation.

---

## 📋 Release gate

Before any release: run the charter's nine-workflow Acceptance Contract
([PROJECT_CHARTER.md §3](PROJECT_CHARTER.md)) instead of chasing coverage or
feature metrics. A release is justified only by the charter's Maintenance
Policy (§4): workflow defects, security, Ignition compatibility, or a
deliberately pulled candidate.

---

## 📞 Contact & Support

**Project Maintainer:** Nigel Gwork
**Repository:** https://github.com/Gaskony-Ignition/ignition-module-python3
**Issues:** https://github.com/Gaskony-Ignition/ignition-module-python3/issues

---

**Last Updated:** 2026-07-02 (v4.2.0, charter adoption)
**Next Review:** After v4.2.0 install verification and v4.3.0 Designer slimming
