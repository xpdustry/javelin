package fr.xpdustry.javelin

import arc.Core
import arc.util.Strings
import cloud.commandframework.annotations.AnnotationParser
import cloud.commandframework.arguments.standard.StringArgument
import cloud.commandframework.services.State
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Provides
import fr.xpdustry.distributor.command.ArcCommandManager
import fr.xpdustry.distributor.command.ArcMeta
import fr.xpdustry.distributor.command.sender.ArcCommandSender
import fr.xpdustry.distributor.message.MessageIntent
import fr.xpdustry.distributor.plugin.AbstractPlugin
import fr.xpdustry.javelin.command.ServerCommand
import fr.xpdustry.javelin.internal.JavelinClientConfig
import fr.xpdustry.javelin.internal.JavelinServerConfig
import fr.xpdustry.javelin.repository.ClientRepository
import fr.xpdustry.javelin.service.JavelinWhisperService
import fr.xpdustry.javelin.service.WhisperContext
import fr.xpdustry.javelin.service.WhisperFormatter
import fr.xpdustry.javelin.service.WhisperService
import fr.xpdustry.javelin.util.format
import fr.xpdustry.javelin.util.formatter
import fr.xpdustry.javelin.util.servicePipeline
import fr.xpdustry.javelin.util.typeToken
import net.mindustry_ddns.store.FileStore
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.KeyGenerator

@Suppress("UNUSED")
class Javelin : AbstractPlugin() {
    companion object {
        @JvmStatic
        var client: JavelinClient? = null
            private set

        @JvmStatic
        var server: JavelinServer? = null
            private set
    }

    private lateinit var clientStore: FileStore<JavelinClientConfig>
    private lateinit var serverStore: FileStore<JavelinServerConfig>
    private lateinit var injector: Injector

    override fun init() {
        clientStore = getStoredConfig("client-config", JavelinClientConfig::class.java)
        serverStore = getStoredConfig("server-config", JavelinServerConfig::class.java)
        injector = Guice.createInjector(JavelinModule())

        servicePipeline.registerServiceType(typeToken<WhisperService>(), WhisperService.local())

        if (serverStore.get().enabled) {
            server = injector.getInstance(JavelinServer::class.java)
            Core.app.addListener(server)
        }

        if (clientStore.get().enabled) {
            client = injector.getInstance(JavelinClient::class.java)
            Core.app.addListener(client)
            servicePipeline.registerServiceImplementation(
                typeToken<WhisperService>(),
                injector.getInstance(JavelinWhisperService::class.java),
                emptyList()
            )
        }
    }

    override fun registerClientCommands(manager: ArcCommandManager) {
        manager.command(manager.commandBuilder("whisper", "w")
            .meta(ArcMeta.DESCRIPTION, "Whisper to somebody, I dunno...")
            .argument(StringArgument.quoted("receiver"))
            .argument(StringArgument.greedy("message"))
            .handler {
                if (Strings.stripColors(it.sender.player.name()) == Strings.stripColors(it["receiver"])) {
                    it.sender.sendMessage(it.sender.formatter.format(
                        "You can't message yourself.", MessageIntent.ERROR)
                    )
                } else {
                    val context = WhisperContext(it.sender.player.name(), it["receiver"], it["message"])
                    val result = try {
                        servicePipeline.pump(context).through(typeToken<WhisperService>()).result
                    } catch (e: Exception) {
                        State.REJECTED
                    }

                    if (result == State.ACCEPTED) {
                        it.sender.player.sendMessage(WhisperFormatter.instance.format(context))
                    } else {
                        it.sender.sendMessage(
                            it.sender.formatter.format(
                                "The player ${context.receiver} is not online.", MessageIntent.ERROR
                            )
                        )
                    }
                }
            }
        )
    }

    override fun registerServerCommands(manager: ArcCommandManager) {
        val annotations = AnnotationParser(manager, ArcCommandSender::class.java) { manager.createDefaultCommandMeta() }
        annotations.parse(injector.getInstance(ServerCommand::class.java))

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