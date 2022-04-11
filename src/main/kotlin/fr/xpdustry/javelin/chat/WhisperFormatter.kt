package fr.xpdustry.javelin.chat

fun interface WhisperFormatter {
    fun format(sender: String, message: String): String

    companion object {
        @JvmStatic
        var instance: WhisperFormatter = simple()

        @JvmStatic
        fun simple(): WhisperFormatter = SimpleWhisperFormatter
    }
}

private object SimpleWhisperFormatter : WhisperFormatter {
    override fun format(sender: String, message: String): String =
        "[gray]<W>[] [purple][[[]$sender[purple]][pink]:[white] $message"
}