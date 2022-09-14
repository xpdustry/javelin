plugins {
    id("javelin.parent-conventions")
}
group = "fr.xpdustry"
version = "1.0.0" + if (indraGit.headTag() == null) "-SNAPSHOT" else ""
description = "A simple communication protocol for broadcasting events on a network."
