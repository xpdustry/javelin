plugins {
    id("javelin.base-conventions")
    id("javelin.mindustry-conventions")
}

dependencies {
    api(project(":javelin-core"))
    runtimeOnly("org.slf4j:slf4j-simple:1.7.36")
}

tasks.shadowJar {
    minimize {
        exclude("org.slf4j")
    }
    val target = "fr.xpdustry.javelin.shadow"
    relocate("org.java_websocket", "$target.java_websocket")
    relocate("net.kyori.event", "$target.event")
    relocate("org.slf4j", "$target.slf4j")
    relocate("com.esotericsoftware.kryo", "$target.kryo")
}
