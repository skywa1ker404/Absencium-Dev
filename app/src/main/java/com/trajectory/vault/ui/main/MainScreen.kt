package com.trajectory.vault.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import com.ramcosta.composedestinations.generated.destinations.SettingsHubScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SubjectsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.TimetableScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.trajectory.vault.data.db.ExamEntity
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
import com.trajectory.vault.ui.settings.rememberSettingsViewModel
import com.trajectory.vault.ui.theme.LocalGlassEnabled
import com.trajectory.vault.ui.theme.LocalHazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.delay
import java.time.LocalDateTime

private val Tabs = listOf(
    MainTab("Home", AppIcons.Home),
    MainTab("Attends", AppIcons.CalendarMonth),
    MainTab("Vault", AppIcons.Folder),
)

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
    val calendarViewModel = containerViewModel { CalendarViewModel(it) }
    val settingsViewModel = rememberSettingsViewModel()
    val data by calendarViewModel.data.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val message by calendarViewModel.message.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(TAB_HOME) }
    var compact by remember { mutableStateOf(false) }
    var showAddEntry by rememberSaveable { mutableStateOf(false) }
    var showProfile by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var openExam by remember { mutableStateOf<ExamEntity?>(null) }

    BackHandler(enabled = tab != TAB_HOME) { tab = TAB_HOME }
    LaunchedEffect(tab) { compact = false }

    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            delay(30_000)
            value = LocalDateTime.now()
        }
    }

    val scrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Dragging up (negative y) scrolls the content down.
                if (available.y < -8f) compact = true else if (available.y > 8f) compact = false
                return Offset.Zero
            }
        }
    }

    val glass = LocalGlassEnabled.current
    val hazeState = if (glass) rememberHazeState() else null
    val colors = MaterialTheme.colorScheme
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPadding = PaddingValues(top = topInset + 72.dp, bottom = bottomInset + 168.dp)

    CompositionLocalProvider(LocalHazeState provides hazeState) {
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
                    .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier),
            ) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        (fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                            slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it / 24 })
                            .togetherWith(fadeOut(spring(stiffness = Spring.StiffnessMedium)))
                    },
                    label = "tab",
                ) { current ->
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
                            onOpenTimetable = { navigator.navigate(TimetableScreenDestination) },
                        )

                        else -> TabColumn(contentPadding) {
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Vault", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Notes and guides are built in the next update.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .floatingGlass(hazeState, RectangleShape, tint = colors.background, bordered = false),
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

            if (tab == TAB_ATTENDS) {
                ShellFab(
                    icon = AppIcons.Add,
                    label = "Add Entry",
                    onClick = { showAddEntry = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = bottomInset + 16.dp + 64.dp + 12.dp + 52.dp + 12.dp)
                        .slideAwayWhen(compact),
                )
            }

            ShellSearchButton(
                hazeState = hazeState,
                onClick = { showSearch = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = bottomInset + 16.dp + 64.dp + 12.dp)
                    .slideAwayWhen(compact),
            )

            BottomPill(
                tabs = Tabs,
                selectedIndex = tab,
                compact = compact,
                hazeState = hazeState,
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
            onSaveHoliday = { date, title, autoSkip, priority ->
                calendarViewModel.addHoliday(date, title, autoSkip, priority)
                showAddEntry = false
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
            onOpenVault = { tab = TAB_VAULT },
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
