package com.inductiveautomation.ignition.examples.python3.gateway;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Manages bundled Python packages for offline installation.
 * <p>
 * Bundles .whl files in module resources and installs them to the Python environment
 * on demand, enabling offline/air-gapped deployment.
 * </p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Platform-specific wheel extraction (Windows x64, Linux x64)</li>
 *   <li>Package installation via pip</li>
 *   <li>Installation tracking and verification</li>
 *   <li>Support for package bundles (e.g., "web" includes requests + dependencies)</li>
 * </ul>
 *
 * @since v2.3.0
 */
public class Python3PackageManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(Python3PackageManager.class);
    private static final Gson GSON = new Gson();

    private final Path moduleDataDir;
    private final Path packagesDir;
    private final Path installedPackagesFile;
    private final String pythonExecutable;

    private Map<String, PackageInfo> packageCatalog;
    private Set<String> installedPackages;

    /**
     * Creates a new package manager.
     *
     * @param moduleDataDir    Directory for module data
     * @param pythonExecutable Path to Python executable
     */
    public Python3PackageManager(Path moduleDataDir, String pythonExecutable) {
        this.moduleDataDir = moduleDataDir;
        this.packagesDir = moduleDataDir.resolve("packages");
        this.installedPackagesFile = moduleDataDir.resolve("installed-packages.json");
        this.pythonExecutable = pythonExecutable;

        try {
            Files.createDirectories(packagesDir);
            loadPackageCatalog();
            loadInstalledPackages();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize package manager", e);
            this.packageCatalog = new HashMap<>();
            this.installedPackages = new HashSet<>();
        }
    }

    /**
     * Load package catalog from packages.json resource.
     */
    private void loadPackageCatalog() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/packages.json")) {
            if (is == null) {
                LOGGER.warn("packages.json not found in resources, package catalog empty");
                packageCatalog = new HashMap<>();
                return;
            }

            String json = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .reduce("", (a, b) -> a + b);

            packageCatalog = GSON.fromJson(json,
                    new TypeToken<Map<String, PackageInfo>>() {
                    }.getType());

            LOGGER.info("Loaded package catalog: {} packages", packageCatalog.size());
        }
    }

    /**
     * Load list of installed packages from tracking file.
     */
    private void loadInstalledPackages() {
        installedPackages = new HashSet<>();

        if (!Files.exists(installedPackagesFile)) {
            return;
        }

        try {
            String json = Files.readString(installedPackagesFile);
            installedPackages = GSON.fromJson(json, new TypeToken<Set<String>>() {
            }.getType());
            LOGGER.info("Loaded installed packages: {}", installedPackages);
        } catch (IOException e) {
            LOGGER.error("Failed to load installed packages file", e);
        }
    }

    /**
     * Save installed packages to tracking file.
     */
    private void saveInstalledPackages() {
        try {
            String json = GSON.toJson(installedPackages);
            Files.writeString(installedPackagesFile, json);
        } catch (IOException e) {
            LOGGER.error("Failed to save installed packages file", e);
        }
    }

    /**
     * Get package catalog (available packages).
     *
     * @return Map of package name to package info
     */
    public Map<String, PackageInfo> getPackageCatalog() {
        return new HashMap<>(packageCatalog);
    }

    /**
     * Get list of installed package names.
     *
     * @return Set of installed package names
     */
    public Set<String> getInstalledPackages() {
        return new HashSet<>(installedPackages);
    }

    /**
     * Check if a package is installed.
     *
     * @param packageName Package name
     * @return True if installed
     */
    public boolean isInstalled(String packageName) {
        return installedPackages.contains(packageName);
    }

    /**
     * Install a package bundle by name.
     *
     * @param packageName Package bundle name (e.g., "jedi", "web", "datascience")
     * @return Installation result
     */
    public InstallResult installPackage(String packageName) {
        LOGGER.info("Installing package: {}", packageName);

        PackageInfo packageInfo = packageCatalog.get(packageName);
        if (packageInfo == null) {
            return new InstallResult(false,
                    "Package not found in catalog: " + packageName, new ArrayList<>());
        }

        // Check if already installed
        if (isInstalled(packageName)) {
            LOGGER.info("Package {} already installed, skipping", packageName);
            return new InstallResult(true, "Already installed", new ArrayList<>());
        }

        List<String> installedWheels = new ArrayList<>();

        try {
            // Get platform-specific wheel directory
            String platform = detectPlatform();
            String resourcePath = "/python-packages/" + platform + "/";

            // Install each wheel in the bundle
            for (String wheelName : packageInfo.wheels) {
                String wheelResource = resourcePath + wheelName;
                LOGGER.debug("Installing wheel: {}", wheelResource);

                // Extract wheel from resources
                Path wheelPath = extractWheel(wheelResource);
                if (wheelPath == null) {
                    return new InstallResult(false,
                            "Wheel not found in resources: " + wheelName, installedWheels);
                }

                // Install with pip
                boolean success = installWheel(wheelPath);
                if (!success) {
                    return new InstallResult(false,
                            "Failed to install wheel: " + wheelName, installedWheels);
                }

                installedWheels.add(wheelName);
            }

            // Mark as installed
            installedPackages.add(packageName);
            saveInstalledPackages();

            LOGGER.info("Successfully installed package: {}", packageName);
            return new InstallResult(true,
                    "Successfully installed " + installedWheels.size() + " wheel(s)", installedWheels);

        } catch (Exception e) {
            LOGGER.error("Failed to install package: {}", packageName, e);
            return new InstallResult(false, "Installation failed: " + e.getMessage(), installedWheels);
        }
    }

    /**
     * Uninstall a package bundle by name.
     *
     * @param packageName Package bundle name
     * @return True if successfully uninstalled
     */
    public boolean uninstallPackage(String packageName) {
        LOGGER.info("Uninstalling package: {}", packageName);

        if (!isInstalled(packageName)) {
            LOGGER.warn("Package {} not installed, skipping uninstall", packageName);
            return true;
        }

        PackageInfo packageInfo = packageCatalog.get(packageName);
        if (packageInfo == null) {
            LOGGER.error("Package not found in catalog: {}", packageName);
            return false;
        }

        try {
            // Uninstall each package in the bundle
            for (String pipPackage : packageInfo.pipPackages) {
                boolean success = uninstallPipPackage(pipPackage);
                if (!success) {
                    LOGGER.warn("Failed to uninstall pip package: {}", pipPackage);
                    // Continue with other packages
                }
            }

            // Remove from installed set
            installedPackages.remove(packageName);
            saveInstalledPackages();

            LOGGER.info("Successfully uninstalled package: {}", packageName);
            return true;

        } catch (Exception e) {
            LOGGER.error("Failed to uninstall package: {}", packageName, e);
            return false;
        }
    }

    /**
     * Verify installed packages (check they're actually available to Python).
     *
     * @return Map of package name to verification status
     */
    public Map<String, Boolean> verifyPackages() {
        Map<String, Boolean> results = new HashMap<>();

        for (String packageName : installedPackages) {
            PackageInfo info = packageCatalog.get(packageName);
            if (info == null) {
                results.put(packageName, false);
                continue;
            }

            // Try importing the primary module
            boolean verified = verifyPythonImport(info.importName);
            results.put(packageName, verified);
        }

        return results;
    }

    /**
     * Extract a wheel file from resources to packages directory.
     *
     * @param resourcePath Resource path to wheel file
     * @return Path to extracted wheel, or null if not found
     */
    private Path extractWheel(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                LOGGER.error("Wheel not found in resources: {}", resourcePath);
                return null;
            }

            String wheelName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
            Path wheelPath = packagesDir.resolve(wheelName);

            Files.copy(is, wheelPath, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.debug("Extracted wheel to: {}", wheelPath);

            return wheelPath;

        } catch (IOException e) {
            LOGGER.error("Failed to extract wheel: {}", resourcePath, e);
            return null;
        }
    }

    /**
     * Install a wheel file using pip.
     *
     * @param wheelPath Path to wheel file
     * @return True if successful
     */
    private boolean installWheel(Path wheelPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    pythonExecutable,
                    "-m", "pip", "install",
                    "--no-index",  // Don't use PyPI
                    "--no-deps",   // Don't install dependencies (we bundle them)
                    wheelPath.toString()
            );

            Process process = pb.start();

            // Capture output
            BufferedReader stdout = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            BufferedReader stderr = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = stdout.readLine()) != null) {
                LOGGER.debug("pip stdout: {}", line);
            }
            while ((line = stderr.readLine()) != null) {
                LOGGER.debug("pip stderr: {}", line);
            }

            boolean exited = process.waitFor(60, TimeUnit.SECONDS);
            if (!exited) {
                process.destroyForcibly();
                LOGGER.error("pip install timed out");
                return false;
            }

            if (process.exitValue() == 0) {
                LOGGER.info("Successfully installed wheel: {}", wheelPath.getFileName());
                return true;
            } else {
                LOGGER.error("pip install failed with exit code: {}", process.exitValue());
                return false;
            }

        } catch (Exception e) {
            LOGGER.error("Failed to run pip install", e);
            return false;
        }
    }

    /**
     * Uninstall a pip package.
     *
     * @param packageName Package name
     * @return True if successful
     */
    private boolean uninstallPipPackage(String packageName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    pythonExecutable,
                    "-m", "pip", "uninstall",
                    "-y",  // Don't ask for confirmation
                    packageName
            );

            Process process = pb.start();
            boolean exited = process.waitFor(30, TimeUnit.SECONDS);

            if (!exited) {
                process.destroyForcibly();
                return false;
            }

            return process.exitValue() == 0;

        } catch (Exception e) {
            LOGGER.error("Failed to run pip uninstall", e);
            return false;
        }
    }

    /**
     * Verify a Python module can be imported.
     *
     * @param moduleName Module name to import
     * @return True if import succeeds
     */
    private boolean verifyPythonImport(String moduleName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    pythonExecutable,
                    "-c",
                    "import " + moduleName
            );

            Process process = pb.start();
            boolean exited = process.waitFor(5, TimeUnit.SECONDS);

            if (!exited) {
                process.destroyForcibly();
                return false;
            }

            return process.exitValue() == 0;

        } catch (Exception e) {
            LOGGER.error("Failed to verify import: {}", moduleName, e);
            return false;
        }
    }

    /**
     * Detect platform for wheel selection.
     *
     * @return Platform string (windows-x64 or linux-x64)
     */
    private String detectPlatform() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return "windows-x64";
        } else if (os.contains("linux")) {
            return "linux-x64";
        } else {
            LOGGER.warn("Unsupported platform: {}, defaulting to linux-x64", os);
            return "linux-x64";
        }
    }

    /**
     * Get installation status summary.
     *
     * @return Status map
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("platform", detectPlatform());
        status.put("pythonExecutable", pythonExecutable);
        status.put("packagesDir", packagesDir.toString());
        status.put("catalogSize", packageCatalog.size());
        status.put("installedCount", installedPackages.size());
        status.put("installedPackages", new ArrayList<>(installedPackages));

        return status;
    }

    // Inner classes

    /**
     * Package metadata from packages.json.
     */
    public static class PackageInfo {
        public String version;
        public String description;
        public double sizeMb;
        public List<String> wheels;
        public List<String> pipPackages;
        public String importName;
        public List<String> requiredFor;

        public PackageInfo() {
            this.wheels = new ArrayList<>();
            this.pipPackages = new ArrayList<>();
            this.requiredFor = new ArrayList<>();
        }
    }

    /**
     * Installation result.
     */
    public static class InstallResult {
        public final boolean success;
        public final String message;
        public final List<String> installedWheels;

        public InstallResult(boolean success, String message, List<String> installedWheels) {
            this.success = success;
            this.message = message;
            this.installedWheels = installedWheels;
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("success", success);
            json.addProperty("message", message);
            json.add("installedWheels", GSON.toJsonTree(installedWheels));
            return json;
        }
    }
}
