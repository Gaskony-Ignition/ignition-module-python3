package com.inductiveautomation.ignition.examples.python3.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configurable resource limits for Python execution.
 *
 * Prevents resource exhaustion and DoS attacks by enforcing:
 * - Memory limits per execution
 * - CPU time limits per execution
 * - Execution timeout
 * - Code size limits
 * - Variable count and size limits
 *
 * Limits can be configured via system properties or programmatically.
 *
 * @since v2.14.0 (Phase 2 Week 5-6: Security Enhancements)
 */
public class ResourceLimits {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceLimits.class);

    // Default limits
    private static final long DEFAULT_MEMORY_LIMIT_MB = 512;           // 512 MB
    private static final long DEFAULT_CPU_TIME_LIMIT_MS = 60000;       // 60 seconds
    private static final long DEFAULT_EXECUTION_TIMEOUT_MS = 30000;    // 30 seconds
    private static final int DEFAULT_MAX_CODE_SIZE = 1_048_576;        // 1 MB
    private static final int DEFAULT_MAX_VARIABLES = 100;
    private static final int DEFAULT_MAX_VARIABLE_SIZE = 1_048_576;    // 1 MB per variable

    // Configurable limits
    private long memoryLimitMB;
    private long cpuTimeLimitMs;
    private long executionTimeoutMs;
    private int maxCodeSize;
    private int maxVariables;
    private int maxVariableSize;

    // Enforcement flags
    private boolean enforceMemoryLimit;
    private boolean enforceCpuTimeLimit;
    private boolean enforceCodeSizeLimit;
    private boolean enforceVariableLimit;

    /**
     * Create resource limits with default values.
     */
    public ResourceLimits() {
        this.memoryLimitMB = getLongProperty("ignition.python3.limit.memory.mb", DEFAULT_MEMORY_LIMIT_MB);
        this.cpuTimeLimitMs = getLongProperty("ignition.python3.limit.cputime.ms", DEFAULT_CPU_TIME_LIMIT_MS);
        this.executionTimeoutMs = getLongProperty("ignition.python3.limit.timeout.ms", DEFAULT_EXECUTION_TIMEOUT_MS);
        this.maxCodeSize = getIntProperty("ignition.python3.limit.code.size", DEFAULT_MAX_CODE_SIZE);
        this.maxVariables = getIntProperty("ignition.python3.limit.variables.count", DEFAULT_MAX_VARIABLES);
        this.maxVariableSize = getIntProperty("ignition.python3.limit.variable.size", DEFAULT_MAX_VARIABLE_SIZE);

        // Enforcement flags (default: all enabled)
        this.enforceMemoryLimit = getBooleanProperty("ignition.python3.limit.memory.enforce", true);
        this.enforceCpuTimeLimit = getBooleanProperty("ignition.python3.limit.cputime.enforce", true);
        this.enforceCodeSizeLimit = getBooleanProperty("ignition.python3.limit.code.enforce", true);
        this.enforceVariableLimit = getBooleanProperty("ignition.python3.limit.variables.enforce", true);

        LOGGER.info("ResourceLimits initialized: memory={}MB, cpuTime={}ms, timeout={}ms, maxCode={}, maxVars={}, maxVarSize={}",
            memoryLimitMB, cpuTimeLimitMs, executionTimeoutMs, maxCodeSize, maxVariables, maxVariableSize);
    }

    /**
     * Create resource limits with custom values.
     */
    public ResourceLimits(long memoryLimitMB, long cpuTimeLimitMs, long executionTimeoutMs,
                         int maxCodeSize, int maxVariables, int maxVariableSize) {
        this.memoryLimitMB = memoryLimitMB;
        this.cpuTimeLimitMs = cpuTimeLimitMs;
        this.executionTimeoutMs = executionTimeoutMs;
        this.maxCodeSize = maxCodeSize;
        this.maxVariables = maxVariables;
        this.maxVariableSize = maxVariableSize;

        this.enforceMemoryLimit = true;
        this.enforceCpuTimeLimit = true;
        this.enforceCodeSizeLimit = true;
        this.enforceVariableLimit = true;
    }

    // Validation methods

    /**
     * Validate code size.
     * @throws ResourceLimitException if code exceeds limit
     */
    public void validateCodeSize(String code) throws ResourceLimitException {
        if (!enforceCodeSizeLimit) {
            return;
        }

        if (code == null) {
            return;
        }

        int size = code.getBytes().length;
        if (size > maxCodeSize) {
            throw new ResourceLimitException(
                String.format("Code size (%d bytes) exceeds limit (%d bytes)", size, maxCodeSize)
            );
        }
    }

    /**
     * Validate variable count and sizes.
     * @throws ResourceLimitException if variables exceed limits
     */
    public void validateVariables(java.util.Map<String, Object> variables) throws ResourceLimitException {
        if (!enforceVariableLimit) {
            return;
        }

        if (variables == null || variables.isEmpty()) {
            return;
        }

        // Check variable count
        if (variables.size() > maxVariables) {
            throw new ResourceLimitException(
                String.format("Variable count (%d) exceeds limit (%d)", variables.size(), maxVariables)
            );
        }

        // Check individual variable sizes
        for (java.util.Map.Entry<String, Object> entry : variables.entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                int size = estimateObjectSize(value);
                if (size > maxVariableSize) {
                    throw new ResourceLimitException(
                        String.format("Variable '%s' size (%d bytes) exceeds limit (%d bytes)",
                            entry.getKey(), size, maxVariableSize)
                    );
                }
            }
        }
    }

    /**
     * Validate memory usage.
     * @throws ResourceLimitException if memory usage exceeds limit
     */
    public void validateMemoryUsage(long memoryUsedMB) throws ResourceLimitException {
        if (!enforceMemoryLimit) {
            return;
        }

        if (memoryUsedMB > memoryLimitMB) {
            throw new ResourceLimitException(
                String.format("Memory usage (%d MB) exceeds limit (%d MB)", memoryUsedMB, memoryLimitMB)
            );
        }
    }

    /**
     * Validate CPU time.
     * @throws ResourceLimitException if CPU time exceeds limit
     */
    public void validateCpuTime(long cpuTimeUsedMs) throws ResourceLimitException {
        if (!enforceCpuTimeLimit) {
            return;
        }

        if (cpuTimeUsedMs > cpuTimeLimitMs) {
            throw new ResourceLimitException(
                String.format("CPU time (%d ms) exceeds limit (%d ms)", cpuTimeUsedMs, cpuTimeLimitMs)
            );
        }
    }

    /**
     * Estimate object size in bytes (rough approximation).
     */
    private int estimateObjectSize(Object obj) {
        if (obj == null) {
            return 0;
        }

        if (obj instanceof String) {
            return ((String) obj).getBytes().length;
        } else if (obj instanceof byte[]) {
            return ((byte[]) obj).length;
        } else if (obj instanceof java.util.Collection) {
            int totalSize = 0;
            for (Object item : (java.util.Collection<?>) obj) {
                totalSize += estimateObjectSize(item);
            }
            return totalSize;
        } else if (obj instanceof java.util.Map) {
            int totalSize = 0;
            for (Object value : ((java.util.Map<?, ?>) obj).values()) {
                totalSize += estimateObjectSize(value);
            }
            return totalSize;
        } else {
            // For primitive wrappers and other objects, use rough estimate
            return obj.toString().getBytes().length;
        }
    }

    // Getters and setters

    public long getMemoryLimitMB() {
        return memoryLimitMB;
    }

    public void setMemoryLimitMB(long memoryLimitMB) {
        this.memoryLimitMB = memoryLimitMB;
        LOGGER.info("Memory limit updated to {} MB", memoryLimitMB);
    }

    public long getCpuTimeLimitMs() {
        return cpuTimeLimitMs;
    }

    public void setCpuTimeLimitMs(long cpuTimeLimitMs) {
        this.cpuTimeLimitMs = cpuTimeLimitMs;
        LOGGER.info("CPU time limit updated to {} ms", cpuTimeLimitMs);
    }

    public long getExecutionTimeoutMs() {
        return executionTimeoutMs;
    }

    public void setExecutionTimeoutMs(long executionTimeoutMs) {
        this.executionTimeoutMs = executionTimeoutMs;
        LOGGER.info("Execution timeout updated to {} ms", executionTimeoutMs);
    }

    public int getMaxCodeSize() {
        return maxCodeSize;
    }

    public void setMaxCodeSize(int maxCodeSize) {
        this.maxCodeSize = maxCodeSize;
        LOGGER.info("Max code size updated to {} bytes", maxCodeSize);
    }

    public int getMaxVariables() {
        return maxVariables;
    }

    public void setMaxVariables(int maxVariables) {
        this.maxVariables = maxVariables;
        LOGGER.info("Max variables updated to {}", maxVariables);
    }

    public int getMaxVariableSize() {
        return maxVariableSize;
    }

    public void setMaxVariableSize(int maxVariableSize) {
        this.maxVariableSize = maxVariableSize;
        LOGGER.info("Max variable size updated to {} bytes", maxVariableSize);
    }

    public boolean isEnforceMemoryLimit() {
        return enforceMemoryLimit;
    }

    public void setEnforceMemoryLimit(boolean enforceMemoryLimit) {
        this.enforceMemoryLimit = enforceMemoryLimit;
        LOGGER.info("Memory limit enforcement: {}", enforceMemoryLimit);
    }

    public boolean isEnforceCpuTimeLimit() {
        return enforceCpuTimeLimit;
    }

    public void setEnforceCpuTimeLimit(boolean enforceCpuTimeLimit) {
        this.enforceCpuTimeLimit = enforceCpuTimeLimit;
        LOGGER.info("CPU time limit enforcement: {}", enforceCpuTimeLimit);
    }

    public boolean isEnforceCodeSizeLimit() {
        return enforceCodeSizeLimit;
    }

    public void setEnforceCodeSizeLimit(boolean enforceCodeSizeLimit) {
        this.enforceCodeSizeLimit = enforceCodeSizeLimit;
        LOGGER.info("Code size limit enforcement: {}", enforceCodeSizeLimit);
    }

    public boolean isEnforceVariableLimit() {
        return enforceVariableLimit;
    }

    public void setEnforceVariableLimit(boolean enforceVariableLimit) {
        this.enforceVariableLimit = enforceVariableLimit;
        LOGGER.info("Variable limit enforcement: {}", enforceVariableLimit);
    }

    // Helper methods for loading from system properties

    private static long getLongProperty(String key, long defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid long property {}: {}, using default: {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    private static int getIntProperty(String key, int defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid int property {}: {}, using default: {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    private static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    @Override
    public String toString() {
        return String.format(
            "ResourceLimits{memory=%dMB, cpuTime=%dms, timeout=%dms, maxCode=%d, maxVars=%d, maxVarSize=%d}",
            memoryLimitMB, cpuTimeLimitMs, executionTimeoutMs, maxCodeSize, maxVariables, maxVariableSize
        );
    }

    /**
     * Exception thrown when resource limits are exceeded.
     */
    public static class ResourceLimitException extends Exception {
        public ResourceLimitException(String message) {
            super(message);
        }
    }
}
