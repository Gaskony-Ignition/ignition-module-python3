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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Manages embedded Python distributions.
 * Supports downloading, installing, and uninstalling multiple Python versions.
 *
 * <p>Each Python version is installed into its own subdirectory under the distributions
 * directory (e.g., {@code python3-integration/distributions/3.11/}).</p>
 *
 * <p>Available versions for download are sourced from the
 * <a href="https://github.com/indygreg/python-build-standalone">python-build-standalone</a> project.</p>
 *
 * @since v3.1.0 - Multi-version support
 */
public class PythonDistributionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PythonDistributionManager.class);

    /**
     * Describes a downloadable Python version with URLs per platform.
     */
    public static class PythonDistribution {
        public final String version;         // e.g., "3.11"
        public final String fullVersion;     // e.g., "3.11.6"
        public final String releaseTag;      // e.g., "20231002"
        public final Map<String, String> platformUrls;  // os -> download URL

        public PythonDistribution(String version, String fullVersion, String releaseTag, Map<String, String> urls) {
            this.version = version;
            this.fullVersion = fullVersion;
            this.releaseTag = releaseTag;
            this.platformUrls = Collections.unmodifiableMap(urls);
        }
    }

    /**
     * Status information for an installed (or available) Python version.
     */
    public static class VersionStatus {
        public final String version;
        public final String fullVersion;
        public final boolean installed;
        public final boolean available;   // available for download
        public final String pythonPath;   // executable path if installed
        public final long installSizeBytes;

        public VersionStatus(String version, String fullVersion, boolean installed,
                             boolean available, String pythonPath, long installSizeBytes) {
            this.version = version;
            this.fullVersion = fullVersion;
            this.installed = installed;
            this.available = available;
            this.pythonPath = pythonPath;
            this.installSizeBytes = installSizeBytes;
        }
    }

    // Available Python distributions (from python-build-standalone)
    private static final Map<String, PythonDistribution> AVAILABLE_DISTRIBUTIONS = new LinkedHashMap<>();
    static {
        // Python 3.9
        Map<String, String> py39 = new HashMap<>();
        py39.put("windows", "https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.9.18+20231002-x86_64-pc-windows-msvc-shared-install_only.tar.gz");
        py39.put("linux", "https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.9.18+20231002-x86_64-unknown-linux-gnu-install_only.tar.gz");
        py39.put("macos-x64", "https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.9.18+20231002-x86_64-apple-darwin-install_only.tar.gz");
        py39.put("macos-arm64", "https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.9.18+20231002-aarch64-apple-darwin-install_only.tar.gz");
        AVAILABLE_DISTRIBUTIONS.put("3.9", new PythonDistribution("3.9", "3.9.18", "20231002", py39));

        // Python 3.10
        Map<String, String> py310 = new HashMap<>();
        py310.put("windows", "https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.10.13+20231002-x86_64-pc-windows-msvc-shared-install_only.tar.gz");
        py310.put("linux", "https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.10.13+20231002-x86_64-unknown-linux-gnu-install_only.tar.gz");
        py310.put("macos-x64", "https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.10.13+20231002-x86_64-apple-darwin-install_only.tar.gz");
        py310.put("macos-arm64", "https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.10.13+20231002-aarch64-apple-darwin-install_only.tar.gz");
        AVAILABLE_DISTRIBUTIONS.put("3.10", new PythonDistribution("3.10", "3.10.13", "20231002", py310));

        // Python 3.11
        Map<String, String> py311 = new HashMap<>();
        py311.put("windows", "https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.11.6+20231002-x86_64-pc-windows-msvc-shared-install_only.tar.gz");
        py311.put("linux", "https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.11.6+20231002-x86_64-unknown-linux-gnu-install_only.tar.gz");
        py311.put("macos-x64", "https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.11.6+20231002-x86_64-apple-darwin-install_only.tar.gz");
        py311.put("macos-arm64", "https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.11.6+20231002-aarch64-apple-darwin-install_only.tar.gz");
        AVAILABLE_DISTRIBUTIONS.put("3.11", new PythonDistribution("3.11", "3.11.6", "20231002", py311));

        // Python 3.12
        Map<String, String> py312 = new HashMap<>();
        py312.put("windows", "https://github.com/indygreg/python-build-standalone/releases/download/20240107/cpython-3.12.1+20240107-x86_64-pc-windows-msvc-shared-install_only.tar.gz");
        py312.put("linux", "https://github.com/indygreg/python-build-standalone/releases/download/20240107/cpython-3.12.1+20240107-x86_64-unknown-linux-gnu-install_only.tar.gz");
        py312.put("macos-x64", "https://github.com/indygreg/python-build-standalone/releases/download/20240107/cpython-3.12.1+20240107-x86_64-apple-darwin-install_only.tar.gz");
        py312.put("macos-arm64", "https://github.com/indygreg/python-build-standalone/releases/download/20240107/cpython-3.12.1+20240107-aarch64-apple-darwin-install_only.tar.gz");
        AVAILABLE_DISTRIBUTIONS.put("3.12", new PythonDistribution("3.12", "3.12.1", "20240107", py312));

        // Python 3.13
        Map<String, String> py313 = new HashMap<>();
        py313.put("windows", "https://github.com/indygreg/python-build-standalone/releases/download/20241016/cpython-3.13.0+20241016-x86_64-pc-windows-msvc-shared-install_only.tar.gz");
        py313.put("linux", "https://github.com/indygreg/python-build-standalone/releases/download/20241016/cpython-3.13.0+20241016-x86_64-unknown-linux-gnu-install_only.tar.gz");
        py313.put("macos-x64", "https://github.com/indygreg/python-build-standalone/releases/download/20241016/cpython-3.13.0+20241016-x86_64-apple-darwin-install_only.tar.gz");
        py313.put("macos-arm64", "https://github.com/indygreg/python-build-standalone/releases/download/20241016/cpython-3.13.0+20241016-aarch64-apple-darwin-install_only.tar.gz");
        AVAILABLE_DISTRIBUTIONS.put("3.13", new PythonDistribution("3.13", "3.13.0", "20241016", py313));
    }

    // Legacy single-version URLs (backward compatibility)
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
    private final Path pythonDir;           // Legacy single-version dir
    private final Path distributionsDir;    // Multi-version distributions dir
    private String pythonExecutable;
    private final boolean autoDownload;

    // Track installed versions
    private final Map<String, String> installedVersionPaths = new ConcurrentHashMap<>();

    // Installation progress callback
    private volatile InstallProgressListener progressListener;

    /**
     * Listener for installation progress updates.
     */
    public interface InstallProgressListener {
        void onProgress(String version, String stage, int percentComplete, String message);
        void onComplete(String version, boolean success, String message);
    }

    /**
     * Create a Python distribution manager
     *
     * @param moduleDataDir Directory for module data
     * @param autoDownload  Whether to auto-download Python if not found
     */
    public PythonDistributionManager(Path moduleDataDir, boolean autoDownload) {
        this.moduleDataDir = moduleDataDir;
        this.pythonDir = moduleDataDir.resolve("python");
        this.distributionsDir = moduleDataDir.resolve("distributions");
        this.autoDownload = autoDownload;

        try {
            Files.createDirectories(moduleDataDir);
            Files.createDirectories(pythonDir);
            Files.createDirectories(distributionsDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create module directories", e);
        }

        // Scan for already-installed distributions
        scanInstalledDistributions();
    }

    /**
     * Set a progress listener for installation updates.
     */
    public void setProgressListener(InstallProgressListener listener) {
        this.progressListener = listener;
    }

    // ========================================================================
    // Multi-version API (v3.1.0)
    // ========================================================================

    /**
     * Get all available Python versions with their install status.
     *
     * @return list of version status objects
     */
    public List<VersionStatus> getAllVersionStatuses() {
        List<VersionStatus> statuses = new ArrayList<>();
        String os = detectOS();

        for (Map.Entry<String, PythonDistribution> entry : AVAILABLE_DISTRIBUTIONS.entrySet()) {
            String version = entry.getKey();
            PythonDistribution dist = entry.getValue();

            boolean available = dist.platformUrls.containsKey(os);
            boolean installed = isVersionInstalled(version);
            String execPath = installed ? getVersionExecutablePath(version) : null;
            long size = installed ? getInstalledSize(version) : 0;

            statuses.add(new VersionStatus(version, dist.fullVersion,
                    installed, available, execPath, size));
        }

        return statuses;
    }

    /**
     * Get the list of available Python versions (that can be downloaded).
     *
     * @return sorted list of version strings
     */
    public List<String> getAvailableDownloadVersions() {
        String os = detectOS();
        List<String> versions = new ArrayList<>();

        for (Map.Entry<String, PythonDistribution> entry : AVAILABLE_DISTRIBUTIONS.entrySet()) {
            if (entry.getValue().platformUrls.containsKey(os)) {
                versions.add(entry.getKey());
            }
        }

        Collections.sort(versions);
        return versions;
    }

    /**
     * Get the list of currently installed Python versions.
     *
     * @return sorted list of installed version strings
     */
    public List<String> getInstalledVersions() {
        List<String> versions = new ArrayList<>(installedVersionPaths.keySet());
        Collections.sort(versions);
        return versions;
    }

    /**
     * Check if a specific version is installed.
     *
     * @param version the Python version (e.g., "3.11")
     * @return true if installed
     */
    public boolean isVersionInstalled(String version) {
        return installedVersionPaths.containsKey(version);
    }

    /**
     * Get the Python executable path for a specific installed version.
     *
     * @param version the Python version (e.g., "3.11")
     * @return path to executable, or null if not installed
     */
    public String getVersionExecutablePath(String version) {
        return installedVersionPaths.get(version);
    }

    /**
     * Install a specific Python version by downloading from python-build-standalone.
     *
     * @param version the Python version to install (e.g., "3.11")
     * @throws IOException if download or extraction fails
     * @throws IllegalArgumentException if version is not available
     */
    public void installVersion(String version) throws IOException {
        PythonDistribution dist = AVAILABLE_DISTRIBUTIONS.get(version);
        if (dist == null) {
            throw new IllegalArgumentException("Unknown Python version: " + version
                    + ". Available: " + AVAILABLE_DISTRIBUTIONS.keySet());
        }

        String os = detectOS();
        String url = dist.platformUrls.get(os);
        if (url == null) {
            throw new IOException("No Python " + version + " distribution available for OS: " + os);
        }

        if (isVersionInstalled(version)) {
            LOGGER.info("Python {} is already installed", version);
            return;
        }

        Path versionDir = distributionsDir.resolve(version);
        Files.createDirectories(versionDir);

        LOGGER.info("Installing Python {} ({}) for {}", version, dist.fullVersion, os);
        notifyProgress(version, "downloading", 0, "Starting download...");

        Path downloadPath = Files.createTempFile("python-" + version + "-", ".tar.gz");

        try {
            downloadFileWithProgress(url, downloadPath, version);
            notifyProgress(version, "extracting", 80, "Extracting...");

            extractTarGz(downloadPath, versionDir);
            notifyProgress(version, "verifying", 95, "Verifying installation...");

            // Find and verify the executable
            String execPath = findVersionExecutable(version, versionDir);
            if (execPath == null) {
                deleteDirectory(versionDir);
                throw new IOException("Python " + version + " extraction succeeded but executable not found");
            }

            // Validate the installation
            if (!isPythonValid(execPath)) {
                deleteDirectory(versionDir);
                throw new IOException("Python " + version + " executable is not valid: " + execPath);
            }

            installedVersionPaths.put(version, execPath);
            LOGGER.info("Python {} installed successfully at: {}", version, execPath);
            notifyProgress(version, "complete", 100, "Installation complete");
            notifyComplete(version, true, "Python " + version + " installed successfully");

        } catch (IOException e) {
            LOGGER.error("Failed to install Python {}", version, e);
            notifyComplete(version, false, "Installation failed: " + e.getMessage());
            // Clean up partial installation
            try {
                deleteDirectory(versionDir);
            } catch (IOException cleanupErr) {
                LOGGER.warn("Failed to clean up failed installation for {}", version);
            }
            throw e;
        } finally {
            try {
                Files.deleteIfExists(downloadPath);
            } catch (IOException e) {
                LOGGER.warn("Failed to delete temp file: {}", downloadPath);
            }
        }
    }

    /**
     * Uninstall a specific Python version.
     *
     * @param version the Python version to uninstall
     * @throws IOException if deletion fails
     */
    public void uninstallVersion(String version) throws IOException {
        if (!isVersionInstalled(version)) {
            LOGGER.info("Python {} is not installed, nothing to uninstall", version);
            return;
        }

        Path versionDir = distributionsDir.resolve(version);
        LOGGER.info("Uninstalling Python {} from: {}", version, versionDir);

        deleteDirectory(versionDir);
        installedVersionPaths.remove(version);

        LOGGER.info("Python {} uninstalled successfully", version);
    }

    /**
     * Get the full version string for a distribution (e.g., "3.11.6").
     *
     * @param version the short version (e.g., "3.11")
     * @return the full version string, or the short version if not found
     */
    public String getFullVersion(String version) {
        PythonDistribution dist = AVAILABLE_DISTRIBUTIONS.get(version);
        return dist != null ? dist.fullVersion : version;
    }

    /**
     * Get the installed size of a Python version in bytes.
     *
     * @param version the Python version
     * @return size in bytes, or 0 if not installed
     */
    public long getInstalledSize(String version) {
        Path versionDir = distributionsDir.resolve(version);
        if (!Files.exists(versionDir)) {
            return 0;
        }

        try {
            return Files.walk(versionDir)
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Scan for already-installed distributions on startup.
     */
    private void scanInstalledDistributions() {
        if (!Files.exists(distributionsDir)) {
            return;
        }

        try {
            Files.list(distributionsDir)
                    .filter(Files::isDirectory)
                    .forEach(versionDir -> {
                        String version = versionDir.getFileName().toString();
                        String execPath = findVersionExecutable(version, versionDir);
                        if (execPath != null && isPythonValid(execPath)) {
                            installedVersionPaths.put(version, execPath);
                            LOGGER.info("Found installed Python {}: {}", version, execPath);
                        }
                    });
        } catch (IOException e) {
            LOGGER.error("Failed to scan installed distributions", e);
        }

        LOGGER.info("Installed Python distributions: {}", installedVersionPaths.keySet());
    }

    /**
     * Find the Python executable for a specific version in its install directory.
     */
    private String findVersionExecutable(String version, Path versionDir) {
        String os = detectOS();

        // python-build-standalone extracts to a "python" subdirectory
        Path pythonSubDir = versionDir.resolve("python");
        if (!Files.exists(pythonSubDir)) {
            // Some versions may extract directly
            pythonSubDir = versionDir;
        }

        Path executable;
        if ("windows".equals(os)) {
            executable = pythonSubDir.resolve("python.exe");
        } else {
            // Try version-specific binary first (e.g., python3.11)
            PythonDistribution dist = AVAILABLE_DISTRIBUTIONS.get(version);
            if (dist != null) {
                executable = pythonSubDir.resolve("bin/python" + dist.fullVersion.substring(0, dist.fullVersion.lastIndexOf('.')));
                if (!Files.exists(executable)) {
                    executable = pythonSubDir.resolve("bin/python3." + version.split("\\.")[1]);
                }
            } else {
                executable = pythonSubDir.resolve("bin/python3");
            }

            if (!Files.exists(executable)) {
                executable = pythonSubDir.resolve("bin/python3");
            }
            if (!Files.exists(executable)) {
                executable = pythonSubDir.resolve("bin/python");
            }
        }

        if (Files.exists(executable)) {
            try {
                executable.toFile().setExecutable(true);
            } catch (Exception e) {
                // Ignore on Windows
            }
            return executable.toString();
        }

        return null;
    }

    // ========================================================================
    // Legacy single-version API (backward compatibility)
    // ========================================================================

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

        // Priority 2: Check installed distributions (multi-version)
        if (!installedVersionPaths.isEmpty()) {
            // Prefer 3.11 if available, otherwise use the latest installed
            String preferred = installedVersionPaths.get("3.11");
            if (preferred != null) {
                LOGGER.info("Using installed distribution Python 3.11: {}", preferred);
                return preferred;
            }
            // Use highest version
            List<String> versions = new ArrayList<>(installedVersionPaths.keySet());
            Collections.sort(versions);
            String latest = versions.get(versions.size() - 1);
            String latestPath = installedVersionPaths.get(latest);
            LOGGER.info("Using installed distribution Python {}: {}", latest, latestPath);
            return latestPath;
        }

        // Priority 3: Check if embedded Python already extracted (legacy path)
        if (isEmbeddedPythonInstalled()) {
            LOGGER.info("Using embedded Python: {}", pythonExecutable);
            return pythonExecutable;
        }

        // Priority 4: Download if enabled (prioritize self-contained distribution)
        if (autoDownload) {
            LOGGER.info("Embedded Python not found, downloading distribution...");
            downloadAndInstall();
            return pythonExecutable;
        }

        // Priority 5: Try system Python as fallback (only when autoDownload disabled)
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
     * Check if embedded Python is already installed (legacy single-version)
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
    boolean isPythonValid(String pythonPath) {
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
     * Download and install Python distribution (legacy single-version)
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
     * Download a file with progress callbacks (for multi-version installs).
     */
    private void downloadFileWithProgress(String urlString, Path destination, String version) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        conn.setInstanceFollowRedirects(true);

        // Handle redirects (GitHub releases use redirects)
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                || responseCode == HttpURLConnection.HTTP_MOVED_PERM
                || responseCode == 307 || responseCode == 308) {
            String redirectUrl = conn.getHeaderField("Location");
            conn.disconnect();
            conn = (HttpURLConnection) new URL(redirectUrl).openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
        }

        long fileSize = conn.getContentLengthLong();
        LOGGER.info("Download size for Python {}: {} MB", version,
                fileSize > 0 ? fileSize / 1024 / 1024 : "unknown");

        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             OutputStream out = new BufferedOutputStream(Files.newOutputStream(destination))) {

            byte[] buffer = new byte[8192];
            long totalRead = 0;
            int bytesRead;
            long lastNotify = System.currentTimeMillis();

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;

                long now = System.currentTimeMillis();
                if (now - lastNotify > 2000) {
                    int percent = fileSize > 0 ? (int) ((totalRead * 70) / fileSize) : -1;
                    String msg = fileSize > 0
                            ? String.format("Downloaded %d/%d MB", totalRead / 1024 / 1024, fileSize / 1024 / 1024)
                            : String.format("Downloaded %d MB", totalRead / 1024 / 1024);
                    notifyProgress(version, "downloading", Math.max(0, percent), msg);
                    LOGGER.debug("Download progress for Python {}: {}", version, msg);
                    lastNotify = now;
                }
            }

            LOGGER.info("Download complete for Python {}: {} MB", version, totalRead / 1024 / 1024);
        } finally {
            conn.disconnect();
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
    String detectOS() {
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
     * Get installation status (legacy, includes multi-version info)
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("os", detectOS());
        status.put("embeddedInstalled", isEmbeddedPythonInstalled());
        status.put("pythonDir", pythonDir.toString());
        status.put("distributionsDir", distributionsDir.toString());
        status.put("autoDownload", autoDownload);
        status.put("installedVersions", getInstalledVersions());
        status.put("availableVersions", getAvailableDownloadVersions());

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
     * Get the distributions directory path.
     *
     * @return path to the distributions directory
     */
    public Path getDistributionsDir() {
        return distributionsDir;
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

    // ========================================================================
    // Progress notification helpers
    // ========================================================================

    private void notifyProgress(String version, String stage, int percent, String message) {
        InstallProgressListener listener = this.progressListener;
        if (listener != null) {
            try {
                listener.onProgress(version, stage, percent, message);
            } catch (Exception e) {
                LOGGER.warn("Progress listener error", e);
            }
        }
    }

    private void notifyComplete(String version, boolean success, String message) {
        InstallProgressListener listener = this.progressListener;
        if (listener != null) {
            try {
                listener.onComplete(version, success, message);
            } catch (Exception e) {
                LOGGER.warn("Progress listener error", e);
            }
        }
    }
}
