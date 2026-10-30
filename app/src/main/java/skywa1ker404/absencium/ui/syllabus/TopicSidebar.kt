package skywa1ker404.absencium.ui.syllabus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.data.db.SyllabusNodeEntity
import skywa1ker404.absencium.data.syllabus.SyllabusTree
import skywa1ker404.absencium.ui.components.PillButton
import skywa1ker404.absencium.ui.components.hueFor
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.theme.LocalQuality

/**
 * The topics of one subject as a side panel that springs in from the left over a dimmed page: "All topics" first, then every unit,
 * chapter and topic with the number of notes under it, units that fold open and shut, and the chosen one on a tinted pill.
 * Tapping outside, swiping it back to the left or the system back closes it.
 */
@Composable
fun TopicSidebar(
    visible: Boolean,
    onDismiss: () -> Unit,
    subjectName: String,
    nodes: List<SyllabusNodeEntity>,
    /** Notes under each node, including everything inside it. */
    counts: Map<Long, Int>,
    totalNotes: Int,
    selectedId: Long?,
    onPick: (Long?) -> Unit,
    onOpenSyllabus: () -> Unit,
) {
    val q = LocalQuality.current
    val colors = MaterialTheme.colorScheme
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "topicSidebar",
    )
    if (!visible && progress < 0.002f) return
    BackHandler(enabled = visible, onBack = onDismiss)

    // Units fold open and shut; the path to the chosen topic starts open.
    val parents = remember(nodes) { nodes.mapNotNull { it.parentId }.toSet() }
    var open by remember(nodes.size) {
        mutableStateOf(
            buildSet {
                var up = nodes.firstOrNull { it.id == selectedId }?.parentId
                while (up != null) {
                    add(up)
                    up = nodes.firstOrNull { it.id == up }?.parentId
                }
            },
        )
    }
    val rows = remember(nodes, open, parents) { SyllabusTree.rows(nodes, parents - open) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val panelWidth = minOf(330.dp, maxWidth * 0.86f)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
        var drag by remember { mutableStateOf(0f) }
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(panelWidth)
                .graphicsLayer { translationX = -(1f - progress).coerceAtLeast(0f) * size.width + drag.coerceAtMost(0f) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = { if (drag < -size.width * 0.18f) onDismiss(); drag = 0f },
                        onDragCancel = { drag = 0f },
                    ) { _, amount -> drag += amount }
                },
            shape = RoundedCornerShape(topEnd = 36.dp, bottomEnd = 36.dp),
            color = colors.surfaceContainerLow,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
        ) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    val hue = hueFor(0)
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(hue.container), contentAlignment = Alignment.Center) {
                        Icon(AppIcons.School, contentDescription = null, tint = hue.onContainer)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Topics", style = MaterialTheme.typography.headlineSmall)
                        Text(subjectName, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    item(key = "all") {
                        SidebarRow(
                            depth = 0,
                            title = "All topics",
                            count = totalNotes,
                            chosen = selectedId == null,
                            leading = { Icon(AppIcons.Description, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp)) },
                            expandable = false,
                            expanded = false,
                            strong = true,
                            onToggle = {},
                            onClick = { onPick(null) },
                        )
                    }
                    items(rows, key = { it.node.id }) { row ->
                        val hue = hueFor(row.depth.toLong())
                        SidebarRow(
                            depth = row.depth,
                            title = row.node.title,
                            count = counts[row.node.id] ?: 0,
                            chosen = selectedId == row.node.id,
                            leading = { Box(Modifier.size(10.dp).clip(CircleShape).background(hue.accent)) },
                            expandable = row.hasChildren,
                            expanded = row.node.id in open,
                            strong = row.depth == 0,
                            onToggle = { open = if (row.node.id in open) open - row.node.id else open + row.node.id },
                            onClick = { onPick(row.node.id) },
                        )
                    }
                }
                PillButton(
                    text = if (nodes.isEmpty()) "Build the syllabus" else "Open the syllabus",
                    onClick = onOpenSyllabus,
                    tonal = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun SidebarRow(
    depth: Int,
    title: String,
    count: Int,
    chosen: Boolean,
    leading: @Composable () -> Unit,
    expandable: Boolean,
    expanded: Boolean,
    strong: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val q = LocalQuality.current
    val turn by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "sidebarChevron",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 14).dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (chosen) colors.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics {
                selected = chosen
                contentDescription = "$title, $count notes"
            }
            .padding(start = 14.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) { leading() }
        Text(
            title,
            style = if (strong) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge,
            fontWeight = if (chosen) FontWeight.SemiBold else null,
            color = if (chosen) colors.onPrimaryContainer else colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
        )
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = if (chosen) colors.onPrimaryContainer else colors.onSurfaceVariant,
        )
        if (expandable) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.ExpandMore,
                    contentDescription = if (expanded) "Fold up" else "Open",
                    tint = if (chosen) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    modifier = Modifier.size(22.dp).rotate(turn),
                )
            }
        } else {
            Box(Modifier.size(12.dp))
        }
    }
}
