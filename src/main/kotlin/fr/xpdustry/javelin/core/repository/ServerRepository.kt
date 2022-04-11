package fr.xpdustry.javelin.core.repository

import arc.files.Fi
import fr.xpdustry.javelin.core.model.Server

interface ServerRepository {
    val servers: Collection<Server>

    fun addServer(name: String, token: String): Server?

    fun getServer(name: String): Server?

    fun hasServer(name: String): Boolean

    fun removeServer(name: String)

    companion object {
        @JvmStatic
        fun local(file: Fi): ServerRepository = LocalServerRepository(file.path())
    }
}