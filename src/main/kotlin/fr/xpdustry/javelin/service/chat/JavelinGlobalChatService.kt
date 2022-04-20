package fr.xpdustry.javelin.service.chat

import cloud.commandframework.services.State
import fr.xpdustry.javelin.Javelin
import fr.xpdustry.javelin.JavelinMessage
import fr.xpdustry.javelin.JavelinMessageHandler
import fr.xpdustry.javelin.model.Endpoint
import mindustry.gen.Call

class JavelinGlobalChatService : GlobalChatService, JavelinMessageHandler {
    companion object {
        val ENDPOINT = Endpoint("xpdustry-javelin", "global-chat")
    }

    init {
        Javelin.client.registerEndpoint(ENDPOINT, this)
    }

    override fun handle(context: GlobalChatContext): State {
        return if (Javelin.client.connected) {
            Javelin.client.broadcast(ENDPOINT, context)
            State.ACCEPTED
        } else {
            State.REJECTED
        }
    }

    override fun onMessageReceive(message: JavelinMessage, content: Any) {
        Call.sendMessage(GlobalChatFormatter.instance.format(content as GlobalChatContext))
    }
}
