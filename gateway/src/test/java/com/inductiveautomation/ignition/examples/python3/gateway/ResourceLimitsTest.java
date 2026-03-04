package com.inductiveautomation.ignition.examples.python3.gateway;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ResourceLimits.
 *
 * Tests validation logic for code size, variable count/size, memory, and CPU time.
 * Uses the custom constructor to avoid system property reads.
 * No Ignition SDK required — pure Java logic.
 */
class ResourceLimitsTest {

    /** Create a permissive ResourceLimits for happy-path tests. */
    private ResourceLimits permissive() {
        return new ResourceLimits(
            512,          // memoryLimitMB
            30_000,       // cpuTimeLimitMs
            30_000,       // executionTimeoutMs
            1_048_576,    // maxCodeSize (1 MB)
            100,          // maxVariables
            1_048_576     // maxVariableSize (1 MB)
        );
    }

    // ===== Constructor / getters =====

    @Test
    void customConstructor_setsFields() {
        ResourceLimits rl = new ResourceLimits(256, 15_000, 20_000, 512_000, 50, 65_536);
        assertThat(rl.getMemoryLimitMB()).isEqualTo(256);
        assertThat(rl.getCpuTimeLimitMs()).isEqualTo(15_000);
        assertThat(rl.getExecutionTimeoutMs()).isEqualTo(20_000);
        assertThat(rl.getMaxCodeSize()).isEqualTo(512_000);
        assertThat(rl.getMaxVariables()).isEqualTo(50);
        assertThat(rl.getMaxVariableSize()).isEqualTo(65_536);
    }

    @Test
    void enforceFlags_areAlwaysTrue() {
        ResourceLimits rl = permissive();
        assertThat(rl.isEnforceMemoryLimit()).isTrue();
        assertThat(rl.isEnforceCpuTimeLimit()).isTrue();
        assertThat(rl.isEnforceCodeSizeLimit()).isTrue();
        assertThat(rl.isEnforceVariableLimit()).isTrue();
    }

    // ===== Setters =====

    @Test
    void setMemoryLimitMB_updatesValue() {
        ResourceLimits rl = permissive();
        rl.setMemoryLimitMB(1024);
        assertThat(rl.getMemoryLimitMB()).isEqualTo(1024);
    }

    @Test
    void setCpuTimeLimitMs_updatesValue() {
        ResourceLimits rl = permissive();
        rl.setCpuTimeLimitMs(60_000);
        assertThat(rl.getCpuTimeLimitMs()).isEqualTo(60_000);
    }

    @Test
    void setExecutionTimeoutMs_updatesValue() {
        ResourceLimits rl = permissive();
        rl.setExecutionTimeoutMs(45_000);
        assertThat(rl.getExecutionTimeoutMs()).isEqualTo(45_000);
    }

    @Test
    void setMaxCodeSize_updatesValue() {
        ResourceLimits rl = permissive();
        rl.setMaxCodeSize(2_097_152);
        assertThat(rl.getMaxCodeSize()).isEqualTo(2_097_152);
    }

    @Test
    void setMaxVariables_updatesValue() {
        ResourceLimits rl = permissive();
        rl.setMaxVariables(200);
        assertThat(rl.getMaxVariables()).isEqualTo(200);
    }

    @Test
    void setMaxVariableSize_updatesValue() {
        ResourceLimits rl = permissive();
        rl.setMaxVariableSize(131_072);
        assertThat(rl.getMaxVariableSize()).isEqualTo(131_072);
    }

    // ===== validateCodeSize =====

    @Test
    void validateCodeSize_null_doesNotThrow() {
        assertThatCode(() -> permissive().validateCodeSize(null))
            .doesNotThrowAnyException();
    }

    @Test
    void validateCodeSize_withinLimit_doesNotThrow() {
        assertThatCode(() -> permissive().validateCodeSize("x = 1"))
            .doesNotThrowAnyException();
    }

    @Test
    void validateCodeSize_exceedsLimit_throwsResourceLimitException() {
        ResourceLimits rl = new ResourceLimits(512, 30_000, 30_000, 10, 100, 1_048_576);
        String bigCode = "x".repeat(20); // 20 bytes > 10 byte limit
        assertThatThrownBy(() -> rl.validateCodeSize(bigCode))
            .isInstanceOf(ResourceLimits.ResourceLimitException.class)
            .hasMessageContaining("exceeds limit");
    }

    // ===== validateVariables =====

    @Test
    void validateVariables_emptyMap_doesNotThrow() {
        assertThatCode(() -> permissive().validateVariables(Map.of()))
            .doesNotThrowAnyException();
    }

    @Test
    void validateVariables_withinLimits_doesNotThrow() {
        Map<String, Object> vars = Map.of("x", 42, "y", "hello");
        assertThatCode(() -> permissive().validateVariables(vars))
            .doesNotThrowAnyException();
    }

    @Test
    void validateVariables_tooManyVariables_throwsResourceLimitException() {
        ResourceLimits rl = new ResourceLimits(512, 30_000, 30_000, 1_048_576, 2, 1_048_576);
        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 1);
        vars.put("b", 2);
        vars.put("c", 3); // 3 > limit of 2
        assertThatThrownBy(() -> rl.validateVariables(vars))
            .isInstanceOf(ResourceLimits.ResourceLimitException.class);
    }

    @Test
    void validateVariables_largeStringValue_throwsResourceLimitException() {
        ResourceLimits rl = new ResourceLimits(512, 30_000, 30_000, 1_048_576, 100, 10);
        Map<String, Object> vars = new HashMap<>();
        vars.put("big", "x".repeat(50)); // 50 bytes > 10 byte limit
        assertThatThrownBy(() -> rl.validateVariables(vars))
            .isInstanceOf(ResourceLimits.ResourceLimitException.class);
    }

    @Test
    void validateVariables_integerValue_doesNotThrow() {
        // Integers are small — should always pass
        assertThatCode(() -> permissive().validateVariables(Map.of("n", 12345)))
            .doesNotThrowAnyException();
    }

    @Test
    void validateVariables_listValue_doesNotThrowWhenSmall() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("items", List.of("a", "b")); // small list
        assertThatCode(() -> permissive().validateVariables(vars))
            .doesNotThrowAnyException();
    }

    // ===== validateMemoryUsage =====

    @Test
    void validateMemoryUsage_withinLimit_doesNotThrow() {
        assertThatCode(() -> permissive().validateMemoryUsage(100)) // 100MB < 512MB limit
            .doesNotThrowAnyException();
    }

    @Test
    void validateMemoryUsage_exceedsLimit_throwsResourceLimitException() {
        ResourceLimits rl = new ResourceLimits(64, 30_000, 30_000, 1_048_576, 100, 1_048_576);
        assertThatThrownBy(() -> rl.validateMemoryUsage(100)) // 100MB > 64MB limit
            .isInstanceOf(ResourceLimits.ResourceLimitException.class)
            .hasMessageContaining("exceeds limit");
    }

    // ===== validateCpuTime =====

    @Test
    void validateCpuTime_withinLimit_doesNotThrow() {
        assertThatCode(() -> permissive().validateCpuTime(5_000)) // 5s < 30s limit
            .doesNotThrowAnyException();
    }

    @Test
    void validateCpuTime_exceedsLimit_throwsResourceLimitException() {
        ResourceLimits rl = new ResourceLimits(512, 1_000, 30_000, 1_048_576, 100, 1_048_576);
        assertThatThrownBy(() -> rl.validateCpuTime(5_000)) // 5s > 1s limit
            .isInstanceOf(ResourceLimits.ResourceLimitException.class)
            .hasMessageContaining("exceeds limit");
    }

    // ===== toString =====

    @Test
    void toString_returnsNonEmpty() {
        assertThat(permissive().toString()).isNotEmpty();
    }
}
