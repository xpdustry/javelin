package fr.xpdustry.javelin

import arc.ApplicationListener
import arc.util.Log
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.google.gson.GsonBuilder
import com.google.inject.ImplementedBy
import fr.xpdustry.javelin.internal.JavelinServerConfig
import fr.xpdustry.javelin.model.Client
import fr.xpdustry.javelin.model.registerEndpointTypeAdapter
import fr.xpdustry.javelin.repository.ClientRepository
import fr.xpdustry.javelin.util.fromJson
import fr.xpdustry.javelin.util.getDraft
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

@ImplementedBy(SimpleJavelinServer::class)
sealed interface JavelinServer : ApplicationListener {
    fun isConnected(client: Client): Boolean
}

@Singleton
private class SimpleJavelinServer @Inject constructor(
    private val config: JavelinServerConfig,
    private val repository: ClientRepository
) : JavelinServer, WebSocketServer(InetSocketAddress(config.port), config.workers, listOf(getDraft(config.wss))) {
    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        val AUTHORIZATION_REGEX = Regex("^Bearer .+$")
    }

    init {
        isReuseAddr = true
    }

    private val verifier = JWT.require(Algorithm.HMAC256(config.secret)).build()

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerEndpointTypeAdapter()
        .create()

    override fun init() {
        start()
    }

    override fun dispose() {
        stop(5000)
    }

    @Throws(InvalidDataException::class)
    override fun onWebsocketHandshakeReceivedAsServer(
        connection: WebSocket,
        draft: Draft,
        handshake: ClientHandshake
    ): ServerHandshakeBuilder {
        Log.debug("JAVELIN-SERVER: Receiving connection from @.", connection.remoteSocketAddress)

        val builder = super.onWebsocketHandshakeReceivedAsServer(connection, draft, handshake)
        val authorization = handshake.getFieldValue(AUTHORIZATION_HEADER)

        if (handshake.resourceDescriptor != config.path) {
            Log.debug("JAVELIN-SERVER: Rejecting connection @ (Invalid resource descriptor: @).", connection.remoteSocketAddress)
            throw InvalidDataException(CloseFrame.POLICY_VALIDATION)
        }

        if (!authorization.matches(AUTHORIZATION_REGEX)) {
            Log.debug("JAVELIN-SERVER: Rejecting connection @ (Invalid authorization header: @).", connection.remoteSocketAddress)
            throw InvalidDataException(CloseFrame.POLICY_VALIDATION)
        }

        val token = authorization.split(' ', limit = 2)[1]

        try {
            val verified = verifier.verify(token)
            val client = repository[verified.subject!!]

            if (client == null || client.token != token) {
                Log.debug("JAVELIN-SERVER: Rejecting connection @ (Invalid token).", connection.remoteSocketAddress)
                throw InvalidDataException(CloseFrame.POLICY_VALIDATION)
            }

            if (isConnected(client)) {
                Log.debug("JAVELIN-SERVER: Rejecting connection @ (Already connected).", connection.remoteSocketAddress)
                throw InvalidDataException(CloseFrame.POLICY_VALIDATION)
            }

            connection.setAttachment(client)
            return builder
        } catch (e: JWTVerificationException) {
            Log.debug("JAVELIN-SERVER: Rejecting connection @ (Invalid token).", connection.remoteSocketAddress)
            throw InvalidDataException(CloseFrame.POLICY_VALIDATION)
        }
    }

    override fun onStart() {
        Log.info("JAVELIN-SERVER: Opened javelin server at port @.", config.port)
    }

    override fun onOpen(con: WebSocket, handshake: ClientHandshake) {
        Log.info("JAVELIN-SERVER: @ server has connected.", con.client.name)
    }

    override fun onClose(connection: WebSocket, code: Int, reason: String, remote: Boolean) {
        if (!remote) {
            Log.info("JAVELIN-SERVER: @ server has unexpectedly disconnected: @", connection.client.name, reason)
        } else {
            Log.info("JAVELIN-SERVER: @ server has disconnected: @", connection.client.name, reason)
        }
    }

    override fun onMessage(connection: WebSocket, incoming: String) {
        val message = gson.fromJson<JavelinMessage>(incoming)
        Log.debug("JAVELIN-SERVER: Incoming message from ${connection.client.name} > $incoming.")

        if (message.receiver == null) {
            broadcast(incoming, connections.minus(connection).filter { message.endpoint in it.client.endpoints })
        } else {
            connections
                .find { it.client.name == message.receiver && message.endpoint in it.client.endpoints }
                ?.send(incoming)
        }
    }

    override fun onMessage(connection: WebSocket, message: ByteBuffer) {
        connection.close(CloseFrame.REFUSE)
    }

    override fun onError(connection: WebSocket?, ex: Exception) {
        if (connection == null) {
            Log.err("JAVELIN-SERVER: An exception has occurred in the javelin server.", ex)
        } else {
            Log.err("JAVELIN-SERVER: An exception has occurred in the ${connection.client.name} remote client.", ex)
        }
    }

    override fun isConnected(client: Client): Boolean =
        connections.find { it.client == client } != null

    private val WebSocket.client: Client
        get() = getAttachment()
}