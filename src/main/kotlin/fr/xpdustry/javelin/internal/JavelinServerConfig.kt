package fr.xpdustry.javelin.internal

import org.aeonbits.owner.Accessible
import org.aeonbits.owner.Config

interface JavelinServerConfig : Accessible {
    @get:Config.DefaultValue("false")
    @get:Config.Key("javelin.server.enabled")
    val enabled: Boolean

    @get:Config.DefaultValue("8080")
    @get:Config.Key("javelin.server.port")
    val port: Int

    @get:Config.DefaultValue("your-246bits-secret")
    @get:Config.Key("javelin.server.secret")
    val secret: String

    @get:Config.DefaultValue("1")
    @get:Config.Key("javelin.server.workers")
    val workers: Int

    @get:Config.DefaultValue("false")
    @get:Config.Key("javelin.server.https")
    val https: Boolean

    @get:Config.DefaultValue("/")
    @get:Config.Key("javelin.server.path")
    val path: String
}