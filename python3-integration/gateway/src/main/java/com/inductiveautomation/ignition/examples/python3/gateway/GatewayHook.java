package com.inductiveautomation.ignition.examples.python3.gateway;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.script.ScriptManager;
import com.inductiveautomation.ignition.common.script.hints.PropertiesFileDocProvider;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gateway hook for Python 3 Integration module.
 * Manages the lifecycle of the Python process pool and registers scripting functions.
 */
public class GatewayHook extends AbstractGatewayModuleHook {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayHook.class);

    private GatewayContext gatewayContext;
    private Python3ProcessPool processPool;  // Default pool (backward compatibility)
    private PoolManager poolManager;
    private PythonDistributionManager distributionManager;
    private Python3ScriptModule scriptModule;
    private Python3ScriptRepository scriptRepository;
    private Python3PackageManager packageManager;
    private Python3AuditLogger auditLogger;
    private Python3SecurityService securityService;

    // Configuration
    private int poolSize = 3; // Default pool size
    private boolean autoDownload = true; // Auto-download Python by default
    private String defaultPythonVersion = null; // Configured default version

    @Override
    public void setup(GatewayContext context) {
        this.gatewayContext = context;
        LOGGER.info("Python 3 Integration module setup");

        // Register Gateway Web UI navigation (v3.2.0)
        try {
            com.inductiveautomation.ignition.gateway.web.systemjs.SystemJsModule jsModule =
                new com.inductiveautomation.ignition.gateway.web.systemjs.SystemJsModule(
                    "Python3IDE",
                    "/res/python3integration/Python3IDE.js"
                );

            context.getWebResourceManager()
                .getNavigationModel()
                .getHome()
                .addCategory("Python3", cat -> cat
                    .label("Python 3")
                    .addPage("IDE", page -> page
                        .position(200)
                        .mount("/python3-ide", "Python3IDE", jsModule)
                    )
                );

            LOGGER.info("Python 3 IDE Web UI registered at /system/app/python3-ide");
        } catch (Exception e) {
            LOGGER.error("Failed to register Web UI navigation: {}", e.getMessage(), e);
        }

        // Load configuration
        loadConfiguration();

        // Initialize distribution manager
        distributionManager = new PythonDistributionManager(
                context.getSystemManager().getDataDir().toPath().resolve("python3-integration"),
                autoDownload
        );

        // Initialize script repository
        try {
            scriptRepository = new Python3ScriptRepository(
                    context.getSystemManager().getDataDir().toPath().resolve("python3-integration")
            );
            LOGGER.info("Script repository initialized");
        } catch (IOException e) {
            LOGGER.error("Failed to initialize script repository", e);
        }

        // Test servlet not implemented - testing via Designer IDE and REST API
        LOGGER.info("Testing available via Designer Python 3 IDE and REST API endpoints");
    }

    @Override
    public void startup(LicenseState licenseState) {
        LOGGER.info("Python 3 Integration module startup");

        // Initialize audit logger (v2.6.0)
        String logsDir = System.getProperty("ignition.logs.dir", "logs");
        auditLogger = new Python3AuditLogger(logsDir);
        LOGGER.info("Audit logger initialized: {}", auditLogger.getAuditLogPath());

        // Initialize security service (v2.6.0)
        securityService = new Python3SecurityService(this);
        LOGGER.info("Security service initialized");

        try {
            // Initialize security components (v2.14.0)
            ResourceLimits resourceLimits = new ResourceLimits();
            InputValidator inputValidator = new InputValidator();
            java.nio.file.Path auditDir = gatewayContext.getSystemManager().getDataDir().toPath()
                .resolve("python3-integration").resolve("audit");
            EnhancedAuditLogger enhancedAuditLogger = new EnhancedAuditLogger(auditDir);
            RateLimiter rateLimiter = new RateLimiter();

            LOGGER.info("Security components initialized:");
            LOGGER.info("  - Resource limits: {}", resourceLimits);
            LOGGER.info("  - Input validator: {} patterns", inputValidator.getPatternCount());
            LOGGER.info("  - Audit logger: {}", enhancedAuditLogger.getAuditLogDir());
            LOGGER.info("  - Rate limiter: {}", rateLimiter);

            // Load configured Python versions (v3.1.0)
            Map<String, String> configuredVersions = loadConfiguredVersions();

            // Also add any versions installed via the distribution manager
            for (String installedVersion : distributionManager.getInstalledVersions()) {
                if (!configuredVersions.containsKey(installedVersion)) {
                    String execPath = distributionManager.getVersionExecutablePath(installedVersion);
                    if (execPath != null) {
                        configuredVersions.put(installedVersion, execPath);
                        LOGGER.info("Found installed distribution: Python {} at {}", installedVersion, execPath);
                    }
                }
            }

            if (configuredVersions.isEmpty()) {
                // Single-version mode (backward compatible)
                String pythonPath = distributionManager.getPythonPath();
                String detectedVersion = detectPythonVersion(pythonPath);
                configuredVersions.put(detectedVersion, pythonPath);
                if (defaultPythonVersion == null) {
                    defaultPythonVersion = detectedVersion;
                }
                LOGGER.info("Single Python version mode: {} at {}", detectedVersion, pythonPath);
            }

            if (defaultPythonVersion == null) {
                // Use the first configured version as default
                defaultPythonVersion = configuredVersions.keySet().iterator().next();
            }

            // Initialize PoolManager (v3.1.0)
            poolManager = new PoolManager(defaultPythonVersion);

            for (Map.Entry<String, String> entry : configuredVersions.entrySet()) {
                String version = entry.getKey();
                String pythonPath = entry.getValue();

                LOGGER.info("Initializing Python {} pool (size: {}): {}", version, poolSize, pythonPath);
                try {
                    Python3ProcessPool pool = new Python3ProcessPool(
                        pythonPath, poolSize,
                        resourceLimits, inputValidator, enhancedAuditLogger, rateLimiter
                    );
                    poolManager.registerPool(version, pool, pythonPath);
                } catch (IOException e) {
                    LOGGER.error("Failed to initialize pool for Python {}: {}", version, e.getMessage());
                    // Continue with other versions
                }
            }

            if (poolManager.getPoolCount() == 0) {
                throw new IOException("No Python pools could be initialized");
            }

            // Set default pool for backward compatibility
            processPool = poolManager.getDefaultPool();

            LOGGER.info("Python pools initialized: {} version(s) available: {}",
                poolManager.getPoolCount(), poolManager.getAvailableVersions());

            // Initialize package manager with default Python path (v2.3.0)
            String defaultPythonPath = poolManager.getPythonPath(defaultPythonVersion);
            try {
                packageManager = new Python3PackageManager(
                        gatewayContext.getSystemManager().getDataDir().toPath().resolve("python3-integration"),
                        defaultPythonPath
                );
                LOGGER.info("Package manager initialized");

                // Auto-install Jedi for IDE autocomplete (v2.3.1)
                if (!packageManager.isInstalled("jedi")) {
                    LOGGER.info("Jedi not installed - installing automatically for IDE autocomplete...");
                    try {
                        Python3PackageManager.InstallResult result = packageManager.installPackage("jedi");
                        if (result.success) {
                            LOGGER.info("Jedi installed successfully - autocomplete will be available");
                        } else {
                            LOGGER.warn("Failed to auto-install Jedi: {}", result.message);
                            LOGGER.warn("IDE autocomplete may not work. Install jedi manually or download wheels.");
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to auto-install Jedi", e);
                        LOGGER.warn("IDE autocomplete may not work. Install jedi manually.");
                    }
                } else {
                    LOGGER.info("Jedi already installed - autocomplete ready");
                }

            } catch (Exception e) {
                LOGGER.error("Failed to initialize package manager", e);
            }

            LOGGER.info("Python 3 Integration module started successfully");

        } catch (IOException e) {
            LOGGER.error("Failed to initialize Python 3 process pool", e);
            LOGGER.error("Options:");
            LOGGER.error("  1. Install Python 3.8+ on this server");
            LOGGER.error("  2. Enable auto-download: -Dignition.python3.autodownload=true");
            LOGGER.error("  3. Specify Python path: -Dignition.python3.path=/path/to/python3");
            LOGGER.error("  4. Configure versions: -Dignition.python3.versions=3.10,3.11,3.12");
            // Don't throw - allow module to load but scripting functions will fail gracefully
        }
    }

    @Override
    public void shutdown() {
        LOGGER.info("Python 3 Integration module shutdown");

        // v2.5.8: Close all interactive shell sessions
        try {
            Python3InteractiveShell.closeAllSessions();
        } catch (Exception e) {
            LOGGER.error("Error closing interactive shell sessions", e);
        }

        // Shutdown enhanced audit logger from default pool (v2.14.0)
        if (processPool != null) {
            try {
                EnhancedAuditLogger enhancedAuditLogger = processPool.getAuditLogger();
                if (enhancedAuditLogger != null) {
                    enhancedAuditLogger.shutdown();
                    LOGGER.info("Enhanced audit logger shutdown complete");
                }
            } catch (Exception e) {
                LOGGER.error("Error shutting down enhanced audit logger", e);
            }
        }

        // Shutdown all process pools via PoolManager (v3.1.0)
        if (poolManager != null) {
            try {
                poolManager.shutdown();
            } catch (Exception e) {
                LOGGER.error("Error shutting down pool manager", e);
            }
        } else if (processPool != null) {
            // Fallback: single pool mode
            try {
                processPool.shutdown();
            } catch (Exception e) {
                LOGGER.error("Error shutting down process pool", e);
            }
        }

        // Shutdown audit logger (v2.6.0 - legacy)
        if (auditLogger != null) {
            try {
                auditLogger.shutdown();
            } catch (Exception e) {
                LOGGER.error("Error shutting down audit logger", e);
            }
        }

        // Shutdown static timeout executor (v2.15.9 - memory leak fix)
        try {
            Python3Executor.shutdownTimeoutExecutor();
        } catch (Exception e) {
            LOGGER.error("Error shutting down timeout executor", e);
        }

        LOGGER.info("Python 3 Integration module shutdown complete");
    }

    @Override
    public void initializeScriptManager(ScriptManager manager) {
        super.initializeScriptManager(manager);

        LOGGER.info("Registering Python 3 scripting functions");

        // Create script module with lazy access to process pool
        // The module will become available once startup() initializes the pool
        scriptModule = new Python3ScriptModule(this);

        // Register under system.python3
        manager.addScriptModule(
                "system.python3",
                scriptModule,
                new PropertiesFileDocProvider()
        );

        // Initialize REST API endpoints with script module
        Python3RestEndpoints.initialize(scriptModule);
        if (scriptRepository != null) {
            Python3RestEndpoints.setScriptRepository(scriptRepository);
        }
        if (packageManager != null) {
            Python3RestEndpoints.setPackageManager(packageManager);
        }
        LOGGER.info("REST API endpoints initialized");

        // NOTE: Designer scope exists and uses REST API for communication instead of RPC
        // RPC not required - Designer Python3IDE communicates via REST endpoints
        // If RPC is needed in the future, uncomment the following:
        // try {
        //     gatewayContext.getRPCManager().registerHandler(
        //             Constants.MODULE_ID,
        //             Python3RpcFunctions.class,
        //             scriptModule
        //     );
        //     LOGGER.info("RPC handler registered for Designer/Client access");
        // } catch (Exception e) {
        //     LOGGER.error("Failed to register RPC handler", e);
        // }

        LOGGER.info("Python 3 scripting functions registered (pool will initialize during startup)");
    }

    @Override
    public void mountRouteHandlers(RouteGroup routes) {
        // Configure REST endpoints with required services (v2.6.0)
        Python3RestEndpoints.setSecurityService(securityService);
        Python3RestEndpoints.setAuditLogger(auditLogger);

        // v2.15.5: Set process pool for subprocess monitoring
        Python3RestEndpoints.setProcessPool(processPool);

        // v3.1.0: Set pool manager for multi-version support
        Python3RestEndpoints.setPoolManager(poolManager);

        // v3.1.0: Set distribution manager for Python version installation
        Python3RestEndpoints.setDistributionManager(distributionManager);

        // Mount REST API endpoints at /data/python3integration/api/v1/* (Ignition 8.3 OpenAPI compliant)
        Python3RestEndpoints.mountRoutes(routes);
        LOGGER.info("Python3 REST API routes mounted at /data/python3integration/api/v1/");
    }

    @Override
    public Optional<String> getMountPathAlias() {
        // Use shorter alias for resources: /res/python3integration/ instead of full module ID
        return Optional.of("python3integration");
    }

    @Override
    public Optional<String> getMountedResourceFolder() {
        // Serve web UI resources from classpath at /res/python3integration/
        // Contains Python3IDE.js (webpack UMD bundle) and standalone.html
        return Optional.of("mounted");
    }

    /**
     * Load configuration from system properties or environment variables
     */
    private void loadConfiguration() {
        // Load pool size configuration
        String configuredSize = System.getProperty("ignition.python3.poolsize");
        if (configuredSize != null) {
            try {
                poolSize = Integer.parseInt(configuredSize);
                LOGGER.info("Using configured pool size: {}", poolSize);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid pool size configuration: {}, using default: {}", configuredSize, poolSize);
            }
        }

        // Load auto-download configuration
        String configuredAutoDownload = System.getProperty("ignition.python3.autodownload");
        if (configuredAutoDownload != null) {
            autoDownload = Boolean.parseBoolean(configuredAutoDownload);
            LOGGER.info("Auto-download: {}", autoDownload);
        }

        // Load default version (v3.1.0)
        defaultPythonVersion = System.getProperty("ignition.python3.default");
        if (defaultPythonVersion != null) {
            LOGGER.info("Configured default Python version: {}", defaultPythonVersion);
        }
    }

    /**
     * Load configured Python versions from system properties.
     *
     * Reads:
     *   -Dignition.python3.versions=3.10,3.11,3.12
     *   -Dignition.python3.path.3.10=/usr/bin/python3.10
     *   -Dignition.python3.path.3.11=/usr/bin/python3.11
     *   -Dignition.python3.path.3.12=/opt/python3.12/bin/python3
     *
     * @return ordered map of version -> python path (empty if not configured)
     */
    private Map<String, String> loadConfiguredVersions() {
        Map<String, String> versions = new LinkedHashMap<>();

        String versionList = System.getProperty("ignition.python3.versions");
        if (versionList == null || versionList.trim().isEmpty()) {
            LOGGER.debug("No multi-version configuration found (ignition.python3.versions not set)");
            return versions;
        }

        LOGGER.info("Multi-version configuration detected: {}", versionList);

        for (String version : versionList.split(",")) {
            String trimmed = version.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String path = System.getProperty("ignition.python3.path." + trimmed);
            if (path != null && !path.isEmpty()) {
                versions.put(trimmed, path);
                LOGGER.info("  Python {}: {}", trimmed, path);
            } else {
                LOGGER.warn("  Python {}: no path configured (ignition.python3.path.{} not set), skipping",
                    trimmed, trimmed);
            }
        }

        return versions;
    }

    /**
     * Detect the Python version from a Python executable.
     *
     * @param pythonPath path to the Python executable
     * @return version string like "3.11" (major.minor only)
     */
    private String detectPythonVersion(String pythonPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonPath, "-c",
                "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            String versionLine = reader.readLine();

            boolean exited = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (exited && process.exitValue() == 0 && versionLine != null) {
                String version = versionLine.trim();
                LOGGER.info("Detected Python version: {}", version);
                return version;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to detect Python version from {}: {}", pythonPath, e.getMessage());
        }
        return "3.11"; // Safe default
    }

    /**
     * Get the default process pool (backward compatibility).
     */
    public Python3ProcessPool getProcessPool() {
        return processPool;
    }

    /**
     * Get the pool manager for multi-version access (v3.1.0).
     */
    public PoolManager getPoolManager() {
        return poolManager;
    }

    /**
     * Get the distribution manager (for testing/debugging)
     */
    public PythonDistributionManager getDistributionManager() {
        return distributionManager;
    }

    /**
     * Get the script repository (for script management)
     */
    public Python3ScriptRepository getScriptRepository() {
        return scriptRepository;
    }

    /**
     * Get the audit logger (v2.6.0)
     */
    public Python3AuditLogger getAuditLogger() {
        return auditLogger;
    }

    /**
     * Get the security service (v2.6.0)
     */
    public Python3SecurityService getSecurityService() {
        return securityService;
    }

    /**
     * Check if Python 3 is available
     */
    public boolean isPython3Available() {
        return processPool != null && !processPool.isShutdown();
    }

    /**
     * This module is free - no license required.
     * Overrides the default behavior to ensure the module shows as "Free" not "Trial".
     *
     * @return true indicating this is a free module
     */
    @Override
    public boolean isFreeModule() {
        return true;
    }
}
