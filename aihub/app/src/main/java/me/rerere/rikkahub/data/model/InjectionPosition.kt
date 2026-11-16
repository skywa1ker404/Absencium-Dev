package me.rerere.rikkahub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Where an injected prompt block (a skill) is placed in the message context. */
@Serializable
enum class InjectionPosition {
    @SerialName("before_system")
    BEFORE_SYSTEM,

    @SerialName("after_system")
    AFTER_SYSTEM,

    @SerialName("top_of_chat")
    TOP_OF_CHAT,

    @SerialName("before_latest")
    BEFORE_LATEST,

    @SerialName("at_depth")
    AT_DEPTH
}
