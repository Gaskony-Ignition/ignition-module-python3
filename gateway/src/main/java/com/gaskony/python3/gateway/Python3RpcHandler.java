package com.gaskony.python3.gateway;

import com.gaskony.python3.Python3Rpc;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import com.inductiveautomation.ignition.gateway.clientcomm.ClientReqSession;
import com.inductiveautomation.ignition.gateway.rpc.RpcDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gateway-side implementation of {@link Python3Rpc}.
 *
 * <p>Serves the Designer over the platform's authenticated module-RPC channel,
 * replacing the Designer's old cold-HTTP REST client (which could not
 * authenticate to the Gateway after the C13/C14 security hardening). Because the
 * RPC channel is only reachable by an authenticated Gateway client, and the
 * caller identity is available via {@link RpcDelegate#session()}, no session
 * token is required.</p>
 *
 * <p>Each method mirrors the JSON produced by the equivalent REST handler so the
 * Designer parsing is unchanged. Business logic is delegated to the same
 * services the REST handlers use ({@link Python3ScriptRepository},
 * {@link Python3ScriptModule}); the tested REST path is left untouched.</p>
 *
 * @since v4.2.0
 */
public class Python3RpcHandler implements Python3Rpc {

    private static final Logger logger = LoggerFactory.getLogger(Python3RpcHandler.class);

    private final GatewayHook hook;

    public Python3RpcHandler(GatewayHook hook) {
        this.hook = hook;
    }

    // -------------------------------------------------------------------------
    // Script management
    // -------------------------------------------------------------------------

    @Override
    public String listScripts() throws Exception {
        requireAuthenticatedSession();
        Python3ScriptRepository repo = hook.getScriptRepository();
        if (repo == null) {
            return ApiResponse.error("Script repository not initialized").toString();
        }

        List<Python3ScriptRepository.ScriptMetadata> scripts = repo.listScripts();
        JsonObject response = new JsonObject();
        response.addProperty("success", true);

        JsonArray scriptsArray = new JsonArray();
        for (Python3ScriptRepository.ScriptMetadata s : scripts) {
            JsonObject o = new JsonObject();
            o.addProperty("id", s.getId());
            o.addProperty("name", s.getName());
            o.addProperty("description", s.getDescription());
            o.addProperty("author", s.getAuthor());
            o.addProperty("createdDate", s.getCreatedDate());
            o.addProperty("lastModified", s.getLastModified());
            o.addProperty("folderPath", s.getFolderPath());
            o.addProperty("version", s.getVersion());
            scriptsArray.add(o);
        }
        response.add("scripts", scriptsArray);
        logger.debug("RPC: listed {} scripts", scripts.size());
        return response.toString();
    }

    @Override
    public String loadScript(String name) throws Exception {
        requireAuthenticatedSession();
        Python3ScriptRepository repo = hook.getScriptRepository();
        if (repo == null) {
            return ApiResponse.error("Script repository not initialized").toString();
        }

        Python3ScriptRepository.SavedScript script = repo.loadScript(name);
        JsonObject response = new JsonObject();
        if (script == null) {
            response.addProperty("success", false);
            response.addProperty("message", "Script not found: " + name);
            return response.toString();
        }

        response.addProperty("success", true);
        JsonObject o = new JsonObject();
        o.addProperty("id", script.getId());
        o.addProperty("name", script.getName());
        o.addProperty("code", script.getCode());
        o.addProperty("description", script.getDescription());
        o.addProperty("author", script.getAuthor());
        o.addProperty("createdDate", script.getCreatedDate());
        o.addProperty("lastModified", script.getLastModified());
        o.addProperty("folderPath", script.getFolderPath());
        o.addProperty("version", script.getVersion());
        response.add("script", o);
        return response.toString();
    }

    @Override
    public String saveScript(String name, String code, String description,
                             String author, String folderPath, String version) throws Exception {
        requireAuthenticatedSession();
        Python3ScriptRepository repo = hook.getScriptRepository();
        if (repo == null) {
            return ApiResponse.error("Script repository not initialized").toString();
        }

        repo.saveScript(name, code, description, author, folderPath, version);
        Python3RestEndpoints.auditLog("SCRIPT_SAVE", "name=" + name + " folder=" + folderPath);

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        logger.info("RPC: saved script {} in folder {}", name, folderPath);
        return response.toString();
    }

    @Override
    public String deleteScript(String name) throws Exception {
        requireAuthenticatedSession();
        Python3ScriptRepository repo = hook.getScriptRepository();
        if (repo == null) {
            return ApiResponse.error("Script repository not initialized").toString();
        }

        Python3RestEndpoints.auditLog("SCRIPT_DELETE", "name=" + name);
        boolean deleted = repo.deleteScript(name);

        JsonObject response = new JsonObject();
        response.addProperty("success", deleted);
        response.addProperty("message", deleted ? "Script deleted successfully" : "Script not found: " + name);
        logger.info("RPC: script deletion {} - {}", name, deleted ? "success" : "not found");
        return response.toString();
    }

    // -------------------------------------------------------------------------
    // Execution
    // -------------------------------------------------------------------------

    @Override
    public String exec(String code, String variablesJson, String pythonVersion) throws Exception {
        requireAuthenticatedSession();
        Map<String, Object> variables = parseVariables(variablesJson);
        Python3RestEndpoints.validateCode(code);
        Python3RestEndpoints.auditLog("PYTHON_EXEC", code);

        JsonObject response = new JsonObject();
        try {
            Python3ScriptModule sm = hook.getScriptModule();
            // Per-execution structured audit is emitted by Python3ScriptModule.exec()
            // via Python3AuditLogger; the Administrator scripting gate is enforced there.
            Object result = (pythonVersion != null && !pythonVersion.isEmpty())
                ? sm.exec(code, variables, SecurityMode.DESIGNER_ADMIN.getValue(), pythonVersion)
                : sm.exec(code, variables, SecurityMode.DESIGNER_ADMIN.getValue());
            response.addProperty("success", true);
            response.addProperty("result", result != null ? result.toString() : null);
        } catch (Exception e) {
            response.addProperty("success", false);
            response.addProperty("error", e.getMessage());
        }
        return response.toString();
    }

    @Override
    public String eval(String expression, String variablesJson, String pythonVersion) throws Exception {
        requireAuthenticatedSession();
        Map<String, Object> variables = parseVariables(variablesJson);
        Python3RestEndpoints.validateCode(expression);
        Python3RestEndpoints.auditLog("PYTHON_EVAL", expression);

        JsonObject response = new JsonObject();
        try {
            Python3ScriptModule sm = hook.getScriptModule();
            Object result = (pythonVersion != null && !pythonVersion.isEmpty())
                ? sm.eval(expression, variables, SecurityMode.DESIGNER_ADMIN.getValue(), pythonVersion)
                : sm.eval(expression, variables, SecurityMode.DESIGNER_ADMIN.getValue());
            response.addProperty("success", true);
            response.addProperty("result", result != null ? result.toString() : null);
        } catch (Exception e) {
            response.addProperty("success", false);
            response.addProperty("error", e.getMessage());
        }
        return response.toString();
    }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    @Override
    public String getVersion() throws Exception {
        requireAuthenticatedSession();
        Python3ScriptModule sm = hook.getScriptModule();
        JsonObject response = Python3RestEndpoints.mapToJson(sm.getVersion());
        if (response.has("python_version") && !response.has("pythonVersion")) {
            response.addProperty("pythonVersion", response.get("python_version").getAsString());
        }
        if (response.has("version") && !response.has("pythonVersion")) {
            response.addProperty("pythonVersion", response.get("version").getAsString());
        }
        return response.toString();
    }

    @Override
    public String getPoolStats() throws Exception {
        requireAuthenticatedSession();
        Python3ScriptModule sm = hook.getScriptModule();
        JsonObject response = Python3RestEndpoints.mapToJson(sm.getPoolStats());
        if (!response.has("healthCheckStatus")) {
            response.addProperty("healthCheckStatus", sm.isAvailable() ? "Healthy" : "Down");
        }
        return response.toString();
    }

    @Override
    public String health() throws Exception {
        requireAuthenticatedSession();
        boolean available = hook.getScriptModule().isAvailable();
        JsonObject response = new JsonObject();
        response.addProperty("healthy", available);
        response.addProperty("available", available);
        response.addProperty("status", available ? "HEALTHY" : "DOWN");
        response.addProperty("timestamp", System.currentTimeMillis());
        return response.toString();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * The module-RPC channel is only reachable by an authenticated Gateway
     * client (Designer/Vision/Perspective session), so a valid session is the
     * transport-level equivalent of the REST {@code checkReadPermission} gate.
     * Execution is further constrained inside {@link Python3ScriptModule} by the
     * {@code ignition.python3.scriptingFunctions.allowed} Administrator opt-in.
     */
    void requireAuthenticatedSession() {
        ClientReqSession session = RpcDelegate.session();
        if (session == null || !session.isValid()) {
            throw new SecurityException("Authenticated Gateway session required");
        }
    }

    private Map<String, Object> parseVariables(String variablesJson) {
        Map<String, Object> variables = new HashMap<>();
        if (variablesJson == null || variablesJson.isBlank()) {
            return variables;
        }
        JsonObject obj = JsonParser.parseString(variablesJson).getAsJsonObject();
        for (String key : obj.keySet()) {
            variables.put(key, Python3RestEndpoints.jsonElementToObject(obj.get(key)));
        }
        return variables;
    }
}
