package com.gaskony.python3.designer.managers;

import com.inductiveautomation.ignition.client.gateway_interface.GatewayConnection;
import com.inductiveautomation.ignition.client.gateway_interface.GatewayConnectionManager;
import com.gaskony.python3.designer.DarkDialog;
import com.gaskony.python3.designer.DiagnosticsPanel;
import com.gaskony.python3.designer.ExecutionResult;
import com.gaskony.python3.designer.ModernStatusBar;
import com.gaskony.python3.designer.ModernTheme;
import com.gaskony.python3.designer.PoolStats;
import com.gaskony.python3.designer.Python3CompletionProvider;
import com.gaskony.python3.designer.Python3RestClient;
import com.gaskony.python3.designer.PythonSyntaxChecker;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Owns Gateway connection lifecycle and post-connection refresh logic for the
 * Python 3 IDE. Extracted from {@code Python3IDE} in v3.13.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Auto-detect Gateway URL from system property / env var / Designer connection</li>
 *   <li>Open the {@link Python3RestClient}, install syntax checker and Jedi
 *       auto-completion against the connected client</li>
 *   <li>Refresh diagnostics, Python version, available versions, autocomplete
 *       status, and react to pool-size changes</li>
 * </ul>
 *
 * <p>The IDE supplies an {@link ConnectionContext} so this controller can mutate
 * the same UI fields without owning them; symmetric with the other managers in
 * this package.</p>
 *
 * @since v3.13
 */
public class Python3IDEConnectionController {
    private static final Logger logger = LoggerFactory.getLogger(Python3IDEConnectionController.class);

    /**
     * UI surface the controller mutates.
     */
    public interface ConnectionContext {
        JTextField getGatewayUrlField();
        JLabel getConnectionStatusIndicator();
        ModernStatusBar getStatusBar();
        DiagnosticsPanel getDiagnosticsPanel();
        RSyntaxTextArea getCodeEditor();
        JComboBox<String> getVersionSelector();
        JComponent getParent();

        Python3RestClient getRestClient();
        void setRestClient(Python3RestClient client);

        PythonSyntaxChecker getSyntaxChecker();
        void setSyntaxChecker(PythonSyntaxChecker checker);

        AutoCompletion getAutoCompletion();
        void setAutoCompletion(AutoCompletion ac);

        Python3CompletionProvider getCompletionProvider();
        void setCompletionProvider(Python3CompletionProvider provider);

        String getEffectiveGatewayUrl();
        void setEffectiveGatewayUrl(String url);

        void refreshScriptTree();
        void setStatus(String message, Color color);
    }

    private final ConnectionContext ctx;

    public Python3IDEConnectionController(ConnectionContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Auto-detects Gateway URL from system property / env var / Designer
     * connection, defaulting to {@code http://localhost:8088}.
     */
    public static String detectGatewayUrl() {
        try {
            String url = System.getProperty("ignition.python3.gateway.url");

            if (url == null || url.trim().isEmpty()) {
                url = System.getenv("IGNITION_GATEWAY_URL");
            }

            if (url == null || url.trim().isEmpty()) {
                try {
                    GatewayConnection gwConn = GatewayConnectionManager.getInstance();
                    if (gwConn != null) {
                        String webUrl = gwConn.getGatewayWebURL();
                        if (webUrl != null && !webUrl.trim().isEmpty()) {
                            url = webUrl.trim();
                            logger.info("Using Gateway URL from Designer connection: {}", url);
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Could not auto-detect gateway URL from Designer: {}", e.getMessage());
                }
            }

            if (url == null || url.trim().isEmpty()) {
                url = "http://localhost:8088";
            } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }

            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }

            logger.info("Gateway URL detected/defaulted to: {}", url);
            return url;
        } catch (Exception e) {
            logger.error("Failed to detect Gateway URL, using default", e);
            return "http://localhost:8088";
        }
    }

    /**
     * Connects to the Gateway using the URL currently in the URL field.
     * Initialises syntax checker and auto-completion on success; updates status
     * indicator + status bar on either outcome.
     */
    public void connectToGateway() {
        JTextField urlField = ctx.getGatewayUrlField();
        String url = urlField.getText().trim();

        if (url.isEmpty()) {
            url = ctx.getEffectiveGatewayUrl();
            urlField.setText(url);
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
            urlField.setText(url);
        }

        ctx.setEffectiveGatewayUrl(url);

        JLabel indicator = ctx.getConnectionStatusIndicator();
        ModernStatusBar statusBar = ctx.getStatusBar();
        try {
            indicator.setText("[●] Connecting...");
            indicator.setForeground(ModernTheme.WARNING);
            indicator.setToolTipText("Connecting to " + url);

            Python3RestClient newClient = new Python3RestClient(url);
            ctx.setRestClient(newClient);

            statusBar.setStatus("Connected to " + url, ModernStatusBar.MessageType.SUCCESS);
            statusBar.setConnection("Connected", ModernTheme.SUCCESS);
            statusBar.setPoolStats("Pool: Checking...", ModernTheme.INFO);

            indicator.setText("[●] Connected");
            indicator.setForeground(ModernTheme.SUCCESS);
            indicator.setToolTipText("Connected to " + url);

            DiagnosticsPanel diagnostics = ctx.getDiagnosticsPanel();
            if (diagnostics != null) {
                diagnostics.setRestClient(newClient);
            }

            logger.info("Connected to Gateway: {}", url);

            RSyntaxTextArea codeEditor = ctx.getCodeEditor();
            PythonSyntaxChecker oldChecker = ctx.getSyntaxChecker();
            if (oldChecker != null) {
                oldChecker.dispose();
                codeEditor.removeParser(oldChecker);
            }
            PythonSyntaxChecker newChecker = new PythonSyntaxChecker(codeEditor, newClient);
            ctx.setSyntaxChecker(newChecker);
            codeEditor.addParser(newChecker);
            logger.info("Real-time syntax checking enabled");

            AutoCompletion oldAc = ctx.getAutoCompletion();
            if (oldAc != null) {
                oldAc.uninstall();
            }
            Python3CompletionProvider provider = new Python3CompletionProvider(newClient);
            ctx.setCompletionProvider(provider);
            AutoCompletion ac = new AutoCompletion(provider);
            ac.setAutoActivationEnabled(true);
            ac.setAutoCompleteSingleChoices(false);
            ac.setAutoActivationDelay(500);
            ac.setShowDescWindow(true);
            ac.setTriggerKey(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK));
            ac.install(codeEditor);
            ctx.setAutoCompletion(ac);
            logger.info("Auto-completion enabled: Ctrl+Space to trigger, auto-activates on typing");

            updateAutocompleteStatus();
            refreshDiagnostics();
            refreshPythonVersion();
            refreshAvailableVersions();
            ctx.refreshScriptTree();
        } catch (Exception e) {
            statusBar.setStatus("Connection failed: " + e.getMessage(), ModernStatusBar.MessageType.ERROR);
            statusBar.setConnection("Not Connected", ModernTheme.ERROR_BRIGHT);
            statusBar.setPoolStats("Pool: Not Connected", ModernTheme.ERROR_BRIGHT);
            statusBar.setPythonVersion("Python: --");

            indicator.setText("[●] Disconnected");
            indicator.setForeground(ModernTheme.ERROR_BRIGHT);
            indicator.setToolTipText("Connection failed: " + e.getMessage());

            logger.error("Failed to connect to Gateway: {}", url, e);
        }
    }

    /**
     * Refreshes pool-stats display in the status bar and diagnostics panel.
     */
    public void refreshDiagnostics() {
        Python3RestClient restClient = ctx.getRestClient();
        if (restClient == null) {
            return;
        }

        ModernStatusBar statusBar = ctx.getStatusBar();
        SwingWorker<PoolStats, Void> worker = new SwingWorker<PoolStats, Void>() {
            @Override
            protected PoolStats doInBackground() throws Exception {
                return restClient.getPoolStats();
            }

            @Override
            protected void done() {
                try {
                    PoolStats stats = get();
                    statusBar.updatePoolStats(stats);
                } catch (Exception e) {
                    statusBar.setPoolStats("Pool: Unavailable", ModernTheme.ERROR);
                }
            }
        };
        worker.execute();

        DiagnosticsPanel diagnostics = ctx.getDiagnosticsPanel();
        if (diagnostics != null) {
            diagnostics.refreshMetrics();
        }
    }

    /**
     * Refreshes the Python version label in the status bar.
     */
    public void refreshPythonVersion() {
        logger.info("refreshPythonVersion() - START");
        Python3RestClient restClient = ctx.getRestClient();
        ModernStatusBar statusBar = ctx.getStatusBar();

        if (restClient == null) {
            logger.warn("refreshPythonVersion() - restClient is null");
            SwingUtilities.invokeLater(() -> statusBar.setPythonVersion("Python: --"));
            return;
        }

        try {
            logger.info("refreshPythonVersion() - Calling restClient.getPythonVersion()");
            String version = restClient.getPythonVersion();

            if (version == null || version.trim().isEmpty()) {
                logger.warn("refreshPythonVersion() - Received null or empty version");
                SwingUtilities.invokeLater(() -> statusBar.setPythonVersion("Python: --"));
                return;
            }

            logger.info("refreshPythonVersion() - Successfully retrieved version: {}", version);
            final String finalVersion = version;
            SwingUtilities.invokeLater(() -> {
                statusBar.setPythonVersion("Python: " + finalVersion);
                logger.info("refreshPythonVersion() - Status bar updated with: {}", finalVersion);
            });
        } catch (IOException e) {
            logger.error("refreshPythonVersion() - IOException occurred: {}", e.getMessage(), e);
            SwingUtilities.invokeLater(() -> statusBar.setPythonVersion("Python: Connection Error"));
        } catch (Exception e) {
            logger.error("refreshPythonVersion() - Unexpected exception: {}", e.getMessage(), e);
            SwingUtilities.invokeLater(() -> statusBar.setPythonVersion("Python: Error"));
        }

        logger.info("refreshPythonVersion() - END");
    }

    /**
     * Loads available Python versions from the Gateway and populates the
     * version selector.
     */
    public void refreshAvailableVersions() {
        logger.info("refreshAvailableVersions() - Loading available Python versions");
        Python3RestClient restClient = ctx.getRestClient();
        JComboBox<String> versionSelector = ctx.getVersionSelector();

        if (restClient == null) {
            logger.warn("refreshAvailableVersions() - restClient is null");
            return;
        }

        try {
            List<String> versions = restClient.getAvailableVersions();
            String defaultVersion = restClient.getDefaultPythonVersion();

            SwingUtilities.invokeLater(() -> {
                String previousSelection = (String) versionSelector.getSelectedItem();
                versionSelector.removeAllItems();

                if (versions.isEmpty()) {
                    versionSelector.addItem("default");
                    versionSelector.setEnabled(false);
                } else {
                    for (String v : versions) {
                        versionSelector.addItem(v);
                    }
                    versionSelector.setEnabled(versions.size() > 1);

                    if (previousSelection != null && versions.contains(previousSelection)) {
                        versionSelector.setSelectedItem(previousSelection);
                    } else if (defaultVersion != null && versions.contains(defaultVersion)) {
                        versionSelector.setSelectedItem(defaultVersion);
                    }
                }

                logger.info("Version selector populated with {} version(s), default: {}",
                    versions.size(), defaultVersion);
            });
        } catch (Exception e) {
            logger.warn("Failed to load available versions: {}", e.getMessage());
            SwingUtilities.invokeLater(() -> {
                versionSelector.removeAllItems();
                versionSelector.addItem("default");
                versionSelector.setEnabled(false);
            });
        }
    }

    /**
     * Updates the autocomplete status indicator in the status bar.
     */
    public void updateAutocompleteStatus() {
        Python3CompletionProvider provider = ctx.getCompletionProvider();
        ModernStatusBar statusBar = ctx.getStatusBar();
        if (provider == null) {
            statusBar.setAutocomplete("AC: --", ModernTheme.FOREGROUND_SECONDARY);
            return;
        }

        if (provider.isAvailable()) {
            statusBar.setAutocomplete("AC: Ready", ModernTheme.SUCCESS);
            statusBar.setStatus(provider.getStatusMessage(), ModernStatusBar.MessageType.SUCCESS);
        } else {
            String status = provider.getStatusMessage();
            if (status.contains("Jedi not installed")) {
                statusBar.setAutocomplete("AC: No Jedi", ModernTheme.WARNING);
                statusBar.setStatus("Autocomplete unavailable - Install Jedi: pip install jedi",
                    ModernStatusBar.MessageType.WARNING);
            } else {
                statusBar.setAutocomplete("AC: Cooldown", ModernTheme.INFO);
            }
        }
    }

    /**
     * Handles the user clicking on the pool-stats label in the status bar to
     * change pool size at runtime (1-20).
     *
     * @param onAfterChange callback invoked after a successful pool resize
     */
    public void handlePoolClicked(Consumer<Integer> onAfterChange) {
        Python3RestClient restClient = ctx.getRestClient();
        JComponent parent = ctx.getParent();

        if (restClient == null) {
            DarkDialog.showMessage(parent, "Please connect to a Gateway first", "Not Connected");
            return;
        }

        int currentSize = 3;
        try {
            PoolStats stats = restClient.getPoolStats();
            currentSize = stats.getTotalSize();
        } catch (Exception e) {
            logger.warn("Failed to get current pool size", e);
        }

        String input = DarkDialog.showInput(parent,
            "Enter new pool size (1-20):", "Adjust Pool Size", String.valueOf(currentSize));

        if (input == null || input.trim().isEmpty()) {
            return;
        }

        try {
            int newSize = Integer.parseInt(input.trim());

            if (newSize < 1 || newSize > 20) {
                DarkDialog.showMessage(parent,
                    "Pool size must be between 1 and 20", "Invalid Pool Size");
                return;
            }

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    restClient.setPoolSize(newSize);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        ctx.setStatus("Pool size changed to " + newSize, ModernTheme.SUCCESS);
                        refreshDiagnostics();
                        if (onAfterChange != null) {
                            onAfterChange.accept(newSize);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to set pool size", e);
                        DarkDialog.showMessage(parent,
                            "Failed to set pool size: " + e.getMessage(), "Error");
                    }
                }
            };
            worker.execute();
        } catch (NumberFormatException e) {
            DarkDialog.showMessage(parent, "Please enter a valid number", "Invalid Input");
        }
    }

    /**
     * Suppress an unused-import warning for ExecutionResult — referenced
     * indirectly through PythonSyntaxChecker / RestClient interactions.
     */
    @SuppressWarnings("unused")
    private static final Class<?> KEEP_EXECUTION_RESULT_REFERENCE = ExecutionResult.class;
}
