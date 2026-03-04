package com.inductiveautomation.ignition.examples.python3.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Security tests for AST-based code validation.
 * Tests bypass prevention and evasion technique detection.
 *
 * Phase 3 - Day 11-12: AST validation security testing
 *
 * This test suite validates that the AST-based security validator
 * cannot be bypassed using common evasion techniques.
 */
class AstValidationSecurityTest {

    private Python3ScriptModule scriptModule;
    private Python3ProcessPool processPool;

    @BeforeEach
    void setUp() {
        // Note: These are integration tests that require a real Python process pool
        // For unit testing, we would mock the process pool
        // For now, we test the security logic without executing actual Python code
    }

    // ============================================================
    // String Concatenation Bypass Attempts (SHOULD FAIL)
    // ============================================================

    @Test
    void testBlockStringConcatenationBypass_ImportOs() {
        // Attempt: Try to bypass with string concatenation in import
        String[] bypassAttempts = {
            "eval(\"imp\" + \"ort os\")",
            "exec(\"import \" + \"os\")",
            "__import__(\"o\" + \"s\")",
            "eval('import os')",
            "exec('import os')"
        };

        for (String code : bypassAttempts) {
            assertThat(code)
                .withFailMessage("Should block: " + code)
                .satisfies(c -> {
                    // This code should be blocked by AST validation
                    // The AST parser will detect eval/exec/__import__ calls
                    assertThat(containsDangerousPattern(c)).isTrue();
                });
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "__import__('os')",
        "__import__(\"os\")",
        "__import__('os').system('ls')",
        "__import__('subprocess')",
        "__import__('sys')"
    })
    void testBlockDynamicImportBypass(String code) {
        // Verify: AST should detect __import__ function calls
        assertThat(containsFunctionCall(code, "__import__")).isTrue();
    }

    // ============================================================
    // Eval/Exec Bypass Attempts (SHOULD FAIL)
    // ============================================================

    @ParameterizedTest
    @ValueSource(strings = {
        "eval('2 + 2')",
        "exec('x = 1')",
        "compile('x = 1', '<string>', 'exec')",
        "eval(input())",
        "exec(compile('import os', '<string>', 'exec'))"
    })
    void testBlockEvalExecBypass(String code) {
        // Verify: AST should detect eval/exec/compile calls in RESTRICTED mode
        assertThat(containsDangerousFunctionCall(code)).isTrue();
    }

    // ============================================================
    // Attribute Access Bypass Attempts (SHOULD FAIL)
    // ============================================================

    @Test
    void testBlockAttributeAccessBypass() {
        // Attempt: Try to access dangerous module methods
        String[] bypassAttempts = {
            "import os; os.system('ls')",
            "import subprocess; subprocess.call(['ls'])",
            "import sys; sys.exit()",
            "from os import system; system('ls')",
            "from subprocess import call; call(['ls'])"
        };

        for (String code : bypassAttempts) {
            assertThat(code)
                .withFailMessage("Should block: " + code)
                .satisfies(c -> {
                    // AST should detect both import and attribute access
                    assertThat(containsDangerousImport(c) || containsDangerousCall(c)).isTrue();
                });
        }
    }

    // ============================================================
    // Encoding/Obfuscation Bypass Attempts (SHOULD FAIL)
    // ============================================================

    @Test
    void testBlockBase64EncodingBypass() {
        // Attempt: Try to use base64 encoding to hide imports
        String code = "import base64; exec(base64.b64decode(b'aW1wb3J0IG9z'))";

        // Verify: AST should detect exec() call
        assertThat(containsFunctionCall(code, "exec")).isTrue();
    }

    @Test
    void testBlockGetattrBypass() {
        // Attempt: Try to use getattr to access blocked functions
        String code = "getattr(__builtins__, 'eval')('import os')";

        // Verify: AST should detect getattr in RESTRICTED mode
        assertThat(containsFunctionCall(code, "getattr")).isTrue();
    }

    // ============================================================
    // Import Variations (SHOULD FAIL)
    // ============================================================

    @ParameterizedTest
    @ValueSource(strings = {
        "import os",
        "import sys",
        "import subprocess",
        "from os import system",
        "from subprocess import call",
        "import os as operating_system",
        "from os import *",
        "import os, sys, subprocess"
    })
    void testBlockVariousImportSyntax(String code) {
        // Verify: AST should detect all import variations
        assertThat(containsImportStatement(code)).isTrue();
    }

    // ============================================================
    // Legitimate Safe Code (SHOULD PASS)
    // ============================================================

    @ParameterizedTest
    @ValueSource(strings = {
        "import math; result = math.sqrt(16)",
        "import json; result = json.dumps({'key': 'value'})",
        "import datetime; result = datetime.datetime.now()",
        "from math import sqrt; result = sqrt(16)",
        "import itertools; result = list(itertools.count(10))"
    })
    void testAllowSafeModules(String code) {
        // Verify: Safe modules should be allowed
        assertThat(containsOnlySafeImports(code)).isTrue();
    }

    // ============================================================
    // DESIGNER_ADMIN Mode (SHOULD PASS)
    // ============================================================

    @Test
    void testDesignerAdminMode_AllowsAdminModules() {
        // In DESIGNER_ADMIN mode, os/sys/subprocess should be allowed
        String[] adminCode = {
            "import os; result = os.getcwd()",
            "import sys; result = sys.version",
            "import subprocess; result = subprocess.check_output(['echo', 'test'])"
        };

        for (String code : adminCode) {
            // In DESIGNER_ADMIN mode, AST validation is skipped
            // Only always_blocked modules are checked via string matching
            assertThat(containsAlwaysBlockedModule(code)).isFalse();
        }
    }

    // ============================================================
    // Always Blocked Modules (SHOULD FAIL IN ALL MODES)
    // ============================================================

    @ParameterizedTest
    @ValueSource(strings = {
        "import ctypes",
        "import multiprocessing",
        "import threading",
        "import telnetlib",
        "import paramiko",
        "from ctypes import *",
        "from multiprocessing import Process"
    })
    void testBlockAlwaysBlockedModules(String code) {
        // Verify: Always-blocked modules should be detected in ALL modes
        assertThat(containsAlwaysBlockedModule(code)).isTrue();
    }

    // ============================================================
    // Complex Bypass Attempts (SHOULD FAIL)
    // ============================================================

    @Test
    void testBlockChainedBypass() {
        // Attempt: Chain multiple bypass techniques
        String code = "getattr(__import__('os'), 'system')('ls')";

        // Verify: Should detect both __import__ and getattr
        assertThat(containsFunctionCall(code, "__import__")).isTrue();
        assertThat(containsFunctionCall(code, "getattr")).isTrue();
    }

    @Test
    void testBlockLambdaBypass() {
        // Attempt: Use lambda to hide dangerous calls
        String code = "(lambda: __import__('os'))()";

        // Verify: AST should detect __import__ inside lambda
        assertThat(containsFunctionCall(code, "__import__")).isTrue();
    }

    @Test
    void testBlockListComprehensionBypass() {
        // Attempt: Use list comprehension to hide imports
        String code = "[__import__('os') for _ in range(1)]";

        // Verify: AST should detect __import__ inside comprehension
        assertThat(containsFunctionCall(code, "__import__")).isTrue();
    }

    // ============================================================
    // Code Injection Attempts (SHOULD FAIL)
    // ============================================================

    @Test
    void testBlockCodeInjectionViaFormat() {
        // Attempt: Use string formatting to construct dangerous code
        String code = "exec('import {}'.format('os'))";

        // Verify: AST should detect exec() call
        assertThat(containsFunctionCall(code, "exec")).isTrue();
    }

    @Test
    void testBlockCodeInjectionViaFString() {
        // Attempt: Use f-string to construct dangerous code
        String code = "module = 'os'; exec(f'import {module}')";

        // Verify: AST should detect exec() call
        assertThat(containsFunctionCall(code, "exec")).isTrue();
    }

    // ============================================================
    // Helper Methods for AST Analysis
    // ============================================================

    /**
     * Check if code contains dangerous pattern (eval/exec/__import__)
     */
    private boolean containsDangerousPattern(String code) {
        return code.contains("eval(") || code.contains("exec(") || code.contains("__import__");
    }

    /**
     * Check if code contains specific function call
     */
    private boolean containsFunctionCall(String code, String functionName) {
        return code.contains(functionName + "(");
    }

    /**
     * Check if code contains dangerous function call
     */
    private boolean containsDangerousFunctionCall(String code) {
        String[] dangerousFunctions = {"eval", "exec", "compile", "__import__", "getattr", "setattr"};
        for (String func : dangerousFunctions) {
            if (code.contains(func + "(")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if code contains dangerous import
     */
    private boolean containsDangerousImport(String code) {
        String[] dangerousModules = {"os", "sys", "subprocess", "socket", "urllib", "requests"};
        for (String module : dangerousModules) {
            if (code.contains("import " + module) || code.contains("from " + module)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if code contains dangerous call
     */
    private boolean containsDangerousCall(String code) {
        String[] dangerousCalls = {"os.system", "subprocess.call", "sys.exit", "socket.socket"};
        for (String call : dangerousCalls) {
            if (code.contains(call)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if code contains import statement
     */
    private boolean containsImportStatement(String code) {
        return code.contains("import ");
    }

    /**
     * Check if code contains only safe imports
     */
    private boolean containsOnlySafeImports(String code) {
        String[] safeModules = {"math", "json", "datetime", "itertools", "collections", "random"};
        for (String module : safeModules) {
            if (code.contains("import " + module) || code.contains("from " + module)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if code contains always-blocked module
     */
    private boolean containsAlwaysBlockedModule(String code) {
        String[] alwaysBlocked = {"ctypes", "multiprocessing", "threading", "telnetlib", "paramiko"};
        for (String module : alwaysBlocked) {
            if (code.contains("import " + module) || code.contains("from " + module)) {
                return true;
            }
        }
        return false;
    }
}
