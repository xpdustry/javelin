package fr.xpdustry.javelin.service

import arc.Core
import arc.util.Strings
import cloud.commandframework.services.ExecutionOrder
import cloud.commandframework.services.State
import fr.xpdustry.javelin.JavelinClient
import fr.xpdustry.javelin.JavelinMessage
import fr.xpdustry.javelin.MessageHandler
import fr.xpdustry.javelin.model.Endpoint
import mindustry.gen.Groups
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

class JavelinWhisperService @Inject constructor(private val client: JavelinClient) : WhisperService, MessageHandler {
    companion object {
        var idGen = AtomicInteger()
        val ENDPOINT = Endpoint("xpdustry-javelin", "whisper")
    }

    private val futures = mutableMapOf<Int, CompletableFuture<Boolean>>()

    init {
        client.registerMessageHandler(this)
    }

    override val endpoint: Endpoint
        get() = ENDPOINT

    override fun handle(context: WhisperContext): State {
        val future = CompletableFuture<Boolean>()
        val request = JavelinWhisperRequest(context, idGen.getAndIncrement())
        futures[request.id] = future
        client.broadcast(ENDPOINT, request)
        val received = runCatching { future.get(2L, TimeUnit.SECONDS) }
        futures -= request.id
        return if (received.getOrDefault(false)) State.ACCEPTED else State.REJECTED
    }

    override fun onMessageReceive(message: JavelinMessage, content: Any) {
        when (content) {
            is JavelinWhisperResponse -> futures.remove(content.id)?.complete(true)

            is JavelinWhisperRequest -> {
                val player = Groups.player.find {
                    Strings.stripColors(content.context.receiver) == Strings.stripColors(it.name())
                }
                if (player != null) {
                    Core.app.post { player.sendMessage(WhisperFormatter.instance.format(content.context)) }
                    client.send(ENDPOINT, JavelinWhisperResponse(content.id), message.sender)
                }
            }
        }
    }

    override fun order(): ExecutionOrder = ExecutionOrder.LAST

    private data class JavelinWhisperRequest(val context: WhisperContext, val id: Int)

    private data class JavelinWhisperResponse(val id: Int)
}
