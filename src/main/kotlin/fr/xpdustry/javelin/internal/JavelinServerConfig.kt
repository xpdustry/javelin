package fr.xpdustry.javelin.internal

import org.aeonbits.owner.Accessible
import org.aeonbits.owner.Config.DefaultValue
import org.aeonbits.owner.Config.Key

interface JavelinServerConfig : Accessible {
    @get:DefaultValue("false")
    @get:Key("javelin.server.enabled")
    val server: Boolean

    @get:DefaultValue("8080")
    @get:Key("javelin.server.port")
    val port: Int

    @get:DefaultValue("your-246bits-secret")
    @get:Key("javelin.server.secret")
    val secret: String
}