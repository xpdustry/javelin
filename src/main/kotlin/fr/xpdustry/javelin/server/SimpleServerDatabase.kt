package fr.xpdustry.javelin.server

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import net.mindustry_ddns.store.JsonFileStore
import java.lang.reflect.Type
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

private typealias Salt = ByteArray
private typealias PasswordHash = ByteArray

internal class SimpleServerDatabase(path: String) : ServerDatabase {

    override val servers: ServerCollection
        get() = _servers

    private val _servers = mutableListOf<SimpleServer>()

    private val store = JsonFileStore.load<ServerCollection>(
        path,
        TypeToken.getParameterized(Set::class.java, SimpleServer::class.java).type,
        ::mutableSetOf,
        GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(SimpleServer::class.java, SimpleServerSerializer())
            .registerTypeAdapter(SimpleServer::class.java, SimpleServerDeserializer())
            .create()
    )

    init {
        store.set(_servers)
    }

    override fun addServer(
        name: String,
        password: ByteArray,
        access: Server.ServerAccess
    ): Server {
        if (hasServer(name)) {
            throw IllegalStateException("The $name server already exists...")
        } else {
            val (hash, salt) = hash(password)
            val server = SimpleServer(name, access, hash, salt)
            _servers.add(server)
            store.save()
            return server
        }
    }

    override fun getServer(name: String): Server? = _servers.find { it.name == name }

    override fun hasServer(name: String): Boolean = getServer(name) != null

    override fun removeServer(name: String) {
        if(_servers.removeIf { it.name == name }) store.save()
    }

    override fun isValid(name: String, password: Password): Boolean {
        val server = getServer(name) as SimpleServer? ?: return false
        return hash(password, server.salt).contentEquals(server.hash)
    }

    private fun hash(password: Password, salt: Salt): PasswordHash {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(password)
    }

    private fun hash(password: Password): Pair<PasswordHash, Salt> {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        val salted = hash(password, salt)
        return salted to salt
    }

    private class SimpleServer(
        override val name: String,
        override val access: Server.ServerAccess,
        val hash: PasswordHash,
        val salt: Salt
    ) : Server

    private class SimpleServerSerializer : JsonSerializer<SimpleServer> {
        override fun serialize(
            src: SimpleServer?,
            type: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return if (src == null) {
                JsonNull.INSTANCE
            } else {
                JsonObject().apply {
                    addProperty("name", src.name)
                    addProperty("access", src.access.name)
                    addProperty("hash", Base64.getEncoder().encodeToString(src.hash))
                    addProperty("salt", Base64.getEncoder().encodeToString(src.salt))
                }
            }
        }
    }

    private class SimpleServerDeserializer : JsonDeserializer<SimpleServer> {
        override fun deserialize(
            json: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): SimpleServer {
            val obj = json.asJsonObject
            return SimpleServer(
                obj.get("name").asString,
                Server.ServerAccess.valueOf(obj.get("access").asString),
                Base64.getDecoder().decode(obj.get("hash").asString),
                Base64.getDecoder().decode(obj.get("salt").asString)
            )
        }
    }
}
