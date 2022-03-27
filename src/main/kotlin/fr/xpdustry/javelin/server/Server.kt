package fr.xpdustry.javelin.server

import io.javalin.core.security.RouteRole


interface Server {
    val name: String
    val access: ServerAccess

    enum class ServerAccess : RouteRole {
        PUBLIC, PROTECTED, PRIVATE;
    }
}