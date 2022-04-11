package fr.xpdustry.javelin.chat

import arc.Events
import arc.util.Strings
import fr.xpdustry.javelin.core.JavelinClient
import fr.xpdustry.javelin.core.JavelinEvent
import mindustry.gen.Groups
import javax.inject.Inject

class JavelinWhisperService @Inject constructor(private val client: JavelinClient) : WhisperService {
    init {
        Events.on(JavelinChatMessageEvent::class.java) { event ->
            Groups.player
                .find { Strings.stripColors(event.context.receiver) == Strings.stripColors(it.name()) }
                ?.sendMessage(event.context.formatted())
        }
    }

    override fun accept(context: WhisperContext) {
        client.broadcastEvent(JavelinChatMessageEvent(context))
    }

    data class JavelinChatMessageEvent(val context: WhisperContext) : JavelinEvent
}