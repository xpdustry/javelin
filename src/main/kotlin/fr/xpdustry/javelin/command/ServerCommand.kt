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
import fr.xpdustry.javelin.internal.JavelinServerConfig
import fr.xpdustry.javelin.model.Client
import fr.xpdustry.javelin.model.Endpoint
import fr.xpdustry.javelin.repository.ClientRepository
import java.util.*
import javax.inject.Inject

@Suppress("UNUSED")
class ServerCommand @Inject constructor(
    private val repository: ClientRepository,
    config: JavelinServerConfig
) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    @CommandMethod("javelin server add <name>")
    @CommandDescription("Create a new Javelin client.")
    internal fun addClient(sender: ArcCommandSender, @Argument("name") name: String) {
        val client = Client(name, createAccessToken(name))
        if (repository.addClient(client)) {
            sender.sendMessage("Added server $name.")
            sender.sendMessage("TOKEN: ${client.token}")
        } else {
            sender.sendMessage("The server $name already exist.")
        }
    }

    @CommandMethod("javelin server remove <name>")
    @CommandDescription("Remove a Javelin client.")
    internal fun removeClient(sender: ArcCommandSender, @Argument("name") name: String) {
        if (repository.removeClient(name)) {
            sender.sendMessage("The server $name has been removed.")
        } else {
            sender.sendMessage("The server $name does not exist.")
        }
    }

    @CommandMethod("javelin server list")
    @CommandDescription("List the registered Javelin clients.")
    internal fun listClients(sender: ArcCommandSender) {
        sender.sendMessage("--[ javelin client list ]-- :" + if (repository.clients.isEmpty()) {
            "\nNone..."
        } else {
            repository.clients.joinToString { "\n${String.format("%-10s: ${it.endpoints}", it.name)}" }
        })
    }

    @CommandMethod("javelin server token reset <client>")
    @CommandDescription("Reset the token of a Javelin client.")
    internal fun resetClientToken(sender: ArcCommandSender, @Argument("client") client: Client) {
        client.token = createAccessToken(client.name)
        sender.sendMessage("The new token of ${client.name} is ${client.token}.")
    }

    @CommandMethod("javelin server endpoint add <client> <namespace> <subject>")
    @CommandDescription("Add an endpoint to a client.")
    internal fun addEndpoint(
        sender: ArcCommandSender,
        @Argument("client") client: Client,
        @Argument("namespace") namespace: String,
        @Argument("subject") subject: String
    ) {
        val endpoint = Endpoint(namespace, subject)
        if (client.endpoints.add(endpoint)) {
            repository.updateClient(client)
            sender.sendMessage("The endpoint $endpoint has been added to ${client.name}.")
        } else {
            sender.sendMessage("The endpoint $endpoint is already present in the ${client.name} client.")
        }
    }

    @CommandMethod("javelin server endpoint remove <client> <namespace> <subject>")
    @CommandDescription("Remove an endpoint from a client.")
    internal fun removeEndpoint(
        sender: ArcCommandSender,
        @Argument("client") client: Client,
        @Argument("namespace") namespace: String,
        @Argument("subject") subject: String
    ) {
        val endpoint = Endpoint(namespace, subject)
        if (client.endpoints.remove(endpoint)) {
            repository.updateClient(client)
            sender.sendMessage("The endpoint $endpoint has been removed from ${client.name}.")
        } else {
            sender.sendMessage("The endpoint $endpoint is not present in the ${client.name} client.")
        }
    }

    @Parser
    internal fun parseClient(context: CommandContext<ArcCommandSender>, queue: Queue<String>): Client {
        val input = queue.peek()
        if (input == null) {
            throw NoInputProvidedException(ServerCommand::class.java, context)
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

    private fun createAccessToken(name: String): String = JWT
        .create()
        .withSubject(name)
        .withIssuedAt(Date())
        .sign(algorithm)
}