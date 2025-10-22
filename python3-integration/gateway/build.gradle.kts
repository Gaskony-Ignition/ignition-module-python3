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
    archiveBaseName.set("gateway")
    archiveVersion.set("")
}

dependencies {
    // Common scope dependency
    api(projects.common)

    // SDK dependencies (provided by Ignition)
    compileOnly(libs.ignition.common)
    compileOnly(libs.ignition.gateway.api)
    compileOnly(libs.ignition.perspective.gateway)
    compileOnly(libs.ignition.perspective.common)
    compileOnly("com.google.code.gson:gson:2.10.1")
    compileOnly("javax.servlet:javax.servlet-api:4.0.1")

    // Third-party libraries to bundle in module
    modlImplementation("org.apache.commons:commons-compress:1.24.0")

    // Test dependencies - make compile dependencies available for tests
    testImplementation(libs.ignition.common)
    testImplementation(libs.ignition.gateway.api)
    testImplementation("com.google.code.gson:gson:2.10.1")
    testImplementation("javax.servlet:javax.servlet-api:4.0.1")
}
