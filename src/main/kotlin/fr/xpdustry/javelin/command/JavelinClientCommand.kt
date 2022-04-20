package fr.xpdustry.javelin.command

import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import fr.xpdustry.distributor.command.sender.ArcCommandSender
import fr.xpdustry.distributor.message.MessageIntent
import fr.xpdustry.javelin.Javelin
import fr.xpdustry.javelin.internal.JavelinClientConfig
import fr.xpdustry.javelin.util.formatter
import fr.xpdustry.javelin.util.invoke
import javax.inject.Inject

@Suppress("UNUSED")
class JavelinClientCommand @Inject constructor(private val config: JavelinClientConfig) {

    @CommandMethod("javelin client reconnect")
    @CommandDescription("Reconnects the client to the server.")
    internal fun addClient(sender: ArcCommandSender) {
        if (!config.enabled) {
            sender.sendMessage(sender.formatter("The client is not enabled.", MessageIntent.ERROR))
        } else {
            Javelin.client.restart()
        }
    }
}
