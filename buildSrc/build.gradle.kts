plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://maven.xpdustry.fr/releases") {
        name = "xpdustry-releases"
        mavenContent { releasesOnly() }
    }
    maven("https://maven.xpdustry.fr/snapshots") {
        name = "xpdustry-snapshots"
        mavenContent { snapshotsOnly() }
    }
}

dependencies {
    implementation("net.kyori:indra-common:2.1.1")
    implementation("gradle.plugin.org.cadixdev.gradle:licenser:0.6.1")
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
    implementation("fr.xpdustry:toxopid:2.1.1-SNAPSHOT")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
