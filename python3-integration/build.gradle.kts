plugins {
    base
    id("io.ia.sdk.modl") version "0.4.1"  // Latest stable version for better security
    id("org.owasp.dependencycheck") version "9.0.9"
    checkstyle
    jacoco  // Code coverage plugin
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
    fileName.set("Python3Integration-${project.version}")

    name.set("Python 3 Integration")
    // IMPORTANT: Module ID must remain consistent for upgrade compatibility
    // Changed from com.gaskony.python3integration back to original to allow upgrades
    id.set("com.inductiveautomation.ignition.examples.python3")
    moduleVersion.set(project.version.toString())

    // Include vendor name in description
    moduleDescription.set("Enables Python 3 scripting functions in Ignition via subprocess process pool. Developed by Gaskony.")

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

// Checkstyle Configuration
checkstyle {
    toolVersion = "10.12.5"
    configFile = file("config/checkstyle/checkstyle.xml")
}

// Apply Checkstyle and testing to all subprojects
subprojects {
    apply(plugin = "checkstyle")
    apply(plugin = "java-library")
    apply(plugin = "jacoco")

    configure<CheckstyleExtension> {
        toolVersion = "10.12.5"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    }

    // Apply test dependencies to all subprojects
    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:5.10.1")
        "testImplementation"("org.junit.jupiter:junit-jupiter-params:5.10.1")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.10.1")
        "testImplementation"("org.mockito:mockito-core:5.8.0")
        "testImplementation"("org.mockito:mockito-inline:5.2.0")
        "testImplementation"("org.mockito:mockito-junit-jupiter:5.8.0")
        "testImplementation"("org.assertj:assertj-core:3.24.2")
        "testImplementation"("org.awaitility:awaitility:4.2.0")
        "testImplementation"("org.slf4j:slf4j-simple:2.0.9")
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

    // Coverage verification
    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        dependsOn(tasks.withType<Test>())
        violationRules {
            rule {
                limit {
                    minimum = "0.80".toBigDecimal()  // 80% coverage target
                }
            }
        }
    }
}
