package com.inductiveautomation.ignition.examples.python3.designer.managers;

import com.inductiveautomation.ignition.examples.python3.designer.ExecutionResult;
import com.inductiveautomation.ignition.examples.python3.designer.ModernTheme;
import com.inductiveautomation.ignition.examples.python3.designer.Python3ExecutionWorker;
import com.inductiveautomation.ignition.examples.python3.designer.Python3RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import java.awt.CardLayout;
import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;

/**
 * Manages code execution for the Python 3 IDE.
 * Handles both Python IDE mode and Terminal mode execution, including:
 * - Code execution via REST API
 * - Interactive shell session management
 * - Success/error handling
 * - UI state management (buttons, progress bars)
 *
 * @since v2.8.0
 */
public class ExecutionManager {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionManager.class);

    private final ExecutionContext context;
    private final JButton executeButton;
    private final JProgressBar progressBar;

    private SwingWorker<ExecutionResult, Void> currentWorker;
    private String interactiveShellSessionId;
    private final StringBuilder terminalHistory = new StringBuilder();

    /**
     * Interface to provide context for execution operations.
     * This allows the manager to access necessary IDE state without tight coupling.
     */
    public interface ExecutionContext {
        /**
         * Gets the REST client for communicating with the Gateway.
         *
         * @return REST client, or null if not connected
         */
        Python3RestClient getRestClient();

        /**
         * Gets the current code from the editor.
         *
         * @return editor text content
         */
        String getCurrentCode();

        /**
         * Checks if Terminal mode is active.
         *
         * @return true if terminal tab is selected
         */
        boolean isTerminalMode();

        /**
         * Clears the output area.
         */
        void clearOutput();

        /**
         * Sets the output area text.
         *
         * @param text output text
         */
        void setOutputText(String text);

        /**
         * Sets the error area text.
         *
         * @param text error text
         */
        void setErrorText(String text);

        /**
         * Sets the status bar message.
         *
         * @param message status message
         * @param color message color
         */
        void setStatus(String message, Color color);

        /**
         * Clears the editor (used in terminal mode after command execution).
         */
        void clearEditor();

        /**
         * Refreshes the diagnostics panel.
         */
        void refreshDiagnostics();

        /**
         * Gets the selected Python version for execution (v3.1.0).
         *
         * @return Python version string (e.g., "3.11"), or null for default
         */
        default String getPythonVersion() {
            return null;
        }
    }

    /**
     * Creates a new ExecutionManager with dependency injection.
     *
     * @param context context provider for IDE state
     * @param executeButton execute button to enable/disable
     * @param progressBar progress bar to show/hide
     */
    public ExecutionManager(
        ExecutionContext context,
        JButton executeButton,
        JProgressBar progressBar
    ) {
        this.context = context;
        this.executeButton = executeButton;
        this.progressBar = progressBar;
    }

    /**
     * Executes the current code in the editor.
     * Handles both IDE mode (Python script execution) and Terminal mode (interactive shell).
     */
    public void executeCode() {
        Python3RestClient restClient = context.getRestClient();
        if (restClient == null) {
            context.setStatus("Not connected to Gateway", Color.RED);
            return;
        }

        String code = context.getCurrentCode().trim();

        if (code.isEmpty()) {
            context.setStatus("No code to execute", Color.ORANGE);
            return;
        }

        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }

        boolean isShellMode = context.isTerminalMode();

        context.clearOutput();
        executeButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        if (isShellMode) {
            executeTerminalCommand(restClient, code);
        } else {
            executePythonScript(restClient, code);
        }
    }

    /**
     * Executes a terminal command in interactive shell mode.
     */
    private void executeTerminalCommand(Python3RestClient restClient, String code) {
        context.setStatus("Executing terminal command (interactive shell)...", Color.BLUE);

        // Create session if not exists
        if (interactiveShellSessionId == null) {
            try {
                interactiveShellSessionId = restClient.createInteractiveShellSession();
                logger.info("Created interactive shell session: {}", interactiveShellSessionId);
            } catch (IOException e) {
                handleError(e);
                return;
            }
        }

        // Execute command in interactive session
        final String sessionId = interactiveShellSessionId;
        currentWorker = new SwingWorker<ExecutionResult, Void>() {
            @Override
            protected ExecutionResult doInBackground() throws Exception {
                long startTime = System.currentTimeMillis();
                ExecutionResult result = restClient.executeInteractiveShellCommand(sessionId, code);

                // Add execution time
                long execTime = System.currentTimeMillis() - startTime;
                return new ExecutionResult(result.isSuccess(), result.getResult(), result.getError(), execTime, System.currentTimeMillis());
            }

            @Override
            protected void done() {
                try {
                    ExecutionResult result = get();

                    // Append to terminal history
                    terminalHistory.append("$ ").append(code).append("\n");
                    if (result.getResult() != null && !result.getResult().isEmpty()) {
                        terminalHistory.append(result.getResult()).append("\n");
                    }

                    // Show accumulated history
                    context.setOutputText(terminalHistory.toString());

                    executeButton.setEnabled(true);
                    progressBar.setVisible(false);

                    long time = result.getExecutionTimeMs() != null ? result.getExecutionTimeMs() : 0;
                    context.setStatus(String.format("Command executed in %d ms", time), ModernTheme.SUCCESS);

                    // Clear editor for next command
                    context.clearEditor();

                    context.refreshDiagnostics();
                } catch (Exception e) {
                    handleError(e);
                }
            }
        };

        currentWorker.execute();
    }

    /**
     * Executes a Python script in IDE mode.
     */
    private void executePythonScript(Python3RestClient restClient, String code) {
        String version = context.getPythonVersion();
        String statusMsg = version != null ?
            String.format("Executing (Python %s)...", version) : "Executing...";
        context.setStatus(statusMsg, Color.BLUE);

        currentWorker = new Python3ExecutionWorker(
            restClient,
            code,
            new HashMap<>(),
            false,  // not evaluation
            false,  // not shell mode
            version,  // v3.1.0: Python version
            this::handleSuccess,
            this::handleError
        );

        currentWorker.execute();
    }

    /**
     * Handles successful execution.
     */
    private void handleSuccess(ExecutionResult result) {
        executeButton.setEnabled(true);
        progressBar.setVisible(false);

        if (result.isSuccess()) {
            String output = result.getResult() != null ? result.getResult() : "(no output)";
            context.setOutputText(output);

            long time = result.getExecutionTimeMs() != null ? result.getExecutionTimeMs() : 0;
            context.setStatus(String.format("Execution completed in %d ms", time), ModernTheme.SUCCESS);

        } else {
            String error = result.getError() != null ? result.getError() : "Unknown error";
            context.setErrorText(error);
            context.setStatus("Execution failed", Color.RED);
        }

        context.refreshDiagnostics();
    }

    /**
     * Handles execution errors.
     */
    private void handleError(Exception error) {
        executeButton.setEnabled(true);
        progressBar.setVisible(false);

        context.setErrorText("Connection error: " + error.getMessage());
        context.setStatus("Execution failed", Color.RED);

        logger.error("Execution error", error);
    }

    /**
     * Creates an interactive shell session.
     *
     * @return session ID, or null if creation failed
     */
    public String createShellSession() {
        Python3RestClient restClient = context.getRestClient();
        if (restClient == null) {
            return null;
        }

        try {
            interactiveShellSessionId = restClient.createInteractiveShellSession();
            logger.info("Created interactive shell session: {}", interactiveShellSessionId);
            return interactiveShellSessionId;
        } catch (IOException e) {
            logger.error("Failed to create interactive shell session", e);
            return null;
        }
    }

    /**
     * Closes the current interactive shell session.
     */
    public void closeShellSession() {
        if (interactiveShellSessionId != null) {
            Python3RestClient restClient = context.getRestClient();
            if (restClient != null) {
                try {
                    restClient.closeInteractiveShellSession(interactiveShellSessionId);
                    logger.info("Closed interactive shell session");
                } catch (IOException e) {
                    logger.error("Failed to close interactive shell session", e);
                }
            }
            interactiveShellSessionId = null;
            terminalHistory.setLength(0);  // Clear history
        }
    }

    /**
     * Clears the terminal history.
     */
    public void clearTerminalHistory() {
        terminalHistory.setLength(0);
    }

    /**
     * Gets the current shell session ID.
     *
     * @return session ID, or null if no session active
     */
    public String getShellSessionId() {
        return interactiveShellSessionId;
    }

    /**
     * Checks if there is an active shell session.
     *
     * @return true if shell session exists
     */
    public boolean hasActiveShellSession() {
        return interactiveShellSessionId != null;
    }

    /**
     * Cancels any currently running execution.
     */
    public void cancelExecution() {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
            executeButton.setEnabled(true);
            progressBar.setVisible(false);
            context.setStatus("Execution cancelled", Color.ORANGE);
        }
    }
}
