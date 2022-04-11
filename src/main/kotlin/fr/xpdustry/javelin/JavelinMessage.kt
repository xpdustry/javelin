package fr.xpdustry.javelin

import fr.xpdustry.javelin.model.Endpoint

data class JavelinMessage(
    val endpoint: Endpoint,
    val content: String,
    val clazz: String,
    val sender: String,
    val receiver: String?
)
