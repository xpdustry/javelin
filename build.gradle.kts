val snapshot = hasProperty("releaseProject") && property("releaseProject").toString().toBoolean()
group = "fr.xpdustry"
version = "1.0.0" + if (snapshot) "" else "-SNAPSHOT"
description = "A cross server communication library for Mindustry."
