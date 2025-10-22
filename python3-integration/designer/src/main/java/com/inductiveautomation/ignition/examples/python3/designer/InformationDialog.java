package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Information dialog for Python 3 IDE showing comprehensive user guide.
 *
 * Displays:
 * - Keyboard shortcuts
 * - Workflows (IDE to production scripts)
 * - Feature highlights
 * - Tips and best practices
 *
 * v2.5.1: Created to provide in-app help for new users
 */
public class InformationDialog {

    // Theme colors - updated dynamically based on IDE theme
    private static boolean useDarkTheme = true;

    // Dark theme colors
    private static final Color DARK_BACKGROUND = new Color(43, 43, 43);
    private static final Color DARK_BACKGROUND_DARKER = new Color(30, 30, 30);
    private static final Color DARK_FOREGROUND = new Color(224, 224, 224);
    private static final Color DARK_HEADING = new Color(100, 200, 255);  // Light blue for headings
    private static final Color DARK_ACCENT = new Color(180, 180, 180);   // Gray for accents
    private static final Color DARK_BUTTON_BG = new Color(60, 63, 65);
    private static final Color DARK_BORDER = new Color(60, 63, 65);

    // Light theme colors
    private static final Color LIGHT_BACKGROUND = Color.WHITE;
    private static final Color LIGHT_BACKGROUND_DARKER = new Color(245, 245, 245);
    private static final Color LIGHT_FOREGROUND = Color.BLACK;
    private static final Color LIGHT_HEADING = new Color(0, 80, 200);    // Dark blue for headings
    private static final Color LIGHT_ACCENT = new Color(100, 100, 100);  // Dark gray for accents
    private static final Color LIGHT_BUTTON_BG = new Color(238, 238, 238);
    private static final Color LIGHT_BORDER = new Color(200, 200, 200);

    /**
     * Sets the theme for all future dialogs.
     *
     * @param darkTheme true for dark theme, false for light theme
     */
    public static void setDarkTheme(boolean darkTheme) {
        useDarkTheme = darkTheme;
    }

    // Get current theme colors
    private static Color getBackground() {
        return useDarkTheme ? DARK_BACKGROUND : LIGHT_BACKGROUND;
    }

    private static Color getBackgroundDarker() {
        return useDarkTheme ? DARK_BACKGROUND_DARKER : LIGHT_BACKGROUND_DARKER;
    }

    private static Color getForeground() {
        return useDarkTheme ? DARK_FOREGROUND : LIGHT_FOREGROUND;
    }

    private static Color getHeadingColor() {
        return useDarkTheme ? DARK_HEADING : LIGHT_HEADING;
    }

    private static Color getAccentColor() {
        return useDarkTheme ? DARK_ACCENT : LIGHT_ACCENT;
    }

    private static Color getButtonBg() {
        return useDarkTheme ? DARK_BUTTON_BG : LIGHT_BUTTON_BG;
    }

    private static Color getBorderColor() {
        return useDarkTheme ? DARK_BORDER : LIGHT_BORDER;
    }

    /**
     * Shows the information dialog with comprehensive user guide.
     *
     * @param parent parent component
     */
    public static void show(Component parent) {
        JDialog dialog = createBaseDialog(parent, "Python 3 IDE - User Guide");

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(getBackground());
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Create scrollable content area
        JPanel infoPanel = createInfoPanel();
        JScrollPane scrollPane = new JScrollPane(infoPanel);
        scrollPane.setBackground(getBackground());
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Close button
        JButton closeButton = createThemedButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(getBackground());
        buttonPanel.add(closeButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.setSize(700, 600);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static JPanel createInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(getBackground());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Title
        addHeading(panel, "Welcome to the Python 3 IDE for Ignition!", 18, true);
        addText(panel,
            "This IDE enables you to develop, test, and debug Python 3 code before deploying to production Ignition scripts. " +
            "All Python code executes on the Gateway using a dedicated process pool.");

        addSpacer(panel, 15);

        // Quick Start
        addHeading(panel, "Quick Start", 16, false);
        addText(panel, "1. Connect to Gateway using the URL field (e.g., http://localhost:8088)");
        addText(panel, "2. Write Python 3 code in the editor");
        addText(panel, "3. Click Execute (or press Ctrl+Enter) to run on Gateway");
        addText(panel, "4. View results in Output panel, errors in Error panel");
        addText(panel, "5. Save scripts with Save button (Ctrl+S) for later use");

        addSpacer(panel, 15);

        // Keyboard Shortcuts
        addHeading(panel, "Keyboard Shortcuts", 16, false);
        addShortcut(panel, "Ctrl+Enter", "Execute code on Gateway");
        addShortcut(panel, "Ctrl+S", "Save current script");
        addShortcut(panel, "Ctrl+Shift+S", "Save As (with metadata)");
        addShortcut(panel, "Ctrl+N", "New script (clear editor)");
        addShortcut(panel, "Ctrl+F", "Find in editor");
        addShortcut(panel, "Ctrl+H", "Find and Replace");
        addShortcut(panel, "Ctrl++", "Increase font size");
        addShortcut(panel, "Ctrl+-", "Decrease font size");
        addShortcut(panel, "Ctrl+0", "Reset font size to default");

        addSpacer(panel, 15);

        // Execution Modes
        addHeading(panel, "Execution Modes (v2.5.0)", 16, false);
        addText(panel, "• Python Code Mode - Execute Python 3 scripts (default)");
        addText(panel, "• Shell Command Mode - Run shell commands directly on Gateway");
        addText(panel, "  Examples: pip install pandas, df -h, python3 --version");
        addText(panel, "  Use this for package management and system diagnostics");

        addSpacer(panel, 15);

        // Workflow: IDE to Production
        addHeading(panel, "Workflow: From IDE to Production Scripts", 16, false);
        addText(panel,
            "The Python 3 IDE is a development tool. Here's how to use tested code in production:");

        addSpacer(panel, 5);
        addText(panel, "Step 1: Develop and test in IDE");
        addAccent(panel, "  Write your Python code in the IDE editor and execute multiple times to verify logic");

        addSpacer(panel, 5);
        addText(panel, "Step 2: Copy code to production script");
        addAccent(panel, "  Open Script Console (Designer) or create Gateway Event Script");
        addAccent(panel, "  Wrap your Python code in system.python3.exec() or system.python3.eval()");

        addSpacer(panel, 5);
        addText(panel, "Step 3: Example production usage");
        addCodeBlock(panel,
            "# Gateway Scheduled Script\n" +
            "def execute():\n" +
            "    code = \"\"\"\n" +
            "import pandas as pd\n" +
            "# Your tested Python code here\n" +
            "result = process_data()\n" +
            "    \"\"\"\n" +
            "    result = system.python3.exec(code)\n" +
            "    # Use result in Ignition");

        addSpacer(panel, 15);

        // Script Management
        addHeading(panel, "Script Management", 16, false);
        addText(panel, "• Scripts are saved locally: ~/.python3ide/scripts/ (NOT on Gateway)");
        addText(panel, "• Right-click scripts for Load, Export, Rename, Delete, Move operations");
        addText(panel, "• Right-click folders to create subfolders or new scripts");
        addText(panel, "• Drag-and-drop scripts and folders to reorganize");
        addText(panel, "• Use Save As for full metadata (description, author, version)");
        addText(panel, "• Export to .py files for version control or sharing");

        addSpacer(panel, 15);

        // Features
        addHeading(panel, "Key Features", 16, false);
        addText(panel, "✓ Syntax highlighting for Python code");
        addText(panel, "✓ Real-time syntax checking (red/yellow squiggles)");
        addText(panel, "✓ Jedi-powered autocomplete (Ctrl+Space)");
        addText(panel, "✓ Execute code asynchronously (non-blocking UI)");
        addText(panel, "✓ Separate Output and Error panels with color coding");
        addText(panel, "✓ Execution timing and diagnostics");
        addText(panel, "✓ Process pool statistics (healthy processes, available, in use)");
        addText(panel, "✓ Theme support (Dark, Light, VS Code Dark+)");
        addText(panel, "✓ Find/Replace toolbar");
        addText(panel, "✓ Font size controls (A+/A- buttons)");
        addText(panel, "✓ Script organization with folders");

        addSpacer(panel, 15);

        // Best Practices
        addHeading(panel, "Best Practices", 16, false);
        addText(panel, "• Always test code in IDE before deploying to production");
        addText(panel, "• Use Shell Command Mode to install packages (pip install pandas)");
        addText(panel, "• Organize scripts into folders by category (Database, API, Reports)");
        addText(panel, "• Fill in script metadata (description, author, version)");
        addText(panel, "• Use descriptive script names (CalculateTax not script1)");
        addText(panel, "• Return results as JSON for complex data structures");
        addText(panel, "• Check Error panel for detailed tracebacks when debugging");
        addText(panel, "• Monitor diagnostics panel for process pool health");

        addSpacer(panel, 15);

        // Common Use Cases
        addHeading(panel, "Common Use Cases", 16, false);
        addText(panel, "• Data processing with Pandas (dataframes, aggregations, transformations)");
        addText(panel, "• API integration with Requests (REST APIs, JSON parsing)");
        addText(panel, "• Machine learning predictions (NumPy, scikit-learn, TensorFlow)");
        addText(panel, "• Advanced calculations beyond Jython capabilities");
        addText(panel, "• File operations (CSV, JSON, XML parsing)");
        addText(panel, "• Web scraping and automation");

        addSpacer(panel, 15);

        // Troubleshooting
        addHeading(panel, "Troubleshooting", 16, false);
        addText(panel, "Problem: Can't connect to Gateway");
        addAccent(panel, "  → Check Gateway URL, ensure Gateway is running, verify network connectivity");

        addSpacer(panel, 5);
        addText(panel, "Problem: Code works in IDE but fails in production script");
        addAccent(panel, "  → Ensure packages are installed on Gateway, verify variable passing");

        addSpacer(panel, 5);
        addText(panel, "Problem: Autocomplete not working");
        addAccent(panel, "  → Check status bar for 'AC: Ready', Jedi auto-installs at Gateway startup");

        addSpacer(panel, 5);
        addText(panel, "Problem: Script tree is empty");
        addAccent(panel, "  → Scripts are saved per-workstation locally, copy from backup if needed");

        addSpacer(panel, 15);

        // Footer
        addHeading(panel, "Need More Help?", 16, false);
        addText(panel, "• Full documentation: python3-integration/README.md");
        addText(panel, "• Architecture guide: python3-integration/docs/V2_ARCHITECTURE_GUIDE.md");
        addText(panel, "• Subprocess guide: PYTHON_SUBPROCESS_AND_PIP_GUIDE.md");
        addText(panel, "• GitHub: https://github.com/nigelgwork/ignition-module-python3");

        addSpacer(panel, 10);
        addAccent(panel, "Python 3 Integration Module v2.5.1");
        addAccent(panel, "Built with Ignition SDK 8.3 | Developed by Gaskony with Claude Code");

        return panel;
    }

    // Helper methods for consistent formatting

    private static void addHeading(JPanel panel, String text, int fontSize, boolean first) {
        JLabel label = new JLabel(text);
        label.setForeground(getHeadingColor());
        label.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (!first) {
            label.setBorder(new EmptyBorder(0, 0, 8, 0));
        }
        panel.add(label);
    }

    private static void addText(JPanel panel, String text) {
        JLabel label = new JLabel("<html><body style='width: 600px'>" + text + "</body></html>");
        label.setForeground(getForeground());
        label.setFont(ModernTheme.FONT_REGULAR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(3, 0, 3, 0));
        panel.add(label);
    }

    private static void addAccent(JPanel panel, String text) {
        JLabel label = new JLabel("<html><body style='width: 600px'>" + text + "</body></html>");
        label.setForeground(getAccentColor());
        label.setFont(ModernTheme.FONT_REGULAR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(2, 0, 2, 0));
        panel.add(label);
    }

    private static void addShortcut(JPanel panel, String key, String description) {
        JPanel shortcutPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        shortcutPanel.setBackground(getBackground());
        shortcutPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        shortcutPanel.setMaximumSize(new Dimension(650, 25));

        // Key label (monospace, accent color)
        JLabel keyLabel = new JLabel(String.format("%-20s", key));
        keyLabel.setForeground(getAccentColor());
        keyLabel.setFont(new Font("Consolas", Font.BOLD, 13));
        shortcutPanel.add(keyLabel);

        // Description label
        JLabel descLabel = new JLabel(description);
        descLabel.setForeground(getForeground());
        descLabel.setFont(ModernTheme.FONT_REGULAR);
        shortcutPanel.add(descLabel);

        panel.add(shortcutPanel);
    }

    private static void addCodeBlock(JPanel panel, String code) {
        JTextArea codeArea = new JTextArea(code);
        codeArea.setBackground(getBackgroundDarker());
        codeArea.setForeground(getForeground());
        codeArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        codeArea.setEditable(false);
        codeArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        codeArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setBorder(BorderFactory.createLineBorder(getBorderColor(), 1));
        codeScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        codeScroll.setMaximumSize(new Dimension(650, 150));
        codeScroll.setPreferredSize(new Dimension(650, 150));

        panel.add(codeScroll);
    }

    private static void addSpacer(JPanel panel, int height) {
        panel.add(Box.createRigidArea(new Dimension(0, height)));
    }

    private static JDialog createBaseDialog(Component parent, String title) {
        Window owner = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(getBackground());
        dialog.setIconImage(DarkDialog.createPython3Icon());  // v2.5.4: Custom icon
        return dialog;
    }

    private static JButton createThemedButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(getButtonBg());
        button.setForeground(getForeground());
        button.setFont(ModernTheme.FONT_REGULAR);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getBorderColor(), 1),
            new EmptyBorder(5, 15, 5, 15)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover effect (theme-aware)
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (useDarkTheme) {
                    button.setBackground(new Color(75, 80, 85));
                } else {
                    button.setBackground(new Color(220, 220, 220));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(getButtonBg());
            }
        });

        return button;
    }
}
