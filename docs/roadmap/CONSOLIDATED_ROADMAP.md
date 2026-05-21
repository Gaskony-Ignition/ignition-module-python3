# Python 3 Integration Module — Consolidated Roadmap
**Current Version:** v3.9.0 (March 2026)
**Module ID:** com.gaskony.python3
**Status:** Production-ready — 649 tests passing, 51.7% gateway coverage

---

## ✅ Completed Work

### Core Foundation (through v3.0.0)
- [x] Process pool implementation (3-20 warm Python processes)
- [x] REST API with OpenAPI compliance (41 routes)
- [x] Designer IDE with modern UI (Java Swing + RSyntaxTextArea)
- [x] Script management (save, load, folders, import/export)
- [x] Syntax checking and AST-based validation
- [x] Theme system (Dark, Light)
- [x] Package management (PyPI search/install)
- [x] Virtual environment support (venv detection and VIRTUAL_ENV propagation)
- [x] Project Browser integration (Python 3 Scripts node in Designer sidebar)
- [x] Two-tier security model (DESIGNER_ADMIN, ADMIN) — RESTRICTED removed in v4.0.0
- [x] AST-based code validation with pattern detection
- [x] Resource limits enforcement (memory, CPU, code size, variables)
- [x] Enhanced audit logging (structured JSON, SHA-256 code hashing)
- [x] Rate limiting (per-user and global token bucket)
- [x] API key authentication
- [x] Metrics collection (executions, response times, percentiles, error rates)
- [x] Circuit breaker pattern (prevents cascading failures)
- [x] Alert manager (intelligent alerting with cooldown)
- [x] Enhanced health checking (liveness, performance, memory)
- [x] REST API monitoring endpoints (/monitoring/metrics, /circuit-breaker, /alerts)

### Architectural Refactoring (v3.6.13 – v3.7.1)

#### Phase A/B/C — Single Source of Truth (v3.6.13)
- [x] `ApiEndpoints.java` — 40+ REST route constants in common scope
- [x] `JsonFields.java` — 50+ JSON field name strings in common scope
- [x] `PoolConfig.java` — pool sizes, timeouts in common scope
- [x] `ApiResponse.java` — `success()`/`error()` factory methods
- [x] `PreferenceKeys.java`, `BaseModuleDialog.java`, `Themeable.java` — Designer single sources
- [x] `ComponentThemeHelper.java`, `UiComponentFactory.java` — designer utilities
- [x] Designer theme pollution fix (removed UIManager.put() from ThemeManager)

#### REST Handler Wrapper (v3.6.14)
- [x] `withHandler` / `HandlerLogic` interface applied to all 41 endpoints
- [x] Security headers guaranteed on every response
- [x] Consistent error handling eliminating all boilerplate

#### God Class Split (v3.7.0)
- [x] `Python3RestEndpoints.java` reduced from 3,177 lines to ~550 lines
- [x] `EndpointContext.java` — package-private dependency holder
- [x] `ExecutionHandlers.java` — 11 execution endpoints
- [x] `ScriptAndPackageHandlers.java` — 12 script/package endpoints
- [x] `MonitoringHandlers.java` — 19 monitoring endpoints
- [x] `GatewayHook.java` unchanged (10 static setters preserved)

#### Security Infrastructure Extraction (v3.7.1)
- [x] `CsrfProtection.java` — token generate/validate/expiry, independent of REST layer
- [x] `IpWhitelist.java` — CIDR matching, IP allow-list loading, independent of REST layer
- [x] `Python3RestEndpoints` shrunk to ~420 lines
- [x] Both classes independently unit-testable (no Ignition SDK dependency)

#### Test Coverage Phase 4 (v3.8.0)
- [x] Gateway scope coverage: 19% → 51.7% (target ≥50% achieved)
- [x] Total test count: ~200 → 649 (all passing)
- [x] New test classes: CsrfProtection, IpWhitelist, ExecutionHandlers, ScriptAndPackageHandlers, MonitoringHandlers, CircuitBreaker, AlertManager, ResourceLimits, MetricsCollector, Python3MetricsCollector, Python3RestEndpointsUtil

#### UI Consistency (v3.9.0)
- [x] Card headers: 20px bold title, 13px subtitle, accent border stroke
- [x] Card-styled wrappers for script tree and output panel
- [x] Subtitles on all section headers (Gateway Connection, Script Browser, Script Information, etc.)
- [x] Combined diagnostics + gateway logs with filter toolbar (All/Error/Warn/Info + Module Only)
- [x] `HEADER_BORDER_ACCENT` / `LIGHT_HEADER_BORDER_ACCENT` constants in `ModernTheme`

---

## 🎯 Near Term — Reach 80% Test Coverage

**Target Version:** v3.9.0 (MINOR)
**Current:** 51.7% gateway instruction coverage
**Goal:** 80%+

### Priority 1: Python3ProcessPool Tests (~300 lines)

The largest remaining untested component:

- Pool initialization and sizing
- Borrow/return executor lifecycle
- Concurrent borrowing and queuing
- Borrow timeout handling
- Health check and executor replacement
- Pool shutdown and cleanup
- Dynamic pool resizing
- Process crash recovery

**Target:** 70%+ coverage of Python3ProcessPool
**Approach:** Mock `Python3Executor`; test pool logic without spawning real processes

### Priority 2: PyPI Handler Tests (~100 lines)

Two handlers currently untested:
- `handleSearchPyPI` — partial results, error handling
- `handleGetPyPIInfo` — fetch from PyPI, mock `HttpClient`

**Target:** Happy path + error path covered

### Priority 3: Python3ScriptModule Function Tests (~200 lines)

Test scripting functions exposed to Ignition:
- `system.python3.exec()` with various inputs
- `system.python3.eval()` return values
- `system.python3.getVersion()`
- `system.python3.getPoolStats()`

**Target:** 60%+ coverage of Python3ScriptModule

### Priority 4: Branch Coverage Improvement

Current: ~36% branch coverage (below 50%)

Add tests for conditional edge cases in:
- `Python3RestEndpoints` security methods (null CSRF token, invalid IP format)
- `ExecutionHandlers` (missing fields, timeout paths)
- `IpWhitelist` (malformed CIDR, disabled mode)

---

## 🔮 Future Roadmap

### Phase 3: Enhanced Developer Experience

**Status:** Not started
**Target:** IDE productivity improvements

**Features:**
- Autocomplete/IntelliSense (Jedi integration with RSyntaxTextArea)
- Code completion popup UI
- Debugging with breakpoints
- Variable explorer/inspector
- Linting integration (pylint/flake8)
- Code formatting on save (black/autopep8)
- Performance profiling

**Notes:**
- Jedi is already installed as a package dependency
- RSyntaxTextArea has extension points for completion providers
- Breakpoint debugging would require deeper Python bridge changes

### Performance Optimization

**Status:** Future consideration

**Candidates:**
- Result caching layer (LRU, configurable TTL) for repeated executions
- Priority queue for executions (admin users get priority)
- Connection pooling for REST client (reuse HTTP connections)

### CI/CD Pipeline

**Status:** Disabled since v2.11.0 (free-tier limits)

**To Re-enable:**
1. Upgrade to paid GitHub Actions plan, OR
2. Set up self-hosted runners
3. Re-enable `.github/workflows/*.disabled` files
4. Add JaCoCo coverage threshold check to workflow

---

## 📈 Success Metrics

### v3.8.0 (Current)
- ✅ **Gateway Coverage:** 51.7% instruction coverage
- ✅ **Tests:** 649 passing, 0 failing
- ✅ **Architecture:** God class eliminated, all handlers in focused classes
- ✅ **Security:** CsrfProtection and IpWhitelist independently testable

### v3.9.0 Target
- **Gateway Coverage:** 70%+
- **Python3ProcessPool:** 70%+ coverage
- **Branch Coverage:** 50%+
- **Test Count:** ~800+

### Long-Term
- **Gateway Coverage:** 80%+
- **Branch Coverage:** 60%+
- **CI/CD:** Active pipeline with coverage gates

---

## 🔧 Development Commands

```bash
# Build
cd python3-integration
./gradlew clean build --no-daemon

# Run gateway tests with coverage
./gradlew :gateway:test jacocoTestReport

# View coverage report (gateway)
# gateway/build/reports/jacoco/test/html/index.html

# Run specific test class
./gradlew :gateway:test --tests "*CircuitBreakerTest*"
```

---

## 📚 Related Documents

- **Architecture:** `docs/architecture/OVERVIEW.md` — full module architecture
- **Coverage Report:** `CODE_COVERAGE.md` — per-class breakdown
- **Current Status:** `CURRENT_STATUS.md` — what's working, known issues
- **Testing Guide:** `docs/development/TESTING.md` — how to write tests
- **CHANGELOG:** `CHANGELOG.md` — complete release history

---

**Last Updated:** 2026-02-22
**Next Review:** Before v3.9.0 release
