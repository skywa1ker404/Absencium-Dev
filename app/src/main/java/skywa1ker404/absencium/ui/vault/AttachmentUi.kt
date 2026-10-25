package skywa1ker404.absencium.ui.vault

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import skywa1ker404.absencium.data.ocr.ExtractMode
import skywa1ker404.absencium.data.ocr.FileKind
import skywa1ker404.absencium.ui.icons.AppIcons

/** One tile of an [IconMenu]. */
class MenuAction(val label: String, val icon: ImageVector, val destructive: Boolean = false, val onClick: () -> Unit)

/**
 * The popup that opens when you tap a picture, doodle, recording, file or table: every action is a tile with an icon and a short
 * caption, in two columns (up to four actions) or three. The tile closes the menu, then does its job.
 */
@Composable
fun IconMenu(title: String?, actions: List<MenuAction>, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val columns = if (actions.size <= 4) 2 else 3
    val open = remember { MutableTransitionState(false).also { it.targetState = true } }
    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visibleState = open,
            enter = fadeIn(tween(120)) + scaleIn(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.9f),
        ) {
            Surface(shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerHigh, tonalElevation = 6.dp) {
                Column(
                    modifier = Modifier.padding(16.dp).heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (title != null) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                        )
                    }
                    actions.chunked(columns).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { action ->
                                val tint = if (action.destructive) colors.error else colors.onSurface
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (action.destructive) colors.errorContainer.copy(alpha = 0.45f) else colors.surfaceContainerHighest)
                                        .clickable { onDismiss(); action.onClick() }
                                        .padding(horizontal = 6.dp, vertical = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(action.icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
                                    Text(action.label, style = MaterialTheme.typography.labelMedium, color = tint, textAlign = TextAlign.Center, maxLines = 2)
                                }
                            }
                            repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Asks how to get the text out of a file: with the user's AI, or on the phone without it. Says plainly what each one means.
 * [onSkip] (when given) is a third way out that leaves the file as it is.
 */
@Composable
fun ExtractChoiceDialog(
    fileName: String,
    kind: FileKind,
    aiReady: Boolean,
    onChoose: (ExtractMode) -> Unit,
    onDismiss: () -> Unit,
    skipLabel: String? = null,
    onSkip: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val open = remember { MutableTransitionState(false).also { it.targetState = true } }
    val noun = when (kind) {
        FileKind.IMAGE -> "picture"
        FileKind.PDF -> "PDF"
        FileKind.AUDIO -> "recording"
        else -> "document"
    }
    val localPossible = kind != FileKind.AUDIO && kind != FileKind.OTHER
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = true)) {
        AnimatedVisibility(
            visibleState = open,
            enter = fadeIn(tween(140)) + scaleIn(spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.88f),
        ) {
            Surface(shape = RoundedCornerShape(30.dp), color = colors.surfaceContainerHigh, tonalElevation = 6.dp) {
                Column(
                    modifier = Modifier.padding(18.dp).heightIn(max = 620.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(if (kind == FileKind.AUDIO) AppIcons.Transcribe else AppIcons.ExtractText, contentDescription = null, tint = colors.primary, modifier = Modifier.size(26.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (kind == FileKind.AUDIO) "Turn into text" else "Extract text", style = MaterialTheme.typography.titleMedium)
                            Text(fileName, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1)
                        }
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onDismiss).semantics { contentDescription = "Close" },
                            contentAlignment = Alignment.Center,
                        ) { Icon(AppIcons.Close, contentDescription = null) }
                    }
                    ChoiceCard(
                        icon = AppIcons.AutoAwesome,
                        title = "With AI",
                        body = "Sends this $noun to your AI provider with your own API key. It needs internet, some providers charge for it, " +
                            "and the provider can see what is in it. " + when (kind) {
                            FileKind.AUDIO -> "It works only with a model that accepts audio, such as Google Gemini."
                            FileKind.IMAGE, FileKind.PDF -> "It copes better with handwriting, tables and messy photos."
                            else -> "It tidies the text into clean notes."
                        } + if (!aiReady) " AI is off or not set up: add it in Settings, AI & Integrations." else "",
                        enabled = true,
                        highlighted = aiReady,
                        onClick = { onDismiss(); onChoose(ExtractMode.AI) },
                    )
                    ChoiceCard(
                        icon = AppIcons.ExtractText,
                        title = "Locally (without AI)",
                        body = when (kind) {
                            FileKind.AUDIO -> "A recording cannot be turned into text without AI."
                            FileKind.IMAGE -> "Runs on this phone with no network, and nothing leaves it. Works best on clear, straight, printed English text; handwriting and tables may come out wrong."
                            FileKind.PDF -> "Runs on this phone with no network, and nothing leaves it. Each page is read as a picture, so it works best on clear printed English text. A long PDF takes a while (the first 30 pages are read)."
                            else -> "Reads the text straight out of the file on this phone, with no network."
                        },
                        enabled = localPossible,
                        highlighted = false,
                        onClick = { onDismiss(); onChoose(ExtractMode.LOCAL) },
                    )
                    if (onSkip != null && skipLabel != null) {
                        Text(
                            skipLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .clickable { onDismiss(); onSkip() }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceCard(icon: ImageVector, title: String, body: String, enabled: Boolean, highlighted: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (highlighted) colors.primaryContainer else colors.surfaceContainerHighest)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val content = when {
            !enabled -> colors.onSurfaceVariant.copy(alpha = 0.55f)
            highlighted -> colors.onPrimaryContainer
            else -> colors.onSurface
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = content)
        }
        Text(body, style = MaterialTheme.typography.bodySmall, color = content)
    }
}

/** What an extraction is doing right now, shown over the page. [step] is a short phrase such as "Reading page 3". */
@Composable
fun ExtractRunningDialog(step: String, onCancel: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Surface(shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerHigh, tonalElevation = 6.dp) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                skywa1ker404.absencium.ui.vault.entry.LoadingDots(step.ifBlank { "Working" }, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.surfaceContainerHighest).clickable(onClick = onCancel).semantics { contentDescription = "Stop" },
                    contentAlignment = Alignment.Center,
                ) { Icon(AppIcons.Stop, contentDescription = null) }
            }
        }
    }
}

/** The text that came out, for a last look and fixes before it is used. */
@Composable
fun ExtractResultDialog(
    title: String,
    text: String,
    acceptLabel: String,
    acceptIcon: ImageVector,
    onAccept: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var edited by remember(text) { mutableStateOf(text) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = true)) {
        Surface(shape = RoundedCornerShape(30.dp), color = colors.surfaceContainerHigh, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = edited,
                    onValueChange = { edited = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 360.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.surfaceContainerHighest).clickable(onClick = onDismiss).semantics { contentDescription = "Discard this text" },
                        contentAlignment = Alignment.Center,
                    ) { Icon(AppIcons.Delete, contentDescription = null) }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(colors.primary)
                            .clickable(enabled = edited.isNotBlank()) { onAccept(edited.trim()) }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(acceptIcon, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(22.dp))
                        Text(acceptLabel, style = MaterialTheme.typography.labelLarge, color = colors.onPrimary)
                    }
                }
            }
        }
    }
}

/** A message that needs one tap to dismiss (a failed extraction, AI not set up). */
@Composable
fun ExtractProblemDialog(message: String, setupMissing: Boolean, onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerHigh, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (setupMissing) "AI is off or not set up" else "Could not get the text", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (setupMissing) "Add an AI provider, a model and your API key in Settings, AI & Integrations. Or choose Locally to read pictures and PDFs on this phone." else message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (setupMissing) {
                        Text(
                            "Open Settings",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onPrimary,
                            modifier = Modifier.clip(CircleShape).background(colors.primary).clickable { onDismiss(); onOpenSettings() }.padding(horizontal = 18.dp, vertical = 10.dp),
                        )
                    }
                    Text(
                        "Close",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clip(CircleShape).background(colors.surfaceContainerHighest).clickable(onClick = onDismiss).padding(horizontal = 18.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private val PdfThread: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)

/**
 * A PDF opened inside the app, one page after another, drawn by Android's own renderer. The icons at the top turn its text into a
 * note ([onExtract]) or hand it to another app ([onOpenElsewhere]).
 */
@Composable
fun PdfViewerDialog(path: String, name: String, onClose: () -> Unit, onExtract: (() -> Unit)?, onOpenElsewhere: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val file = remember(path) { java.io.File(path) }
    val renderer by produceState<PdfRenderer?>(null, path) {
        value = withContext(PdfThread) {
            try {
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                try {
                    PdfRenderer(fd)
                } catch (e: Exception) {
                    fd.close()
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    var failed by remember(path) { mutableStateOf(false) }
    DisposableEffect(renderer) {
        val open = renderer
        onDispose { if (open != null) runCatching { open.close() } }
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(modifier = Modifier.fillMaxSize().background(colors.surface).statusBarsPadding().navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClose).semantics { contentDescription = "Close the PDF" },
                    contentAlignment = Alignment.Center,
                ) { Icon(AppIcons.Close, contentDescription = null) }
                Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, modifier = Modifier.weight(1f).padding(horizontal = 6.dp))
                if (onExtract != null) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onExtract).semantics { contentDescription = "Extract text" },
                        contentAlignment = Alignment.Center,
                    ) { Icon(AppIcons.ExtractText, contentDescription = null) }
                }
                if (onOpenElsewhere != null) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onOpenElsewhere).semantics { contentDescription = "Open in another app" },
                        contentAlignment = Alignment.Center,
                    ) { Icon(AppIcons.OpenInNew, contentDescription = null) }
                }
            }
            val open = renderer
            if (open == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (failed || !file.isFile) "This PDF could not be opened. It may be damaged or protected by a password." else "Opening…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
                if (!failed) {
                    androidx.compose.runtime.LaunchedEffect(path) {
                        kotlinx.coroutines.delay(2500)
                        if (renderer == null) failed = true
                    }
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val widthPx = with(density) { maxWidth.roundToPx() }.coerceIn(300, 1700)
                    val pages = remember(open) { (0 until open.pageCount).toList() }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().background(colors.surfaceContainerLowest),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(pages, key = { it }) { index ->
                            val page by produceState<ImageBitmap?>(null, open, index, widthPx) {
                                value = withContext(PdfThread) {
                                    try {
                                        open.openPage(index).use { p ->
                                            val scale = widthPx.toFloat() / p.width.coerceAtLeast(1)
                                            val bitmap = Bitmap.createBitmap(widthPx, (p.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                                            bitmap.eraseColor(AndroidColor.WHITE)
                                            p.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                            bitmap.asImageBitmap()
                                        }
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                            }
                            val image = page
                            if (image != null) {
                                Image(
                                    bitmap = image,
                                    contentDescription = "Page ${index + 1}",
                                    modifier = Modifier.fillMaxWidth().background(Color.White),
                                    contentScale = ContentScale.FillWidth,
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp).background(Color.White), contentAlignment = Alignment.Center) {
                                    Text("Page ${index + 1}", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
