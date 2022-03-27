package fr.xpdustry.javelin

import arc.Events
import arc.util.CommandHandler
import arc.util.Log
import fr.xpdustry.distributor.plugin.AbstractPlugin
import fr.xpdustry.javelin.internal.JavelinConfig
import fr.xpdustry.javelin.server.JavelinServer
import fr.xpdustry.javelin.server.Server
import fr.xpdustry.javelin.server.ServerDatabase
import io.javalin.apibuilder.EndpointGroup
import mindustry.Vars
import mindustry.game.EventType.ServerLoadEvent
import net.mindustry_ddns.store.ConfigFileStore
import org.w3c.dom.events.Event
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.KeyGenerator


@Suppress("UNUSED")
class Javelin : AbstractPlugin() {

    private val database by lazy {
        ServerDatabase.simple(directory.child("servers.json"))
    }

    private val store by lazy {
        val file = directory.child("config.properties").path()
        val defaults = mapOf("javelin.secret" to generateSecretKey())
        ConfigFileStore.load(file, JavelinConfig::class.java, defaults)
    }

    private val config: JavelinConfig
        get() = store.get()

    override fun init() {
        val server = JavelinServer(database, config.issuer, config.port, config.secretKey)
        server.run()

        Events.on(ServerLoadEvent::class.java) {
            Vars.mods.list().forEach { mod ->
                if (mod.main != null && mod.main is EndpointGroup) {
                    (mod.main as EndpointGroup).addEndpoints()

                    // TODO Finish it
                }
            }
        }
    }

    override fun registerServerCommands(handler: CommandHandler) {
        handler.register("add-server", "<name> <password>", "Add a server") { args ->
            val server = database.addServer(args[0], args[1].toByteArray(), Server.ServerAccess.PUBLIC)
            Log.info("You successfully added the @ server. (access: @)", server.name, server.access)
        }
    }

    private fun generateSecretKey(): String {
        val generator = KeyGenerator.getInstance("HmacSHA256")
        val secret = generator.generateKey().encoded
        return Base64.getEncoder().encode(secret).toString(StandardCharsets.UTF_8)
    }
}