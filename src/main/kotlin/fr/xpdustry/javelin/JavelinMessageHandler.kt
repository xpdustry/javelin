package fr.xpdustry.javelin

fun interface JavelinMessageHandler {
    fun onMessageReceive(message: JavelinMessage, content: Any)
}
