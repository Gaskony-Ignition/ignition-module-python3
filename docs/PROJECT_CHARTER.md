# Project Charter — Python 3 Integration Module

**Adopted:** 2026-07-02 (v4.2.0) · **Revised:** 2026-07-02 after maintainer purpose statement
**Supersedes:** `docs/roadmap/CONSOLIDATED_ROADMAP.md` (retired)

This document is the single authoritative statement of what this project is for,
what "done" means, and what will never be built. It exists to end open-ended
improvement loops: a release is justified **only** by the Maintenance Policy below.

---

## 1. Purpose

Ignition is built on Jython 2.7 and realistically always will be. Nearly every
programmer today works in Python 3, and the packages that matter (`requests`,
`pandas`, `numpy`, …) are Python 3 only.

**This module makes Python 3 feel like a native part of any Ignition 8.3
gateway:** a developer opens the Designer, writes and tests a Python 3 script in
a first-class editor, saves it into a Project-Library-like tree, and calls it
from anywhere Jython runs — Perspective pages, tag scripts, gateway events — via
`system.python3.*`, exactly as they already do with the Jython scripting library.

## 2. Personas and trust model

The security model mirrors Ignition's own: **authoring is the boundary.**
A person with Designer access can already execute arbitrary Jython (and thus
arbitrary code) on the gateway — Python 3 must be neither more nor less gated.

| Persona | Access | What they can do |
|---------|--------|------------------|
| **Gateway administrator** | Gateway web UI (Administrator role) | Controls the *environment*: which Python versions are installed, which packages are available, pool sizing. This is the supply chain control point. |
| **Designer developer** | Designer (authenticated Designer session; may have **no** gateway web access) | Full develop/test cycle: create, edit, run, organise Python 3 scripts; see execution diagnostics and server impact; see (read-only) which versions/packages the admin has provided. |
| **Runtime caller** (Perspective page, tag/timer script, gateway event) | None directly | Executes only what developers authored — pages call saved scripts through bindings/events the developer wrote. End users never author code. |
| **External/REST caller** | REST API | Remote execution stays locked behind Administrator/Designer token issuance (C14). Unauthenticated → 401. |

**Runtime execution policy:** `system.python3.*` from project scripts is
**allowed by default** (matching Jython's trust level — a project script that
wanted to do harm can already do it in Jython). A gateway administrator can
disable it fleet-wide with `ignition.python3.scriptingFunctions.allowed=false`
(opt-out; reverses the C13 opt-in default, decided 2026-07-02).

**The real runtime threat is injection, not access:** a Perspective page must
never feed end-user input into `exec`/`eval`. The documented pattern is: author
a saved script, call it with typed arguments (`system.python3.callScript`).
This anti-pattern warning is a required part of the user documentation, exactly
like SQL-injection guidance.

## 3. Surface ownership — one owner per function

| Function | Owner | Notes |
|----------|-------|------|
| Script authoring, testing, organisation | **Designer** | IDE + Project Browser node, via authenticated module RPC |
| Execution diagnostics & server impact | **Designer** (read-only view; web UI has the full admin version) | Devs without gateway access must be able to see if their scripts hurt the server |
| Environment visibility (installed versions/packages) | **Designer read-only**; **web UI read-write** | Devs see what's available; admins change it |
| Package + Python version management (write) | **Gateway web UI** | Admin only; removed from Designer |
| Calling scripts from projects | **`system.python3.*`** | `callScript` is the primary integration point |
| Remote/external execution | **REST API** | Token-gated, unchanged |

Duplicating a *write* function on a second surface is a scope violation.

## 4. Definition of Done — the Acceptance Contract

The module is **done** when all ten workflows pass on a clean install. This is
the release gate — run it instead of chasing internal metrics.

| # | Workflow | Surface | Status 2026-07-02 |
|---|----------|---------|-------------------|
| 1 | Install signed `.modl` on clean Ignition 8.3 → pool healthy < 60 s, **zero manual config** | Gateway | ✅ (v4.2.0 test) |
| 2 | Designer Script Console: run a script → output; failing script → Python traceback — **with no gateway flags set** | Designer | ❌ blocked by C13 gate (v4.3.0 fix) |
| 3 | Project Browser "Python 3 Scripts": list/create/edit/save/delete round-trip, folders like Project Library | Designer | ✅ (v4.2.0 test) |
| 4 | Designer diagnostics: pool stats, execution timing, gateway impact visible **without gateway web access** | Designer | ❌ panel on dead REST path (v4.3.0) |
| 5 | Designer environment view: installed Python versions + packages, read-only | Designer | ❌ to build (v4.3.0) |
| 6 | `system.python3.callScript("TestFolder1/MyfirstScript")` from a Jython project script / Perspective event returns the result | Runtime | ⬜ untested |
| 7 | `system.python3.exec("result = 2 + 2")` → `4` from a project script **by default**; with opt-out property set, fails with a clear error | Runtime | ❌ default is deny (v4.3.0 flip) |
| 8 | Web UI as Administrator: log in → install a package → manage Python versions → changes appear in the Designer environment view | Admin | ⬜ partially tested |
| 9 | REST `/exec` with admin token succeeds; without token → 401; `/auth/session` refuses callers lacking Designer/Administrator role | REST | ✅ (code-verified C14) |
| 10 | Version identical in Designer, web UI, gateway module list; uninstall → zero orphaned Python processes | All | ⬜ version ✅ per v4.2.0 test; uninstall untested |

Internal quality floors (not goals): all tests pass, gateway coverage ≥ 50%
(JaCoCo gate), signed builds only, docs accurate.

### Designer editor quality bar (finite checklist, part of Done)

Developers are used to VS Code/JetBrains. "Clean and powerful" means this list —
when it's ticked, editor work **stops**:

- [x] Python syntax highlighting, dark/light themes, find/replace, keyboard shortcuts
- [ ] Visual parity with the web UI's card-based styling (one polish pass, v4.3.0)
- [ ] Autocomplete via the bundled Jedi (Ctrl+Space) — *promoted from candidate list 2026-07-02*
- [ ] Syntax-error squiggles from the existing check-syntax service (over RPC)

## 5. Maintenance Policy

After the contract passes, a new release is justified **only** by:

1. A defect in one of the ten workflows
2. A security issue
3. Compatibility with a new Ignition version
4. A candidate feature deliberately pulled from §7 (one at a time)

Everything else — refactors, polish, metric climbing — does not ship on its own.

## 6. Work remaining to reach Done

- [x] v4.2.0: Designer↔Gateway module RPC for scripts + console (verified in Designer 2026-07-02)
- [ ] **v4.3.0 — "Native Designer" release:**
  - Designer exec/eval over RPC trusts the authenticated Designer session (server-side `ClientReqSession` role check) — removes the C13 property gate from the Designer path (fixes workflow 2)
  - Flip runtime `system.python3.*` default to **allow**, property becomes opt-out (workflow 7); update SECURITY.md to match
  - Migrate Designer diagnostics to RPC and keep them (workflow 4); add read-only environment view (workflow 5)
  - Remove package/version **write** management from the Designer (web UI owns it)
  - Editor quality bar: styling parity pass, Jedi autocomplete, syntax squiggles
- [ ] **Docs for integrators:** a task-oriented guide — author a script in the Designer → call it from a Perspective button/binding/tag script — plus the injection anti-pattern warning (§2)
- [ ] Docs accuracy sweep of `docs/operations/` and `docs/security/` (remove references to deleted components)
- [ ] Run the full ten-workflow Acceptance Contract on a clean gateway; record results here

## 7. Candidate list (no commitment)

- **Linting / format-on-save** (pyflakes/black in the Designer editor)

*(Jedi autocomplete was promoted into the Done quality bar, 2026-07-02.)*

## 8. Won't-Do list (permanent)

| Item | Why not |
|------|---------|
| Debugger / breakpoints | Deep bridge surgery to imitate free, better tools |
| Multi-cursor, code folding upgrades, profiling UI | IDE-parity treadmill beyond the §4 quality bar |
| In-process Python sandboxing / RESTRICTED mode | Proven bypassable (C13); false security claims are worse than none |
| Result caching, priority execution queues | Built once, deleted as dead code in v4.1.0 |
| Horizontal scaling / Kubernetes orchestration | Not this module's job |
| 80% coverage target | Metric treadmill; ≥ 50% JaCoCo gate stays as a floor |
| macOS package bundling | Niche in SCADA; documented `pip install` workaround |
| Designer package/version **management** (write ops) | Admin function; gateway web UI owns it (§3) |

---

*Change to this charter requires the maintainer's explicit decision, recorded here with a date.*
