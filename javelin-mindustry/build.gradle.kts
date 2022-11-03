import fr.xpdustry.toxopid.ModPlatform
import fr.xpdustry.toxopid.util.ModMetadata
import fr.xpdustry.toxopid.util.anukenJitpack
import fr.xpdustry.toxopid.util.mindustryDependencies

plugins {
    id("javelin.base-conventions")
    id("com.github.johnrengelman.shadow")
    id("fr.xpdustry.toxopid")
}

val metadata = ModMetadata.fromJson(file("${project.projectDir}/plugin.json"))
metadata.version = project.version.toString()
metadata.description = project.description.toString()

repositories {
    mavenCentral()
    anukenJitpack()
}

dependencies {
    api(project(":javelin-core"))
    mindustryDependencies()
    runtimeOnly("org.slf4j:slf4j-simple:1.7.36")
}

toxopid {
    compileVersion.set("v" + metadata.minGameVersion)
    platforms.add(ModPlatform.HEADLESS)
}

tasks.shadowJar {
    doFirst {
        val temp = temporaryDir.resolve("plugin.json")
        temp.writeText(metadata.toJson(true))
        from(temp)
    }
    from(rootProject.file("LICENSE.md")) {
        into("META-INF")
    }
    minimize {
        exclude("org.slf4j")
    }
    val target = "fr.xpdustry.javelin.shadow"
    relocate("org.java_websocket", "$target.java_websocket")
    relocate("net.kyori.event", "$target.event")
    relocate("org.slf4j", "$target.slf4j")
    relocate("com.esotericsoftware.kryo", "$target.kryo")
}

// For plugin publishing
tasks.register("getArtifactPath") {
    doLast { println(tasks.shadowJar.get().archiveFile.get().toString()) }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
