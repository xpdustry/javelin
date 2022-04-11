package fr.xpdustry.javelin.core

import fr.xpdustry.javelin.core.model.Scope

data class JavelinMessage(
    val content: String,
    val clazz: String,
    val scope: Scope,
    val target: MessageTarget
)

enum class MessageTarget {
    CLIENT, SERVER
}
