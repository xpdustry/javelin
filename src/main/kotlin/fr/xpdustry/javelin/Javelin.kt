package fr.xpdustry.javelin

import arc.Core
import arc.util.CommandHandler
import arc.util.Log
import arc.util.Strings
import cloud.commandframework.annotations.AnnotationParser
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import fr.xpdustry.distributor.command.ArcCommandManager
import fr.xpdustry.distributor.command.sender.ArcCommandSender
import fr.xpdustry.distributor.plugin.AbstractPlugin
import fr.xpdustry.javelin.chat.MessageService
import fr.xpdustry.javelin.chat.MessageCommand
import fr.xpdustry.javelin.core.JavelinClient
import fr.xpdustry.javelin.core.JavelinServer
import fr.xpdustry.javelin.internal.JavelinConfig
import fr.xpdustry.javelin.core.repository.ServerRepository
import fr.xpdustry.javelin.chat.MessageContext
import fr.xpdustry.javelin.core.JavelinEvent
import fr.xpdustry.javelin.util.servicePipeline
import fr.xpdustry.javelin.util.typeToken
import mindustry.gen.Groups
import net.mindustry_ddns.store.FileStore
import java.util.*

class Javelin : AbstractPlugin() {
    companion object {
        @JvmStatic
        val server: JavelinServer?
            get() = Core.app.listeners.find { it is JavelinServer } as JavelinServer?

        @JvmStatic
        val client: JavelinClient?
            get() = Core.app.listeners.find { it is JavelinClient } as JavelinClient?
    }

    private lateinit var store: FileStore<JavelinConfig>
    private lateinit var injector: Injector
    private lateinit var algorithm: Algorithm

    private fun createAccessToken(name: String): String = JWT
        .create()
        .withSubject(name)
        .withIssuedAt(Date())
        .sign(algorithm)

    override fun init() {
        store = getStoredConfig("config", JavelinConfig::class.java)
        algorithm = Algorithm.HMAC256(store.get().secret)
        injector = Guice.createInjector(JavelinModule())

        Log.info("AAAAAAAAAAA @", JavelinMessageService.JavelinChatMessageEvent::class.java.name)
        Log.info("BBBB @", JavelinMessageService.JavelinChatMessageEvent::class.java.canonicalName)
        Log.info("CCCCCC @", JavelinMessageService.JavelinChatMessageEvent::class.java.typeName)

        servicePipeline.registerServiceType(typeToken<MessageService>(), MessageService.local())

        if (store.get().server) {
            val server = injector.getInstance(JavelinServer::class.java)
            Core.app.addListener(server)
        }

        if (store.get().client) {
            val client = injector.getInstance(JavelinClient::class.java)
            Core.app.addListener(client)
            servicePipeline.registerServiceImplementation(typeToken<MessageService>(), JavelinMessageService(client), emptyList())
        }
    }

    override fun registerServerCommands(handler: CommandHandler) {
        handler.register("javelin-add-server", "<name>", "wow") { args ->
            val repository = injector.getInstance(ServerRepository::class.java)
            val server = repository.addServer(args[0], createAccessToken(args[0]))

            if (server != null) {
                Log.info("Added server ${args[0]}.")
            } else {
                Log.info("The server ${args[0]} already exist.")
            }
        }
    }

    override fun registerClientCommands(manager: ArcCommandManager) {
        val annotations = AnnotationParser(manager, ArcCommandSender::class.java) { manager.createDefaultCommandMeta() }
        annotations.parse(MessageCommand())
    }

    private inner class JavelinModule : AbstractModule() {
        override fun configure() {
            bind(JavelinConfig::class.java).toInstance(store.get())
            bind(JWTVerifier::class.java).toInstance(JWT.require(algorithm).build())
            bind(ServerRepository::class.java).toInstance(ServerRepository.local(directory.child("servers.json")))
        }
    }

    private class JavelinMessageService(private val client: JavelinClient) : MessageService {
        init {
            client.setHandler(JavelinChatMessageEvent::class.java) { event ->
                Groups.player.find { Strings.stripColors(event.context.receiver) == Strings.stripColors(it.name()) }?.sendMessage(event.context.message)
            }
        }

        override fun accept(context: MessageContext) {
            client.broadcastEvent(JavelinChatMessageEvent(context))
        }

        data class JavelinChatMessageEvent(val context: MessageContext) : JavelinEvent
    }
}