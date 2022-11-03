plugins {
    id("javelin.base-conventions")
}

dependencies {
    implementation("net.kyori:event-api:3.0.0")
    implementation("org.java-websocket:Java-WebSocket:1.5.3")
    implementation("com.esotericsoftware.kryo:kryo5:5.3.0")
    implementation("com.password4j:password4j:1.6.2")
    testRuntimeOnly("org.slf4j:slf4j-simple:1.7.36")
    testImplementation("org.assertj:assertj-core:3.23.1")
}
