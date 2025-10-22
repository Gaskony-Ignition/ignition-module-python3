package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Panel displaying metadata for a selected script.
 * Shows: name, author, created date, modified date, version, description.
 */
public class ScriptMetadataPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final JLabel nameLabel;
    private final JLabel authorLabel;
    private final JLabel createdLabel;
    private final JLabel modifiedLabel;
    private final JLabel versionLabel;
    private final JTextArea descriptionArea;

    public ScriptMetadataPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT),
                        "Script Information",
                        TitledBorder.DEFAULT_JUSTIFICATION,
                        TitledBorder.DEFAULT_POSITION,
                        ModernTheme.FONT_REGULAR,
                        ModernTheme.FOREGROUND_PRIMARY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        setPreferredSize(new Dimension(250, 320));  // Increased from 280 to 320 for even larger description area
        setBackground(ModernTheme.PANEL_BACKGROUND);

        // Create labels
        nameLabel = createValueLabel();
        authorLabel = createValueLabel();
        createdLabel = createValueLabel();
        modifiedLabel = createValueLabel();
        versionLabel = createValueLabel();

        // Description text area - expanded for better usability (v2.0.1 UX fix)
        descriptionArea = new JTextArea();
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));
        descriptionArea.setBackground(ModernTheme.BACKGROUND_DARKER);
        descriptionArea.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        descriptionArea.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);

        // Expanded size for better usability (v2.0.1 UX fix: 60px → 100px)
        descriptionArea.setMinimumSize(new Dimension(200, 60));
        descriptionArea.setPreferredSize(new Dimension(200, 100));

        // Layout
        JPanel fieldsPanel = new JPanel(new GridLayout(5, 2, 5, 3));
        fieldsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        fieldsPanel.setBackground(ModernTheme.PANEL_BACKGROUND);

        fieldsPanel.add(createKeyLabel("Name:"));
        fieldsPanel.add(nameLabel);

        fieldsPanel.add(createKeyLabel("Author:"));
        fieldsPanel.add(authorLabel);

        fieldsPanel.add(createKeyLabel("Created:"));
        fieldsPanel.add(createdLabel);

        fieldsPanel.add(createKeyLabel("Modified:"));
        fieldsPanel.add(modifiedLabel);

        fieldsPanel.add(createKeyLabel("Version:"));
        fieldsPanel.add(versionLabel);

        add(fieldsPanel, BorderLayout.NORTH);

        // Description section with better spacing
        JPanel descPanel = new JPanel(new BorderLayout(3, 3));
        descPanel.setBorder(new EmptyBorder(5, 5, 5, 5));  // More padding
        descPanel.setBackground(ModernTheme.PANEL_BACKGROUND);

        JLabel descLabel = createKeyLabel("Description:");
        descLabel.setBorder(new EmptyBorder(0, 0, 3, 0));  // Space below label
        descPanel.add(descLabel, BorderLayout.NORTH);

        // Description area without scrollbar - 30% taller than original (v2.3.3 UX improvement)
        // Removed scroll pane as requested - if description is too long, it just won't fit
        descriptionArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT),
            new EmptyBorder(5, 5, 5, 5)
        ));
        descriptionArea.setPreferredSize(new Dimension(240, 244));  // Increased by 30% from 188px (v2.3.3)
        descriptionArea.setMinimumSize(new Dimension(240, 150));

        descPanel.add(descriptionArea, BorderLayout.CENTER);

        add(descPanel, BorderLayout.CENTER);

        // Initially show "No script selected"
        clear();
    }

    /**
     * Displays metadata for a script.
     *
     * @param metadata the script metadata
     */
    public void displayMetadata(ScriptMetadata metadata) {
        if (metadata == null) {
            clear();
            return;
        }

        nameLabel.setText(metadata.getName());
        authorLabel.setText(metadata.getAuthor() != null ? metadata.getAuthor() : "Unknown");

        createdLabel.setText(formatDate(metadata.getCreatedDate()));
        modifiedLabel.setText(formatDate(metadata.getLastModified()));

        versionLabel.setText(metadata.getVersion() != null ? metadata.getVersion() : "1.0");

        String description = metadata.getDescription() != null ? metadata.getDescription() : "";
        descriptionArea.setText(description);

        // Scroll to top when displaying new metadata
        descriptionArea.setCaretPosition(0);
    }

    /**
     * Clears all metadata fields.
     */
    public void clear() {
        nameLabel.setText("—");
        authorLabel.setText("—");
        createdLabel.setText("—");
        modifiedLabel.setText("—");
        versionLabel.setText("—");
        descriptionArea.setText("");
    }

    /**
     * Formats an ISO 8601 date string to human-readable format.
     *
     * @param isoDate ISO 8601 date string
     * @return formatted date string
     */
    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            return "—";
        }

        try {
            Instant instant = Instant.parse(isoDate);
            return DATE_FORMAT.format(instant);
        } catch (Exception e) {
            // If parsing fails, return original string
            return isoDate;
        }
    }

    /**
     * Creates a key label (e.g., "Name:", "Author:").
     *
     * @param text the label text
     * @return configured label
     */
    private JLabel createKeyLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ModernTheme.withSize(ModernTheme.FONT_BOLD, 11));
        label.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        return label;
    }

    /**
     * Creates a value label (displays actual metadata values).
     *
     * @return configured label
     */
    private JLabel createValueLabel() {
        JLabel label = new JLabel("—");
        label.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));
        label.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        return label;
    }

    /**
     * Applies theme to the metadata panel.
     *
     * @param isDarkTheme true for dark theme, false for light theme
     *
     * v2.0.15: Added for dynamic theme switching
     */
    public void applyTheme(boolean isDarkTheme) {
        if (isDarkTheme) {
            // Dark theme
            setBackground(ModernTheme.PANEL_BACKGROUND);
            descriptionArea.setBackground(ModernTheme.BACKGROUND_DARKER);
            descriptionArea.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            descriptionArea.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);
        } else {
            // Light theme
            setBackground(Color.WHITE);
            descriptionArea.setBackground(Color.WHITE);
            descriptionArea.setForeground(Color.BLACK);
            descriptionArea.setCaretColor(Color.BLACK);
        }
        repaint();
    }
}
