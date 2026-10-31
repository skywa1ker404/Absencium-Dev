package skywa1ker404.absencium.ui.vault

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import skywa1ker404.absencium.data.share.PdfTheme
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.theme.LocalQuality
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

/**
 * Share or export a note. The only real choice is PDF, with a toggle button for a themed PDF (the page takes the app's current
 * light or dark theme and colours, so what is shared looks like what is on screen). Sharewave, the planned way to hand a note to
 * another Absencium, is shown greyed out until it exists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareFormatDialog(onPick: (ShareFormat, PdfTheme?) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val q = LocalQuality.current
    var themed by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "themedPress",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(32.dp),
        title = { Text("Share or export") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.surfaceContainerHigh)
                        .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(AppIcons.Description, contentDescription = null, tint = colors.primary, modifier = Modifier.size(28.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PDF", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (themed) "Themed PDF: the page uses your current theme, light or dark, with its colours."
                            else "Plain pages, readable anywhere. Photos and doodles are included, recordings are not.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    FilledIconToggleButton(
                        checked = themed,
                        onCheckedChange = { themed = it },
                        interactionSource = interaction,
                        modifier = Modifier.graphicsLayer { scaleX = press; scaleY = press }.semantics { contentDescription = "Themed PDF" },
                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                            containerColor = colors.surfaceContainerHighest,
                            contentColor = colors.onSurfaceVariant,
                            checkedContainerColor = colors.primary,
                            checkedContentColor = colors.onPrimary,
                        ),
                    ) {
                        Icon(AppIcons.Palette, contentDescription = null)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.4f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.surfaceContainerHigh)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .semantics { contentDescription = "Sharewave, not available yet" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(AppIcons.Share, contentDescription = null, modifier = Modifier.size(28.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sharewave", style = MaterialTheme.typography.titleMedium)
                        Text("Send a note straight to another Absencium. Coming later.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            PillButton(
                text = "Share PDF",
                onClick = {
                    val theme = if (themed) PdfTheme(
                        background = colors.background.toArgb(),
                        text = colors.onBackground.toArgb(),
                        shade = colors.surfaceContainerHigh.toArgb(),
                        line = colors.outline.toArgb(),
                        highlight = lerp(colors.background, colors.primary, 0.35f).toArgb(),
                    ) else null
                    onPick(ShareFormat.PDF, theme)
                },
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
