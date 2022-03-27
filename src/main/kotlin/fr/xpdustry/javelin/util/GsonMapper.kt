package fr.xpdustry.javelin.util

import com.google.gson.Gson
import io.javalin.plugin.json.JsonMapper
import java.io.InputStream

class GsonMapper(private val instance: Gson = Gson()) : JsonMapper {

    override fun toJsonString(obj: Any): String =
        instance.toJson(obj)

    override fun toJsonStream(obj: Any): InputStream =
        instance.toJson(obj).byteInputStream()

    override fun <T> fromJsonString(json: String, clazz: Class<T>): T =
        instance.fromJson(json, clazz)

    override fun <T> fromJsonStream(json: InputStream, targetClass: Class<T>): T =
        json.reader().use { instance.fromJson(it, targetClass) }
}
