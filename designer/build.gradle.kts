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

    // Logging (Updated to 2.0.16 from 1.7.36 - latest stable)
    compileOnly(libs.slf4j.api)
    implementation(libs.slf4j.simple)  // For standalone test harness

    // RSyntaxTextArea - Advanced code editor with syntax highlighting (v2.15.9: updated to 3.5.2)
    implementation(libs.rsyntaxtextarea)   // Updated from 3.3.4 - latest stable
    implementation(libs.autocomplete)      // Keep at 3.3.1 (3.3.4 not available in repos)
    implementation(libs.rstaui)            // Keep at 3.3.1 (3.3.4 not available in repos)

    // FlatLaf - Modern Swing Look and Feel (v3.6.0: Visual redesign)
    implementation(libs.flatlaf)

    // Test framework
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.ignition.common)  // Ignition-bundled Gson for JSON-parsing tests
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

