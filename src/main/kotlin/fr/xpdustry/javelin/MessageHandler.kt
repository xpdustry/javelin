package fr.xpdustry.javelin

import fr.xpdustry.javelin.model.Endpoint

interface MessageHandler {
    val endpoint: Endpoint
    fun onMessageReceive(message: JavelinMessage, content: Any)
}

/*
class TypedMessageHandler<M>(
    override val endpoint: Endpoint,
    private val handler: BiConsumer<JavelinMessage, M>
) : MessageHandler {
    constructor(namespace: String, subject: String, handler: BiConsumer<JavelinMessage, M>) : this(Endpoint(namespace, subject), handler)
    @Suppress("UNCHECKED_CAST")
    override fun onMessageReceive(message: JavelinMessage, content: Any) = handler.accept(message, content as M)
}
 */