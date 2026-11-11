package me.rerere.rikkahub.data.ai.transformers

import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.ai.provider.Modality

/**
 * Transforms residual unsupported attachments into text references after the
 * dedicated document and OCR transformers have already had a chance to consume them.
 */
object UnsupportedFileTransformer : InputMessageTransformer {
    internal fun buildResidualImageFallbackText(
        fileName: String,
        sourceUrl: String,
        workspaceEnabled: Boolean,
    ): String {
        return "\n[Image attachment: $fileName - The selected model cannot read pictures. Tell the user plainly that this model cannot read images and that they can pick a model that accepts images in the settings. Do not guess what the picture shows and do not mention tools, python or a workspace.]\n"
    }

    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        val isWorkspaceEnabled = ctx.assistant.workspaceId != null
        val modelSupportsImages = ctx.model.inputModalities.contains(Modality.IMAGE)

        return messages.map { msg ->
            msg.copy(parts = msg.parts.map { part ->
                when (part) {
                    is UIMessagePart.Document -> {
                        // Supported native types (Images/Video/Audio/Text/PDF)
                        val isNative = part.mime.startsWith("image/") ||
                            part.mime.startsWith("text/") ||
                            part.mime.startsWith("video/") ||
                            part.mime.startsWith("audio/") ||
                            part.mime == "application/pdf"

                        if (!isNative) {
                            UIMessagePart.Text("\n[Attachment: ${part.fileName} (${part.mime}) - This file format cannot be read by the model. Tell the user plainly that this kind of file is not supported; do not mention tools, python or a workspace.]\n")
                        } else {
                            part
                        }
                    }
                    is UIMessagePart.Image -> {
                        if (!modelSupportsImages) {
                            val filename = part.url.substringAfterLast("/").substringBefore("?").ifEmpty { "image.jpg" }
                            UIMessagePart.Text(
                                buildResidualImageFallbackText(
                                    fileName = filename,
                                    sourceUrl = part.url,
                                    workspaceEnabled = isWorkspaceEnabled,
                                )
                            )
                        } else {
                            part
                        }
                    }
                    else -> part
                }
            })
        }
    }
}

