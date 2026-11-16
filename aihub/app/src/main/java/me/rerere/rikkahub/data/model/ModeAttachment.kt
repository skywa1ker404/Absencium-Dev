package me.rerere.rikkahub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Type of multimedia attachment a skill can carry (name kept for stored-data compatibility). */
@Serializable
enum class ModeAttachmentType {
    @SerialName("image")
    IMAGE,
    @SerialName("video")
    VIDEO,
    @SerialName("audio")
    AUDIO,
    @SerialName("document")
    DOCUMENT
}

/** A multimedia attachment that a skill can carry. */
@Serializable
data class ModeAttachment(
    val url: String,  // Local file URI
    val type: ModeAttachmentType,
    val fileName: String = "",  // For documents
    val mime: String = ""  // For documents
)
