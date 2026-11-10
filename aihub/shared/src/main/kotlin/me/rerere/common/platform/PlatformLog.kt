package me.rerere.common.platform

object PlatformLog {
    // Absencium: debug and info lines carry the full request (the student's questions and files), so they are dropped.
    @Suppress("UNUSED_PARAMETER")
    fun d(tag: String, message: String) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun i(tag: String, message: String) = Unit

    fun w(tag: String, message: String) = log("W", tag, message)

    fun e(tag: String, message: String) = log("E", tag, message)

    private fun log(level: String, tag: String, message: String) {
        println("$level/$tag: $message")
    }
}
