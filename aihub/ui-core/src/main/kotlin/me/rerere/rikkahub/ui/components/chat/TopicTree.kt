package me.rerere.rikkahub.ui.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** One row of a syllabus tree. A list of these comes depth first: a row's parent is the nearest earlier row one level up. */
data class TopicNode(val id: Long, val title: String, val depth: Int)

/**
 * The one unified topic list used wherever a syllabus unit, chapter or topic is chosen (filing a note or material, the Add Entry
 * page, the note's topic chip, and the AI's Vault link). Every unit and chapter that has something under it shows an arrow at its
 * left; everything starts collapsed and any level can be chosen. [noneLabel] (when not null) adds the "no topic" row at the top.
 * Opening and closing animate each row with a spring, and the list never changes the size of the sheet around it.
 */
@Composable
fun TopicTree(
    nodes: List<TopicNode>,
    selectedId: Long?,
    noneLabel: String?,
    onPick: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val parents = remember(nodes) {
        val out = HashMap<Long, Long?>()
        val stack = ArrayList<TopicNode>()
        nodes.forEach { n ->
            while (stack.size > n.depth) stack.removeAt(stack.lastIndex)
            out[n.id] = stack.lastOrNull()?.id
            stack.add(n)
        }
        out
    }
    val withChildren = remember(parents) { parents.values.filterNotNull().toSet() }
    val expanded = remember { mutableStateListOf<Long>() }
    fun isVisible(n: TopicNode): Boolean {
        var parent = parents[n.id]
        while (parent != null) {
            if (parent !in expanded) return false
            parent = parents[parent]
        }
        return true
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (noneLabel != null) {
            TopicRowItem(noneLabel, depth = 0, chosen = selectedId == null, expandable = false, expanded = false, strong = true, onToggle = {}) { onPick(null) }
        }
        nodes.forEach { n ->
            androidx.compose.runtime.key(n.id) {
                AnimatedVisibility(
                    visible = isVisible(n),
                    enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                    exit = shrinkVertically(spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)) + fadeOut(),
                ) {
                    TopicRowItem(
                        title = n.title, depth = n.depth, chosen = selectedId == n.id,
                        expandable = n.id in withChildren, expanded = n.id in expanded, strong = n.depth == 0,
                        onToggle = { if (n.id in expanded) expanded.remove(n.id) else expanded.add(n.id) },
                    ) { onPick(n.id) }
                }
            }
        }
    }
}

@Composable
private fun TopicRowItem(
    title: String,
    depth: Int,
    chosen: Boolean,
    expandable: Boolean,
    expanded: Boolean,
    strong: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val turn = animateFloatAsState(if (expanded) 90f else 0f, spring(dampingRatio = 0.7f, stiffness = 500f), label = "topicArrow")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 14).dp)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (expandable) {
            IconButton(onClick = onToggle, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(turn.value),
                )
            }
        } else {
            Spacer(Modifier.size(40.dp))
        }
        Text(
            title,
            style = if (strong) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge,
            color = if (chosen) colors.primary else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (chosen) Icon(Icons.Rounded.Check, contentDescription = "Chosen", tint = colors.primary, modifier = Modifier.padding(end = 12.dp).size(20.dp))
    }
}
