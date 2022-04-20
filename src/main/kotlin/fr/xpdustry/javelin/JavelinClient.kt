package fr.xpdustry.javelin

import arc.ApplicationListener
import arc.util.Log
import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.interfaces.DecodedJWT
import com.google.gson.GsonBuilder
import com.google.inject.ImplementedBy
import fr.xpdustry.javelin.internal.JavelinClientConfig
import fr.xpdustry.javelin.model.Endpoint
import fr.xpdustry.javelin.model.registerEndpointTypeAdapter
import fr.xpdustry.javelin.util.fromJson
import fr.xpdustry.javelin.util.getDraft
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@ImplementedBy(SimpleJavelinClient::class)
sealed interface JavelinClient : ApplicationListener {
    fun send(endpoint: Endpoint, message: Any, receiver: String)

    fun broadcast(endpoint: Endpoint, message: Any)

    fun registerMessageHandler(handler: MessageHandler)

    fun unregisterMessageHandler(handler: MessageHandler)
}

@Singleton
private class SimpleJavelinClient @Inject constructor(
    private val config: JavelinClientConfig
) : JavelinClient, WebSocketClient(config.host, getDraft(config.wss)) {
    private val handlers = mutableMapOf<Endpoint, MessageHandler>()
    private val jwt: DecodedJWT
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerEndpointTypeAdapter()
        .create()

    init {
        addHeader("Authorization", "Bearer ${config.token}")
        connectionLostTimeout = config.timeout
        try {
            jwt = JWT.decode(config.token)
        } catch (e: JWTDecodeException) {
            throw RuntimeException("The client token is invalid.", e)
        }
    }

    override fun init() {
        Log.info("JAVELIN-CLIENT: Connecting to ${config.host}...")
        if (!connectBlocking(15, TimeUnit.SECONDS)) {
            Log.info("JAVELIN-CLIENT: Failed to connect to ${config.host}.")
        }
    }

    override fun send(endpoint: Endpoint, message: Any, receiver: String) {
        sendMessage(endpoint, message, receiver)
    }

    override fun broadcast(endpoint: Endpoint, message: Any) {
        sendMessage(endpoint, message)
    }

    private fun sendMessage(endpoint: Endpoint, message: Any, receiver: String? = null) {
        Log.debug("JAVELIN-CLIENT: Sending ${if (receiver == null) "broadcast" else ""} message to endpoint $endpoint $message.")
        send(gson.toJson(JavelinMessage(endpoint, gson.toJson(message), message.javaClass.name, jwt.subject, receiver)))
    }

    override fun registerMessageHandler(handler: MessageHandler) {
        if (handler.endpoint in handlers) throw IllegalStateException()
        handlers[handler.endpoint] = handler
    }

    override fun unregisterMessageHandler(handler: MessageHandler) {
        handlers -= handler.endpoint
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.info("JAVELIN-CLIENT: Successfully connected to ${config.host}")
    }

    override fun onMessage(incoming: String) {
        val message = gson.fromJson<JavelinMessage>(incoming)
        Log.debug("JAVELIN-CLIENT: Incoming message from ${message.sender} > $incoming.")

        try {
            val clazz = Class.forName(message.clazz)
            val content = gson.fromJson(message.content, clazz)
            handlers[message.endpoint]?.onMessageReceive(message, content)
        } catch (e: ClassNotFoundException) {
            Log.debug("Unhandled message class ${message.clazz} for ${message.endpoint}.")
        } catch (e: ClassCastException) {
            Log.err("Failed cast for typed endpoint ${message.endpoint}.", e)
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        if (remote) {
            Log.info("JAVELIN-CLIENT: The server @ unexpectedly closed the connection. (@: @)", config.host, code, reason)
        } else {
            Log.info("JAVELIN-CLIENT: The connection to @ has been closed. (@: @)", config.host, code, reason)
        }
    }

    override fun onError(ex: Exception) {
        Log.err("JAVELIN-CLIENT: An unexpected exception occurred.", ex)
    }

    override fun dispose() {
        if (!isClosed) closeBlocking()
    }
}
