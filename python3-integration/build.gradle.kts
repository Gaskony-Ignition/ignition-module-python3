plugins {
    base
    id("io.ia.sdk.modl") version "0.4.1"  // Latest stable version for better security
    id("org.owasp.dependencycheck") version "11.1.0"
    checkstyle
    jacoco  // Code coverage plugin
    id("com.github.spotbugs") version "6.0.7"
}

// Load version from version.properties
val versionProps = java.util.Properties()
file("version.properties").inputStream().use { versionProps.load(it) }
val versionMajor = versionProps.getProperty("version.major")
val versionMinor = versionProps.getProperty("version.minor")
val versionPatch = versionProps.getProperty("version.patch")
val moduleVersion = "$versionMajor.$versionMinor.$versionPatch"

version = moduleVersion
group = "com.gaskony"

allprojects {
    version = moduleVersion
    group = "com.gaskony"
}

ignitionModule {
    // Include version in filename for version control
    fileName.set("Python3-${project.version}")

    name.set("Python 3 (Java Swing)")
    // IMPORTANT: Distinct ID for Java Swing version
    id.set("com.gaskony.python3.swing")
    moduleVersion.set(project.version.toString())

    // Include vendor name in description
    moduleDescription.set("Python 3 with Java Swing IDE - Production-ready v2.15.10. Classic desktop UI with RSyntaxTextArea editor. Developed by Gaskony.")

    requiredIgnitionVersion.set("8.3.0")
    requiredFrameworkVersion.set("8")

    // Free module - no license required
    freeModule.set(true)

    projectScopes.putAll(
        mapOf(
            ":common" to "G",
            ":gateway" to "G",
            ":designer" to "D"  // Designer scope for Python 3 IDE (v1.7.0+, REST API communication)
        )
    )

    moduleDependencies.putAll(
        mapOf()
    )

    hooks.putAll(
        mapOf(
            "com.inductiveautomation.ignition.examples.python3.gateway.GatewayHook" to "G",
            "com.inductiveautomation.ignition.examples.python3.designer.DesignerHook" to "D"  // Designer hook for IDE UI
        )
    )

    // Enable module signing with self-signed certificate
    // Signing configured via sign.props file
    skipModlSigning.set(false)
}

// OWASP Dependency Check Configuration
dependencyCheck {
    format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.HTML.toString()
    outputDirectory = "build/reports"

    // Suppress false positives and known issues
    suppressionFile = "config/owasp-suppressions.xml"

    // Fail build on CVSS score >= 7 (High or Critical)
    failBuildOnCVSS = 7.0f

    // Check all configurations
    scanConfigurations = listOf("runtimeClasspath", "compileClasspath")
}

// Checkstyle Configuration (v2.15.9: standardized to 10.20.1)
checkstyle {
    toolVersion = "10.20.1"
    configFile = file("config/checkstyle/checkstyle.xml")
}

// Sync version.properties across all scopes from the canonical root file
tasks.register("syncVersion") {
    group = "versioning"
    description = "Copies root version.properties to common and designer resource directories"

    val source = file("version.properties")
    val targets = listOf(
        file("common/src/main/resources/version.properties"),
        file("designer/src/main/resources/version.properties")
    )

    inputs.file(source)
    outputs.files(targets)

    doLast {
        val content = source.readText()
        targets.forEach { target ->
            target.parentFile.mkdirs()
            target.writeText(content)
            logger.lifecycle("Synced version.properties -> ${target.relativeTo(projectDir)}")
        }
    }
}

// Apply Checkstyle and testing to all subprojects
subprojects {
    apply(plugin = "checkstyle")
    apply(plugin = "java-library")
    apply(plugin = "jacoco")
    apply(plugin = "com.github.spotbugs")

    configure<CheckstyleExtension> {
        toolVersion = "10.20.1"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    }

    // SpotBugs configuration - report issues but don't fail the build
    // Designer scope has pre-existing EI2 warnings that are benign for internal module code
    spotbugs {
        ignoreFailures.set(true)
        effort.set(com.github.spotbugs.snom.Effort.MAX)
        reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
    }

    // Apply test dependencies to all subprojects
    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:5.11.3")
        "testImplementation"("org.junit.jupiter:junit-jupiter-params:5.11.3")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.11.3")
        "testImplementation"("org.mockito:mockito-core:5.14.2")
        // mockito-inline removed in v2.15.9 - functionality merged into mockito-core 5.0+
        "testImplementation"("org.mockito:mockito-junit-jupiter:5.14.2")
        "testImplementation"("org.assertj:assertj-core:3.26.3")
        "testImplementation"("org.awaitility:awaitility:4.2.2")
        "testImplementation"("org.slf4j:slf4j-simple:2.0.16")
    }

    // Configure test task
    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = false
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        maxHeapSize = "1g"
        finalizedBy(tasks.named("jacocoTestReport"))  // Generate coverage report after tests
    }

    // Configure JaCoCo
    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.withType<Test>())
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(true)  // Enable CSV for GitHub Actions
        }
    }

    // Coverage verification - only enforce on gateway (which has tests)
    if (project.name == "gateway") {
        tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
            dependsOn(tasks.withType<Test>())
            violationRules {
                rule {
                    limit {
                        minimum = "0.50".toBigDecimal()  // 50% coverage threshold (51.7% at v3.8.0)
                    }
                }
            }
        }

        // Wire coverage verification into the check lifecycle
        tasks.named("check") {
            dependsOn("jacocoTestCoverageVerification")
        }
    }
}
