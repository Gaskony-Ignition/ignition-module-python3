package com.inductiveautomation.ignition.examples.python3.designer.managers;

import com.inductiveautomation.ignition.examples.python3.designer.DarkDialog;
import com.inductiveautomation.ignition.examples.python3.designer.Python3RestClient;
import com.inductiveautomation.ignition.examples.python3.designer.SavedScript;
import com.inductiveautomation.ignition.examples.python3.designer.ScriptMetadata;
import com.inductiveautomation.ignition.examples.python3.designer.ScriptTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.List;

/**
 * Manages script and folder transfer operations (drag-and-drop) for the Python 3 IDE.
 * Handles moving scripts and folders within the tree structure, including:
 * - Single script moves
 * - Folder moves (recursive, updates all contained scripts)
 * - Path validation (prevent circular moves)
 * - Gateway synchronization
 *
 * @since v2.8.0
 */
public class ScriptTransferManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptTransferManager.class);

    private final TransferContext context;
    private final ScriptTreeNode rootNode;

    /**
     * Interface to provide context for transfer operations.
     */
    public interface TransferContext {
        /**
         * Gets the REST client for Gateway communication.
         *
         * @return REST client, or null if not connected
         */
        Python3RestClient getRestClient();

        /**
         * Gets the root node of the script tree.
         *
         * @return root tree node
         */
        ScriptTreeNode getRootNode();

        /**
         * Refreshes the script tree after transfer.
         */
        void refreshScriptTree();

        /**
         * Sets the status bar message.
         *
         * @param message status message
         * @param color message color
         */
        void setStatus(String message, Color color);

        /**
         * Gets the parent component for dialogs.
         *
         * @return parent component
         */
        Component getParentComponent();
    }

    /**
     * Creates a new ScriptTransferManager.
     *
     * @param context context provider for transfer operations
     * @param rootNode root node of the script tree
     */
    public ScriptTransferManager(TransferContext context, ScriptTreeNode rootNode) {
        this.context = context;
        this.rootNode = rootNode;
    }

    /**
     * Creates a TransferHandler for the script tree.
     *
     * @return configured TransferHandler
     */
    public TransferHandler createTransferHandler() {
        return new ScriptTreeTransferHandler();
    }

    /**
     * Transfer handler for drag and drop in the script tree.
     */
    private class ScriptTreeTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            if (c instanceof JTree) {
                JTree tree = (JTree) c;
                TreePath path = tree.getSelectionPath();
                if (path != null) {
                    Object node = path.getLastPathComponent();
                    if (node instanceof ScriptTreeNode) {
                        ScriptTreeNode scriptNode = (ScriptTreeNode) node;
                        // Allow dragging scripts and folders (but not root)
                        if (scriptNode.isScript()) {
                            return new StringSelection("SCRIPT:" + scriptNode.getScriptMetadata().getName());
                        } else if (scriptNode.isFolder() && scriptNode != rootNode) {
                            return new StringSelection("FOLDER:" + getFolderPathForNode(scriptNode));
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop()) {
                return false;
            }

            // Check if we're dropping on a folder
            JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
            TreePath path = dropLocation.getPath();

            if (path == null) {
                return false;
            }

            Object targetNode = path.getLastPathComponent();
            if (!(targetNode instanceof ScriptTreeNode)) {
                return false;
            }

            ScriptTreeNode target = (ScriptTreeNode) targetNode;
            // Can only drop on folders or root
            return target.isFolder() || target == rootNode;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            try {
                // Get the data being dragged
                String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);

                // Get the target folder
                JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
                TreePath path = dropLocation.getPath();
                ScriptTreeNode targetFolder = (ScriptTreeNode) path.getLastPathComponent();

                // Calculate new folder path
                String newFolderPath;
                if (targetFolder == rootNode) {
                    newFolderPath = "";  // Root level
                } else {
                    newFolderPath = getFolderPath(targetFolder);
                }

                // Determine if we're moving a script or folder
                if (data.startsWith("SCRIPT:")) {
                    String scriptName = data.substring(7);  // Remove "SCRIPT:" prefix
                    moveScript(scriptName, newFolderPath);
                } else if (data.startsWith("FOLDER:")) {
                    String folderPath = data.substring(7);  // Remove "FOLDER:" prefix

                    // Prevent moving folder into itself or its own subfolder
                    if (newFolderPath.equals(folderPath) || newFolderPath.startsWith(folderPath + "/")) {
                        context.setStatus("Cannot move folder into itself", Color.ORANGE);
                        return false;
                    }

                    moveFolder(folderPath, newFolderPath);
                } else {
                    // Backward compatibility - assume it's a script name without prefix
                    moveScript(data, newFolderPath);
                }

                return true;

            } catch (Exception e) {
                LOGGER.error("Failed to import", e);
                context.setStatus("Failed to move: " + e.getMessage(), Color.RED);
                return false;
            }
        }

        /**
         * Gets the full folder path for a folder node.
         */
        private String getFolderPath(ScriptTreeNode folderNode) {
            if (folderNode == rootNode) {
                return "";
            }

            StringBuilder path = new StringBuilder();
            Object[] pathArray = folderNode.getPath();

            // Skip root node (index 0)
            for (int i = 1; i < pathArray.length; i++) {
                if (path.length() > 0) {
                    path.append("/");
                }
                path.append(pathArray[i].toString());
            }

            return path.toString();
        }

        /**
         * Gets the folder path for a node (used in createTransferable).
         */
        private String getFolderPathForNode(ScriptTreeNode node) {
            return getFolderPath(node);
        }

        /**
         * Moves a script to a new folder.
         */
        private void moveScript(String scriptName, String newFolderPath) {
            Python3RestClient restClient = context.getRestClient();
            if (restClient == null) {
                return;
            }

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                private SavedScript script;

                @Override
                protected Void doInBackground() throws Exception {
                    // Load the script
                    script = restClient.loadScript(scriptName);

                    // Update folder path
                    restClient.saveScript(
                        script.getName(),
                        script.getCode(),
                        script.getDescription(),
                        script.getAuthor(),
                        newFolderPath,  // New folder path
                        script.getVersion()
                    );

                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        context.setStatus("Moved '" + scriptName + "' to " +
                            (newFolderPath.isEmpty() ? "root" : newFolderPath), new Color(0, 128, 0));
                        context.refreshScriptTree();
                    } catch (Exception e) {
                        LOGGER.error("Failed to move script", e);
                        DarkDialog.showMessage(
                            context.getParentComponent(),
                            "Failed to move script: " + e.getMessage(),
                            "Error"
                        );
                    }
                }
            };

            worker.execute();
        }

        /**
         * Moves a folder to a new parent folder.
         */
        private void moveFolder(String oldFolderPath, String newParentPath) {
            Python3RestClient restClient = context.getRestClient();
            if (restClient == null) {
                return;
            }

            // Calculate the new folder path
            // Extract the folder name from the old path
            String folderName;
            int lastSlash = oldFolderPath.lastIndexOf('/');
            if (lastSlash >= 0) {
                folderName = oldFolderPath.substring(lastSlash + 1);
            } else {
                folderName = oldFolderPath;
            }

            // Combine with new parent path
            final String newFolderPath = newParentPath.isEmpty() ? folderName : newParentPath + "/" + folderName;

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    // Get all scripts
                    List<ScriptMetadata> allScripts = restClient.listScripts();

                    // Find scripts that need to be moved
                    for (ScriptMetadata script : allScripts) {
                        String scriptFolder = script.getFolderPath();
                        if (scriptFolder != null) {
                            // Check if script is in this folder or subfolder
                            if (scriptFolder.equals(oldFolderPath) || scriptFolder.startsWith(oldFolderPath + "/")) {
                                // Update the folder path
                                String updatedPath = scriptFolder.equals(oldFolderPath) ?
                                    newFolderPath :
                                    newFolderPath + scriptFolder.substring(oldFolderPath.length());

                                // Load full script and save with new path
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
                        context.setStatus("Moved folder '" + folderName + "' to " +
                            (newParentPath.isEmpty() ? "root" : newParentPath), new Color(0, 128, 0));
                        context.refreshScriptTree();
                    } catch (Exception e) {
                        LOGGER.error("Failed to move folder", e);
                        DarkDialog.showMessage(
                            context.getParentComponent(),
                            "Failed to move folder: " + e.getMessage(),
                            "Error"
                        );
                    }
                }
            };

            worker.execute();
        }
    }
}
