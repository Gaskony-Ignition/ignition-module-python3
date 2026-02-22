package com.inductiveautomation.ignition.examples.python3.gateway;

/**
 * Holds all service dependencies needed by handler companion classes.
 * Package-private: only visible within the gateway package.
 *
 * Created in v3.6.15 as part of Phase 2 refactoring of Python3RestEndpoints.
 */
class EndpointContext {
    final Python3ScriptModule scriptModule;
    final Python3ScriptRepository scriptRepository;
    final Python3PackageManager packageManager;
    final Python3SecurityService securityService;
    final Python3AuditLogger auditLogger;
    final PoolManager poolManager;
    final PythonDistributionManager distributionManager;
    final java.io.File logsDir;
    final Python3MetricsCollector metricsCollector;

    EndpointContext(
            Python3ScriptModule scriptModule,
            Python3ScriptRepository scriptRepository,
            Python3PackageManager packageManager,
            Python3SecurityService securityService,
            Python3AuditLogger auditLogger,
            PoolManager poolManager,
            PythonDistributionManager distributionManager,
            java.io.File logsDir,
            Python3MetricsCollector metricsCollector) {
        this.scriptModule = scriptModule;
        this.scriptRepository = scriptRepository;
        this.packageManager = packageManager;
        this.securityService = securityService;
        this.auditLogger = auditLogger;
        this.poolManager = poolManager;
        this.distributionManager = distributionManager;
        this.logsDir = logsDir;
        this.metricsCollector = metricsCollector;
    }
}
