package fr.xpdustry.javelin.chat

import arc.util.Strings
import cloud.commandframework.services.types.ConsumerService
import mindustry.gen.Groups

interface WhisperService : ConsumerService<WhisperContext> {
    companion object {
        @JvmStatic
        fun local(): WhisperService = LocalWhisperService
    }
}

internal object LocalWhisperService : WhisperService {
    override fun accept(context: WhisperContext) {
        val player = Groups.player.find {
            Strings.stripColors(it.name()) == Strings.stripColors(context.receiver)
        }

        if (player != null) {
            player.sendMessage(context.formatted())
            ConsumerService.interrupt()
        }
    }
}
