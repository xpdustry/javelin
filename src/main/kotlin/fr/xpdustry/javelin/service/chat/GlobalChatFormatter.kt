package fr.xpdustry.javelin.service.chat

fun interface GlobalChatFormatter {
    fun format(sender: String, message: String): String

    fun format(context: GlobalChatContext): String = format(context.sender, context.message)

    companion object {
        @JvmStatic
        var instance: GlobalChatFormatter = simple()

        @JvmStatic
        fun simple(): GlobalChatFormatter = SimpleGlobalChatFormatter
    }

    private object SimpleGlobalChatFormatter : GlobalChatFormatter {
        override fun format(sender: String, message: String): String =
            "[gray]<G>[] [purple][[[]$sender[purple]][pink]:[white] $message"
    }
}