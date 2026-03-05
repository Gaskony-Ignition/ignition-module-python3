# Python 3 Integration - Learnings

Lessons learned during development. Reference this to avoid repeating past mistakes.

## Security
- X-Source header CSRF bypass was a critical vulnerability (removed v3.8.0) - never trust client headers for auth
- /auth/session must require checkExecutePermission, not RouteAccess.GRANTED
- IP whitelisting must validate CIDR ranges properly
- AST validation for Python code prevents code injection in RESTRICTED mode

## Build System
- JaCoCo on designer subproject fails (no real tests) - exclude designer from check task
- SpotBugs ignoreFailures=true needed for designer scope (pre-existing EI2 warnings)
- commons-compress must be kept current (CVE-2024-25710, CVE-2024-26308)
- Flatten from python3-integration/ subdirectory to root required updating all paths
- sign.props.template provides safe template for signing credentials

## Designer IDE
- Python3IDE.java is 3800+ lines - decomposition deferred but should be done eventually
- Swing theme application must use Themeable interface + ComponentThemeHelper
- Version fallback in DesignerHook must be updated manually each release
- macOS wheel bundling not implemented (users must install packages manually)

## Ignition SDK
- Route mounting uses RouteGroup pattern with AccessControl
- Handler companion classes (v3.6.14+) eliminate 41x boilerplate repetition
- ApiEndpoints and JsonFields constants in common scope prevent magic strings
- Process pool borrow must always return in finally block

## Common Mistakes to Avoid
- Never commit gradle.properties or sign.props
- Always use parameterised SLF4J logging
- syncVersion must be run before releases (3 version.properties files)
- CI/CD was disabled since v2.11.0 due to free tier limits - now uses release-only workflow
