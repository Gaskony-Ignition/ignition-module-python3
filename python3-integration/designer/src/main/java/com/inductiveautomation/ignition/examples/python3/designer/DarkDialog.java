package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom theme-aware dialog builder for Python 3 IDE.
 *
 * Provides properly themed alternatives to JOptionPane that actually work.
 * UIManager-based theming doesn't work reliably because Ignition overrides it.
 *
 * v2.0.11: Created to fix persistent light-themed dialog issues
 * v2.0.12: Made theme-aware to respond to IDE theme changes
 */
public class DarkDialog {

    // Theme colors - updated dynamically based on IDE theme
    private static boolean useDarkTheme = true;

    // Dark theme colors
    private static final Color DARK_BACKGROUND = new Color(43, 43, 43);
    private static final Color DARK_BACKGROUND_DARKER = new Color(30, 30, 30);
    private static final Color DARK_FOREGROUND = new Color(224, 224, 224);
    private static final Color DARK_BUTTON_BG = new Color(60, 63, 65);
    private static final Color DARK_BORDER = new Color(60, 63, 65);

    // Light theme colors
    private static final Color LIGHT_BACKGROUND = Color.WHITE;
    private static final Color LIGHT_BACKGROUND_DARKER = new Color(245, 245, 245);
    private static final Color LIGHT_FOREGROUND = Color.BLACK;
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

    private static Color getButtonBg() {
        return useDarkTheme ? DARK_BUTTON_BG : LIGHT_BUTTON_BG;
    }

    private static Color getBorderColor() {
        return useDarkTheme ? DARK_BORDER : LIGHT_BORDER;
    }

    /**
     * Shows a themed message dialog.
     *
     * @param parent parent component
     * @param message message to display
     * @param title dialog title
     */
    public static void showMessage(Component parent, String message, String title) {
        showMessage(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Shows a themed message dialog with specific message type.
     *
     * @param parent parent component
     * @param message message to display
     * @param title dialog title
     * @param messageType JOptionPane message type (INFO, WARNING, ERROR)
     */
    public static void showMessage(Component parent, String message, String title, int messageType) {
        JDialog dialog = createBaseDialog(parent, title);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(getBackground());
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Message label
        JLabel messageLabel = new JLabel("<html><body style='width: 300px'>" + message + "</body></html>");
        messageLabel.setForeground(getForeground());
        messageLabel.setFont(ModernTheme.FONT_REGULAR);
        contentPanel.add(messageLabel, BorderLayout.CENTER);

        // OK button
        JButton okButton = createThemedButton("OK");
        okButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(getBackground());
        buttonPanel.add(okButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    /**
     * Shows a themed confirmation dialog (Yes/No).
     *
     * @param parent parent component
     * @param message message to display
     * @param title dialog title
     * @return true if Yes was clicked, false otherwise
     */
    public static boolean showConfirm(Component parent, String message, String title) {
        JDialog dialog = createBaseDialog(parent, title);
        final boolean[] result = {false};

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(getBackground());
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Message label
        JLabel messageLabel = new JLabel("<html><body style='width: 300px'>" + message + "</body></html>");
        messageLabel.setForeground(getForeground());
        messageLabel.setFont(ModernTheme.FONT_REGULAR);
        contentPanel.add(messageLabel, BorderLayout.CENTER);

        // Buttons
        JButton yesButton = createThemedButton("Yes");
        yesButton.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });

        JButton noButton = createThemedButton("No");
        noButton.addActionListener(e -> {
            result[0] = false;
            dialog.dispose();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(getBackground());
        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setModal(true);
        dialog.setVisible(true);

        return result[0];
    }

    /**
     * Shows a themed input dialog.
     *
     * @param parent parent component
     * @param message message to display
     * @param title dialog title
     * @param initialValue initial text value
     * @return user input, or null if cancelled
     */
    public static String showInput(Component parent, String message, String title, String initialValue) {
        JDialog dialog = createBaseDialog(parent, title);
        final String[] result = {null};

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(getBackground());
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Message label
        JLabel messageLabel = new JLabel(message);
        messageLabel.setForeground(getForeground());
        messageLabel.setFont(ModernTheme.FONT_REGULAR);
        contentPanel.add(messageLabel, BorderLayout.NORTH);

        // Input field
        JTextField inputField = createThemedTextField(initialValue, 30);
        contentPanel.add(inputField, BorderLayout.CENTER);

        // Buttons
        JButton okButton = createThemedButton("OK");
        okButton.addActionListener(e -> {
            result[0] = inputField.getText();
            dialog.dispose();
        });

        JButton cancelButton = createThemedButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(getBackground());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setModal(true);
        dialog.setVisible(true);

        return result[0];
    }

    /**
     * Shows a themed multi-field input dialog (for Save Script).
     *
     * @param parent parent component
     * @param title dialog title
     * @param fields map of field names to initial values
     * @return map of field names to user input, or null if cancelled
     */
    public static Map<String, String> showMultiInput(Component parent, String title, Map<String, String> fields) {
        JDialog dialog = createBaseDialog(parent, title);
        final Map<String, String> result = new HashMap<>();
        final boolean[] cancelled = {true};

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(getBackground());
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Create input fields
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBackground(getBackground());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Map<String, JTextField> fieldComponents = new HashMap<>();
        int row = 0;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            JLabel label = new JLabel(entry.getKey() + ":");
            label.setForeground(getForeground());
            label.setFont(ModernTheme.FONT_REGULAR);
            fieldsPanel.add(label, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            JTextField textField = createThemedTextField(entry.getValue(), 30);
            fieldComponents.put(entry.getKey(), textField);
            fieldsPanel.add(textField, gbc);

            row++;
        }

        contentPanel.add(fieldsPanel, BorderLayout.CENTER);

        // Buttons
        JButton okButton = createThemedButton("OK");
        okButton.addActionListener(e -> {
            for (Map.Entry<String, JTextField> entry : fieldComponents.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getText());
            }
            cancelled[0] = false;
            dialog.dispose();
        });

        JButton cancelButton = createThemedButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(getBackground());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setModal(true);
        dialog.setVisible(true);

        return cancelled[0] ? null : result;
    }

    // === Private Helper Methods ===

    private static JDialog createBaseDialog(Component parent, String title) {
        Window owner = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(getBackground());
        dialog.setIconImage(createPython3Icon());  // v2.5.4: Custom icon
        return dialog;
    }

    /**
     * Creates a custom Python 3 icon for dialogs and windows.
     *
     * v2.5.4: Programmatically generates a Python-themed icon
     *
     * @return BufferedImage icon (64x64 pixels)
     */
    public static java.awt.image.BufferedImage createPython3Icon() {
        int size = 64;
        java.awt.image.BufferedImage icon = new java.awt.image.BufferedImage(
            size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();

        // Enable antialiasing for smooth edges
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background: Dark circle with Python blue gradient
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(53, 114, 165),  // Python blue
            size, size, new Color(48, 105, 152)
        );
        g.setPaint(gradient);
        g.fillOval(2, 2, size - 4, size - 4);

        // Border: Lighter blue outline
        g.setColor(new Color(70, 130, 180));
        g.setStroke(new BasicStroke(2.5f));
        g.drawOval(2, 2, size - 4, size - 4);

        // Draw "P3" text in white (for Python 3)
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 28));
        FontMetrics fm = g.getFontMetrics();
        String text = "P3";
        int textX = (size - fm.stringWidth(text)) / 2;
        int textY = (size - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, textX, textY);

        g.dispose();
        return icon;
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

    private static JTextField createThemedTextField(String initialValue, int columns) {
        JTextField textField = new JTextField(initialValue != null ? initialValue : "", columns);
        textField.setBackground(getBackgroundDarker());
        textField.setForeground(getForeground());
        textField.setCaretColor(getForeground());
        textField.setFont(ModernTheme.FONT_REGULAR);
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getBorderColor(), 1),
            new EmptyBorder(5, 5, 5, 5)
        ));
        return textField;
    }
}
