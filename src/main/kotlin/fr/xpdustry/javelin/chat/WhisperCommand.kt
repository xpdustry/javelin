package fr.xpdustry.javelin.chat

import arc.Events
import arc.util.Strings
import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.specifier.Greedy
import fr.xpdustry.distributor.command.sender.ArcCommandSender
import fr.xpdustry.distributor.message.MessageIntent
import fr.xpdustry.javelin.util.clientMessageFormatter
import fr.xpdustry.javelin.util.format
import fr.xpdustry.javelin.util.servicePipeline
import fr.xpdustry.javelin.util.typeToken
import mindustry.game.EventType.PlayerLeave
import mindustry.gen.Player

class WhisperCommand {
    private val replies = mutableMapOf<Player, String>()

    init {
        Events.on(PlayerLeave::class.java) { replies.remove(it.player) }
    }

    @CommandMethod("whisper|w <receiver> <message>")
    @CommandDescription("Whisper a message to another player.")
    fun sendMessage(
        sender: ArcCommandSender,
        @Argument("receiver") receiver: String,
        @Argument("message") @Greedy message: String
    ) {
        if (Strings.stripColors(sender.player.name()) == Strings.stripColors(receiver)) {
            sender.sendMessage(clientMessageFormatter.format("You can't message yourself.", MessageIntent.ERROR))
        } else {
            sender.whisper(receiver, message)
        }
    }

    @CommandMethod("reply|r <message>")
    @CommandDescription("Whisper a message to another player.")
    fun sendMessage(
        sender: ArcCommandSender,
        @Argument("message") @Greedy message: String
    ) {
        val receiver = replies[sender.player]
        if (receiver == null) {
            sender.sendMessage(clientMessageFormatter.format("Error, no recent messages.", MessageIntent.ERROR))
        } else {
            sender.whisper(receiver, message)
        }
    }

    private fun ArcCommandSender.whisper(receiver: String, message: String) {
        val context = WhisperContext(player.name(), receiver, message)
        servicePipeline.pump(context).through(typeToken<WhisperService>()).result
        replies[player] = receiver
        player.sendMessage(context.formatted())
    }
}
