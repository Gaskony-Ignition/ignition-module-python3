package com.inductiveautomation.ignition.examples.python3.designer.ui;

import com.inductiveautomation.ignition.examples.python3.designer.ModernTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command Palette overlay dialog (like VS Code's Ctrl+Shift+P).
 * Provides keyboard-driven access to all IDE commands with fuzzy search.
 *
 * @since v2.8.0
 */
public class CommandPaletteDialog extends JDialog {
    private final JTextField searchField;
    private final JList<Command> commandList;
    private final DefaultListModel<Command> listModel;
    private final List<Command> allCommands;

    /**
     * Represents a command in the palette.
     */
    public static class Command {
        private final String name;
        private final String description;
        private final String category;
        private final Runnable action;
        private final KeyStroke shortcut;

        public Command(String name, String description, String category, Runnable action) {
            this(name, description, category, action, null);
        }

        public Command(String name, String description, String category, Runnable action, KeyStroke shortcut) {
            this.name = name;
            this.description = description;
            this.category = category;
            this.action = action;
            this.shortcut = shortcut;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getCategory() {
            return category;
        }

        public void execute() {
            if (action != null) {
                action.run();
            }
        }

        public KeyStroke getShortcut() {
            return shortcut;
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * Calculates fuzzy match score (0 = no match, higher = better match).
         */
        public int fuzzyMatch(String query) {
            if (query.isEmpty()) {
                return 100;  // Show all when query is empty
            }

            query = query.toLowerCase();
            String nameLower = name.toLowerCase();
            String descLower = description.toLowerCase();

            // Exact match = highest score
            if (nameLower.equals(query)) {
                return 1000;
            }

            // Starts with query = high score
            if (nameLower.startsWith(query)) {
                return 500;
            }

            // Contains query = medium score
            if (nameLower.contains(query)) {
                return 200;
            }

            // Description match = lower score
            if (descLower.contains(query)) {
                return 100;
            }

            // Fuzzy match: all chars of query appear in order in name
            int score = 0;
            int pos = 0;
            for (char c : query.toCharArray()) {
                int found = nameLower.indexOf(c, pos);
                if (found == -1) {
                    return 0;  // No match
                }
                score += 10;
                pos = found + 1;
            }

            return score;
        }
    }

    /**
     * Creates a new Command Palette dialog.
     *
     * @param parent the parent frame
     */
    public CommandPaletteDialog(Frame parent) {
        super(parent, "Command Palette", false);

        this.allCommands = new ArrayList<>();
        this.listModel = new DefaultListModel<>();
        this.commandList = new JList<>(listModel);
        this.searchField = new JTextField();

        initializeUI();
        attachListeners();
    }

    /**
     * Initializes the UI components.
     */
    private void initializeUI() {
        // Dialog setup
        setUndecorated(true);  // Remove window decorations for overlay effect
        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Main panel with dark theme
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(ModernTheme.BACKGROUND_DARK);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernTheme.ACCENT_PRIMARY, 2),
                new EmptyBorder(10, 10, 10, 10)
        ));

        // Search field
        searchField.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 14));
        searchField.setBackground(ModernTheme.BACKGROUND_DARKER);
        searchField.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        searchField.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);
        searchField.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Command list
        commandList.setFont(ModernTheme.FONT_REGULAR);
        commandList.setBackground(ModernTheme.BACKGROUND_DARK);
        commandList.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        commandList.setSelectionBackground(ModernTheme.ACCENT_PRIMARY);
        commandList.setSelectionForeground(Color.WHITE);
        commandList.setCellRenderer(new CommandCellRenderer());
        commandList.setVisibleRowCount(10);

        JScrollPane scrollPane = new JScrollPane(commandList);
        scrollPane.setBorder(null);
        scrollPane.setBackground(ModernTheme.BACKGROUND_DARK);

        // Layout
        mainPanel.add(searchField, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Instructions label
        JLabel instructionsLabel = new JLabel("Type to filter commands, ↑↓ to navigate, Enter to execute, Esc to close");
        instructionsLabel.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 10));
        instructionsLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        instructionsLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
        mainPanel.add(instructionsLabel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        setSize(600, 400);
    }

    /**
     * Attaches event listeners.
     */
    private void attachListeners() {
        // Search field listeners
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ESCAPE:
                        dispose();
                        break;

                    case KeyEvent.VK_ENTER:
                        executeSelectedCommand();
                        break;

                    case KeyEvent.VK_UP:
                        int currentIndex = commandList.getSelectedIndex();
                        if (currentIndex > 0) {
                            commandList.setSelectedIndex(currentIndex - 1);
                            commandList.ensureIndexIsVisible(currentIndex - 1);
                        }
                        e.consume();
                        break;

                    case KeyEvent.VK_DOWN:
                        int current = commandList.getSelectedIndex();
                        if (current < listModel.getSize() - 1) {
                            commandList.setSelectedIndex(current + 1);
                            commandList.ensureIndexIsVisible(current + 1);
                        }
                        e.consume();
                        break;
                }
            }
        });

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filterCommands();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filterCommands();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filterCommands();
            }
        });

        // Command list double-click
        commandList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    executeSelectedCommand();
                }
            }
        });

        // Close on focus loss
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                dispose();
            }
        });
    }

    /**
     * Filters commands based on search query.
     */
    private void filterCommands() {
        String query = searchField.getText().trim();

        // Calculate scores and filter
        List<Command> filtered = allCommands.stream()
                .map(cmd -> new ScoredCommand(cmd, cmd.fuzzyMatch(query)))
                .filter(sc -> sc.score > 0)
                .sorted((a, b) -> Integer.compare(b.score, a.score))  // Highest score first
                .map(sc -> sc.command)
                .collect(Collectors.toList());

        // Update list
        listModel.clear();
        for (Command cmd : filtered) {
            listModel.addElement(cmd);
        }

        // Select first item
        if (!listModel.isEmpty()) {
            commandList.setSelectedIndex(0);
        }
    }

    /**
     * Helper class for scoring commands.
     */
    private static class ScoredCommand {
        final Command command;
        final int score;

        ScoredCommand(Command command, int score) {
            this.command = command;
            this.score = score;
        }
    }

    /**
     * Executes the currently selected command.
     */
    private void executeSelectedCommand() {
        Command selected = commandList.getSelectedValue();
        if (selected != null) {
            dispose();
            // Execute on EDT after dialog closes
            SwingUtilities.invokeLater(selected::execute);
        }
    }

    /**
     * Adds a command to the palette.
     */
    public void addCommand(Command command) {
        allCommands.add(command);
    }

    /**
     * Shows the command palette centered on parent.
     */
    public void showPalette() {
        // Center on parent
        if (getOwner() != null) {
            setLocationRelativeTo(getOwner());
        }

        // Populate initial list
        filterCommands();

        // Show and focus search field
        setVisible(true);
        searchField.requestFocusInWindow();
    }

    /**
     * Custom cell renderer for commands.
     */
    private static class CommandCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof Command) {
                Command cmd = (Command) value;

                // Format: "Command Name - description"
                String text = "<html><b>" + cmd.getName() + "</b>";
                if (cmd.getCategory() != null && !cmd.getCategory().isEmpty()) {
                    text += " <span style='color: gray;'>(" + cmd.getCategory() + ")</span>";
                }
                text += "<br/><span style='font-size: 10px; color: gray;'>" + cmd.getDescription() + "</span>";

                if (cmd.getShortcut() != null) {
                    text += " <span style='font-size: 10px; color: #5599FF;'>[" + formatShortcut(cmd.getShortcut()) + "]</span>";
                }
                text += "</html>";

                setText(text);
                setBorder(new EmptyBorder(5, 10, 5, 10));
            }

            return this;
        }

        private String formatShortcut(KeyStroke ks) {
            String text = "";
            int modifiers = ks.getModifiers();

            if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
                text += "Ctrl+";
            }
            if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
                text += "Shift+";
            }
            if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
                text += "Alt+";
            }

            text += KeyEvent.getKeyText(ks.getKeyCode());
            return text;
        }
    }
}
