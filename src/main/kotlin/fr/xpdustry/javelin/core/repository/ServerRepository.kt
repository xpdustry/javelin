package fr.xpdustry.javelin.core.repository

import arc.files.Fi
import fr.xpdustry.javelin.core.model.Scope
import fr.xpdustry.javelin.core.model.Server

interface ServerRepository {
    val servers: Collection<Server>

    fun addServer(name: String, token: String, scope: Scope): Server?

    fun getServer(name: String): Server?

    fun hasServer(name: String): Boolean

    fun removeServer(name: String)

    operator fun get(name: String): Server? = getServer(name)

    operator fun contains(name: String): Boolean = hasServer(name)

    operator fun minusAssign(name: String) = removeServer(name)

    companion object {
        @JvmStatic
        fun local(file: Fi): ServerRepository = LocalServerRepository(file.path())
    }
}