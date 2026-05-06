package com.inductiveautomation.ignition.examples.python3.designer.managers;

import com.inductiveautomation.ignition.examples.python3.designer.ExecutionResult;
import com.inductiveautomation.ignition.examples.python3.designer.ModernTheme;
import com.inductiveautomation.ignition.examples.python3.designer.Python3RestClient;
import com.inductiveautomation.ignition.examples.python3.designer.TerminalPanel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.CardLayout;
import java.awt.Color;

/**
 * Owns terminal-mode behaviour for the Python 3 IDE: switching between
 * editor/terminal CardLayout views, executing shell commands via the Gateway's
 * Python subprocess, and tracking the terminal's working directory.
 *
 * <p>Extracted from {@code Python3IDE} in v3.13. Pure structural refactor.</p>
 *
 * @since v3.13
 */
public class Python3IDETerminalController {
    private static final Logger logger = LoggerFactory.getLogger(Python3IDETerminalController.class);

    public interface TerminalContext {
        TerminalPanel getTerminalPanel();
        JPanel getCenterPanel();
        RSyntaxTextArea getCodeEditor();
        JLabel getEditorTitleLabel();
        JLabel getCurrentScriptLabel();
        Python3RestClient getRestClient();
        void setStatus(String message, Color color);
        void updateCurrentScriptLabel();
    }

    private final TerminalContext ctx;
    private final ExecutionManager executionManager;

    public Python3IDETerminalController(TerminalContext ctx, ExecutionManager executionManager) {
        this.ctx = ctx;
        this.executionManager = executionManager;
    }

    /**
     * Handles execution mode change between Python Code and Terminal.
     *
     * @param isTerminalMode true for Terminal mode, false for Python IDE mode
     */
    public void onModeTabChanged(boolean isTerminalMode) {
        JPanel centerPanel = ctx.getCenterPanel();
        TerminalPanel terminalPanel = ctx.getTerminalPanel();
        JLabel editorTitleLabel = ctx.getEditorTitleLabel();
        JLabel currentScriptLabel = ctx.getCurrentScriptLabel();

        if (isTerminalMode) {
            ((CardLayout) centerPanel.getLayout()).show(centerPanel, "TERMINAL");

            if (!executionManager.hasActiveShellSession() && ctx.getRestClient() != null) {
                executionManager.createShellSession();
                updateTerminalWorkingDirectory();
            }

            terminalPanel.focusCommandInput();

            editorTitleLabel.setText("Terminal");
            currentScriptLabel.setVisible(false);

            ctx.setStatus("Terminal mode: Interactive shell (type commands and press Enter)",
                ModernTheme.ACCENT_PRIMARY);
        } else {
            ((CardLayout) centerPanel.getLayout()).show(centerPanel, "EDITOR");

            executionManager.closeShellSession();

            RSyntaxTextArea codeEditor = ctx.getCodeEditor();
            codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
            codeEditor.setBackground(ModernTheme.EDITOR_BACKGROUND);
            codeEditor.setCurrentLineHighlightColor(ModernTheme.BACKGROUND_LIGHT);
            codeEditor.setFont(ModernTheme.FONT_MONOSPACE);

            editorTitleLabel.setText("Python 3 Code Editor");
            currentScriptLabel.setVisible(true);
            ctx.updateCurrentScriptLabel();

            ctx.setStatus("Python Code mode: Write Python 3 code", ModernTheme.ACCENT_PRIMARY);
        }

        centerPanel.revalidate();
        centerPanel.repaint();
    }

    /**
     * Executes a terminal command via the Gateway's Python subprocess.
     */
    public void executeTerminalCommand(String command) {
        TerminalPanel terminalPanel = ctx.getTerminalPanel();
        Python3RestClient restClient = ctx.getRestClient();

        if (restClient == null) {
            terminalPanel.appendOutput("ERROR: Not connected to gateway");
            return;
        }

        String workingCommand = command;
        if (workingCommand.trim().startsWith("sudo ")) {
            terminalPanel.appendOutput("WARNING: sudo is not available. Command will run without elevated privileges.\n");
            workingCommand = workingCommand.trim().substring(5).trim();
        }

        if (workingCommand.trim().matches("pip3?\\s+install\\s+.*")
                && !workingCommand.contains("--break-system-packages")) {
            workingCommand = workingCommand.trim() + " --break-system-packages";
            logger.info("Auto-added --break-system-packages to pip install command");
        }

        final String finalCommand = workingCommand;

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                String escapedCommand = finalCommand.replace("'", "'\\''");

                String pythonCode = String.format(
                    "import subprocess\n"
                    + "import os\n"
                    + "os.environ['PATH'] = '/usr/local/bin:/usr/bin:/bin:/usr/local/sbin:/usr/sbin:/sbin'\n"
                    + "result = subprocess.run('%s', shell=True, capture_output=True, text=True, timeout=30)\n"
                    + "output = result.stdout\n"
                    + "if result.stderr:\n"
                    + "    output += result.stderr\n"
                    + "result = output if output else '(no output)'",
                    escapedCommand
                );

                ExecutionResult result = restClient.executeCode(pythonCode, null);
                return result.getResult();
            }

            @Override
            protected void done() {
                try {
                    String output = get();
                    if (output != null && !output.isEmpty()) {
                        if (output.toLowerCase().contains("error")) {
                            terminalPanel.appendOutput("ERROR: " + output + "\n");
                        } else {
                            terminalPanel.appendOutput(output + "\n");
                        }
                    } else {
                        terminalPanel.appendOutput("(no output)\n");
                    }

                    if (finalCommand.trim().startsWith("cd ")) {
                        updateTerminalWorkingDirectory();
                    }
                } catch (Exception e) {
                    String errorMsg = "ERROR: " + e.getMessage();
                    terminalPanel.appendOutput(errorMsg + "\n");
                    logger.error("Terminal command execution failed", e);
                }
            }
        };

        worker.execute();
    }

    /**
     * Updates the terminal prompt with current working directory by issuing a
     * pwd/cd command via the active shell session.
     */
    public void updateTerminalWorkingDirectory() {
        String sessionId = executionManager.getShellSessionId();
        Python3RestClient restClient = ctx.getRestClient();
        TerminalPanel terminalPanel = ctx.getTerminalPanel();

        if (restClient == null || sessionId == null) {
            return;
        }

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                String os = System.getProperty("os.name").toLowerCase();
                String pwdCommand = os.contains("win") ? "cd" : "pwd";
                ExecutionResult result = restClient.executeInteractiveShellCommand(sessionId, pwdCommand);
                return result.getResult();
            }

            @Override
            protected void done() {
                try {
                    String pwd = get();
                    if (pwd != null && !pwd.isEmpty()) {
                        pwd = pwd.trim();
                        if (!pwd.isEmpty()) {
                            terminalPanel.updateWorkingDirectory(pwd);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to get working directory", e);
                }
            }
        };

        worker.execute();
    }
}
