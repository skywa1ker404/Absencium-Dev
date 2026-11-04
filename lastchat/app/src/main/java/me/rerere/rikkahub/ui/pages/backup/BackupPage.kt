package me.rerere.rikkahub.ui.pages.backup

import me.rerere.rikkahub.ui.theme.LocalDarkMode
import me.rerere.rikkahub.utils.PermissionChecker

import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import me.rerere.rikkahub.ui.components.ui.ToastType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.WebDavConfig
import me.rerere.rikkahub.data.sync.BackupContentOption
import me.rerere.rikkahub.data.sync.WebDavBackupItem
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.FormItem
import me.rerere.rikkahub.ui.components.ui.LastChatDestructiveConfirmDialog
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.hooks.HapticPattern
import me.rerere.rikkahub.ui.hooks.rememberPremiumHaptics
import me.rerere.rikkahub.ui.modifier.lastChatSheetContainerColor
import me.rerere.rikkahub.ui.pages.setting.LocalSettingsWideLayout
import me.rerere.rikkahub.ui.pages.setting.components.SettingGroupItem
import me.rerere.rikkahub.ui.pages.setting.components.SettingsGroup
import me.rerere.rikkahub.ui.theme.AppShapes
import me.rerere.rikkahub.ui.theme.AppSurface
import me.rerere.rikkahub.utils.fileSizeToString
import me.rerere.rikkahub.utils.onError
import me.rerere.rikkahub.utils.onLoading
import me.rerere.rikkahub.utils.onSuccess
import me.rerere.rikkahub.utils.toLocalDateTime
import okio.buffer
import okio.sink
import okio.source
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

enum class BackupTab(
    val routeValue: String,
    val icon: ImageVector,
    val titleRes: Int,
) {
    Local("local", Icons.Rounded.Folder, R.string.backup_page_import_export);

    companion object {
        fun fromRoute(route: String): BackupTab {
            return entries.firstOrNull { it.routeValue == route } ?: Local
        }
    }
}

@Composable
fun BackupPage(
    initialTab: BackupTab = BackupTab.Local,
    vm: BackupVM = koinViewModel(),
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = initialTab.ordinal) { BackupTab.entries.size }
    val scope = rememberCoroutineScope()
    val useWideLayout = LocalSettingsWideLayout.current
    val currentTab = BackupTab.entries[pagerState.currentPage]

    LaunchedEffect(initialTab) {
        if (pagerState.currentPage != initialTab.ordinal) {
            pagerState.scrollToPage(initialTab.ordinal)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(currentTab.titleRes))
                },
                navigationIcon = {
                    BackButton()
                }
            )
        },
        bottomBar = {
            if (!useWideLayout) {
                BackupTabPillBar(
                    selectedTab = currentTab,
                    enableHaptics = settings.displaySetting.enableUIHaptics,
                    onTabSelected = { tab ->
                        scope.launch { pagerState.animateScrollToPage(tab.ordinal) }
                    }
                )
            }
        }
    ) { innerPadding ->
        if (useWideLayout) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                when (currentTab) {
                    BackupTab.Local -> ImportExportPage(vm)
                }
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .background(MaterialTheme.colorScheme.background),
                userScrollEnabled = true,
            ) { page ->
                when (BackupTab.entries[page]) {
                    BackupTab.Local -> ImportExportPage(vm)
                }
            }
        }
    }
}

@Composable
private fun BackupTabPillBar(
    selectedTab: BackupTab,
    enableHaptics: Boolean,
    onTabSelected: (BackupTab) -> Unit,
) {
    val haptics = rememberPremiumHaptics(enabled = enableHaptics)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BackupTab.entries.forEach { tab ->
                    val selected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .then(
                                if (selected) {
                                    Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                } else {
                                    Modifier.clickable {
                                        haptics.perform(HapticPattern.Tick)
                                        onTabSelected(tab)
                                    }
                                }
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = stringResource(tab.titleRes),
                            tint = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupCardLabel(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ImportExportPage(
    vm: BackupVM
) {
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    var isExporting by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var showExportContentDialog by remember { mutableStateOf(false) }
    var selectedExportContent by remember { mutableStateOf<Set<BackupContentOption>?>(null) }
    var selectedExportKeyIds by remember { mutableStateOf<Set<kotlin.uuid.Uuid>?>(null) }

    var restoreResult by remember { mutableStateOf<me.rerere.rikkahub.data.sync.WebdavSync.RestoreResult?>(null) }
    
    // Permission handling after restore
    var pendingFeatureAccess by remember {
        mutableStateOf(PermissionChecker.MissingFeatureAccess())
    }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        scope.launch {
            val missing = PermissionChecker.getMissingFeatureAccess(context, vm.getAssistantsSnapshot())
            if (missing.specialAccesses.isNotEmpty()) {
                pendingFeatureAccess = PermissionChecker.MissingFeatureAccess(
                    specialAccesses = missing.specialAccesses
                )
                showPermissionDialog = true
            } else {
                showRestartDialog = true
            }
        }
    }

    val specialAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        showRestartDialog = true
    }

    // 导入类型：local 为本地备份，chatbox 为 Chatbox 导入
    var importType by remember { mutableStateOf("local") }
    var pendingLocalRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // 创建文件保存的launcher
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { targetUri ->
            scope.launch {
                isExporting = true
                runCatching {
                    // 导出文件
                    val exportFile = vm.exportToFile(
                        selectedContent = selectedExportContent,
                        selectedKeyIds = selectedExportKeyIds,
                    )

                    // 复制到用户选择的位置
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                            exportFile.source().buffer().inputStream().use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }

                    // 清理临时文件
                    withContext(Dispatchers.IO) {
                        exportFile.delete()
                    }

                    toaster.show(
                        context.getString(R.string.backup_page_backup_success),
                        type = ToastType.Success
                    )
                }.onFailure { e ->
                    e.printStackTrace()
                    toaster.show(
                        context.getString(R.string.backup_page_backup_failed, e.message ?: ""),
                        type = ToastType.Error
                    )
                }
                isExporting = false
            }
        }
    }

    // 创建文件选择的launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { sourceUri ->
            if (importType == "local") {
                pendingLocalRestoreUri = sourceUri
                return@rememberLauncherForActivityResult
            }
            scope.launch {
                isRestoring = true
                runCatching {
                    when (importType) {
                        "chatbox" -> {
                            val tempFile =
                                File(context.cacheDir, "temp_chatbox_${System.currentTimeMillis()}.json")

                            try {
                                withContext(Dispatchers.IO) {
                                    context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                                        tempFile.sink().buffer().outputStream().use { outputStream ->
                                            inputStream.copyTo(outputStream)
                                        }
                                    }
                                }
                                vm.restoreFromChatBox(tempFile)
                            } finally {
                                withContext(Dispatchers.IO) {
                                    tempFile.delete()
                                }
                            }
                        }
                        "cherry" -> {
                            val tempFile =
                                File(context.cacheDir, "temp_cherry_${System.currentTimeMillis()}.zip")

                            try {
                                withContext(Dispatchers.IO) {
                                    context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                                        tempFile.sink().buffer().outputStream().use { outputStream ->
                                            inputStream.copyTo(outputStream)
                                        }
                                    }
                                }
                                vm.restoreFromCherryStudio(tempFile)
                            } finally {
                                withContext(Dispatchers.IO) {
                                    tempFile.delete()
                                }
                            }
                        }
                    }

                    toaster.show(
                        context.getString(R.string.backup_page_restore_success),
                        type = ToastType.Success
                    )
                }.onFailure { e ->
                    e.printStackTrace()
                    toaster.show(
                        context.getString(R.string.backup_page_restore_failed, e.message ?: ""),
                        type = ToastType.Error
                    )
                }
                isRestoring = false
            }
        }
    }

    pendingLocalRestoreUri?.let { sourceUri ->
        LastChatDestructiveConfirmDialog(
            title = stringResource(R.string.backup_page_restore_overwrite_title),
            consequence = stringResource(R.string.backup_page_restore_overwrite_consequence),
            confirmLabel = stringResource(R.string.backup_page_restore_now),
            onDismiss = { pendingLocalRestoreUri = null },
            onConfirm = {
                pendingLocalRestoreUri = null
                scope.launch {
                    isRestoring = true
                    runCatching {
                        val tempFile =
                            File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}.zip")
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                                tempFile.sink().buffer().outputStream().use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        }
                        val result = vm.restoreFromLocalFile(tempFile)
                        restoreResult = result
                        withContext(Dispatchers.IO) {
                            tempFile.delete()
                        }
                        toaster.show(
                            context.getString(R.string.backup_page_restore_success),
                            type = ToastType.Success
                        )
                        val missing = PermissionChecker.getMissingFeatureAccess(
                            context,
                            vm.getAssistantsSnapshot()
                        )
                        if (!missing.isEmpty) {
                            pendingFeatureAccess = missing
                            showPermissionDialog = true
                        } else {
                            showRestartDialog = true
                        }
                    }.onFailure { e ->
                        e.printStackTrace()
                        toaster.show(
                            context.getString(R.string.backup_page_restore_failed, e.message ?: ""),
                            type = ToastType.Error
                        )
                    }
                    isRestoring = false
                }
            },
        )
    }

    if (showExportContentDialog) {
        ExportContentSelectionDialog(
            providers = settings.providers,
            onDismiss = { showExportContentDialog = false },
            onConfirm = { selectedContent, selectedKeys ->
                showExportContentDialog = false
                selectedExportContent = selectedContent
                selectedExportKeyIds = selectedKeys
                val timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                createDocumentLauncher.launch("LastChat_backup_$timestamp.zip")
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp)
    ) {
        item {
            SettingsGroup(
                title = stringResource(R.string.backup_page_local_backup_export),
                horizontalPadding = 0.dp,
                titleStartPadding = 4.dp,
            ) {
                SettingGroupItem(
                    title = stringResource(R.string.backup_page_local_backup_export),
                    subtitle = if (isExporting) {
                        stringResource(R.string.backup_page_exporting)
                    } else {
                        stringResource(R.string.backup_page_export_desc)
                    },
                    icon = {
                        if (isExporting) {
                            CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Rounded.FileUpload, null)
                        }
                    },
                    onClick = {
                        if (!isExporting) {
                            showExportContentDialog = true
                        }
                    }
                )
                SettingGroupItem(
                    title = stringResource(R.string.backup_page_local_backup_import),
                    subtitle = if (isRestoring && importType == "local") {
                        stringResource(R.string.backup_page_importing)
                    } else {
                        stringResource(R.string.backup_page_import_desc)
                    },
                    icon = {
                        if (isRestoring && importType == "local") {
                            CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Rounded.SystemUpdateAlt, null)
                        }
                    },
                    onClick = {
                        if (!isRestoring) {
                            importType = "local"
                            openDocumentLauncher.launch(arrayOf("application/zip"))
                        }
                    }
                )
            }
        }

        item {
            SettingsGroup(
                title = stringResource(R.string.backup_page_import_from_other_app),
                horizontalPadding = 0.dp,
                titleStartPadding = 4.dp,
            ) {
                SettingGroupItem(
                    title = stringResource(R.string.backup_page_import_from_chatbox),
                    subtitle = stringResource(R.string.backup_page_import_chatbox_desc),
                    icon = {
                        if (isRestoring && importType == "chatbox") {
                            CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Rounded.SystemUpdateAlt, null)
                        }
                    },
                    onClick = {
                        if (!isRestoring) {
                            importType = "chatbox"
                            openDocumentLauncher.launch(arrayOf("application/json", "text/plain"))
                        }
                    }
                )
                SettingGroupItem(
                    title = stringResource(R.string.backup_page_import_from_cherry_studio),
                    subtitle = stringResource(R.string.backup_page_import_cherry_studio_desc),
                    icon = {
                        if (isRestoring && importType == "cherry") {
                            CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Rounded.Folder, null)
                        }
                    },
                    onClick = {
                        if (!isRestoring) {
                            importType = "cherry"
                            openDocumentLauncher.launch(
                                arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")
                            )
                        }
                    }
                )
            }
        }
    }

    // Permission explanation dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                // User dismissed - proceed without permissions
                showPermissionDialog = false
                showRestartDialog = true
            },
            title = { Text(stringResource(R.string.backup_restore_permissions_required)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.backup_restore_permissions_message))
                    PermissionChecker.getFeatureAccessDescriptions(pendingFeatureAccess).forEach { description ->
                        val desc = description
                        Text("- $desc", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        stringResource(R.string.backup_restore_permissions_follow_up),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        when {
                            pendingFeatureAccess.runtimePermissions.isNotEmpty() -> {
                                permissionLauncher.launch(pendingFeatureAccess.runtimePermissions.toTypedArray())
                            }
                            pendingFeatureAccess.specialAccesses.isNotEmpty() -> {
                                val nextAccess = pendingFeatureAccess.specialAccesses.firstOrNull()
                                    ?: return@Button
                                specialAccessLauncher.launch(
                                    PermissionChecker.createSpecialAccessIntent(nextAccess)
                                )
                            }
                            else -> {
                                showRestartDialog = true
                            }
                        }
                    }
                ) {
                    Text(
                        if (pendingFeatureAccess.runtimePermissions.isNotEmpty()) {
                            stringResource(R.string.backup_restore_grant_permissions)
                        } else {
                            stringResource(R.string.open_settings)
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        showRestartDialog = true
                    }
                ) {
                    Text(stringResource(R.string.backup_restore_skip))
                }
            }
        )
    }
    
    // 重启对话框
    if (showRestartDialog) {
        val result = restoreResult // Capture immutable for checking
        BackupDialog(
             result = result,
             onConfirm = {
                 vm.restartApp(context)
             }
        )
    }
}

@Composable
private fun BackupDialog(
    result: me.rerere.rikkahub.data.sync.WebdavSync.RestoreResult?,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {}, // Disallow dismissing by clicking outside
        title = { Text(stringResource(R.string.backup_page_restart_app)) },
        text = { 
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.backup_page_restart_desc))
                
                result?.let {
                    if (it.sanitization.skippedRows > 0 || it.settingsCleanup.totalIssuesFixed > 0 || it.settingsCleanup.unsupportedZipEntriesBytes > 0) {                        Card(
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = stringResource(R.string.backup_restore_report_title),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                if (it.sanitization.skippedRows > 0) {
                                    Text(
                                        text = stringResource(
                                            R.string.backup_restore_report_removed_items,
                                            it.sanitization.skippedRows
                                        )
                                    )
                                }
                                if (it.settingsCleanup.totalIssuesFixed > 0) {
                                    Text(
                                        text = stringResource(
                                            R.string.backup_restore_report_fixed_settings,
                                            it.settingsCleanup.totalIssuesFixed
                                        )
                                    )
                                }
                                if (it.settingsCleanup.unsupportedZipEntriesBytes > 0) {
                                    Text(
                                        text = stringResource(
                                            R.string.backup_restore_report_cleaned_data,
                                            it.settingsCleanup.unsupportedZipEntriesBytes.fileSizeToString()
                                        )
                                    )
                                }
                            }
                        }
                    }
                    val envTargets = it.workspacesMetadata.filter { meta -> meta.hasRootfs }
                    if (envTargets.isNotEmpty()) {
                        Card(
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.backup_restore_workspaces_env_title),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = stringResource(
                                        R.string.backup_restore_workspaces_env_desc,
                                        envTargets.size
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text(stringResource(R.string.backup_page_restart_app))
            }
        },
    )
}
