package com.trajectory.vault.ui.main

import com.trajectory.vault.ui.theme.LocalQuality
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.trajectory.vault.ui.components.GlassStage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.DraftsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.HolidayImportScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NewEntryScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NoteEditorScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsHubScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SubjectFolderScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SubjectsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.TimetableScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.trajectory.vault.data.db.ExamEntity
import com.trajectory.vault.data.db.HolidayEntity
import com.trajectory.vault.ui.calendar.AddEntrySheet
import com.trajectory.vault.ui.calendar.CalendarTab
import com.trajectory.vault.ui.calendar.CalendarViewModel
import com.trajectory.vault.ui.calendar.ExamDetailSheet
import com.trajectory.vault.ui.components.AppCard
import com.trajectory.vault.ui.components.floatingGlass
import com.trajectory.vault.ui.containerViewModel
import com.trajectory.vault.ui.home.HomeTab
import com.trajectory.vault.ui.icons.AppIcons
import com.trajectory.vault.ui.profile.ProfileSheet
import com.trajectory.vault.ui.search.SearchSheet
import com.trajectory.vault.ui.vault.VaultTab
import com.trajectory.vault.ui.vault.VaultViewModel
import com.trajectory.vault.ui.vault.openGuide
import com.trajectory.vault.data.db.NoteStatus
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.trajectory.vault.ui.settings.rememberSettingsViewModel
import com.trajectory.vault.ui.theme.LocalGlassEnabled
import com.trajectory.vault.ui.theme.LocalBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private val Tabs = listOf(
    MainTab("Home", AppIcons.Home),
    MainTab("Attendance", AppIcons.CalendarMonth),
    MainTab("Vault", AppIcons.Folder),
)

/** Scroll distance before the pill collapses, and before it expands again. */
private const val CompactTravelPx = 72f
private const val ExpandTravelPx = 36f

private const val TAB_HOME = 0
private const val TAB_ATTENDS = 1
private const val TAB_VAULT = 2

/** The scrolling body of one tab, with room for the floating top bar and bottom controls. */
@Composable
fun TabColumn(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/**
 * The app shell: a frosted top bar with the profile avatar, the tab content scrolling under it, and
 * a floating navigation pill. Scrolling down shrinks the pill to icons and slides the action
 * button off the right edge; scrolling up brings both back.
 */
@Destination<RootGraph>(start = true)
@Composable
fun MainScreen(navigator: DestinationsNavigator) {
    val q = LocalQuality.current
    val calendarViewModel = containerViewModel { CalendarViewModel(it) }
    val settingsViewModel = rememberSettingsViewModel()
    val data by calendarViewModel.data.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val message by calendarViewModel.message.collectAsStateWithLifecycle()
    val vaultViewModel = containerViewModel { VaultViewModel(it) }
    val vaultData by vaultViewModel.data.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var tab by rememberSaveable { mutableIntStateOf(TAB_HOME) }
    // Held as a State object and read only inside the pill and the floating buttons, so scrolling
    // never recomposes the whole screen.
    val compactState = remember { mutableStateOf(false) }
    var showAddEntry by rememberSaveable { mutableStateOf(false) }
    var fabOpen by remember { mutableStateOf(false) }
    var showProfile by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var openExam by remember { mutableStateOf<ExamEntity?>(null) }
    var editExam by remember { mutableStateOf<ExamEntity?>(null) }
    var editHoliday by remember { mutableStateOf<HolidayEntity?>(null) }

    BackHandler(enabled = fabOpen) { fabOpen = false }
    BackHandler(enabled = tab != TAB_HOME && !fabOpen) { tab = TAB_HOME }
    LaunchedEffect(tab) {
        compactState.value = false
        fabOpen = false
    }

    // Truncated to the minute, so the screen is only recomposed when the visible time can change.
    val now by produceState(initialValue = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)) {
        while (true) {
            delay(30_000)
            value = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        }
    }

    val scrollConnection = remember {
        object : NestedScrollConnection {
            // Scroll distance in one direction; it restarts when the direction changes, so small
            // wobbles while scrolling never flip the pill back and forth.
            private var travelled = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy == 0f) return Offset.Zero
                if (travelled * dy < 0f) travelled = 0f
                travelled += dy
                // Dragging up (negative y) scrolls the content down.
                if (travelled < -CompactTravelPx && !compactState.value) {
                    compactState.value = true
                } else if (travelled > ExpandTravelPx && compactState.value) {
                    compactState.value = false
                }
                return Offset.Zero
            }
        }
    }

    val glass = LocalGlassEnabled.current
    val backdrop = if (glass) rememberLayerBackdrop() else null
    val colors = MaterialTheme.colorScheme
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPadding = PaddingValues(top = topInset + 72.dp, bottom = bottomInset + 168.dp)

    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .nestedScroll(scrollConnection),
        ) {
            // The blur source: everything that scrolls behind the floating bars.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                    // Inside the recorded layer, so the glass bars blur a solid page colour, not empty space.
                    .background(colors.background),
            ) {
                GlassStage(modifier = Modifier.fillMaxSize()) {
                // Every tab stays composed once it has been opened, so switching is just a fade, not a rebuild
                // of the whole tab (the Calendar month grid is the heavy one). Calendar and Vault are prepared
                // a few seconds after start while Home is idle.
                var visited by remember { mutableStateOf(setOf(tab)) }
                LaunchedEffect(tab) { if (tab !in visited) visited = visited + tab }
                LaunchedEffect(Unit) {
                    delay(3_000)
                    visited = visited + TAB_ATTENDS
                    delay(1_500)
                    visited = visited + TAB_VAULT
                }
                listOf(TAB_HOME, TAB_ATTENDS, TAB_VAULT).forEach { current ->
                    if (current in visited) key(current) {
                        TabSlot(visible = current == tab, fade = q.decorativeMotion) {
                    val loaded = data
                    when {
                        loaded == null -> TabColumn(contentPadding) {}
                        current == TAB_HOME -> HomeTab(
                            data = loaded,
                            settings = settings,
                            now = now,
                            contentPadding = contentPadding,
                            onOpenExam = { openExam = it },
                            onManageSubjects = { navigator.navigate(SubjectsScreenDestination) },
                        )

                        current == TAB_ATTENDS -> CalendarTab(
                            data = loaded,
                            now = now,
                            contentPadding = contentPadding,
                            viewModel = calendarViewModel,
                            onOpenExam = { openExam = it },
                            onEditHoliday = { editHoliday = it },
                            onOpenTimetable = { navigator.navigate(TimetableScreenDestination) },
                            threshold = settings.attendanceThresholdPercent,
                            onAddNotes = { subjectId, date ->
                                navigator.navigate(
                                    NewEntryScreenDestination(
                                        subjectId = subjectId,
                                        draftId = 0L,
                                        collectDate = date.toString(),
                                    ),
                                )
                            },
                        )

                        else -> {
                            val vault = vaultData
                            if (vault == null) {
                                TabColumn(contentPadding) {}
                            } else {
                                VaultTab(
                                    data = vault,
                                    contentPadding = contentPadding,
                                    onOpenFolder = { navigator.navigate(SubjectFolderScreenDestination(subjectId = it)) },
                                    onManageSubjects = { navigator.navigate(SubjectsScreenDestination) },
                                    onOpenDrafts = { navigator.navigate(DraftsScreenDestination) },
                                )
                            }
                        }
                    }
                        }
                    }
                }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    // No bar: only a soft fade of the background colour so scrolling content never collides with the title.
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            0f to colors.background.copy(alpha = 0.88f),
                            0.7f to colors.background.copy(alpha = 0.5f),
                            1f to androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    ),
            ) {
                ShellTopBar(displayName = settings.displayName, onProfile = { showProfile = true })
            }

            if (message != null) {
                AppCard(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = topInset + 76.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    highlighted = true,
                    onClick = { calendarViewModel.clearMessage() },
                ) {
                    Text(message.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Tap to dismiss",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            if (tab == TAB_ATTENDS || tab == TAB_VAULT) {
                // Tapping anywhere outside the menu closes it.
                if (fabOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.scrim.copy(alpha = if (glass) 0.18f else 0.32f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { fabOpen = false },
                    )
                }
                val actions = if (tab == TAB_ATTENDS) {
                    listOf(
                        FabAction("Add entry", AppIcons.Add) { fabOpen = false; showAddEntry = true },
                        FabAction("Timetable", AppIcons.Schedule) { fabOpen = false; navigator.navigate(TimetableScreenDestination) },
                        FabAction("Import holidays", AppIcons.AutoAwesome) { fabOpen = false; navigator.navigate(HolidayImportScreenDestination) },
                    )
                } else {
                    listOf(
                        FabAction("New entry", AppIcons.Add) {
                            fabOpen = false
                            navigator.navigate(NewEntryScreenDestination(subjectId = 0L, draftId = 0L))
                        },
                        FabAction("Drafts", AppIcons.Draft) { fabOpen = false; navigator.navigate(DraftsScreenDestination) },
                    )
                }
                SlideAway(
                    hidden = { compactState.value && !fabOpen },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = bottomInset + 16.dp + 64.dp + 12.dp + 52.dp + 12.dp),
                ) {
                    ShellFabMenu(
                        expanded = fabOpen,
                        onToggle = { fabOpen = !fabOpen },
                        actions = actions,
                        backdrop = backdrop,
                    )
                }
            }

            SlideAway(
                hidden = { compactState.value },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = bottomInset + 16.dp + 64.dp + 12.dp),
            ) {
                ShellSearchButton(backdrop = backdrop, onClick = { showSearch = true })
            }

            BottomPill(
                tabs = Tabs,
                selectedIndex = tab,
                compact = { compactState.value },
                backdrop = backdrop,
                onSelect = { tab = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomInset + 16.dp),
            )
        }
    }

    val loaded = data
    if (showAddEntry && loaded != null) {
        AddEntrySheet(
            subjects = loaded.subjects.filter { it.isActive },
            initialDate = now.toLocalDate(),
            onDismiss = { showAddEntry = false },
            onSaveExam = { subjectId, date, title, weightage, room, notes, priority ->
                calendarViewModel.addExam(subjectId, date, title, weightage, room, notes, priority)
                showAddEntry = false
            },
            onSaveHoliday = { start, end, title, autoSkip, priority ->
                calendarViewModel.addHolidayRange(start, end, title, autoSkip, priority)
                showAddEntry = false
            },
            onImportWithAi = {
                showAddEntry = false
                navigator.navigate(HolidayImportScreenDestination)
            },
        )
    }

    val examBeingEdited = editExam
    val holidayBeingEdited = editHoliday
    if ((examBeingEdited != null || holidayBeingEdited != null) && loaded != null) {
        AddEntrySheet(
            // The exam's own subject stays selectable even if it has since been made inactive.
            subjects = loaded.subjects.filter { it.isActive || it.id == examBeingEdited?.subjectId },
            initialDate = now.toLocalDate(),
            onDismiss = {
                editExam = null
                editHoliday = null
            },
            onSaveExam = { _, _, _, _, _, _, _ -> },
            onSaveHoliday = { _, _, _, _, _ -> },
            editExam = examBeingEdited,
            editHoliday = holidayBeingEdited,
            onUpdateExam = {
                calendarViewModel.updateExam(it)
                editExam = null
            },
            onUpdateHoliday = {
                calendarViewModel.updateHoliday(it)
                editHoliday = null
            },
        )
    }

    val exam = openExam
    if (exam != null && loaded != null) {
        ExamDetailSheet(
            exam = exam,
            subjectName = loaded.subjects.firstOrNull { it.id == exam.subjectId }?.name ?: "Unknown subject",
            today = now.toLocalDate(),
            onDismiss = { openExam = null },
            onEdit = {
                editExam = exam
                openExam = null
            },
            onDelete = {
                calendarViewModel.deleteExam(exam)
                openExam = null
            },
        )
    }

    if (showSearch) {
        SearchSheet(
            calendar = loaded,
            onDismiss = { showSearch = false },
            onOpenNote = { note ->
                if (note.status == NoteStatus.DRAFT) {
                    navigator.navigate(NewEntryScreenDestination(subjectId = note.subjectId, draftId = note.id))
                } else {
                    navigator.navigate(NoteEditorScreenDestination(noteId = note.id))
                }
            },
            onOpenGuide = { guide ->
                openGuide(context, guide)?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
            },
            onOpenExam = { openExam = it },
        )
    }

    if (showProfile) {
        ProfileSheet(
            data = loaded,
            settings = settings,
            onDismiss = { showProfile = false },
            onSaveName = { settingsViewModel.setDisplayName(it) },
            onOpenSettings = {
                showProfile = false
                navigator.navigate(SettingsHubScreenDestination)
            },
        )
    }
}


/**
 * One tab of the shell. A tab that is not showing keeps its state and its composition but is not drawn,
 * not touched and takes no space, so the visible tab is the only one doing work each frame.
 */
@Composable
private fun TabSlot(visible: Boolean, fade: Boolean, content: @Composable () -> Unit) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (fade) tween(durationMillis = 180) else snap(),
        label = "tabAlpha",
    )
    Box(
        modifier = Modifier
            .graphicsLayer { this.alpha = alpha }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                if (!visible && alpha == 0f) {
                    layout(0, 0) {}
                } else {
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
            },
    ) { content() }
}
