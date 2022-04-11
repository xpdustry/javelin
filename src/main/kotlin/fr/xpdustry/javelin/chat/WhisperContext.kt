package fr.xpdustry.javelin.chat

data class WhisperContext(val sender: String, val receiver: String, val message: String) {
    fun formatted(): String = WhisperFormatter.instance.format(sender, message)
}
