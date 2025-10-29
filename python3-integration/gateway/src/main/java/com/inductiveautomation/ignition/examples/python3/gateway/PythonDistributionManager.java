package com.inductiveautomation.ignition.examples.python3.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Manages embedded Python distributions.
 * Downloads and extracts Python on first use, or uses system Python if available.
 */
public class PythonDistributionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PythonDistributionManager.class);

    // Python standalone build URLs (Python 3.11.6)
    private static final Map<String, String> DISTRIBUTION_URLS = new HashMap<>();
    static {
        DISTRIBUTION_URLS.put("windows",
                "https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.11.6+20231002-x86_64-pc-windows-msvc-shared-install_only.tar.gz");
        DISTRIBUTION_URLS.put("linux",
                "https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.11.6+20231002-x86_64-unknown-linux-gnu-install_only.tar.gz");
        DISTRIBUTION_URLS.put("macos-x64",
                "https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.11.6+20231002-x86_64-apple-darwin-install_only.tar.gz");
        DISTRIBUTION_URLS.put("macos-arm64",
                "https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.11.6+20231002-aarch64-apple-darwin-install_only.tar.gz");
    }

    private final Path moduleDataDir;
    private final Path pythonDir;
    private String pythonExecutable;
    private final boolean autoDownload;

    /**
     * Create a Python distribution manager
     *
     * @param moduleDataDir Directory for module data
     * @param autoDownload  Whether to auto-download Python if not found
     */
    public PythonDistributionManager(Path moduleDataDir, boolean autoDownload) {
        this.moduleDataDir = moduleDataDir;
        this.pythonDir = moduleDataDir.resolve("python");
        this.autoDownload = autoDownload;

        try {
            Files.createDirectories(moduleDataDir);
            Files.createDirectories(pythonDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create module directories", e);
        }
    }

    /**
     * Get Python executable path.
     * Priority for self-contained module:
     * 1. Virtual environment (if configured via system property)
     * 2. Embedded Python (if already installed)
     * 3. Download embedded Python (if autoDownload enabled)
     * 4. System Python (fallback if autoDownload disabled)
     *
     * @return Path to Python executable
     * @throws IOException if Python cannot be found or installed
     */
    public String getPythonPath() throws IOException {
        // Priority 1: Check for virtual environment via system property
        String venvPath = detectVirtualEnv();
        if (venvPath != null) {
            LOGGER.info("Using virtual environment Python: {}", venvPath);
            return venvPath;
        }

        // Priority 2: Check if embedded Python already extracted
        if (isEmbeddedPythonInstalled()) {
            LOGGER.info("Using embedded Python: {}", pythonExecutable);
            return pythonExecutable;
        }

        // Priority 3: Download if enabled (prioritize self-contained distribution)
        if (autoDownload) {
            LOGGER.info("Embedded Python not found, downloading distribution...");
            downloadAndInstall();
            return pythonExecutable;
        }

        // Priority 4: Try system Python as fallback (only when autoDownload disabled)
        String systemPython = detectSystemPython();
        if (systemPython != null) {
            LOGGER.info("Using system Python: {}", systemPython);
            return systemPython;
        }

        throw new IOException(
                "Python 3 not found. Please install Python 3.8+ or enable auto-download.\n"
                        + "Set system property: -Dignition.python3.autodownload=true"
        );
    }

    /**
     * Detect virtual environment via system property.
     * Supports two configuration methods:
     * 1. Direct venv path: -Dignition.python3.venv=/path/to/venv
     * 2. Direct Python path pointing to venv: -Dignition.python3.path=/path/to/venv/bin/python3
     *
     * @return Path to Python executable in venv, or null if not configured
     */
    private String detectVirtualEnv() {
        // Method 1: Explicit venv directory
        String venvDir = System.getProperty("ignition.python3.venv");
        if (venvDir != null && !venvDir.isEmpty()) {
            String os = detectOS();
            Path venvPath;

            if ("windows".equals(os)) {
                venvPath = Path.of(venvDir, "Scripts", "python.exe");
            } else {
                venvPath = Path.of(venvDir, "bin", "python3");
            }

            if (Files.exists(venvPath) && Files.isExecutable(venvPath)) {
                String pythonPath = venvPath.toString();
                if (isPythonValid(pythonPath)) {
                    LOGGER.info("Virtual environment detected: {}", venvDir);
                    return pythonPath;
                } else {
                    LOGGER.warn("Virtual environment Python is invalid: {}", pythonPath);
                }
            } else {
                LOGGER.warn("Virtual environment not found at: {}", venvPath);
            }
        }

        // Method 2: Python path pointing to venv (check if it's in a venv)
        String pythonPath = System.getProperty("ignition.python3.path");
        if (pythonPath != null && !pythonPath.isEmpty()) {
            Path path = Path.of(pythonPath);

            // Check if this Python is inside a virtual environment
            // Typical venv structure: venv/bin/python3 or venv/Scripts/python.exe
            if (path.getParent() != null) {
                Path parentDir = path.getParent();
                String parentName = parentDir.getFileName().toString();

                if ("bin".equals(parentName) || "Scripts".equals(parentName)) {
                    Path possibleVenvRoot = parentDir.getParent();

                    // Verify venv markers exist
                    if (possibleVenvRoot != null) {
                        Path pyvenvCfg = possibleVenvRoot.resolve("pyvenv.cfg");
                        if (Files.exists(pyvenvCfg)) {
                            LOGGER.info("Virtual environment detected via python3.path: {}", possibleVenvRoot);
                            if (isPythonValid(pythonPath)) {
                                return pythonPath;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Check if embedded Python is already installed
     */
    private boolean isEmbeddedPythonInstalled() {
        String os = detectOS();
        Path executable;

        if ("windows".equals(os)) {
            executable = pythonDir.resolve("python/python.exe");
        } else {
            // Try python3.11 first (standalone builds have broken python3 symlink)
            executable = pythonDir.resolve("python/bin/python3.11");
            if (!Files.exists(executable) || !Files.isExecutable(executable)) {
                // Fallback to python3
                executable = pythonDir.resolve("python/bin/python3");
            }
        }

        if (Files.exists(executable) && Files.isExecutable(executable)) {
            pythonExecutable = executable.toString();
            return true;
        }

        return false;
    }

    /**
     * Detect system Python installation
     */
    private String detectSystemPython() {
        String os = detectOS();
        String[] candidates;

        if ("windows".equals(os)) {
            candidates = new String[]{
                    "python3",
                    "python",
                    "C:\\Python311\\python.exe",
                    "C:\\Python310\\python.exe",
                    "C:\\Python39\\python.exe"
            };
        } else if (os.startsWith("macos")) {
            candidates = new String[]{
                    "python3",
                    "/usr/local/bin/python3",
                    "/opt/homebrew/bin/python3",
                    "/usr/bin/python3"
            };
        } else {
            candidates = new String[]{
                    "python3",
                    "/usr/bin/python3",
                    "/usr/local/bin/python3"
            };
        }

        for (String candidate : candidates) {
            if (isPythonValid(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Test if a Python path is valid and version >= 3.8
     */
    private boolean isPythonValid(String pythonPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonPath, "--version");
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );
            String versionLine = reader.readLine();

            boolean exited = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);

            if (exited && process.exitValue() == 0 && versionLine != null) {
                // Parse version (e.g., "Python 3.11.6")
                if (versionLine.startsWith("Python 3.")) {
                    String[] parts = versionLine.split(" ")[1].split("\\.");
                    int minor = Integer.parseInt(parts[1]);
                    if (minor >= 8) {
                        LOGGER.debug("Valid Python found: {} ({})", pythonPath, versionLine);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Path not valid
        }

        return false;
    }

    /**
     * Download and install Python distribution
     */
    private void downloadAndInstall() throws IOException {
        String os = detectOS();
        String url = DISTRIBUTION_URLS.get(os);

        if (url == null) {
            throw new IOException("No Python distribution available for OS: " + os);
        }

        LOGGER.info("Downloading Python distribution for {}", os);
        LOGGER.info("URL: {}", url);

        // Download to temp file
        Path downloadPath = Files.createTempFile("python", ".tar.gz");

        try {
            downloadFile(url, downloadPath);
            LOGGER.info("Download complete, extracting...");

            // Extract
            extractTarGz(downloadPath, pythonDir);

            LOGGER.info("Python distribution installed successfully");

            // Verify installation
            if (!isEmbeddedPythonInstalled()) {
                throw new IOException("Python extraction succeeded but executable not found");
            }

        } finally {
            // Clean up download
            try {
                Files.deleteIfExists(downloadPath);
            } catch (IOException e) {
                LOGGER.warn("Failed to delete temp file: {}", downloadPath);
            }
        }
    }

    /**
     * Download a file with progress logging
     */
    private void downloadFile(String urlString, Path destination) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        long fileSize = conn.getContentLengthLong();
        LOGGER.info("Download size: {} MB", fileSize / 1024 / 1024);

        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             OutputStream out = new BufferedOutputStream(Files.newOutputStream(destination))) {

            byte[] buffer = new byte[8192];
            long totalRead = 0;
            int bytesRead;
            long lastLog = System.currentTimeMillis();

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;

                // Log progress every 5 seconds
                long now = System.currentTimeMillis();
                if (now - lastLog > 5000) {
                    double progress = (totalRead * 100.0) / fileSize;
                    LOGGER.info("Download progress: {}/{} MB ({:.1f}%)",
                            totalRead / 1024 / 1024,
                            fileSize / 1024 / 1024,
                            progress);
                    lastLog = now;
                }
            }

            LOGGER.info("Download complete: {} MB", totalRead / 1024 / 1024);
        }
    }

    /**
     * Extract tar.gz file
     */
    private void extractTarGz(Path tarGzPath, Path destDir) throws IOException {
        LOGGER.info("Extracting to: {}", destDir);

        try (InputStream fileIn = Files.newInputStream(tarGzPath);
             GZIPInputStream gzIn = new GZIPInputStream(fileIn);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(gzIn)) {

            TarArchiveEntry entry;
            int extractedFiles = 0;

            while ((entry = tarIn.getNextTarEntry()) != null) {
                Path outputPath = destDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                } else {
                    // Ensure parent directory exists
                    Files.createDirectories(outputPath.getParent());

                    // Extract file
                    try (OutputStream out = Files.newOutputStream(outputPath)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = tarIn.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }

                    // Set executable permissions for Unix systems
                    if (entry.getName().contains("/bin/") || entry.getName().endsWith(".so")) {
                        try {
                            outputPath.toFile().setExecutable(true);
                        } catch (Exception e) {
                            // Ignore permission errors on Windows
                        }
                    }
                }

                extractedFiles++;
                if (extractedFiles % 1000 == 0) {
                    LOGGER.debug("Extracted {} files...", extractedFiles);
                }
            }

            LOGGER.info("Extraction complete: {} files", extractedFiles);
        }
    }

    /**
     * Detect operating system
     */
    private String detectOS() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        if (os.contains("win")) {
            return "windows";
        } else if (os.contains("mac")) {
            if (arch.contains("aarch64") || arch.contains("arm")) {
                return "macos-arm64";
            } else {
                return "macos-x64";
            }
        } else {
            return "linux";
        }
    }

    /**
     * Get installation status
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("os", detectOS());
        status.put("embeddedInstalled", isEmbeddedPythonInstalled());
        status.put("pythonDir", pythonDir.toString());
        status.put("autoDownload", autoDownload);

        // Check for virtual environment
        String venvPath = detectVirtualEnv();
        if (venvPath != null) {
            status.put("usingVenv", true);
            status.put("venvPath", venvPath);
        } else {
            status.put("usingVenv", false);
        }

        try {
            String pythonPath = getPythonPath();
            status.put("pythonPath", pythonPath);
            status.put("available", true);
        } catch (IOException e) {
            status.put("available", false);
            status.put("error", e.getMessage());
        }

        return status;
    }

    /**
     * Get the currently configured virtual environment path, if any.
     *
     * @return Virtual environment root directory, or null if not using venv
     */
    public String getVirtualEnvPath() {
        String venvDir = System.getProperty("ignition.python3.venv");
        if (venvDir != null && !venvDir.isEmpty()) {
            Path venvPath = Path.of(venvDir);
            if (Files.exists(venvPath)) {
                return venvDir;
            }
        }

        // Check if python3.path points to a venv
        String pythonPath = System.getProperty("ignition.python3.path");
        if (pythonPath != null && !pythonPath.isEmpty()) {
            Path path = Path.of(pythonPath);
            if (path.getParent() != null) {
                Path parentDir = path.getParent();
                String parentName = parentDir.getFileName().toString();

                if ("bin".equals(parentName) || "Scripts".equals(parentName)) {
                    Path possibleVenvRoot = parentDir.getParent();
                    if (possibleVenvRoot != null) {
                        Path pyvenvCfg = possibleVenvRoot.resolve("pyvenv.cfg");
                        if (Files.exists(pyvenvCfg)) {
                            return possibleVenvRoot.toString();
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Force reinstall of embedded Python (for troubleshooting)
     */
    public void reinstall() throws IOException {
        LOGGER.info("Reinstalling embedded Python...");

        // Delete existing installation
        if (Files.exists(pythonDir)) {
            deleteDirectory(pythonDir);
        }

        Files.createDirectories(pythonDir);

        // Download and install
        downloadAndInstall();
    }

    /**
     * Delete directory recursively
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        Files.walk(directory)
                .sorted((a, b) -> -a.compareTo(b)) // Reverse order for bottom-up deletion
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        LOGGER.warn("Failed to delete: {}", path);
                    }
                });
    }
}
