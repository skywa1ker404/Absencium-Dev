package com.trajectory.vault.ui.vault

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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.trajectory.vault.data.db.NoteStatus
import com.trajectory.vault.data.vault.VaultData
import com.trajectory.vault.ui.calendar.subjectColor
import com.trajectory.vault.ui.components.AppCard
import com.trajectory.vault.ui.components.PillButton
import com.trajectory.vault.ui.icons.AppIcons

/** The Vault tab: one folder per subject in a two-column grid. */
@Composable
fun VaultTab(
    data: VaultData,
    contentPadding: PaddingValues,
    onOpenFolder: (Long) -> Unit,
    onManageSubjects: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val subjects = data.subjects.filter { it.isActive }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
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
            androidx.compose.foundation.layout.Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                Text("Vault", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Notes and guides, one folder per subject",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
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
        items(subjects, key = { it.id }) { subject ->
            val active = data.notesOf(subject.id, NoteStatus.ACTIVE).size
            val finished = data.notesOf(subject.id, NoteStatus.FINISHED).size
            val guides = data.guides.count { it.subjectId == subject.id }
            AppCard(modifier = Modifier.fillMaxWidth(), onClick = { onOpenFolder(subject.id) }) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(subjectColor(subject.id).copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.Folder, contentDescription = null, tint = colors.onSurface)
                }
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = "$active ${if (active == 1) "note" else "notes"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = "$finished finished · $guides ${if (guides == 1) "guide" else "guides"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}
