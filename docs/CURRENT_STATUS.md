# Current Status - Python 3 Integration Module

**Date:** 2026-07-07
**Version:** v4.5.3 (released — source repo + public portal). First Maintenance-Policy patch after Done: dark-mode Script Console editor text was unreadable (RSTA syntax theme silently not applied); fixed by building the scheme programmatically, maintainer-confirmed.
**Status:** ✅ **Done** — all ten Acceptance Contract workflows confirmed ([Project Charter](PROJECT_CHARTER.md) §4); releases now governed solely by the charter's Maintenance Policy

This document tracks what's working, what's broken, and the remaining distance to
**Done** as defined by the charter's ten-workflow Acceptance Contract.

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
2. **Acceptance Contract complete (charter §4, 06/07/2026):** all ten workflows ✅ on v4.5.x, including the final maintainer visual confirms of W4 (diagnostics numbers move after running scripts) and W10 (version identity 4.5.2 in Designer, web UI, and gateway module list). The module is **Done** as the charter defines it; future releases require a Maintenance Policy trigger (workflow defect, security, Ignition compatibility, or a deliberately pulled candidate).

### Newly found (07/08/2026, README screenshot review — not yet fixed)
- **`GET /api/v1/metrics`'s execution counters are permanently frozen at 0/—,
  regardless of real load.** This feeds both the Dashboard's "Execution
  Metrics" card and the Diagnostics page's "EXECUTION METRICS" panel.
  Root cause: `Python3MetricsCollector.getMetrics()`
  (`gateway/src/main/java/com/gaskony/python3/gateway/Python3MetricsCollector.java`)
  reads its own `totalExecutions`/`successfulExecutions`/`failedExecutions`
  `AtomicLong` fields, and nothing in production code ever calls
  `recordExecution()`/`recordFailure()` on that object — confirmed by grep
  across the whole gateway module. v4.4.0 fixed the *identical* problem for
  `getGatewayImpact()` (see `Python3MetricsCollectorLiveWiringTest` and the
  code comment at `Python3MetricsCollector.java:251-256`) by sourcing counts
  from the process pool's own live `MetricsCollector` instead
  (`processPool.getMetricsCollector()`, wired via `setProcessPool()`); that
  fix was never applied to `getMetrics()`. Verified with real load: ran four
  scripts through the Gateway Web UI IDE on the `ignition-module-testing`
  gateway (v4.6.1) — the `python3-audit-*.log` and the Diagnostics "Module
  Logs" panel show the real executions, Process Pool "Active" genuinely moved
  0→1 during a run, but "Total Executions" stayed at 0 throughout. No calling
  path (REST, RPC, web UI) can move this counter without a code fix, because
  it is disconnected from every execution path, not merely gated by auth mode.
  **This means the v4.5.x "W4: diagnostics numbers move after running
  scripts" Acceptance Contract confirm above almost certainly referred to the
  Process Pool figures, not this counter** — worth a maintainer check next
  time W4 is re-run. Fix: mirror the `getGatewayImpact()` pattern inside
  `getMetrics()`.
- **Audit log never records caller identity** (`user=UNAUTHENTICATED` /
  `sourceIP=LOCAL` on every entry, all paths) — `Python3SecurityUtils
  .getCurrentUser()`/`getSourceIP()` are unimplemented stubs. Not an
  authorisation bypass (the real gates are separate code and still enforce);
  full detail and recommendation in `SECURITY.md` under "Known Security
  Considerations".

### Standing limitations (documented, accepted)
- **CI/CD disabled** (free-tier limits) — all builds/tests run locally; `release.sh` handles signing + publishing.
- **macOS packages not bundled** (won't-do per charter §7) — macOS users run `pip install` post-install.
- **Designer IDE conveniences** (autocomplete, linting) — on the charter's candidate list (§6), zero obligation.

---

## 📋 Release gate

Before any release: run the charter's ten-workflow Acceptance Contract
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

**Last Updated:** 2026-07-06 (v4.5.2 released; Acceptance Contract complete)
**Next Review:** Only on a Maintenance Policy trigger (charter §5)
