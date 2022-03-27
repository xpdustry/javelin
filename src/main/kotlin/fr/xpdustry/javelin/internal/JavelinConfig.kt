package fr.xpdustry.javelin.internal

import org.aeonbits.owner.Accessible
import org.aeonbits.owner.Config.DefaultValue
import org.aeonbits.owner.Config.Key

interface JavelinConfig : Accessible {

    @get:Key("javelin.secret")
    val secretKey: String

    @get:DefaultValue("unknown")
    @get:Key("javelin.issuer-name")
    val issuer: String

    @get:DefaultValue("8080")
    @get:Key("javelin.server.port")
    val port: Int

    @get:DefaultValue("false")
    @get:Key("javelin.server.enabled")
    val server: Boolean
}