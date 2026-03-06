# Python 3 Integration - Skills

Knowledge base for the Python 3 Integration module. Reference this to understand patterns and conventions.

## Architecture
- Gateway scope: Python subprocess pool, REST API, security services (41 classes)
- Designer scope: Swing-based IDE with syntax highlighting (65 classes)
- Common scope: Shared constants - ApiEndpoints, JsonFields, PoolConfig (5 classes)
- React UI: Gateway web interface with CodeMirror 6 editor (31 components)

## Key Patterns
- Handler companion classes isolate route logic: ExecutionHandlers, ScriptAndPackageHandlers, MonitoringHandlers
- Three-tier security model: RESTRICTED (safe modules), ADMIN (authenticated), DESIGNER_ADMIN (full access)
- Process pool with BlockingQueue, health checking, and adaptive sizing
- CSRF protection with token lifecycle management
- withHandler() wrapper guarantees security headers on every response
- EndpointContext holds service dependencies for handler classes

## Build System
- Gradle Kotlin DSL with io.ia.sdk.modl plugin 0.5.0
- syncVersion task copies version.properties to common/ and designer/
- OWASP dependency check enforces CVSS >= 7.0
- JaCoCo minimum 50% coverage on gateway scope (currently 51.7%)
- SpotBugs ignoreFailures=true (designer scope has pre-existing EI2 warnings)

## Testing
- 649 tests across gateway scope, all passing
- Pure Java tests for critical components (no Ignition SDK dependency needed)
- Designer scope has 0 tests (Swing UI testing framework needed)
- Awaitility used for async assertions

## Common Mistakes to Avoid
- Never commit gradle.properties or sign.props (contain signing credentials)
- Always use parameterised SLF4J logging
- Designer IDE version fallback in DesignerHook must be updated manually each release
- JaCoCo on designer subproject fails (no tests) - exclude from check task
- commons-compress must be kept current (known CVE history)
