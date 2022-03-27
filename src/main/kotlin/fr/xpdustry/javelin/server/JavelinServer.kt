package fr.xpdustry.javelin.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import fr.xpdustry.javelin.util.GsonMapper
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import javalinjwt.JWTProvider
import javalinjwt.JavalinJWT
import java.util.*
import io.javalin.apibuilder.ApiBuilder.*

class JavelinServer(
    private val database: ServerDatabase,
    private val issuer: String,
    private val port: Int,
    secret: String
) : Runnable {
    companion object {
        // one hour should do the trick...
        const val TOKEN_LIFETIME = 1000L * 60 * 60
    }

    private val algorithm = Algorithm.HMAC256(secret)

    private val provider = JWTProvider<Server>(
        algorithm, { server, alg ->
            JWT.create()
                .withIssuer(issuer)
                .withSubject(server.name)
                .withAudience(server.access.name)
                .withExpiresAt(Date(System.currentTimeMillis() + TOKEN_LIFETIME))
                .sign(alg)
        }, JWT.require(algorithm).build()
    )

    private val javalin = Javalin.create {
        it.jsonMapper(GsonMapper())
        it.accessManager { handler, ctx, roles ->
            val role = if (!JavalinJWT.containsJWT(ctx)) {
                Server.ServerAccess.PUBLIC
            } else {
                Server.ServerAccess.valueOf(JavalinJWT.getDecodedFromContext(ctx).audience[0])
            }

            if (roles.isEmpty() || roles.contains(role)) {
                handler.handle(ctx)
            } else {
                ctx.status(401).result("Missing or invalid token")
            }
        }
    }.apply {
        before(JavalinJWT.createHeaderDecodeHandler(provider))
        get("/api/auth") {
            try {
                val request = it.bodyAsClass<AuthRequestMessage>()
                if (database.isValid(request.name, request.password.toByteArray())) {
                    val server = database.getServer(request.name)
                    val token = provider.generateToken(server)
                    it.json(AuthResponseMessage.success(token))
                } else {
                    it.json(AuthResponseMessage.fail("Invalid credentials."))
                }
            } catch (e: Exception) {
                it.json(AuthResponseMessage.fail("Malformed credentials"))
            }
        }
    }.routes {

    }

    override fun run() {
        // https://github.com/tipsy/javalin/issues/358#issuecomment-420982615 I lost my mind...
        val classLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = Javalin::class.java.classLoader
        javalin.start(port)
        Thread.currentThread().contextClassLoader = classLoader
    }

    data class AuthRequestMessage(val name: String, val password: String)

    data class AuthResponseMessage(
        val success: Boolean,
        val token: String?,
        val reason: String?
    ) {
        companion object {
            @JvmStatic
            fun success(token: String) = AuthResponseMessage(true, token, null)

            @JvmStatic
            fun fail(reason: String) = AuthResponseMessage(false, null, reason)
        }
    }
}
