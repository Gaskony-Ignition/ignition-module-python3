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
    void testAllManagersHavePublicConstructors() {
        // Verify that all manager classes have at least one public constructor
        String[] managerClasses = {
            "AutoSaveManager",
            "SearchManager",
            "ScriptImportExportManager",
            "ExecutionManager",
            "KeyboardShortcutsManager",
            "ScriptTransferManager",
            "CommandPaletteManager"
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
            "CommandPaletteManager"
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
