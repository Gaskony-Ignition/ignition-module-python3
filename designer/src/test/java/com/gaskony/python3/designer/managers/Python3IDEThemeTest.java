package com.gaskony.python3.designer.managers;

import com.gaskony.python3.designer.ModernTheme;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.TitledBorder;
import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for the static helpers extracted from {@code Python3IDE} into
 * {@link Python3IDETheme}. The dynamic, RSyntaxTextArea-driven
 * {@code applyTheme(name)} flow is not unit-tested here because it requires a
 * fully constructed theme cascade and is exercised by the integration / Designer
 * runtime.
 */
class Python3IDEThemeTest {

    @Test
    void mapThemeNameToKey_handlesAllDisplayNames() {
        assertEquals("dark", Python3IDETheme.mapThemeNameToKey("Dark"));
        assertEquals("vs", Python3IDETheme.mapThemeNameToKey("VS Code Dark+"));
        assertEquals("monokai", Python3IDETheme.mapThemeNameToKey("Monokai"));
        assertEquals("druid", Python3IDETheme.mapThemeNameToKey("Dracula"));
        assertEquals("default", Python3IDETheme.mapThemeNameToKey("Default (Light)"));
        assertEquals("idea", Python3IDETheme.mapThemeNameToKey("IntelliJ Light"));
        assertEquals("eclipse", Python3IDETheme.mapThemeNameToKey("Eclipse"));
    }

    @Test
    void mapThemeNameToKey_unknownDefaultsToDark() {
        assertEquals("dark", Python3IDETheme.mapThemeNameToKey("nonsense"));
        assertEquals("dark", Python3IDETheme.mapThemeNameToKey(""));
    }

    @Test
    void createThemedDialogButton_darkUsesDarkPalette() {
        JButton btn = Python3IDETheme.createThemedDialogButton("OK", true);
        assertNotNull(btn);
        assertEquals("OK", btn.getText());
        assertSame(ModernTheme.BUTTON_BACKGROUND, btn.getBackground());
        assertSame(ModernTheme.FOREGROUND_PRIMARY, btn.getForeground());
    }

    @Test
    void createThemedDialogButton_lightUsesLightPalette() {
        JButton btn = Python3IDETheme.createThemedDialogButton("Cancel", false);
        assertSame(ModernTheme.LIGHT_BACKGROUND_LIGHT, btn.getBackground());
        assertEquals(Color.BLACK, btn.getForeground());
    }

    @Test
    void updateTitledBorders_recursesAndRecolours() {
        JPanel root = new JPanel();
        TitledBorder rootBorder = new TitledBorder("outer");
        root.setBorder(rootBorder);

        JPanel inner = new JPanel();
        TitledBorder innerBorder = new TitledBorder("inner");
        inner.setBorder(innerBorder);
        root.add(inner);

        Python3IDETheme.updateTitledBorders(root, true);

        assertEquals(ModernTheme.FOREGROUND_PRIMARY, rootBorder.getTitleColor());
        assertEquals(ModernTheme.FOREGROUND_PRIMARY, innerBorder.getTitleColor());

        Python3IDETheme.updateTitledBorders(root, false);
        assertEquals(Color.BLACK, rootBorder.getTitleColor());
        assertEquals(Color.BLACK, innerBorder.getTitleColor());
    }

    @Test
    void setComponentsDark_recursesAndStyles() {
        // Note: this test pins down the existing (pre-extraction) behaviour of
        // setComponentsDark: after the JButton branch sets BUTTON_BACKGROUND,
        // the recursive container-traversal then overrides that background to
        // PANEL_BACKGROUND because every JButton is also a Container. The
        // foreground colour survives because nothing rewrites it.
        JPanel root = new JPanel();
        JLabel label = new JLabel("hello");
        JButton button = new JButton("click");
        root.add(label);
        root.add(button);

        Python3IDETheme.setComponentsDark(root);

        assertSame(ModernTheme.PANEL_BACKGROUND, root.getBackground());
        assertSame(ModernTheme.FOREGROUND_PRIMARY, label.getForeground());
        // Button background is finally PANEL_BACKGROUND (recursion override),
        // matching v3.12.x behaviour.
        assertSame(ModernTheme.PANEL_BACKGROUND, button.getBackground());
        assertSame(ModernTheme.FOREGROUND_PRIMARY, button.getForeground());
    }

    @Test
    void stylePopupMenu_darkAppliesPalette() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem item = new JMenuItem("Open");
        menu.add(item);

        Python3IDETheme controller = new Python3IDETheme(null, "dark");
        controller.stylePopupMenu(menu, true);

        assertSame(ModernTheme.BACKGROUND_DARK, menu.getBackground());
        assertSame(ModernTheme.FOREGROUND_PRIMARY, menu.getForeground());
        assertSame(ModernTheme.BACKGROUND_DARK, item.getBackground());
        assertSame(ModernTheme.FOREGROUND_PRIMARY, item.getForeground());
        assertEquals(ModernTheme.BUTTON_HOVER,
            item.getClientProperty("MenuItem.selectionBackground"));
        assertEquals(Color.WHITE,
            item.getClientProperty("MenuItem.selectionForeground"));
    }

    @Test
    void stylePopupMenu_lightAppliesPalette() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem item = new JMenuItem("Open");
        menu.add(item);

        Python3IDETheme controller = new Python3IDETheme(null, "default");
        controller.stylePopupMenu(menu, false);

        assertSame(ModernTheme.LIGHT_BACKGROUND, menu.getBackground());
        assertEquals(Color.BLACK, menu.getForeground());
        assertSame(ModernTheme.LIGHT_BACKGROUND, item.getBackground());
        assertEquals(Color.BLACK, item.getForeground());
    }

    @Test
    void getCurrentTheme_returnsConstructorValue() {
        Python3IDETheme controller = new Python3IDETheme(null, "monokai");
        assertEquals("monokai", controller.getCurrentTheme());
    }
}
