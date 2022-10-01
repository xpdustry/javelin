plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("net.kyori:indra-common:2.1.1")
    implementation("gradle.plugin.org.cadixdev.gradle:licenser:0.6.1")
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:2.0.2")
    implementation("fr.xpdustry:toxopid:2.1.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
