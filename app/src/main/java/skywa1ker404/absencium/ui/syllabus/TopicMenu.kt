package skywa1ker404.absencium.ui.syllabus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import skywa1ker404.absencium.data.db.SyllabusNodeEntity
import skywa1ker404.absencium.data.syllabus.SyllabusTree
import skywa1ker404.absencium.ui.icons.AppIcons
import skywa1ker404.absencium.ui.components.AppMenu
import skywa1ker404.absencium.ui.components.AppMenuDivider
import skywa1ker404.absencium.ui.components.AppMenuItem
import skywa1ker404.absencium.ui.components.hueFor

/**
 * The syllabus of one subject as a menu: a first row for "no topic" ([noneLabel]), then every unit, chapter and topic indented by
 * level with a small dot in the colour of its level and a check on the chosen one, then a way to open the syllabus. Every row has the
 * same height and the same 20 dp leading slot, so the first row and the tree rows line up. The container is solid and scrolls when long.
 */
@Composable
fun TopicMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    nodes: List<SyllabusNodeEntity>,
    selectedId: Long?,
    noneLabel: String,
    onPick: (Long?) -> Unit,
    onOpenSyllabus: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AppMenu(expanded = expanded, onDismiss = onDismiss, solid = true, offset = DpOffset(0.dp, 4.dp), modifier = Modifier.widthIn(min = 240.dp, max = 320.dp)) {
        Box(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
            androidx.compose.foundation.layout.Column {
                TopicRow(
                    depth = 0,
                    title = noneLabel,
                    chosen = selectedId == null,
                    leading = { Icon(AppIcons.Description, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                    onClick = { onPick(null) },
                    strong = true,
                )
                val rows = SyllabusTree.rows(nodes, emptySet())
                if (rows.isNotEmpty()) AppMenuDivider()
                rows.forEach { row ->
                    val hue = hueFor(row.depth.toLong())
                    TopicRow(
                        depth = row.depth,
                        title = row.node.title,
                        chosen = selectedId == row.node.id,
                        leading = { Box(Modifier.size(10.dp).clip(CircleShape).background(hue.accent)) },
                        onClick = { onPick(row.node.id) },
                        strong = row.depth == 0,
                    )
                }
                AppMenuDivider()
                AppMenuItem(AppIcons.School, if (nodes.isEmpty()) "Build the syllabus" else "Open the syllabus", onOpenSyllabus)
            }
        }
    }
}

@Composable
private fun TopicRow(
    depth: Int,
    title: String,
    chosen: Boolean,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
    strong: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) { leading() }
                Text(
                    title,
                    style = if (strong) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge,
                    color = if (chosen) colors.primary else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        },
        onClick = onClick,
        trailingIcon = if (chosen) ({ Icon(AppIcons.Check, contentDescription = "Chosen", tint = colors.primary, modifier = Modifier.size(20.dp)) }) else null,
        modifier = Modifier
            .padding(start = (6 + depth * 12).dp, end = 6.dp, top = 1.dp, bottom = 1.dp)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(18.dp))
            .then(if (chosen) Modifier.background(colors.primaryContainer.copy(alpha = 0.45f)) else Modifier),
    )
}
