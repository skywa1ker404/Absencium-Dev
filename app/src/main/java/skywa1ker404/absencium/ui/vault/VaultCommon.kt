package skywa1ker404.absencium.ui.vault

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import skywa1ker404.absencium.data.db.GuideEntity
import skywa1ker404.absencium.data.db.GuideType
import skywa1ker404.absencium.data.db.NoteEntity
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.icons.AppIcons
import java.io.File
import java.text.DateFormat
import java.util.Date

/** The heading of a group of study material of this type. */
fun GuideType.sectionTitle(): String = when (this) {
    GuideType.QP -> "Previous year question papers"
    GuideType.SYLLABUS -> "Syllabus documents"
    GuideType.ANSWER_BANK -> "Answer books and banks"
    GuideType.GUIDE -> "Guides"
}

fun GuideType.label(): String = when (this) {
    GuideType.QP -> "Question paper"
    GuideType.SYLLABUS -> "Syllabus"
    GuideType.ANSWER_BANK -> "Answer bank"
    GuideType.GUIDE -> "Guide"
}

/** Opens a stored guide in a PDF viewer. Returns null on success, or a message if it could not be opened. */
fun openGuide(context: Context, guide: GuideEntity): String? {
    val uri = try {
        if (guide.localUri.startsWith("content://")) {
            Uri.parse(guide.localUri)
        } else {
            val file = File(guide.localUri)
            if (!file.isFile) return "The file for ${guide.fileName} is no longer in the notes folder."
            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        }
    } catch (e: IllegalArgumentException) {
        return "This file cannot be shared with a PDF viewer."
    }
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "application/pdf")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        context.startActivity(intent)
        null
    } catch (e: ActivityNotFoundException) {
        "No app on this phone can open PDFs."
    } catch (e: SecurityException) {
        "Permission to open this file was lost. Add it again from the Vault."
    }
}

/** Opens a file attached to a note in another app. Returns null on success, or a message if it could not be opened. */
fun openAttachment(context: Context, attachment: skywa1ker404.absencium.data.db.AttachmentEntity): String? {
    val file = File(attachment.filePath)
    if (!file.isFile) return "The file ${attachment.displayName} is no longer stored."
    val uri = try {
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    } catch (e: IllegalArgumentException) {
        return "This file cannot be shared with another app."
    }
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, attachment.mimeType.ifBlank { "*/*" })
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        context.startActivity(intent)
        null
    } catch (e: ActivityNotFoundException) {
        "No app on this phone can open ${attachment.displayName}."
    }
}

private val DateText: DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)

fun formatNoteDate(epochMs: Long): String = DateText.format(Date(epochMs))

/** A short single-line preview of a note body. */
fun NoteEntity.snippet(): String =
    bodyText.trim().replace(Regex("\\s+"), " ").take(90)

/** "2 photos, #maths #exam": what an entry carries, shown under its title so a folder can be scanned without opening anything. */
fun noteBadges(note: NoteEntity, photoCount: Int): String {
    val parts = mutableListOf<String>()
    if (photoCount > 0) parts += if (photoCount == 1) "1 photo" else "$photoCount photos"
    val tags = skywa1ker404.absencium.ui.vault.markdown.NoteTags.parse(note.tags)
    if (tags.isNotEmpty()) parts += tags.take(3).joinToString(" ") { "#$it" } + if (tags.size > 3) " +${tags.size - 3}" else ""
    return parts.joinToString("  \u00b7  ")
}

@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    photoCount: Int = 0,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    AppCard(modifier = modifier.fillMaxWidth(), onClick = onClick, onLongClick = onLongClick, highlighted = selected) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (selected) Text("\u2713", style = MaterialTheme.typography.titleMedium, color = colors.primary)
            Text(note.title.ifBlank { "Untitled note" }, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
        val preview = note.snippet()
        if (preview.isNotEmpty()) {
            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 2,
            )
        }
        val badges = noteBadges(note, photoCount)
        if (badges.isNotEmpty()) {
            Text(text = badges, style = MaterialTheme.typography.labelSmall, color = colors.primary, maxLines = 1)
        }
        Text(
            text = formatNoteDate(note.createdDate),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
fun GuideCard(
    guide: GuideEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    /** Where the material is filed in the syllabus, shown under its type. */
    topic: String? = null,
    onFile: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    AppCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(AppIcons.Book, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(guide.fileName, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    text = guide.guideType.label() + (topic?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (onFile != null) androidx.compose.material3.TextButton(onClick = onFile) { Text("Topic") }
            androidx.compose.material3.TextButton(onClick = onDelete) { Text("Remove") }
        }
    }
}
