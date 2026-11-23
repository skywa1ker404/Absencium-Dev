package me.rerere.rikkahub.ui.pages.assistant.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.AssistantMemory
import me.rerere.rikkahub.ui.theme.AppShapes
import me.rerere.rikkahub.ui.theme.LocalDarkMode
import me.rerere.rikkahub.ui.theme.RoundedCornerShape

/** The query field and Test button shared by both memory debuggers. */
@Composable
internal fun MemoryQueryRow(query: String, onQueryChange: (String) -> Unit, running: Boolean, onTest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.assistant_memory_test_query)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        Button(onClick = onTest, enabled = query.isNotBlank() && !running) {
            if (running) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(R.string.assistant_memory_test))
            }
        }
    }
}

/**
 * One retrieved memory with its score. With a [cutoff] (the live threshold) the card says whether the score passes it; without one
 * it shows the memory's type and id instead.
 */
@Composable
internal fun MemoryResultCard(index: Int, memory: AssistantMemory, score: Float, cutoff: Float?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val passes = cutoff?.let { score >= it } ?: (score >= 0.5f)
    val scoreColor = when {
        passes -> colors.primary
        cutoff != null -> colors.error
        else -> colors.onSurfaceVariant
    }
    Card(
        shape = AppShapes.CardMedium,
        colors = CardDefaults.cardColors(containerColor = if (LocalDarkMode.current) colors.surfaceContainerLow else colors.surfaceContainerHighest),
        modifier = modifier.padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("#${index + 1}", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                Text(
                    stringResource(R.string.assistant_memory_score, String.format("%.4f", score)),
                    style = MaterialTheme.typography.labelMedium,
                    color = scoreColor,
                )
            }
            if (cutoff != null) {
                Text(
                    text = if (passes) "Score meets live cutoff" else "Score below live cutoff",
                    style = MaterialTheme.typography.labelSmall,
                    color = scoreColor,
                )
            }
            Text(
                text = memory.content,
                style = if (cutoff != null) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                maxLines = if (cutoff != null) 3 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            if (cutoff == null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = stringResource(
                            R.string.assistant_memory_type_label,
                            stringResource(if (memory.type == 0) R.string.assistant_memory_type_core else R.string.assistant_memory_type_episodic),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.assistant_memory_id_label, memory.id),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
