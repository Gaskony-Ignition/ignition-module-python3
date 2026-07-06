# CLAUDE.md — Python 3 Integration Module

Module-specific facts only. Shared standards live in `/modules/CLAUDE.md`; procedures
(building, versioning, testing, releasing, security, SDK patterns) live in the shared
skills under `/modules/.claude/skills/` — load the relevant skill before those tasks.

## What this module is

**Read `docs/PROJECT_CHARTER.md` first** — it is the authoritative statement of
purpose, definition of done, maintenance policy, and the permanent won't-do list.
A release is justified only by the charter's Maintenance Policy.

Bridges Ignition's Jython 2.7 to real Python 3 via a pool of warm subprocesses.
Exposes `system.python3.*` scripting functions, a REST API, a Gateway Web UI (React),
and a Designer IDE / Script Console (Java Swing).

- **Module ID**: `com.gaskony.python3`
- **Scopes**: Common (GD) + Gateway + Designer, plus `web-ui/` React bundle
- **Version**: single source of truth is `version = "..."` in `build.gradle.kts`.
  **Current Version: v4.5.2** ← auto-updated by `syncVersion`; do not edit by hand.

## Architecture in one paragraph

`GatewayHook` starts a `Python3ProcessPool` (default 3, max 20 warm processes).
Each `Python3Executor` owns one subprocess and speaks **line-based JSON** over
stdin/stdout (`python_bridge.py` is the Python side, extracted from resources at
runtime). Executors are borrowed/returned via a `BlockingQueue` — always return in
a `finally` block. A health checker thread runs every 30 s. A dedicated daemon
thread drains each subprocess's **stderr** (a v4.1.0 fix for a pipe-buffer
deadlock — do not remove it).

## Layout and single-source-of-truth files

| Path | Contents |
| ---- | -------- |
| `common/.../ApiEndpoints.java` | ALL REST route path constants |
| `common/.../JsonFields.java` | ALL JSON field name constants |
| `common/.../PoolConfig.java` | Pool sizes, timeouts, font sizes |
| `common/src/main/resources/version.properties` | Runtime version (GD classpath — must stay in common, see v4.0.1) |
| `gateway/.../Python3ProcessPool.java`, `Python3Executor.java` | Pool + subprocess protocol |
| `gateway/.../Python3RestEndpoints.java` + handler companions | REST API |
| `gateway/.../resources/python_bridge.py` | Python-side request handler |
| `designer/.../Python3ScriptConsole.java`, `managers/`, `navtree/`, `ui/` | Designer Script Console + Project Browser nodes (the legacy standalone `Python3IDE` cluster was deleted in v4.3.3) |
| `web-ui/` | Gateway Web UI (React + TS, webpack UMD) |
| `docs/` | See `docs/README.md` for the index |

Build output: `build/Python3-{version}.modl`.

## Script storage (v4.5.0+)

Saved scripts are **file-backed and gateway-global** (not Ignition project
resources): each is a `<Name>.py` (source of truth) + `<Name>.meta.json` sidecar
under `data/python3-integration/scripts/<folderPath>/`. A `WatchService` in
`Python3ScriptRepository` hot-reloads on any create/edit/delete (~1s, no module
restart). The legacy single `index.json` is auto-migrated to files on first
v4.5.0 startup (archived as `index.json.migrated-*`). Signatures are recomputed
from file contents on load, so hand-edited files always verify (filesystem write
access is the trust boundary). To add scripts out-of-band, drop `.py` files in —
see `~/Downloads/python3-demos/add-scripts-as-files.sh`.

## Module-specific gotchas

- **Never `print()` in `python_bridge.py`** — stdout is the JSON protocol; it breaks
  parsing. Requests/responses are single-line JSON; Python runs with `-u`.
- **Designer ↔ Gateway auth (v4.2.0+)**: after the C13/C14 REST hardening, ALL
  Designer↔Gateway communication goes over the authenticated **module RPC**
  channel (`Python3Rpc` in common, `Python3RpcHandler` in gateway,
  `GatewayConnection.getRpcInterface` in designer); the RPC gate requires a
  Designer session (`requireDesignerSession`). The REST API remains for the
  browser Web UI (Bearer token / session auth). As of v4.3.0 the Designer has
  NO management surfaces — packages/versions/pool control are web-UI-only
  (charter §3); the Designer's diagnostics + environment views are read-only.
- **Swing theming**: all theming via `setBackground()`/`setForeground()` and
  `ModernTheme` constants — **never `UIManager.put()`** (it polluted the whole
  Designer, fixed v3.6.12). Use `Themeable`, `ComponentThemeHelper`,
  `BaseModuleDialog`; preference keys in `PreferenceKeys`.
- **Gson**: gateway JSON uses Ignition's bundled Gson
  (`com.inductiveautomation.ignition.common.gson.*`) via `ApiResponse.success()/error()`.
- **Subprocesses**: terminate the pool in `shutdown()`; never `waitFor()` without a
  timeout.
- The old `InputValidator` sandbox was deliberately removed (v4.1.0): OS isolation +
  the Administrator role gate are the security boundary. Don't reintroduce it.

## Configuration (read in `GatewayHook.setup()`)

Python path priority: `-Dignition.python3.path` → `IGNITION_PYTHON3_PATH` env var →
OS auto-detection → `python3`. Pool size: `-Dignition.python3.poolsize` (default 3).
Set via `wrapper.java.additional.N=` lines in `ignition.conf`.

## REST API

Base path `/data/python3integration/api/v1/` (Ignition 8.3 OpenAPI convention;
appears in `/openapi.json`). Routes are declared with `ApiEndpoints.ROUTE_*`
constants and mounted in `GatewayHook`. Endpoints: `exec`, `eval`, `call-module`
(POST); `version`, `pool-stats`, `health`, `diagnostics` (GET). Details:
`docs/api/REST_API.md`.

## Releases

1. Bump `version = "..."` in `build.gradle.kts` (only place — `syncVersion` runs
   during every build and propagates to README badge, web-ui/package.json,
   version.properties, the DesignerHook fallback, and this file).
2. Add a `CHANGELOG.md` entry (full history lives there, not here).
3. Update `docs/CURRENT_STATUS.md` and `docs/CODE_COVERAGE.md` only when their
   contents actually changed (tests added, features shipped); `SECURITY.md` on
   supported-version changes. (`docs/roadmap/CONSOLIDATED_ROADMAP.md` is retired
   — the charter's §6 work list replaced it.)
4. Build and release per the `creating-releases` skill.
