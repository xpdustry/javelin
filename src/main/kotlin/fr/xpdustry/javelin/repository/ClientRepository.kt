package fr.xpdustry.javelin.repository

import arc.files.Fi
import fr.xpdustry.javelin.model.Client

interface ClientRepository {
    val clients: Collection<Client>

    fun addClient(client: Client): Boolean

    fun getClient(name: String): Client?

    fun hasClient(name: String): Boolean

    fun removeClient(name: String): Boolean

    fun updateClient(client: Client)

    operator fun get(name: String): Client? = getClient(name)

    operator fun contains(name: String): Boolean = hasClient(name)

    operator fun minusAssign(name: String) {
        removeClient(name)
    }

    companion object {
        @JvmStatic
        fun local(file: Fi): ClientRepository = LocalClientRepository(file.path())
    }
}