import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("net.kyori.indra")
    id("net.kyori.indra.publishing")
    id("net.kyori.indra.licenser.spotless")
    id("net.ltgt.errorprone")
}

repositories {
    mavenCentral()
    sonatype.ossSnapshots()
}

dependencies {
    val junit = "5.8.2"
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.3")
    testImplementation("org.assertj:assertj-core:3.23.1")

    // Static analysis
    compileOnlyApi("org.checkerframework:checker-qual:3.26.0")
    testCompileOnly("org.checkerframework:checker-qual:3.26.0")
    annotationProcessor("com.uber.nullaway:nullaway:0.9.7")
    errorprone("com.google.errorprone:error_prone_core:2.13.1")
}

tasks.withType(JavaCompile::class.java).configureEach {
    options.errorprone {
        disableWarningsInGeneratedCode.set(true)
        disable("MissingSummary", "FutureReturnValueIgnored")
        if (!name.contains("test", true)) {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "fr.xpdustry.javelin")
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
}

indra {
    javaVersions {
        target(17)
        minimumToolchain(17)
    }

    publishReleasesTo("xpdustry", "https://maven.xpdustry.com/releases")
    publishSnapshotsTo("xpdustry", "https://maven.xpdustry.com/snapshots")

    gpl3OnlyLicense()

    github("xpdustry", "javelin") {
        ci(true)
        issues(true)
        scm(true)
    }

    configurePublications {
        pom {
            organization {
                name.set("Xpdustry")
                url.set("https://www.xpdustry.com")
            }

            developers {
                developer {
                    id.set("Phinner")
                    timezone.set("Europe/Brussels")
                }
            }
        }
    }
}

indraSpotlessLicenser {
    licenseHeaderFile(rootProject.file("LICENSE_HEADER.md"))
}

spotless {
    java {
        palantirJavaFormat()
        formatAnnotations()
    }
}
