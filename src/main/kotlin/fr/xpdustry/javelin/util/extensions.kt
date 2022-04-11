package fr.xpdustry.javelin.util

import cloud.commandframework.services.ServicePipeline
import com.google.gson.Gson
import fr.xpdustry.distributor.Distributor
import fr.xpdustry.distributor.message.MessageIntent
import fr.xpdustry.distributor.message.format.MessageFormatter
import io.leangen.geantyref.TypeToken

private val EMPTY_ARRAY = emptyArray<Any>()

var clientMessageFormatter: MessageFormatter
    get() = Distributor.getClientMessageFormatter()
    set(value) = Distributor.setClientMessageFormatter(value)

/*
var serverMessageFormatter: MessageFormatter
    get() = Distributor.getServerMessageFormatter()
    set(value) = Distributor.setServerMessageFormatter(value)
 */

val servicePipeline: ServicePipeline
    get() = Distributor.getServicePipeline()

fun MessageFormatter.format(message: String, intent: MessageIntent = MessageIntent.INFO): String =
    format(intent, message, EMPTY_ARRAY)

inline fun <reified T> typeToken(): TypeToken<T> = object : TypeToken<T>() {}

inline fun <reified T> Gson.fromJson(json: String): T = fromJson(json, typeToken<T>().type)
