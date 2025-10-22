package com.inductiveautomation.ignition.examples.python3.designer;

/**
 * Represents the result of a Python code execution via REST API.
 * This class models the JSON response from the Gateway's REST API endpoints.
 *
 * <p>Example JSON response:</p>
 * <pre>{
 *   "success": true,
 *   "result": "42",
 *   "executionTimeMs": 15,
 *   "timestamp": 1760571024031,
 *   "error": null
 * }</pre>
 */
public class ExecutionResult {
    private final boolean success;
    private final String result;
    private final String error;
    private final Long executionTimeMs;
    private final Long timestamp;

    /**
     * Constructor for successful execution.
     *
     * @param success true if execution succeeded
     * @param result the result value as a string
     * @param executionTimeMs execution time in milliseconds (optional)
     * @param timestamp execution timestamp (optional)
     */
    public ExecutionResult(boolean success, String result, Long executionTimeMs, Long timestamp) {
        this.success = success;
        this.result = result;
        this.error = null;
        this.executionTimeMs = executionTimeMs;
        this.timestamp = timestamp;
    }

    /**
     * Constructor for failed execution.
     *
     * @param success false for failed execution
     * @param error the error message
     */
    public ExecutionResult(boolean success, String error) {
        this.success = success;
        this.result = null;
        this.error = error;
        this.executionTimeMs = null;
        this.timestamp = null;
    }

    /**
     * Full constructor with all fields.
     *
     * @param success true if execution succeeded
     * @param result the result value as a string
     * @param error the error message (null if success)
     * @param executionTimeMs execution time in milliseconds (optional)
     * @param timestamp execution timestamp (optional)
     */
    public ExecutionResult(boolean success, String result, String error, Long executionTimeMs, Long timestamp) {
        this.success = success;
        this.result = result;
        this.error = error;
        this.executionTimeMs = executionTimeMs;
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getResult() {
        return result;
    }

    public String getError() {
        return error;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        if (success) {
            return String.format("ExecutionResult{success=true, result='%s', time=%dms}",
                    result, executionTimeMs != null ? executionTimeMs : 0);
        } else {
            return String.format("ExecutionResult{success=false, error='%s'}", error);
        }
    }
}
