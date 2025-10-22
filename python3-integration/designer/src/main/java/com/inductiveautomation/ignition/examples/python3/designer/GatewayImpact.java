package com.inductiveautomation.ignition.examples.python3.designer;

/**
 * Represents Gateway performance impact assessment from Python 3 module.
 * Contains impact level classification and overall health score.
 */
public class GatewayImpact {
    private String impactLevel;          // LOW, MODERATE, HIGH, CRITICAL
    private int healthScore;             // 0-100
    private String recommendation;
    private Double memoryUsageMb;        // v2.5.19: RAM usage in MB
    private Double averageCpuTimeMs;     // v2.5.19: Average CPU time in milliseconds
    private Double cpuUsagePercent;      // v2.5.21: CPU usage as percentage

    public GatewayImpact() {
    }

    public GatewayImpact(String impactLevel, int healthScore, String recommendation) {
        this.impactLevel = impactLevel;
        this.healthScore = healthScore;
        this.recommendation = recommendation;
    }

    public String getImpactLevel() {
        return impactLevel;
    }

    public void setImpactLevel(String impactLevel) {
        this.impactLevel = impactLevel;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(int healthScore) {
        this.healthScore = healthScore;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    // v2.5.19: Getters and setters for memory and CPU usage
    public Double getMemoryUsageMb() {
        return memoryUsageMb;
    }

    public void setMemoryUsageMb(Double memoryUsageMb) {
        this.memoryUsageMb = memoryUsageMb;
    }

    public Double getAverageCpuTimeMs() {
        return averageCpuTimeMs;
    }

    public void setAverageCpuTimeMs(Double averageCpuTimeMs) {
        this.averageCpuTimeMs = averageCpuTimeMs;
    }

    // v2.5.21: Getters and setters for CPU usage percentage
    public Double getCpuUsagePercent() {
        return cpuUsagePercent;
    }

    public void setCpuUsagePercent(Double cpuUsagePercent) {
        this.cpuUsagePercent = cpuUsagePercent;
    }

    @Override
    public String toString() {
        return "GatewayImpact{" +
                "impactLevel='" + impactLevel + '\'' +
                ", healthScore=" + healthScore +
                ", recommendation='" + recommendation + '\'' +
                ", memoryUsageMb=" + memoryUsageMb +
                ", averageCpuTimeMs=" + averageCpuTimeMs +
                ", cpuUsagePercent=" + cpuUsagePercent +
                '}';
    }
}
