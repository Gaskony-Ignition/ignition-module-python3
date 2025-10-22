package com.inductiveautomation.ignition.examples.python3.designer.ui;

import com.inductiveautomation.ignition.examples.python3.designer.ModernTheme;
import com.inductiveautomation.ignition.examples.python3.designer.WarpScrollBarUI;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;

/**
 * Panel containing code editor and execution results.
 * Simplified for v2.0 architecture.
 *
 * v2.0.0: Extracted from Python3IDE.java monolith
 * v2.0.6: Added find/replace toolbar
 */
public class EditorPanel extends JPanel {

    private final RSyntaxTextArea codeEditor;
    private final JTextArea outputArea;
    private final JTextArea errorArea;
    private final JTextField findField;
    private final JTextField replaceField;
    private final JCheckBox matchCaseCheckbox;
    private final JComboBox<String> executionModeCombo;

    public EditorPanel() {
        setLayout(new BorderLayout());
        setBackground(ModernTheme.BACKGROUND_DARK);

        // Find/Replace toolbar
        JPanel findReplacePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        findReplacePanel.setBackground(ModernTheme.PANEL_BACKGROUND);

        findField = new JTextField(15);
        findField.setFont(ModernTheme.FONT_REGULAR);

        replaceField = new JTextField(15);
        replaceField.setFont(ModernTheme.FONT_REGULAR);

        matchCaseCheckbox = new JCheckBox("Match Case");
        matchCaseCheckbox.setFont(ModernTheme.FONT_REGULAR);
        matchCaseCheckbox.setBackground(ModernTheme.PANEL_BACKGROUND);
        matchCaseCheckbox.setForeground(ModernTheme.FOREGROUND_PRIMARY);

        JButton findNextButton = new JButton("Find Next");
        findNextButton.setFont(ModernTheme.FONT_REGULAR);
        findNextButton.addActionListener(e -> findNext());

        JButton replaceButton = new JButton("Replace");
        replaceButton.setFont(ModernTheme.FONT_REGULAR);
        replaceButton.addActionListener(e -> replace());

        JButton replaceAllButton = new JButton("Replace All");
        replaceAllButton.setFont(ModernTheme.FONT_REGULAR);
        replaceAllButton.addActionListener(e -> replaceAll());

        JButton clearOutputButton = new JButton("Clear Output");
        clearOutputButton.setFont(ModernTheme.FONT_REGULAR);
        clearOutputButton.addActionListener(e -> clearOutput());

        // Execution mode selector (v2.5.0)
        executionModeCombo = new JComboBox<>(new String[]{"Python Code", "Shell Command"});
        executionModeCombo.setFont(ModernTheme.FONT_REGULAR);
        executionModeCombo.setBackground(ModernTheme.PANEL_BACKGROUND);
        executionModeCombo.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        executionModeCombo.setToolTipText("Select execution mode: Python code or direct shell commands");

        findReplacePanel.add(new JLabel("Find:"));
        findReplacePanel.add(findField);
        findReplacePanel.add(new JLabel("Replace:"));
        findReplacePanel.add(replaceField);
        findReplacePanel.add(matchCaseCheckbox);
        findReplacePanel.add(findNextButton);
        findReplacePanel.add(replaceButton);
        findReplacePanel.add(replaceAllButton);
        findReplacePanel.add(new JLabel("  Mode:"));
        findReplacePanel.add(executionModeCombo);
        findReplacePanel.add(clearOutputButton);

        add(findReplacePanel, BorderLayout.NORTH);

        // Code editor
        codeEditor = new RSyntaxTextArea(20, 60);
        codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
        codeEditor.setCodeFoldingEnabled(true);
        codeEditor.setFont(ModernTheme.withSize(ModernTheme.FONT_MONOSPACE, 13));
        codeEditor.setTabSize(4);
        codeEditor.setBackground(ModernTheme.BACKGROUND_DARKER);
        codeEditor.setForeground(ModernTheme.FOREGROUND_PRIMARY);

        RTextScrollPane editorScroll = new RTextScrollPane(codeEditor);
        editorScroll.setLineNumbersEnabled(true);

        // UX Fix v2.4.2: NULL borders for completely seamless appearance
        editorScroll.setBorder(null);
        editorScroll.setViewportBorder(null);
        if (editorScroll.getGutter() != null) {
            editorScroll.getGutter().setBorder(null);
        }

        // Match dark theme backgrounds
        editorScroll.setBackground(new Color(30, 30, 30));
        editorScroll.getViewport().setBackground(new Color(30, 30, 30));

        // UX Fix v2.4.2: Warp-style invisible scrollbars (subtle grey thumb only)
        applyMinimalScrollBar(editorScroll.getVerticalScrollBar());
        applyMinimalScrollBar(editorScroll.getHorizontalScrollBar());

        // Output/error areas - exact color matching per user spec
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(ModernTheme.FONT_MONOSPACE);
        outputArea.setBackground(new Color(30, 30, 30));
        outputArea.setForeground(new Color(200, 200, 200));
        outputArea.setCaretColor(new Color(200, 200, 200));
        outputArea.setBorder(null);

        errorArea = new JTextArea();
        errorArea.setEditable(false);
        errorArea.setFont(ModernTheme.FONT_MONOSPACE);
        errorArea.setBackground(new Color(30, 30, 30));
        errorArea.setForeground(ModernTheme.ERROR);
        errorArea.setCaretColor(new Color(200, 200, 200));
        errorArea.setBorder(null);

        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        outputScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // UX Fix v2.4.2: NULL borders (not empty) for seamless appearance
        outputScroll.setBorder(null);
        outputScroll.setViewportBorder(null);

        // Exact color matching to parent panel
        outputScroll.setBackground(new Color(30, 30, 30));
        outputScroll.getViewport().setBackground(new Color(30, 30, 30));

        // UX Fix v2.4.2: Minimal Warp-style scrollbars
        applyMinimalScrollBar(outputScroll.getVerticalScrollBar());
        applyMinimalScrollBar(outputScroll.getHorizontalScrollBar());

        JScrollPane errorScroll = new JScrollPane(errorArea);
        errorScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        errorScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // UX Fix v2.4.2: NULL borders for seamless appearance
        errorScroll.setBorder(null);
        errorScroll.setViewportBorder(null);

        // Exact color matching
        errorScroll.setBackground(new Color(30, 30, 30));
        errorScroll.getViewport().setBackground(new Color(30, 30, 30));

        // UX Fix v2.4.2: Minimal Warp-style scrollbars
        applyMinimalScrollBar(errorScroll.getVerticalScrollBar());
        applyMinimalScrollBar(errorScroll.getHorizontalScrollBar());

        JTabbedPane outputTabs = new JTabbedPane();
        outputTabs.addTab("Output", outputScroll);
        outputTabs.addTab("Errors", errorScroll);

        // Match parent panel background to eliminate white gaps
        outputTabs.setBackground(new Color(30, 30, 30));
        outputTabs.setForeground(new Color(200, 200, 200));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(editorScroll);
        splitPane.setBottomComponent(outputTabs);
        splitPane.setResizeWeight(0.7);
        splitPane.setDividerLocation(400);
        splitPane.setBackground(ModernTheme.BACKGROUND_DARK);
        splitPane.setBorder(null);
        splitPane.setDividerSize(8);

        // UX Fix: Style divider to match dark theme
        if (splitPane.getUI() instanceof javax.swing.plaf.basic.BasicSplitPaneUI) {
            ((javax.swing.plaf.basic.BasicSplitPaneUI) splitPane.getUI()).getDivider().setBackground(ModernTheme.BACKGROUND_DARKER);
        }

        add(splitPane, BorderLayout.CENTER);
    }

    public String getCode() {
        return codeEditor.getText();
    }

    public void setCode(String code) {
        codeEditor.setText(code != null ? code : "");
    }

    public void setOutput(String text) {
        outputArea.setText(text != null ? text : "");
    }

    public void setError(String text) {
        errorArea.setText(text != null ? text : "");
    }

    public void clearOutput() {
        outputArea.setText("");
        errorArea.setText("");
    }

    public RSyntaxTextArea getCodeEditor() {
        return codeEditor;
    }

    public JTextArea getOutputArea() {
        return outputArea;
    }

    public JTextArea getErrorArea() {
        return errorArea;
    }

    /**
     * Get execution mode ("Python Code" or "Shell Command")
     * @return selected execution mode
     */
    public String getExecutionMode() {
        return (String) executionModeCombo.getSelectedItem();
    }

    /**
     * Check if Shell Command mode is selected
     * @return true if Shell Command mode is active
     */
    public boolean isShellMode() {
        return "Shell Command".equals(getExecutionMode());
    }

    private void findNext() {
        String searchText = findField.getText();
        if (searchText == null || searchText.isEmpty()) {
            return;
        }

        SearchContext context = new SearchContext();
        context.setSearchFor(searchText);
        context.setMatchCase(matchCaseCheckbox.isSelected());
        context.setSearchForward(true);
        context.setWholeWord(false);

        SearchEngine.find(codeEditor, context);
    }

    private void replace() {
        String searchText = findField.getText();
        String replaceText = replaceField.getText();

        if (searchText == null || searchText.isEmpty()) {
            return;
        }

        SearchContext context = new SearchContext();
        context.setSearchFor(searchText);
        context.setReplaceWith(replaceText != null ? replaceText : "");
        context.setMatchCase(matchCaseCheckbox.isSelected());
        context.setSearchForward(true);
        context.setWholeWord(false);

        SearchEngine.replace(codeEditor, context);
    }

    private void replaceAll() {
        String searchText = findField.getText();
        String replaceText = replaceField.getText();

        if (searchText == null || searchText.isEmpty()) {
            return;
        }

        SearchContext context = new SearchContext();
        context.setSearchFor(searchText);
        context.setReplaceWith(replaceText != null ? replaceText : "");
        context.setMatchCase(matchCaseCheckbox.isSelected());
        context.setSearchForward(true);
        context.setWholeWord(false);

        SearchEngine.replaceAll(codeEditor, context);
    }

    /**
     * Applies minimal Warp-style scrollbar - invisible track with subtle grey thumb only.
     *
     * v2.4.2: Simplified approach per user feedback - hidden tracks, minimal grey thumb
     *
     * @param scrollBar the scrollbar to style
     */
    private void applyMinimalScrollBar(JScrollBar scrollBar) {
        if (scrollBar == null) {
            return;
        }

        scrollBar.setOpaque(false);
        scrollBar.setUnitIncrement(16);  // Smooth scrolling

        // Custom UI: invisible track, subtle grey rounded thumb
        scrollBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected javax.swing.JButton createDecreaseButton(int orientation) {
                return createInvisibleButton();
            }

            @Override
            protected javax.swing.JButton createIncreaseButton(int orientation) {
                return createInvisibleButton();
            }

            private javax.swing.JButton createInvisibleButton() {
                javax.swing.JButton button = new javax.swing.JButton();
                button.setPreferredSize(new java.awt.Dimension(0, 0));
                button.setMinimumSize(new java.awt.Dimension(0, 0));
                button.setMaximumSize(new java.awt.Dimension(0, 0));
                return button;
            }

            @Override
            protected void paintTrack(java.awt.Graphics g, javax.swing.JComponent c, java.awt.Rectangle trackBounds) {
                // Invisible track - paint nothing
            }

            @Override
            protected void paintThumb(java.awt.Graphics g, javax.swing.JComponent c, java.awt.Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                    return;
                }

                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                                   java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

                // Subtle grey thumb
                g2.setColor(new java.awt.Color(80, 80, 80));
                g2.fillRoundRect(thumbBounds.x, thumbBounds.y,
                               thumbBounds.width, thumbBounds.height, 4, 4);

                g2.dispose();
            }
        });
    }
}
