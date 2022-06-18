import fr.xpdustry.toxopid.ModPlatform
import fr.xpdustry.toxopid.util.ModMetadata
import fr.xpdustry.toxopid.util.anukenJitpack
import fr.xpdustry.toxopid.util.mindustryDependencies

plugins {
    kotlin("jvm") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.kyori.indra") version "2.1.1"
    id("net.kyori.indra.publishing") version "2.1.1"
    id("fr.xpdustry.toxopid") version "2.0.0"
}

val metadata = ModMetadata.fromJson(file("${rootProject.rootDir}/plugin.json"))
metadata.version += if (hasProperty("releaseProject") && property("releaseProject").toString().toBoolean()) "" else "-SNAPSHOT"

group = property("props.project-group").toString()
version = metadata.version

toxopid {
    compileVersion.set("v" + metadata.minGameVersion)
    platforms.add(ModPlatform.HEADLESS)
}

repositories {
    mavenCentral()
    anukenJitpack()
    maven("https://repo.xpdustry.fr/releases") {
        name = "xpdustry-repository-releases"
        mavenContent {
            includeGroupByRegex("fr\\.xpdustry|net\\.mindustry_ddns")
            releasesOnly()
        }
    }
}

dependencies {
    compileOnly(kotlin("stdlib"))
    mindustryDependencies()

    val distributor = "2.6.1"
    compileOnly("fr.xpdustry:distributor-core:$distributor")
    testImplementation("fr.xpdustry:distributor-core:$distributor")

    implementation("org.java-websocket:Java-WebSocket:1.5.3")
    implementation("com.google.inject:guice:5.1.0")
    implementation("com.auth0:java-jwt:3.19.2")
    implementation("org.slf4j:slf4j-simple:1.7.36")

    val junit = "5.8.2"
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junit")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit")
    testImplementation(kotlin("test-junit"))

    val jetbrains = "23.0.0"
    compileOnly("org.jetbrains:annotations:$jetbrains")
    testCompileOnly("org.jetbrains:annotations:$jetbrains")
}

// Required if you want to use the Release GitHub action
tasks.create("getArtifactPath") {
    doLast { println(tasks.shadowJar.get().archiveFile.get().toString()) }
}

tasks.shadowJar {
    doFirst {
        val temp = temporaryDir.resolve("plugin.json")
        temp.writeText(metadata.toJson(true))
        from(temp)
    }
    val destination = "fr.xpdustry.javelin.internal"
    relocate("com.google.inject", "$destination.guice")
    relocate("com.fasterxml.jackson", "$destination.jackson")
    relocate("org.slf4j", "$destination.slf4j")
    minimize()
}

tasks.build.get().dependsOn(tasks.shadowJar)

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

    publishReleasesTo("xpdustry", "https://repo.xpdustry.fr/releases")
    publishSnapshotsTo("xpdustry", "https://repo.xpdustry.fr/snapshots")

    gpl3OnlyLicense()

    if (metadata.repo.isNotBlank()) {
        val repo = metadata.repo.split("/")
        github(repo[0], repo[1]) {
            ci(true)
            issues(true)
            scm(true)
        }
    }

    configurePublications {
        pom {
            developers {
                developer { id.set(metadata.author) }
            }
        }
    }
}
