package com.inductiveautomation.ignition.examples.python3.designer;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import com.inductiveautomation.ignition.designer.model.DesignerContext;
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

    private static final String API_BASE_PATH = "/data/python3integration/api/v1";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final String gatewayUrl;

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
        LOGGER.info("Executing Python code via REST API (code length: {} chars)", code.length());

        // Build JSON request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("code", code);

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
        String response = post("/exec", requestBody.toString());
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
        LOGGER.debug("Evaluating Python expression via REST API: {}", expression);

        // Build JSON request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("expression", expression);

        // Add variables as JSON object
        JsonObject varsJson = new JsonObject();
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                addToJson(varsJson, entry.getKey(), entry.getValue());
            }
        }
        requestBody.add("variables", varsJson);

        // Make POST request to /eval endpoint
        String response = post("/eval", requestBody.toString());

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
        String response = post("/shell-exec", requestBody.toString());
        LOGGER.info("Received response from /shell-exec endpoint (length: {} chars)", response.length());

        // Parse response
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        boolean success = json.has("success") && json.get("success").getAsBoolean();
        String stdout = json.has("stdout") ? json.get("stdout").getAsString() : "";
        String stderr = json.has("stderr") ? json.get("stderr").getAsString() : "";
        int exitCode = json.has("exitCode") ? json.get("exitCode").getAsInt() : -1;

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
        String response = post("/shell-interactive/create", requestBody.toString());

        // Parse response
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        if (json.has("success") && json.get("success").getAsBoolean() && json.has("sessionId")) {
            String sessionId = json.get("sessionId").getAsString();
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
        String response = post("/shell-interactive/exec", requestBody.toString());

        // Parse response
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        boolean success = json.has("success") && json.get("success").getAsBoolean();
        String output = json.has("output") ? json.get("output").getAsString() : "";

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
        post("/shell-interactive/close", requestBody.toString());

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

        String response = get("/pool-stats");

        // Parse JSON response
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        int totalSize = json.has("totalSize") ? json.get("totalSize").getAsInt() : 0;
        int healthy = json.has("healthy") ? json.get("healthy").getAsInt() : 0;
        int available = json.has("available") ? json.get("available").getAsInt() : 0;
        int inUse = json.has("inUse") ? json.get("inUse").getAsInt() : 0;

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
            String response = get("/health");
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            return json.has("healthy") && json.get("healthy").getAsBoolean();
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
        return get("/diagnostics");
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
            String response = get("/version");
            LOGGER.info("getPythonVersion() - Raw response: {}", response);

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            LOGGER.info("getPythonVersion() - Parsed JSON object, keys: {}", json.keySet());

            if (json.has("pythonVersion")) {
                String version = json.get("pythonVersion").getAsString();
                LOGGER.info("getPythonVersion() - Found 'pythonVersion' key: {}", version);
                return version;
            } else if (json.has("version")) {
                String version = json.get("version").getAsString();
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
        String response = post("/check-syntax", requestBody.toString());

        // Parse response
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        Map<String, Object> result = new HashMap<>();
        result.put("success", json.has("success") && json.get("success").getAsBoolean());

        // Parse errors array
        if (json.has("errors") && json.get("errors").isJsonArray()) {
            JsonArray errorsArray = json.getAsJsonArray("errors");
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
        String response = post("/completions", requestBody.toString());

        // Parse response
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        List<CompletionResult> completions = new ArrayList<>();

        // Parse completions array
        if (json.has("completions") && json.get("completions").isJsonArray()) {
            JsonArray completionsArray = json.getAsJsonArray("completions");

            for (int i = 0; i < completionsArray.size(); i++) {
                JsonObject compJson = completionsArray.get(i).getAsJsonObject();

                CompletionResult completion = new CompletionResult();
                completion.setText(getJsonString(compJson, "text"));
                completion.setType(getJsonString(compJson, "type"));
                completion.setComplete(getJsonString(compJson, "complete"));
                completion.setDescription(getJsonString(compJson, "description"));
                completion.setDocstring(getJsonString(compJson, "docstring"));
                completion.setSignature(getJsonString(compJson, "signature"));

                completions.add(completion);
            }
        }

        LOGGER.debug("Retrieved {} completions", completions.size());
        return completions;
    }

    /**
     * Makes a GET request to the specified endpoint.
     *
     * @param endpoint the API endpoint (e.g., "/health", "/pool-stats")
     * @return the response body as a string
     * @throws IOException if the request fails
     */
    private String get(String endpoint) throws IOException {
        String url = gatewayUrl + API_BASE_PATH + endpoint;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .header("Accept", "application/json")
                .header("User-Agent", "Ignition-Designer/8.3")
                .header("X-Source", "Python3-IDE")
                .build();

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
        String url = gatewayUrl + API_BASE_PATH + endpoint;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "Ignition-Designer/8.3")
                .header("X-Source", "Python3-IDE")
                .build();

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

            boolean success = json.has("success") && json.get("success").getAsBoolean();
            String result = json.has("result") ? json.get("result").getAsString() : null;
            String error = json.has("error") ? json.get("error").getAsString() : null;
            Long executionTimeMs = json.has("executionTimeMs") ? json.get("executionTimeMs").getAsLong() : null;
            Long timestamp = json.has("timestamp") ? json.get("timestamp").getAsLong() : null;

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
     * Builds the Gateway URL from system properties, environment variables, or defaults.
     *
     * @param context the Designer context (currently unused, reserved for future auto-detection)
     * @return the Gateway base URL (e.g., "http://localhost:8088")
     */
    private static String buildGatewayUrl(DesignerContext context) {
        try {
            // Try system property first (allows manual override)
            // Set via: -Dignition.python3.gateway.url=http://localhost:9088
            String url = System.getProperty("ignition.python3.gateway.url");

            // Try environment variable
            if (url == null || url.trim().isEmpty()) {
                url = System.getenv("IGNITION_GATEWAY_URL");
            }

            // Default to localhost:8088
            if (url == null || url.trim().isEmpty()) {
                url = "http://localhost:8088";
                LOGGER.info("Using default Gateway URL: {} (configure via IDE settings or set -Dignition.python3.gateway.url=http://host:port)", url);
            } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
                // If URL doesn't have protocol, add http://
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
            return "http://localhost:8088";  // Fallback default
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

        String response = get("/scripts/list");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        List<ScriptMetadata> scripts = new ArrayList<>();

        if (json.has("scripts") && json.get("scripts").isJsonArray()) {
            JsonArray scriptsArray = json.getAsJsonArray("scripts");
            for (int i = 0; i < scriptsArray.size(); i++) {
                JsonObject scriptJson = scriptsArray.get(i).getAsJsonObject();
                ScriptMetadata metadata = new ScriptMetadata();
                metadata.setId(getJsonString(scriptJson, "id"));
                metadata.setName(getJsonString(scriptJson, "name"));
                metadata.setDescription(getJsonString(scriptJson, "description"));
                metadata.setAuthor(getJsonString(scriptJson, "author"));
                metadata.setCreatedDate(getJsonString(scriptJson, "createdDate"));
                metadata.setLastModified(getJsonString(scriptJson, "lastModified"));
                metadata.setFolderPath(getJsonString(scriptJson, "folderPath"));
                metadata.setVersion(getJsonString(scriptJson, "version"));
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
        String response = get("/scripts/load/" + encodedName);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        if (json.has("script") && json.get("script").isJsonObject()) {
            JsonObject scriptJson = json.getAsJsonObject("script");
            SavedScript script = new SavedScript();
            script.setId(getJsonString(scriptJson, "id"));
            script.setName(getJsonString(scriptJson, "name"));
            script.setCode(getJsonString(scriptJson, "code"));
            script.setDescription(getJsonString(scriptJson, "description"));
            script.setAuthor(getJsonString(scriptJson, "author"));
            script.setCreatedDate(getJsonString(scriptJson, "createdDate"));
            script.setLastModified(getJsonString(scriptJson, "lastModified"));
            script.setFolderPath(getJsonString(scriptJson, "folderPath"));
            script.setVersion(getJsonString(scriptJson, "version"));
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

        String response = post("/scripts/save", requestBody.toString());
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        if (!json.has("success") || !json.get("success").getAsBoolean()) {
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
     *
     * @param name the script name
     * @throws IOException if the HTTP request fails
     */
    public void deleteScript(String name) throws IOException {
        LOGGER.debug("Deleting script: {}", name);

        // URL encode the name to handle spaces and special characters
        String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
        String url = gatewayUrl + API_BASE_PATH + "/scripts/delete/" + encodedName;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .DELETE()
                .header("Accept", "application/json")
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!json.has("success") || !json.get("success").getAsBoolean()) {
                throw new IOException("Failed to delete script: " + name);
            }

            LOGGER.info("Script deleted successfully: {}", name);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
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
            String response = get("/gateway-impact");
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();

            GatewayImpact impact = new GatewayImpact();
            impact.setImpactLevel(getJsonString(json, "impactLevel"));
            impact.setHealthScore(json.has("healthScore") ? json.get("healthScore").getAsInt() : 0);
            impact.setRecommendation(getJsonString(json, "recommendation"));

            // v2.5.20: Parse RAM and CPU metrics (added in v2.5.19)
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

        String response = post("/pool-size", requestBody.toString());
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        if (!json.has("success") || !json.get("success").getAsBoolean()) {
            String error = json.has("error") ? json.get("error").getAsString() : "Unknown error";
            throw new IOException("Failed to set pool size: " + error);
        }

        LOGGER.info("Pool size changed to {}", size);
    }
}
