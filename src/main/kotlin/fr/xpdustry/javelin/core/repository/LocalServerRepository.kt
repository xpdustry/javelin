package fr.xpdustry.javelin.core.repository

import com.google.gson.reflect.TypeToken
import fr.xpdustry.javelin.core.model.Scope
import fr.xpdustry.javelin.core.model.Server
import net.mindustry_ddns.store.JsonFileStore

class LocalServerRepository(path: String) : ServerRepository {

    override val servers: Collection<Server>
        get() = store.get()

    private val store = JsonFileStore.load<MutableList<Server>>(
        path,
        TypeToken.getParameterized(java.util.List::class.java, Server::class.java).type,
        ::mutableListOf
    )

    override fun addServer(name: String, token: String, scope: Scope): Server? {
        if (!hasServer(name)) {
            val server = Server(name, token, scope)
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
}