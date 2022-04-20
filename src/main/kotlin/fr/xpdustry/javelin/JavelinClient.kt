package fr.xpdustry.javelin

import arc.ApplicationListener
import arc.util.Log
import com.google.gson.GsonBuilder
import com.google.inject.ImplementedBy
import fr.xpdustry.javelin.internal.JavelinClientConfig
import fr.xpdustry.javelin.model.Endpoint
import fr.xpdustry.javelin.model.registerEndpointTypeAdapter
import fr.xpdustry.javelin.util.fromJson
import fr.xpdustry.javelin.util.getDraft
import org.java_websocket.client.WebSocketClient
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.java_websocket.handshake.ServerHandshake
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Singleton

@ImplementedBy(SimpleJavelinClient::class)
sealed interface JavelinClient : ApplicationListener {
    val connected: Boolean
        @get:JvmName("isConnected") get

    fun restart()

    fun send(endpoint: Endpoint, message: Any, receiver: String)

    fun broadcast(endpoint: Endpoint, message: Any)

    fun registerEndpoint(endpoint: Endpoint, handler: JavelinMessageHandler)

    fun unregisterEndpoint(endpoint: Endpoint)
}

@Singleton
private class SimpleJavelinClient @Inject constructor(
    private val config: JavelinClientConfig
) : JavelinClient, WebSocketClient(config.host, getDraft(config.wss)) {
    private var connectTask: CompletableFuture<Void>? = null
    private val handlers = mutableMapOf<Endpoint, JavelinMessageHandler>()
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerEndpointTypeAdapter()
        .create()

    override val connected: Boolean
        get() = this.isOpen

    init {
        addHeader("Authorization", "Bearer ${config.token}")
        connectionLostTimeout = config.timeout
    }

    override fun init() {
        checkConnectTask()
        connectTask = CompletableFuture.runAsync {
            Log.info("JAVELIN-CLIENT: Connecting to ${config.host}...")
            if (!connectBlocking()) {
                Log.info("JAVELIN-CLIENT: Failed to connect to ${config.host}.")
            }
        }
    }

    override fun restart() {
        checkConnectTask()
        connectTask = CompletableFuture.runAsync {
            Log.info("JAVELIN-CLIENT: Reconnecting to ${config.host}...")
            if (!reconnectBlocking()) {
                Log.info("JAVELIN-CLIENT: Failed to reconnect to ${config.host}.")
            }
        }
    }

    override fun send(endpoint: Endpoint, message: Any, receiver: String) {
        sendMessage(endpoint, message, receiver)
    }

    override fun broadcast(endpoint: Endpoint, message: Any) {
        sendMessage(endpoint, message)
    }

    private fun sendMessage(endpoint: Endpoint, message: Any, receiver: String? = null) {
        Log.debug("JAVELIN-CLIENT: Sending ${if (receiver == null) "broadcast" else ""} message to endpoint $endpoint > $message.")
        try {
            send(gson.toJson(JavelinMessage(endpoint, gson.toJson(message), message.javaClass.name, "unknown", receiver)))
        } catch (ignored: WebsocketNotConnectedException) {
            Log.debug("JAVELIN-CLIENT: Failed to send a message because not connected...")
        }
    }

    override fun registerEndpoint(endpoint: Endpoint, handler: JavelinMessageHandler) {
        if (endpoint in handlers) throw IllegalStateException("The endpoint $endpoint has been registered twice.")
        handlers[endpoint] = handler
    }

    override fun unregisterEndpoint(endpoint: Endpoint) {
        handlers -= endpoint
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
        } catch (e: Exception) {
            Log.err("An unexpected exception occurred while handling a message for ${message.endpoint}.", e)
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

    override fun exit() {
        if (connected) close()
    }

    private fun checkConnectTask() {
        if (connectTask != null && !connectTask!!.isDone) connectTask!!.cancel(true)
    }
}
