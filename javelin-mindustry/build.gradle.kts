plugins {
    id("javelin.base-conventions")
    id("javelin.mindustry-conventions")
}

dependencies {
    api(project(":javelin-core"))
    runtimeOnly("org.slf4j:slf4j-simple:2.0.1")
}
