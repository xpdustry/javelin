plugins {
    id("javelin.parent-conventions")
}

group = "fr.xpdustry"
version = "2.0.0" + if (indraGit.headTag() == null) "-SNAPSHOT" else ""
description = "A simple messaging library for inter-server communication."
