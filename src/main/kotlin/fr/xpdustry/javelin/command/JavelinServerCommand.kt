package fr.xpdustry.javelin.command

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.parsers.Parser
import cloud.commandframework.context.CommandContext
import cloud.commandframework.exceptions.parsing.NoInputProvidedException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import fr.xpdustry.distributor.command.sender.ArcCommandSender
import fr.xpdustry.distributor.message.MessageIntent
import fr.xpdustry.javelin.internal.JavelinServerConfig
import fr.xpdustry.javelin.model.Client
import fr.xpdustry.javelin.model.Endpoint
import fr.xpdustry.javelin.repository.ClientRepository
import fr.xpdustry.javelin.util.invoke
import fr.xpdustry.javelin.util.formatter
import java.util.*
import javax.inject.Inject

@Suppress("UNUSED")
class JavelinServerCommand @Inject constructor(
    private val repository: ClientRepository,
    config: JavelinServerConfig
) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    @CommandMethod("javelin server add <name>")
    @CommandDescription("Create a new Javelin client.")
    internal fun addClient(sender: ArcCommandSender, @Argument("name") name: String) {
        val client = Client(name, createToken(name))
        if (repository.addClient(client)) {
            sender.sendMessage(sender.formatter("Added server @ (TOKEN: @).", MessageIntent.SUCCESS, name, client.token))
        } else {
            sender.sendMessage(sender.formatter("The server @ already exist.", MessageIntent.ERROR, name))
        }
    }

    @CommandMethod("javelin server remove <name>")
    @CommandDescription("Remove a Javelin client.")
    internal fun removeClient(sender: ArcCommandSender, @Argument("name") name: String) {
        if (repository.removeClient(name)) {
            sender.sendMessage(sender.formatter("The server @ has been removed.", MessageIntent.SUCCESS, name))
        } else {
            sender.sendMessage(sender.formatter("The server @ does not exist.", MessageIntent.ERROR, name))
        }
    }

    @CommandMethod("javelin server list")
    @CommandDescription("List the registered Javelin clients.")
    internal fun listClients(sender: ArcCommandSender) {
        sender.sendMessage(StringBuilder().apply {
            appendLine("Javelin clients list :")
            appendLine("|")
            if (repository.clients.isEmpty()) {
                appendLine("| None")
                appendLine("|")
            } else {
                repository.clients.forEach {
                    appendLine("| ${it.name} :")
                    appendLine("| > token: ${it.token}")
                    appendLine("| > blacklist: ${it.blacklist}")
                    appendLine("| > whitelist: ${it.whitelist}")
                    appendLine("|")
                }
            }
        }.toString())
    }

    @CommandMethod("javelin server token reset <client>")
    @CommandDescription("Reset the token of a Javelin client.")
    internal fun resetClientToken(sender: ArcCommandSender, @Argument("client") client: Client) {
        client.token = createToken(client.name)
        sender.sendMessage(sender.formatter("The new token of @ is now @.", MessageIntent.SUCCESS, client.name, client.token))
    }

    @CommandMethod("javelin server blacklist add <client> <namespace> <subject>")
    @CommandDescription("Add an endpoint to a client blacklist.")
    internal fun addBlacklistedEndpoint(
        sender: ArcCommandSender,
        @Argument("client") client: Client,
        @Argument("namespace") namespace: String,
        @Argument("subject") subject: String
    ) {
        addEndpoint(sender, client, Endpoint(namespace, subject), ListType.BLACKLIST)
    }

    @CommandMethod("javelin server blacklist remove <client> <namespace> <subject>")
    @CommandDescription("Remove an endpoint from a client.")
    internal fun removeBlacklistedEndpoint(
        sender: ArcCommandSender,
        @Argument("client") client: Client,
        @Argument("namespace") namespace: String,
        @Argument("subject") subject: String
    ) {
        removeEndpoint(sender, client, Endpoint(namespace, subject), ListType.BLACKLIST)
    }

    @CommandMethod("javelin server whitelist add <client> <namespace> <subject>")
    @CommandDescription("Add an endpoint to a client whitelist.")
    internal fun addWhitelistedEndpoint(
        sender: ArcCommandSender,
        @Argument("client") client: Client,
        @Argument("namespace") namespace: String,
        @Argument("subject") subject: String
    ) {
        addEndpoint(sender, client, Endpoint(namespace, subject), ListType.WHITELIST)
    }

    @CommandMethod("javelin server whitelist remove <client> <namespace> <subject>")
    @CommandDescription("Remove an whitelist from a client.")
    internal fun removeWhitelistedEndpoint(
        sender: ArcCommandSender,
        @Argument("client") client: Client,
        @Argument("namespace") namespace: String,
        @Argument("subject") subject: String
    ) {
        removeEndpoint(sender, client, Endpoint(namespace, subject), ListType.WHITELIST)
    }

    @Parser
    internal fun parseClient(context: CommandContext<ArcCommandSender>, queue: Queue<String>): Client {
        val input = queue.peek()
        if (input == null) {
            throw NoInputProvidedException(JavelinServerCommand::class.java, context)
        } else {
            val client = repository.getClient(input)
            if (client == null) {
                throw NoSuchElementException("No such client named $input.")
            } else {
                queue.remove()
                return client
            }
        }
    }

    private fun addEndpoint(sender: ArcCommandSender, client: Client, endpoint: Endpoint, type: ListType) {
        val list = when(type) {
            ListType.BLACKLIST -> client.blacklist
            ListType.WHITELIST -> client.whitelist
        }

        if (list.add(endpoint)) {
            repository.updateClient(client)
            sender.sendMessage(sender.formatter(
                "The endpoint @ has been added to @ ${type.name.lowercase()}.", MessageIntent.SUCCESS, endpoint, client.name
            ))
        } else {
            sender.sendMessage(sender.formatter(
                "The endpoint @ is already present in @ ${type.name.lowercase()}.", MessageIntent.ERROR, endpoint, client.name
            ))
        }
    }

    private fun removeEndpoint(sender: ArcCommandSender, client: Client, endpoint: Endpoint, type: ListType) {
        val list = when(type) {
            ListType.BLACKLIST -> client.blacklist
            ListType.WHITELIST -> client.whitelist
        }

        if (list.remove(endpoint)) {
            repository.updateClient(client)
            sender.sendMessage(sender.formatter(
                "The endpoint @ has been removed from @ ${type.name.lowercase()}.", MessageIntent.SUCCESS, endpoint, client.name
            ))
        } else {
            sender.sendMessage(sender.formatter(
                "The endpoint @ is not present in @ ${type.name.lowercase()}.", MessageIntent.ERROR, endpoint, client.name
            ))
        }
    }

    private fun createToken(name: String): String = JWT
        .create()
        .withSubject(name)
        .withIssuedAt(Date())
        .sign(algorithm)

    private enum class ListType {
        BLACKLIST, WHITELIST
    }
}
