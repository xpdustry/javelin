package fr.xpdustry.javelin.internal

import fr.xpdustry.javelin.core.model.Scope

data class JavelinClientConfig(
    val hosts: MutableList<ConnectionData> = arrayListOf()
)

data class ConnectionData(val host: String, val token: String, val scope: Scope)
