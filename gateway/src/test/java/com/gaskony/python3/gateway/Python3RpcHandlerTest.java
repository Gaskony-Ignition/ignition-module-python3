package com.gaskony.python3.gateway;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link Python3RpcHandler}.
 *
 * <p>Verifies the JSON shapes returned to the Designer match what the Designer's
 * {@code Python3RestClient} parses, plus null-safety guards and error handling.
 * The authenticated-session gate is overridden to a no-op in the functional
 * tests (the real gate is covered separately), mirroring the module's existing
 * pattern of injecting test seams for security checks.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Python3RpcHandlerTest {

    @Mock private GatewayHook hook;
    @Mock private Python3ScriptRepository repository;
    @Mock private Python3ScriptModule scriptModule;

    private Python3RpcHandler handler;

    /** Handler variant whose session gate is a no-op, so functional logic is exercised. */
    private static class TestableHandler extends Python3RpcHandler {
        TestableHandler(GatewayHook hook) {
            super(hook);
        }

        @Override
        void requireAuthenticatedSession() {
            // no-op: transport authentication is not under test here
        }
    }

    @BeforeEach
    void setUp() {
        when(hook.getScriptRepository()).thenReturn(repository);
        when(hook.getScriptModule()).thenReturn(scriptModule);
        handler = new TestableHandler(hook);
    }

    // -------------------------------------------------------------------------
    // Script management
    // -------------------------------------------------------------------------

    @Test
    void listScripts_returnsSuccessWithScriptArray() throws Exception {
        Python3ScriptRepository.ScriptMetadata meta = new Python3ScriptRepository.ScriptMetadata(
            "id-1", "hello", "desc", "Designer", "2026-07-01", "2026-07-01", "folder/sub", "1.0");
        when(repository.listScripts()).thenReturn(List.of(meta));

        JsonObject json = parse(handler.listScripts());

        assertThat(json.get("success").getAsBoolean()).isTrue();
        assertThat(json.getAsJsonArray("scripts")).hasSize(1);
        JsonObject s = json.getAsJsonArray("scripts").get(0).getAsJsonObject();
        assertThat(s.get("name").getAsString()).isEqualTo("hello");
        assertThat(s.get("folderPath").getAsString()).isEqualTo("folder/sub");
        assertThat(s.get("version").getAsString()).isEqualTo("1.0");
    }

    @Test
    void listScripts_nullRepository_returnsError() throws Exception {
        when(hook.getScriptRepository()).thenReturn(null);

        JsonObject json = parse(handler.listScripts());

        assertThat(json.get("success").getAsBoolean()).isFalse();
    }

    @Test
    void loadScript_found_returnsScriptWithCode() throws Exception {
        Python3ScriptRepository.SavedScript saved = new Python3ScriptRepository.SavedScript(
            "id-1", "hello", "print('hi')", "desc", "Designer",
            "2026-07-01", "2026-07-01", "", "1.0");
        when(repository.loadScript("hello")).thenReturn(saved);

        JsonObject json = parse(handler.loadScript("hello"));

        assertThat(json.get("success").getAsBoolean()).isTrue();
        JsonObject script = json.getAsJsonObject("script");
        assertThat(script.get("code").getAsString()).isEqualTo("print('hi')");
        assertThat(script.get("name").getAsString()).isEqualTo("hello");
    }

    @Test
    void loadScript_notFound_returnsFailureWithMessage() throws Exception {
        when(repository.loadScript("missing")).thenReturn(null);

        JsonObject json = parse(handler.loadScript("missing"));

        assertThat(json.get("success").getAsBoolean()).isFalse();
        assertThat(json.get("message").getAsString()).contains("missing");
    }

    @Test
    void saveScript_delegatesToRepositoryAndReturnsSuccess() throws Exception {
        JsonObject json = parse(handler.saveScript(
            "hello", "print('hi')", "desc", "Designer", "folder", "1.0"));

        assertThat(json.get("success").getAsBoolean()).isTrue();
        verify(repository).saveScript("hello", "print('hi')", "desc", "Designer", "folder", "1.0");
    }

    @Test
    void saveScript_nullRepository_returnsError() throws Exception {
        when(hook.getScriptRepository()).thenReturn(null);

        JsonObject json = parse(handler.saveScript("n", "c", "d", "a", "f", "v"));

        assertThat(json.get("success").getAsBoolean()).isFalse();
    }

    @Test
    void deleteScript_deleted_returnsSuccess() throws Exception {
        when(repository.deleteScript("hello")).thenReturn(true);

        JsonObject json = parse(handler.deleteScript("hello"));

        assertThat(json.get("success").getAsBoolean()).isTrue();
        assertThat(json.get("message").getAsString()).contains("deleted");
    }

    @Test
    void deleteScript_notFound_returnsFailure() throws Exception {
        when(repository.deleteScript("missing")).thenReturn(false);

        JsonObject json = parse(handler.deleteScript("missing"));

        assertThat(json.get("success").getAsBoolean()).isFalse();
        assertThat(json.get("message").getAsString()).contains("missing");
    }

    // -------------------------------------------------------------------------
    // Execution
    // -------------------------------------------------------------------------

    @Test
    void exec_success_returnsResult() throws Exception {
        when(scriptModule.exec(eq("2+2"), anyMap(), anyString())).thenReturn("4");

        JsonObject json = parse(handler.exec("2+2", "{}", ""));

        assertThat(json.get("success").getAsBoolean()).isTrue();
        assertThat(json.get("result").getAsString()).isEqualTo("4");
    }

    @Test
    void exec_withVersion_usesVersionedOverload() throws Exception {
        when(scriptModule.exec(eq("x=1"), anyMap(), anyString(), eq("3.11"))).thenReturn("ok");

        JsonObject json = parse(handler.exec("x=1", "{\"a\":1}", "3.11"));

        assertThat(json.get("success").getAsBoolean()).isTrue();
        assertThat(json.get("result").getAsString()).isEqualTo("ok");
        verify(scriptModule).exec(eq("x=1"), anyMap(), anyString(), eq("3.11"));
    }

    @Test
    void exec_failure_returnsErrorMessage() throws Exception {
        when(scriptModule.exec(anyString(), anyMap(), anyString()))
            .thenThrow(new RuntimeException("boom"));

        JsonObject json = parse(handler.exec("bad", "{}", ""));

        assertThat(json.get("success").getAsBoolean()).isFalse();
        assertThat(json.get("error").getAsString()).isEqualTo("boom");
    }

    @Test
    void eval_success_returnsResult() throws Exception {
        when(scriptModule.eval(eq("1+1"), anyMap(), anyString())).thenReturn("2");

        JsonObject json = parse(handler.eval("1+1", null, null));

        assertThat(json.get("success").getAsBoolean()).isTrue();
        assertThat(json.get("result").getAsString()).isEqualTo("2");
    }

    @Test
    void eval_failure_returnsErrorMessage() throws Exception {
        when(scriptModule.eval(anyString(), anyMap(), anyString()))
            .thenThrow(new RuntimeException("nope"));

        JsonObject json = parse(handler.eval("bad", "{}", ""));

        assertThat(json.get("success").getAsBoolean()).isFalse();
        assertThat(json.get("error").getAsString()).isEqualTo("nope");
    }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    @Test
    void getVersion_derivesPythonVersionField() throws Exception {
        Map<String, Object> version = new HashMap<>();
        version.put("version", "3.11.2");
        when(scriptModule.getVersion()).thenReturn(version);

        JsonObject json = parse(handler.getVersion());

        assertThat(json.get("pythonVersion").getAsString()).isEqualTo("3.11.2");
    }

    @Test
    void getPoolStats_addsHealthCheckStatusWhenMissing() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSize", 3);
        when(scriptModule.getPoolStats()).thenReturn(stats);
        when(scriptModule.isAvailable()).thenReturn(true);

        JsonObject json = parse(handler.getPoolStats());

        assertThat(json.get("totalSize").getAsInt()).isEqualTo(3);
        assertThat(json.get("healthCheckStatus").getAsString()).isEqualTo("Healthy");
    }

    @Test
    void health_reportsAvailability() throws Exception {
        when(scriptModule.isAvailable()).thenReturn(true);

        JsonObject json = parse(handler.health());

        assertThat(json.get("healthy").getAsBoolean()).isTrue();
        assertThat(json.get("available").getAsBoolean()).isTrue();
        assertThat(json.get("status").getAsString()).isEqualTo("HEALTHY");
    }

    // -------------------------------------------------------------------------
    // Session gate
    // -------------------------------------------------------------------------

    @Test
    void requireAuthenticatedSession_withoutSession_throws() {
        // The real gate (not the test override): outside an RPC dispatch there is no
        // ClientReqSession bound, so it must refuse.
        Python3RpcHandler realHandler = new Python3RpcHandler(hook);
        assertThatThrownBy(realHandler::requireAuthenticatedSession)
            .isInstanceOf(RuntimeException.class);
    }

    private static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }
}
