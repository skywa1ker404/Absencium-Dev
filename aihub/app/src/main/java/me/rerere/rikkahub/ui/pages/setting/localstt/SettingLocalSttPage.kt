package me.rerere.rikkahub.ui.pages.setting.localstt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import me.rerere.rikkahub.ui.components.ui.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.rerere.rikkahub.utils.plus
import me.rerere.common.inference.LocalInferenceWorkload
import me.rerere.asr.local.InstalledSherpaModel
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.ai.models.inferFamilyEntry
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.AutoAIIcon
import me.rerere.rikkahub.ui.components.ui.AutoAIIconWithUrl
import me.rerere.rikkahub.ui.components.ui.DebouncedTextField
import me.rerere.rikkahub.ui.components.ui.EmptyStateCard
import me.rerere.rikkahub.ui.components.ui.HapticSwitch
import me.rerere.rikkahub.ui.components.ui.ItemPosition
import me.rerere.rikkahub.ui.components.ui.PhysicsSwipeToDelete
import me.rerere.rikkahub.ui.components.ui.Tag
import me.rerere.rikkahub.ui.components.ui.TagType
import me.rerere.rikkahub.ui.components.ui.ToastType
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.hooks.HapticPattern
import me.rerere.rikkahub.ui.hooks.rememberPremiumHaptics
import me.rerere.rikkahub.ui.pages.setting.localstt.SettingLocalSttViewModel
import me.rerere.rikkahub.ui.pages.setting.localstt.SherpaDownloadableModelCard
import me.rerere.rikkahub.ui.pages.setting.localstt.SherpaInstalledModelCard
import me.rerere.rikkahub.ui.pages.setting.localstt.SherpaModelDragHandle
import me.rerere.rikkahub.ui.pages.setting.localstt.SherpaModelSettingsSheet
import me.rerere.rikkahub.ui.theme.AppShapes
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** On-device speech recognition models (sherpa-onnx). Absencium: Absencium AI's "Local models" page without the on-device chat models. */
@Composable
fun SettingLocalSttPage(
    sttVm: SettingLocalSttViewModel = koinViewModel(),
    navigationIcon: @Composable () -> Unit = { BackButton() },
) {
    val sttState by sttVm.uiState.collectAsStateWithLifecycle()
    val haptics = rememberPremiumHaptics()
    var editingSherpaModel by remember { mutableStateOf<InstalledSherpaModel?>(null) }
    var sherpaModelPendingDelete by remember { mutableStateOf<InstalledSherpaModel?>(null) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val sttReorderableState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = from.index - 1
        val toIndex = to.index - 1
        if (fromIndex in sttState.installed.indices && toIndex in sttState.installed.indices) {
            sttVm.moveInstalledModel(fromIndex, toIndex)
        }
    }
    var sttDraggingIndex by remember { mutableStateOf(-1) }
    var sttDragOffset by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var sttIsUnlocked by remember { mutableStateOf(false) }
    var sttNeighborsUnlocked by remember { mutableStateOf(false) }
    if (sttDragOffset == 0f && sttNeighborsUnlocked) {
        sttNeighborsUnlocked = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = navigationIcon,
                title = { Text("Speech models") },
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(16.dp) + padding,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader("Speech recognition models (sherpa-onnx)")
                Text(
                    "These models run speech-to-text fully on-device. Audio is not uploaded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                )
            }
            itemsIndexed(sttState.installed, key = { _, model -> "sherpa-installed-${model.id}" }) { index, model ->
                val position = when {
                    sttState.installed.size == 1 -> ItemPosition.ONLY
                    index == 0 -> ItemPosition.FIRST
                    index == sttState.installed.lastIndex -> ItemPosition.LAST
                    else -> ItemPosition.MIDDLE
                }
                val thresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 35.dp.toPx() }
                if (sttDraggingIndex >= 0 && !sttNeighborsUnlocked && kotlin.math.abs(sttDragOffset) >= thresholdPx) {
                    sttNeighborsUnlocked = true
                }
                val neighborOffset = if (
                    sttDraggingIndex >= 0 && sttDraggingIndex != index && !sttIsUnlocked && !sttNeighborsUnlocked
                ) {
                    when (kotlin.math.abs(index - sttDraggingIndex)) {
                        1 -> sttDragOffset * 0.35f
                        2 -> sttDragOffset * 0.12f
                        else -> 0f
                    }
                } else {
                    0f
                }
                ReorderableItem(state = sttReorderableState, key = "sherpa-installed-${model.id}") { isDragging ->
                    PhysicsSwipeToDelete(
                        position = position,
                        neighborOffset = neighborOffset,
                        onDragProgress = { offset, unlocked ->
                            sttDraggingIndex = index
                            sttDragOffset = offset
                            sttIsUnlocked = unlocked
                        },
                        onDragEnd = {
                            if (sttDraggingIndex == index) {
                                sttDraggingIndex = -1
                                sttDragOffset = 0f
                            }
                        },
                        onDelete = { sherpaModelPendingDelete = model },
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(if (isDragging) 0.95f else 1f),
                    ) { shape ->
                        SherpaInstalledModelCard(
                            model = model,
                            shape = shape,
                            onClick = { editingSherpaModel = model },
                            dragHandle = {
                                SherpaModelDragHandle(
                                    modifier = Modifier.longPressDraggableHandle(
                                        onDragStarted = { haptics.perform(HapticPattern.Pop) },
                                        onDragStopped = { haptics.perform(HapticPattern.Thud) },
                                    ),
                                )
                            },
                        )
                    }
                }
            }
            if (sttState.downloadable.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader("Download speech recognition models")
                }
                itemsIndexed(sttState.downloadable, key = { _, model -> "sherpa-download-${model.id}" }) { index, model ->
                    val shape = when {
                        sttState.downloadable.size == 1 -> RoundedCornerShape(24.dp)
                        index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 10.dp, bottomEnd = 10.dp)
                        index == sttState.downloadable.lastIndex -> RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                        else -> RoundedCornerShape(10.dp)
                    }
                    SherpaDownloadableModelCard(
                        model = model,
                        shape = shape,
                        download = sttState.downloads[model.id],
                        onDownload = {
                            haptics.perform(HapticPattern.Pop)
                            sttVm.download(model)
                        },
                        onCancel = { sttVm.cancelDownload(model.id) },
                        onDismissFailure = { sttVm.dismissDownloadError(model.id) },
                    )
                }
            }

        }
    }

    editingSherpaModel?.let { model ->
        val live = sttState.installed.firstOrNull { it.id == model.id } ?: model
        SherpaModelSettingsSheet(
            model = live,
            onDismiss = { editingSherpaModel = null },
            onRename = { sttVm.rename(live.id, it) },
            onConfigChange = { sttVm.updateConfig(live.id, it) },
            onDelete = {
                haptics.perform(HapticPattern.Thud)
                sttVm.delete(live)
                editingSherpaModel = null
            },
        )
    }

    sherpaModelPendingDelete?.let { model ->
        AlertDialog(
            onDismissRequest = { sherpaModelPendingDelete = null },
            title = { Text("Delete ${model.displayName}?") },
            text = { Text("The downloaded speech model files will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    haptics.perform(HapticPattern.Thud)
                    sttVm.delete(model)
                    sherpaModelPendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { sherpaModelPendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    )
}
