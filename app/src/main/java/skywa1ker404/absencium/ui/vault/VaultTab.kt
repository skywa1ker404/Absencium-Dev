package skywa1ker404.absencium.ui.vault

import skywa1ker404.absencium.ui.theme.LocalQuality
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.data.db.NoteStatus
import skywa1ker404.absencium.data.vault.VaultData
import skywa1ker404.absencium.ui.calendar.subjectColor
import skywa1ker404.absencium.ui.components.AppCard
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.icons.AppIcons

/** How far (in dp, after resistance) the list must be pulled down before Drafts opens on release. */
private val PullThreshold = 96.dp
private val PullMax = 160.dp

/**
 * The Vault tab: one folder per subject in a two-column grid.
 *
 * Pulling down while the grid is already at its very top reveals a draft icon; releasing past the
 * threshold opens the Drafts page, releasing earlier snaps back. The pull only reacts to leftover
 * downward drag at the top, so ordinary scrolling is never affected.
 */
@Composable
fun VaultTab(
    data: VaultData,
    contentPadding: PaddingValues,
    onOpenFolder: (Long) -> Unit,
    onManageSubjects: () -> Unit,
    onOpenDrafts: () -> Unit,
    onOpenTrash: () -> Unit,
) {
    val q = LocalQuality.current
    val colors = MaterialTheme.colorScheme
    val subjects = data.subjects.filter { it.isActive }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { PullThreshold.toPx() }
    val maxPx = with(density) { PullMax.toPx() }
    var pull by remember { mutableFloatStateOf(0f) }
    val currentOpenDrafts by rememberUpdatedState(onOpenDrafts)

    val pullConnection = remember(thresholdPx, maxPx) {
        object : NestedScrollConnection {
            private fun atTop() = gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0

            // Dragging back up first undoes the pull before the list scrolls.
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && pull > 0f && available.y < 0f) {
                    val used = maxOf(available.y, -pull)
                    pull += used
                    return Offset(0f, used)
                }
                return Offset.Zero
            }

            // Downward drag the list could not use is only left over when it is at the top.
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f && atTop()) {
                    pull = (pull + available.y * 0.5f).coerceAtMost(maxPx)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pull <= 0f) return Velocity.Zero
                val open = pull >= thresholdPx
                scope.launch {
                    animate(pull, 0f, animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { value, _ ->
                        pull = value
                    }
                }
                if (open) currentOpenDrafts()
                // The release is used up here, so the list does not also fling.
                return Velocity(0f, available.y)
            }
        }
    }

    val reached = pull >= thresholdPx
    val progress = (pull / thresholdPx).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxSize().nestedScroll(pullConnection)) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = pull * 0.6f },
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                        Text("Vault", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            text = "Notes and guides, one folder per subject",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onOpenTrash) {
                        Icon(AppIcons.Delete, contentDescription = "Open Trash")
                    }
                }
            }
            if (subjects.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Text("No folders yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Each subject gets its own folder for notes, finished notes and guides.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                        PillButton(text = "Add subjects", onClick = onManageSubjects)
                    }
                }
            }
            itemsIndexed(subjects, key = { _, subject -> subject.id }) { index, subject ->
                val active = data.notesOf(subject.id, NoteStatus.ACTIVE).size
                val finished = data.notesOf(subject.id, NoteStatus.FINISHED).size
                val guides = data.guides.count { it.subjectId == subject.id }
                FolderTile(
                    name = subject.name,
                    notes = active,
                    finished = finished,
                    guides = guides,
                    seed = subject.id,
                    dot = subjectColor(subject.id),
                    onClick = { onOpenFolder(subject.id) },
                )
            }
        }

        // The draft icon slides down from the top edge while pulling.
        if (pull > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = contentPadding.calculateTopPadding())
                    .graphicsLayer {
                        translationY = pull * 0.6f - 56.dp.toPx()
                        alpha = progress
                        scaleX = 0.6f + 0.4f * progress + if (reached) 0.12f else 0f
                        scaleY = scaleX
                    }
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (reached) colors.primary else colors.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.Draft,
                    contentDescription = "Release to open Drafts",
                    tint = if (reached) colors.onPrimary else colors.onSurface,
                )
            }
        }
    }
}

/**
 * One subject as a gradient tile (image 07): a soft two-tone wash of the theme's tonal colours, a frosted icon chip at the top left
 * with the subject's colour dot, and the name with the note count at the bottom. Every tile is the same size; it springs when pressed.
 */
@Composable
private fun FolderTile(
    name: String,
    notes: Int,
    finished: Int,
    guides: Int,
    seed: Long,
    dot: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    val q = LocalQuality.current
    val a = skywa1ker404.absencium.ui.components.hueFor(seed)
    val b = skywa1ker404.absencium.ui.components.hueFor(seed + 2)
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "folderPress",
    )
    val ink = a.onContainer
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(28.dp))
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(a.container, androidx.compose.ui.graphics.lerp(a.container, b.container, 0.85f)),
                ),
            )
            .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Folder, contentDescription = null, tint = ink)
            Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(10.dp).clip(CircleShape).background(dot))
        }
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = ink, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(
                "$notes ${if (notes == 1) "note" else "notes"}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                color = ink,
            )
            Text(
                "$finished finished \u00b7 $guides ${if (guides == 1) "guide" else "guides"}",
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.75f),
                maxLines = 1,
            )
        }
    }
}
