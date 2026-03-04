package com.inductiveautomation.ignition.examples.python3.designer;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import com.inductiveautomation.ignition.client.gateway_interface.GatewayConnection;
import com.inductiveautomation.ignition.client.gateway_interface.GatewayConnectionManager;
import com.inductiveautomation.ignition.designer.model.DesignerContext;
import com.inductiveautomation.ignition.examples.python3.ApiEndpoints;
import com.inductiveautomation.ignition.examples.python3.JsonFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API client for communicating with the Gateway's Python 3 Integration module.
 *
 * <p>This client provides methods to execute Python code, get pool statistics, and
 * retrieve diagnostics by making HTTP requests to the Gateway's REST API endpoints.</p>
 *
 * <p>All API endpoints follow the pattern: /data/python3integration/api/v1/{endpoint}</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * Python3RestClient client = new Python3RestClient(designerContext);
 * ExecutionResult result = client.executeCode("result = 2 + 2", new HashMap&lt;&gt;());
 * </pre>
 */
public class Python3RestClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(Python3RestClient.class);

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final String gatewayUrl;
    private String sessionToken;  // HMAC-signed session token (v2.9.0+)
    private long tokenExpiresAt;  // Expiration timestamp in milliseconds

    /**
     * Creates a new REST client for the Python 3 Integration module.
     *
     * @param gatewayUrl the Gateway URL (e.g., "http://localhost:9088")
     */
    public Python3RestClient(String gatewayUrl) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Remove trailing slash if present
        if (gatewayUrl != null && gatewayUrl.endsWith("/")) {
            gatewayUrl = gatewayUrl.substring(0, gatewayUrl.length() - 1);
        }

        this.gatewayUrl = gatewayUrl != null ? gatewayUrl : "http://localhost:8088";

        LOGGER.info("Python3RestClient initialized with Gateway URL: {}", this.gatewayUrl);
    }

    /**
     * Creates a new REST client using the Designer context (auto-detect Gateway URL).
     *
     * @param context the Designer context
     */
    public Python3RestClient(DesignerContext context) {
        this(buildGatewayUrl(context));
    }

    /**
     * Executes Python code on the Gateway.
     *
     * @param code the Python code to execute
     * @param variables variables to pass to the Python environment
     * @return execution result with output or error
     * @throws IOException if the HTTP request fails
     */
    public ExecutionResult executeCode(String code, Map<String, Object> variables) throws IOException {
        return executeCode(code, variables, null);
    }

    /**
     * Executes Python code on the Gateway with a specific Python version.
     *
     * @param code the Python code to execute
     * @param variables variables to pass to the Python environment
     * @param pythonVersion Python version to use (e.g., "3.11"), null for default
     * @return execution result with output or error
     * @throws IOException if the HTTP request fails
     * @since v3.1.0
     */
    public ExecutionResult executeCode(String code, Map<String, Object> variables, String pythonVersion) throws IOException {
        LOGGER.info("Executing Python code via REST API (code length: {} chars, version: {})",
            code.length(), pythonVersion != null ? pythonVersion : "default");

        // Build JSON request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("code", code);

        // v3.1.0: Add version if specified
        if (pythonVersion != null && !pythonVersion.isEmpty()) {
            requestBody.addProperty("version", pythonVersion);
        }

        // Add variables as JSON object
        JsonObject varsJson = new JsonObject();
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                addToJson(varsJson, entry.getKey(), entry.getValue());
            }
        }
        requestBody.add("variables", varsJson);

        // Make POST request to /exec endpoint
        LOGGER.info("Sending POST request to /exec endpoint");
        String response = post(ApiEndpoints.EXEC, requestBody.toString());
        LOGGER.info("Received response from /exec endpoint (length: {} chars)", response.length());

        // Parse response
        return parseExecutionResult(response);
    }

    /**
     * Evaluates a Python expression on the Gateway.
     *
     * @param expression the Python expression to evaluate
     * @param variables variables to pass to the Python environment
     * @return execution result with the expression value or error
     * @throws IOException if the HTTP request fails
     */
    public ExecutionResult evaluateExpression(String expression, Map<String, Object> variables) throws IOException {
        return evaluateExpression(expression, variables, null);
    }

    /**
     * Evaluates a Python expression on the Gateway with a specific Python version.
     *
     * @param expression the Python expression to evaluate
     * @param variables variables to pass to the Python environment
     * @param pythonVersion Python version to use (e.g., "3.11"), null for default
     * @return execution result with the expression value or error
     * @throws IOException if the HTTP request fails
     * @since v3.1.0
     */
    public ExecutionResult evaluateExpression(String expression, Map<String, Object> variables, String pythonVersion) throws IOException {
        LOGGER.debug("Evaluating Python expression via REST API: {} (version: {})",
            expression, pythonVersion != null ? pythonVersion : "default");

        // Build JSON request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("expression", expression);

        // v3.1.0: Add version if specified
        if (pythonVersion != null && !pythonVersion.isEmpty()) {
            requestBody.addProperty("version", pythonVersion);
        }

        // Add variables as JSON object
        JsonObject varsJson = new JsonObject();
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                addToJson(varsJson, entry.getKey(), entry.getValue());
            }
        }
        requestBody.add("variables", varsJson);

        // Make POST request to /eval endpoint
        String response = post(ApiEndpoints.EVAL, requestBody.toString());

        // Parse response
        return parseExecutionResult(response);
    }

    /**
     * Executes a shell command on the Gateway.
     *
     * v2.5.0: Added for Shell Command mode in Designer IDE
     *
     * @param command the shell command to execute
     * @return execution result with stdout, stderr, exit code
     * @throws IOException if the HTTP request fails
     */
    public ExecutionResult executeShellCommand(String command) throws IOException {
        LOGGER.info("Executing shell command via REST API: {}", command);

        // Build JSON request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("command", command);

        // Make POST request to /shell-exec endpoint
        LOGGER.info("Sending POST request to /shell-exec endpoint");
        String response = post(ApiEndpoints.SHELL_EXEC, requestBody.toString());
        LOGGER.info("Received response from /shell-exec endpoint (length: {} chars)", response.length());

        // Parse response
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        boolean success = json.has(JsonFields.SUCCESS) && json.get(JsonFields.SUCCESS).getAsBoolean();
        String stdout = json.has(JsonFields.STDOUT) ? json.get(JsonFields.STDOUT).getAsString() : "";
        String stderr = json.has(JsonFields.STDERR) ? json.get(JsonFields.STDERR).getAsString() : "";
        int exitCode = json.has(JsonFields.EXIT_CODE) ? json.get(JsonFields.EXIT_CODE).getAsInt() : -1;

        // Format output for display
        StringBuilder output = new StringBuilder();
        if (!stdout.isEmpty()) {
            output.append(stdout);
        }

        String error = null;
        if (!stderr.isEmpty()) {
            error = stderr;
        }

        if (!success && error == null && exitCode != 0) {
            error = "Command failed with exit code: " + exitCode;
        }

        return new ExecutionResult(success, output.toString(), error, 0L, System.currentTimeMillis());
    }

    /**
     * Creates a new interactive shell session.
     *
     * v2.5.8: Interactive shell support
     *
     * @return session ID
     * @throws IOException if the HTTP request fails
     */
    public String createInteractiveShellSession() throws IOException {
        LOGGER.info("Creating interactive shell session via REST API");

        // Build JSON request body (empty for create)
        JsonObject requestBody = new JsonObject();

        // Make POST request to /shell-interactive/create endpoint
        String response = post(ApiEndpoints.SHELL_INTERACTIVE_CREATE, requestBody.toString());

        // Parse response
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        if (json.has(JsonFields.SUCCESS) && json.get(JsonFields.SUCCESS).getAsBoolean() && json.has(JsonFields.SESSION_ID)) {
            String sessionId = json.get(JsonFields.SESSION_ID).getAsString();
            LOGGER.info("Created interactive shell session: {}", sessionId);
            return sessionId;
        } else {
            throw new IOException("Failed to create interactive shell session");
        }
    }

    /**
     * Executes a command in an interactive shell session.
     *
     * v2.5.8: Interactive shell support
     *
     * @param sessionId the session ID
     * @param command the command to execute
     * @return execution result with command output
     * @throws IOException if the HTTP request fails
     */
    public ExecutionResult executeInteractiveShellCommand(String sessionId, String command) throws IOException {
        LOGGER.info("Executing interactive shell command (session: {}): {}", sessionId, command);

        // Build JSON request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("sessionId", sessionId);
        requestBody.addProperty("command", command);

        // Make POST request to /shell-interactive/exec endpoint
        String response = post(ApiEndpoints.SHELL_INTERACTIVE_EXEC, requestBody.toString());

        // Parse response
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        boolean success = json.has(JsonFields.SUCCESS) && json.get(JsonFields.SUCCESS).getAsBoolean();
        String output = json.has(JsonFields.OUTPUT) ? json.get(JsonFields.OUTPUT).getAsString() : "";

        return new ExecutionResult(success, output, null, 0L, System.currentTimeMillis());
    }

    /**
     * Closes an interactive shell session.
     *
     * v2.5.8: Interactive shell support
     *
     * @param sessionId the session ID to close
     * @throws IOException if the HTTP request fails
     */
    public void closeInteractiveShellSession(String sessionId) throws IOException {
        LOGGER.info("Closing interactive shell session: {}", sessionId);

        // Build JSON request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("sessionId", sessionId);

        // Make POST request to /shell-interactive/close endpoint
        post(ApiEndpoints.SHELL_INTERACTIVE_CLOSE, requestBody.toString());

        LOGGER.info("Closed interactive shell session: {}", sessionId);
    }

    /**
     * Gets the current Python process pool statistics.
     *
     * @return pool statistics
     * @throws IOException if the HTTP request fails
     */
    public PoolStats getPoolStats() throws IOException {
        LOGGER.debug("Getting pool stats via REST API");

        String response = get(ApiEndpoints.POOL_STATS);

        // Parse JSON response
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        int totalSize = json.has("totalSize") ? json.get("totalSize").getAsInt() : 0;
        int healthy = json.has(JsonFields.HEALTHY) ? json.get(JsonFields.HEALTHY).getAsInt() : 0;
        int available = json.has(JsonFields.AVAILABLE) ? json.get(JsonFields.AVAILABLE).getAsInt() : 0;
        int inUse = json.has(JsonFields.IN_USE) ? json.get(JsonFields.IN_USE).getAsInt() : 0;

        return new PoolStats(totalSize, healthy, available, inUse);
    }

    /**
     * Checks if the Python 3 module is available and healthy.
     *
     * @return true if the module is healthy and available
     * @throws IOException if the HTTP request fails
     */
    public boolean isHealthy() throws IOException {
        try {
            String response = get(ApiEndpoints.HEALTH);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            return json.has(JsonFields.HEALTHY) && json.get(JsonFields.HEALTHY).getAsBoolean();
        } catch (Exception e) {
            LOGGER.warn("Health check failed", e);
            return false;
        }
    }

    /**
     * Gets comprehensive diagnostic information from the Gateway.
     *
     * @return JSON string with diagnostics
     * @throws IOException if the HTTP request fails
     */
    public String getDiagnostics() throws IOException {
        return get(ApiEndpoints.DIAGNOSTICS);
    }

    /**
     * Gets module-specific log entries from the Gateway.
     * Fetches recent log entries filtered to Python3 module messages.
     *
     * @param maxLines maximum number of log lines to return
     * @return raw JSON response string with log entries
     * @throws IOException if the HTTP request fails
     * @since v3.6.8
     */
    public String getModuleLogs(int maxLines) throws IOException {
        LOGGER.debug("Getting module logs via REST API (max {} lines)", maxLines);
        return get(ApiEndpoints.LOGS + "?lines=" + maxLines + "&filter=Python3");
    }

    /**
     * Gets the Python version from the Gateway.
     *
     * @return Python version string (e.g., "3.11.2")
     * @throws IOException if the HTTP request fails
     *
     * v2.0.14: Enhanced logging to debug version detection issues
     */
    public String getPythonVersion() throws IOException {
        LOGGER.info("getPythonVersion() - Getting Python version via REST API");

        try {
            String response = get(ApiEndpoints.VERSION);
            LOGGER.info("getPythonVersion() - Raw response: {}", response);

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            LOGGER.info("getPythonVersion() - Parsed JSON object, keys: {}", json.keySet());

            if (json.has(JsonFields.PYTHON_VERSION)) {
                String version = json.get(JsonFields.PYTHON_VERSION).getAsString();
                LOGGER.info("getPythonVersion() - Found 'pythonVersion' key: {}", version);
                return version;
            } else if (json.has(JsonFields.VERSION)) {
                String version = json.get(JsonFields.VERSION).getAsString();
                LOGGER.info("getPythonVersion() - Found 'version' key: {}", version);
                return version;
            } else {
                LOGGER.warn("getPythonVersion() - Neither 'pythonVersion' nor 'version' key found in response");
                LOGGER.warn("getPythonVersion() - Available keys: {}", json.keySet());
            }

        } catch (Exception e) {
            LOGGER.error("getPythonVersion() - Exception occurred", e);
            throw e;
        }

        LOGGER.warn("getPythonVersion() - Returning 'Unknown' (no version found)");
        return "Unknown";
    }

    /**
     * Gets available Python versions from the Gateway (v3.1.0).
     *
     * @return list of available version strings (e.g., ["3.10", "3.11", "3.12"])
     * @throws IOException if the HTTP request fails
     */
    public java.util.List<String> getAvailableVersions() throws IOException {
        LOGGER.info("Getting available Python versions via REST API");

        try {
            String response = get(ApiEndpoints.VERSIONS);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();

            java.util.List<String> versions = new java.util.ArrayList<>();
            if (json.has(JsonFields.VERSIONS) && json.get(JsonFields.VERSIONS).isJsonArray()) {
                for (var element : json.getAsJsonArray(JsonFields.VERSIONS)) {
                    versions.add(element.getAsString());
                }
            }

            LOGGER.info("Available Python versions: {}", versions);
            return versions;

        } catch (Exception e) {
            LOGGER.warn("Failed to get available versions: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Gets the default Python version from the Gateway (v3.1.0).
     *
     * @return default version string, or null
     * @throws IOException if the HTTP request fails
     */
    public String getDefaultPythonVersion() throws IOException {
        LOGGER.debug("Getting default Python version via REST API");

        try {
            String response = get(ApiEndpoints.VERSIONS);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();

            if (json.has(JsonFields.DEFAULT)) {
                return json.get(JsonFields.DEFAULT).getAsString();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get default version: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Checks Python code for syntax errors using AST parser and pyflakes.
     *
     * @param code the Python code to check
     * @return Map containing "errors" list with syntax error details
     * @throws IOException if the HTTP request fails
     */
    public Map<String, Object> checkSyntax(String code) throws IOException {
        LOGGER.debug("Checking syntax via REST API");

        // Build JSON request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("code", code);

        // Make POST request to /check-syntax endpoint
        String response = post(ApiEndpoints.CHECK_SYNTAX, requestBody.toString());

        // Parse response
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        Map<String, Object> result = new HashMap<>();
        result.put(JsonFields.SUCCESS, json.has(JsonFields.SUCCESS) && json.get(JsonFields.SUCCESS).getAsBoolean());

        // Parse errors array
        if (json.has(JsonFields.ERRORS) && json.get(JsonFields.ERRORS).isJsonArray()) {
            JsonArray errorsArray = json.getAsJsonArray(JsonFields.ERRORS);
            List<Map<String, Object>> errorsList = new ArrayList<>();

            for (int i = 0; i < errorsArray.size(); i++) {
                JsonObject errorJson = errorsArray.get(i).getAsJsonObject();

                Map<String, Object> error = new HashMap<>();
                error.put("line", errorJson.has("line") ? errorJson.get("line").getAsInt() : 1);
                error.put("column", errorJson.has("column") ? errorJson.get("column").getAsInt() : 0);
                error.put("message", errorJson.has("message") ? errorJson.get("message").getAsString() : "Syntax error");
                error.put("severity", errorJson.has("severity") ? errorJson.get("severity").getAsString() : "error");

                errorsList.add(error);
            }

            result.put("errors", errorsList);
        } else {
            result.put("errors", new ArrayList<>());
        }

        return result;
    }

    /**
     * Gets code completions at cursor position.
     *
     * @param code the Python code
     * @param line the line number (1-based)
     * @param column the column number (0-based)
     * @return list of completion results
     * @throws IOException if the HTTP request fails
     */
    public List<CompletionResult> getCompletions(String code, int line, int column) throws IOException {
        LOGGER.debug("Getting completions at line {}, column {} via REST API", line, column);

        // Build JSON request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("code", code);
        requestBody.addProperty("line", line);
        requestBody.addProperty("column", column);

        // Make POST request to /completions endpoint
        String response = post(ApiEndpoints.COMPLETIONS, requestBody.toString());

        // Parse response
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        List<CompletionResult> completions = new ArrayList<>();

        // Parse completions array
        if (json.has(JsonFields.COMPLETIONS) && json.get(JsonFields.COMPLETIONS).isJsonArray()) {
            JsonArray completionsArray = json.getAsJsonArray(JsonFields.COMPLETIONS);

            for (int i = 0; i < completionsArray.size(); i++) {
                JsonObject compJson = completionsArray.get(i).getAsJsonObject();

                CompletionResult completion = new CompletionResult(
                    getJsonString(compJson, "text"),
                    getJsonString(compJson, "type"),
                    getJsonString(compJson, "complete"),
                    getJsonString(compJson, "description"),
                    getJsonString(compJson, "docstring"),
                    getJsonString(compJson, "signature")
                );

                completions.add(completion);
            }
        }

        LOGGER.debug("Retrieved {} completions", completions.size());
        return completions;
    }

    /**
     * Obtain a new session token from the Gateway.
     * <p>
     * Session tokens are HMAC-signed and expire after 8 hours.
     * This replaces the insecure User-Agent header authentication.
     *
     * @throws IOException if the request fails
     * @since v2.9.0 - Security fix for CRITICAL-01
     */
    private void obtainSessionToken() throws IOException {
        String url = gatewayUrl + ApiEndpoints.CLIENT_AUTH_BASE + ApiEndpoints.AUTH_SESSION;

        // Build request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("client_id", "ignition-designer-8.3");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();

        try {
            LOGGER.debug("Requesting session token from: {}", url);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("Failed to obtain session token: HTTP " + response.statusCode());
            }

            // Parse response
            JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();

            if (!responseJson.get(JsonFields.SUCCESS).getAsBoolean()) {
                String error = responseJson.has(JsonFields.ERROR) ? responseJson.get(JsonFields.ERROR).getAsString() : "Unknown error";
                throw new IOException("Failed to obtain session token: " + error);
            }

            // Use api_token (HMAC-signed) for Bearer auth, not token (CSRF UUID)
            // The "token" field is the CSRF token for browser sessions;
            // "api_token" is the HMAC-signed token that the Gateway validates for Bearer auth
            if (responseJson.has(JsonFields.API_TOKEN) && !responseJson.get(JsonFields.API_TOKEN).isJsonNull()) {
                sessionToken = responseJson.get(JsonFields.API_TOKEN).getAsString();
            } else {
                sessionToken = responseJson.get(JsonFields.TOKEN).getAsString();
            }
            long expiresIn = responseJson.get(JsonFields.EXPIRES_IN).getAsLong();
            tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000);

            LOGGER.info("Session token obtained successfully (expires in {} seconds)", expiresIn);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Token request interrupted", e);
        }
    }

    /**
     * Check if the session token is valid or needs renewal.
     *
     * @return true if the token is valid, false if expired or missing
     */
    private boolean isSessionTokenValid() {
        if (sessionToken == null) {
            return false;
        }

        // Consider token expired 5 minutes before actual expiration (buffer)
        long expirationBuffer = 5 * 60 * 1000; // 5 minutes
        return System.currentTimeMillis() < (tokenExpiresAt - expirationBuffer);
    }

    /**
     * Ensure we have a valid session token, obtaining a new one if necessary.
     * If token acquisition fails, proceed without auth (endpoints use GRANTED access).
     */
    private void ensureValidToken() {
        if (!isSessionTokenValid()) {
            try {
                LOGGER.debug("Session token missing or expired, obtaining new token");
                obtainSessionToken();
            } catch (Exception e) {
                LOGGER.debug("Session token acquisition failed (non-fatal, proceeding without auth): {}", e.getMessage());
                sessionToken = null;
            }
        }
    }

    /**
     * Makes a GET request to the specified endpoint.
     *
     * @param endpoint the API endpoint (e.g., "/health", "/pool-stats")
     * @return the response body as a string
     * @throws IOException if the request fails
     */
    private String get(String endpoint) throws IOException {
        // Try to get a session token (non-fatal if it fails)
        ensureValidToken();

        String url = gatewayUrl + ApiEndpoints.CLIENT_API_BASE + endpoint;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .header("Accept", "application/json")
                .header("X-Source", "Python3-IDE");

        if (sessionToken != null) {
            builder.header("Authorization", "Bearer " + sessionToken);
        }

        HttpRequest request = builder.build();

        try {
            LOGGER.debug("GET request to: {}", url);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
            }

            return response.body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    /**
     * Makes a POST request to the specified endpoint.
     *
     * @param endpoint the API endpoint (e.g., "/exec", "/eval")
     * @param jsonBody the JSON request body
     * @return the response body as a string
     * @throws IOException if the request fails
     */
    private String post(String endpoint, String jsonBody) throws IOException {
        // Try to get a session token (non-fatal if it fails)
        ensureValidToken();

        String url = gatewayUrl + ApiEndpoints.CLIENT_API_BASE + endpoint;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-Source", "Python3-IDE");

        if (sessionToken != null) {
            builder.header("Authorization", "Bearer " + sessionToken);
        }

        HttpRequest request = builder.build();

        try {
            LOGGER.info("POST request to: {}", url);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            LOGGER.info("POST response status: {} from {}", response.statusCode(), url);

            if (response.statusCode() != 200) {
                LOGGER.error("POST request failed with status {}: {}", response.statusCode(), response.body());
                throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
            }

            return response.body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    /**
     * Parses an execution result from JSON response.
     *
     * @param jsonResponse the JSON response string
     * @return parsed ExecutionResult
     */
    private ExecutionResult parseExecutionResult(String jsonResponse) {
        try {
            LOGGER.info("Parsing execution result from JSON response");
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();

            boolean success = json.has(JsonFields.SUCCESS) && json.get(JsonFields.SUCCESS).getAsBoolean();
            String result = json.has(JsonFields.RESULT) ? json.get(JsonFields.RESULT).getAsString() : null;
            String error = json.has(JsonFields.ERROR) ? json.get(JsonFields.ERROR).getAsString() : null;
            Long executionTimeMs = json.has(JsonFields.EXECUTION_TIME_MS) ? json.get(JsonFields.EXECUTION_TIME_MS).getAsLong() : null;
            Long timestamp = json.has(JsonFields.TIMESTAMP) ? json.get(JsonFields.TIMESTAMP).getAsLong() : null;

            LOGGER.info("Parsed execution result: success={}, hasResult={}, hasError={}", success, result != null, error != null);
            if (error != null) {
                LOGGER.warn("Execution error: {}", error);
            }

            return new ExecutionResult(success, result, error, executionTimeMs, timestamp);

        } catch (Exception e) {
            LOGGER.error("Failed to parse execution result from JSON: {}", jsonResponse, e);
            return new ExecutionResult(false, "Failed to parse response: " + e.getMessage());
        }
    }

    /**
     * Builds the Gateway URL from system properties, environment variables, Designer connection, or defaults.
     *
     * @param context the Designer context (reserved for future use)
     * @return the Gateway base URL (e.g., "http://localhost:8088")
     */
    private static String buildGatewayUrl(DesignerContext context) {
        try {
            // 1. System property override
            // Set via: -Dignition.python3.gateway.url=http://localhost:9088
            String url = System.getProperty("ignition.python3.gateway.url");

            // 2. Environment variable
            if (url == null || url.trim().isEmpty()) {
                url = System.getenv("IGNITION_GATEWAY_URL");
            }

            // 3. Check IDE preferences for saved gateway override (shared across IDE, Script Console, Project Browser)
            if (url == null || url.trim().isEmpty()) {
                try {
                    java.util.prefs.Preferences idePrefs = java.util.prefs.Preferences.userNodeForPackage(Python3IDE.class);
                    String override = idePrefs.get(PreferenceKeys.IDE_GATEWAY_OVERRIDE, "");
                    if (override != null && !override.trim().isEmpty()) {
                        url = override.trim();
                        LOGGER.info("Using Gateway URL from IDE settings: {}", url);
                    }
                } catch (Exception e) {
                    LOGGER.debug("Could not read IDE preferences: {}", e.getMessage());
                }
            }

            // 4. Auto-detect from Designer's active gateway connection
            if (url == null || url.trim().isEmpty()) {
                try {
                    GatewayConnection gwConn = GatewayConnectionManager.getInstance();
                    if (gwConn != null) {
                        String webUrl = gwConn.getGatewayWebURL();
                        if (webUrl != null && !webUrl.trim().isEmpty()) {
                            url = webUrl.trim();
                            LOGGER.info("Using Gateway URL from Designer connection: {}", url);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.debug("Could not auto-detect gateway URL from Designer: {}", e.getMessage());
                }
            }

            // 5. Default to localhost:8088
            if (url == null || url.trim().isEmpty()) {
                url = "http://localhost:8088";
                LOGGER.info("Using default Gateway URL: {} (configure via IDE settings or set -Dignition.python3.gateway.url=http://host:port)", url);
            } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
                LOGGER.info("Added http:// protocol to Gateway URL: {}", url);
            }

            // Remove trailing slash if present
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }

            return url;

        } catch (Exception e) {
            LOGGER.error("Failed to get Gateway URL, using default", e);
            return "http://localhost:8088";
        }
    }

    /**
     * Adds a value to a JSON object with appropriate type handling.
     *
     * @param json the JSON object to add to
     * @param key the key
     * @param value the value (String, Number, Boolean, or other)
     */
    private void addToJson(JsonObject json, String key, Object value) {
        if (value == null) {
            json.add(key, null);
        } else if (value instanceof String) {
            json.addProperty(key, (String) value);
        } else if (value instanceof Number) {
            json.addProperty(key, (Number) value);
        } else if (value instanceof Boolean) {
            json.addProperty(key, (Boolean) value);
        } else {
            // For other types, convert to string
            json.addProperty(key, value.toString());
        }
    }

    /**
     * Safely gets a string value from a JSON object, handling null values.
     *
     * @param json the JSON object
     * @param key the key to look up
     * @return the string value, or null if the key doesn't exist or value is null
     */
    private String getJsonString(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return null;
    }

    // Script Management Methods

    /**
     * Lists all saved scripts from the Gateway.
     *
     * @return list of script metadata
     * @throws IOException if the HTTP request fails
     */
    public List<ScriptMetadata> listScripts() throws IOException {
        LOGGER.debug("Listing saved scripts via REST API");

        String response = get(ApiEndpoints.SCRIPTS_LIST);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        List<ScriptMetadata> scripts = new ArrayList<>();

        if (json.has(JsonFields.SCRIPTS) && json.get(JsonFields.SCRIPTS).isJsonArray()) {
            JsonArray scriptsArray = json.getAsJsonArray(JsonFields.SCRIPTS);
            for (int i = 0; i < scriptsArray.size(); i++) {
                JsonObject scriptJson = scriptsArray.get(i).getAsJsonObject();
                ScriptMetadata metadata = new ScriptMetadata(
                    getJsonString(scriptJson, "id"),
                    getJsonString(scriptJson, "name"),
                    getJsonString(scriptJson, "description"),
                    getJsonString(scriptJson, "author"),
                    getJsonString(scriptJson, "createdDate"),
                    getJsonString(scriptJson, "lastModified"),
                    getJsonString(scriptJson, "folderPath"),
                    getJsonString(scriptJson, "version")
                );
                scripts.add(metadata);
            }
        }

        LOGGER.debug("Loaded {} scripts", scripts.size());
        return scripts;
    }

    /**
     * Loads a saved script from the Gateway.
     *
     * @param name the script name
     * @return the saved script with code
     * @throws IOException if the HTTP request fails
     */
    public SavedScript loadScript(String name) throws IOException {
        LOGGER.debug("Loading script: {}", name);

        // URL encode the name to handle spaces and special characters
        String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
        String response = get(ApiEndpoints.SCRIPTS_LOAD_PREFIX + encodedName);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        if (json.has("script") && json.get("script").isJsonObject()) {
            JsonObject scriptJson = json.getAsJsonObject("script");
            // v2.10.0: SavedScript is now a record, use constructor instead of setters
            SavedScript script = new SavedScript(
                getJsonString(scriptJson, "id"),
                getJsonString(scriptJson, "name"),
                getJsonString(scriptJson, "code"),
                getJsonString(scriptJson, "description"),
                getJsonString(scriptJson, "author"),
                getJsonString(scriptJson, "createdDate"),
                getJsonString(scriptJson, "lastModified"),
                getJsonString(scriptJson, "folderPath"),
                getJsonString(scriptJson, "version")
            );
            return script;
        }

        throw new IOException("Failed to load script: " + name);
    }

    /**
     * Saves a script to the Gateway.
     *
     * @param name the script name
     * @param code the Python code
     * @param description optional description
     * @param author the script author
     * @param folderPath the folder path (e.g., "My Scripts/Utils")
     * @param version the script version (e.g., "1.0")
     * @throws IOException if the HTTP request fails
     */
    public void saveScript(String name, String code, String description,
                          String author, String folderPath, String version) throws IOException {
        LOGGER.debug("Saving script: {} in folder: {}", name, folderPath);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("name", name);
        requestBody.addProperty("code", code);
        requestBody.addProperty("description", description);
        requestBody.addProperty("author", author);
        requestBody.addProperty("folderPath", folderPath);
        requestBody.addProperty("version", version);

        String response = post(ApiEndpoints.SCRIPTS_SAVE, requestBody.toString());
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        if (!json.has(JsonFields.SUCCESS) || !json.get(JsonFields.SUCCESS).getAsBoolean()) {
            throw new IOException("Failed to save script: " + name);
        }

        LOGGER.info("Script saved successfully: {} in folder: {}", name, folderPath);
    }

    /**
     * Saves a script to the Gateway (simplified overload for backward compatibility).
     *
     * @param name the script name
     * @param code the Python code
     * @param description optional description
     * @throws IOException if the HTTP request fails
     */
    public void saveScript(String name, String code, String description) throws IOException {
        saveScript(name, code, description, "Unknown", "", "1.0");
    }

    /**
     * Deletes a saved script from the Gateway.
     * v3.6.5: Changed from HTTP DELETE to POST for broader servlet compatibility.
     *
     * @param name the script name
     * @throws IOException if the HTTP request fails
     */
    public void deleteScript(String name) throws IOException {
        LOGGER.info("Deleting script: {}", name);

        // URL encode the name to handle spaces and special characters
        String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
        String response = post(ApiEndpoints.SCRIPTS_DELETE_PREFIX + encodedName, "{}");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        if (!json.has(JsonFields.SUCCESS) || !json.get(JsonFields.SUCCESS).getAsBoolean()) {
            String msg = json.has(JsonFields.MESSAGE) ? json.get(JsonFields.MESSAGE).getAsString() : "Unknown error";
            throw new IOException("Failed to delete script '" + name + "': " + msg);
        }

        LOGGER.info("Script deleted successfully: {}", name);
    }

    /**
     * Gets the Gateway impact assessment from Python 3 module.
     *
     * @return gateway impact with level and health score
     * @throws IOException if the HTTP request fails
     */
    public GatewayImpact getGatewayImpact() throws IOException {
        LOGGER.debug("Getting Gateway impact via REST API");

        try {
            String response = get(ApiEndpoints.GATEWAY_IMPACT);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();

            GatewayImpact impact = new GatewayImpact();
            impact.setImpactLevel(getJsonString(json, "impactLevel"));
            impact.setHealthScore(json.has("healthScore") ? json.get("healthScore").getAsInt() : 0);
            impact.setRecommendation(getJsonString(json, "recommendation"));

            // v2.5.20: Parse RAM and CPU metrics (added in v2.5.19) - Legacy fields
            if (json.has("memoryUsageMb")) {
                impact.setMemoryUsageMb(json.get("memoryUsageMb").getAsDouble());
            }
            if (json.has("averageCpuTimeMs")) {
                impact.setAverageCpuTimeMs(json.get("averageCpuTimeMs").getAsDouble());
            }
            // v2.5.21: Parse CPU usage percentage
            if (json.has("cpuUsagePercent")) {
                impact.setCpuUsagePercent(json.get("cpuUsagePercent").getAsDouble());
            }

            // v2.15.5: Parse Python3-specific and system-wide metrics
            if (json.has("python3MemoryMb")) {
                impact.setPython3MemoryMb(json.get("python3MemoryMb").getAsDouble());
            }
            if (json.has("python3CpuPercent")) {
                impact.setPython3CpuPercent(json.get("python3CpuPercent").getAsDouble());
            }
            if (json.has("gatewayMemoryMb")) {
                impact.setGatewayMemoryMb(json.get("gatewayMemoryMb").getAsDouble());
            }
            if (json.has("gatewayCpuPercent")) {
                impact.setGatewayCpuPercent(json.get("gatewayCpuPercent").getAsDouble());
            }
            if (json.has("maxMemoryMb")) {
                impact.setMaxMemoryMb(json.get("maxMemoryMb").getAsDouble());
            }
            if (json.has("availableCores")) {
                impact.setAvailableCores(json.get("availableCores").getAsInt());
            }

            return impact;

        } catch (Exception e) {
            LOGGER.warn("Failed to get Gateway impact, returning default", e);
            // Return default "healthy" impact if endpoint not available
            return new GatewayImpact("LOW", 100, "All systems operational");
        }
    }

    /**
     * Sets the Python process pool size (1-20).
     *
     * @param size the new pool size (must be between 1 and 20)
     * @throws IOException if the HTTP request fails
     * @throws IllegalArgumentException if size is out of range
     *
     * v1.17.2: Added for dynamic pool size adjustment
     */
    public void setPoolSize(int size) throws IOException {
        if (size < 1 || size > 20) {
            throw new IllegalArgumentException("Pool size must be between 1 and 20");
        }

        LOGGER.debug("Setting pool size to {} via REST API", size);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("size", size);

        String response = post(ApiEndpoints.POOL_SIZE, requestBody.toString());
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        if (!json.has(JsonFields.SUCCESS) || !json.get(JsonFields.SUCCESS).getAsBoolean()) {
            String error = json.has(JsonFields.ERROR) ? json.get(JsonFields.ERROR).getAsString() : "Unknown error";
            throw new IOException("Failed to set pool size: " + error);
        }

        LOGGER.info("Pool size changed to {}", size);
    }

    // ============================================================================
    // Package Management Methods (v2.7.0 - Stubs for future implementation)
    // ============================================================================

    /**
     * Lists all installed Python packages from the Gateway.
     *
     * @return List of installed packages with name and version
     * @throws IOException if the HTTP request fails
     *
     * v2.7.0: Stub method - Gateway endpoint not yet implemented
     */
    public List<PackageInfo> listPackages() throws IOException {
        LOGGER.debug("Listing installed packages via REST API");

        // TODO: Implement when Gateway endpoint is available
        // Expected endpoint: GET /data/python3integration/api/v1/packages/list
        // Expected response: {"packages": [{"name": "numpy", "version": "1.24.0"}, ...]}

        throw new IOException("Package list endpoint not yet implemented on Gateway");
    }

    /**
     * Searches PyPI for a package by exact name.
     *
     * @param packageName the package name to search for
     * @return Package search result with details
     * @throws IOException if the HTTP request fails
     *
     * v2.7.0: Stub method - Gateway endpoint not yet implemented
     */
    public PackageSearchResult searchPackage(String packageName) throws IOException {
        LOGGER.debug("Searching for package '{}' via REST API", packageName);

        // TODO: Implement when Gateway endpoint is available
        // Expected endpoint: POST /data/python3integration/api/v1/packages/search
        // Expected request: {"name": "numpy"}
        // Expected response: {"found": true, "name": "numpy", "latestVersion": "1.24.0", "description": "..."}

        throw new IOException("Package search endpoint not yet implemented on Gateway");
    }

    /**
     * Installs a package from PyPI.
     *
     * @param packageName the package name to install
     * @param version optional version (null for latest)
     * @return Installation result
     * @throws IOException if the HTTP request fails
     *
     * v2.7.0: Stub method - Gateway endpoint not yet implemented
     */
    public InstallResult installPackage(String packageName, String version) throws IOException {
        String packageSpec = version != null ? packageName + "==" + version : packageName;
        LOGGER.info("Installing package '{}' via REST API", packageSpec);

        // POST /api/v1/packages/install/:name
        String encodedName = URLEncoder.encode(packageSpec, StandardCharsets.UTF_8);
        String response = post(ApiEndpoints.PACKAGES_INSTALL_PREFIX + encodedName, "{}");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        boolean success = json.has(JsonFields.SUCCESS) && json.get(JsonFields.SUCCESS).getAsBoolean();
        String message = json.has(JsonFields.MESSAGE) ? json.get(JsonFields.MESSAGE).getAsString() : "";
        return new InstallResult(success, message, packageName, version != null ? version : "");
    }

    /**
     * Uploads and installs a .whl file.
     *
     * @param whlFile the wheel file to upload
     * @return Installation result
     * @throws IOException if the HTTP request fails
     *
     * v2.7.0: Stub method - Gateway endpoint not yet implemented
     */
    public InstallResult uploadWheel(java.io.File whlFile) throws IOException {
        LOGGER.debug("Uploading wheel file '{}' via REST API", whlFile.getName());

        // TODO: Implement when Gateway endpoint is available
        // Expected endpoint: POST /data/python3integration/api/v1/packages/upload
        // Expected request: Multipart form data with file
        // Expected response: {"success": true, "message": "Wheel installed successfully", "packageName": "...", "version": "..."}

        throw new IOException("Wheel upload endpoint not yet implemented on Gateway");
    }

    /**
     * Uninstalls a Python package.
     *
     * @param packageName the package name to uninstall
     * @return Uninstall result
     * @throws IOException if the HTTP request fails
     *
     * v2.7.0: Stub method - Gateway endpoint not yet implemented
     */
    public UninstallResult uninstallPackage(String packageName) throws IOException {
        LOGGER.info("Uninstalling package '{}' via REST API", packageName);

        // POST /api/v1/packages/uninstall/:name
        String encodedName = URLEncoder.encode(packageName, StandardCharsets.UTF_8);
        String response = post(ApiEndpoints.PACKAGES_UNINSTALL_PREFIX + encodedName, "{}");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        boolean success = json.has(JsonFields.SUCCESS) && json.get(JsonFields.SUCCESS).getAsBoolean();
        String message = json.has(JsonFields.MESSAGE) ? json.get(JsonFields.MESSAGE).getAsString() : "";
        return new UninstallResult(success, message);
    }

    // ============================================================================
    // Data Classes for Package Management (v2.7.0)
    // ============================================================================

    /**
     * Represents information about an installed package.
     */
    public static class PackageInfo {
        private String name;
        private String version;

        public PackageInfo(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }
    }

    /**
     * Represents a package search result from PyPI.
     */
    public static class PackageSearchResult {
        private boolean found;
        private String name;
        private String latestVersion;
        private String description;

        public PackageSearchResult(boolean found, String name, String latestVersion, String description) {
            this.found = found;
            this.name = name;
            this.latestVersion = latestVersion;
            this.description = description;
        }

        public boolean isFound() {
            return found;
        }

        public String getName() {
            return name;
        }

        public String getLatestVersion() {
            return latestVersion;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Represents the result of a package installation.
     */
    public static class InstallResult {
        private boolean success;
        private String message;
        private String packageName;
        private String version;

        public InstallResult(boolean success, String message, String packageName, String version) {
            this.success = success;
            this.message = message;
            this.packageName = packageName;
            this.version = version;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getVersion() {
            return version;
        }
    }

    /**
     * Represents the result of a package uninstallation.
     */
    public static class UninstallResult {
        private boolean success;
        private String message;

        public UninstallResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    // =========================================================================
    // Python Distribution Management (v3.1.0)
    // =========================================================================

    /**
     * Status information for a Python distribution version.
     */
    public static class DistributionInfo {
        public final String version;
        public final String fullVersion;
        public final boolean installed;
        public final boolean available;
        public final String pythonPath;
        public final long installSizeMB;
        public final boolean poolActive;

        public DistributionInfo(String version, String fullVersion, boolean installed,
                                boolean available, String pythonPath, long installSizeMB,
                                boolean poolActive) {
            this.version = version;
            this.fullVersion = fullVersion;
            this.installed = installed;
            this.available = available;
            this.pythonPath = pythonPath;
            this.installSizeMB = installSizeMB;
            this.poolActive = poolActive;
        }
    }

    /**
     * Gets all available Python distributions with their install status (v3.1.0).
     *
     * @return list of distribution info objects
     * @throws IOException if the HTTP request fails
     */
    public List<DistributionInfo> getDistributions() throws IOException {
        LOGGER.info("Getting Python distributions via REST API");

        List<DistributionInfo> distributions = new ArrayList<>();

        try {
            String response = get(ApiEndpoints.DISTRIBUTIONS);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();

            if (json.has(JsonFields.DISTRIBUTIONS) && json.get(JsonFields.DISTRIBUTIONS).isJsonArray()) {
                for (var element : json.getAsJsonArray(JsonFields.DISTRIBUTIONS)) {
                    JsonObject dist = element.getAsJsonObject();
                    distributions.add(new DistributionInfo(
                            dist.get("version").getAsString(),
                            dist.has("fullVersion") ? dist.get("fullVersion").getAsString() : dist.get("version").getAsString(),
                            dist.has("installed") && dist.get("installed").getAsBoolean(),
                            dist.has("available") && dist.get("available").getAsBoolean(),
                            dist.has("pythonPath") ? dist.get("pythonPath").getAsString() : null,
                            dist.has("installSizeMB") ? dist.get("installSizeMB").getAsLong() : 0,
                            dist.has("poolActive") && dist.get("poolActive").getAsBoolean()
                    ));
                }
            }

            LOGGER.info("Found {} Python distributions", distributions.size());
            return distributions;

        } catch (Exception e) {
            LOGGER.warn("Failed to get distributions: {}", e.getMessage());
            return distributions;
        }
    }

    /**
     * Installs a Python version on the Gateway (v3.1.0).
     * This triggers a download and extraction of the Python distribution.
     *
     * @param version the Python version to install (e.g., "3.12")
     * @return true if installation was successful
     * @throws IOException if the HTTP request fails
     */
    public boolean installPythonVersion(String version) throws IOException {
        LOGGER.info("Installing Python version {} via REST API", version);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("version", version);

        String response = post(ApiEndpoints.DISTRIBUTIONS_INSTALL, requestBody.toString());
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        boolean success = json.has(JsonFields.SUCCESS) && json.get(JsonFields.SUCCESS).getAsBoolean();
        if (success) {
            LOGGER.info("Python {} installed successfully", version);
        } else {
            String error = json.has(JsonFields.ERROR) ? json.get(JsonFields.ERROR).getAsString() : "Unknown error";
            LOGGER.error("Failed to install Python {}: {}", version, error);
        }
        return success;
    }

    /**
     * Uninstalls a Python version from the Gateway (v3.1.0).
     *
     * @param version the Python version to uninstall (e.g., "3.12")
     * @return true if uninstallation was successful
     * @throws IOException if the HTTP request fails
     */
    public boolean uninstallPythonVersion(String version) throws IOException {
        LOGGER.info("Uninstalling Python version {} via REST API", version);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("version", version);

        String response = post(ApiEndpoints.DISTRIBUTIONS_UNINSTALL, requestBody.toString());
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        boolean success = json.has(JsonFields.SUCCESS) && json.get(JsonFields.SUCCESS).getAsBoolean();
        if (success) {
            LOGGER.info("Python {} uninstalled successfully", version);
        } else {
            String error = json.has(JsonFields.ERROR) ? json.get(JsonFields.ERROR).getAsString() : "Unknown error";
            LOGGER.error("Failed to uninstall Python {}: {}", version, error);
        }
        return success;
    }
}
