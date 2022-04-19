package fr.xpdustry.javelin.internal

import org.aeonbits.owner.Accessible
import org.aeonbits.owner.Config
import java.net.URI

interface JavelinClientConfig : Accessible {
    @get:Config.DefaultValue("false")
    @get:Config.Key("javelin.client.enabled")
    val enabled: Boolean

    @get:Config.DefaultValue("ws://localhost:8080")
    @get:Config.Key("javelin.client.host")
    val host: URI

    @get:Config.DefaultValue("insert-token-here")
    @get:Config.Key("javelin.client.token")
    val token: String

    @get:Config.DefaultValue("false")
    @get:Config.Key("javelin.client.wss")
    val wss: Boolean

    @get:Config.DefaultValue("60")
    @get:Config.Key("javelin.client.timeout")
    val timeout: Int
}
