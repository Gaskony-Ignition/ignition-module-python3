package com.inductiveautomation.ignition.examples.python3.designer;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks unsaved changes in the code editor.
 * Notifies listeners when the dirty state changes.
 */
public class UnsavedChangesTracker {

    private final RSyntaxTextArea editor;
    private String originalContent;
    private boolean dirty;
    private final List<ChangeListener> listeners;

    /**
     * Listener interface for dirty state changes.
     */
    public interface ChangeListener {
        void dirtyStateChanged(boolean isDirty);
    }

    /**
     * Creates a new unsaved changes tracker.
     *
     * @param editor the code editor to track
     */
    public UnsavedChangesTracker(RSyntaxTextArea editor) {
        this.editor = editor;
        this.originalContent = "";
        this.dirty = false;
        this.listeners = new ArrayList<>();

        // Listen to document changes
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                checkDirty();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                checkDirty();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                checkDirty();
            }
        });
    }

    /**
     * Marks the current content as saved (resets dirty flag).
     */
    public void markSaved() {
        originalContent = editor.getText();
        setDirty(false);
    }

    /**
     * Loads new content into the editor and marks it as saved.
     *
     * @param content the new content
     */
    public void loadContent(String content) {
        editor.setText(content);
        originalContent = content;
        setDirty(false);
    }

    /**
     * Checks if the current content differs from the saved content.
     *
     * @return true if there are unsaved changes
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Gets the current content of the editor.
     *
     * @return current editor content
     */
    public String getCurrentContent() {
        return editor.getText();
    }

    /**
     * Gets the original saved content.
     *
     * @return original content
     */
    public String getOriginalContent() {
        return originalContent;
    }

    /**
     * Adds a listener to be notified of dirty state changes.
     *
     * @param listener the listener
     */
    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a change listener.
     *
     * @param listener the listener
     */
    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Checks if content has changed and updates dirty flag.
     */
    private void checkDirty() {
        String current = editor.getText();
        boolean newDirty = !current.equals(originalContent);

        if (newDirty != dirty) {
            setDirty(newDirty);
        }
    }

    /**
     * Sets the dirty flag and notifies listeners.
     *
     * @param newDirty the new dirty state
     */
    private void setDirty(boolean newDirty) {
        dirty = newDirty;

        // Notify listeners
        for (ChangeListener listener : listeners) {
            listener.dirtyStateChanged(dirty);
        }
    }
}
