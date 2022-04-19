package fr.xpdustry.javelin

import fr.xpdustry.javelin.model.Endpoint

interface MessageHandler {
    val endpoint: Endpoint
    fun onMessageReceive(message: JavelinMessage, content: Any)
}
