package skywa1ker404.absencium.ui.vault

import androidx.compose.ui.input.nestedscroll.nestedScroll
import skywa1ker404.absencium.ui.main.BottomPill
import skywa1ker404.absencium.ui.main.MainTab
import skywa1ker404.absencium.ui.main.SlideAway
import skywa1ker404.absencium.ui.main.rememberPillScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.navigationBarsPadding
import skywa1ker404.absencium.ui.components.AppMenu
import skywa1ker404.absencium.ui.components.AppMenuItem
import skywa1ker404.absencium.ui.components.AppMenuDivider
import skywa1ker404.absencium.ui.main.ShellFab
import skywa1ker404.absencium.ui.theme.LocalBackdrop
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import skywa1ker404.absencium.ui.components.verticalScrollbar
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import skywa1ker404.absencium.ui.components.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import skywa1ker404.absencium.ui.icons.AppIcons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import com.ramcosta.composedestinations.generated.destinations.SyllabusScreenDestination
import skywa1ker404.absencium.data.db.GuideType
import skywa1ker404.absencium.data.syllabus.SyllabusTree
import skywa1ker404.absencium.ui.components.ExpressiveChip
import skywa1ker404.absencium.ui.syllabus.SyllabusPickerDialog
import skywa1ker404.absencium.ui.syllabus.syllabusLabel
import skywa1ker404.absencium.ui.vault.entry.ChoiceBar
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AiSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NewEntryScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NoteEditorScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import skywa1ker404.absencium.data.db.GuideEntity
import skywa1ker404.absencium.data.db.NoteEntity
import skywa1ker404.absencium.data.db.NoteStatus
import skywa1ker404.absencium.data.share.ShareFormat
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.vault.entry.AddEntryPopup
import skywa1ker404.absencium.ui.components.HintText
import skywa1ker404.absencium.ui.components.MessageCard
import skywa1ker404.absencium.ui.components.ScreenScaffold
import skywa1ker404.absencium.ui.components.SectionTitle
import skywa1ker404.absencium.ui.containerViewModel

/** One subject's folder: its notes, the Finished / Written Notes archive, and Guides and Question Banks. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Destination<RootGraph>
@Composable
fun SubjectFolderScreen(subjectId: Long, navigator: DestinationsNavigator) {
    val viewModel = containerViewModel { VaultViewModel(it) }
    val data by viewModel.data.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var finishedOpen by rememberSaveable { mutableStateOf(false) }
    var addEntry by remember { mutableStateOf(false) }
    var guideToDelete by remember { mutableStateOf<GuideEntity?>(null) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedGuideIds by remember { mutableStateOf(setOf<Long>()) }
    // Cards (the rich tinted tiles) or a compact list, for the notes and for the study material.
    var cardMode by rememberSaveable { mutableStateOf(true) }
    var showEdit by remember { mutableStateOf(false) }
    var showGuideTopic by remember { mutableStateOf(false) }
    var confirmGuideDelete by remember { mutableStateOf(false) }
    var confirmBatchDelete by remember { mutableStateOf(false) }
    var showMove by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var materialFilter by rememberSaveable { mutableIntStateOf(-1) }
    var topicFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var topicsOpen by remember { mutableStateOf(false) }
    var fileGuide by remember { mutableStateOf<GuideEntity?>(null) }
    var viewGuide by remember { mutableStateOf<GuideEntity?>(null) }
    val nodes by remember(subjectId) { viewModel.syllabusNodes(subjectId) }.collectAsStateWithLifecycle(emptyList())
    val colors = MaterialTheme.colorScheme
    val pillScroll = rememberPillScroll()
    androidx.compose.runtime.LaunchedEffect(tab) { pillScroll.compact.value = false }

    val loaded = data
    val subject = loaded?.subjects?.firstOrNull { it.id == subjectId }

    // A lazy list: a folder with hundreds of notes only builds the rows on screen instead of every card at once.
    ScreenScaffold(
        title = subject?.name ?: "Folder",
        onBack = { navigator.navigateUp() },
        scrollable = false,
        edgeToEdgeBottom = true,
        modifier = Modifier.nestedScroll(pillScroll.connection),
        titleContent = {
            // The subject's name is a button: it opens the topics of the syllabus, and picking one filters the notes under it.
            Box {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(enabled = tab == 0 && subject != null) { topicsOpen = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(subject?.name ?: "Folder", style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (tab == 0 && topicFilter != null) {
                            Text(
                                "Under " + syllabusLabel(nodes, topicFilter).substringAfterLast(" \u203a "),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (tab == 0 && subject != null) Icon(AppIcons.ExpandMore, contentDescription = "Filter notes by topic", tint = colors.onSurfaceVariant)
                }
            }
        },
        overlay = {
            // Like the main screen: the same pill (it shrinks to icons while the list scrolls down) and the button above it on the right.
            Box(Modifier.fillMaxSize().navigationBarsPadding()) {
                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp + 64.dp + 12.dp)) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !(selectedIds.isNotEmpty() || selectedGuideIds.isNotEmpty()) && subject != null,
                        enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut(),
                    ) {
                        ShellFab(
                            icon = if (tab == 2) AppIcons.Edit else AppIcons.Add,
                            label = when (tab) {
                                0 -> "New entry"
                                1 -> "Add material"
                                else -> if (nodes.isEmpty()) "Build syllabus" else "Edit syllabus"
                            },
                            onClick = {
                                when (tab) {
                                    0 -> addEntry = true
                                    1 -> navigator.navigate(NewEntryScreenDestination(subjectId = subjectId))
                                    else -> navigator.navigate(SyllabusScreenDestination(subjectId = subjectId))
                                }
                            },
                            backdrop = LocalBackdrop.current,
                            expanded = !pillScroll.compact.value,
                        )
                    }
                }
                BottomPill(
                    tabs = FolderTabs,
                    selectedIndex = tab,
                    compact = { pillScroll.compact.value },
                    backdrop = LocalBackdrop.current,
                    onSelect = { tab = it; selectedIds = emptySet(); selectedGuideIds = emptySet() },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                )
            }
            // The topics as a side panel over everything, the pill included.
            if (subject != null) {
                val noteCounts = remember(loaded, nodes, subjectId) {
                    val own = (loaded?.notesOf(subjectId, NoteStatus.ACTIVE).orEmpty() + loaded?.notesOf(subjectId, NoteStatus.FINISHED).orEmpty())
                    val direct = own.groupingBy { it.syllabusNodeId }.eachCount()
                    nodes.associate { n -> n.id to SyllabusTree.withDescendants(nodes, n.id).sumOf { direct[it] ?: 0 } } to own.size
                }
                skywa1ker404.absencium.ui.syllabus.TopicSidebar(
                    visible = topicsOpen,
                    onDismiss = { topicsOpen = false },
                    subjectName = subject.name,
                    nodes = nodes,
                    counts = noteCounts.first,
                    totalNotes = noteCounts.second,
                    selectedId = topicFilter,
                    onPick = { topicsOpen = false; topicFilter = it },
                    onOpenSyllabus = {
                        topicsOpen = false
                        navigator.navigate(SyllabusScreenDestination(subjectId = subjectId))
                    },
                )
            }
            if (addEntry) {
            AddEntryPopup(
                onDismiss = { addEntry = false },
                onSaved = { id ->
                    addEntry = false
                    navigator.navigate(NoteEditorScreenDestination(noteId = id, startInEdit = true))
                },
                onOpenAiSettings = {
                    addEntry = false
                    navigator.navigate(AiSettingsScreenDestination)
                },
                initialSubjectId = subjectId,
            )
        }
        },
    ) {
        if (loaded == null) {
            HintText("Loading...")
            return@ScreenScaffold
        }
        if (subject == null) {
            HintText("This subject no longer exists.")
            return@ScreenScaffold
        }
        val active = remember(loaded, subjectId) { loaded.notesOf(subjectId, NoteStatus.ACTIVE) }
        val finished = remember(loaded, subjectId) { loaded.notesOf(subjectId, NoteStatus.FINISHED) }
        val guides = remember(loaded, subjectId) { loaded.guides.filter { it.subjectId == subjectId } }
        val drafts = remember(loaded, subjectId) { loaded.notesOf(subjectId, NoteStatus.DRAFT) }
        // The notes under the chosen topic and everything inside it; null shows every note.
        val allowed = remember(nodes, topicFilter) { topicFilter?.let { SyllabusTree.withDescendants(nodes, it).toSet() } }
        val activeShown = if (allowed == null) active else active.filter { it.syllabusNodeId in allowed }
        val finishedShown = if (allowed == null) finished else finished.filter { it.syllabusNodeId in allowed }
        // Selected notes that still exist (one may have been deleted or moved elsewhere while selected).
        val everyNote = active + finished
        val selected = everyNote.filter { it.id in selectedIds }
        val selectedGuides = guides.filter { it.id in selectedGuideIds }
        val selecting = selectedIds.isNotEmpty() || selectedGuideIds.isNotEmpty()
        fun toggle(id: Long) {
            selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
        }
        fun toggleGuide(id: Long) {
            selectedGuideIds = if (id in selectedGuideIds) selectedGuideIds - id else selectedGuideIds + id
        }
        BackHandler(enabled = selecting) { selectedIds = emptySet(); selectedGuideIds = emptySet() }
        val listState = rememberLazyListState()

        Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().verticalScrollbar(listState, colors.onSurfaceVariant),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 172.dp),
        ) {
            if (message != null) {
                item(key = "message") {
                    AppCard(modifier = Modifier.fillMaxWidth(), highlighted = true, onClick = { viewModel.clearMessage() }) {
                        Text(message.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item(key = "crumbs") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Vault",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                        modifier = Modifier.clickable { navigator.navigateUp() },
                    )
                    Text("\u203a", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    Text(subject.name, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, maxLines = 1, modifier = Modifier.weight(1f))
                    if (tab != 2) {
                        // Cards or a compact list, for the notes and the study material alike.
                        IconButton(onClick = { cardMode = !cardMode }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                if (cardMode) AppIcons.ViewList else AppIcons.ViewAgenda,
                                contentDescription = if (cardMode) "Show as a list" else "Show as cards",
                                tint = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (tab == 0) {
            if (topicFilter != null) {
                item(key = "topic-filter") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExpressiveChip(
                            selected = true,
                            onClick = { topicFilter = null },
                            label = { Text("Under " + syllabusLabel(nodes, topicFilter).substringAfterLast(" \u203a ") + "   \u2715") },
                        )
                    }
                }
            }
            if (drafts.isNotEmpty()) {
                item(key = "drafts-title") { SectionTitle("Drafts") }
                items(drafts, key = { "draft-${it.id}" }) { draft ->
                    NoteCard(
                        draft,
                        photoCount = loaded.attachmentsOf(draft.id).size,
                        onClick = { navigator.navigate(NewEntryScreenDestination(subjectId = subjectId, draftId = draft.id)) },
                    )
                }
            }
            item(key = "notes-title") { SectionTitle("Notes") }
            if (activeShown.isEmpty()) {
                item(key = "notes-empty") {
                    if (topicFilter == null && finished.isEmpty() && drafts.isEmpty()) {
                        skywa1ker404.absencium.ui.components.ExpressiveEmptyState(
                            icon = AppIcons.Description,
                            title = "This folder is empty",
                            text = "Use New entry below to add the first note. Press and hold a note later to select several.",
                        )
                    } else HintText(
                        if (topicFilter != null) "No notes under this topic. Clear the topic to see every note."
                        else if (finished.isEmpty() && drafts.isEmpty()) "This folder is empty. Use New entry below, or press and hold a note later to select several."
                        else "No notes here right now. Notes you mark as finished move to the archive below.",
                    )
                }
            }
            items(activeShown, key = { "note-${it.id}" }) { note ->
                if (cardMode) {
                    if (cardMode) {
                        NoteCard(
                            note,
                            photoCount = loaded.attachmentsOf(note.id).size,
                            selected = note.id in selectedIds,
                            topic = note.syllabusNodeId?.let { syllabusLabel(nodes, it).substringAfterLast(" › ") },
                            onLongClick = { toggle(note.id) },
                            onClick = { if (selecting) toggle(note.id) else navigator.navigate(NoteEditorScreenDestination(noteId = note.id)) },
                        )
                    } else {
                        NoteRow(
                            note,
                            selected = note.id in selectedIds,
                            topic = note.syllabusNodeId?.let { syllabusLabel(nodes, it).substringAfterLast(" › ") },
                            onLongClick = { toggle(note.id) },
                            onClick = { if (selecting) toggle(note.id) else navigator.navigate(NoteEditorScreenDestination(noteId = note.id)) },
                        )
                    }
                } else {
                    NoteRow(
                        note,
                        selected = note.id in selectedIds,
                        topic = note.syllabusNodeId?.let { syllabusLabel(nodes, it).substringAfterLast(" › ") },
                        onLongClick = { toggle(note.id) },
                        onClick = { if (selecting) toggle(note.id) else navigator.navigate(NoteEditorScreenDestination(noteId = note.id)) },
                    )
                }
            }

            item(key = "finished-title") { SectionTitle("Finished / Written Notes") }
            item(key = "finished-toggle") {
                AppCard(modifier = Modifier.fillMaxWidth(), onClick = { finishedOpen = !finishedOpen }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${finishedShown.size} ${if (finishedShown.size == 1) "note" else "notes"} archived",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = if (finishedOpen) "Hide" else "Show",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary,
                        )
                    }
                }
            }
            if (finishedOpen) {
                items(finishedShown, key = { "done-${it.id}" }) { note ->
                    NoteCard(
                        note,
                        photoCount = loaded.attachmentsOf(note.id).size,
                        selected = note.id in selectedIds,
                        topic = note.syllabusNodeId?.let { syllabusLabel(nodes, it).substringAfterLast(" › ") },
                        onLongClick = { toggle(note.id) },
                        onClick = { if (selecting) toggle(note.id) else navigator.navigate(NoteEditorScreenDestination(noteId = note.id)) },
                    )
                }
            }

            }

            if (tab == 1) {
                if (guides.isEmpty()) {
                    item(key = "guides-empty") {
                        skywa1ker404.absencium.ui.components.ExpressiveEmptyState(
                            icon = AppIcons.Book,
                            title = "No study material yet",
                            text = "Add previous year papers, answer books, guides or the official syllabus as PDFs, then file each under the topic it covers. Use the Guide / Answer PDF format in a new entry.",
                        )
                    }
                } else {
                    item(key = "material-filters") {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ExpressiveChip(
                                selected = materialFilter == -1,
                                onClick = { materialFilter = -1 },
                                label = { Text("All  ${guides.size}") },
                            )
                            GuideType.entries.forEach { type ->
                                val count = guides.count { it.guideType == type }
                                if (count > 0) {
                                    ExpressiveChip(
                                        selected = materialFilter == type.ordinal,
                                        onClick = { materialFilter = type.ordinal },
                                        label = { Text("${type.label()}  $count") },
                                    )
                                }
                            }
                        }
                    }
                    GuideType.entries.forEach { type ->
                        val ofType = guides.filter { it.guideType == type }
                        if (ofType.isNotEmpty() && (materialFilter == -1 || materialFilter == type.ordinal)) {
                            if (materialFilter == -1) item(key = "type-${type.name}") { SectionTitle(type.sectionTitle()) }
                            items(ofType, key = { "guide-${it.id}" }) { guide ->
                                val guideTopic = guide.syllabusNodeId?.let { syllabusLabel(nodes, it).substringAfterLast(" › ") }
                                val openIt = {
                                    if (selecting) toggleGuide(guide.id)
                                    else viewGuide = guide
                                    Unit
                                }
                                if (cardMode) {
                                    GuideCard(
                                        guide = guide,
                                        topic = guideTopic,
                                        onFile = { fileGuide = guide },
                                        onClick = openIt,
                                        onDelete = { guideToDelete = guide },
                                        selected = guide.id in selectedGuideIds,
                                        onLongClick = { toggleGuide(guide.id) },
                                    )
                                } else {
                                    GuideRow(
                                        guide = guide,
                                        topic = guideTopic,
                                        onClick = openIt,
                                        selected = guide.id in selectedGuideIds,
                                        onLongClick = { toggleGuide(guide.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (tab == 2) {
                if (nodes.isEmpty()) {
                    item(key = "syllabus-empty") {
                        skywa1ker404.absencium.ui.components.ExpressiveEmptyState(
                            icon = AppIcons.School,
                            title = "No syllabus yet",
                            text = "Add the units, chapters and topics of this subject, or import an outline. Notes and study material can then be filed under them.",
                        )
                    }
                } else {
                    item(key = "syllabus-card") {
                        val overall = SyllabusTree.progress(nodes, null)
                        val done = nodes.count { it.status == skywa1ker404.absencium.data.db.SyllabusStatus.COMPLETED }
                        val hue = skywa1ker404.absencium.ui.components.hueFor(0)
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            container = hue.container.copy(alpha = 0.75f),
                            onClick = { navigator.navigate(SyllabusScreenDestination(subjectId = subjectId)) },
                        ) {
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("${(overall * 100).toInt()}%", style = MaterialTheme.typography.displaySmall, color = hue.onContainer)
                                Text(
                                    "of the syllabus studied",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = hue.onContainer.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(bottom = 6.dp),
                                )
                            }
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { overall },
                                modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                                color = hue.accent,
                                trackColor = hue.accent.copy(alpha = 0.18f),
                                drawStopIndicator = {},
                            )
                            Text(
                                "${nodes.size} items, $done done. Tap to open the full tree.",
                                style = MaterialTheme.typography.bodySmall,
                                color = hue.onContainer.copy(alpha = 0.8f),
                            )
                        }
                    }
                    val units = nodes.filter { it.parentId == null }.sortedWith(compareBy({ it.position }, { it.id }))
                    items(units.size, key = { "unit-${units[it].id}" }) { index ->
                        val unit = units[index]
                        val hue = skywa1ker404.absencium.ui.components.hueFor((index + 1).toLong())
                        val progress = SyllabusTree.progress(nodes, unit.id)
                        val chapters = nodes.count { it.parentId == unit.id }
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            container = hue.container.copy(alpha = 0.75f),
                            onClick = { navigator.navigate(SyllabusScreenDestination(subjectId = subjectId)) },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(colors.surface.copy(alpha = 0.9f)),
                                    contentAlignment = Alignment.Center,
                                ) { Text("${index + 1}", style = MaterialTheme.typography.titleMedium, color = hue.accent) }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(unit.title, style = MaterialTheme.typography.titleMedium, color = hue.onContainer, maxLines = 2)
                                    Text(
                                        if (chapters == 0) "No chapters yet" else if (chapters == 1) "1 chapter" else "$chapters chapters",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = hue.onContainer.copy(alpha = 0.75f),
                                    )
                                }
                                Text(
                                    "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.onSurface,
                                    modifier = Modifier.clip(CircleShape).background(colors.surface.copy(alpha = 0.9f)).padding(horizontal = 12.dp, vertical = 7.dp),
                                )
                            }
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = hue.accent,
                                trackColor = hue.accent.copy(alpha = 0.18f),
                                drawStopIndicator = {},
                            )
                        }
                    }
                }
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = selecting,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = androidx.compose.animation.scaleIn(
                initialScale = 0.6f,
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
            ) + androidx.compose.animation.slideInVertically(
                initialOffsetY = { -it },
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
            ) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.scaleOut(targetScale = 0.7f) + androidx.compose.animation.slideOutVertically { -it } + androidx.compose.animation.fadeOut(),
        ) {
            val allFinished = selected.isNotEmpty() && selected.all { it.status == NoteStatus.FINISHED }
            // Material 3 Expressive floating toolbar: the count on the left, the actions in the middle, Cancel on the right.
            HorizontalFloatingToolbar(
                expanded = true,
                modifier = Modifier.padding(top = 4.dp),
                leadingContent = {
                    // The count sits on a round primary badge so it reads as the state of the whole toolbar.
                    Box(
                        modifier = Modifier.padding(start = 4.dp).size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${if (tab == 1) selectedGuides.size else selected.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
                trailingContent = {
                    IconButton(onClick = { selectedIds = emptySet(); selectedGuideIds = emptySet() }) { Icon(AppIcons.Close, contentDescription = "Cancel selection") }
                },
                content = {
                    if (tab == 1) {
                        // Study material: file it under a topic, or remove it.
                        SelectionAction(AppIcons.Folder, "Topic") { showGuideTopic = true }
                        SelectionAction(AppIcons.Delete, "Delete", destructive = true) { confirmGuideDelete = true }
                    } else {
                        // Edit (the title and the date) is for one note at a time.
                        if (selected.size == 1) SelectionAction(AppIcons.Edit, "Edit") { showEdit = true }
                        SelectionAction(AppIcons.Check, if (allFinished) "Reopen" else "Finish") {
                            viewModel.setNotesStatus(selected, if (allFinished) NoteStatus.ACTIVE else NoteStatus.FINISHED)
                            selectedIds = emptySet()
                        }
                        SelectionAction(AppIcons.Share, "Share") { showShare = true }
                        SelectionAction(AppIcons.Folder, "Move") { showMove = true }
                        SelectionAction(AppIcons.Delete, "Delete", destructive = true) { confirmBatchDelete = true }
                    }
                },
            )
        }
        }
    }

    viewGuide?.let { GuideViewer(it, onClose = { viewGuide = null }) }
    fileGuide?.let { guide ->
        SyllabusPickerDialog(
            nodes = nodes,
            current = guide.syllabusNodeId,
            onPick = { viewModel.setGuideSyllabus(guide.id, it); fileGuide = null },
            onDismiss = { fileGuide = null },
            onOpenSyllabus = { fileGuide = null; navigator.navigate(SyllabusScreenDestination(subjectId = subjectId)) },
        )
    }
    ShareHost(viewModel)
    if (showShare) {
        val toShare = remember(selectedIds, data) { (data?.notes.orEmpty()).filter { it.id in selectedIds } }
        ShareFormatDialog(
            // One entry can be a PDF, Markdown or text file; several always travel as a ZIP.
            formats = if (toShare.size == 1) listOf(ShareFormat.PDF, ShareFormat.MARKDOWN, ShareFormat.TEXT, ShareFormat.ZIP)
            else listOf(ShareFormat.MARKDOWN, ShareFormat.ZIP),
            onPick = { format ->
                showShare = false
                viewModel.share(toShare, format)
                selectedIds = emptySet()
            },
            onDismiss = { showShare = false },
        )
    }
    if (confirmBatchDelete) {
        val victims = remember(selectedIds, data) { (data?.notes.orEmpty()).filter { it.id in selectedIds } }
        AlertDialog(
            onDismissRequest = { confirmBatchDelete = false },
            title = { Text("Delete ${victims.size} ${if (victims.size == 1) "entry" else "entries"}?") },
            text = { Text("They move to the Trash and can be restored from Settings, Trash, for 30 days. Drafts are deleted for good.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNotes(victims)
                    selectedIds = emptySet()
                    confirmBatchDelete = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmBatchDelete = false }) { Text("Keep") } },
        )
    }
    if (showMove) {
        val targets = (data?.subjects.orEmpty()).filter { it.id != subjectId && it.isActive }
        val moving = remember(selectedIds, data) { (data?.notes.orEmpty()).filter { it.id in selectedIds } }
        AlertDialog(
            onDismissRequest = { showMove = false },
            title = { Text("Move to which subject?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (targets.isEmpty()) HintText("There is no other active subject to move to.")
                    targets.forEach { target ->
                        Text(
                            text = target.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.moveNotes(moving, target.id)
                                    selectedIds = emptySet()
                                    showMove = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showMove = false }) { Text("Cancel") } },
        )
    }

    if (showEdit) {
        val note = (data?.notes.orEmpty()).firstOrNull { it.id in selectedIds }
        if (note == null) {
            showEdit = false
        } else {
            EditNoteDialog(
                note = note,
                onSave = { title, millis ->
                    viewModel.editNote(note.id, title, millis)
                    selectedIds = emptySet()
                    showEdit = false
                },
                onDismiss = { showEdit = false },
            )
        }
    }
    if (showGuideTopic) {
        SyllabusPickerDialog(
            nodes = nodes,
            current = null,
            onPick = { picked ->
                viewModel.setGuidesSyllabus(selectedGuideIds, picked)
                selectedGuideIds = emptySet()
                showGuideTopic = false
            },
            onDismiss = { showGuideTopic = false },
            onOpenSyllabus = { showGuideTopic = false; navigator.navigate(SyllabusScreenDestination(subjectId = subjectId)) },
        )
    }
    if (confirmGuideDelete) {
        val victims = (data?.guides.orEmpty()).filter { it.id in selectedGuideIds }
        AlertDialog(
            onDismissRequest = { confirmGuideDelete = false },
            title = { Text("Remove ${victims.size} ${if (victims.size == 1) "PDF" else "PDFs"}?") },
            text = { Text("They are removed from the vault and from the notes folder.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGuides(victims)
                    selectedGuideIds = emptySet()
                    confirmGuideDelete = false
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmGuideDelete = false }) { Text("Keep") } },
        )
    }

    val pending = guideToDelete
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { guideToDelete = null },
            title = { Text("Remove this PDF?") },
            text = { Text("${pending.fileName} will be removed from the vault and from the notes folder.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGuide(pending)
                    guideToDelete = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { guideToDelete = null }) { Text("Keep") } },
        )
    }

}

/** One action of the selection toolbar: an icon with its name under it. */
@Composable
private fun SelectionAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, destructive: Boolean = false, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = if (destructive) colors.error else colors.onSurface
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

/** The three tabs of a subject folder, in the same pill as the main screen's. */
private val FolderTabs = listOf(
    MainTab("Notes", AppIcons.Description),
    MainTab("Study material", AppIcons.Book),
    MainTab("Syllabus", AppIcons.School),
)


/** Renames one note and changes the date it is filed under. The time of day it was made at is kept. */
@Composable
private fun EditNoteDialog(note: NoteEntity, onSave: (title: String, createdMillis: Long) -> Unit, onDismiss: () -> Unit) {
    val zone = remember { java.time.ZoneId.systemDefault() }
    var title by remember(note.id) { mutableStateOf(note.title) }
    var date by remember(note.id) { mutableStateOf(java.time.Instant.ofEpochMilli(note.createdDate).atZone(zone).toLocalDate()) }
    var picking by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                skywa1ker404.absencium.ui.components.AppTextField(
                    value = title,
                    onValueChange = { title = it.replace("\n", " ") },
                    label = { Text("Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy")), style = MaterialTheme.typography.titleMedium)
                    }
                    skywa1ker404.absencium.ui.components.PillButton(text = "Change date", tonal = true, compact = true, onClick = { picking = true })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val time = java.time.Instant.ofEpochMilli(note.createdDate).atZone(zone).toLocalTime()
                onSave(title, date.atTime(time).atZone(zone).toInstant().toEpochMilli())
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    skywa1ker404.absencium.ui.components.AnimatedPopup(if (picking) true else null) {
        skywa1ker404.absencium.ui.components.DatePickerPopup(
            initial = date,
            title = "Pick a date",
            onPicked = { date = it },
            onDismiss = { picking = false },
        )
    }
}
