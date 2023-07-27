plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("fr.xpdustry:toxopid:3.2.0")
    implementation("net.kyori:indra-licenser-spotless:3.1.2")
    implementation("net.kyori:indra-common:3.1.2")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.20.0")
    implementation("com.github.johnrengelman:shadow:8.1.1")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:3.1.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
