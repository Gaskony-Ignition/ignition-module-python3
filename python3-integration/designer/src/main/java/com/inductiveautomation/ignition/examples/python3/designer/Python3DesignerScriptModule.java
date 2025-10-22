package com.inductiveautomation.ignition.examples.python3.designer;

import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.designer.model.DesignerContext;
import com.inductiveautomation.ignition.examples.python3.Python3RpcFunctions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Designer-scope implementation of Python 3 script module.
 *
 * This implementation uses lazy RPC initialization to avoid blocking Designer startup.
 * The RPC connection is established on the first function call, not during module initialization.
 *
 * If the Gateway connection is not available, functions will throw descriptive errors
 * instead of causing the Designer to hang.
 */
public class Python3DesignerScriptModule {
    private static final LoggerEx LOGGER = LoggerEx.newBuilder().build(Python3DesignerScriptModule.class);

    private final DesignerContext context;
    private volatile Python3RpcFunctions rpcFunctions;
    private volatile boolean initialized = false;
    private volatile String initializationError = null;

    public Python3DesignerScriptModule(DesignerContext context) {
        this.context = context;
        LOGGER.info("Python3DesignerScriptModule created (RPC will initialize on first use)");
    }

    /**
     * Lazily initialize RPC connection to Gateway.
     *
     * This is called on the first function invocation, not during module startup.
     * If Gateway connection is not ready, an exception is thrown with a helpful message.
     */
    private synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }

        try {
            LOGGER.debug("Initializing RPC connection to Gateway...");

            // NOTE: RPC not currently used - Designer Python3IDE uses REST API for Gateway communication
            // Script console functions currently not implemented in Designer scope
            // If Designer script console support is needed, uncomment RPC initialization in GatewayHook
            initializationError = "Designer script functions use REST API via Python3IDE. Script console not supported in Designer scope.";
            LOGGER.debug(initializationError);

        } catch (Exception e) {
            initializationError = "Failed to initialize RPC connection to Gateway: " + e.getMessage();
            LOGGER.error(initializationError, e);
        } finally {
            initialized = true;
        }
    }

    /**
     * Get RPC functions, initializing if necessary.
     *
     * @return RPC functions interface
     * @throws RuntimeException if initialization failed or RPC is not available
     */
    private Python3RpcFunctions getRpcFunctions() {
        if (!initialized) {
            ensureInitialized();
        }

        if (rpcFunctions == null) {
            String errorMsg = initializationError != null
                    ? initializationError
                    : "Python 3 Integration is not available in Designer. Please connect to a Gateway with the module installed.";
            throw new RuntimeException(errorMsg);
        }

        return rpcFunctions;
    }

    public Object exec(String code, Map<String, Object> variables) {
        LOGGER.debug("exec() called in Designer scope (will execute on Gateway via RPC)");

        try {
            return getRpcFunctions().exec(code, variables != null ? variables : Collections.emptyMap());
        } catch (Exception e) {
            LOGGER.error("Failed to execute Python code via RPC", e);
            throw new RuntimeException("Failed to execute Python code on Gateway: " + e.getMessage(), e);
        }
    }

    public Object eval(String expression, Map<String, Object> variables) {
        LOGGER.debug("eval() called in Designer scope (will execute on Gateway via RPC)");

        try {
            return getRpcFunctions().eval(expression, variables != null ? variables : Collections.emptyMap());
        } catch (Exception e) {
            LOGGER.error("Failed to evaluate Python expression via RPC", e);
            throw new RuntimeException("Failed to evaluate Python expression on Gateway: " + e.getMessage(), e);
        }
    }

    public Object callModule(String moduleName, String functionName, List<Object> args) {
        LOGGER.debug("callModule() called in Designer scope (will execute on Gateway via RPC)");

        try {
            return getRpcFunctions().callModule(moduleName, functionName, args);
        } catch (Exception e) {
            LOGGER.error("Failed to call Python module function via RPC", e);
            throw new RuntimeException("Failed to call Python module function on Gateway: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getVersion() {
        try {
            return getRpcFunctions().getVersion();
        } catch (Exception e) {
            LOGGER.error("Failed to get version via RPC", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    public Map<String, Object> getPoolStats() {
        try {
            return getRpcFunctions().getPoolStats();
        } catch (Exception e) {
            LOGGER.error("Failed to get pool stats via RPC", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    public boolean isAvailable() {
        try {
            if (!initialized) {
                ensureInitialized();
            }
            return rpcFunctions != null && getRpcFunctions().isAvailable();
        } catch (Exception e) {
            LOGGER.debug("isAvailable() returned false due to: " + e.getMessage());
            return false;
        }
    }

    public String example() {
        LOGGER.debug("example() called in Designer scope (will execute on Gateway via RPC)");

        try {
            return getRpcFunctions().example();
        } catch (Exception e) {
            LOGGER.error("Failed to run example via RPC", e);
            throw new RuntimeException("Failed to run example on Gateway: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getDistributionInfo() {
        try {
            return getRpcFunctions().getDistributionInfo();
        } catch (Exception e) {
            LOGGER.error("Failed to get distribution info via RPC", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Shutdown the script module.
     * Called when Designer module is shutting down.
     */
    public void shutdown() {
        LOGGER.info("Python3DesignerScriptModule shutting down");
        rpcFunctions = null;
        initialized = false;
    }
}
