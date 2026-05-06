package com.inductiveautomation.ignition.examples.python3.designer.managers;

import com.inductiveautomation.ignition.examples.python3.designer.DarkDialog;
import com.inductiveautomation.ignition.examples.python3.designer.ModernTheme;
import com.inductiveautomation.ignition.examples.python3.designer.Python3RestClient;
import com.inductiveautomation.ignition.examples.python3.designer.SavedScript;
import com.inductiveautomation.ignition.examples.python3.designer.ScriptMetadata;
import com.inductiveautomation.ignition.examples.python3.designer.ScriptMetadataPanel;
import com.inductiveautomation.ignition.examples.python3.designer.ScriptTreeNode;
import com.inductiveautomation.ignition.examples.python3.designer.UnsavedChangesTracker;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Script CRUD operations for the Python 3 IDE: refresh tree, build tree, load,
 * save, save-as, rename, edit-metadata, move-to-folder, delete, create folder
 * / new script, plus the right-click context menu.
 *
 * <p>Extracted from {@code Python3IDE} in v3.13. Pure structural refactor; same
 * dialogs (DarkDialog), same SwingWorkers, same status messages.</p>
 *
 * <p>Public statics provide testable helpers: {@link #isValidName},
 * {@link #buildScriptTree}, {@link #convertToMetadata},
 * {@link #getFolderPathForNode}.</p>
 *
 * @since v3.13
 */
public class Python3IDEScriptOps {
    private static final Logger logger = LoggerFactory.getLogger(Python3IDEScriptOps.class);

    /** Set of characters that are not allowed in script or folder names. */
    static final String ILLEGAL_NAME_CHARS = "/\\:*?\"<>|";

    public interface ScriptOpsContext {
        JComponent getParent();
        RSyntaxTextArea getCodeEditor();
        JTree getScriptTree();
        DefaultTreeModel getTreeModel();
        ScriptTreeNode getRootNode();
        ScriptMetadataPanel getMetadataPanel();

        Python3RestClient getRestClient();
        UnsavedChangesTracker getChangesTracker();

        ScriptMetadata getCurrentScript();
        void setCurrentScript(ScriptMetadata metadata);

        boolean isUseDarkTheme();

        void setStatus(String message, Color color);
        void updateCurrentScriptLabel();
        void stylePopupMenu(JPopupMenu menu);
    }

    private final ScriptOpsContext ctx;

    public Python3IDEScriptOps(ScriptOpsContext ctx) {
        this.ctx = ctx;
    }

    // ===== Validation helpers =================================================

    /**
     * Validates a script or folder name &mdash; non-empty, no illegal chars.
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        for (char c : ILLEGAL_NAME_CHARS.toCharArray()) {
            if (name.indexOf(c) >= 0) {
                return false;
            }
        }
        return true;
    }

    public void showInvalidNameError(String name) {
        DarkDialog.showMessage(ctx.getParent(),
            "Invalid name: '" + name + "'\n\n"
                + "Names cannot contain the following characters:\n"
                + "/ \\ : * ? \" < > |",
            "Invalid Name");
    }

    // ===== Tree refresh / build ==============================================

    public void refreshScriptTree() {
        Python3RestClient restClient = ctx.getRestClient();
        if (restClient == null) {
            return;
        }

        SwingWorker<List<ScriptMetadata>, Void> worker = new SwingWorker<List<ScriptMetadata>, Void>() {
            @Override
            protected List<ScriptMetadata> doInBackground() throws Exception {
                return restClient.listScripts();
            }

            @Override
            protected void done() {
                try {
                    List<ScriptMetadata> scripts = get();
                    buildScriptTree(scripts, ctx.getRootNode(), ctx.getTreeModel(),
                        ctx.getScriptTree(), ctx.getCurrentScript(), ctx.getMetadataPanel());
                    logger.info("Loaded {} scripts", scripts.size());
                } catch (Exception e) {
                    logger.error("Failed to load scripts", e);
                }
            }
        };
        worker.execute();
    }

    /**
     * Rebuilds the script tree under {@code rootNode} from the given metadata
     * list. Public-static so it can be unit-tested without a live REST client.
     */
    public static void buildScriptTree(List<ScriptMetadata> scripts,
                                        ScriptTreeNode rootNode,
                                        DefaultTreeModel treeModel,
                                        JTree scriptTree,
                                        ScriptMetadata preservedMetadata,
                                        ScriptMetadataPanel metadataPanel) {
        rootNode.removeAllChildren();

        Map<String, ScriptTreeNode> folders = new HashMap<>();
        for (ScriptMetadata script : scripts) {
            String folderPath = script.getFolderPath();
            if (folderPath == null || folderPath.isEmpty()) {
                rootNode.add(new ScriptTreeNode(script));
            } else {
                ScriptTreeNode parent = getOrCreateFolder(folderPath, folders, rootNode);
                parent.add(new ScriptTreeNode(script));
            }
        }

        treeModel.reload();
        if (scriptTree != null) {
            scriptTree.expandRow(0);
        }

        if (preservedMetadata != null && preservedMetadata.getName() != null && metadataPanel != null) {
            metadataPanel.displayMetadata(preservedMetadata);
        }
    }

    /**
     * Gets or creates the folder node at the given path. Public-static for
     * unit testing.
     */
    public static ScriptTreeNode getOrCreateFolder(String folderPath,
                                                    Map<String, ScriptTreeNode> folders,
                                                    ScriptTreeNode rootNode) {
        if (folders.containsKey(folderPath)) {
            return folders.get(folderPath);
        }

        String[] parts = folderPath.split("/");
        ScriptTreeNode currentParent = rootNode;
        StringBuilder currentPath = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (currentPath.length() > 0) {
                currentPath.append("/");
            }
            currentPath.append(part);

            String pathStr = currentPath.toString();

            if (folders.containsKey(pathStr)) {
                currentParent = folders.get(pathStr);
            } else {
                ScriptTreeNode folderNode = new ScriptTreeNode(part);
                currentParent.add(folderNode);
                folders.put(pathStr, folderNode);
                currentParent = folderNode;
            }
        }

        return currentParent;
    }

    // ===== Tree selection / loading ==========================================

    public void onTreeSelectionChanged() {
        TreePath path = ctx.getScriptTree().getSelectionPath();
        ScriptMetadataPanel metadataPanel = ctx.getMetadataPanel();

        if (path == null) {
            metadataPanel.clear();
            return;
        }

        Object node = path.getLastPathComponent();
        if (node instanceof ScriptTreeNode) {
            ScriptTreeNode scriptNode = (ScriptTreeNode) node;
            if (scriptNode.isScript()) {
                metadataPanel.displayMetadata(scriptNode.getScriptMetadata());
            } else {
                metadataPanel.clear();
            }
        }
    }

    public void loadSelectedScript() {
        TreePath path = ctx.getScriptTree().getSelectionPath();
        if (path == null) {
            return;
        }

        Object node = path.getLastPathComponent();
        if (!(node instanceof ScriptTreeNode)) {
            return;
        }
        ScriptTreeNode scriptNode = (ScriptTreeNode) node;
        if (!scriptNode.isScript()) {
            return;
        }

        if (ctx.getChangesTracker().isDirty()) {
            int choice = showUnsavedChangesDialog();
            if (choice == JOptionPane.YES_OPTION) {
                saveCurrentScript();
            } else if (choice == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }

        ScriptMetadata metadata = scriptNode.getScriptMetadata();
        loadScript(metadata.getName());
    }

    public void loadScript(String name) {
        Python3RestClient restClient = ctx.getRestClient();
        if (restClient == null) {
            return;
        }

        SwingWorker<SavedScript, Void> worker = new SwingWorker<SavedScript, Void>() {
            @Override
            protected SavedScript doInBackground() throws Exception {
                return restClient.loadScript(name);
            }

            @Override
            protected void done() {
                try {
                    SavedScript script = get();
                    ctx.getChangesTracker().loadContent(script.getCode());
                    ctx.setCurrentScript(convertToMetadata(script));
                    ctx.updateCurrentScriptLabel();
                    ctx.setStatus("Loaded: " + script.getName(), ModernTheme.SUCCESS);
                } catch (Exception e) {
                    logger.error("Failed to load script", e);
                    DarkDialog.showMessage(ctx.getParent(),
                        "Failed to load script: " + e.getMessage(), "Error");
                }
            }
        };
        worker.execute();
    }

    /**
     * Converts a SavedScript into a ScriptMetadata snapshot.
     */
    public static ScriptMetadata convertToMetadata(SavedScript script) {
        return new ScriptMetadata(
            script.getId(),
            script.getName(),
            script.getDescription(),
            script.getAuthor(),
            script.getCreatedDate(),
            script.getLastModified(),
            script.getFolderPath(),
            script.getVersion()
        );
    }

    // ===== Save / Save As ====================================================

    public void saveCurrentScript() {
        Python3RestClient restClient = ctx.getRestClient();
        JComponent parent = ctx.getParent();
        RSyntaxTextArea codeEditor = ctx.getCodeEditor();

        if (restClient == null) {
            DarkDialog.showMessage(parent, "Please connect to a Gateway first", "Not Connected");
            return;
        }

        String code = codeEditor.getText().trim();
        if (code.isEmpty()) {
            DarkDialog.showMessage(parent, "Cannot save empty script", "Empty Script");
            return;
        }

        ScriptMetadata currentScript = ctx.getCurrentScript();
        if (currentScript != null && currentScript.getName() != null && !currentScript.getName().isEmpty()) {
            String name = currentScript.getName();
            String author = currentScript.getAuthor() != null
                ? currentScript.getAuthor() : System.getProperty("user.name", "Unknown");
            String version = currentScript.getVersion() != null ? currentScript.getVersion() : "1.0";
            String folder = currentScript.getFolderPath() != null ? currentScript.getFolderPath() : "";
            String description = currentScript.getDescription() != null ? currentScript.getDescription() : "";

            logger.info("Quick save (no prompt) for existing script: {}", name);
            ctx.setStatus("Saving " + name + "...", Color.BLUE);
            saveScript(name, code, description, author, folder, version);
        } else {
            logger.info("New script - showing save dialog");
            saveScriptAs();
        }
    }

    public void saveScriptAs() {
        Python3RestClient restClient = ctx.getRestClient();
        JComponent parent = ctx.getParent();
        RSyntaxTextArea codeEditor = ctx.getCodeEditor();

        if (restClient == null) {
            DarkDialog.showMessage(parent, "Please connect to a Gateway first", "Not Connected");
            return;
        }

        String code = codeEditor.getText().trim();
        if (code.isEmpty()) {
            DarkDialog.showMessage(parent, "Cannot save empty script", "Empty Script");
            return;
        }

        showSaveDialog(null);
    }

    public void showSaveDialog(String folderPath) {
        ScriptMetadata currentScript = ctx.getCurrentScript();
        RSyntaxTextArea codeEditor = ctx.getCodeEditor();

        String defaultAuthor = currentScript != null
            ? currentScript.getAuthor() : System.getProperty("user.name", "Unknown");

        String defaultFolder = folderPath != null
            ? folderPath
            : (currentScript != null ? currentScript.getFolderPath() : "");

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Script Name", currentScript != null ? currentScript.getName() : "");
        fields.put("Author", defaultAuthor);
        fields.put("Version", currentScript != null ? currentScript.getVersion() : "1.0");
        fields.put("Folder Path", defaultFolder != null ? defaultFolder : "");
        fields.put("Description", currentScript != null ? currentScript.getDescription() : "");

        Map<String, String> result = DarkDialog.showMultiInput(ctx.getParent(), "Save Script", fields);
        if (result == null) {
            return;
        }

        String name = result.get("Script Name").trim();
        String author = result.get("Author").trim();
        String version = result.get("Version").trim();
        String folder = result.get("Folder Path").trim();
        String description = result.get("Description").trim();

        if (name.isEmpty()) {
            DarkDialog.showMessage(ctx.getParent(), "Script name cannot be empty", "Invalid Name");
            return;
        }

        if (!isValidName(name)) {
            showInvalidNameError(name);
            return;
        }

        saveScript(name, codeEditor.getText(), description, author, folder, version);
    }

    public void saveScript(String name, String code, String description,
                            String author, String folderPath, String version) {
        Python3RestClient restClient = ctx.getRestClient();

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                restClient.saveScript(name, code, description, author, folderPath, version);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ctx.getChangesTracker().markSaved();

                    ScriptMetadata current = ctx.getCurrentScript();
                    ctx.setCurrentScript(new ScriptMetadata(
                        current != null ? current.getId() : null,
                        name,
                        description,
                        author,
                        current != null ? current.getCreatedDate() : null,
                        current != null ? current.getLastModified() : null,
                        folderPath,
                        version
                    ));

                    ctx.setStatus("Script saved: " + name, ModernTheme.SUCCESS);
                    refreshScriptTree();
                } catch (Exception e) {
                    logger.error("Failed to save script", e);
                    DarkDialog.showMessage(ctx.getParent(),
                        "Failed to save script: " + e.getMessage(), "Error");
                }
            }
        };
        worker.execute();
    }

    // ===== Folder / new-script =================================================

    public void createNewFolder() {
        String folderName = DarkDialog.showInput(ctx.getParent(),
            "Enter folder name:", "New Folder", "");

        if (folderName == null || folderName.trim().isEmpty()) {
            return;
        }

        String trimmedName = folderName.trim();
        if (!isValidName(trimmedName)) {
            showInvalidNameError(trimmedName);
            return;
        }

        ScriptTreeNode rootNode = ctx.getRootNode();
        ScriptTreeNode newFolder = new ScriptTreeNode(trimmedName);
        rootNode.add(newFolder);
        ctx.getTreeModel().reload();
        ctx.getScriptTree().expandPath(new TreePath(rootNode.getPath()));
    }

    public void createNewScript() {
        createNewScript(null);
    }

    public void createNewScript(String folderPath) {
        if (ctx.getChangesTracker().isDirty()) {
            int choice = showUnsavedChangesDialog();
            if (choice == JOptionPane.YES_OPTION) {
                saveCurrentScript();
            } else if (choice == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }

        if (ctx.getRestClient() == null) {
            DarkDialog.showMessage(ctx.getParent(),
                "Please connect to a Gateway first", "Not Connected");
            return;
        }

        ctx.getChangesTracker().loadContent("# New Python Script\n\n");
        ctx.setCurrentScript(null);
        ctx.updateCurrentScriptLabel();
        ctx.getMetadataPanel().clear();
        ctx.setStatus("New script - enter details to save", Color.BLUE);

        showSaveDialog(folderPath);
    }

    // ===== Context menu ======================================================

    public void showContextMenu(MouseEvent e) {
        JTree scriptTree = ctx.getScriptTree();
        ScriptTreeNode rootNode = ctx.getRootNode();

        TreePath path = scriptTree.getPathForLocation(e.getX(), e.getY());
        if (path == null) {
            return;
        }

        scriptTree.setSelectionPath(path);
        Object node = path.getLastPathComponent();
        if (!(node instanceof ScriptTreeNode)) {
            return;
        }

        ScriptTreeNode scriptNode = (ScriptTreeNode) node;
        JPopupMenu menu = new JPopupMenu();

        if (scriptNode.isScript()) {
            JMenuItem loadItem = new JMenuItem("Load");
            loadItem.addActionListener(ev -> loadSelectedScript());
            menu.add(loadItem);

            JMenuItem exportItem = new JMenuItem("Export...");
            exportItem.addActionListener(ev -> { /* delegated to caller */ });
            menu.add(exportItem);

            JMenuItem renameItem = new JMenuItem("Rename...");
            renameItem.addActionListener(ev -> renameScript(scriptNode));
            menu.add(renameItem);

            JMenuItem editMetadataItem = new JMenuItem("Edit Metadata...");
            editMetadataItem.addActionListener(ev -> editMetadata(scriptNode));
            menu.add(editMetadataItem);

            JMenuItem moveItem = new JMenuItem("Move to Folder...");
            moveItem.addActionListener(ev -> showMoveToFolderDialog(scriptNode));
            menu.add(moveItem);

            menu.addSeparator();

            JMenuItem deleteItem = new JMenuItem("Delete");
            deleteItem.addActionListener(ev -> deleteScript(scriptNode));
            menu.add(deleteItem);
        } else {
            JMenuItem newScriptItem = new JMenuItem("New Script Here");
            final String folderPath = getFolderPathForNode(scriptNode, rootNode);
            newScriptItem.addActionListener(ev -> createNewScript(folderPath));
            menu.add(newScriptItem);

            JMenuItem newFolderItem = new JMenuItem("New Subfolder");
            newFolderItem.addActionListener(ev -> createNewFolder());
            menu.add(newFolderItem);

            if (scriptNode != rootNode) {
                menu.addSeparator();
                JMenuItem renameFolderItem = new JMenuItem("Rename...");
                renameFolderItem.addActionListener(ev -> renameFolder(scriptNode));
                menu.add(renameFolderItem);
            }
        }

        ctx.stylePopupMenu(menu);
        menu.show(scriptTree, e.getX(), e.getY());
    }

    /**
     * Variant of {@link #showContextMenu(MouseEvent)} that wires the Export
     * menu item to the supplied handler. Used by Python3IDE since
     * import/export logic still lives there via ScriptImportExportManager.
     */
    public void showContextMenu(MouseEvent e, java.util.function.Consumer<ScriptTreeNode> exportHandler) {
        JTree scriptTree = ctx.getScriptTree();
        ScriptTreeNode rootNode = ctx.getRootNode();

        TreePath path = scriptTree.getPathForLocation(e.getX(), e.getY());
        if (path == null) {
            return;
        }

        scriptTree.setSelectionPath(path);
        Object node = path.getLastPathComponent();
        if (!(node instanceof ScriptTreeNode)) {
            return;
        }

        ScriptTreeNode scriptNode = (ScriptTreeNode) node;
        JPopupMenu menu = new JPopupMenu();

        if (scriptNode.isScript()) {
            JMenuItem loadItem = new JMenuItem("Load");
            loadItem.addActionListener(ev -> loadSelectedScript());
            menu.add(loadItem);

            JMenuItem exportItem = new JMenuItem("Export...");
            exportItem.addActionListener(ev -> exportHandler.accept(scriptNode));
            menu.add(exportItem);

            JMenuItem renameItem = new JMenuItem("Rename...");
            renameItem.addActionListener(ev -> renameScript(scriptNode));
            menu.add(renameItem);

            JMenuItem editMetadataItem = new JMenuItem("Edit Metadata...");
            editMetadataItem.addActionListener(ev -> editMetadata(scriptNode));
            menu.add(editMetadataItem);

            JMenuItem moveItem = new JMenuItem("Move to Folder...");
            moveItem.addActionListener(ev -> showMoveToFolderDialog(scriptNode));
            menu.add(moveItem);

            menu.addSeparator();

            JMenuItem deleteItem = new JMenuItem("Delete");
            deleteItem.addActionListener(ev -> deleteScript(scriptNode));
            menu.add(deleteItem);
        } else {
            JMenuItem newScriptItem = new JMenuItem("New Script Here");
            final String folderPath = getFolderPathForNode(scriptNode, rootNode);
            newScriptItem.addActionListener(ev -> createNewScript(folderPath));
            menu.add(newScriptItem);

            JMenuItem newFolderItem = new JMenuItem("New Subfolder");
            newFolderItem.addActionListener(ev -> createNewFolder());
            menu.add(newFolderItem);

            if (scriptNode != rootNode) {
                menu.addSeparator();
                JMenuItem renameFolderItem = new JMenuItem("Rename...");
                renameFolderItem.addActionListener(ev -> renameFolder(scriptNode));
                menu.add(renameFolderItem);
            }
        }

        ctx.stylePopupMenu(menu);
        menu.show(scriptTree, e.getX(), e.getY());
    }

    // ===== Rename / edit metadata / delete ===================================

    public void renameScript(ScriptTreeNode scriptNode) {
        ScriptMetadata metadata = scriptNode.getScriptMetadata();
        String oldName = metadata.getName();

        String newName = DarkDialog.showInput(ctx.getParent(),
            "Enter new name for script:", "Rename Script", oldName);

        if (newName == null || newName.trim().isEmpty()) {
            return;
        }

        final String finalNewName = newName.trim();
        if (!isValidName(finalNewName)) {
            showInvalidNameError(finalNewName);
            return;
        }
        if (finalNewName.equals(oldName)) {
            return;
        }

        Python3RestClient restClient = ctx.getRestClient();
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private SavedScript script;

            @Override
            protected Void doInBackground() throws Exception {
                script = restClient.loadScript(oldName);
                restClient.deleteScript(oldName);
                restClient.saveScript(
                    finalNewName,
                    script.getCode(),
                    script.getDescription(),
                    script.getAuthor(),
                    script.getFolderPath(),
                    script.getVersion()
                );
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ctx.setStatus("Renamed '" + oldName + "' to '" + finalNewName + "'", ModernTheme.SUCCESS);
                    refreshScriptTree();

                    ScriptMetadata current = ctx.getCurrentScript();
                    if (current != null && current.getName().equals(oldName)) {
                        ctx.setCurrentScript(new ScriptMetadata(
                            current.getId(),
                            finalNewName,
                            current.getDescription(),
                            current.getAuthor(),
                            current.getCreatedDate(),
                            current.getLastModified(),
                            current.getFolderPath(),
                            current.getVersion()
                        ));
                    }
                } catch (Exception e) {
                    logger.error("Failed to rename script", e);
                    DarkDialog.showMessage(ctx.getParent(),
                        "Failed to rename script: " + e.getMessage(), "Error");
                }
            }
        };
        worker.execute();
    }

    public void editMetadata(ScriptTreeNode scriptNode) {
        ScriptMetadata metadata = scriptNode.getScriptMetadata();
        String oldName = metadata.getName();
        boolean useDarkTheme = ctx.isUseDarkTheme();

        Window owner = SwingUtilities.getWindowAncestor(ctx.getParent());
        JDialog dialog = new JDialog(owner, "Edit Metadata", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARK : ModernTheme.LIGHT_BACKGROUND);
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARK : ModernTheme.LIGHT_BACKGROUND);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        nameLabel.setFont(ModernTheme.FONT_REGULAR);
        fieldsPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JTextField nameField = new JTextField(oldName, 30);
        themeTextField(nameField, useDarkTheme);
        fieldsPanel.add(nameField, gbc);

        // Description
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel descLabel = new JLabel("Description:");
        descLabel.setForeground(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        descLabel.setFont(ModernTheme.FONT_REGULAR);
        fieldsPanel.add(descLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JTextArea descArea = new JTextArea(metadata.getDescription() != null ? metadata.getDescription() : "", 4, 30);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARKER : ModernTheme.LIGHT_BACKGROUND_DARKER);
        descArea.setForeground(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        descArea.setCaretColor(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        descArea.setFont(ModernTheme.FONT_REGULAR);
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setPreferredSize(new Dimension(300, 80));
        descScroll.setBorder(BorderFactory.createLineBorder(
            useDarkTheme ? ModernTheme.BORDER_DEFAULT : ModernTheme.LIGHT_BORDER, 1));
        fieldsPanel.add(descScroll, gbc);

        // Author
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        JLabel authorLabel = new JLabel("Author:");
        authorLabel.setForeground(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        authorLabel.setFont(ModernTheme.FONT_REGULAR);
        fieldsPanel.add(authorLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JTextField authorField = new JTextField(metadata.getAuthor() != null ? metadata.getAuthor() : "", 30);
        themeTextField(authorField, useDarkTheme);
        fieldsPanel.add(authorField, gbc);

        // Version
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        JLabel versionLabel = new JLabel("Version:");
        versionLabel.setForeground(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        versionLabel.setFont(ModernTheme.FONT_REGULAR);
        fieldsPanel.add(versionLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JTextField versionField = new JTextField(metadata.getVersion() != null ? metadata.getVersion() : "1.0", 30);
        themeTextField(versionField, useDarkTheme);
        fieldsPanel.add(versionField, gbc);

        contentPanel.add(fieldsPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARK : ModernTheme.LIGHT_BACKGROUND);

        final boolean[] okClicked = {false};

        JButton okButton = Python3IDETheme.createThemedDialogButton("OK", useDarkTheme);
        okButton.addActionListener(ev -> {
            okClicked[0] = true;
            dialog.dispose();
        });
        JButton cancelButton = Python3IDETheme.createThemedDialogButton("Cancel", useDarkTheme);
        cancelButton.addActionListener(ev -> dialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(ctx.getParent());
        dialog.setVisible(true);

        if (!okClicked[0]) {
            return;
        }

        String newName = nameField.getText().trim();
        String newDescription = descArea.getText().trim();
        String newAuthor = authorField.getText().trim();
        String newVersion = versionField.getText().trim();

        if (newName.isEmpty()) {
            DarkDialog.showMessage(ctx.getParent(), "Name cannot be empty", "Error");
            return;
        }
        if (!isValidName(newName)) {
            showInvalidNameError(newName);
            return;
        }

        Python3RestClient restClient = ctx.getRestClient();
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private SavedScript script;

            @Override
            protected Void doInBackground() throws Exception {
                script = restClient.loadScript(oldName);
                if (!newName.equals(oldName)) {
                    restClient.deleteScript(oldName);
                }
                restClient.saveScript(
                    newName,
                    script.getCode(),
                    newDescription,
                    newAuthor,
                    script.getFolderPath(),
                    newVersion
                );
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ctx.setStatus("Updated metadata for '" + newName + "'", ModernTheme.SUCCESS);
                    refreshScriptTree();

                    ScriptMetadata current = ctx.getCurrentScript();
                    if (current != null && current.getName().equals(oldName)) {
                        ScriptMetadata updated = new ScriptMetadata(
                            current.getId(),
                            newName,
                            newDescription,
                            newAuthor,
                            current.getCreatedDate(),
                            current.getLastModified(),
                            current.getFolderPath(),
                            newVersion
                        );
                        ctx.setCurrentScript(updated);
                        ScriptMetadataPanel mp = ctx.getMetadataPanel();
                        if (mp != null) {
                            mp.displayMetadata(updated);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to update metadata", e);
                    DarkDialog.showMessage(ctx.getParent(),
                        "Failed to update metadata: " + e.getMessage(), "Error");
                }
            }
        };
        worker.execute();
    }

    private static void themeTextField(JTextField field, boolean useDarkTheme) {
        field.setBackground(useDarkTheme ? ModernTheme.BACKGROUND_DARKER : ModernTheme.LIGHT_BACKGROUND_DARKER);
        field.setForeground(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        field.setCaretColor(useDarkTheme ? ModernTheme.FOREGROUND_PRIMARY : Color.BLACK);
        field.setFont(ModernTheme.FONT_REGULAR);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(
                useDarkTheme ? ModernTheme.BORDER_DEFAULT : ModernTheme.LIGHT_BORDER, 1),
            new EmptyBorder(5, 5, 5, 5)
        ));
    }

    public void renameFolder(ScriptTreeNode folderNode) {
        ScriptTreeNode rootNode = ctx.getRootNode();
        String oldName = folderNode.toString();
        String oldPath = getFolderPathForNode(folderNode, rootNode);

        String newName = DarkDialog.showInput(ctx.getParent(),
            "Enter new name for folder:", "Rename Folder", oldName);

        if (newName == null || newName.trim().isEmpty()) {
            return;
        }

        final String finalNewName = newName.trim();
        if (!isValidName(finalNewName)) {
            showInvalidNameError(finalNewName);
            return;
        }
        if (finalNewName.equals(oldName)) {
            return;
        }

        String parentPath = "";
        if (folderNode.getParent() != rootNode && folderNode.getParent() != null) {
            parentPath = getFolderPathForNode((ScriptTreeNode) folderNode.getParent(), rootNode);
        }
        final String newPath = parentPath.isEmpty() ? finalNewName : parentPath + "/" + finalNewName;

        Python3RestClient restClient = ctx.getRestClient();
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<ScriptMetadata> allScripts = restClient.listScripts();
                for (ScriptMetadata script : allScripts) {
                    String scriptFolder = script.getFolderPath();
                    if (scriptFolder != null) {
                        if (scriptFolder.equals(oldPath) || scriptFolder.startsWith(oldPath + "/")) {
                            String updatedPath = scriptFolder.equals(oldPath)
                                ? newPath
                                : newPath + scriptFolder.substring(oldPath.length());

                            SavedScript fullScript = restClient.loadScript(script.getName());
                            restClient.saveScript(
                                fullScript.getName(),
                                fullScript.getCode(),
                                fullScript.getDescription(),
                                fullScript.getAuthor(),
                                updatedPath,
                                fullScript.getVersion()
                            );
                        }
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ctx.setStatus("Renamed folder '" + oldName + "' to '" + finalNewName + "'",
                        ModernTheme.SUCCESS);
                    refreshScriptTree();
                } catch (Exception e) {
                    logger.error("Failed to rename folder", e);
                    DarkDialog.showMessage(ctx.getParent(),
                        "Failed to rename folder: " + e.getMessage(), "Error");
                }
            }
        };
        worker.execute();
    }

    /**
     * Returns the slash-separated folder path for the given folder node, or
     * empty string if it is the root.
     */
    public static String getFolderPathForNode(ScriptTreeNode folderNode, ScriptTreeNode rootNode) {
        if (folderNode == rootNode) {
            return "";
        }

        StringBuilder path = new StringBuilder();
        Object[] pathArray = folderNode.getPath();

        for (int i = 1; i < pathArray.length; i++) {
            String nodeName = pathArray[i].toString();
            if (path.length() > 0) {
                path.append("/");
            }
            path.append(nodeName);
        }
        return path.toString();
    }

    public void showMoveToFolderDialog(ScriptTreeNode scriptNode) {
        ScriptMetadata metadata = scriptNode.getScriptMetadata();
        String scriptName = metadata.getName();
        String currentFolderPath = metadata.getFolderPath() != null ? metadata.getFolderPath() : "";

        List<String> folders = new ArrayList<>();
        folders.add("[Root]");
        collectFolderPaths(ctx.getRootNode(), "", folders);

        if (folders.size() == 1) {
            DarkDialog.showMessage(ctx.getParent(),
                "No other folders available. Create folders first.", "Move to Folder");
            return;
        }

        JComboBox<String> folderCombo = new JComboBox<>(folders.toArray(new String[0]));
        folderCombo.setFont(ModernTheme.FONT_REGULAR);
        folderCombo.setBackground(ModernTheme.BACKGROUND_DARKER);
        folderCombo.setForeground(ModernTheme.FOREGROUND_PRIMARY);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(ModernTheme.BACKGROUND_DARKER);
        JLabel label = new JLabel("Select destination folder for '" + scriptName + "':");
        label.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        label.setFont(ModernTheme.FONT_REGULAR);
        panel.add(label, BorderLayout.NORTH);
        panel.add(folderCombo, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
            ctx.getParent(), panel, "Move to Folder",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String selected = (String) folderCombo.getSelectedItem();
        if (selected == null) {
            return;
        }

        String newFolderPath = selected.equals("[Root]") ? "" : selected;
        if (newFolderPath.equals(currentFolderPath)) {
            return;
        }

        moveScriptToFolder(scriptName, newFolderPath);
    }

    /**
     * Recursively collects all folder paths from the tree below {@code node}.
     */
    public static void collectFolderPaths(ScriptTreeNode node, String currentPath, List<String> folders) {
        for (int i = 0; i < node.getChildCount(); i++) {
            ScriptTreeNode child = (ScriptTreeNode) node.getChildAt(i);
            if (!child.isScript()) {
                String childPath = currentPath.isEmpty()
                    ? child.toString()
                    : currentPath + "/" + child.toString();
                folders.add(childPath);
                collectFolderPaths(child, childPath, folders);
            }
        }
    }

    public void moveScriptToFolder(String scriptName, String newFolderPath) {
        Python3RestClient restClient = ctx.getRestClient();
        if (restClient == null) {
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private SavedScript script;

            @Override
            protected Void doInBackground() throws Exception {
                script = restClient.loadScript(scriptName);
                restClient.saveScript(
                    script.getName(),
                    script.getCode(),
                    script.getDescription(),
                    script.getAuthor(),
                    newFolderPath,
                    script.getVersion()
                );
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ctx.setStatus("Moved '" + scriptName + "' to "
                        + (newFolderPath.isEmpty() ? "root" : newFolderPath), ModernTheme.SUCCESS);
                    refreshScriptTree();
                } catch (Exception e) {
                    logger.error("Failed to move script", e);
                    DarkDialog.showMessage(ctx.getParent(),
                        "Failed to move script: " + e.getMessage(), "Error");
                }
            }
        };
        worker.execute();
    }

    public void deleteScript(ScriptTreeNode scriptNode) {
        ScriptMetadata metadata = scriptNode.getScriptMetadata();

        boolean confirm = DarkDialog.showConfirm(ctx.getParent(),
            "Are you sure you want to delete '" + metadata.getName() + "'?", "Confirm Delete");

        if (!confirm) {
            return;
        }

        Python3RestClient restClient = ctx.getRestClient();
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                restClient.deleteScript(metadata.getName());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ctx.setStatus("Deleted: " + metadata.getName(), ModernTheme.SUCCESS);
                    refreshScriptTree();
                } catch (Exception e) {
                    logger.error("Failed to delete script", e);
                    DarkDialog.showMessage(ctx.getParent(),
                        "Failed to delete script: " + e.getMessage(), "Error");
                }
            }
        };
        worker.execute();
    }

    public int showUnsavedChangesDialog() {
        return JOptionPane.showConfirmDialog(
            ctx.getParent(),
            "You have unsaved changes. Do you want to save them?",
            "Unsaved Changes",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
    }
}
