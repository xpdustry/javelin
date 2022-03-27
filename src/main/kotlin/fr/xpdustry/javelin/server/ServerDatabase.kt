package fr.xpdustry.javelin.server

import arc.files.Fi

typealias Password = ByteArray
typealias ServerCollection = Collection<Server>

interface ServerDatabase {
    companion object {
        @JvmStatic
        fun simple(file: Fi): ServerDatabase = SimpleServerDatabase(file.path())
    }

    val servers: ServerCollection

    fun addServer(name: String, password: Password, access: Server.ServerAccess): Server

    fun getServer(name: String): Server?

    fun hasServer(name: String): Boolean

    fun removeServer(name: String)

    fun isValid(name: String, password: Password): Boolean
}
