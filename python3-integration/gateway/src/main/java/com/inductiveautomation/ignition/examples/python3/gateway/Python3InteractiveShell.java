package com.inductiveautomation.ignition.examples.python3.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages persistent interactive shell sessions for Terminal mode.
 *
 * v2.5.8: Simple interactive shell implementation
 * - Maintains persistent shell process (bash/cmd.exe/powershell)
 * - Keeps streams open for interactive command execution
 * - Command history per session
 * - Session lifecycle management
 */
public class Python3InteractiveShell {

    private static final Logger LOGGER = LoggerFactory.getLogger(Python3InteractiveShell.class);

    // Session storage
    private static final ConcurrentHashMap<String, ShellSession> SESSIONS = new ConcurrentHashMap<>();

    // Session timeout (30 minutes of inactivity)
    private static final long SESSION_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30);

    /**
     * Creates a new interactive shell session.
     *
     * @return session ID
     */
    public static String createSession() {
        String sessionId = UUID.randomUUID().toString();
        ShellSession session = new ShellSession(sessionId);

        if (session.start()) {
            SESSIONS.put(sessionId, session);
            LOGGER.info("Created new shell session: {}", sessionId);
            return sessionId;
        } else {
            LOGGER.error("Failed to start shell session: {}", sessionId);
            return null;
        }
    }

    /**
     * Executes a command in an existing session.
     *
     * @param sessionId session ID
     * @param command   command to execute
     * @return shell output
     */
    public static String executeCommand(String sessionId, String command) {
        ShellSession session = SESSIONS.get(sessionId);
        if (session == null) {
            return "ERROR: Session not found. Please restart terminal.";
        }

        session.touch();  // Update last activity time
        return session.executeCommand(command);
    }

    /**
     * Closes a shell session.
     *
     * @param sessionId session ID
     */
    public static void closeSession(String sessionId) {
        ShellSession session = SESSIONS.remove(sessionId);
        if (session != null) {
            session.close();
            LOGGER.info("Closed shell session: {}", sessionId);
        }
    }

    /**
     * Closes all sessions (called during module shutdown).
     */
    public static void closeAllSessions() {
        LOGGER.info("Closing all shell sessions ({} active)", SESSIONS.size());
        for (ShellSession session : SESSIONS.values()) {
            session.close();
        }
        SESSIONS.clear();
    }

    /**
     * Cleans up inactive sessions (called periodically).
     */
    public static void cleanupInactiveSessions() {
        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();

        for (String sessionId : SESSIONS.keySet()) {
            ShellSession session = SESSIONS.get(sessionId);
            if (session != null && (now - session.getLastActivity()) > SESSION_TIMEOUT_MS) {
                toRemove.add(sessionId);
            }
        }

        for (String sessionId : toRemove) {
            LOGGER.info("Removing inactive shell session: {}", sessionId);
            closeSession(sessionId);
        }
    }

    /**
     * Individual shell session.
     */
    private static class ShellSession {
        private final String sessionId;
        private Process process;
        private BufferedWriter stdin;
        private BufferedReader stdout;
        private BufferedReader stderr;
        private long lastActivity;
        private final String shellCommand;
        private final String workingDirectory;

        public ShellSession(String sessionId) {
            this.sessionId = sessionId;
            this.lastActivity = System.currentTimeMillis();

            // Determine shell command based on OS
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                this.shellCommand = "cmd.exe";
            } else {
                this.shellCommand = "/bin/bash";
            }

            // Use user home as working directory
            this.workingDirectory = System.getProperty("user.home");
        }

        /**
         * Starts the shell process.
         */
        public boolean start() {
            try {
                ProcessBuilder pb = new ProcessBuilder(shellCommand);
                pb.directory(new File(workingDirectory));
                pb.redirectErrorStream(false);  // Keep stdout and stderr separate

                process = pb.start();

                // Setup streams
                stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
                stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                stderr = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));

                LOGGER.info("Started shell process: {} (session: {})", shellCommand, sessionId);
                return true;

            } catch (IOException e) {
                LOGGER.error("Failed to start shell process", e);
                return false;
            }
        }

        /**
         * Executes a command in the shell.
         */
        public String executeCommand(String command) {
            if (process == null || !process.isAlive()) {
                return "ERROR: Shell process is not alive. Please restart terminal.";
            }

            try {
                // Write command to shell
                stdin.write(command);
                stdin.newLine();
                stdin.flush();

                // For Windows, add extra command to mark end of output
                boolean isWindows = shellCommand.contains("cmd");
                if (isWindows) {
                    stdin.write("echo __END_OF_COMMAND__");
                    stdin.newLine();
                    stdin.flush();
                } else {
                    // For bash/sh, use marker
                    stdin.write("echo __END_OF_COMMAND__");
                    stdin.newLine();
                    stdin.flush();
                }

                // Read output until marker
                StringBuilder output = new StringBuilder();
                StringBuilder errors = new StringBuilder();

                // Read stdout (with timeout)
                long startTime = System.currentTimeMillis();
                long timeout = 10000;  // 10 second timeout

                while (System.currentTimeMillis() - startTime < timeout) {
                    if (stdout.ready()) {
                        String line = stdout.readLine();
                        if (line == null) {
                            break;
                        }
                        if (line.contains("__END_OF_COMMAND__")) {
                            break;
                        }
                        output.append(line).append("\n");
                    }

                    // Check for errors
                    if (stderr.ready()) {
                        String errLine = stderr.readLine();
                        if (errLine != null) {
                            errors.append(errLine).append("\n");
                        }
                    }

                    // Small sleep to avoid busy waiting
                    Thread.sleep(10);
                }

                // Combine output and errors
                String result = output.toString();
                if (errors.length() > 0) {
                    result += "\nERROR:\n" + errors.toString();
                }

                return result.isEmpty() ? "(no output)" : result;

            } catch (IOException | InterruptedException e) {
                LOGGER.error("Error executing command in shell session: {}", sessionId, e);
                return "ERROR: " + e.getMessage();
            }
        }

        /**
         * Updates last activity timestamp.
         */
        public void touch() {
            this.lastActivity = System.currentTimeMillis();
        }

        /**
         * Gets last activity timestamp.
         */
        public long getLastActivity() {
            return lastActivity;
        }

        /**
         * Closes the shell session.
         */
        public void close() {
            try {
                if (stdin != null) {
                    stdin.close();
                }
                if (stdout != null) {
                    stdout.close();
                }
                if (stderr != null) {
                    stderr.close();
                }
                if (process != null && process.isAlive()) {
                    process.destroy();
                    if (!process.waitFor(5, TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error closing shell session: {}", sessionId, e);
            }
        }
    }
}
