package fr.xpdustry.javelin

import arc.Core
import cloud.commandframework.annotations.AnnotationParser
import cloud.commandframework.arguments.standard.EnumArgument
import cloud.commandframework.arguments.standard.StringArgument
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import fr.xpdustry.distributor.command.ArcCommandManager
import fr.xpdustry.distributor.command.ArcMeta
import fr.xpdustry.distributor.command.sender.ArcCommandSender
import fr.xpdustry.distributor.plugin.AbstractPlugin
import fr.xpdustry.javelin.chat.JavelinWhisperService
import fr.xpdustry.javelin.chat.WhisperService
import fr.xpdustry.javelin.chat.WhisperCommand
import fr.xpdustry.javelin.core.JavelinClient
import fr.xpdustry.javelin.core.JavelinServer
import fr.xpdustry.javelin.core.model.Scope
import fr.xpdustry.javelin.internal.JavelinServerConfig
import fr.xpdustry.javelin.core.repository.ServerRepository
import fr.xpdustry.javelin.internal.JavelinClientConfig
import fr.xpdustry.javelin.util.servicePipeline
import fr.xpdustry.javelin.util.typeToken
import net.mindustry_ddns.store.FileStore
import java.util.*
import javax.inject.Provider

class Javelin : AbstractPlugin() {
    companion object {
        @JvmStatic
        val server: JavelinServer
            get() = injector.getInstance(JavelinServer::class.java)

        @JvmStatic
        val client: JavelinClient
            get() = injector.getInstance(JavelinClient::class.java)

        private lateinit var injector: Injector
    }

    private lateinit var serverStore: FileStore<JavelinServerConfig>
    private lateinit var clientStore: FileStore<JavelinClientConfig>
    private lateinit var algorithm: Algorithm

    private fun createAccessToken(name: String): String = JWT
        .create()
        .withSubject(name)
        .withIssuedAt(Date())
        .sign(algorithm)

    override fun init() {
        serverStore = getStoredConfig("server-config", JavelinServerConfig::class.java)
        clientStore = getStoredObject("client-config", JavelinClientConfig::class.java, ::JavelinClientConfig)

        algorithm = Algorithm.HMAC256(serverStore.get().secret)
        injector = Guice.createInjector(JavelinModule())

        servicePipeline.registerServiceType(typeToken<WhisperService>(), WhisperService.local())

        if (serverStore.get().server) {
            Core.app.addListener(server)
        }

        if (clientStore.get().hosts.isNotEmpty()) {
            Core.app.addListener(client)
            servicePipeline.registerServiceImplementation(
                typeToken<WhisperService>(),
                injector.getInstance(JavelinWhisperService::class.java),
                listOf()
            )
        }
    }

    override fun registerServerCommands(manager: ArcCommandManager) {
        manager.command(manager.commandBuilder("javelin-add-server")
            .meta(ArcMeta.DESCRIPTION, "Add a new server to the server repository")
            .argument(StringArgument.of("name"))
            .argument(EnumArgument.optional(Scope::class.java, "scope", Scope.PUBLIC))
            .handler {
                val repository = injector.getInstance(ServerRepository::class.java)
                val server = repository.addServer(it["name"], createAccessToken(it["name"]), it["scope"])
                if (server != null) {
                    it.sender.sendMessage("Added server ${it.get<String>("name")}.")
                } else {
                    it.sender.sendMessage("The server ${it.get<String>("name")} already exist.")
                }
            }
        )
    }

    override fun registerClientCommands(manager: ArcCommandManager) {
        val annotations = AnnotationParser(manager, ArcCommandSender::class.java) { manager.createDefaultCommandMeta() }
        annotations.parse(WhisperCommand())
    }

    private inner class JavelinModule : AbstractModule() {
        override fun configure() {
            bind(JavelinServerConfig::class.java).toProvider(Provider { serverStore.get() })
            bind(JWTVerifier::class.java).toInstance(JWT.require(algorithm).build())
            bind(ServerRepository::class.java).toInstance(ServerRepository.local(directory.child("servers.json")))
            bind(JavelinClientConfig::class.java).toProvider(Provider { clientStore.get() })
        }
    }
}