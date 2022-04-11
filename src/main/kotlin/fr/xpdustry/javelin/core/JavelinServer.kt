package fr.xpdustry.javelin.core

import arc.ApplicationListener
import arc.Events
import arc.util.Log
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.JWTVerifier
import com.google.gson.Gson
import fr.xpdustry.javelin.core.model.Server
import fr.xpdustry.javelin.core.repository.ServerRepository
import fr.xpdustry.javelin.internal.JavelinServerConfig
import fr.xpdustry.javelin.util.fromJson
import org.java_websocket.WebSocket
import org.java_websocket.drafts.Draft
import org.java_websocket.exceptions.InvalidDataException
import org.java_websocket.framing.CloseFrame
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.handshake.ServerHandshakeBuilder
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JavelinServer @Inject constructor(
    private val config: JavelinServerConfig,
    private val verifier: JWTVerifier,
    private val repository: ServerRepository
) : ApplicationListener {
    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        val AUTHORIZATION_REGEX = Regex("^Bearer .+$")
    }

    private val gson = Gson()
    private val server = JavelinWebSocketServer()

    override fun init() {
        server.start()
    }

    override fun dispose() {
        server.stop(5000)
    }

    private inner class JavelinWebSocketServer : WebSocketServer(InetSocketAddress(config.port)) {
        private val servers = mutableMapOf<WebSocket, Server>()

        @Throws(InvalidDataException::class)
        override fun onWebsocketHandshakeReceivedAsServer(
            connection: WebSocket,
            draft: Draft,
            handshake: ClientHandshake
        ): ServerHandshakeBuilder {
            val builder = super.onWebsocketHandshakeReceivedAsServer(connection, draft, handshake)
            val authorization = handshake.getFieldValue(AUTHORIZATION_HEADER)

            if (handshake.resourceDescriptor != "/" || !authorization.matches(AUTHORIZATION_REGEX)) {
                throw InvalidDataException(CloseFrame.POLICY_VALIDATION, "Invalid!")
            }

            val token = authorization.split(' ', limit = 2)[1]

            try {
                val verified = verifier.verify(token)
                val server = repository.getServer(verified.subject)

                if (server == null || servers.values.contains(server) || server.token != token) {
                    throw InvalidDataException(CloseFrame.POLICY_VALIDATION, "Invalid token!!")
                } else {
                    servers[connection] = server
                    return builder
                }
            } catch (e: JWTVerificationException) {
                Log.info("Verification $e $token")
                throw InvalidDataException(CloseFrame.POLICY_VALIDATION, "Unauthorized!!")
            }
        }

        override fun onStart() {
            Log.info("JAVELIN-SERVER: Opened javelin server at port @.", config.port)
        }

        override fun onOpen(connection: WebSocket, handshake: ClientHandshake) {
            Log.info("JAVELIN-SERVER: @ server has connected.", servers[connection]!!.name)
        }

        override fun onClose(connection: WebSocket, code: Int, reason: String, remote: Boolean) {
            if (!remote) {
                Log.info("JAVELIN-SERVER: @ server has unexpectedly disconnected: @", servers[connection]!!.name, reason)
            } else {
                Log.info("JAVELIN-SERVER: @ server has disconnected: @", servers[connection]!!.name, reason)
            }
            servers -= connection
        }

        override fun onMessage(connection: WebSocket, message: String) {
            val parsed = gson.fromJson<JavelinMessage>(message)
            if (parsed.target == MessageTarget.SERVER) {
                try {
                    val clazz = Class.forName(parsed.clazz)
                    Events.fire(gson.fromJson(parsed.content, clazz))
                } catch (e: ClassNotFoundException) {
                    Log.debug("JAVELIN-SERVER: Failed to find the class ${parsed.clazz}.")
                }
            } else {
                connections.forEach {
                    if (it != connection && servers[connection]!!.scope >= parsed.scope) it.send(message)
                }
            }
        }

        override fun onMessage(connection: WebSocket, message: ByteBuffer) {
            connection.close(CloseFrame.REFUSE)
        }

        override fun onError(connection: WebSocket?, ex: Exception) {
            if (connection == null) {
                Log.err("JAVELIN-SERVER: An exception has occurred in the javelin server.", ex)
            } else {
                Log.err("JAVELIN-SERVER: An exception has occurred in the ${servers[connection]!!.name} remote client.", ex)
            }
        }
    }
}