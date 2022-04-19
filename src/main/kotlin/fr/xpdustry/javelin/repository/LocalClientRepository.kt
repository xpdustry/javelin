package fr.xpdustry.javelin.repository

import com.google.gson.GsonBuilder
import fr.xpdustry.javelin.model.Client
import fr.xpdustry.javelin.model.registerEndpointTypeAdapter
import fr.xpdustry.javelin.util.typeToken
import net.mindustry_ddns.store.JsonFileStore

internal class LocalClientRepository(path: String) : ClientRepository {
    override val clients: Collection<Client>
        get() = store.get()

    private val store = JsonFileStore.load<MutableList<Client>>(
        path,
        typeToken<MutableList<Client>>().type,
        ::mutableListOf,
        GsonBuilder()
            .setPrettyPrinting()
            .registerEndpointTypeAdapter()
            .create()
    )

    override fun addClient(client: Client): Boolean {
        if (!hasClient(client.name)) {
            store.get().add(client)
            store.save()
            return true
        } else {
            return true
        }
    }

    override fun getClient(name: String): Client? =
        clients.find { it.name == name }

    override fun hasClient(name: String): Boolean =
        getClient(name) != null

    override fun removeClient(name: String): Boolean {
        val removed = store.get().removeAll { it.name == name }
        store.save()
        return removed
    }

    override fun updateClient(client: Client) {
        if (client in clients) store.save()
    }
}