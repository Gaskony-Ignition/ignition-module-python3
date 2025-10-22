package com.inductiveautomation.ignition.examples.python3.designer.managers;

import com.inductiveautomation.ignition.examples.python3.designer.ExecutionResult;
import com.inductiveautomation.ignition.examples.python3.designer.PoolStats;
import com.inductiveautomation.ignition.examples.python3.designer.Python3RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Manages Gateway connection and Python code execution.
 * Simplified version working with existing Python3RestClient API.
 *
 * v2.0.0: Extracted from Python3IDE.java monolith
 */
public class GatewayConnectionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayConnectionManager.class);

    private Python3RestClient restClient;
    private String gatewayUrl;
    private boolean isConnected;

    public GatewayConnectionManager() {
        this.restClient = null;
        this.gatewayUrl = null;
        this.isConnected = false;
    }

    /**
     * Connects to Gateway at the specified URL.
     */
    public boolean connect(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        url = url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        try {
            this.restClient = new Python3RestClient(url);
            this.gatewayUrl = url;
            this.isConnected = true;
            LOGGER.info("Connected to Gateway: {}", url);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to connect: {}", url, e);
            this.isConnected = false;
            this.restClient = null;
            return false;
        }
    }

    public void disconnect() {
        this.restClient = null;
        this.gatewayUrl = null;
        this.isConnected = false;
    }

    public boolean isConnected() {
        return isConnected && restClient != null;
    }

    public Python3RestClient getRestClient() {
        return restClient;
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    // Execution methods using real API
    public ExecutionResult executeCode(String code, Map<String, Object> variables) throws IOException {
        if (!isConnected || restClient == null) {
            throw new IllegalStateException("Not connected to Gateway");
        }
        return restClient.executeCode(code, variables);
    }

    public PoolStats getPoolStats() throws IOException {
        if (!isConnected || restClient == null) {
            throw new IllegalStateException("Not connected to Gateway");
        }
        return restClient.getPoolStats();
    }

    public String getPythonVersion() throws IOException {
        if (!isConnected || restClient == null) {
            throw new IllegalStateException("Not connected to Gateway");
        }
        return restClient.getPythonVersion();
    }

    public void setPoolSize(int size) throws IOException {
        if (!isConnected || restClient == null) {
            throw new IllegalStateException("Not connected to Gateway");
        }
        restClient.setPoolSize(size);
    }
}
