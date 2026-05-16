package com.gaskony.python3.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for InputValidator.
 *
 * InputValidator has no Ignition SDK dependencies — pure unit tests.
 * Uses the multi-arg constructor to control which patterns are active.
 */
class InputValidatorTest {

    // Permissive validator: allows file, subprocess, network
    private InputValidator permissive;

    // Strict validator: blocks file, subprocess, network (default-like)
    private InputValidator strict;

    @BeforeEach
    void setUp() {
        permissive = new InputValidator(
            1_048_576, 100, 1_048_576,
            true, true, true, true
        );
        strict = new InputValidator(
            1_048_576, 100, 1_048_576,
            false, false, false, true
        );
    }

    // ===== validateCode — basic cases =====

    @Test
    void validateCode_null_doesNotThrow() {
        assertThatCode(() -> strict.validateCode(null)).doesNotThrowAnyException();
    }

    @Test
    void validateCode_emptyString_doesNotThrow() {
        assertThatCode(() -> strict.validateCode("")).doesNotThrowAnyException();
    }

    @Test
    void validateCode_simpleCode_doesNotThrow() {
        assertThatCode(() -> strict.validateCode("x = 2 + 2")).doesNotThrowAnyException();
    }

    // ===== validateCode — code length =====

    @Test
    void validateCode_codeAtLimit_doesNotThrow() throws Exception {
        InputValidator v = new InputValidator(10, 100, 100, false, false, false, true);
        String code = "x" + "x".repeat(9); // exactly 10 chars
        assertThatCode(() -> v.validateCode(code)).doesNotThrowAnyException();
    }

    @Test
    void validateCode_codeExceedsLimit_throwsValidationException() {
        InputValidator v = new InputValidator(10, 100, 100, false, false, false, true);
        String code = "x".repeat(11); // 11 bytes > 10
        assertThatThrownBy(() -> v.validateCode(code))
            .isInstanceOf(InputValidator.ValidationException.class)
            .hasMessageContaining("exceeds maximum");
    }

    // ===== validateCode — pattern detection disabled =====

    @Test
    void validateCode_patternDetectionDisabled_allowsDangerousCode() {
        InputValidator noPatterns = new InputValidator(
            1_048_576, 100, 1_048_576, false, false, false, false
        );
        assertThatCode(() -> noPatterns.validateCode("eval('hello')")).doesNotThrowAnyException();
    }

    // ===== validateCode — file access patterns =====

    @Test
    void validateCode_openCall_strictValidator_throwsValidationException() {
        assertThatThrownBy(() -> strict.validateCode("f = open('file.txt')"))
            .isInstanceOf(InputValidator.ValidationException.class)
            .hasMessageContaining("Security validation failed");
    }

    @Test
    void validateCode_openCall_permissiveValidator_doesNotThrow() {
        assertThatCode(() -> permissive.validateCode("f = open('file.txt')"))
            .doesNotThrowAnyException();
    }

    @Test
    void validateCode_osRemove_strictValidator_throwsValidationException() {
        assertThatThrownBy(() -> strict.validateCode("os.remove('/tmp/file')"))
            .isInstanceOf(InputValidator.ValidationException.class);
    }

    @Test
    void validateCode_shutilRmtree_strictValidator_throwsValidationException() {
        assertThatThrownBy(() -> strict.validateCode("shutil.rmtree('/tmp')"))
            .isInstanceOf(InputValidator.ValidationException.class);
    }

    // ===== validateCode — subprocess patterns =====

    @Test
    void validateCode_subprocessRun_strictValidator_throwsValidationException() {
        assertThatThrownBy(() -> strict.validateCode("subprocess.run(['ls'])"))
            .isInstanceOf(InputValidator.ValidationException.class);
    }

    @Test
    void validateCode_osPopen_strictValidator_throwsValidationException() {
        assertThatThrownBy(() -> strict.validateCode("os.popen('ls')"))
            .isInstanceOf(InputValidator.ValidationException.class);
    }

    @Test
    void validateCode_dynamicOsImport_strictValidator_throwsValidationException() {
        assertThatThrownBy(() -> strict.validateCode("__import__('os')"))
            .isInstanceOf(InputValidator.ValidationException.class);
    }

    // ===== validateCode — network patterns =====

    @Test
    void validateCode_socketCall_strictValidator_throwsValidationException() {
        assertThatThrownBy(() -> strict.validateCode("socket.connect(('1.2.3.4', 80))"))
            .isInstanceOf(InputValidator.ValidationException.class);
    }

    @Test
    void validateCode_requestsGet_strictValidator_throwsValidationException() {
        assertThatThrownBy(() -> strict.validateCode("requests.get('http://example.com')"))
            .isInstanceOf(InputValidator.ValidationException.class);
    }

    // ===== validateCode — always-block patterns =====

    @Test
    void validateCode_evalCall_alwaysBlocked() {
        // Blocked even in permissive mode
        assertThatThrownBy(() -> permissive.validateCode("eval('x + y')"))
            .isInstanceOf(InputValidator.ValidationException.class)
            .hasMessageContaining("eval/exec");
    }

    @Test
    void validateCode_execCall_alwaysBlocked() {
        assertThatThrownBy(() -> permissive.validateCode("exec('import os')"))
            .isInstanceOf(InputValidator.ValidationException.class);
    }

    @Test
    void validateCode_dynamicSubprocessImport_alwaysBlocked() {
        assertThatThrownBy(() -> permissive.validateCode("__import__('subprocess')"))
            .isInstanceOf(InputValidator.ValidationException.class);
    }

    // ===== validateCode — WARN patterns (not thrown) =====

    @Test
    void validateCode_whileTrue_doesNotThrow() {
        // "while True:" is a WARN-level pattern, should not throw
        assertThatCode(() -> strict.validateCode("while True:\n    pass")).doesNotThrowAnyException();
    }

    @Test
    void validateCode_compileCall_doesNotThrow() {
        // "compile(" is a WARN-level pattern in permissive mode, should not throw
        assertThatCode(() -> permissive.validateCode("compile('x=1', '<>', 'exec')"))
            .doesNotThrowAnyException();
    }

    @Test
    void validateCode_pickleLoads_doesNotThrow() {
        // "pickle.loads(" is WARN-level, should not throw
        assertThatCode(() -> permissive.validateCode("pickle.loads(data)")).doesNotThrowAnyException();
    }

    // ===== validateVariables — basic cases =====

    @Test
    void validateVariables_null_doesNotThrow() {
        assertThatCode(() -> strict.validateVariables(null)).doesNotThrowAnyException();
    }

    @Test
    void validateVariables_emptyMap_doesNotThrow() {
        assertThatCode(() -> strict.validateVariables(Collections.emptyMap())).doesNotThrowAnyException();
    }

    @Test
    void validateVariables_validVars_doesNotThrow() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("x", 42);
        vars.put("my_name", "hello");
        vars.put("_private", 3.14);
        assertThatCode(() -> strict.validateVariables(vars)).doesNotThrowAnyException();
    }

    // ===== validateVariables — count limit =====

    @Test
    void validateVariables_tooManyVars_throwsValidationException() {
        InputValidator v = new InputValidator(1_048_576, 2, 1_048_576, false, false, false, true);
        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 1);
        vars.put("b", 2);
        vars.put("c", 3); // 3 > limit of 2
        assertThatThrownBy(() -> v.validateVariables(vars))
            .isInstanceOf(InputValidator.ValidationException.class)
            .hasMessageContaining("Variable count");
    }

    // ===== validateVariables — variable size limit =====

    @Test
    void validateVariables_largeString_throwsValidationException() {
        InputValidator v = new InputValidator(1_048_576, 100, 10, false, false, false, true);
        Map<String, Object> vars = new HashMap<>();
        vars.put("bigVar", "x".repeat(11)); // 11 bytes > 10 byte limit
        assertThatThrownBy(() -> v.validateVariables(vars))
            .isInstanceOf(InputValidator.ValidationException.class)
            .hasMessageContaining("size");
    }

    // ===== validateVariables — variable name validation =====

    @Test
    void validateVariables_nullVariableName_throwsValidationException() {
        Map<String, Object> vars = new HashMap<>();
        vars.put(null, "value");
        assertThatThrownBy(() -> strict.validateVariables(vars))
            .isInstanceOf(InputValidator.ValidationException.class);
    }

    @Test
    void validateVariables_emptyVariableName_throwsValidationException() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("", "value");
        assertThatThrownBy(() -> strict.validateVariables(vars))
            .isInstanceOf(InputValidator.ValidationException.class);
    }

    @Test
    void validateVariables_reservedKeyword_throwsValidationException() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("if", 42);
        assertThatThrownBy(() -> strict.validateVariables(vars))
            .isInstanceOf(InputValidator.ValidationException.class)
            .hasMessageContaining("reserved keyword");
    }

    @Test
    void validateVariables_invalidIdentifier_throwsValidationException() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("123invalid", 42);
        assertThatThrownBy(() -> strict.validateVariables(vars))
            .isInstanceOf(InputValidator.ValidationException.class)
            .hasMessageContaining("not a valid Python identifier");
    }

    @Test
    void validateVariables_identifierWithDash_throwsValidationException() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("my-var", 42);
        assertThatThrownBy(() -> strict.validateVariables(vars))
            .isInstanceOf(InputValidator.ValidationException.class);
    }

    // ===== validateVariables — variable size for different types =====

    @Test
    void validateVariables_nullValue_doesNotThrow() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("x", null);
        assertThatCode(() -> strict.validateVariables(vars)).doesNotThrowAnyException();
    }

    @Test
    void validateVariables_intValue_doesNotThrow() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("count", 42);
        assertThatCode(() -> strict.validateVariables(vars)).doesNotThrowAnyException();
    }

    @Test
    void validateVariables_listValue_doesNotThrow() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("items", java.util.List.of("a", "b", "c"));
        assertThatCode(() -> strict.validateVariables(vars)).doesNotThrowAnyException();
    }

    @Test
    void validateVariables_mapValue_doesNotThrow() {
        Map<String, Object> vars = new HashMap<>();
        Map<String, Object> nested = new HashMap<>();
        nested.put("key", "value");
        vars.put("data", nested);
        assertThatCode(() -> strict.validateVariables(vars)).doesNotThrowAnyException();
    }

    @Test
    void validateVariables_byteArrayValue_doesNotThrow() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("raw", new byte[]{1, 2, 3});
        assertThatCode(() -> strict.validateVariables(vars)).doesNotThrowAnyException();
    }

    // ===== validateExecutionRequest =====

    @Test
    void validateExecutionRequest_validCodeAndVars_doesNotThrow() {
        assertThatCode(() -> strict.validateExecutionRequest("x = 1", Collections.emptyMap()))
            .doesNotThrowAnyException();
    }

    @Test
    void validateExecutionRequest_invalidCode_throwsValidationException() {
        assertThatThrownBy(() -> strict.validateExecutionRequest("eval('x')", Collections.emptyMap()))
            .isInstanceOf(InputValidator.ValidationException.class);
    }

    // ===== getters =====

    @Test
    void getters_returnConfiguredValues() {
        InputValidator v = new InputValidator(100, 5, 200, true, false, true, false);
        assertThat(v.getMaxCodeLength()).isEqualTo(100);
        assertThat(v.getMaxVariables()).isEqualTo(5);
        assertThat(v.getMaxVariableSize()).isEqualTo(200);
        assertThat(v.isAllowFileAccess()).isTrue();
        assertThat(v.isAllowSubprocess()).isFalse();
        assertThat(v.isAllowNetwork()).isTrue();
        assertThat(v.isEnforcePatternDetection()).isFalse();
    }

    @Test
    void getPatternCount_strictValidator_hasMorePatterns() {
        // Strict has file + subprocess + network patterns; permissive does not
        assertThat(strict.getPatternCount()).isGreaterThan(permissive.getPatternCount());
    }

    @Test
    void toString_containsConfig() {
        assertThat(strict.toString()).contains("InputValidator");
    }

    // ===== Reserved keywords =====

    @Test
    void validateVariables_evalKeyword_throwsValidationException() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("eval", 42);
        assertThatThrownBy(() -> strict.validateVariables(vars))
            .isInstanceOf(InputValidator.ValidationException.class);
    }

    @Test
    void validateVariables_importKeyword_throwsValidationException() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("import", 42);
        assertThatThrownBy(() -> strict.validateVariables(vars))
            .isInstanceOf(InputValidator.ValidationException.class);
    }

    @Test
    void validateVariables_trueKeyword_throwsValidationException() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("True", 42);
        assertThatThrownBy(() -> strict.validateVariables(vars))
            .isInstanceOf(InputValidator.ValidationException.class);
    }
}
