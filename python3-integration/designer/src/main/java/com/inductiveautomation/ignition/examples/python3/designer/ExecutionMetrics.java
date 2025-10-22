package com.inductiveautomation.ignition.examples.python3.designer;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;

/**
 * Represents execution metrics from the Gateway diagnostics endpoint.
 *
 * v2.0.8: Created for enhanced diagnostics panel
 */
public class ExecutionMetrics {

    private final long totalExecutions;
    private final long successfulExecutions;
    private final long failedExecutions;
    private final double averageExecutionTime;
    private final double successRate;

    /**
     * Creates ExecutionMetrics from JSON response.
     *
     * @param json the JSON object from diagnostics endpoint
     */
    public ExecutionMetrics(JsonObject json) {
        this.totalExecutions = json.has("totalExecutions") ? json.get("totalExecutions").getAsLong() : 0;
        this.successfulExecutions = json.has("successfulExecutions") ? json.get("successfulExecutions").getAsLong() : 0;
        this.failedExecutions = json.has("failedExecutions") ? json.get("failedExecutions").getAsLong() : 0;
        this.averageExecutionTime = json.has("averageExecutionTime") ? json.get("averageExecutionTime").getAsDouble() : 0.0;

        // Calculate success rate
        if (this.totalExecutions > 0) {
            this.successRate = (this.successfulExecutions * 100.0) / this.totalExecutions;
        } else {
            this.successRate = 0.0;
        }
    }

    /**
     * Parses JSON string to create ExecutionMetrics.
     *
     * @param jsonString the JSON string from diagnostics endpoint
     * @return ExecutionMetrics instance
     */
    public static ExecutionMetrics fromJson(String jsonString) {
        JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
        return new ExecutionMetrics(json);
    }

    public long getTotalExecutions() {
        return totalExecutions;
    }

    public long getSuccessfulExecutions() {
        return successfulExecutions;
    }

    public long getFailedExecutions() {
        return failedExecutions;
    }

    public double getAverageExecutionTime() {
        return averageExecutionTime;
    }

    public double getSuccessRate() {
        return successRate;
    }

    @Override
    public String toString() {
        return String.format("ExecutionMetrics{total=%d, successful=%d, failed=%d, avgTime=%.2fms, successRate=%.1f%%}",
                totalExecutions, successfulExecutions, failedExecutions, averageExecutionTime, successRate);
    }
}
