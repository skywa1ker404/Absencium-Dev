package skywa1ker404.absencium.ui.vault

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import skywa1ker404.absencium.ui.components.AppMenu
import skywa1ker404.absencium.ui.components.AppMenuItem
import skywa1ker404.absencium.ui.components.hueFor
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

/** A small rounded label on a tinted card: a topic, a count, a status. */
@Composable
fun CardChip(text: String, color: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier = modifier.clip(CircleShape).background(color.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * A note in a list, laid out like the tinted cards of reference image 03: a hue per topic, a large title, a clean text preview
 * (no raw markers), and small chips for its topic, pictures, tags, date and status.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    photoCount: Int = 0,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    /** Where the note is filed in the syllabus, or null for Uncategorized. */
    topic: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val hue = hueFor(note.syllabusNodeId ?: note.id)
    val lines = remember(note.bodyText) { notePreviewLines(note.bodyText) }
    val ink = hue.onContainer
    AppCard(
        modifier = modifier.fillMaxWidth().then(
            if (selected) Modifier.border(2.dp, colors.primary, MaterialTheme.shapes.large) else Modifier,
        ),
        onClick = onClick,
        onLongClick = onLongClick,
        container = hue.container.copy(alpha = 0.75f),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                note.title.ifBlank { "Untitled note" },
                style = MaterialTheme.typography.titleLarge,
                color = ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(colors.primary),
                    contentAlignment = Alignment.Center,
                ) { Icon(AppIcons.Check, contentDescription = "Selected", tint = colors.onPrimary, modifier = Modifier.size(18.dp)) }
            }
        }
        if (lines.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                lines.forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodyMedium, color = ink.copy(alpha = 0.82f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (topic != null) CardChip(topic, hue.accent, icon = AppIcons.Folder)
            if (photoCount > 0) CardChip(if (photoCount == 1) "1 photo" else "$photoCount photos", ink, icon = AppIcons.Image)
            skywa1ker404.absencium.ui.vault.markdown.NoteTags.parse(note.tags).take(3).forEach { CardChip("#$it", ink) }
            if (note.status == skywa1ker404.absencium.data.db.NoteStatus.FINISHED) CardChip("Finished", hue.accent, icon = AppIcons.Check)
            CardChip(formatNoteDate(note.createdDate), ink)
        }
    }
}

/** The symbol of a kind of study material. */
private fun GuideType.icon(): ImageVector = when (this) {
    GuideType.QP -> AppIcons.Quiz
    GuideType.SYLLABUS -> AppIcons.School
    GuideType.ANSWER_BANK -> AppIcons.Check
    GuideType.GUIDE -> AppIcons.Book
}

/**
 * A piece of study material as a pastel row (reference image 09): a white rounded-square icon, the file name, its kind and
 * topic, and an Open pill. The menu files it under a topic or removes it.
 */
@Composable
fun GuideCard(
    guide: GuideEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    /** Where the material is filed in the syllabus, shown under its type. */
    topic: String? = null,
    onFile: (() -> Unit)? = null,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val hue = hueFor(guide.guideType.ordinal.toLong())
    var menu by remember { mutableStateOf(false) }
    AppCard(
        modifier = modifier.fillMaxWidth().then(if (selected) Modifier.border(2.dp, colors.primary, MaterialTheme.shapes.large) else Modifier),
        onClick = onClick,
        onLongClick = onLongClick,
        container = hue.container.copy(alpha = 0.75f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(colors.surface.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center,
            ) { Icon(guide.guideType.icon(), contentDescription = null, tint = hue.accent, modifier = Modifier.size(24.dp)) }
            Column(modifier = Modifier.weight(1f)) {
                Text(guide.fileName, style = MaterialTheme.typography.titleMedium, color = hue.onContainer, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    text = guide.guideType.label() + (topic?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = hue.onContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "Open",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurface,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.surface.copy(alpha = 0.9f))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
            Box {
                IconButton(onClick = { menu = true }) { Icon(AppIcons.MoreVert, contentDescription = "More", tint = hue.onContainer) }
                AppMenu(expanded = menu, onDismiss = { menu = false }) {
                    if (onFile != null) AppMenuItem(AppIcons.Folder, "File under a topic", { menu = false; onFile() })
                    AppMenuItem(AppIcons.Delete, "Remove", { menu = false; onDelete() }, destructive = true)
                }
            }
        }
    }
}


/** A note as one compact line (list mode): a dot in the topic's colour, the title, and the topic, date and status under it. */
@Composable
fun NoteRow(
    note: NoteEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    topic: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val hue = hueFor(note.syllabusNodeId ?: note.id)
    val date = remember(note.createdDate) {
        java.time.Instant.ofEpochMilli(note.createdDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
    }
    AppCard(
        modifier = modifier.fillMaxWidth().then(if (selected) Modifier.border(2.dp, colors.primary, MaterialTheme.shapes.large) else Modifier),
        onClick = onClick,
        onLongClick = onLongClick,
        container = hue.container.copy(alpha = 0.6f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(hue.accent))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    note.title.ifBlank { "Untitled note" },
                    style = MaterialTheme.typography.titleMedium,
                    color = hue.onContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(topic, date).joinToString("  \u00b7  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = hue.onContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A piece of study material as one compact line (list mode). */
@Composable
fun GuideRow(
    guide: GuideEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    topic: String? = null,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val hue = hueFor(guide.guideType.ordinal.toLong())
    AppCard(
        modifier = modifier.fillMaxWidth().then(if (selected) Modifier.border(2.dp, colors.primary, MaterialTheme.shapes.large) else Modifier),
        onClick = onClick,
        onLongClick = onLongClick,
        container = hue.container.copy(alpha = 0.6f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(guide.guideType.icon(), contentDescription = null, tint = hue.accent, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(guide.fileName, style = MaterialTheme.typography.titleMedium, color = hue.onContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    guide.guideType.label() + (topic?.let { "  \u00b7  $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = hue.onContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
