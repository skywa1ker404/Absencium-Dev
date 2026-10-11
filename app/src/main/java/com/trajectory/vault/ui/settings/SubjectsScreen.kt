package com.trajectory.vault.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.trajectory.vault.data.db.SubjectEntity
import com.trajectory.vault.ui.SubjectsViewModel
import com.trajectory.vault.ui.components.AppCard
import com.trajectory.vault.ui.components.HintText
import com.trajectory.vault.ui.components.MessageCard
import com.trajectory.vault.ui.components.PillButton
import com.trajectory.vault.ui.components.ScreenScaffold
import com.trajectory.vault.ui.components.SectionTitle
import com.trajectory.vault.ui.containerViewModel

/** Subject muting and the active status manager. Subjects are stored in Room (Subjects_Table). */
@Destination<RootGraph>
@Composable
fun SubjectsScreen(navigator: DestinationsNavigator) {
    val viewModel = containerViewModel { SubjectsViewModel(it) }
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var newName by remember { mutableStateOf("") }

    ScreenScaffold(title = "Subjects", onBack = { navigator.navigateUp() }) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text("Add a subject", style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Subject name") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    text = "Add",
                    onClick = {
                        viewModel.add(newName)
                        newName = ""
                    },
                    enabled = newName.isNotBlank(),
                )
            }
        }

        val feedback = message
        if (feedback != null) MessageCard(feedback)

        SectionTitle("Your subjects")
        if (subjects.isEmpty()) {
            HintText("No subjects yet. Add one above, or add sample data from Settings, then Test Lab.")
        }
        subjects.forEach { subject ->
            key(subject.id) { SubjectCard(subject, viewModel) }
        }
    }
}

@Composable
private fun SubjectCard(subject: SubjectEntity, viewModel: SubjectsViewModel) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (subject.isActive) 1f else 0.6f),
        highlighted = subject.isActive && !subject.isMuted,
    ) {
        Text(subject.name, style = MaterialTheme.typography.titleMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            FilterChip(
                selected = subject.isActive,
                onClick = { viewModel.setActive(subject, !subject.isActive) },
                label = { Text(if (subject.isActive) "Active" else "Inactive") },
                shape = CircleShape,
            )
            FilterChip(
                selected = subject.isMuted,
                onClick = { viewModel.setMuted(subject, !subject.isMuted) },
                label = { Text(if (subject.isMuted) "Muted" else "Mute") },
                shape = CircleShape,
            )
            TextButton(onClick = { viewModel.delete(subject) }) { Text("Delete") }
        }
    }
}
