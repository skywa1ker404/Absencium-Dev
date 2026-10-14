package com.trajectory.vault.ui.vault.entry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trajectory.vault.data.db.AttendanceEntity
import com.trajectory.vault.ui.components.GlassStage
import com.trajectory.vault.ui.components.PillButton
import com.trajectory.vault.ui.containerViewModel
import com.trajectory.vault.ui.icons.AppIcons
import com.trajectory.vault.ui.theme.LocalQuality
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Asks the Add Entry popup to open, optionally already on a subject and an absent date. */
data class AddEntryRequest(val subjectId: Long = 0L, val date: LocalDate? = null)

private val DateLabel: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())

/**
 * The Add Entry popup. It starts as "Select a Subject"; once a subject is chosen the card grows (with a spring)
 * to ask which absent date the notes are for, and Cancel shrinks to make room for Next. After Next the subject and
 * date fade away and the card shows the ways to add an entry.
 *
 * With both [initialSubjectId] and [initialDate] (from the "Add today's notes" buttons) it opens on the actions.
 */
@Composable
fun AddEntryPopup(
    onDismiss: () -> Unit,
    onSaved: (noteId: Long) -> Unit,
    onOpenAiSettings: () -> Unit,
    initialSubjectId: Long = 0L,
    initialDate: LocalDate? = null,
) {
    val viewModel = containerViewModel { AddEntryViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val absences by viewModel.absences.collectAsStateWithLifecycle()
    val q = LocalQuality.current
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(initialSubjectId, initialDate) { viewModel.begin(initialSubjectId, initialDate) }

    val subject = subjects.firstOrNull { it.id == state.subjectId }

    fun close() {
        viewModel.cancel()
        onDismiss()
    }

    fun back() {
        when (state.stage) {
            EntryStage.SUBJECT -> close()
            EntryStage.ACTIONS -> viewModel.toStage(EntryStage.SUBJECT)
            EntryStage.PICTURES, EntryStage.YOUTUBE -> viewModel.toStage(EntryStage.ACTIONS)
        }
    }

    Dialog(
        onDismissRequest = { back() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(34.dp),
            color = colors.surfaceContainerHigh,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .animateContentSize(q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)),
        ) {
            GlassStage {
                AnimatedContent(
                    targetState = state.stage,
                    transitionSpec = {
                        (fadeIn(tween(240, delayMillis = 100)) + scaleIn(initialScale = 0.94f, animationSpec = tween(240, delayMillis = 100)))
                            .togetherWith(fadeOut(tween(110))) using SizeTransform(clip = false) { _, _ -> androidx.compose.animation.core.snap() }
                    },
                    label = "entryStage",
                ) { stage ->
                    when (stage) {
                        EntryStage.SUBJECT -> SubjectStage(
                            subjects = subjects.map { it.id to it.name },
                            selectedId = state.subjectId,
                            absences = absences.filter { it.subjectId == state.subjectId },
                            selectedDate = state.date,
                            onSubject = viewModel::chooseSubject,
                            onDate = viewModel::chooseDate,
                            onCancel = { close() },
                            onNext = viewModel::next,
                        )

                        EntryStage.ACTIONS -> ActionsStage(
                            subjectName = subject?.name.orEmpty(),
                            date = state.date,
                            onBack = { back() },
                            onPictures = { viewModel.openPictures(camera = false) },
                            onCamera = { viewModel.openPictures(camera = true) },
                            onYouTube = viewModel::openYouTube,
                            onComingSoon = { name -> viewModel.showMessage("$name arrives in the next update.") },
                            message = state.message,
                        )

                        EntryStage.YOUTUBE -> YouTubeStage(
                            viewModel = viewModel,
                            onBack = { back() },
                            onSaved = onSaved,
                            onOpenAiSettings = {
                                close()
                                onOpenAiSettings()
                            },
                        )

                        EntryStage.PICTURES -> PicturesStage(
                            viewModel = viewModel,
                            subjectName = subject?.name.orEmpty(),
                            onBack = { back() },
                            onSaved = onSaved,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectStage(
    subjects: List<Pair<Long, String>>,
    selectedId: Long,
    absences: List<AttendanceEntity>,
    selectedDate: LocalDate?,
    onSubject: (Long) -> Unit,
    onDate: (LocalDate?) -> Unit,
    onCancel: () -> Unit,
    onNext: () -> Unit,
) {
    val q = LocalQuality.current
    val colors = MaterialTheme.colorScheme
    val chosen = subjects.firstOrNull { it.first == selectedId }
    var menuOpen by remember { mutableStateOf(false) }
    val nextFraction by animateFloatAsState(
        targetValue = if (chosen != null) 1f else 0f,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "nextFraction",
    )

    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Select a Subject", style = MaterialTheme.typography.headlineSmall)

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceContainerHighest)
                    .clickable { menuOpen = true }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = chosen?.second ?: if (subjects.isEmpty()) "No subjects yet" else "Choose a subject",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (chosen == null) colors.onSurfaceVariant else colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(AppIcons.ExpandMore, contentDescription = "Open subject list", tint = colors.onSurfaceVariant)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                subjects.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onSubject(id)
                            menuOpen = false
                        },
                    )
                }
            }
        }
        if (subjects.isEmpty()) {
            Text(
                "Add a subject first (Settings, Subjects). Every entry lives in a subject folder.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        AnimatedVisibility(
            visible = chosen != null,
            enter = fadeIn() + expandVertically(q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Which absent date are these notes for?", style = MaterialTheme.typography.titleMedium)
                Column(
                    modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DateRow("Not for a specific absent day", selectedDate == null) { onDate(null) }
                    absences.sortedByDescending { it.date }.forEach { record ->
                        DateRow(
                            label = record.date.format(DateLabel) + if (record.notesCollected) "  ·  notes collected" else "",
                            selected = selectedDate == record.date,
                        ) { onDate(record.date) }
                    }
                }
                if (absences.isEmpty()) {
                    Text(
                        "No absent days are logged for this subject, so you can continue without a date.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }

        // Cancel starts full width and shrinks to half as Next springs in beside it.
        Row(modifier = Modifier.fillMaxWidth()) {
            PillButton(text = "Cancel", tonal = true, onClick = onCancel, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp * nextFraction))
            PillButton(
                text = "Next",
                onClick = onNext,
                enabled = chosen != null,
                modifier = Modifier
                    .weight(nextFraction.coerceAtLeast(0.0001f))
                    .graphicsLayer { alpha = nextFraction },
            )
        }
    }
}

@Composable
private fun DateRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val container = if (selected) colors.primaryContainer else colors.surfaceContainerHighest
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) colors.onPrimaryContainer else colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) Icon(AppIcons.Check, contentDescription = "Selected", tint = colors.onPrimaryContainer, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ActionsStage(
    subjectName: String,
    date: LocalDate?,
    onBack: () -> Unit,
    onPictures: () -> Unit,
    onCamera: () -> Unit,
    onYouTube: () -> Unit,
    onComingSoon: (String) -> Unit,
    message: String?,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SquareIconButton(AppIcons.ArrowBack, "Back", onBack)
            Column {
                Text("Add an entry", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = subjectName + (date?.let { "  ·  ${it.format(DateLabel)}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        // Seven buttons in a bento layout: one wide, then pairs. Each has its own corner shape that softens when pressed.
        ActionTile("Upload a Pic", AppIcons.Image, colors.primaryContainer, colors.onPrimaryContainer, 0, onPictures, Modifier.fillMaxWidth(), 84.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile("Text Notes", AppIcons.Edit, colors.secondaryContainer, colors.onSecondaryContainer, 1, { onComingSoon("Text Notes") }, Modifier.weight(1f), 104.dp)
            ActionTile("Document / File", AppIcons.Description, colors.tertiaryContainer, colors.onTertiaryContainer, 2, { onComingSoon("Document / File") }, Modifier.weight(1f), 104.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile("Audio Note", AppIcons.Mic, colors.tertiaryContainer, colors.onTertiaryContainer, 3, { onComingSoon("Audio Note") }, Modifier.weight(1f), 104.dp)
            ActionTile("Camera", AppIcons.PhotoCamera, colors.secondaryContainer, colors.onSecondaryContainer, 0, onCamera, Modifier.weight(1f), 104.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile("Solve Homework", AppIcons.School, colors.primaryContainer, colors.onPrimaryContainer, 2, { onComingSoon("Solve Homework") }, Modifier.weight(1f), 104.dp)
            ActionTile("Generate AI Notes", AppIcons.AutoAwesome, colors.secondaryContainer, colors.onSecondaryContainer, 1, { onComingSoon("Generate AI Notes") }, Modifier.weight(1f), 104.dp)
        }
        ActionTile("Transcribe YouTube Video", AppIcons.PlayCircle, colors.tertiaryContainer, colors.onTertiaryContainer, 3, onYouTube, Modifier.fillMaxWidth(), 84.dp)
        if (message != null) {
            Text(message, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

/** A tile with its own corner personality ([variant] picks which corners are round) that relaxes while pressed. */
@Composable
private fun ActionTile(
    title: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    variant: Int,
    onClick: () -> Unit,
    modifier: Modifier,
    height: androidx.compose.ui.unit.Dp,
) {
    val q = LocalQuality.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val big by animateDpAsState(if (pressed) 20.dp else 34.dp, q.spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "tileBig")
    val small by animateDpAsState(if (pressed) 20.dp else 10.dp, q.spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "tileSmall")
    val shape = when (variant) {
        0 -> RoundedCornerShape(topStart = big, topEnd = small, bottomEnd = big, bottomStart = small)
        1 -> RoundedCornerShape(topStart = small, topEnd = big, bottomEnd = small, bottomStart = big)
        2 -> RoundedCornerShape(topStart = big, topEnd = big, bottomEnd = small, bottomStart = small)
        else -> RoundedCornerShape(topStart = small, topEnd = small, bottomEnd = big, bottomStart = big)
    }
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, q.spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "tileScale")
    Column(
        modifier = modifier
            .heightIn(min = height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(container)
            .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(26.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = content, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
internal fun SquareIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceContainerHighest)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = colors.onSurface, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun PicturesStage(
    viewModel: AddEntryViewModel,
    subjectName: String,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    var preview by remember { mutableStateOf<Int?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        viewModel.addPictures(uris)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        viewModel.cameraFinished(taken)
    }

    Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SquareIconButton(AppIcons.ArrowBack, "Back", onBack)
            Text(if (state.cameraFirst) "Camera" else "Upload a Pic", style = MaterialTheme.typography.titleLarge)
        }

        // The big button: wide, with the supported formats under it.
        val primaryIsCamera = state.cameraFirst
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 12.dp, bottomEnd = 30.dp, bottomStart = 12.dp))
                .background(colors.primaryContainer)
                .clickable {
                    if (primaryIsCamera) camera.launch(viewModel.prepareCameraFile()) else picker.launch("image/*")
                }
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                if (primaryIsCamera) AppIcons.PhotoCamera else AppIcons.Image,
                contentDescription = null,
                tint = colors.onPrimaryContainer,
                modifier = Modifier.size(30.dp),
            )
            Text(
                if (primaryIsCamera) "Take a photo" else "Choose pictures from your phone",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onPrimaryContainer,
            )
            Text(
                if (primaryIsCamera) "Take as many as you need. Each one is added below." else "Supported: JPG, PNG, WebP, GIF and BMP",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = if (primaryIsCamera) "Or choose pictures you already have" else "Or take a photo now",
            style = MaterialTheme.typography.labelLarge,
            color = colors.primary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable {
                    if (primaryIsCamera) picker.launch("image/*") else camera.launch(viewModel.prepareCameraFile())
                }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )

        AnimatedVisibility(visible = state.pictures.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.pictures, key = { it.id }) { picture ->
                    PhotoThumb(
                        path = picture.path,
                        onClick = { preview = state.pictures.indexOf(picture) },
                        onRemove = { viewModel.removePicture(picture.id) },
                    )
                }
            }
        }

        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::setTitle,
            label = { Text("Title (optional)") },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PillButton(text = "Back", tonal = true, onClick = onBack, modifier = Modifier.weight(1f))
            PillButton(
                text = if (state.pictures.size > 1) "Save ${state.pictures.size} photos" else "Save",
                loading = state.busy,
                enabled = state.pictures.isNotEmpty(),
                onClick = { viewModel.savePictures(subjectName, onSaved) },
                modifier = Modifier.weight(1f),
            )
        }
    }

    preview?.let { index ->
        PhotoPreviewDialog(
            paths = state.pictures.map { it.path },
            startIndex = index.coerceIn(0, (state.pictures.size - 1).coerceAtLeast(0)),
            onBack = { preview = null },
        )
    }
}

/** A full-screen, in-app photo viewer with a back button. Swipe sideways to move between photos. */
@Composable
fun PhotoPreviewDialog(paths: List<String>, startIndex: Int, onBack: () -> Unit, version: Int = 0) {
    if (paths.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val pager = rememberPagerState(initialPage = startIndex, pageCount = { paths.size })
    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.94f))) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                val bitmap by rememberPhoto(paths[page], 2048, version)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    bitmap?.let {
                        androidx.compose.foundation.Image(
                            bitmap = it,
                            contentDescription = "Photo ${page + 1} of ${paths.size}",
                            modifier = Modifier.fillMaxSize().padding(top = 72.dp, bottom = 24.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SquareIconButton(AppIcons.ArrowBack, "Back", onBack)
                Text(
                    text = "${pager.currentPage + 1} / ${paths.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
        }
    }
}

/** ". .. ..." that keeps counting up while something runs. */
@Composable
internal fun LoadingDots(label: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    val dots by androidx.compose.runtime.produceState(".") {
        var count = 1
        while (true) {
            value = ".".repeat(count)
            count = if (count == 3) 1 else count + 1
            kotlinx.coroutines.delay(420)
        }
    }
    Text(text = "$label $dots", style = MaterialTheme.typography.bodyMedium, color = color, modifier = modifier)
}

@Composable
private fun YouTubeStage(
    viewModel: AddEntryViewModel,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onOpenAiSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val context = androidx.compose.ui.platform.LocalContext.current
    val running = state.youtubeStep != YouTubeStep.IDLE

    Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SquareIconButton(AppIcons.ArrowBack, "Back", onBack)
            Text("Transcribe YouTube Video", style = MaterialTheme.typography.titleLarge)
        }
        OutlinedTextField(
            value = state.youtubeUrl,
            onValueChange = viewModel::setYouTubeUrl,
            label = { Text("Paste a YouTube link") },
            singleLine = true,
            enabled = !running,
            shape = RoundedCornerShape(20.dp),
            trailingIcon = {
                Text(
                    text = "Paste",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !running) {
                            val manager = context.getSystemService(android.content.ClipboardManager::class.java)
                            manager?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
                                ?.let { viewModel.setYouTubeUrl(it.trim()) }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        state.videoError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error) }
        if (state.videoLoading) LoadingDots("Fetching video details")

        val video = state.video
        AnimatedVisibility(visible = video != null) {
            if (video != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.surfaceContainerHighest),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(colors.surfaceContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        val thumb = state.videoThumb
                        if (thumb != null) {
                            androidx.compose.foundation.Image(
                                bitmap = thumb.asImageBitmap(),
                                contentDescription = "Video thumbnail",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(AppIcons.PlayCircle, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(40.dp))
                        }
                    }
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(video.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                        if (video.channel.isNotBlank()) {
                            Text(video.channel, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (running) {
            LoadingDots(
                label = if (state.youtubeStep == YouTubeStep.TRANSCRIPT) "Fetching the transcript" else "Writing your notes",
            )
        }
        state.youtubeError?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error)
            if (state.video != null) {
                Text(
                    "You can try again, or save just the transcript without AI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        AnimatedVisibility(visible = state.aiSetupMissing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.secondaryContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("AI is not set up yet", style = MaterialTheme.typography.titleSmall, color = colors.onSecondaryContainer)
                Text(
                    "Add an AI provider, a model and your API key in Settings, AI & Integrations, then come back. " +
                        "Or skip the AI and keep just the transcript.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSecondaryContainer,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton(text = "Open Settings", compact = true, onClick = onOpenAiSettings, modifier = Modifier.weight(1f))
                    PillButton(
                        text = "Skip AI",
                        compact = true,
                        tonal = true,
                        onClick = { viewModel.transcribe(skipAi = true, onSaved = onSaved) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PillButton(text = "Back", tonal = true, onClick = onBack, enabled = !running, modifier = Modifier.weight(1f))
            PillButton(
                text = "Transcribe & Generate Notes",
                enabled = video != null && !running,
                loading = running,
                onClick = { viewModel.transcribe(skipAi = false, onSaved = onSaved) },
                modifier = Modifier.weight(2f),
            )
        }
        if (state.youtubeError != null && video != null && !running) {
            Text(
                text = "Save the transcript only",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .clickable { viewModel.transcribe(skipAi = true, onSaved = onSaved) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
