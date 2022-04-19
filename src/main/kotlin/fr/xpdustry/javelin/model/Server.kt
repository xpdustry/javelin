package fr.xpdustry.javelin.model

import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

data class Server(val name: String, var token: String, val endpoints: MutableSet<Endpoint>)

data class Endpoint(val namespace: String, val subject: String) {
    override fun toString(): String = "$namespace:$subject"

    object EndpointTypeAdapter : TypeAdapter<Endpoint>() {
        override fun write(writer: JsonWriter, value: Endpoint?) {
            if (value == null) writer.nullValue()
            else writer.value(value.namespace + ":" + value.subject)
        }

        override fun read(reader: JsonReader): Endpoint? {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                return null
            } else {
                val value = reader.nextString()
                val parts = value.split(':', limit = 2)
                if (parts.size != 2) throw JsonIOException("Invalid format $value, it should be 'namespace:handler'.")
                return Endpoint(parts[0], parts[1])
            }
        }
    }
}

fun GsonBuilder.registerEndpointTypeAdapter(): GsonBuilder =
    apply { registerTypeAdapter(Endpoint::class.java, Endpoint.EndpointTypeAdapter) }

