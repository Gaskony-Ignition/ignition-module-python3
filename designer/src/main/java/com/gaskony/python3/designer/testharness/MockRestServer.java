package com.gaskony.python3.designer.testharness;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Mock REST API server for Python 3 Integration module endpoints.
 * <p>
 * This embedded HTTP server simulates the Gateway's REST API endpoints
 * to allow standalone testing of the Python3IDE without a running gateway.
 * <p>
 * Mocked endpoints:
 * <ul>
 *   <li>POST /data/python3integration/api/v1/exec - Execute Python code</li>
 *   <li>POST /data/python3integration/api/v1/eval - Evaluate Python expression</li>
 *   <li>GET /data/python3integration/api/v1/health - Health check</li>
 *   <li>GET /data/python3integration/api/v1/version - Python version</li>
 *   <li>GET /data/python3integration/api/v1/pool-stats - Pool statistics</li>
 *   <li>GET /data/python3integration/api/v1/diagnostics - Diagnostics</li>
 * </ul>
 */
public class MockRestServer {
    private static final String API_BASE = "/data/python3integration/api/v1";
    private HttpServer server;
    private final int port;

    public MockRestServer(int port) {
        this.port = port;
    }

    /**
     * Start the mock REST API server.
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));

        // Register endpoint handlers
        server.createContext(API_BASE + "/exec", new ExecHandler());
        server.createContext(API_BASE + "/eval", new EvalHandler());
        server.createContext(API_BASE + "/health", new HealthHandler());
        server.createContext(API_BASE + "/version", new VersionHandler());
        server.createContext(API_BASE + "/pool-stats", new PoolStatsHandler());
        server.createContext(API_BASE + "/diagnostics", new DiagnosticsHandler());

        server.start();
        System.out.println("Mock REST server started on port " + port);
        System.out.println("Endpoints available at: http://localhost:" + port + API_BASE);
    }

    /**
     * Stop the mock REST API server.
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Mock REST server stopped");
        }
    }

    // ============================================================================
    // Endpoint Handlers
    // ============================================================================

    /**
     * POST /exec - Execute Python code
     * Returns success with mock output after simulated delay.
     */
    private static final class ExecHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
                return;
            }

            // Read request body
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("Mock /exec received: " + requestBody);

            // Simulate execution delay
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Return mock success response
            String response = """
                {
                    "success": true,
                    "output": "Mock execution successful!\\nCode executed in simulated Python environment.\\nResult: 42",
                    "executionTime": 523
                }
                """;

            sendResponse(exchange, 200, response);
        }
    }

    /**
     * POST /eval - Evaluate Python expression
     * Returns success with mock result.
     */
    private static final class EvalHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
                return;
            }

            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("Mock /eval received: " + requestBody);

            // Simulate evaluation delay
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            String response = """
                {
                    "success": true,
                    "result": 42,
                    "executionTime": 210
                }
                """;

            sendResponse(exchange, 200, response);
        }
    }

    /**
     * GET /health - Health check
     */
    private static final class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = """
                {
                    "status": "healthy",
                    "poolAvailable": true,
                    "timestamp": "%s"
                }
                """.formatted(System.currentTimeMillis());

            sendResponse(exchange, 200, response);
        }
    }

    /**
     * GET /version - Python version information
     */
    private static final class VersionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = """
                {
                    "version": "3.11.9",
                    "implementation": "CPython",
                    "versionInfo": {
                        "major": 3,
                        "minor": 11,
                        "micro": 9,
                        "releaseLevel": "final",
                        "serial": 0
                    },
                    "executable": "/usr/bin/python3 (MOCKED)"
                }
                """;

            sendResponse(exchange, 200, response);
        }
    }

    /**
     * GET /pool-stats - Process pool statistics
     */
    private static final class PoolStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = """
                {
                    "poolSize": 5,
                    "available": 4,
                    "inUse": 1,
                    "totalExecutions": 127,
                    "averageExecutionTime": 234.5,
                    "healthyExecutors": 5,
                    "unhealthyExecutors": 0
                }
                """;

            sendResponse(exchange, 200, response);
        }
    }

    /**
     * GET /diagnostics - Performance diagnostics
     */
    private static final class DiagnosticsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = """
                {
                    "totalExecutions": 127,
                    "successfulExecutions": 121,
                    "failedExecutions": 6,
                    "averageExecutionTime": 234.5,
                    "minExecutionTime": 12,
                    "maxExecutionTime": 1523,
                    "poolUtilization": "20%",
                    "uptime": "2h 34m 12s"
                }
                """;

            sendResponse(exchange, 200, response);
        }
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
