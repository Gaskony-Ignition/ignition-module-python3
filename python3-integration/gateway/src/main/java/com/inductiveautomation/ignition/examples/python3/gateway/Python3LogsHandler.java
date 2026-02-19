package com.inductiveautomation.ignition.examples.python3.gateway;

import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads Ignition gateway logs from the wrapper.log file.
 * Provides filtered, paginated log entries for the Gateway Web UI.
 *
 * @since v3.5.0
 */
public final class Python3LogsHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Python3LogsHandler.class);

    // Wrapper log line pattern: "INFO  | jvm 1 | 2024/01/15 10:30:45 | [message]"
    // Also matches: "WARN  | jvm 1 | ..." and "ERROR | jvm 1 | ..."
    private static final Pattern LOG_LINE_PATTERN = Pattern.compile(
        "^(INFO|WARN|ERROR|DEBUG|TRACE)\\s*\\|\\s*jvm\\s+\\d+\\s*\\|\\s*(\\d{4}/\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})\\s*\\|\\s*(.*)$"
    );

    private Python3LogsHandler() {
        // Static utility class
    }

    /**
     * Read log entries from the Ignition wrapper.log file.
     *
     * @param lines    Maximum number of lines to return (default 100)
     * @param level    Minimum log level filter: ALL, DEBUG, INFO, WARN, ERROR
     * @param filter   Text search filter (case-insensitive substring match)
     * @param afterLine Line number offset for pagination (0 = most recent)
     * @return JSON object with log entries
     */
    public static JsonObject readLogs(int lines, String level, String filter, long afterLine) {
        JsonObject response = new JsonObject();
        JsonArray entries = new JsonArray();

        // Find the wrapper.log file
        Path logFile = findWrapperLog();
        if (logFile == null || !Files.exists(logFile)) {
            response.addProperty("success", true);
            response.add("entries", entries);
            response.addProperty("count", 0);
            response.addProperty("warning", "wrapper.log not found");
            return response;
        }

        try {
            // Read the tail of the log file efficiently
            List<String> rawLines = readTail(logFile, lines * 3 + (int) afterLine);

            // Parse log entries
            List<JsonObject> parsed = new ArrayList<>();
            String currentLevel = null;
            String currentTimestamp = null;
            StringBuilder currentMessage = new StringBuilder();
            long lineNum = 0;

            for (String line : rawLines) {
                lineNum++;
                Matcher matcher = LOG_LINE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    // Save previous entry if exists
                    if (currentLevel != null) {
                        JsonObject entry = buildEntry(lineNum - 1, currentTimestamp, currentLevel, currentMessage.toString().trim());
                        if (matchesFilter(entry, level, filter)) {
                            parsed.add(entry);
                        }
                    }
                    currentLevel = matcher.group(1);
                    currentTimestamp = matcher.group(2);
                    currentMessage = new StringBuilder(matcher.group(3));
                } else if (currentLevel != null) {
                    // Continuation line (stacktrace, multi-line message)
                    currentMessage.append("\n").append(line);
                }
            }

            // Don't forget the last entry
            if (currentLevel != null) {
                JsonObject entry = buildEntry(lineNum, currentTimestamp, currentLevel, currentMessage.toString().trim());
                if (matchesFilter(entry, level, filter)) {
                    parsed.add(entry);
                }
            }

            // Reverse for newest-first ordering
            Collections.reverse(parsed);

            // Apply pagination
            int start = (int) Math.min(afterLine, parsed.size());
            int end = Math.min(start + lines, parsed.size());
            for (int i = start; i < end; i++) {
                entries.add(parsed.get(i));
            }

            response.addProperty("success", true);
            response.add("entries", entries);
            response.addProperty("count", entries.size());
            response.addProperty("total", parsed.size());
            response.addProperty("hasMore", end < parsed.size());

        } catch (Exception e) {
            LOGGER.error("Failed to read logs", e);
            response.addProperty("success", false);
            response.addProperty("error", "Failed to read logs: " + e.getMessage());
            response.add("entries", entries);
            response.addProperty("count", 0);
        }

        return response;
    }

    private static JsonObject buildEntry(long lineNum, String timestamp, String level, String message) {
        JsonObject entry = new JsonObject();
        entry.addProperty("id", lineNum);
        entry.addProperty("timestamp", timestamp != null ? timestamp.replace('/', '-') : "");
        entry.addProperty("level", level);
        entry.addProperty("message", message);
        return entry;
    }

    private static boolean matchesFilter(JsonObject entry, String level, String filter) {
        // Level filter
        if (level != null && !level.isEmpty() && !"ALL".equalsIgnoreCase(level)) {
            String entryLevel = entry.get("level").getAsString();
            int entryPriority = levelPriority(entryLevel);
            int filterPriority = levelPriority(level);
            if (entryPriority < filterPriority) {
                return false;
            }
        }

        // Text filter
        if (filter != null && !filter.trim().isEmpty()) {
            String msg = entry.get("message").getAsString().toLowerCase();
            return msg.contains(filter.trim().toLowerCase());
        }

        return true;
    }

    private static int levelPriority(String level) {
        switch (level.toUpperCase()) {
            case "TRACE": return 0;
            case "DEBUG": return 1;
            case "INFO":  return 2;
            case "WARN":  return 3;
            case "ERROR": return 4;
            default:      return 2;
        }
    }

    /**
     * Find the wrapper.log file path.
     * Checks common Ignition installation locations.
     */
    private static Path findWrapperLog() {
        // Standard Ignition log paths
        String[] candidates = {
            System.getProperty("ignition.install.dir", "") + "/logs/wrapper.log",
            "/usr/local/bin/ignition/logs/wrapper.log",
            "/var/log/ignition/wrapper.log",
            "C:/Program Files/Inductive Automation/Ignition/logs/wrapper.log",
        };

        // Also check relative to working directory
        Path cwd = Paths.get(System.getProperty("user.dir", "."));
        Path cwdLog = cwd.resolve("logs/wrapper.log");
        if (Files.exists(cwdLog)) {
            return cwdLog;
        }

        // Check parent directories (Ignition often runs from bin/)
        Path parentLog = cwd.getParent() != null ? cwd.getParent().resolve("logs/wrapper.log") : null;
        if (parentLog != null && Files.exists(parentLog)) {
            return parentLog;
        }

        for (String candidate : candidates) {
            if (!candidate.isEmpty()) {
                Path p = Paths.get(candidate);
                if (Files.exists(p)) {
                    return p;
                }
            }
        }

        // Fallback: try system property for Ignition home
        String ignHome = System.getProperty("ignition.home", System.getenv("IGNITION_HOME"));
        if (ignHome != null && !ignHome.isEmpty()) {
            Path p = Paths.get(ignHome, "logs", "wrapper.log");
            if (Files.exists(p)) {
                return p;
            }
        }

        LOGGER.debug("wrapper.log not found in standard locations");
        return null;
    }

    /**
     * Efficiently read the last N lines from a file using RandomAccessFile.
     */
    private static List<String> readTail(Path file, int maxLines) throws Exception {
        List<String> lines = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long fileLength = raf.length();
            if (fileLength == 0) return lines;

            // Start from end of file and work backwards
            long pos = fileLength - 1;
            int lineCount = 0;
            StringBuilder sb = new StringBuilder();

            // Read backwards to find enough lines
            // Estimate: average line ~150 chars, read enough bytes
            long startPos = Math.max(0, fileLength - (long) maxLines * 200);
            raf.seek(startPos);

            // Skip partial first line if we didn't start at beginning
            if (startPos > 0) {
                raf.readLine(); // discard partial line
            }

            String line;
            while ((line = raf.readLine()) != null && lineCount < maxLines) {
                // readLine strips newlines; handle encoding
                lines.add(new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8));
                lineCount++;
            }
        }

        return lines;
    }
}
