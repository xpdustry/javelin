package fr.xpdustry.javelin.internal

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.parsers.Parser
import cloud.commandframework.context.CommandContext
import cloud.commandframework.exceptions.parsing.NoInputProvidedException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import fr.xpdustry.distributor.command.argument.PlayerArgument.PlayerParser
import fr.xpdustry.distributor.command.sender.ArcCommandSender
import fr.xpdustry.javelin.model.Endpoint
import fr.xpdustry.javelin.model.Server
import fr.xpdustry.javelin.repository.ServerRepository
import java.util.*
import javax.inject.Inject

@Suppress("UNUSED")
class ServerRepositoryCommand @Inject constructor(
    private val algorithm: Algorithm,
    private val repository: ServerRepository
) {
    @CommandMethod("javelin server add <name>")
    @CommandDescription("Add a server to the repository.")
    fun addServer(
        sender: ArcCommandSender,
        @Argument("name") name: String
    ) {
        val server = repository.addServer(name, createAccessToken(name), mutableSetOf())
        if (server != null) {
            sender.sendMessage("Added server $name.")
            println("TOKEN: ${server.token}")
        } else {
            sender.sendMessage("The server $name already exist.")
        }
    }

    @CommandMethod("javelin server remove <name>")
    @CommandDescription("Remove a server from the repository.")
    fun removeServer(
        sender: ArcCommandSender,
        @Argument("name") server: Server
    ) {
        repository -= server.name
        sender.sendMessage("The server ${server.name} has been removed exist.")
    }

    @CommandMethod("javelin server list")
    @CommandDescription("List the registered servers.")
    fun listServers(
        sender: ArcCommandSender,
    ) {
        val builder = StringBuilder()
        builder.append("-- javelin server list -- :")
        repository.servers.forEach { builder.append("\n${it.name}: ${it.endpoints}") }
        sender.sendMessage(builder.toString())
    }

    @CommandMethod("javelin server endpoint add <name> <namespace> <subject>")
    @CommandDescription("Add an endpoint to a server.")
    fun addEndpoint(
        sender: ArcCommandSender,
        @Argument("name") server: Server,
        @Argument("namespace") namespace: String,
        @Argument("subject") subject: String
    ) {
        val endpoint = Endpoint(namespace, subject)
        server.endpoints += endpoint
        repository.updateServer(server.name)
        sender.sendMessage("The endpoint $endpoint has been added to ${server.name}.")
    }

    @CommandMethod("javelin server endpoint remove <name> <namespace> <subject>")
    @CommandDescription("Remove an endpoint from a server.")
    fun removeEndpoint(
        sender: ArcCommandSender,
        @Argument("name") server: Server,
        @Argument("namespace") namespace: String,
        @Argument("subject") subject: String
    ) {
        val endpoint = Endpoint(namespace, subject)
        server.endpoints -= endpoint
        repository.updateServer(server.name)
        sender.sendMessage("The endpoint $endpoint has been removed from ${server.name}.")
    }

    @Parser
    fun getServer(context: CommandContext<ArcCommandSender>, queue: Queue<String>): Server {
        val input = queue.peek()
        if (input == null) {
            throw NoInputProvidedException(PlayerParser::class.java, context)
        } else {
            val server = repository.getServer(input)
            if (server == null) {
                throw NoSuchElementException("No such server named $input.")
            } else {
                queue.remove()
                return server
            }
        }
    }

    private fun createAccessToken(name: String): String = JWT
        .create()
        .withSubject(name)
        .withIssuedAt(Date())
        .sign(algorithm)
}