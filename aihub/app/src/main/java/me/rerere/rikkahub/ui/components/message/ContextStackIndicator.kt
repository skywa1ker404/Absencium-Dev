package me.rerere.rikkahub.ui.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Memory
import me.rerere.rikkahub.ui.components.dots.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import me.rerere.ai.ui.UsedMemory
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.theme.LocalDarkMode

/**
 * Represents a single context source item for unified display
 */
private sealed class ContextStackItem {
    abstract val priority: Int
    
    data class Memory(val memory: UsedMemory) : ContextStackItem() {
        override val priority: Int get() = memory.priority
    }
}

/**
 * Displays stacked context source covers indicating which memories were used.
 * Up to 3 covers are shown stacked, with a "+N" badge if more exist.
 * Left-most = highest priority at full visibility.
 * Uses color overlay for dimming instead of transparency.
 */
@Composable
fun ContextStackIndicator(
    memories: List<UsedMemory> = emptyList(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalDarkMode.current
    
    // Combine all items and sort by priority
    val allItems = remember(memories) {
        buildList {
            memories.forEach { add(ContextStackItem.Memory(it)) }
        }.sortedByDescending { it.priority }
    }
    
    if (allItems.isEmpty()) return
    
    val displayItems = allItems.take(3)
    val extraCount = (allItems.size - 3).coerceAtLeast(0)
    
    // Config
    val bookWidth = 24.dp
    val bookHeight = 32.dp
    val overlap = 14.dp
    val badgeHeight = 20.dp
    val badgeWidth = when {
        extraCount <= 0 -> 0.dp
        extraCount <= 9 -> 20.dp
        extraCount <= 99 -> 26.dp
        else -> 32.dp
    }
    
    val totalWidth = if (displayItems.isNotEmpty()) {
        val booksWidth = bookWidth + (overlap * (displayItems.size - 1))
        if (extraCount > 0) {
            booksWidth - 10.dp + badgeWidth
        } else {
            booksWidth
        }
    } else {
        0.dp
    }
    
    // Overlay color for dimming (darken in dark mode, lighten in light mode)
    val overlayColor = if (isDarkMode) Color.Black else Color.White

    Box(
        modifier = modifier
            .width(totalWidth)
            .height(bookHeight)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        displayItems.forEachIndexed { index, item ->
            // Calculate overlay alpha for dimming effect
            val overlayAlpha = when (index) {
                0 -> 0f       // First (left-most) - no overlay
                1 -> 0.3f     // Second - slight dim
                else -> 0.55f // Third - more dim
            }
            
            when (item) {
                is ContextStackItem.Memory -> {
                    MemoryCover(
                        memory = item.memory,
                        overlayColor = overlayColor,
                        overlayAlpha = overlayAlpha,
                        modifier = Modifier
                            .offset(x = overlap * index)
                            .zIndex((displayItems.size - index).toFloat())
                            .width(bookWidth)
                            .height(bookHeight)
                    )
                }
            }
        }
        
        // "+N" badge (grows into a pill when extraCount >= 10)
        if (extraCount > 0) {
            val badgeOffset = bookWidth + (overlap * (displayItems.size - 1)) - 10.dp
            Box(
                modifier = Modifier
                    .offset(x = badgeOffset)
                    .height(badgeHeight)
                    .defaultMinSize(minWidth = badgeWidth)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(horizontal = if (extraCount > 9) 4.dp else 0.dp)
                    .zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$extraCount",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 9.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * Memory cover with type-based styling and icons
 */
@Composable
private fun MemoryCover(
    memory: UsedMemory,
    overlayColor: Color,
    overlayAlpha: Float,
    modifier: Modifier = Modifier
) {
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
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp)
            )
            .drawWithContent {
                drawContent()
                if (overlayAlpha > 0f) {
                    drawRect(overlayColor.copy(alpha = overlayAlpha))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            isCore -> {
                Icon(
                    imageVector = Icons.Rounded.Memory,
                    contentDescription = memoryTypeLabel,
                    modifier = Modifier.size(16.dp),
                    tint = contentColor
                )
            }
            memory.memoryId < 0 -> {
                // Recent chat reference (non-RAG mode)
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = memoryTypeLabel,
                    modifier = Modifier.size(16.dp),
                    tint = contentColor
                )
            }
            else -> {
                // True episodic memory (RAG mode)
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.search_activity_24),
                    contentDescription = memoryTypeLabel,
                    modifier = Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(contentColor)
                )
            }
        }
    }
}

