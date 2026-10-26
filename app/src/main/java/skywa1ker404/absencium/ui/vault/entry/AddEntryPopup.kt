package skywa1ker404.absencium.ui.vault.entry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import skywa1ker404.absencium.ui.components.AppTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import skywa1ker404.absencium.data.db.AttendanceEntity
import skywa1ker404.absencium.ui.components.DropdownOption
import skywa1ker404.absencium.ui.components.InlineDropdown
import skywa1ker404.absencium.ui.components.AnimatedPopup
import skywa1ker404.absencium.ui.components.DatePickerPopup
import skywa1ker404.absencium.ui.components.PopupShell
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.containerViewModel
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.theme.LocalQuality
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
    var pickingDate by remember { mutableStateOf(false) }
    var pendingDocs by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        pendingDocs = uris
    }

    fun close() {
        viewModel.cancel()
        onDismiss()
    }

    fun back() {
        when (state.stage) {
            EntryStage.SUBJECT -> close()
            EntryStage.ACTIONS -> viewModel.toStage(EntryStage.SUBJECT)
            EntryStage.PICTURES, EntryStage.YOUTUBE, EntryStage.AUDIO, EntryStage.SOLVE, EntryStage.GENERATE -> viewModel.toStage(EntryStage.ACTIONS)
        }
    }

    PopupShell(onDismissRequest = { back() }) {
                    AnimatedContent(
                        targetState = state.stage,
                        transitionSpec = {
                            fadeIn(tween(170, delayMillis = 90)).togetherWith(fadeOut(tween(90))) using
                                SizeTransform(clip = false) { _, _ -> snap() }
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
                                onPickCustom = { pickingDate = true },
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
                                onTextNote = { viewModel.createTextNote(subject?.name.orEmpty(), onSaved) },
                                onDocuments = { documentPicker.launch(arrayOf("*/*")) },
                                onAudio = viewModel::openAudio,
                                onSolve = viewModel::openSolve,
                                onGenerate = viewModel::openGenerate,
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

                            EntryStage.AUDIO -> AudioStage(
                                viewModel = viewModel,
                                subjectName = subject?.name.orEmpty(),
                                onBack = { back() },
                                onSaved = onSaved,
                            )

                            EntryStage.SOLVE, EntryStage.GENERATE -> AiTaskStage(
                                viewModel = viewModel,
                                solve = stage == EntryStage.SOLVE,
                                subjectName = subject?.name.orEmpty(),
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
                                onOpenAiSettings = {
                                    close()
                                    onOpenAiSettings()
                                },
                            )
                        }
                    }
    }

    if (pendingDocs.isNotEmpty()) {
        // The list is copied first: the dialog clears pendingDocs when it closes, before it calls the action.
        val docs = pendingDocs
        val first = docs.first()
        val resolver = androidx.compose.ui.platform.LocalContext.current.contentResolver
        val mime = remember(first) { resolver.getType(first).orEmpty() }
        skywa1ker404.absencium.ui.vault.ExtractChoiceDialog(
            fileName = if (docs.size == 1) first.lastPathSegment.orEmpty().substringAfterLast('/') else "${docs.size} files",
            kind = skywa1ker404.absencium.data.ocr.FileKind.of(mime, first.lastPathSegment.orEmpty()),
            aiReady = viewModel.aiAvailable(),
            onConfirm = { viewModel.addDocuments(docs, subject?.name.orEmpty(), true, onSaved) },
            onDismiss = { pendingDocs = emptyList() },
            skipLabel = "Just attach",
            onSkip = { viewModel.addDocuments(docs, subject?.name.orEmpty(), false, onSaved) },
        )
    }

    AnimatedPopup(if (pickingDate) true else null) {
        DatePickerPopup(
            initial = state.date ?: java.time.LocalDate.now(),
            onPicked = viewModel::chooseDate,
            onDismiss = { pickingDate = false },
        )
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
    onPickCustom: () -> Unit,
    onCancel: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val chosen = subjects.firstOrNull { it.first == selectedId }
    // 0 = only Cancel, 1 = Cancel and Next side by side. A plain eased tween, because a width that overshoots
    // full width would run outside the card.
    val split by animateFloatAsState(if (chosen != null) 1f else 0f, tween(300, easing = FastOutSlowInEasing), label = "buttonSplit")
    val reveal by animateFloatAsState(if (chosen != null) 1f else 0f, tween(260, delayMillis = 60), label = "dateReveal")

    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Select a Subject", style = MaterialTheme.typography.headlineSmall)

        InlineDropdown(
            options = subjects.map { DropdownOption(it.first, it.second) },
            selected = chosen?.first,
            onSelect = onSubject,
            placeholder = if (subjects.isEmpty()) "No subjects yet" else "Choose a subject",
            enabled = subjects.isNotEmpty(),
        )
        if (subjects.isEmpty()) {
            Text(
                "Add a subject first (Settings, Subjects). Every entry lives in a subject folder.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        if (chosen != null) {
            // Present at full size from the first frame (the card's spring does the growing), fading in on top.
            Column(
                modifier = Modifier.graphicsLayer { alpha = reveal },
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Which absent date are these notes for?", style = MaterialTheme.typography.titleMedium)
                Column(
                    modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DateRow("Not for a specific absent day", selectedDate == null) { onDate(null) }
                    // A date typed in by hand (not one of the logged absences) gets its own row so it can be seen.
                    if (selectedDate != null && absences.none { it.date == selectedDate }) {
                        DateRow(selectedDate.format(DateLabel) + "  ·  custom date", true) { onPickCustom() }
                    }
                    absences.sortedByDescending { it.date }.forEach { record ->
                        DateRow(
                            label = record.date.format(DateLabel) + if (record.notesCollected) "  ·  notes collected" else "",
                            selected = selectedDate == record.date,
                        ) { onDate(record.date) }
                    }
                }
                DateRow("Pick a custom date…", false) { onPickCustom() }
                if (absences.isEmpty()) {
                    Text(
                        "No absent days are logged for this subject. You can continue without a date or pick any date.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }

        // Cancel starts full width and shrinks to half as Next slides in beside it. Widths are worked out from the
        // row's real width, and Next keeps one fixed width so its label never re-wraps while the row animates.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val gap = 10.dp
            val half = (maxWidth - gap) / 2
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(half)
                    .graphicsLayer {
                        alpha = split
                        translationX = (1f - split) * (half.toPx() * 0.35f)
                    },
            ) {
                PillButton(text = "Next", onClick = onNext, enabled = chosen != null, modifier = Modifier.fillMaxWidth())
            }
            PillButton(
                text = "Cancel",
                tonal = true,
                onClick = onCancel,
                modifier = Modifier.width(maxWidth - (maxWidth - half) * split),
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
    onTextNote: () -> Unit,
    onDocuments: () -> Unit,
    onAudio: () -> Unit,
    onSolve: () -> Unit,
    onGenerate: () -> Unit,
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
            ActionTile("Text Notes", AppIcons.Edit, colors.secondaryContainer, colors.onSecondaryContainer, 1, onTextNote, Modifier.weight(1f), 104.dp)
            ActionTile("Document / File", AppIcons.Description, colors.tertiaryContainer, colors.onTertiaryContainer, 2, onDocuments, Modifier.weight(1f), 104.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile("Audio Note", AppIcons.Mic, colors.tertiaryContainer, colors.onTertiaryContainer, 3, onAudio, Modifier.weight(1f), 104.dp)
            ActionTile("Camera", AppIcons.PhotoCamera, colors.secondaryContainer, colors.onSecondaryContainer, 0, onCamera, Modifier.weight(1f), 104.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile("Solve Homework", AppIcons.School, colors.primaryContainer, colors.onPrimaryContainer, 2, onSolve, Modifier.weight(1f), 104.dp)
            ActionTile("Generate AI Notes", AppIcons.AutoAwesome, colors.secondaryContainer, colors.onSecondaryContainer, 1, onGenerate, Modifier.weight(1f), 104.dp)
        }
        ActionTile("YouTube Video Notes", AppIcons.PlayCircle, colors.tertiaryContainer, colors.onTertiaryContainer, 3, onYouTube, Modifier.fillMaxWidth(), 84.dp)
        if (message != null) {
            Text(message, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

/** A tile with its own corner personality ([variant] picks which corners are round) that relaxes while pressed. */
@Composable
internal fun ActionTile(
    title: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    variant: Int,
    onClick: () -> Unit,
    modifier: Modifier,
    height: androidx.compose.ui.unit.Dp,
    enabled: Boolean = true,
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
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
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
    onOpenAiSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    var preview by remember { mutableStateOf<Int?>(null) }
    // The photo being cropped, by its id; the crop editor opens over the preview.
    var cropping by remember { mutableStateOf<Long?>(null) }

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

        AnimatedVisibility(visible = state.pictures.isNotEmpty(), enter = fadeIn(tween(200)), exit = fadeOut(tween(100))) {
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

        if (state.pictures.isNotEmpty()) {
            ExtractionPanel(state = state, viewModel = viewModel, onOpenAiSettings = onOpenAiSettings)
        }

        AppTextField(
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
            onCrop = { page -> cropping = state.pictures.getOrNull(page)?.id },
        )
    }

    cropping?.let { id ->
        val picture = state.pictures.firstOrNull { it.id == id }
        if (picture == null) {
            cropping = null
        } else {
            PhotoCropDialog(
                path = picture.path,
                newFile = viewModel::newPictureFile,
                onCancel = { cropping = null },
                onDone = { file ->
                    viewModel.replacePicture(id, file)
                    cropping = null
                },
            )
        }
    }
}

/** The "Extract text with AI" control under the photos, and everything it can turn into. */
@Composable
private fun ExtractionPanel(state: AddEntryState, viewModel: AddEntryViewModel, onOpenAiSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    when {
        state.extractStep == ExtractStep.RUNNING -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHighest).padding(16.dp),
        ) {
            LoadingDots("Extracting text", modifier = Modifier.weight(1f))
            PillButton(text = "Cancel", compact = true, tonal = true, onClick = viewModel::cancelExtraction)
        }

        state.extractStep == ExtractStep.REVIEW -> Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.surfaceContainerHighest).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Text found in your photos", style = MaterialTheme.typography.titleSmall)
            AppTextField(
                value = state.extractedText,
                onValueChange = viewModel::setExtractedText,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
            )
            Text(
                "Check and fix anything the AI misread. Your photos are kept either way.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(text = "Cancel", compact = true, tonal = true, onClick = viewModel::cancelExtraction, modifier = Modifier.weight(1f))
                PillButton(text = "Add to Text Notes", compact = true, onClick = viewModel::acceptExtraction, modifier = Modifier.weight(1.4f))
            }
        }

        state.extractSetupMissing -> Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.secondaryContainer).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("AI is off or not set up", style = MaterialTheme.typography.titleSmall, color = colors.onSecondaryContainer)
            Text(
                "Reading text from photos needs an AI provider, a model that accepts images and your API key. " +
                    "Add them in Settings, AI & Integrations, or continue without the text.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSecondaryContainer,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(text = "Open Settings", compact = true, onClick = onOpenAiSettings, modifier = Modifier.weight(1f))
                PillButton(text = "Continue", compact = true, tonal = true, onClick = viewModel::dismissExtractSetup, modifier = Modifier.weight(1f))
            }
        }

        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.acceptedText != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.primaryContainer).padding(start = 16.dp, end = 6.dp),
                ) {
                    Text(
                        "Extracted text will be saved with these photos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onPrimaryContainer,
                        modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                    )
                    androidx.compose.material3.TextButton(onClick = viewModel::discardAcceptedText) { Text("Remove") }
                }
            }
            PillButton(
                text = if (state.acceptedText != null) "Extract text again" else "Extract text with AI",
                tonal = true,
                onClick = viewModel::extractText,
                modifier = Modifier.fillMaxWidth(),
            )
            state.extractError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error) }
        }
    }
}

/** A full-screen, in-app photo viewer with a back button. Swipe sideways to move between photos. */
@Composable
fun PhotoPreviewDialog(
    paths: List<String>,
    startIndex: Int,
    onBack: () -> Unit,
    version: Int = 0,
    onCrop: ((page: Int) -> Unit)? = null,
) {
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
                    modifier = Modifier.weight(1f),
                )
                if (onCrop != null) {
                    PillButton(text = "Crop", compact = true, onClick = { onCrop(pager.currentPage) })
                }
            }
        }
    }
}

/** ". .. ..." that keeps counting up while something runs. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
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
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
    ) {
        // Material 3 Expressive loading indicator: a shape that morphs from one to the next while it works.
        androidx.compose.material3.LoadingIndicator(modifier = Modifier.size(32.dp))
        Text(text = "$label $dots", style = MaterialTheme.typography.bodyMedium, color = color)
    }
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
            Text("YouTube Video Notes", style = MaterialTheme.typography.titleLarge)
        }
        AppTextField(
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
        AnimatedVisibility(visible = video != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(100))) {
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

        if (video != null) {
            val canWatch = remember(video.id) { viewModel.canWatchLink() }
            AppTextField(
                value = state.pastedTranscript,
                onValueChange = viewModel::setPastedTranscript,
                label = { Text(if (canWatch) "Transcript (optional)" else "Paste the transcript") },
                enabled = !running,
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = if (canWatch) {
                    "Gemini can watch the video from its link, so a transcript is optional. If you paste one, the notes are written from it."
                } else {
                    "Your AI provider cannot watch a link. On YouTube open the video, tap the description, choose Show transcript, copy it and paste it here."
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        if (running) {
            LoadingDots(
                label = "Writing your notes",
            )
        }
        state.youtubeError?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error)
            if (state.video != null) {
                Text(
                    "You can try again, or save just your pasted transcript without AI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        AnimatedVisibility(visible = state.aiSetupMissing, enter = fadeIn(tween(200)), exit = fadeOut(tween(100))) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.secondaryContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("AI is off or not set up", style = MaterialTheme.typography.titleSmall, color = colors.onSecondaryContainer)
                Text(
                    "Add an AI provider, a model and your API key in Settings, AI & Integrations, then come back. " +
                        "Or skip the AI and keep just the transcript you pasted.",
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
                text = "Generate Notes",
                enabled = video != null && !running,
                loading = running,
                onClick = { viewModel.transcribe(skipAi = false, onSaved = onSaved) },
                modifier = Modifier.weight(2f),
            )
        }
        if (state.youtubeError != null && video != null && !running) {
            Text(
                text = "Save the pasted transcript only",
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
