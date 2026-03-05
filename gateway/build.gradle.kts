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
    compileOnly("com.google.code.gson:gson:2.11.0")
    compileOnly("jakarta.servlet:jakarta.servlet-api:5.0.0")

    // Third-party libraries to bundle in module
    modlImplementation("org.apache.commons:commons-compress:1.27.1")  // Updated from 1.24.0 (CVE-2024-25710, CVE-2024-26308)

    // Test dependencies - make compile dependencies available for tests
    testImplementation(libs.ignition.common)
    testImplementation(libs.ignition.gateway.api)
    testImplementation("com.google.code.gson:gson:2.11.0")
    testImplementation("jakarta.servlet:jakarta.servlet-api:5.0.0")
}

// ========== React UI Build Automation (v3.2.0 - Gateway Web UI) ==========

val reactUIDir = file("${project.rootDir}/react-ui")
val reactDistDir = file("${reactUIDir}/build/generated-resources/mounted")
val gatewayResourcesDir = file("${projectDir}/src/main/resources")

/**
 * Check if npm/node is available on the system.
 */
fun isNpmAvailable(): Boolean {
    return try {
        val process = ProcessBuilder("npm", "--version").start()
        process.waitFor() == 0
    } catch (e: Exception) {
        false
    }
}

/**
 * Task: Install npm dependencies for React UI.
 * Only runs if node_modules doesn't exist.
 */
tasks.register<Exec>("npmInstall") {
    group = "build"
    description = "Install npm dependencies for React UI"

    workingDir = reactUIDir
    commandLine = if (System.getProperty("os.name").lowercase().contains("windows")) {
        listOf("cmd", "/c", "npm", "install")
    } else {
        listOf("npm", "install")
    }

    onlyIf {
        !file("${reactUIDir}/node_modules").exists() && isNpmAvailable()
    }

    doFirst {
        if (!isNpmAvailable()) {
            throw GradleException("npm is not available. Please install Node.js and npm to build the React UI.")
        }
        logger.lifecycle("Installing npm dependencies in ${reactUIDir}...")
    }
}

/**
 * Task: Build React UI using Webpack.
 * Outputs: react-ui/build/generated-resources/mounted/Python3IDE.js (UMD module)
 */
tasks.register<Exec>("buildReactUI") {
    group = "build"
    description = "Build React UI with Webpack (TypeScript + UMD bundling)"

    dependsOn("npmInstall")

    workingDir = reactUIDir
    commandLine = if (System.getProperty("os.name").lowercase().contains("windows")) {
        listOf("cmd", "/c", "npm", "run", "build")
    } else {
        listOf("npm", "run", "build")
    }

    inputs.dir("${reactUIDir}/src")
    inputs.file("${reactUIDir}/package.json")
    inputs.file("${reactUIDir}/tsconfig.json")
    inputs.file("${reactUIDir}/webpack.config.js")

    outputs.dir(reactDistDir)

    onlyIf {
        isNpmAvailable()
    }

    doFirst {
        logger.lifecycle("Building React UI with Webpack (UMD module)...")
        if (reactDistDir.exists()) {
            delete(reactDistDir)
        }
    }

    doLast {
        if (reactDistDir.exists()) {
            logger.lifecycle("React UI built successfully: ${reactDistDir.absolutePath}")
        } else {
            throw GradleException("React build failed: build output directory not created")
        }
    }
}

/**
 * Task: Copy React build output to gateway resources.
 */
tasks.register<Task>("copyReactUIResources") {
    group = "build"
    description = "Copy webpack UMD module to gateway resources"

    dependsOn("buildReactUI")

    onlyIf {
        reactDistDir.exists() && isNpmAvailable()
    }

    doFirst {
        logger.lifecycle("Copying React UI webpack bundle to gateway...")

        // Clean old UI resources in mounted folder
        file("${gatewayResourcesDir}/mounted").deleteRecursively()
        file("${gatewayResourcesDir}/mounted").mkdirs()

        // Copy webpack output (Python3IDE.js and source map)
        copy {
            from(reactDistDir) {
                include("Python3IDE.js")
                include("Python3IDE.js.map")
            }
            into("${gatewayResourcesDir}/mounted")
        }

        // Copy standalone HTML page from static resources
        copy {
            from("${gatewayResourcesDir}/static") {
                include("standalone.html")
            }
            into("${gatewayResourcesDir}/mounted")
        }

        // Copy React UMD files for standalone page
        val reactUmdDir = file("${reactUIDir}/node_modules/react/umd")
        val reactDomUmdDir = file("${reactUIDir}/node_modules/react-dom/umd")

        if (reactUmdDir.exists() && reactDomUmdDir.exists()) {
            copy {
                from(reactUmdDir) {
                    include("react.production.min.js")
                }
                from(reactDomUmdDir) {
                    include("react-dom.production.min.js")
                }
                into("${gatewayResourcesDir}/mounted")
            }
            logger.lifecycle("Copied React UMD files for standalone page")
        } else {
            logger.warn("React UMD files not found - standalone page will not work")
        }
    }

    doLast {
        val jsFile = file("${gatewayResourcesDir}/mounted/Python3IDE.js")
        if (jsFile.exists()) {
            val sizeKb = jsFile.length() / 1024
            logger.lifecycle("Copied UMD module: Python3IDE.js (${sizeKb} KB)")
        } else {
            throw GradleException("Failed to copy Python3IDE.js - file not found")
        }
    }
}

/**
 * Task: Clean React build outputs.
 */
tasks.register<Delete>("cleanReactUI") {
    group = "build"
    description = "Clean React UI build outputs"

    delete("${reactUIDir}/build")
    delete("${gatewayResourcesDir}/mounted")

    doLast {
        logger.lifecycle("Cleaned React UI build outputs")
    }
}

// Integrate React build into standard Gradle lifecycle
tasks.named("processResources") {
    dependsOn("copyReactUIResources")
}

tasks.named("clean") {
    dependsOn("cleanReactUI")
}
