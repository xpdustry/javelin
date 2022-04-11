package fr.xpdustry.javelin

import arc.Core
import cloud.commandframework.annotations.AnnotationParser
import com.auth0.jwt.algorithms.Algorithm
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Provides
import fr.xpdustry.distributor.command.ArcCommandManager
import fr.xpdustry.distributor.command.ArcMeta
import fr.xpdustry.distributor.command.sender.ArcCommandSender
import fr.xpdustry.distributor.plugin.AbstractPlugin
import fr.xpdustry.javelin.internal.JavelinClientConfig
import fr.xpdustry.javelin.internal.JavelinServerConfig
import fr.xpdustry.javelin.internal.ServerRepositoryCommand
import fr.xpdustry.javelin.repository.ServerRepository
import fr.xpdustry.javelin.util.servicePipeline
import fr.xpdustry.javelin.util.typeToken
import fr.xpdustry.javelin.whisper.JavelinWhisperService
import fr.xpdustry.javelin.whisper.WhisperCommand
import fr.xpdustry.javelin.whisper.WhisperService
import net.mindustry_ddns.store.FileStore
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.KeyGenerator

@Suppress("UNUSED")
class Javelin : AbstractPlugin() {
    companion object {
        @JvmStatic
        val client: JavelinClient?
            get() = Core.app.listeners.find { it is JavelinClient } as JavelinClient?

        @JvmStatic
        val server: JavelinServer?
            get() = Core.app.listeners.find { it is JavelinServer } as JavelinServer?
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
            Core.app.addListener(injector.getInstance(JavelinServer::class.java))
        }

        if (clientStore.get().enabled) {
            Core.app.addListener(injector.getInstance(JavelinClient::class.java))
            servicePipeline.registerServiceImplementation(
                typeToken<WhisperService>(),
                injector.getInstance(JavelinWhisperService::class.java),
                emptyList()
            )
        }
    }

    override fun registerClientCommands(manager: ArcCommandManager) {
        val annotations = AnnotationParser(manager, ArcCommandSender::class.java) { manager.createDefaultCommandMeta() }
        annotations.parse(WhisperCommand())
    }

    override fun registerServerCommands(manager: ArcCommandManager) {
        val annotations = AnnotationParser(manager, ArcCommandSender::class.java) { manager.createDefaultCommandMeta() }
        if (serverStore.get().enabled) annotations.parse(injector.getInstance(ServerRepositoryCommand::class.java))

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
        val clientConfig: JavelinServerConfig
            get() = serverStore.get()

        @get:Provides
        val serverConfig: JavelinClientConfig
            get() = clientStore.get()

        override fun configure() {
            if (serverStore.get().enabled) {
                bind(Algorithm::class.java).toInstance(Algorithm.HMAC256(serverStore.get().secret))
                bind(ServerRepository::class.java).toInstance(ServerRepository.local(directory.child("servers.json")))
            }
        }
    }
}