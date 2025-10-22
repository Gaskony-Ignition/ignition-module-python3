package com.inductiveautomation.ignition.examples.python3.gateway;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for SecurityMode enum.
 * Tests security mode parsing, validation, and behavior.
 */
class SecurityModeTest {

    @Test
    void testSecurityModeValues() {
        // Verify all three modes exist
        assertThat(SecurityMode.values()).hasSize(3);
        assertThat(SecurityMode.values()).containsExactlyInAnyOrder(
            SecurityMode.DESIGNER_ADMIN,
            SecurityMode.ADMIN,
            SecurityMode.RESTRICTED
        );
    }

    @Test
    void testGetValue() {
        assertThat(SecurityMode.DESIGNER_ADMIN.getValue()).isEqualTo("DESIGNER_ADMIN");
        assertThat(SecurityMode.ADMIN.getValue()).isEqualTo("ADMIN");
        assertThat(SecurityMode.RESTRICTED.getValue()).isEqualTo("RESTRICTED");
    }

    @Test
    void testFromString_DesignerAdmin() {
        assertThat(SecurityMode.fromString("DESIGNER_ADMIN")).isEqualTo(SecurityMode.DESIGNER_ADMIN);
        assertThat(SecurityMode.fromString("designer_admin")).isEqualTo(SecurityMode.DESIGNER_ADMIN);
        assertThat(SecurityMode.fromString("Designer_Admin")).isEqualTo(SecurityMode.DESIGNER_ADMIN);
    }

    @Test
    void testFromString_Admin() {
        assertThat(SecurityMode.fromString("ADMIN")).isEqualTo(SecurityMode.ADMIN);
        assertThat(SecurityMode.fromString("admin")).isEqualTo(SecurityMode.ADMIN);
        assertThat(SecurityMode.fromString("Admin")).isEqualTo(SecurityMode.ADMIN);
    }

    @Test
    void testFromString_Restricted() {
        assertThat(SecurityMode.fromString("RESTRICTED")).isEqualTo(SecurityMode.RESTRICTED);
        assertThat(SecurityMode.fromString("restricted")).isEqualTo(SecurityMode.RESTRICTED);
        assertThat(SecurityMode.fromString("Restricted")).isEqualTo(SecurityMode.RESTRICTED);
    }

    @Test
    void testFromString_Invalid() {
        // Invalid values should default to RESTRICTED (safe default)
        assertThat(SecurityMode.fromString("INVALID")).isEqualTo(SecurityMode.RESTRICTED);
        assertThat(SecurityMode.fromString("")).isEqualTo(SecurityMode.RESTRICTED);
        assertThat(SecurityMode.fromString(null)).isEqualTo(SecurityMode.RESTRICTED);
        assertThat(SecurityMode.fromString("UNKNOWN")).isEqualTo(SecurityMode.RESTRICTED);
    }

    @Test
    void testIsAdminMode() {
        assertThat(SecurityMode.DESIGNER_ADMIN.isAdminMode()).isTrue();
        assertThat(SecurityMode.ADMIN.isAdminMode()).isTrue();
        assertThat(SecurityMode.RESTRICTED.isAdminMode()).isFalse();
    }

    @Test
    void testIsDesignerMode() {
        assertThat(SecurityMode.DESIGNER_ADMIN.isDesignerMode()).isTrue();
        assertThat(SecurityMode.ADMIN.isDesignerMode()).isFalse();
        assertThat(SecurityMode.RESTRICTED.isDesignerMode()).isFalse();
    }

    @Test
    void testIsRestricted() {
        assertThat(SecurityMode.DESIGNER_ADMIN.isRestricted()).isFalse();
        assertThat(SecurityMode.ADMIN.isRestricted()).isFalse();
        assertThat(SecurityMode.RESTRICTED.isRestricted()).isTrue();
    }

    @Test
    void testToString() {
        assertThat(SecurityMode.DESIGNER_ADMIN.toString()).isEqualTo("DESIGNER_ADMIN");
        assertThat(SecurityMode.ADMIN.toString()).isEqualTo("ADMIN");
        assertThat(SecurityMode.RESTRICTED.toString()).isEqualTo("RESTRICTED");
    }

    @Test
    void testEnumEquality() {
        SecurityMode mode1 = SecurityMode.fromString("DESIGNER_ADMIN");
        SecurityMode mode2 = SecurityMode.DESIGNER_ADMIN;

        assertThat(mode1).isEqualTo(mode2);
        assertThat(mode1).isSameAs(mode2); // Same instance (enum)
    }

    @Test
    void testSecurityModeInSwitch() {
        // Verify can be used in switch statements
        SecurityMode mode = SecurityMode.DESIGNER_ADMIN;

        String result = switch (mode) {
            case DESIGNER_ADMIN -> "Full access";
            case ADMIN -> "Extended access";
            case RESTRICTED -> "Limited access";
        };

        assertThat(result).isEqualTo("Full access");
    }
}
