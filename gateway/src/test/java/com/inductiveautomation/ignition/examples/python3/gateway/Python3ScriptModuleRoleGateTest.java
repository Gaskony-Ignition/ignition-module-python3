package com.inductiveautomation.ignition.examples.python3.gateway;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the C13 Administrator-role gate on
 * {@link Python3ScriptModule#exec}, {@link Python3ScriptModule#eval},
 * {@link Python3ScriptModule#callModule}, and {@link Python3ScriptModule#callScript}.
 *
 * <p>Each {@code system.python3.*} entry-point that runs Python source must
 * call {@link RoleResolver#requireAdministratorForScripting()} before reaching
 * the process pool. These tests verify that:</p>
 * <ul>
 *   <li>a non-administrator caller is rejected with the expected error
 *       message (and the pool is never touched);</li>
 *   <li>an administrator caller successfully reaches the pool;</li>
 *   <li>the gate covers every code-executing entry-point on the Jython
 *       binding surface.</li>
 * </ul>
 *
 * @since v3.13.0 (C13)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Python3ScriptModuleRoleGateTest {

    /** Stub resolver that always denies. */
    private static final RoleResolver DENY_ALL = new RoleResolver() {
        @Override
        boolean isScriptingCallerAdministrator() {
            return false;
        }
    };

    /** Stub resolver that always allows (Administrator-equivalent for tests). */
    private static final RoleResolver ALLOW_ALL = new RoleResolver() {
        @Override
        boolean isScriptingCallerAdministrator() {
            return true;
        }
    };

    private static final String EXPECTED_DENIED_MESSAGE_FRAGMENT =
        "Administrator role required to execute Python";

    @Mock
    private GatewayHook mockGatewayHook;

    @Mock
    private Python3ProcessPool mockPool;

    @Mock
    private Python3ScriptRepository mockScriptRepository;

    private Python3ScriptModule scriptModule;

    @BeforeEach
    void setUp() {
        when(mockGatewayHook.getProcessPool()).thenReturn(mockPool);
        when(mockGatewayHook.getScriptRepository()).thenReturn(mockScriptRepository);
        scriptModule = new Python3ScriptModule(mockGatewayHook);
    }

    @AfterEach
    void tearDown() {
        Python3ScriptModule.setRoleResolverForTesting(null);
    }

    // ------------------------------------------------------------------
    // Deny path: non-Administrator → RuntimeException, pool never touched
    // ------------------------------------------------------------------

    @Test
    void execIsRejectedForNonAdministrator() throws Exception {
        Python3ScriptModule.setRoleResolverForTesting(DENY_ALL);

        assertThatThrownBy(() -> scriptModule.exec("result = 2 + 2"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining(EXPECTED_DENIED_MESSAGE_FRAGMENT);

        verify(mockPool, never()).execute(any(), anyMap(), any());
    }

    @Test
    void execWithVariablesIsRejectedForNonAdministrator() throws Exception {
        Python3ScriptModule.setRoleResolverForTesting(DENY_ALL);

        assertThatThrownBy(() ->
                scriptModule.exec("result = x", new HashMap<>(), "DESIGNER_ADMIN"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining(EXPECTED_DENIED_MESSAGE_FRAGMENT);

        verify(mockPool, never()).execute(any(), anyMap(), any());
    }

    @Test
    void evalIsRejectedForNonAdministrator() throws Exception {
        Python3ScriptModule.setRoleResolverForTesting(DENY_ALL);

        assertThatThrownBy(() -> scriptModule.eval("2 + 2"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining(EXPECTED_DENIED_MESSAGE_FRAGMENT);

        verify(mockPool, never()).evaluate(any(), anyMap(), any());
    }

    @Test
    void evalWithSecurityModeIsRejectedForNonAdministrator() throws Exception {
        Python3ScriptModule.setRoleResolverForTesting(DENY_ALL);

        assertThatThrownBy(() ->
                scriptModule.eval("x", new HashMap<>(), "ADMIN"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining(EXPECTED_DENIED_MESSAGE_FRAGMENT);

        verify(mockPool, never()).evaluate(any(), anyMap(), any());
    }

    @Test
    void callModuleIsRejectedForNonAdministrator() throws Exception {
        Python3ScriptModule.setRoleResolverForTesting(DENY_ALL);

        assertThatThrownBy(() ->
                scriptModule.callModule("math", "sqrt", Arrays.asList(16)))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining(EXPECTED_DENIED_MESSAGE_FRAGMENT);

        verify(mockPool, never()).callModule(any(), any(), any(), anyMap(), any());
    }

    @Test
    void callScriptIsRejectedForNonAdministrator() throws Exception {
        Python3ScriptModule.setRoleResolverForTesting(DENY_ALL);

        assertThatThrownBy(() ->
                scriptModule.callScript("MyScript",
                        Collections.emptyList(),
                        Collections.emptyMap()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining(EXPECTED_DENIED_MESSAGE_FRAGMENT);

        // Pool/repository must not be touched on the deny path.
        verify(mockPool, never()).execute(any(), anyMap(), any());
        verify(mockPool, never()).execute(any(), anyMap());
    }

    // ------------------------------------------------------------------
    // Allow path: Administrator-equivalent caller proceeds normally
    // ------------------------------------------------------------------

    @Test
    void execSucceedsForAdministrator() throws Exception {
        Python3ScriptModule.setRoleResolverForTesting(ALLOW_ALL);
        Python3Result success = new Python3Result(true, 4.0, null, null);
        when(mockPool.execute(eq("result = 2 + 2"), anyMap(), eq("DESIGNER_ADMIN")))
            .thenReturn(success);

        Object result = scriptModule.exec("result = 2 + 2");

        assertThat(result).isEqualTo(4.0);
        verify(mockPool).execute(eq("result = 2 + 2"), anyMap(), eq("DESIGNER_ADMIN"));
    }

    @Test
    void evalSucceedsForAdministrator() throws Exception {
        Python3ScriptModule.setRoleResolverForTesting(ALLOW_ALL);
        Python3Result success = new Python3Result(true, 1024.0, null, null);
        when(mockPool.evaluate(eq("2 ** 10"), anyMap(), eq("DESIGNER_ADMIN")))
            .thenReturn(success);

        Object result = scriptModule.eval("2 ** 10");

        assertThat(result).isEqualTo(1024.0);
        verify(mockPool).evaluate(eq("2 ** 10"), anyMap(), eq("DESIGNER_ADMIN"));
    }

    @Test
    void callModuleSucceedsForAdministrator() throws Exception {
        Python3ScriptModule.setRoleResolverForTesting(ALLOW_ALL);
        Python3Result success = new Python3Result(true, 4.0, null, null);
        when(mockPool.callModule(eq("math"), eq("sqrt"), eq(Arrays.asList(16)),
                anyMap(), eq("DESIGNER_ADMIN")))
            .thenReturn(success);

        Object result = scriptModule.callModule("math", "sqrt", Arrays.asList(16));

        assertThat(result).isEqualTo(4.0);
    }

    // ------------------------------------------------------------------
    // System-property gate (default RoleResolver behaviour)
    // ------------------------------------------------------------------

    @Test
    void defaultResolver_systemPropertyOff_deniesScripting() throws Exception {
        // No override → RoleResolver.getDefault() reads the system property.
        // With the property unset, scripting access must be denied.
        String prop = "ignition.python3.scriptingFunctions.allowed";
        String previous = System.getProperty(prop);
        System.clearProperty(prop);
        try {
            Python3ScriptModule.setRoleResolverForTesting(null); // default resolver

            assertThatThrownBy(() -> scriptModule.exec("result = 1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(EXPECTED_DENIED_MESSAGE_FRAGMENT);
        } finally {
            if (previous != null) {
                System.setProperty(prop, previous);
            }
        }
    }

    @Test
    void defaultResolver_systemPropertyOn_allowsScripting() throws Exception {
        String prop = "ignition.python3.scriptingFunctions.allowed";
        String previous = System.getProperty(prop);
        System.setProperty(prop, "true");
        try {
            Python3ScriptModule.setRoleResolverForTesting(null);

            Python3Result success = new Python3Result(true, 1L, null, null);
            when(mockPool.execute(eq("result = 1"), anyMap(), eq("DESIGNER_ADMIN")))
                .thenReturn(success);

            Object result = scriptModule.exec("result = 1");
            assertThat(result).isEqualTo(1L);
        } finally {
            if (previous == null) {
                System.clearProperty(prop);
            } else {
                System.setProperty(prop, previous);
            }
        }
    }

    // ------------------------------------------------------------------
    // RESTRICTED removal: compile-time check that the symbol is gone.
    // ------------------------------------------------------------------

    @Test
    void restrictedEnumValueIsRemoved() {
        // C13: SecurityMode.RESTRICTED is gone. We verify by walking the enum.
        boolean hasRestricted = false;
        for (SecurityMode m : SecurityMode.values()) {
            if ("RESTRICTED".equals(m.name())) {
                hasRestricted = true;
                break;
            }
        }
        assertThat(hasRestricted)
            .as("SecurityMode.RESTRICTED must remain removed (C13). The mode "
                + "purported to confine untrusted callers but the underlying "
                + "sandbox in python_bridge.py was trivially bypassable.")
            .isFalse();
    }
}
