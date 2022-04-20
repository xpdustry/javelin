@file:JvmName("Utils")

package fr.xpdustry.javelin.util

import com.google.gson.Gson
import fr.xpdustry.distributor.Distributor
import fr.xpdustry.distributor.command.sender.ArcCommandSender
import fr.xpdustry.distributor.message.MessageIntent
import fr.xpdustry.distributor.message.format.MessageFormatter
import io.leangen.geantyref.TypeToken
import org.java_websocket.drafts.Draft
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.protocols.Protocol

val ArcCommandSender.formatter: MessageFormatter
    get() = Distributor.getMessageFormatter(this)

operator fun MessageFormatter.invoke(
    message: String,
    intent: MessageIntent = MessageIntent.INFO,
    vararg args: Any = emptyArray()
): String {
    return format(intent, message, *args)
}

inline fun <reified T> typeToken(): TypeToken<T> = object : TypeToken<T>() {}

inline fun <reified T> Gson.fromJson(json: String): T = fromJson(json, typeToken<T>().type)

fun getDraft(https: Boolean): Draft {
    return if (https) {
        Draft_6455(emptyList(), listOf(Protocol("ocpp2.0")))
    } else {
        Draft_6455()
    }
}