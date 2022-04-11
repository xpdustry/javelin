package fr.xpdustry.javelin.whisper

import arc.util.Strings
import cloud.commandframework.services.ExecutionOrder
import cloud.commandframework.services.State
import cloud.commandframework.services.types.SideEffectService
import mindustry.gen.Groups

interface WhisperService : SideEffectService<WhisperContext> {
    companion object {
        @JvmStatic
        fun local(): WhisperService = LocalWhisperService
    }

    private object LocalWhisperService : WhisperService {
        override fun handle(context: WhisperContext): State {
            val player = Groups.player.find {
                Strings.stripColors(it.name()) == Strings.stripColors(context.receiver)
            }

            return if (player != null) {
                player.sendMessage(WhisperFormatter.instance.format(context))
                State.ACCEPTED
            } else {
                State.REJECTED
            }
        }

        override fun order(): ExecutionOrder = ExecutionOrder.FIRST
    }
}
