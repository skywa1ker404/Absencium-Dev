package skywa1ker404.absencium.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import skywa1ker404.absencium.ui.components.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import skywa1ker404.absencium.data.share.ShareFormat

/** Opens the Sharesheet as soon as an export is ready. Place it once in any screen that can start a share. */
@Composable
fun ShareHost(viewModel: VaultViewModel) {
    val pending by viewModel.pendingShare.collectAsStateWithLifecycle()
    LaunchedEffect(pending) {
        pending?.let { viewModel.openShare(it) }
    }
}

private fun ShareFormat.hint(): String = when (this) {
    ShareFormat.PDF -> "Formatted pages, readable anywhere. Photos are not included."
    ShareFormat.MARKDOWN -> "The text with its formatting marks, for other note apps."
    ShareFormat.TEXT -> "The text only, as a .txt file."
    ShareFormat.ZIP -> "Markdown files plus the photos of each entry, in one ZIP."
}

/** Asks which format to share in. */
@Composable
fun ShareFormatDialog(formats: List<ShareFormat>, onPick: (ShareFormat) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Share as") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                formats.forEach { format ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
                            .clickable { onPick(format) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(format.label, style = MaterialTheme.typography.titleSmall)
                        Text(format.hint(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
