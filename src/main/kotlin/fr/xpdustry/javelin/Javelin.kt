package fr.xpdustry.javelin

import arc.Core
import cloud.commandframework.annotations.AnnotationParser
import cloud.commandframework.arguments.standard.StringArgument
import cloud.commandframework.services.State
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Provides
import fr.xpdustry.distributor.Distributor
import fr.xpdustry.distributor.command.ArcCommandManager
import fr.xpdustry.distributor.command.ArcMeta
import fr.xpdustry.distributor.command.sender.ArcCommandSender
import fr.xpdustry.distributor.message.MessageIntent
import fr.xpdustry.distributor.plugin.AbstractPlugin
import fr.xpdustry.javelin.command.JavelinClientCommand
import fr.xpdustry.javelin.command.JavelinServerCommand
import fr.xpdustry.javelin.internal.JavelinClientConfig
import fr.xpdustry.javelin.internal.JavelinServerConfig
import fr.xpdustry.javelin.repository.ClientRepository
import fr.xpdustry.javelin.service.chat.GlobalChatContext
import fr.xpdustry.javelin.service.chat.GlobalChatFormatter
import fr.xpdustry.javelin.service.chat.GlobalChatService
import fr.xpdustry.javelin.service.chat.JavelinGlobalChatService

import fr.xpdustry.javelin.util.invoke
import fr.xpdustry.javelin.util.formatter
import fr.xpdustry.javelin.util.typeToken
import mindustry.gen.Call
import net.mindustry_ddns.store.FileStore
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.KeyGenerator

@Suppress("UNUSED")
class Javelin : AbstractPlugin() {
    companion object {
        @JvmStatic
        lateinit var client: JavelinClient
            private set

        @JvmStatic
        lateinit var server: JavelinServer
            private set
    }

    private lateinit var clientStore: FileStore<JavelinClientConfig>
    private lateinit var serverStore: FileStore<JavelinServerConfig>
    private lateinit var injector: Injector

    override fun init() {
        clientStore = getStoredConfig("client-config", JavelinClientConfig::class.java)
        serverStore = getStoredConfig("server-config", JavelinServerConfig::class.java)
        injector = Guice.createInjector(JavelinModule())

        server = injector.getInstance(JavelinServer::class.java)
        client = injector.getInstance(JavelinClient::class.java)

        Distributor.getServicePipeline().registerServiceType(
            typeToken<GlobalChatService>(),
            JavelinGlobalChatService()
        )

        if (serverStore.get().enabled) {
            Core.app.addListener(server)
        }

        if (clientStore.get().enabled) {
            Core.app.addListener(client)
        }
    }

    override fun registerClientCommands(manager: ArcCommandManager) {
        manager.command(manager.commandBuilder("global", "g")
            .meta(ArcMeta.DESCRIPTION, "Send a message globally to all servers.")
            .meta(ArcMeta.PARAMETERS, "<message...>")
            .argument(StringArgument.greedy("message"))
            .handler {
                val context = GlobalChatContext(it.sender.player.name(), it["message"])
                val result = try {
                    Distributor.getServicePipeline().pump(context).through(typeToken<GlobalChatService>()).result
                } catch (e: Exception) {
                    State.REJECTED
                }

                if (result == State.ACCEPTED) {
                    Call.sendMessage(GlobalChatFormatter.instance.format(context))
                } else {
                    it.sender.sendMessage(it.sender.formatter.invoke(
                        "Failed to broadcast the message. Please, report it to the server owner.", MessageIntent.ERROR
                    ))
                }
            }
        )
    }

    override fun registerServerCommands(manager: ArcCommandManager) {
        val annotations = AnnotationParser(manager, ArcCommandSender::class.java) { manager.createDefaultCommandMeta() }
        annotations.parse(injector.getInstance(JavelinServerCommand::class.java))
        annotations.parse(injector.getInstance(JavelinClientCommand::class.java))

        manager.command(manager.commandBuilder("javelin").literal("server").literal("generate-secret")
            .meta(ArcMeta.DESCRIPTION, "Generates a secret key for your server, be aware that changing it revokes all you server tokens.")
            .handler {
                val generator = KeyGenerator.getInstance("HmacSHA256")
                val encoded = generator.generateKey().encoded
                val secret = Base64.getEncoder().encode(encoded).toString(StandardCharsets.UTF_8)
                it.sender.sendMessage("SECRET: $secret")
            }
        )
    }

    private inner class JavelinModule : AbstractModule() {
        @get:Provides
        val clientConfig: JavelinClientConfig
            get() = clientStore.get()

        @get:Provides
        val serverConfig: JavelinServerConfig
            get() = serverStore.get()

        override fun configure() {
            bind(ClientRepository::class.java).toInstance(ClientRepository.local(directory.child("clients.json")))
        }
    }
}