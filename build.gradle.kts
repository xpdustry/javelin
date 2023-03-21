plugins {
    id("javelin.parent-conventions")
}

group = "fr.xpdustry"
version = "1.3.1" + if (indraGit.headTag() == null) "-SNAPSHOT" else ""
description = "A simple communication protocol for broadcasting events on a network."
