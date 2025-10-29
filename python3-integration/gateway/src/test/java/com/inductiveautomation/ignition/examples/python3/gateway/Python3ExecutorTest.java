package com.inductiveautomation.ignition.examples.python3.gateway;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * Unit tests for Python3Executor.
 *
 * These tests verify the executor's ability to:
 * - Execute Python code and capture output
 * - Inject variables into Python environment
 * - Handle timeouts and errors gracefully
 * - Process various data types and encodings
 * - Recover from failures
 *
 * @since v2.12.0 (Phase 2 Week 1-2: Testing Infrastructure)
 */
public class Python3ExecutorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(Python3ExecutorTest.class);
    private static final String TEST_PYTHON_PATH = "python3";

    private Python3Executor executor;

    @Before
    public void setUp() throws IOException {
        LOGGER.info("Setting up Python3ExecutorTest");
        executor = new Python3Executor(TEST_PYTHON_PATH);
    }

    @After
    public void tearDown() {
        if (executor != null) {
            try {
                executor.shutdown();
            } catch (Exception e) {
                LOGGER.warn("Error during executor shutdown in tearDown", e);
            }
        }
    }

    /**
     * Test 1: Simple code execution with print statement.
     */
    @Test
    public void testSimpleCodeExecution() throws Exception {
        LOGGER.info("Test: Simple code execution");

        Map<String, Object> variables = new HashMap<>();
        Python3Result result = executor.execute("print('hello world')", variables);

        assertNotNull("Result should not be null", result);
        assertTrue("Execution should succeed", result.isSuccess());
        assertNull("Should not have error", result.getError());

        // Output should contain "hello world"
        String output = result.getResult();
        assertNotNull("Output should not be null", output);
        assertTrue("Output should contain 'hello world'", output.contains("hello world"));

        LOGGER.info("✓ Simple execution test passed");
    }

    /**
     * Test 2: Variable injection into Python environment.
     */
    @Test
    public void testVariableInjection() throws Exception {
        LOGGER.info("Test: Variable injection");

        Map<String, Object> variables = new HashMap<>();
        variables.put("x", 5);
        variables.put("y", 10);

        Python3Result result = executor.execute("result = x + y\nprint(result)", variables);

        assertTrue("Execution should succeed", result.isSuccess());
        String output = result.getResult();
        assertTrue("Output should contain '15'", output.contains("15"));

        LOGGER.info("✓ Variable injection test passed");
    }

    /**
     * Test 3: Expression evaluation (eval mode).
     */
    @Test
    public void testExpressionEvaluation() throws Exception {
        LOGGER.info("Test: Expression evaluation");

        Map<String, Object> variables = new HashMap<>();
        variables.put("a", 3);
        variables.put("b", 7);

        Python3Result result = executor.evaluate("a * b", variables);

        assertTrue("Evaluation should succeed", result.isSuccess());
        assertEquals("Result should be '21'", "21", result.getResult().trim());

        LOGGER.info("✓ Expression evaluation test passed");
    }

    /**
     * Test 4: Syntax error handling.
     * Verifies that syntax errors are caught and reported.
     */
    @Test
    public void testSyntaxErrorHandling() throws Exception {
        LOGGER.info("Test: Syntax error handling");

        Map<String, Object> variables = new HashMap<>();
        Python3Result result = executor.execute("def invalid syntax here", variables);

        assertFalse("Execution should fail", result.isSuccess());
        assertNotNull("Error message should be present", result.getError());
        assertTrue("Error should mention syntax",
            result.getError().toLowerCase().contains("syntax") ||
            result.getError().toLowerCase().contains("invalid"));

        LOGGER.info("✓ Syntax error handling test passed");
    }

    /**
     * Test 5: Runtime error handling (undefined variable).
     */
    @Test
    public void testRuntimeErrorHandling() throws Exception {
        LOGGER.info("Test: Runtime error handling");

        Map<String, Object> variables = new HashMap<>();
        Python3Result result = executor.execute("x = undefined_variable", variables);

        assertFalse("Execution should fail", result.isSuccess());
        assertNotNull("Error message should be present", result.getError());
        assertTrue("Error should mention name or undefined",
            result.getError().toLowerCase().contains("name") ||
            result.getError().toLowerCase().contains("undefined") ||
            result.getError().toLowerCase().contains("not defined"));

        LOGGER.info("✓ Runtime error handling test passed");
    }

    /**
     * Test 6: Timeout handling for long-running code.
     * Note: This test uses a short timeout to avoid slowing down test suite.
     */
    @Test(expected = TimeoutException.class)
    public void testTimeoutHandling() throws Exception {
        LOGGER.info("Test: Timeout handling");

        Map<String, Object> variables = new HashMap<>();

        // Execute code that would run forever
        executor.execute("import time\nwhile True: time.sleep(1)", variables);

        // Should throw TimeoutException before reaching here
        fail("Should have thrown TimeoutException");
    }

    /**
     * Test 7: Large output handling.
     * Verifies that executor can handle large amounts of output.
     */
    @Test
    public void testLargeOutputHandling() throws Exception {
        LOGGER.info("Test: Large output handling");

        Map<String, Object> variables = new HashMap<>();

        // Generate 1000 lines of output
        String code = "for i in range(1000):\n    print(f'Line {i}')";
        Python3Result result = executor.execute(code, variables);

        assertTrue("Execution should succeed", result.isSuccess());
        String output = result.getResult();
        assertNotNull("Output should not be null", output);

        // Should contain references to both early and late lines
        assertTrue("Output should contain Line 0", output.contains("Line 0"));
        assertTrue("Output should contain Line 999", output.contains("Line 999"));

        LOGGER.info("✓ Large output handling test passed");
    }

    /**
     * Test 8: Unicode and special character handling.
     */
    @Test
    public void testUnicodeHandling() throws Exception {
        LOGGER.info("Test: Unicode handling");

        Map<String, Object> variables = new HashMap<>();
        variables.put("emoji", "🐍");
        variables.put("chinese", "你好");
        variables.put("arabic", "مرحبا");

        String code = "print(emoji)\nprint(chinese)\nprint(arabic)";
        Python3Result result = executor.execute(code, variables);

        assertTrue("Execution should succeed", result.isSuccess());
        String output = result.getResult();

        assertTrue("Output should contain emoji", output.contains("🐍"));
        assertTrue("Output should contain Chinese", output.contains("你好"));
        assertTrue("Output should contain Arabic", output.contains("مرحبا"));

        LOGGER.info("✓ Unicode handling test passed");
    }

    /**
     * Test 9: Multiple variable types injection.
     * Tests integers, floats, strings, booleans, and null.
     */
    @Test
    public void testMultipleVariableTypes() throws Exception {
        LOGGER.info("Test: Multiple variable types");

        Map<String, Object> variables = new HashMap<>();
        variables.put("int_var", 42);
        variables.put("float_var", 3.14);
        variables.put("str_var", "hello");
        variables.put("bool_var", true);
        variables.put("null_var", null);

        String code =
            "print(f'int: {int_var}')\n" +
            "print(f'float: {float_var}')\n" +
            "print(f'str: {str_var}')\n" +
            "print(f'bool: {bool_var}')\n" +
            "print(f'null: {null_var}')";

        Python3Result result = executor.execute(code, variables);

        assertTrue("Execution should succeed", result.isSuccess());
        String output = result.getResult();

        assertTrue("Output should contain int", output.contains("int: 42"));
        assertTrue("Output should contain float", output.contains("float: 3.14"));
        assertTrue("Output should contain str", output.contains("str: hello"));
        assertTrue("Output should contain bool", output.contains("bool: True"));
        assertTrue("Output should contain None", output.contains("null: None"));

        LOGGER.info("✓ Multiple variable types test passed");
    }

    /**
     * Test 10: Module import functionality.
     * Verifies that standard library modules can be imported.
     */
    @Test
    public void testModuleImport() throws Exception {
        LOGGER.info("Test: Module import");

        Map<String, Object> variables = new HashMap<>();

        String code =
            "import math\n" +
            "import json\n" +
            "import sys\n" +
            "result = math.sqrt(16)\n" +
            "print(f'sqrt(16) = {result}')";

        Python3Result result = executor.execute(code, variables);

        assertTrue("Execution should succeed", result.isSuccess());
        String output = result.getResult();
        assertTrue("Output should contain sqrt result", output.contains("sqrt(16) = 4"));

        LOGGER.info("✓ Module import test passed");
    }

    /**
     * Test 11: Empty code execution.
     * Verifies handling of empty or whitespace-only code.
     */
    @Test
    public void testEmptyCodeExecution() throws Exception {
        LOGGER.info("Test: Empty code execution");

        Map<String, Object> variables = new HashMap<>();

        // Empty string
        Python3Result result1 = executor.execute("", variables);
        assertTrue("Empty code should succeed", result1.isSuccess());

        // Whitespace only
        Python3Result result2 = executor.execute("   \n  \n  ", variables);
        assertTrue("Whitespace-only code should succeed", result2.isSuccess());

        LOGGER.info("✓ Empty code execution test passed");
    }

    /**
     * Test 12: Exception with traceback.
     * Verifies that full traceback is captured for debugging.
     */
    @Test
    public void testExceptionTraceback() throws Exception {
        LOGGER.info("Test: Exception traceback");

        Map<String, Object> variables = new HashMap<>();

        String code =
            "def func1():\n" +
            "    func2()\n" +
            "\n" +
            "def func2():\n" +
            "    raise ValueError('Test error')\n" +
            "\n" +
            "func1()";

        Python3Result result = executor.execute(code, variables);

        assertFalse("Execution should fail", result.isSuccess());
        assertNotNull("Error should be present", result.getError());

        String error = result.getError();
        assertTrue("Error should contain ValueError", error.contains("ValueError"));
        assertTrue("Error should contain 'Test error'", error.contains("Test error"));
        // Traceback should show the call stack
        assertTrue("Error should contain traceback info",
            error.contains("func1") || error.contains("func2") || error.contains("Traceback"));

        LOGGER.info("✓ Exception traceback test passed");
    }

    /**
     * Test 13: Multiple sequential executions.
     * Verifies that executor can be reused for multiple executions.
     */
    @Test
    public void testMultipleSequentialExecutions() throws Exception {
        LOGGER.info("Test: Multiple sequential executions");

        Map<String, Object> variables = new HashMap<>();

        // Execute 10 different scripts sequentially
        for (int i = 0; i < 10; i++) {
            variables.clear();
            variables.put("iteration", i);

            String code = "result = iteration * 2\nprint(f'Result: {result}')";
            Python3Result result = executor.execute(code, variables);

            assertTrue("Execution " + i + " should succeed", result.isSuccess());
            assertTrue("Output " + i + " should contain correct result",
                result.getResult().contains("Result: " + (i * 2)));
        }

        LOGGER.info("✓ Multiple sequential executions test passed");
    }

    /**
     * Test 14: JSON data handling.
     * Verifies that JSON data can be passed and parsed.
     */
    @Test
    public void testJsonDataHandling() throws Exception {
        LOGGER.info("Test: JSON data handling");

        Map<String, Object> variables = new HashMap<>();
        variables.put("json_str", "{\"name\": \"Python\", \"version\": 3}");

        String code =
            "import json\n" +
            "data = json.loads(json_str)\n" +
            "print(f\"Name: {data['name']}\")\n" +
            "print(f\"Version: {data['version']}\")";

        Python3Result result = executor.execute(code, variables);

        assertTrue("Execution should succeed", result.isSuccess());
        String output = result.getResult();
        assertTrue("Output should contain name", output.contains("Name: Python"));
        assertTrue("Output should contain version", output.contains("Version: 3"));

        LOGGER.info("✓ JSON data handling test passed");
    }

    /**
     * Test 15: Division by zero error handling.
     */
    @Test
    public void testDivisionByZeroError() throws Exception {
        LOGGER.info("Test: Division by zero error");

        Map<String, Object> variables = new HashMap<>();
        Python3Result result = executor.execute("x = 1 / 0", variables);

        assertFalse("Execution should fail", result.isSuccess());
        assertNotNull("Error should be present", result.getError());
        assertTrue("Error should mention division",
            result.getError().toLowerCase().contains("division") ||
            result.getError().toLowerCase().contains("zerodivision"));

        LOGGER.info("✓ Division by zero error test passed");
    }

    /**
     * Test 16: Process health check.
     * Verifies that executor reports correct alive status.
     */
    @Test
    public void testProcessHealthCheck() throws Exception {
        LOGGER.info("Test: Process health check");

        // Initially should be alive
        assertTrue("Executor should be alive initially", executor.isAlive());

        // Execute some code
        Map<String, Object> variables = new HashMap<>();
        executor.execute("x = 1 + 1", variables);

        // Should still be alive after execution
        assertTrue("Executor should still be alive after execution", executor.isAlive());

        // Shutdown
        executor.shutdown();

        // Should not be alive after shutdown
        assertFalse("Executor should not be alive after shutdown", executor.isAlive());

        executor = null; // Don't double-shutdown in tearDown

        LOGGER.info("✓ Process health check test passed");
    }

    /**
     * Test 17: String with quotes handling.
     * Verifies proper escaping of special characters.
     */
    @Test
    public void testStringWithQuotesHandling() throws Exception {
        LOGGER.info("Test: String with quotes handling");

        Map<String, Object> variables = new HashMap<>();
        variables.put("text", "He said \"Hello\" and she said 'Hi'");

        String code = "print(text)";
        Python3Result result = executor.execute(code, variables);

        assertTrue("Execution should succeed", result.isSuccess());
        String output = result.getResult();
        assertTrue("Output should contain double quotes", output.contains("\"Hello\""));
        assertTrue("Output should contain single quotes", output.contains("'Hi'"));

        LOGGER.info("✓ String with quotes handling test passed");
    }

    /**
     * Test 18: Multi-line code with proper indentation.
     */
    @Test
    public void testMultiLineIndentation() throws Exception {
        LOGGER.info("Test: Multi-line indentation");

        Map<String, Object> variables = new HashMap<>();
        variables.put("numbers", "[1, 2, 3, 4, 5]");

        String code =
            "import json\n" +
            "nums = json.loads(numbers)\n" +
            "total = 0\n" +
            "for num in nums:\n" +
            "    if num % 2 == 0:\n" +
            "        total += num\n" +
            "print(f'Sum of even numbers: {total}')";

        Python3Result result = executor.execute(code, variables);

        assertTrue("Execution should succeed", result.isSuccess());
        String output = result.getResult();
        assertTrue("Output should contain sum", output.contains("Sum of even numbers: 6"));

        LOGGER.info("✓ Multi-line indentation test passed");
    }
}
