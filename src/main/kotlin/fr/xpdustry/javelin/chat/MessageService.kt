package fr.xpdustry.javelin.chat

import arc.util.Strings
import cloud.commandframework.services.types.ConsumerService
import mindustry.gen.Groups

interface MessageService : ConsumerService<MessageContext> {
    companion object {
        @JvmStatic
        fun local(): MessageService = LocalMessageService
    }
}

internal object LocalMessageService : MessageService {
    override fun accept(context: MessageContext) {
        val player = Groups.player.find {
            Strings.stripColors(it.name()) == Strings.stripColors(context.receiver)
        }

        if (player != null) {
            player.sendMessage(context.message)
            ConsumerService.interrupt()
        }
    }
}
