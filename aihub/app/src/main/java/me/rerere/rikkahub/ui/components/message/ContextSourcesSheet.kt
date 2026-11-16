package me.rerere.rikkahub.ui.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Memory
import me.rerere.rikkahub.ui.components.dots.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.rerere.ai.ui.UsedMemory
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.theme.LocalDarkMode

// Corner radius values matching PhysicsSwipeToDelete
private val groupCornerRadius = 24.dp
private val itemCornerRadius = 10.dp

/**
 * Bottom sheet displaying the memories used in a message.
 */
@Composable
fun ContextSourcesSheet(
    memories: List<UsedMemory> = emptyList(),
    onMemoryClick: ((UsedMemory) -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    val sortedMemories = remember(memories) { memories.sortedByDescending { it.priority } }
    
    ModalBottomSheet(
containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        dragHandle = {
            IconButton(
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onDismissRequest()
                    }
                }
            ) {
                Icon(Icons.Rounded.KeyboardArrowDown, null)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header - centered
            Text(
                text = stringResource(R.string.context_sources_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Memories Section
                if (sortedMemories.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.context_sources_memories)) }
                    itemsIndexed(sortedMemories) { index, memory ->
                        val shape = getGroupedShape(index, sortedMemories.size)
                        MemoryItem(
                            memory = memory,
                            shape = shape,
                            isFirst = index == 0,
                            isLast = index == sortedMemories.lastIndex,
                            onClick = { onMemoryClick?.invoke(memory) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Calculates the corner shape for a grouped item based on position
 */
private fun getGroupedShape(index: Int, total: Int): RoundedCornerShape {
    return when {
        total == 1 -> RoundedCornerShape(groupCornerRadius)
        index == 0 -> RoundedCornerShape(
            topStart = groupCornerRadius, topEnd = groupCornerRadius,
            bottomStart = itemCornerRadius, bottomEnd = itemCornerRadius
        )
        index == total - 1 -> RoundedCornerShape(
            topStart = itemCornerRadius, topEnd = itemCornerRadius,
            bottomStart = groupCornerRadius, bottomEnd = groupCornerRadius
        )
        else -> RoundedCornerShape(itemCornerRadius)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
private fun MemoryItem(
    memory: UsedMemory,
    shape: RoundedCornerShape,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit
) {
    val isDarkMode = LocalDarkMode.current
    val isCore = memory.memoryType == 0
    val memoryTypeLabel = when {
        isCore -> stringResource(R.string.activity_timeline_memory_core)
        memory.memoryId < 0 -> stringResource(R.string.context_sources_recent_chat)
        else -> stringResource(R.string.activity_timeline_memory_episodic)
    }
    
    val backgroundColor = if (isCore) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isCore) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    
    // Optical roundness for the cover
    val opticalRadius = 12.dp
    val defaultRadius = 6.dp
    val coverShape = RoundedCornerShape(
        topStart = if (isFirst) opticalRadius else defaultRadius,
        topEnd = defaultRadius,
        bottomStart = if (isLast) opticalRadius else defaultRadius,
        bottomEnd = defaultRadius
    )
    
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Memory type icon
            Box(
                modifier = Modifier
                    .width(45.dp)
                    .height(60.dp)
                    .clip(coverShape)
                    .background(backgroundColor)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = coverShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isCore -> {
                        Icon(
                            imageVector = Icons.Rounded.Memory,
                            contentDescription = memoryTypeLabel,
                            modifier = Modifier.size(24.dp),
                            tint = contentColor
                        )
                    }
                    memory.memoryId < 0 -> {
                        // Recent chat reference (non-RAG mode)
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = memoryTypeLabel,
                            modifier = Modifier.size(24.dp),
                            tint = contentColor
                        )
                    }
                    else -> {
                        // True episodic memory (RAG mode)
                        Image(
                            painter = painterResource(R.drawable.search_activity_24),
                            contentDescription = memoryTypeLabel,
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(contentColor)
                        )
                    }
                }
            }
            
            // Memory info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = memoryTypeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = memory.memoryContent,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                memory.activationReason?.let { reason ->
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

