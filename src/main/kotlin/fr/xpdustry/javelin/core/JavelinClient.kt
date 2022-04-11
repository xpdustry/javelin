package fr.xpdustry.javelin.core

import arc.ApplicationListener
import arc.util.Log
import com.google.gson.Gson
import fr.xpdustry.javelin.internal.JavelinConfig
import fr.xpdustry.javelin.util.fromJson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.reflect.Type
import java.net.URI
import javax.inject.Inject

class JavelinClient @Inject constructor(private val config: JavelinConfig) : ApplicationListener {
    private val gson = Gson()
    private val client = JavelinWebSocketClient()
    private val handlers = mutableMapOf<String, JavelinEventHandler<out JavelinEvent>>()

    fun <T : JavelinEvent> setHandler(clazz: Class<out T>, handler: JavelinEventHandler<T>) {
        Log.debug("Adding handler for class ${clazz.name}")
        handlers[clazz.name] = handler
    }

    fun broadcastEvent(event: JavelinEvent) {
        val message = JavelinMessage(gson.toJson(event), event.javaClass.name)
        Log.debug("JAVELIN-CLIENT: Sending message $message")
        client.send(gson.toJson(message))
    }

    override fun init() {
        Log.info("JAVELIN-CLIENT: Connecting to remote server @.", config.host)
        client.connectBlocking()
    }

    override fun dispose() {
        client.closeBlocking()
    }

    private inner class JavelinWebSocketClient : WebSocketClient(
        URI(config.host),
        mapOf("Authorization" to "Bearer ${config.token}")
    ) {
        override fun onOpen(handshakedata: ServerHandshake) {
            Log.info("JAVELIN-CLIENT: Logged to remote server @.", config.host)
        }

        override fun onMessage(message: String) {
            Log.debug("JAVELIN-CLIENT: received message $message")
            val parsed = gson.fromJson<JavelinMessage>(message)
            try {
                val clazz = Class.forName(parsed.clazz)
                handlers[clazz.name]?.onEvent(gson.fromJson(parsed.content, clazz as Type))
            } catch (e: ClassNotFoundException) {
                Log.debug("JAVELIN-CLIENT: Failed to find the class ${parsed.clazz} for incoming message.")
            }
        }

        override fun onClose(code: Int, reason: String, remote: Boolean) {
            if (remote) {
                Log.info("JAVELIN-CLIENT: The remote server @ has unexpectedly closed the connection: @.", config.host, reason)
            } else {
                Log.info("JAVELIN-CLIENT: Failed to connect to @ remote server: @", config.host, reason)
            }
        }

        override fun onError(ex: Exception) {
            Log.err("JAVELIN-CLIENT: An unexpected exception occurred.", ex)
        }
    }
}

fun interface JavelinEventHandler<T : JavelinEvent> {
    fun onEvent(event: T)
}

interface JavelinEvent