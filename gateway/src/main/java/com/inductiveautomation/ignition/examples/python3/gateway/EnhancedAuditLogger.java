package com.inductiveautomation.ignition.examples.python3.gateway;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced audit logger for Python3 executions.
 *
 * Logs every execution with:
 * - Username and user context
 * - Timestamp (ISO-8601 format)
 * - Code hash (SHA-256 for deduplication)
 * - Execution time and result
 * - Resource usage (memory, CPU time)
 * - IP address and source
 * - Structured JSON format
 *
 * Features:
 * - Asynchronous logging (non-blocking)
 * - Structured JSON for log aggregation (Splunk, ELK)
 * - Automatic log rotation
 * - Configurable retention policy
 * - SIEM export ready
 *
 * @since v2.14.0 (Phase 2 Week 5-6: Security Enhancements)
 */
public class EnhancedAuditLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedAuditLogger.class);

    private final Path auditLogDir;
    private final Gson gson;
    private final BlockingQueue<AuditEntry> logQueue;
    private final Thread logWorker;
    private final AtomicBoolean running;
    private final AtomicLong totalEntries;

    // Configuration
    private final int maxEntriesPerFile;
    private final int maxRetentionDays;
    private final boolean enableRotation;

    /**
     * Audit entry for structured logging.
     */
    public static class AuditEntry {
        public final String timestamp;
        public final String username;
        public final String userId;
        public final String sessionId;
        public final String ipAddress;
        public final String source;
        public final String sourceDetails;
        public final String codeHash;
        public final int codeLength;
        public final long executionTimeMs;
        public final boolean success;
        public final String error;
        public final long memoryUsedMB;
        public final long cpuTimeMs;
        public final String securityMode;
        public final String result;

        public AuditEntry(UserContext userContext, String codeHash, int codeLength,
                         long executionTimeMs, boolean success, String error,
                         long memoryUsedMB, long cpuTimeMs, String securityMode, String result) {
            this.timestamp = Instant.now().toString();
            this.username = userContext != null ? userContext.getUsername() : "unknown";
            this.userId = userContext != null ? userContext.getUserId() : null;
            this.sessionId = userContext != null ? userContext.getSessionId() : null;
            this.ipAddress = userContext != null ? userContext.getIpAddress() : null;
            this.source = userContext != null ? userContext.getSource().name() : "UNKNOWN";
            this.sourceDetails = userContext != null ? userContext.getSourceDetails() : null;
            this.codeHash = codeHash;
            this.codeLength = codeLength;
            this.executionTimeMs = executionTimeMs;
            this.success = success;
            this.error = error;
            this.memoryUsedMB = memoryUsedMB;
            this.cpuTimeMs = cpuTimeMs;
            this.securityMode = securityMode;
            this.result = result != null && result.length() > 200 ? result.substring(0, 200) + "..." : result;
        }
    }

    /**
     * Create enhanced audit logger with default settings.
     */
    public EnhancedAuditLogger(Path auditLogDir) {
        this(auditLogDir, 10000, 90, true);
    }

    /**
     * Create enhanced audit logger with custom settings.
     *
     * @param auditLogDir Directory for audit logs
     * @param maxEntriesPerFile Maximum entries per log file before rotation
     * @param maxRetentionDays Maximum days to retain old logs
     * @param enableRotation Enable automatic log rotation
     */
    public EnhancedAuditLogger(Path auditLogDir, int maxEntriesPerFile, int maxRetentionDays, boolean enableRotation) {
        this.auditLogDir = auditLogDir;
        this.maxEntriesPerFile = maxEntriesPerFile;
        this.maxRetentionDays = maxRetentionDays;
        this.enableRotation = enableRotation;

        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

        this.logQueue = new LinkedBlockingQueue<>(1000);
        this.running = new AtomicBoolean(true);
        this.totalEntries = new AtomicLong(0);

        // Create audit log directory
        try {
            Files.createDirectories(auditLogDir);
            LOGGER.info("Enhanced audit logger initialized: dir={}, maxPerFile={}, retentionDays={}",
                auditLogDir, maxEntriesPerFile, maxRetentionDays);
        } catch (IOException e) {
            LOGGER.error("Failed to create audit log directory: {}", auditLogDir, e);
        }

        // Start async log worker
        this.logWorker = new Thread(this::processLogQueue, "Python3-AuditLogger");
        this.logWorker.setDaemon(true);
        this.logWorker.start();

        // Schedule cleanup task (runs once per day)
        if (enableRotation) {
            startCleanupScheduler();
        }
    }

    /**
     * Log a Python execution.
     */
    public void logExecution(UserContext userContext, String code, long executionTimeMs,
                            boolean success, String error, long memoryUsedMB, long cpuTimeMs,
                            String securityMode, String result) {
        try {
            String codeHash = hashCode(code);
            int codeLength = code != null ? code.length() : 0;

            AuditEntry entry = new AuditEntry(
                userContext, codeHash, codeLength, executionTimeMs,
                success, error, memoryUsedMB, cpuTimeMs, securityMode, result
            );

            if (!logQueue.offer(entry)) {
                LOGGER.warn("Audit log queue is full, dropping entry");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to queue audit entry", e);
        }
    }

    /**
     * Process log queue asynchronously.
     */
    private void processLogQueue() {
        List<AuditEntry> currentBatch = new ArrayList<>();
        int currentFileEntryCount = 0;
        FileWriter currentWriter = null;
        String currentFileName = null;

        try {
            while (running.get() || !logQueue.isEmpty()) {
                AuditEntry entry = logQueue.poll(1, java.util.concurrent.TimeUnit.SECONDS);

                if (entry != null) {
                    // Check if we need to rotate file
                    if (currentWriter == null || (enableRotation && currentFileEntryCount >= maxEntriesPerFile)) {
                        if (currentWriter != null) {
                            currentWriter.close();
                        }

                        currentFileName = generateLogFileName();
                        File logFile = auditLogDir.resolve(currentFileName).toFile();
                        currentWriter = new FileWriter(logFile, true);
                        currentFileEntryCount = 0;

                        LOGGER.debug("Opened new audit log file: {}", currentFileName);
                    }

                    // Write entry as JSON line
                    String json = gson.toJson(entry);
                    currentWriter.write(json);
                    currentWriter.write('\n');
                    currentWriter.flush();

                    currentFileEntryCount++;
                    totalEntries.incrementAndGet();
                }
            }
        } catch (InterruptedException e) {
            LOGGER.info("Audit logger interrupted");
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            LOGGER.error("Error writing audit log", e);
        } finally {
            if (currentWriter != null) {
                try {
                    currentWriter.close();
                } catch (IOException e) {
                    LOGGER.error("Error closing audit log writer", e);
                }
            }
        }
    }

    /**
     * Generate log file name with timestamp.
     */
    private String generateLogFileName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            .withZone(ZoneId.systemDefault());
        return String.format("python3-audit-%s.jsonl", formatter.format(Instant.now()));
    }

    /**
     * Calculate SHA-256 hash of code for deduplication.
     */
    private String hashCode(String code) {
        if (code == null) {
            return "null";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes());
            return Base64.getEncoder().encodeToString(hash).substring(0, 16); // First 16 chars
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("SHA-256 algorithm not available", e);
            return "error";
        }
    }

    /**
     * Start cleanup scheduler for old log files.
     */
    private void startCleanupScheduler() {
        Thread cleanupThread = new Thread(() -> {
            while (running.get()) {
                try {
                    Thread.sleep(24 * 60 * 60 * 1000); // Once per day
                    cleanupOldLogs();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Python3-AuditCleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    /**
     * Clean up log files older than retention period.
     */
    private void cleanupOldLogs() {
        try {
            long cutoffTime = System.currentTimeMillis() - (maxRetentionDays * 24L * 60 * 60 * 1000);

            Files.list(auditLogDir)
                .filter(path -> path.toString().endsWith(".jsonl"))
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        LOGGER.info("Deleted old audit log: {}", path.getFileName());
                    } catch (IOException e) {
                        LOGGER.warn("Failed to delete old audit log: {}", path.getFileName(), e);
                    }
                });
        } catch (IOException e) {
            LOGGER.error("Error during audit log cleanup", e);
        }
    }

    /**
     * Shutdown audit logger gracefully.
     */
    public void shutdown() {
        LOGGER.info("Shutting down enhanced audit logger, processing {} remaining entries", logQueue.size());
        running.set(false);

        try {
            logWorker.join(5000); // Wait up to 5 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        LOGGER.info("Enhanced audit logger shutdown complete. Total entries logged: {}", totalEntries.get());
    }

    // Getters

    public long getTotalEntries() {
        return totalEntries.get();
    }

    public int getQueueSize() {
        return logQueue.size();
    }

    public Path getAuditLogDir() {
        return auditLogDir;
    }
}
