package com.gaskony.python3.designer.managers;

import com.gaskony.python3.designer.ui.FindReplaceDialog;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Frame;

/**
 * Manages search and replace dialogs for the Python 3 IDE.
 * Provides lazy initialization and lifecycle management for Find, Replace, and Advanced Find/Replace dialogs.
 *
 * @since v2.8.0
 */
public class SearchManager {
    private final Component parentComponent;
    private final RSyntaxTextArea codeEditor;
    private final SearchListener searchListener;

    private FindDialog findDialog;
    private ReplaceDialog replaceDialog;
    private FindReplaceDialog advancedFindReplaceDialog;

    /**
     * Creates a new SearchManager with dependency injection.
     *
     * @param parentComponent parent component for dialog positioning
     * @param codeEditor code editor to search within
     * @param searchListener listener for search events
     */
    public SearchManager(
        Component parentComponent,
        RSyntaxTextArea codeEditor,
        SearchListener searchListener
    ) {
        this.parentComponent = parentComponent;
        this.codeEditor = codeEditor;
        this.searchListener = searchListener;
    }

    /**
     * Shows the Find dialog.
     * Uses lazy initialization - dialog is created on first use.
     */
    public void showFindDialog() {
        if (findDialog == null) {
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(parentComponent);
            findDialog = new FindDialog(parentFrame, searchListener);
            findDialog.setSearchContext(new SearchContext());
        }

        findDialog.setVisible(true);
    }

    /**
     * Shows the Replace dialog.
     * Uses lazy initialization - dialog is created on first use.
     */
    public void showReplaceDialog() {
        if (replaceDialog == null) {
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(parentComponent);
            replaceDialog = new ReplaceDialog(parentFrame, searchListener);
            replaceDialog.setSearchContext(new SearchContext());
        }

        replaceDialog.setVisible(true);
    }

    /**
     * Shows the advanced Find/Replace dialog (v2.1.0).
     * This dialog includes regex support, whole word matching, and search history.
     * Uses lazy initialization - dialog is created on first use.
     */
    public void showAdvancedFindReplaceDialog() {
        if (advancedFindReplaceDialog == null) {
            JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(parentComponent);
            advancedFindReplaceDialog = new FindReplaceDialog(parentFrame, codeEditor);
        }

        advancedFindReplaceDialog.showDialog();
    }

    /**
     * Disposes all search dialogs and releases resources.
     * Call this when closing the IDE to prevent memory leaks.
     */
    public void shutdown() {
        if (findDialog != null) {
            findDialog.dispose();
            findDialog = null;
        }

        if (replaceDialog != null) {
            replaceDialog.dispose();
            replaceDialog = null;
        }

        if (advancedFindReplaceDialog != null) {
            advancedFindReplaceDialog.dispose();
            advancedFindReplaceDialog = null;
        }
    }

    /**
     * Checks if any search dialog is currently visible.
     *
     * @return true if any search dialog is visible
     */
    public boolean isAnyDialogVisible() {
        return (findDialog != null && findDialog.isVisible())
            || (replaceDialog != null && replaceDialog.isVisible())
            || (advancedFindReplaceDialog != null && advancedFindReplaceDialog.isVisible());
    }
}
