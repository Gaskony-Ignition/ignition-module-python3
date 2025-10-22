package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

/**
 * True terminal-style panel with inline command/output history.
 *
 * v2.5.9: Real terminal experience
 * - Single scrolling view with all history
 * - Command input at bottom with prompt
 * - Enter key executes command
 * - Output appears inline after command
 */
public class TerminalPanel extends JPanel {

    private final JTextArea historyArea;
    private final JTextField commandField;
    private final JLabel promptLabel;
    private final Consumer<String> commandExecutor;

    private String currentPrompt = "$";
    private String currentWorkingDirectory = "~";
    private String currentUser = System.getProperty("user.name");

    /**
     * Creates a new terminal panel.
     *
     * @param commandExecutor callback to execute commands
     */
    public TerminalPanel(Consumer<String> commandExecutor) {
        this.commandExecutor = commandExecutor;

        setLayout(new BorderLayout());
        setBackground(ModernTheme.BACKGROUND_DARK);

        // History area (top) - shows all past commands and output
        historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        historyArea.setBackground(new Color(10, 10, 10));  // Very dark, like real terminal
        historyArea.setForeground(new Color(200, 200, 200));
        historyArea.setCaretColor(new Color(200, 200, 200));
        historyArea.setBorder(null);
        historyArea.setLineWrap(false);

        // Scroll pane for history (no scrollbars - per user request)
        JScrollPane historyScroll = new JScrollPane(historyArea);
        historyScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        historyScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        historyScroll.setBorder(null);
        historyScroll.setViewportBorder(null);
        historyScroll.setBackground(new Color(10, 10, 10));
        historyScroll.getViewport().setBackground(new Color(10, 10, 10));

        // Command input panel (bottom)
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setBackground(new Color(10, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Prompt label (shows user@host:/path$)
        promptLabel = new JLabel(buildPrompt());
        promptLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
        promptLabel.setForeground(new Color(100, 200, 100));  // Green prompt, terminal style

        // Command input field
        commandField = new JTextField();
        commandField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        commandField.setBackground(new Color(10, 10, 10));
        commandField.setForeground(new Color(200, 200, 200));
        commandField.setCaretColor(new Color(200, 200, 200));
        commandField.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // Enter key executes command
        commandField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    executeCommand();
                }
            }
        });

        inputPanel.add(promptLabel, BorderLayout.WEST);
        inputPanel.add(commandField, BorderLayout.CENTER);

        add(historyScroll, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // Add welcome message
        appendToHistory("Terminal started. Interactive shell session active.\n");
        appendToHistory("Type commands and press Enter. Type 'clear' to clear history.\n\n");
    }

    /**
     * Builds the terminal prompt string.
     */
    private String buildPrompt() {
        return currentUser + "@ignition:" + currentWorkingDirectory + currentPrompt + " ";
    }

    /**
     * Updates the prompt with new working directory.
     */
    public void updateWorkingDirectory(String pwd) {
        this.currentWorkingDirectory = pwd;
        promptLabel.setText(buildPrompt());
    }

    /**
     * Executes the current command.
     */
    private void executeCommand() {
        String command = commandField.getText().trim();

        if (command.isEmpty()) {
            return;
        }

        // Special command: clear history
        if (command.equalsIgnoreCase("clear")) {
            historyArea.setText("");
            commandField.setText("");
            return;
        }

        // Append command to history
        appendToHistory(buildPrompt() + command + "\n");

        // Clear input field
        commandField.setText("");

        // Execute command via callback
        if (commandExecutor != null) {
            commandExecutor.accept(command);
        }
    }

    /**
     * Appends text to the history area.
     */
    public void appendToHistory(String text) {
        historyArea.append(text);

        // Auto-scroll to bottom
        historyArea.setCaretPosition(historyArea.getDocument().getLength());
    }

    /**
     * Appends command output to history.
     */
    public void appendOutput(String output) {
        if (output != null && !output.isEmpty()) {
            appendToHistory(output);
            if (!output.endsWith("\n")) {
                appendToHistory("\n");
            }
        }
        appendToHistory("\n");  // Extra line after output
    }

    /**
     * Clears the entire history.
     */
    public void clearHistory() {
        historyArea.setText("");
    }

    /**
     * Focuses the command input field.
     */
    public void focusCommandInput() {
        commandField.requestFocusInWindow();
    }
}
