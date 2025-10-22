# Ignition Designer UX & Theme Guide

**Based on extensive UX/theme development in Python 3 IDE module**
**Last Updated:** 2025-10-18
**Version:** 2.0 (Updated with Warp-style scrollbars)

---

## Quick Wins for Modern UX

### 1. Remove ALL Borders
```java
JScrollPane scrollPane = new JScrollPane(content);
scrollPane.setBorder(BorderFactory.createEmptyBorder());
scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());

// For RTextScrollPane (RSyntaxTextArea library)
RTextScrollPane textScroll = new RTextScrollPane(editor);
textScroll.setBorder(BorderFactory.createEmptyBorder());
textScroll.setViewportBorder(BorderFactory.createEmptyBorder());
if (textScroll.getGutter() != null) {
    textScroll.getGutter().setBorder(BorderFactory.createEmptyBorder());
}
```

**Why:** Borders create visual "boxes" that break the seamless flow. Empty borders = clean, modern appearance.

### 2. Warp-Style Ultra-Minimal Scrollbars
```java
scrollPane.getVerticalScrollBar().setUI(new WarpScrollBarUI());
scrollPane.getVerticalScrollBar().setOpaque(false);
scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 6));
```

**Features:**
- Invisible track (no background)
- 3px indicator (6px on hover)
- Auto-hides after 1.5 seconds
- Fade animations
- Only visible when scrolling

See full `WarpScrollBarUI` implementation below.

### 3. Dark Theme Backgrounds
```java
scrollPane.setBackground(DARK_BACKGROUND);
scrollPane.getViewport().setBackground(DARK_BACKGROUND);
```

**Critical:** Always set BOTH scroll pane and viewport backgrounds.

---

## Complete WarpScrollBarUI Implementation

```java
package your.package.here;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class WarpScrollBarUI extends BasicScrollBarUI {

    private static final int THUMB_MIN_WIDTH = 3;      // Barely visible
    private static final int THUMB_HOVER_WIDTH = 6;    // On hover
    private static final int THUMB_RADIUS = 2;         // Rounded
    private static final int FADE_DELAY = 1500;        // 1.5s before hide

    private boolean isHovered = false;
    private boolean isVisible = false;
    private int currentAlpha = 0;                      // 0-255
    private Timer fadeTimer;
    private Timer fadeOutTimer;

    @Override
    protected void installListeners() {
        super.installListeners();

        // Show on hover
        scrollbar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                showScrollbar();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                scheduleHide();
            }
        });

        // Show when scrolling
        scrollbar.addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                showScrollbar();
                scheduleHide();
            }
        });
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createInvisibleButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createInvisibleButton();
    }

    private JButton createInvisibleButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        // Invisible track - paint nothing
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled() || currentAlpha == 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);

        int thumbWidth = isHovered ? THUMB_HOVER_WIDTH : THUMB_MIN_WIDTH;
        Color thumbColor = new Color(150, 150, 150, currentAlpha);
        g2.setColor(thumbColor);

        int x, y, width, height;

        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            width = thumbWidth;
            height = thumbBounds.height;
            x = thumbBounds.x + (thumbBounds.width - width) / 2;
            y = thumbBounds.y;
        } else {
            width = thumbBounds.width;
            height = thumbWidth;
            x = thumbBounds.x;
            y = thumbBounds.y + (thumbBounds.height - height) / 2;
        }

        g2.fillRoundRect(x, y, width, height, THUMB_RADIUS, THUMB_RADIUS);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            return new Dimension(THUMB_HOVER_WIDTH, 48);
        } else {
            return new Dimension(48, THUMB_HOVER_WIDTH);
        }
    }

    private void showScrollbar() {
        isVisible = true;

        if (fadeOutTimer != null && fadeOutTimer.isRunning()) {
            fadeOutTimer.stop();
        }

        if (fadeTimer != null && fadeTimer.isRunning()) {
            fadeTimer.stop();
        }

        fadeTimer = new Timer(16, e -> {  // ~60fps
            currentAlpha = Math.min(255, currentAlpha + 30);
            scrollbar.repaint();

            if (currentAlpha >= 255) {
                ((Timer) e.getSource()).stop();
            }
        });
        fadeTimer.start();
    }

    private void scheduleHide() {
        if (fadeOutTimer != null && fadeOutTimer.isRunning()) {
            fadeOutTimer.restart();
            return;
        }

        fadeOutTimer = new Timer(FADE_DELAY, e -> {
            if (!isHovered) {
                hideScrollbar();
            }
        });
        fadeOutTimer.setRepeats(false);
        fadeOutTimer.start();
    }

    private void hideScrollbar() {
        isVisible = false;

        if (fadeTimer != null && fadeTimer.isRunning()) {
            fadeTimer.stop();
        }

        fadeTimer = new Timer(16, e -> {  // ~60fps
            currentAlpha = Math.max(0, currentAlpha - 15);
            scrollbar.repaint();

            if (currentAlpha <= 0) {
                ((Timer) e.getSource()).stop();
            }
        });
        fadeTimer.start();
    }
}
```

---

## Usage Patterns

### Pattern 1: Themed Text Editor
```java
RSyntaxTextArea editor = new RSyntaxTextArea();
editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
editor.setBackground(DARK_BACKGROUND);
editor.setForeground(LIGHT_FOREGROUND);
editor.setCaretColor(LIGHT_FOREGROUND);  // CRITICAL for visibility

RTextScrollPane scrollPane = new RTextScrollPane(editor);
scrollPane.setLineNumbersEnabled(true);

// Remove all borders
scrollPane.setBorder(BorderFactory.createEmptyBorder());
scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
if (scrollPane.getGutter() != null) {
    scrollPane.getGutter().setBorder(BorderFactory.createEmptyBorder());
}

// Apply Warp scrollbars
scrollPane.getVerticalScrollBar().setUI(new WarpScrollBarUI());
scrollPane.getHorizontalScrollBar().setUI(new WarpScrollBarUI());
```

### Pattern 2: Themed Output Area
```java
JTextArea outputArea = new JTextArea();
outputArea.setEditable(false);
outputArea.setBackground(DARK_BACKGROUND);
outputArea.setForeground(LIGHT_FOREGROUND);
outputArea.setCaretColor(LIGHT_FOREGROUND);

JScrollPane scrollPane = new JScrollPane(outputArea);

// Remove borders
scrollPane.setBorder(BorderFactory.createEmptyBorder());
scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());

// Backgrounds
scrollPane.setBackground(DARK_BACKGROUND);
scrollPane.getViewport().setBackground(DARK_BACKGROUND);

// Warp scrollbars
scrollPane.getVerticalScrollBar().setUI(new WarpScrollBarUI());
scrollPane.getVerticalScrollBar().setOpaque(false);
scrollPane.getVerticalScrollBar().setUnitIncrement(16);  // Smooth scroll
```

### Pattern 3: Split Pane Dividers
```java
JSplitPane splitPane = new JSplitPane(
    JSplitPane.HORIZONTAL_SPLIT,
    leftPanel,
    rightPanel
);

splitPane.setBorder(null);
splitPane.setDividerSize(5);

// Dark theme divider
if (splitPane.getUI() instanceof BasicSplitPaneUI) {
    ((BasicSplitPaneUI) splitPane.getUI())
        .getDivider()
        .setBackground(DIVIDER_COLOR);
}
```

---

## Common Mistakes to Avoid

### ❌ DON'T: Forget viewport background
```java
scrollPane.setBackground(DARK_BG);  // Only sets scroll pane, not content!
```

### ✅ DO: Set both scroll pane and viewport
```java
scrollPane.setBackground(DARK_BG);
scrollPane.getViewport().setBackground(DARK_BG);
```

---

### ❌ DON'T: Forget caret color in dark themes
```java
textArea.setBackground(DARK_BG);
textArea.setForeground(LIGHT_FG);
// Caret will be invisible!
```

### ✅ DO: Always set caret color
```java
textArea.setBackground(DARK_BG);
textArea.setForeground(LIGHT_FG);
textArea.setCaretColor(LIGHT_FG);  // Critical!
```

---

### ❌ DON'T: Use visible borders
```java
scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
```

### ✅ DO: Use empty borders
```java
scrollPane.setBorder(BorderFactory.createEmptyBorder());
```

---

### ❌ DON'T: Forget gutter borders (RTextScrollPane)
```java
RTextScrollPane scroll = new RTextScrollPane(editor);
scroll.setBorder(BorderFactory.createEmptyBorder());
// Gutter still has border!
```

### ✅ DO: Remove gutter border too
```java
scroll.setBorder(BorderFactory.createEmptyBorder());
scroll.setViewportBorder(BorderFactory.createEmptyBorder());
if (scroll.getGutter() != null) {
    scroll.getGutter().setBorder(BorderFactory.createEmptyBorder());
}
```

---

## Color Palette Reference

### Dark Theme
```java
public static final Color BACKGROUND_DARKER = new Color(23, 23, 23);    // Deepest
public static final Color BACKGROUND_DARK = new Color(30, 30, 30);      // Main background
public static final Color BACKGROUND_LIGHT = new Color(37, 37, 38);     // Elevated surfaces

public static final Color FOREGROUND_PRIMARY = new Color(204, 204, 204);  // Main text
public static final Color FOREGROUND_SECONDARY = new Color(150, 150, 150); // Subdued text

public static final Color BORDER_DEFAULT = new Color(64, 64, 64);       // Subtle borders
public static final Color DIVIDER_COLOR = new Color(45, 45, 45);        // Split panes

public static final Color ACCENT_PRIMARY = new Color(14, 99, 156);      // Blue accent
public static final Color SUCCESS = new Color(76, 175, 80);             // Green
public static final Color ERROR = new Color(244, 67, 54);               // Red
public static final Color WARNING = new Color(255, 167, 38);            // Orange
```

---

## Testing Checklist

Before releasing UI changes, verify:

- [ ] All scroll panes have no visible borders
- [ ] Scrollbars are minimal and auto-hide
- [ ] Text caret is visible in all themes
- [ ] Viewport backgrounds match content
- [ ] Split pane dividers are themed
- [ ] No white/gray "boxes" visible
- [ ] Hover effects work on scrollbars
- [ ] Theme switching updates everything
- [ ] Gutter borders removed (if using RTextScrollPane)
- [ ] Content has seamless appearance

---

## Performance Notes

**Warp Scrollbar Animations:**
- Timer runs at 16ms intervals (~60fps)
- Fade-in: Alpha increases by 30 per frame (fast)
- Fade-out: Alpha decreases by 15 per frame (slower, smoother)
- Timers stop when animation completes (no CPU waste)

**Memory:**
- Each scrollbar creates 2 timers (fade + fadeOut)
- Timers are stopped when not animating
- No memory leaks (timers are GC'd with scrollbar)

---

## Additional Resources

- [RSyntaxTextArea Library](https://github.com/bobbylight/RSyntaxTextArea)
- [Warp Terminal](https://www.warp.dev/) (inspiration for scrollbars)
- [VS Code Themes](https://code.visualstudio.com/docs/getstarted/themes) (color palettes)

---

## Version History

- **v2.0** (2025-10-18): Added Warp-style scrollbars, borderless patterns
- **v1.0** (2025-10-18): Initial guide with theme basics

---

**License:** Use freely in your Ignition Designer projects
**Maintained by:** Project contributors
