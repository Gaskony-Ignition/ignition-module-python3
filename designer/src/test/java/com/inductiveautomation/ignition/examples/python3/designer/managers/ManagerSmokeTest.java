package com.inductiveautomation.ignition.examples.python3.designer.managers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for all Manager classes.
 * Verifies that all manager classes exist, are compiled correctly, and can be loaded.
 * This is a basic sanity check that the manager classes are present and valid.
 *
 * @since v2.11.0
 */
class ManagerSmokeTest {

    @Test
    void testAutoSaveManagerClassExists() {
        // Verify that the AutoSaveManager class exists and can be loaded
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName(
                "com.inductiveautomation.ignition.examples.python3.designer.managers.AutoSaveManager"
            );
            assertNotNull(clazz, "AutoSaveManager class should exist");
            assertTrue(clazz.getName().contains("AutoSaveManager"),
                "Class name should contain 'AutoSaveManager'");
        });
    }

    @Test
    void testSearchManagerClassExists() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName(
                "com.inductiveautomation.ignition.examples.python3.designer.managers.SearchManager"
            );
            assertNotNull(clazz, "SearchManager class should exist");
        });
    }

    @Test
    void testScriptImportExportManagerClassExists() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName(
                "com.inductiveautomation.ignition.examples.python3.designer.managers.ScriptImportExportManager"
            );
            assertNotNull(clazz, "ScriptImportExportManager class should exist");
        });
    }

    @Test
    void testExecutionManagerClassExists() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName(
                "com.inductiveautomation.ignition.examples.python3.designer.managers.ExecutionManager"
            );
            assertNotNull(clazz, "ExecutionManager class should exist");
        });
    }

    @Test
    void testKeyboardShortcutsManagerClassExists() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName(
                "com.inductiveautomation.ignition.examples.python3.designer.managers.KeyboardShortcutsManager"
            );
            assertNotNull(clazz, "KeyboardShortcutsManager class should exist");
        });
    }

    @Test
    void testScriptTransferManagerClassExists() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName(
                "com.inductiveautomation.ignition.examples.python3.designer.managers.ScriptTransferManager"
            );
            assertNotNull(clazz, "ScriptTransferManager class should exist");
        });
    }

    @Test
    void testCommandPaletteManagerClassExists() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName(
                "com.inductiveautomation.ignition.examples.python3.designer.managers.CommandPaletteManager"
            );
            assertNotNull(clazz, "CommandPaletteManager class should exist");
        });
    }

    @Test
    void testPython3IDEThemeClassExists() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName(
                "com.inductiveautomation.ignition.examples.python3.designer.managers.Python3IDETheme"
            );
            assertNotNull(clazz, "Python3IDETheme class should exist");
        });
    }

    @Test
    void testPython3IDEConnectionControllerClassExists() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName(
                "com.inductiveautomation.ignition.examples.python3.designer.managers.Python3IDEConnectionController"
            );
            assertNotNull(clazz, "Python3IDEConnectionController class should exist");
        });
    }

    @Test
    void testPython3IDETerminalControllerClassExists() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName(
                "com.inductiveautomation.ignition.examples.python3.designer.managers.Python3IDETerminalController"
            );
            assertNotNull(clazz, "Python3IDETerminalController class should exist");
        });
    }

    @Test
    void testPython3IDEScriptOpsClassExists() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName(
                "com.inductiveautomation.ignition.examples.python3.designer.managers.Python3IDEScriptOps"
            );
            assertNotNull(clazz, "Python3IDEScriptOps class should exist");
        });
    }

    @Test
    void testPython3IDELayoutClassExists() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName(
                "com.inductiveautomation.ignition.examples.python3.designer.managers.Python3IDELayout"
            );
            assertNotNull(clazz, "Python3IDELayout class should exist");
        });
    }

    @Test
    void testAllManagersHavePublicConstructors() {
        // Verify that all manager classes have at least one public constructor
        String[] managerClasses = {
            "AutoSaveManager",
            "SearchManager",
            "ScriptImportExportManager",
            "ExecutionManager",
            "KeyboardShortcutsManager",
            "ScriptTransferManager",
            "CommandPaletteManager",
            "Python3IDETheme",
            "Python3IDEConnectionController",
            "Python3IDETerminalController",
            "Python3IDEScriptOps"
        };

        for (String managerName : managerClasses) {
            assertDoesNotThrow(() -> {
                Class<?> clazz = Class.forName(
                    "com.inductiveautomation.ignition.examples.python3.designer.managers." + managerName
                );
                assertTrue(clazz.getConstructors().length > 0,
                    managerName + " should have at least one public constructor");
            }, managerName + " should be loadable");
        }
    }

    @Test
    void testAllManagersAreInCorrectPackage() {
        String[] managerClasses = {
            "AutoSaveManager",
            "SearchManager",
            "ScriptImportExportManager",
            "ExecutionManager",
            "KeyboardShortcutsManager",
            "ScriptTransferManager",
            "CommandPaletteManager",
            "Python3IDETheme",
            "Python3IDEConnectionController",
            "Python3IDETerminalController",
            "Python3IDEScriptOps",
            "Python3IDELayout"
        };

        String expectedPackage = "com.inductiveautomation.ignition.examples.python3.designer.managers";

        for (String managerName : managerClasses) {
            assertDoesNotThrow(() -> {
                Class<?> clazz = Class.forName(expectedPackage + "." + managerName);
                assertEquals(expectedPackage, clazz.getPackage().getName(),
                    managerName + " should be in correct package");
            });
        }
    }
}
