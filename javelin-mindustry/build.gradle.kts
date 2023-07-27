import fr.xpdustry.toxopid.dsl.mindustryDependencies

plugins {
    id("javelin.base-conventions")
    id("com.github.johnrengelman.shadow")
    id("fr.xpdustry.toxopid")
}

val metadata = fr.xpdustry.toxopid.spec.ModMetadata.fromJson(file("${project.projectDir}/plugin.json"))
metadata.version = project.version.toString()
metadata.description = project.description.toString()

toxopid {
    compileVersion.set("v" + metadata.minGameVersion)
    platforms.add(fr.xpdustry.toxopid.spec.ModPlatform.HEADLESS)
}

repositories {
    mavenCentral()
    maven("https://maven.xpdustry.com/anuken") {
        name = "xpdustry-anuken"
        mavenContent { releasesOnly() }
    }
}

dependencies {
    api(project(":javelin-core")) {
        exclude("org.slf4j")
    }
    mindustryDependencies()
    runtimeOnly("org.slf4j:slf4j-simple:1.7.36")
}

tasks.shadowJar {
    archiveFileName.set("javelin.jar")
    doFirst {
        val temp = temporaryDir.resolve("plugin.json")
        temp.writeText(metadata.toJson(true))
        from(temp)
    }
    from(rootProject.file("LICENSE.md")) {
        into("META-INF")
    }
    val target = "fr.xpdustry.javelin.shadow"
    relocate("com.google.common", "$target.google.common")
    relocate("org.java_websocket", "$target.java_websocket")
    relocate("net.kyori.event", "$target.event")
    relocate("org.slf4j", "$target.slf4j")
    relocate("com.esotericsoftware.kryo", "$target.kryo")
    relocate("com.password4j", "$target.password4j")
    mergeServiceFiles()
    minimize {
        exclude(dependency("org.slf4j:.*:.*"))
    }
}

// For plugin publishing
tasks.register("getArtifactPath") {
    doLast { println(tasks.shadowJar.get().archiveFile.get().toString()) }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.runMindustryClient {
    mods.setFrom()
}