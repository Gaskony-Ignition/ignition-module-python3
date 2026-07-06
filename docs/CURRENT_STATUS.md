# Current Status - Python 3 Integration Module

**Date:** 2026-07-06
**Version:** v4.5.2 (built + signed; installed & maintainer-confirmed on the live test gateway)
**Status:** ⚠️ Two acceptance boxes from Done — governed by the [Project Charter](PROJECT_CHARTER.md)

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
- ✅ 634 tests passing (628 gateway + 6 designer), 0 failing (designer count fell when the legacy IDE testharness was deleted in v4.3.3; gateway rose with v4.4.0–v4.5.2 additions)
- ✅ Gateway instruction coverage 58.3% (JaCoCo gate ≥ 50% enforced in the build — this is a floor, not a target to climb)

---

## ⚠️ Known Issues / Remaining Work to reach Done

Tracked authoritatively in [PROJECT_CHARTER.md §6](PROJECT_CHARTER.md). Summary:

1. **v4.3.0 "Native Designer" shipped, plus a run of workflow-defect fixes through v4.5.2.** Highlights since v4.3.0: clean-gateway self-provisioning (v4.3.5), web-UI package install catalogue/wheel drift (v4.3.6), numpy/pandas under the memory cap (v4.4.0), live diagnostics wiring (v4.4.0), file-backed script storage with hot-reload (v4.5.0), package install/uninstall across **all** installed Python distributions (v4.5.1), and uninstall of individually pip-installed packages (v4.5.2).
2. **Acceptance remainder (charter §4):** most workflows are ✅. Confirmed this session on v4.5.x — W2 (Python traceback on a failing script), W3 (Project Browser round-trip + file-backed storage), W8 (web-UI package install→uninstall). **Two boxes still need a maintainer visual confirm:** W4 (diagnostics numbers actually move after running scripts — rewired in v4.4.0) and W10 (version identity reads 4.5.2 in Designer, web UI, and the gateway module list).

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

**Last Updated:** 2026-07-06 (v4.5.2, pre-release review)
**Next Review:** After the W4 + W10 visual confirms and the v4.3.0→v4.5.2 release
