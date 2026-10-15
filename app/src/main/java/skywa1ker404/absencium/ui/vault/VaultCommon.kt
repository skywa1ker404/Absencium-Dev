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

fun GuideType.label(): String = when (this) {
    GuideType.QP -> "Question paper"
    GuideType.SYLLABUS -> "Syllabus"
    GuideType.ANSWER_BANK -> "Answer bank"
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

private val DateText: DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)

fun formatNoteDate(epochMs: Long): String = DateText.format(Date(epochMs))

/** A short single-line preview of a note body. */
fun NoteEntity.snippet(): String =
    bodyText.trim().replace(Regex("\\s+"), " ").take(90)

@Composable
fun NoteCard(note: NoteEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    AppCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Text(note.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        val preview = note.snippet()
        if (preview.isNotEmpty()) {
            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 2,
            )
        }
        Text(
            text = formatNoteDate(note.createdDate),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
fun GuideCard(guide: GuideEntity, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    AppCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(AppIcons.Book, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(guide.fileName, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    text = guide.guideType.label(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            androidx.compose.material3.TextButton(onClick = onDelete) { Text("Remove") }
        }
    }
}
