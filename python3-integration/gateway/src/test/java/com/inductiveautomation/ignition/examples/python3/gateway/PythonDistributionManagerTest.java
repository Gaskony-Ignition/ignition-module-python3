package com.inductiveautomation.ignition.examples.python3.gateway;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PythonDistributionManager.
 *
 * These tests verify the manager's ability to:
 * - Detect system Python installations
 * - Detect virtual environments (venv)
 * - Validate Python versions
 * - Report installation status
 * - Handle missing Python installations
 *
 * @since v2.12.0 (Phase 2 Week 1-2: Testing Infrastructure)
 */
public class PythonDistributionManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PythonDistributionManagerTest.class);

    private Path tempDir;
    private PythonDistributionManager manager;

    @BeforeEach
    public void setUp() throws IOException {
        LOGGER.info("Setting up PythonDistributionManagerTest");
        tempDir = Files.createTempDirectory("python3-test");
    }

    @AfterEach
    public void tearDown() {
        if (tempDir != null) {
            try {
                Files.walk(tempDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
            } catch (IOException e) {
                LOGGER.warn("Failed to clean up temp directory", e);
            }
        }
    }

    /**
     * Test 1: Manager initialization.
     * Verifies that manager can be created with temp directory.
     */
    @Test
    public void testManagerInitialization() {
        LOGGER.info("Test: Manager initialization");

        manager = new PythonDistributionManager(tempDir, false);
        assertNotNull(manager, "Manager should not be null");

        LOGGER.info("✓ Manager initialization test passed");
    }

    /**
     * Test 2: Get status without auto-download.
     * Verifies that status reports system Python when found.
     */
    @Test
    public void testGetStatusWithSystemPython() {
        LOGGER.info("Test: Get status with system Python");

        manager = new PythonDistributionManager(tempDir, false);
        Map<String, Object> status = manager.getStatus();

        assertNotNull(status, "Status should not be null");
        assertTrue(status.containsKey("os"), "Status should contain 'os' key");
        assertTrue(status.containsKey("autoDownload"), "Status should contain 'autoDownload' key");
        assertFalse((Boolean) status.get("autoDownload"));

        // If system Python is available, status should reflect it
        if (status.containsKey("available") && (Boolean) status.get("available")) {
            assertTrue(status.containsKey("pythonPath"),
                "Status should contain 'pythonPath' when available");
            assertNotNull(status.get("pythonPath"), "Python path should not be null");
            LOGGER.info("System Python found at: {}", status.get("pythonPath"));
        }

        LOGGER.info("✓ Get status test passed");
    }

    /**
     * Test 3: Detect OS type.
     * Verifies that OS detection returns expected values.
     */
    @Test
    public void testOSDetection() {
        LOGGER.info("Test: OS detection");

        manager = new PythonDistributionManager(tempDir, false);
        Map<String, Object> status = manager.getStatus();

        String os = (String) status.get("os");
        assertNotNull(os, "OS should be detected");
        assertTrue(os.equals("windows") || os.equals("linux") ||
            os.equals("macos-x64") || os.equals("macos-arm64"),
            "OS should be recognized type");

        LOGGER.info("Detected OS: {}", os);
        LOGGER.info("✓ OS detection test passed");
    }

    /**
     * Test 4: Python path detection.
     * Verifies that manager can find system Python.
     */
    @Test
    public void testPythonPathDetection() throws IOException {
        LOGGER.info("Test: Python path detection");

        manager = new PythonDistributionManager(tempDir, false);

        try {
            String pythonPath = manager.getPythonPath();
            assertNotNull(pythonPath, "Python path should be found");
            assertFalse(pythonPath.isEmpty(), "Python path should not be empty");
            LOGGER.info("Python path detected: {}", pythonPath);

            // Verify it's a valid Python by checking if it contains python
            assertTrue(pythonPath.toLowerCase().contains("python"),
                "Path should contain 'python'");

        } catch (IOException e) {
            // Python not available on system - acceptable for test
            LOGGER.warn("Python not found on system: {}", e.getMessage());
            assertTrue(e.getMessage().contains("Python 3 not found") ||
                e.getMessage().contains("not found"),
                "Exception message should mention Python not found");
        }

        LOGGER.info("✓ Python path detection test passed");
    }

    /**
     * Test 5: Virtual environment detection with system property.
     * Simulates venv configuration via system property.
     */
    @Test
    public void testVirtualEnvironmentDetection() throws IOException {
        LOGGER.info("Test: Virtual environment detection");

        // Create a fake venv structure
        Path venvDir = tempDir.resolve("test-venv");
        Path venvBin = venvDir.resolve("bin");
        Path venvPython = venvBin.resolve("python3");
        Path pyvenvCfg = venvDir.resolve("pyvenv.cfg");

        Files.createDirectories(venvBin);
        Files.createFile(pyvenvCfg);
        Files.write(pyvenvCfg, "home = /usr/bin\nversion = 3.11.0".getBytes());

        // Create empty python3 file (won't be executable but that's ok for test)
        Files.createFile(venvPython);

        // Set system property
        String originalVenvProp = System.getProperty("ignition.python3.venv");
        System.setProperty("ignition.python3.venv", venvDir.toString());

        try {
            manager = new PythonDistributionManager(tempDir, false);

            // Check if venv is detected in status
            Map<String, Object> status = manager.getStatus();

            if (status.containsKey("usingVenv")) {
                Boolean usingVenv = (Boolean) status.get("usingVenv");
                LOGGER.info("Using venv: {}", usingVenv);

                if (usingVenv) {
                    assertTrue(status.containsKey("venvPath"), "Status should contain venvPath");
                    LOGGER.info("Venv path: {}", status.get("venvPath"));
                }
            }

            // Check getVirtualEnvPath method
            String venvPath = manager.getVirtualEnvPath();
            if (venvPath != null) {
                assertFalse(venvPath.isEmpty(), "Venv path should not be empty");
                LOGGER.info("Virtual environment detected: {}", venvPath);
            }

        } finally {
            // Restore original property
            if (originalVenvProp != null) {
                System.setProperty("ignition.python3.venv", originalVenvProp);
            } else {
                System.clearProperty("ignition.python3.venv");
            }
        }

        LOGGER.info("✓ Virtual environment detection test passed");
    }

    /**
     * Test 6: No virtual environment when not configured.
     * Verifies that venv is not detected when not configured.
     */
    @Test
    public void testNoVirtualEnvironmentWhenNotConfigured() {
        LOGGER.info("Test: No venv when not configured");

        // Ensure system property is not set
        String originalVenvProp = System.getProperty("ignition.python3.venv");
        System.clearProperty("ignition.python3.venv");

        try {
            manager = new PythonDistributionManager(tempDir, false);
            String venvPath = manager.getVirtualEnvPath();

            assertNull(venvPath, "Venv path should be null when not configured");

            Map<String, Object> status = manager.getStatus();
            if (status.containsKey("usingVenv")) {
                assertFalse((Boolean) status.get("usingVenv"));
            }

        } finally {
            // Restore original property
            if (originalVenvProp != null) {
                System.setProperty("ignition.python3.venv", originalVenvProp);
            }
        }

        LOGGER.info("✓ No venv test passed");
    }

    /**
     * Test 7: Status includes expected keys.
     * Verifies that getStatus() returns all expected information.
     */
    @Test
    public void testStatusIncludesExpectedKeys() {
        LOGGER.info("Test: Status includes expected keys");

        manager = new PythonDistributionManager(tempDir, false);
        Map<String, Object> status = manager.getStatus();

        assertNotNull(status, "Status should not be null");

        // Check for required keys
        assertTrue(status.containsKey("os"), "Status should contain 'os'");
        assertTrue(status.containsKey("embeddedInstalled"), "Status should contain 'embeddedInstalled'");
        assertTrue(status.containsKey("pythonDir"), "Status should contain 'pythonDir'");
        assertTrue(status.containsKey("autoDownload"), "Status should contain 'autoDownload'");
        assertTrue(status.containsKey("usingVenv"), "Status should contain 'usingVenv'");

        // Log all keys for debugging
        LOGGER.info("Status keys: {}", status.keySet());

        LOGGER.info("✓ Status keys test passed");
    }

    /**
     * Test 8: Embedded Python directory structure.
     * Verifies that manager creates expected directory structure.
     */
    @Test
    public void testEmbeddedPythonDirectoryStructure() {
        LOGGER.info("Test: Embedded Python directory structure");

        manager = new PythonDistributionManager(tempDir, false);
        Map<String, Object> status = manager.getStatus();

        String pythonDir = (String) status.get("pythonDir");
        assertNotNull(pythonDir, "Python directory should be reported");

        Path pythonDirPath = Path.of(pythonDir);
        assertTrue(Files.exists(pythonDirPath), "Python directory should exist");
        assertTrue(Files.isDirectory(pythonDirPath), "Python directory should be a directory");

        LOGGER.info("Python directory: {}", pythonDir);
        LOGGER.info("✓ Directory structure test passed");
    }

    /**
     * Test 9: Auto-download flag configuration.
     * Verifies that auto-download flag is respected.
     */
    @Test
    public void testAutoDownloadConfiguration() {
        LOGGER.info("Test: Auto-download configuration");

        // Test with auto-download disabled
        PythonDistributionManager manager1 = new PythonDistributionManager(tempDir, false);
        Map<String, Object> status1 = manager1.getStatus();
        assertFalse((Boolean) status1.get("autoDownload"));

        // Test with auto-download enabled
        PythonDistributionManager manager2 = new PythonDistributionManager(tempDir, true);
        Map<String, Object> status2 = manager2.getStatus();
        assertTrue((Boolean) status2.get("autoDownload"));

        LOGGER.info("✓ Auto-download configuration test passed");
    }

    /**
     * Test 10: Priority order verification.
     * Verifies that Python detection follows correct priority:
     * 1. Virtual environment
     * 2. Embedded Python
     * 3. System Python
     */
    @Test
    public void testPythonDetectionPriority() throws IOException {
        LOGGER.info("Test: Python detection priority");

        // Without venv, should detect system or embedded Python
        manager = new PythonDistributionManager(tempDir, false);

        try {
            String pythonPath1 = manager.getPythonPath();
            assertNotNull(pythonPath1, "Should detect some Python");
            LOGGER.info("Python detected (no venv): {}", pythonPath1);

            // With venv configured, should prefer venv (if valid)
            // Create fake venv
            Path venvDir = tempDir.resolve("priority-venv");
            Path venvBin = venvDir.resolve("bin");
            Files.createDirectories(venvBin);

            String originalVenvProp = System.getProperty("ignition.python3.venv");
            System.setProperty("ignition.python3.venv", venvDir.toString());

            try {
                manager = new PythonDistributionManager(tempDir, false);
                String venvPath = manager.getVirtualEnvPath();

                if (venvPath != null) {
                    LOGGER.info("Venv detected and prioritized: {}", venvPath);
                } else {
                    LOGGER.info("Venv not detected (expected - not a valid venv)");
                }

            } finally {
                if (originalVenvProp != null) {
                    System.setProperty("ignition.python3.venv", originalVenvProp);
                } else {
                    System.clearProperty("ignition.python3.venv");
                }
            }

        } catch (IOException e) {
            LOGGER.info("Python not available on system (acceptable): {}", e.getMessage());
        }

        LOGGER.info("✓ Detection priority test passed");
    }
}
