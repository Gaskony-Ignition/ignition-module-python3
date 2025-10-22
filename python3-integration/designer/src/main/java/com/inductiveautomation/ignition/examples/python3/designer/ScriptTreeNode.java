package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Tree node representing either a folder or a script in the script browser.
 */
public class ScriptTreeNode extends DefaultMutableTreeNode {

    private final NodeType type;
    private ScriptMetadata scriptMetadata;
    private String folderName;

    /**
     * Node type enumeration.
     */
    public enum NodeType {
        FOLDER,
        SCRIPT
    }

    /**
     * Creates a folder node.
     *
     * @param folderName the folder name
     */
    public ScriptTreeNode(String folderName) {
        super(folderName);
        this.type = NodeType.FOLDER;
        this.folderName = folderName;
        this.setAllowsChildren(true);
    }

    /**
     * Creates a script node.
     *
     * @param metadata the script metadata
     */
    public ScriptTreeNode(ScriptMetadata metadata) {
        super(metadata.getName());
        this.type = NodeType.SCRIPT;
        this.scriptMetadata = metadata;
        this.setAllowsChildren(false);
    }

    /**
     * Gets the node type.
     *
     * @return FOLDER or SCRIPT
     */
    public NodeType getType() {
        return type;
    }

    /**
     * Checks if this is a folder node.
     *
     * @return true if folder
     */
    public boolean isFolder() {
        return type == NodeType.FOLDER;
    }

    /**
     * Checks if this is a script node.
     *
     * @return true if script
     */
    public boolean isScript() {
        return type == NodeType.SCRIPT;
    }

    /**
     * Gets the script metadata (only for script nodes).
     *
     * @return script metadata, or null if this is a folder
     */
    public ScriptMetadata getScriptMetadata() {
        return scriptMetadata;
    }

    /**
     * Gets the folder name (only for folder nodes).
     *
     * @return folder name, or null if this is a script
     */
    public String getFolderName() {
        return folderName;
    }

    /**
     * Gets the display name for this node.
     *
     * @return folder name or script name
     */
    public String getDisplayName() {
        if (isFolder()) {
            return folderName;
        } else {
            return scriptMetadata.getName();
        }
    }

    /**
     * Gets the full path of this node (e.g., "Utilities/Network/API Scripts").
     *
     * @return full folder path
     */
    public String getFullPath() {
        StringBuilder path = new StringBuilder();
        Object[] pathArray = getPath();

        for (int i = 1; i < pathArray.length; i++) {  // Skip root
            ScriptTreeNode node = (ScriptTreeNode) pathArray[i];
            if (i > 1) {
                path.append("/");
            }
            path.append(node.getDisplayName());
        }

        return path.toString();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
