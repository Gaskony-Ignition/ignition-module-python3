package com.inductiveautomation.ignition.examples.python3.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Input validator for Python code and variables.
 *
 * Validates:
 * - Code length (prevent DoS attacks)
 * - Variable count and size
 * - Malicious patterns (imports, system calls, file access)
 * - Suspicious constructs (excessive loops, recursion)
 *
 * Configurable via system properties:
 * - ignition.python3.validation.max.code.length (default: 1MB)
 * - ignition.python3.validation.max.variables (default: 100)
 * - ignition.python3.validation.max.variable.size (default: 1MB)
 * - ignition.python3.validation.allow.file.access (default: false)
 * - ignition.python3.validation.allow.subprocess (default: false)
 * - ignition.python3.validation.allow.network (default: false)
 *
 * @since v2.14.0 (Phase 2 Week 5-6: Security Enhancements)
 */
public class InputValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(InputValidator.class);

    // Default limits
    private static final int DEFAULT_MAX_CODE_LENGTH = 1_048_576;      // 1 MB
    private static final int DEFAULT_MAX_VARIABLES = 100;
    private static final int DEFAULT_MAX_VARIABLE_SIZE = 1_048_576;    // 1 MB

    // Configuration
    private final int maxCodeLength;
    private final int maxVariables;
    private final int maxVariableSize;
    private final boolean allowFileAccess;
    private final boolean allowSubprocess;
    private final boolean allowNetwork;
    private final boolean enforcePatternDetection;

    // Pattern detection (compiled patterns for performance)
    private final List<MaliciousPattern> maliciousPatterns;

    /**
     * Malicious pattern definition.
     */
    private static class MaliciousPattern {
        final Pattern pattern;
        final String description;
        final SecurityLevel level;

        enum SecurityLevel {
            BLOCK,      // Always block
            WARN,       // Log warning but allow
            INFO        // Log info only
        }

        MaliciousPattern(String regex, String description, SecurityLevel level) {
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            this.description = description;
            this.level = level;
        }
    }

    /**
     * Create input validator with default settings.
     */
    public InputValidator() {
        this(
            getIntProperty("ignition.python3.validation.max.code.length", DEFAULT_MAX_CODE_LENGTH),
            getIntProperty("ignition.python3.validation.max.variables", DEFAULT_MAX_VARIABLES),
            getIntProperty("ignition.python3.validation.max.variable.size", DEFAULT_MAX_VARIABLE_SIZE),
            getBooleanProperty("ignition.python3.validation.allow.file.access", false),
            getBooleanProperty("ignition.python3.validation.allow.subprocess", false),
            getBooleanProperty("ignition.python3.validation.allow.network", false),
            getBooleanProperty("ignition.python3.validation.enforce.patterns", true)
        );
    }

    /**
     * Create input validator with custom settings.
     */
    public InputValidator(int maxCodeLength, int maxVariables, int maxVariableSize,
                         boolean allowFileAccess, boolean allowSubprocess, boolean allowNetwork,
                         boolean enforcePatternDetection) {
        this.maxCodeLength = maxCodeLength;
        this.maxVariables = maxVariables;
        this.maxVariableSize = maxVariableSize;
        this.allowFileAccess = allowFileAccess;
        this.allowSubprocess = allowSubprocess;
        this.allowNetwork = allowNetwork;
        this.enforcePatternDetection = enforcePatternDetection;

        this.maliciousPatterns = new ArrayList<>();
        initializeMaliciousPatterns();

        LOGGER.info("InputValidator initialized: maxCodeLength={}, maxVariables={}, maxVarSize={}, " +
                   "allowFileAccess={}, allowSubprocess={}, allowNetwork={}, enforcePatterns={}",
            maxCodeLength, maxVariables, maxVariableSize,
            allowFileAccess, allowSubprocess, allowNetwork, enforcePatternDetection);
    }

    /**
     * Initialize malicious pattern detection.
     */
    private void initializeMaliciousPatterns() {
        // File access patterns (if not allowed)
        if (!allowFileAccess) {
            maliciousPatterns.add(new MaliciousPattern(
                "\\b(open|file|read|write|mkdir|rmdir|remove|unlink|rename)\\s*\\(",
                "File access detected",
                MaliciousPattern.SecurityLevel.BLOCK
            ));
            maliciousPatterns.add(new MaliciousPattern(
                "\\bos\\.(remove|unlink|mkdir|rmdir|rename|chmod|chown)",
                "OS file manipulation detected",
                MaliciousPattern.SecurityLevel.BLOCK
            ));
            maliciousPatterns.add(new MaliciousPattern(
                "\\bshutil\\.(rmtree|move|copy|copyfile)",
                "Shutil file operations detected",
                MaliciousPattern.SecurityLevel.BLOCK
            ));
        }

        // Subprocess execution (if not allowed)
        if (!allowSubprocess) {
            maliciousPatterns.add(new MaliciousPattern(
                "\\bsubprocess\\.(run|call|Popen|check_output|check_call)",
                "Subprocess execution detected",
                MaliciousPattern.SecurityLevel.BLOCK
            ));
            maliciousPatterns.add(new MaliciousPattern(
                "\\bos\\.(system|popen|spawn|exec)",
                "OS command execution detected",
                MaliciousPattern.SecurityLevel.BLOCK
            ));
            maliciousPatterns.add(new MaliciousPattern(
                "\\b__import__\\s*\\(\\s*['\"]os['\"]\\s*\\)",
                "Dynamic os module import detected",
                MaliciousPattern.SecurityLevel.BLOCK
            ));
        }

        // Network access (if not allowed)
        if (!allowNetwork) {
            maliciousPatterns.add(new MaliciousPattern(
                "\\b(socket|urllib|requests|http\\.client|ftplib)\\.",
                "Network access detected",
                MaliciousPattern.SecurityLevel.BLOCK
            ));
            maliciousPatterns.add(new MaliciousPattern(
                "\\b__import__\\s*\\(\\s*['\"]socket['\"]\\s*\\)",
                "Dynamic socket import detected",
                MaliciousPattern.SecurityLevel.BLOCK
            ));
        }

        // Always block dangerous operations
        maliciousPatterns.add(new MaliciousPattern(
            "\\beval\\s*\\(|\\bexec\\s*\\(",
            "eval/exec usage detected (code injection risk)",
            MaliciousPattern.SecurityLevel.BLOCK
        ));
        maliciousPatterns.add(new MaliciousPattern(
            "\\bcompile\\s*\\(",
            "Dynamic code compilation detected",
            MaliciousPattern.SecurityLevel.WARN
        ));
        maliciousPatterns.add(new MaliciousPattern(
            "\\b__import__\\s*\\(.*\\bsubprocess\\b.*\\)",
            "Dynamic subprocess import detected",
            MaliciousPattern.SecurityLevel.BLOCK
        ));
        maliciousPatterns.add(new MaliciousPattern(
            "\\bpickle\\.(loads?|dumps?)\\s*\\(",
            "Pickle usage detected (arbitrary code execution risk)",
            MaliciousPattern.SecurityLevel.WARN
        ));

        // Warn on potentially dangerous patterns
        maliciousPatterns.add(new MaliciousPattern(
            "while\\s+True\\s*:",
            "Infinite loop detected (potential DoS)",
            MaliciousPattern.SecurityLevel.WARN
        ));
        maliciousPatterns.add(new MaliciousPattern(
            "\\bctypes\\.",
            "ctypes usage detected (low-level memory access)",
            MaliciousPattern.SecurityLevel.WARN
        ));
    }

    /**
     * Validate Python code.
     * @throws ValidationException if validation fails
     */
    public void validateCode(String code) throws ValidationException {
        if (code == null) {
            return; // Allow null/empty code
        }

        // Check code length
        int length = code.getBytes().length;
        if (length > maxCodeLength) {
            throw new ValidationException(
                String.format("Code length (%d bytes) exceeds maximum (%d bytes)", length, maxCodeLength)
            );
        }

        // Check for malicious patterns
        if (enforcePatternDetection) {
            for (MaliciousPattern mp : maliciousPatterns) {
                if (mp.pattern.matcher(code).find()) {
                    switch (mp.level) {
                        case BLOCK:
                            throw new ValidationException(
                                String.format("Security validation failed: %s", mp.description)
                            );
                        case WARN:
                            LOGGER.warn("Security warning in code: {}", mp.description);
                            break;
                        case INFO:
                            LOGGER.info("Security info: {}", mp.description);
                            break;
                    }
                }
            }
        }
    }

    /**
     * Validate variables (count and size).
     * @throws ValidationException if validation fails
     */
    public void validateVariables(java.util.Map<String, Object> variables) throws ValidationException {
        if (variables == null || variables.isEmpty()) {
            return;
        }

        // Check variable count
        if (variables.size() > maxVariables) {
            throw new ValidationException(
                String.format("Variable count (%d) exceeds maximum (%d)", variables.size(), maxVariables)
            );
        }

        // Check individual variable sizes
        for (java.util.Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Validate variable name (prevent injection)
            validateVariableName(key);

            // Validate variable size
            if (value != null) {
                int size = estimateObjectSize(value);
                if (size > maxVariableSize) {
                    throw new ValidationException(
                        String.format("Variable '%s' size (%d bytes) exceeds maximum (%d bytes)",
                            key, size, maxVariableSize)
                    );
                }
            }
        }
    }

    /**
     * Validate variable name (prevent injection attacks).
     */
    private void validateVariableName(String name) throws ValidationException {
        if (name == null || name.isEmpty()) {
            throw new ValidationException("Variable name cannot be null or empty");
        }

        // Check for Python reserved keywords
        String[] reservedKeywords = {
            "and", "as", "assert", "break", "class", "continue", "def", "del", "elif", "else",
            "except", "False", "finally", "for", "from", "global", "if", "import", "in", "is",
            "lambda", "None", "nonlocal", "not", "or", "pass", "raise", "return", "True", "try",
            "while", "with", "yield", "__import__", "eval", "exec", "compile"
        };

        for (String keyword : reservedKeywords) {
            if (name.equals(keyword)) {
                throw new ValidationException(
                    String.format("Variable name '%s' is a Python reserved keyword", name)
                );
            }
        }

        // Check for valid Python identifier
        if (!name.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new ValidationException(
                String.format("Variable name '%s' is not a valid Python identifier", name)
            );
        }

        // Check for potentially malicious names
        if (name.startsWith("__") && name.endsWith("__")) {
            LOGGER.warn("Variable name '{}' uses dunder pattern (potential magic method override)", name);
        }
    }

    /**
     * Estimate object size in bytes (rough approximation).
     */
    private int estimateObjectSize(Object obj) {
        if (obj == null) {
            return 0;
        }

        if (obj instanceof String) {
            return ((String) obj).getBytes().length;
        } else if (obj instanceof byte[]) {
            return ((byte[]) obj).length;
        } else if (obj instanceof java.util.Collection) {
            int totalSize = 0;
            for (Object item : (java.util.Collection<?>) obj) {
                totalSize += estimateObjectSize(item);
            }
            return totalSize;
        } else if (obj instanceof java.util.Map) {
            int totalSize = 0;
            for (Object value : ((java.util.Map<?, ?>) obj).values()) {
                totalSize += estimateObjectSize(value);
            }
            return totalSize;
        } else {
            // For primitive wrappers and other objects, use string representation size
            return obj.toString().getBytes().length;
        }
    }

    /**
     * Validate complete execution request (code + variables).
     */
    public void validateExecutionRequest(String code, java.util.Map<String, Object> variables)
            throws ValidationException {
        validateCode(code);
        validateVariables(variables);
    }

    // Getters

    public int getMaxCodeLength() {
        return maxCodeLength;
    }

    public int getMaxVariables() {
        return maxVariables;
    }

    public int getMaxVariableSize() {
        return maxVariableSize;
    }

    public boolean isAllowFileAccess() {
        return allowFileAccess;
    }

    public boolean isAllowSubprocess() {
        return allowSubprocess;
    }

    public boolean isAllowNetwork() {
        return allowNetwork;
    }

    public boolean isEnforcePatternDetection() {
        return enforcePatternDetection;
    }

    public int getPatternCount() {
        return maliciousPatterns.size();
    }

    // Helper methods for loading from system properties

    private static int getIntProperty(String key, int defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid int property {}: {}, using default: {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    private static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    @Override
    public String toString() {
        return String.format(
            "InputValidator{maxCodeLength=%d, maxVariables=%d, maxVarSize=%d, " +
            "allowFileAccess=%s, allowSubprocess=%s, allowNetwork=%s, patterns=%d}",
            maxCodeLength, maxVariables, maxVariableSize,
            allowFileAccess, allowSubprocess, allowNetwork, maliciousPatterns.size()
        );
    }

    /**
     * Exception thrown when validation fails.
     */
    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}
