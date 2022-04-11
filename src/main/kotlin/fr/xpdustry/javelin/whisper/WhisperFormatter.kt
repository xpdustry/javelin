package fr.xpdustry.javelin.whisper

fun interface WhisperFormatter {
    fun format(sender: String, message: String): String

    fun format(context: WhisperContext): String = format(context.sender, context.message)

    companion object {
        @JvmStatic
        var instance: WhisperFormatter = simple()

        @JvmStatic
        fun simple(): WhisperFormatter = SimpleWhisperFormatter
    }

    private object SimpleWhisperFormatter : WhisperFormatter {
        override fun format(sender: String, message: String): String =
            "[gray]<W>[] [purple][[[]$sender[purple]][pink]:[white] $message"
    }
}
