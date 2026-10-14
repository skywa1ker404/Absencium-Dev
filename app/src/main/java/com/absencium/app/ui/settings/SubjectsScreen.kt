package com.absencium.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.absencium.app.ui.components.ExpressiveChip
import androidx.compose.material3.MaterialTheme
import com.absencium.app.ui.components.AppTextField
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
import com.absencium.app.data.db.SubjectEntity
import com.absencium.app.ui.SubjectsViewModel
import com.absencium.app.ui.components.AppCard
import com.absencium.app.ui.components.HintText
import com.absencium.app.ui.components.MessageCard
import com.absencium.app.ui.components.PillButton
import com.absencium.app.ui.components.ScreenScaffold
import com.absencium.app.ui.components.SectionTitle
import com.absencium.app.ui.containerViewModel

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
                AppTextField(
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
    val scope = rememberCoroutineScope()
    // null: no dialog. stage 1: "are you sure". stage 2: type Yes.
    var entryCount by remember { mutableStateOf<Int?>(null) }
    var stage by remember { mutableStateOf(0) }
    var typed by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf(false) }
    var newName by remember(subject.name) { mutableStateOf(subject.name) }

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
            ExpressiveChip(
                selected = subject.isActive,
                onClick = { viewModel.setActive(subject, !subject.isActive) },
                label = { Text(if (subject.isActive) "Active" else "Inactive") },
                shape = CircleShape,
            )
            ExpressiveChip(
                selected = subject.isMuted,
                onClick = { viewModel.setMuted(subject, !subject.isMuted) },
                label = { Text(if (subject.isMuted) "Muted" else "Mute") },
                shape = CircleShape,
            )
            TextButton(onClick = { renaming = true }) { Text("Rename") }
            TextButton(onClick = {
                scope.launch {
                    entryCount = viewModel.entryCount(subject)
                    typed = ""
                    stage = 1
                }
            }) { Text("Delete") }
        }
    }

    if (renaming) {
        AlertDialog(
            onDismissRequest = { renaming = false; newName = subject.name },
            title = { Text("Rename subject") },
            text = {
                AppTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Subject name") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        viewModel.rename(subject, newName)
                        renaming = false
                    },
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = false; newName = subject.name }) { Text("Cancel") } },
        )
    }

    val count = entryCount
    if (stage > 0 && count != null) {
        fun close() {
            stage = 0
            entryCount = null
            typed = ""
        }
        when {
            count == 0 -> AlertDialog(
                onDismissRequest = { close() },
                title = { Text("Delete ${subject.name}?") },
                text = { Text("It has no entries. It will be deleted for good.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteWithEntries(subject)
                        close()
                    }) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { close() }) { Text("Keep") } },
            )

            stage == 1 -> AlertDialog(
                onDismissRequest = { close() },
                title = { Text("Are you sure?") },
                text = {
                    Text(
                        "${subject.name} has $count ${if (count == 1) "entry" else "entries"} under it " +
                            "(attendance, exams, lectures, notes and guides). Deleting the subject deletes all of them.",
                    )
                },
                confirmButton = { TextButton(onClick = { stage = 2 }) { Text("Yes, continue") } },
                dismissButton = { TextButton(onClick = { close() }) { Text("Cancel") } },
            )

            else -> AlertDialog(
                onDismissRequest = { close() },
                title = { Text("Type Yes to confirm") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("This permanently deletes ${subject.name} and its $count entries.")
                        AppTextField(
                            value = typed,
                            onValueChange = { typed = it },
                            label = { Text("Yes") },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = typed.trim().equals("yes", ignoreCase = true),
                        onClick = {
                            viewModel.deleteWithEntries(subject)
                            close()
                        },
                    ) { Text("Confirm") }
                },
                dismissButton = { TextButton(onClick = { close() }) { Text("Cancel") } },
            )
        }
    }
}
