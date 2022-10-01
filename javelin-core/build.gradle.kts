plugins {
    id("javelin.base-conventions")
}

dependencies {
    implementation("net.kyori:event-api:5.0.0-SNAPSHOT")
    implementation("org.java-websocket:Java-WebSocket:1.5.3")
    implementation("com.esotericsoftware.kryo:kryo5:5.3.0")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.1")
}
