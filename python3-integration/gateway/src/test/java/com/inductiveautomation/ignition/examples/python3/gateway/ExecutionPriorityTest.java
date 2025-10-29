package com.inductiveautomation.ignition.examples.python3.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExecutionPriority.
 *
 * @since v2.16.0 (Phase 3 Week 3-4)
 */
public class ExecutionPriorityTest {

    @Test
    public void testPriorityValues() {
        assertEquals(1, ExecutionPriority.HIGH.getValue());
        assertEquals(5, ExecutionPriority.NORMAL.getValue());
        assertEquals(10, ExecutionPriority.LOW.getValue());

        // Verify ordering (lower value = higher priority)
        assertTrue(ExecutionPriority.HIGH.getValue() < ExecutionPriority.NORMAL.getValue());
        assertTrue(ExecutionPriority.NORMAL.getValue() < ExecutionPriority.LOW.getValue());
    }

    @Test
    public void testFromString_Valid() {
        assertEquals(ExecutionPriority.HIGH, ExecutionPriority.fromString("high"));
        assertEquals(ExecutionPriority.HIGH, ExecutionPriority.fromString("HIGH"));
        assertEquals(ExecutionPriority.HIGH, ExecutionPriority.fromString("High"));

        assertEquals(ExecutionPriority.NORMAL, ExecutionPriority.fromString("normal"));
        assertEquals(ExecutionPriority.NORMAL, ExecutionPriority.fromString("NORMAL"));

        assertEquals(ExecutionPriority.LOW, ExecutionPriority.fromString("low"));
        assertEquals(ExecutionPriority.LOW, ExecutionPriority.fromString("LOW"));
    }

    @Test
    public void testFromString_Invalid() {
        // Invalid strings default to NORMAL
        assertEquals(ExecutionPriority.NORMAL, ExecutionPriority.fromString("invalid"));
        assertEquals(ExecutionPriority.NORMAL, ExecutionPriority.fromString(""));
        assertEquals(ExecutionPriority.NORMAL, ExecutionPriority.fromString(null));
        assertEquals(ExecutionPriority.NORMAL, ExecutionPriority.fromString("medium"));
    }

    @Test
    public void testFromSecurityMode_Admin() {
        assertEquals(ExecutionPriority.HIGH, ExecutionPriority.fromSecurityMode(SecurityMode.ADMIN));
        assertEquals(ExecutionPriority.HIGH, ExecutionPriority.fromSecurityMode(SecurityMode.DESIGNER_ADMIN));
    }

    @Test
    public void testFromSecurityMode_NonAdmin() {
        assertEquals(ExecutionPriority.NORMAL, ExecutionPriority.fromSecurityMode(SecurityMode.RESTRICTED));
    }

    @Test
    public void testFromSecurityMode_Null() {
        assertEquals(ExecutionPriority.NORMAL, ExecutionPriority.fromSecurityMode(null));
    }

    @Test
    public void testEnumValues() {
        ExecutionPriority[] values = ExecutionPriority.values();
        assertEquals(3, values.length);
        assertEquals(ExecutionPriority.HIGH, values[0]);
        assertEquals(ExecutionPriority.NORMAL, values[1]);
        assertEquals(ExecutionPriority.LOW, values[2]);
    }

    @Test
    public void testEnumValueOf() {
        assertEquals(ExecutionPriority.HIGH, ExecutionPriority.valueOf("HIGH"));
        assertEquals(ExecutionPriority.NORMAL, ExecutionPriority.valueOf("NORMAL"));
        assertEquals(ExecutionPriority.LOW, ExecutionPriority.valueOf("LOW"));

        assertThrows(IllegalArgumentException.class, () -> ExecutionPriority.valueOf("INVALID"));
    }
}
