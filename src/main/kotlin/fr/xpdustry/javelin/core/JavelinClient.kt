package fr.xpdustry.javelin.core

import arc.ApplicationListener
import arc.Events
import arc.util.Log
import arc.util.Timer
import arc.util.Timer.Task
import com.google.gson.Gson
import fr.xpdustry.javelin.core.model.Scope
import fr.xpdustry.javelin.internal.ConnectionData
import fr.xpdustry.javelin.internal.JavelinClientConfig
import fr.xpdustry.javelin.util.fromJson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JavelinClient @Inject constructor(private val config: JavelinClientConfig) : ApplicationListener {
    private val connections = mutableListOf<JavelinClientConnection>()
    private val gson = Gson()

    fun broadcastEvent(event: JavelinEvent, target: MessageTarget = MessageTarget.CLIENT, scope: Scope = Scope.PUBLIC) {
        val message = JavelinMessage(gson.toJson(event), event.javaClass.name, scope, target)
        val text = gson.toJson(message)
        connections.forEach { if (it.data.scope >= message.scope) it.send(text) }
    }

    private fun handleMessage(message: String) {
        val parsed = gson.fromJson<JavelinMessage>(message)
        try {
            val clazz = Class.forName(parsed.clazz)
            Events.fire(gson.fromJson(parsed.content, clazz))
        } catch (e: ClassNotFoundException) {
            Log.debug("JAVELIN-CLIENT: Failed to find the class ${parsed.clazz}.")
        }
    }

    override fun init() {
        for (data in config.hosts) {
            val task = object : Task() {
                override fun run() {
                    val connection = JavelinClientConnection(data)
                    if (connection.connectBlocking()) {
                        Log.info("JAVELIN-CLIENT: Successfully connected to ${data.host}")
                        connections += connection
                        cancel()
                    } else {
                        Log.info("JAVELIN-CLIENT: Failed to connect to ${data.host}, next try in 10 seconds.")
                    }
                }
            }
            Timer.schedule(task, 0F, 10F, 3)
        }
    }

    override fun dispose() {
        connections.forEach { it.closeBlocking() }
    }

    private inner class JavelinClientConnection(val data: ConnectionData) : WebSocketClient(URI(data.host), mapOf("Authorization" to "Bearer ${data.token}")) {
        override fun onOpen(handshakedata: ServerHandshake?) {
        }

        override fun onMessage(message: String) {
            Log.debug("JAVELIN-CLIENT: Received message from @ server. (@)", data.host, message)
            handleMessage(message)
        }

        override fun onClose(code: Int, reason: String, remote: Boolean) {
            if (remote) {
                Log.info("JAVELIN-CLIENT: The server @ unexpectedly closed the connection. (@: @)", data.host, code, reason)
            } else {
                Log.info("JAVELIN-CLIENT: The connection to @ has been closed. (@: @)", data.host, code, reason)
            }
        }

        override fun onError(ex: Exception) {
            Log.err("JAVELIN-CLIENT: An unexpected exception occurred.", ex)
        }
    }
}

interface JavelinEvent