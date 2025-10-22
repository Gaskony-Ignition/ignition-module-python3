plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Configure jar name to be version-independent for module upgrade compatibility
tasks.jar {
    archiveBaseName.set("designer")
    archiveVersion.set("")
}

dependencies {
    // Common scope dependency
    api(projects.common)

    // Ignition SDK dependencies (provided by platform)
    compileOnly(libs.ignition.common)
    compileOnly(libs.ignition.designer.api)

    // HTTP Client for REST API communication (Java 11+)
    // Note: HttpClient is part of java.net.http since Java 11, no additional dependency needed

    // JSON parsing (Gson - already available via Ignition SDK)
    compileOnly(libs.ignition.common)  // Provides Gson

    // Logging
    compileOnly("org.slf4j:slf4j-api:1.7.36")
    implementation("org.slf4j:slf4j-simple:1.7.36")  // For standalone test harness

    // RSyntaxTextArea - Advanced code editor with syntax highlighting
    implementation("com.fifesoft:rsyntaxtextarea:3.3.4")
    implementation("com.fifesoft:autocomplete:3.3.1")      // Code completion (for v2.0.0)
    implementation("com.fifesoft:rstaui:3.3.1")            // Find/Replace dialogs (for v2.0.0)
}

// ============================================================================
// Standalone Test Harness for Rapid UX Development
// ============================================================================

/**
 * Run the Python3IDE in standalone mode for rapid UX testing.
 *
 * Usage: ./gradlew runIDE
 *
 * This launches the IDE with a mock Gateway REST API server, allowing you to
 * test UI changes without building/installing the module or restarting the gateway.
 */
tasks.register<JavaExec>("runIDE") {
    description = "Launch Python3IDE standalone test harness for rapid UX development"
    group = "application"

    mainClass.set("com.inductiveautomation.ignition.examples.python3.designer.testharness.Python3IDETestHarness")

    // Include both runtime classpath AND compileOnly dependencies for standalone mode
    classpath = sourceSets["main"].runtimeClasspath + configurations["compileClasspath"]

    // Enable assertions for better debugging
    jvmArgs("-ea")

    // Set system properties for standalone mode
    systemProperty("ignition.dev.mode", "true")

    // Better console output
    standardInput = System.`in`
    standardOutput = System.out
    errorOutput = System.err
}
