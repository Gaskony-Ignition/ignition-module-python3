package com.inductiveautomation.ignition.examples.python3.designer.ui;

import com.inductiveautomation.ignition.examples.python3.designer.ModernTheme;
import com.inductiveautomation.ignition.examples.python3.designer.ScriptMetadata;
import com.inductiveautomation.ignition.examples.python3.designer.ScriptTreeNode;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Panel containing script tree browser.
 * Simplified for v2.0 architecture.
 *
 * v2.0.0: Extracted from Python3IDE.java monolith
 */
public class ScriptTreePanel extends JPanel {

    private final JTree scriptTree;
    private final DefaultTreeModel treeModel;
    private final ScriptTreeNode rootNode;
    private Consumer<String> selectionListener;

    public ScriptTreePanel() {
        setLayout(new BorderLayout());
        setBackground(ModernTheme.BACKGROUND_DARK);

        // Create root
        rootNode = new ScriptTreeNode("Scripts");
        treeModel = new DefaultTreeModel(rootNode);

        // Create tree
        scriptTree = new JTree(treeModel);
        scriptTree.setBackground(ModernTheme.TREE_BACKGROUND);
        scriptTree.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        scriptTree.setFont(ModernTheme.FONT_REGULAR);

        // Selection listener
        scriptTree.addTreeSelectionListener(e -> {
            TreePath path = e.getNewLeadSelectionPath();
            if (path != null) {
                ScriptTreeNode node = (ScriptTreeNode) path.getLastPathComponent();
                if (node.isScript() && selectionListener != null) {
                    selectionListener.accept(node.getScriptMetadata().getName());
                }
            }
        });

        JScrollPane treeScroll = new JScrollPane(scriptTree);
        treeScroll.setBackground(ModernTheme.TREE_BACKGROUND);
        treeScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        treeScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(treeScroll, BorderLayout.CENTER);
    }

    public void refreshTree(List<ScriptMetadata> scripts) {
        rootNode.removeAllChildren();
        Map<String, ScriptTreeNode> folders = new HashMap<>();

        for (ScriptMetadata script : scripts) {
            String folderPath = script.getFolderPath();
            if (folderPath == null || folderPath.isEmpty()) {
                folderPath = "";
            }

            ScriptTreeNode parentNode;
            if (folderPath.isEmpty()) {
                parentNode = rootNode;
            } else {
                parentNode = getOrCreateFolder(folderPath, folders);
            }

            ScriptTreeNode scriptNode = new ScriptTreeNode(script);
            parentNode.add(scriptNode);
        }

        treeModel.reload();
        scriptTree.expandRow(0);
    }

    private ScriptTreeNode getOrCreateFolder(String folderPath, Map<String, ScriptTreeNode> folders) {
        if (folders.containsKey(folderPath)) {
            return folders.get(folderPath);
        }

        String[] parts = folderPath.split("/");
        ScriptTreeNode currentParent = rootNode;
        StringBuilder currentPath = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i > 0) {
                currentPath.append("/");
            }
            currentPath.append(part);

            String pathSoFar = currentPath.toString();

            if (folders.containsKey(pathSoFar)) {
                currentParent = folders.get(pathSoFar);
            } else {
                ScriptTreeNode folderNode = new ScriptTreeNode(part);
                currentParent.add(folderNode);
                folders.put(pathSoFar, folderNode);
                currentParent = folderNode;
            }
        }

        return currentParent;
    }

    public void setSelectionListener(Consumer<String> listener) {
        this.selectionListener = listener;
    }

    public String getSelectedScriptName() {
        TreePath path = scriptTree.getSelectionPath();
        if (path != null) {
            ScriptTreeNode node = (ScriptTreeNode) path.getLastPathComponent();
            if (node.isScript()) {
                return node.getScriptMetadata().getName();
            }
        }
        return null;
    }

    public JTree getTree() {
        return scriptTree;
    }
}
