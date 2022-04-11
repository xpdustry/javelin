package fr.xpdustry.javelin.internal

import org.aeonbits.owner.Accessible
import org.aeonbits.owner.Config.DefaultValue
import org.aeonbits.owner.Config.Key

interface JavelinConfig : Accessible {
    @get:DefaultValue("false")
    @get:Key("javelin.server.enabled")
    val server: Boolean

    @get:DefaultValue("8080")
    @get:Key("javelin.server.port")
    val port: Int

    @get:DefaultValue("your-246bits-secret")
    @get:Key("javelin.server.secret")
    val secret: String

    @get:DefaultValue("false")
    @get:Key("javelin.client.enabled")
    val client: Boolean

    @get:DefaultValue("ws://localhost:8080")
    @get:Key("javelin.client.host")
    val host: String

    @get:DefaultValue("your-token-issued-by-the-main-server")
    @get:Key("javelin.client.token")
    val token: String
}