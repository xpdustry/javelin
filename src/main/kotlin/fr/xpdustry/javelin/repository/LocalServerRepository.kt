package fr.xpdustry.javelin.repository

import com.google.gson.GsonBuilder
import fr.xpdustry.javelin.model.Endpoint
import fr.xpdustry.javelin.model.Server
import fr.xpdustry.javelin.model.registerEndpointTypeAdapter
import fr.xpdustry.javelin.util.typeToken
import net.mindustry_ddns.store.JsonFileStore

class LocalServerRepository(path: String) : ServerRepository {

    override val servers: Collection<Server>
        get() = store.get()

    private val store = JsonFileStore.load<MutableList<Server>>(
        path,
        typeToken<MutableList<Server>>().type,
        ::mutableListOf,
        GsonBuilder()
            .setPrettyPrinting()
            .registerEndpointTypeAdapter()
            .create()
    )

    override fun addServer(name: String, token: String, endpoints: MutableSet<Endpoint>): Server? {
        if (!hasServer(name)) {
            val server = Server(name, token, mutableSetOf())
            store.get().add(server)
            store.save()
            return server
        } else {
            return null
        }
    }

    override fun getServer(name: String): Server? =
        servers.find { it.name == name }

    override fun hasServer(name: String): Boolean =
        getServer(name) != null

    override fun removeServer(name: String) {
        store.get().removeAll { it.name == name }
        store.save()
    }

    override fun updateServer(name: String) {
        if (hasServer(name)) store.save()
    }
}