@file:OptIn(ExperimentalLayoutApi::class)

package com.passheep.sticky_note.feature.todo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.passheep.sticky_note.core.model.RemoteDeletedResolution
import com.passheep.sticky_note.core.model.Todo
import com.passheep.sticky_note.core.repository.TodoSyncSummary

@Composable
fun TodoHomeScreen(
    uiState: TodoHomeUiState,
    snackbarHostState: SnackbarHostState,
    onFilterChange: (TodoFilter) -> Unit,
    onAddClick: () -> Unit,
    onEditTodo: (Todo) -> Unit,
    onDismissEditor: () -> Unit,
    onUpdateEditor: (TodoEditorState) -> Unit,
    onSaveEditor: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    onUpdateSettings: (SettingsSheetState) -> Unit,
    onSaveSettings: () -> Unit,
    onOpenDevicePicker: () -> Unit,
    onDismissDevicePicker: () -> Unit,
    onRefreshDevices: () -> Unit,
    onSelectDevice: (String?) -> Unit,
    onToggleTodo: (String) -> Unit,
    onDeleteTodo: (String) -> Unit,
    onResolveRemoteDeleted: (String, RemoteDeletedResolution) -> Unit,
    onRequestSync: () -> Unit,
) {
    uiState.editor?.let { editor ->
        TodoEditorSheet(
            state = editor,
            isSaving = uiState.isSaving,
            onDismiss = onDismissEditor,
            onStateChange = onUpdateEditor,
            onSave = onSaveEditor,
        )
    }
    uiState.settings?.let { settings ->
        SettingsSheet(
            state = settings,
            isSaving = uiState.isSaving,
            onDismiss = onDismissSettings,
            onStateChange = onUpdateSettings,
            onOpenDevicePicker = onOpenDevicePicker,
            onSave = onSaveSettings,
        )
    }
    uiState.devicePicker?.let { picker ->
        DevicePickerSheet(
            state = picker,
            onDismiss = onDismissDevicePicker,
            onRefresh = onRefreshDevices,
            onSelectDevice = onSelectDevice,
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier.pressScale(),
                shape = RoundedCornerShape(22.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "新建待办")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                )
                .padding(innerPadding),
        ) {
            BackdropGlow()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                TodoHeroHeader(
                    allCount = uiState.allCount,
                    activeCount = uiState.activeCount,
                    completedCount = uiState.completedCount,
                    hasRequiredSyncConfig = uiState.hasRequiredSyncConfig,
                    selectedDeviceLabel = uiState.selectedDeviceLabel,
                    deviceCount = uiState.deviceCount,
                    onRequestSync = onRequestSync,
                    onOpenSettings = onOpenSettings,
                    onOpenDevicePicker = onOpenDevicePicker,
                )
                HomeAlertSection(
                    hasRequiredSyncConfig = uiState.hasRequiredSyncConfig,
                    selectedDeviceLabel = uiState.selectedDeviceLabel,
                    deviceCount = uiState.deviceCount,
                    syncSummary = uiState.syncSummary,
                    onOpenSettings = onOpenSettings,
                    onOpenDevicePicker = onOpenDevicePicker,
                    onRequestSync = onRequestSync,
                )
                SyncSummaryBar(uiState.syncSummary)
                FilterRow(
                    selected = uiState.filter,
                    allCount = uiState.allCount,
                    activeCount = uiState.activeCount,
                    completedCount = uiState.completedCount,
                    onSelected = onFilterChange,
                )
                Crossfade(
                    targetState = uiState.visibleTodos.isEmpty(),
                    animationSpec = tween(180),
                    label = "todoListCrossfade",
                ) { empty ->
                    if (empty) {
                        TodoEmptyState(
                            modifier = Modifier.fillMaxSize(),
                            hasRequiredSyncConfig = uiState.hasRequiredSyncConfig,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 16.dp),
                            contentPadding = PaddingValues(bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(uiState.visibleTodos, key = { it.localId }) { todo ->
                                TodoCard(
                                    todo = todo,
                                    onEdit = { onEditTodo(todo) },
                                    onToggle = { onToggleTodo(todo.localId) },
                                    onDelete = { onDeleteTodo(todo.localId) },
                                    onResolveRemoteDeleted = { resolution ->
                                        onResolveRemoteDeleted(todo.localId, resolution)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackdropGlow() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x334285F4), Color.Transparent),
            ),
            radius = size.minDimension * 0.45f,
            center = Offset(size.width * 0.18f, size.height * 0.12f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x269B51E0), Color.Transparent),
            ),
            radius = size.minDimension * 0.52f,
            center = Offset(size.width * 0.82f, size.height * 0.2f),
        )
    }
}

@Composable
private fun TodoHeroHeader(
    allCount: Int,
    activeCount: Int,
    completedCount: Int,
    hasRequiredSyncConfig: Boolean,
    selectedDeviceLabel: String?,
    deviceCount: Int,
    onRequestSync: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDevicePicker: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "智能便利贴",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (hasRequiredSyncConfig) {
                            "先写本地，再把变更慢慢同步到云端和设备。"
                        } else {
                            "先记录也没关系，补齐平台地址和 API Key 后再同步。"
                        },
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(
                        onClick = onRequestSync,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                            .pressScale(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "立即同步",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f))
                            .pressScale(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "打开设置",
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            DeviceSummaryBar(
                selectedDeviceLabel = selectedDeviceLabel,
                deviceCount = deviceCount,
                onOpenDevicePicker = onOpenDevicePicker,
            )
            FlowRow(
                modifier = Modifier.padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HeaderStat("全部", allCount.toString())
                HeaderStat("未完成", activeCount.toString())
                HeaderStat("已完成", completedCount.toString())
            }
        }
    }
}

@Composable
private fun DeviceSummaryBar(
    selectedDeviceLabel: String?,
    deviceCount: Int,
    onOpenDevicePicker: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .pressScale(),
        onClick = onOpenDevicePicker,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Devices,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            ) {
                Text(
                    text = selectedDeviceLabel ?: "还没有选择默认设备",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (deviceCount > 0) {
                        "已同步到 $deviceCount 台设备，点这里切换默认设备"
                    } else {
                        "点这里打开设备选择页并同步设备列表"
                    },
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "管理",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun HeaderStat(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun HomeAlertSection(
    hasRequiredSyncConfig: Boolean,
    selectedDeviceLabel: String?,
    deviceCount: Int,
    syncSummary: TodoSyncSummary,
    onOpenSettings: () -> Unit,
    onOpenDevicePicker: () -> Unit,
    onRequestSync: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedVisibility(visible = !hasRequiredSyncConfig) {
            AlertCard(
                title = "还差一步就能同步",
                message = "请先在设置里填写平台 URL 和 API Key。没配置前，本地待办仍然可以正常新增和编辑。",
                accent = MaterialTheme.colorScheme.primary,
                primaryLabel = "去设置",
                onPrimaryClick = onOpenSettings,
            )
        }

        AnimatedVisibility(visible = hasRequiredSyncConfig && deviceCount == 0) {
            AlertCard(
                title = "还没有设备列表",
                message = "同步成功后会从云端拉取设备。你也可以直接打开设备选择页，在里面点“同步设备”。",
                accent = MaterialTheme.colorScheme.secondary,
                primaryLabel = "选设备",
                onPrimaryClick = onOpenDevicePicker,
                secondaryLabel = "立即同步",
                onSecondaryClick = onRequestSync,
            )
        }

        AnimatedVisibility(visible = hasRequiredSyncConfig && deviceCount > 0 && selectedDeviceLabel == null) {
            AlertCard(
                title = "还没有默认设备",
                message = "现在已经同步到设备列表了，建议先选一个默认设备，后续待办推送会更顺手。",
                accent = MaterialTheme.colorScheme.secondary,
                primaryLabel = "去选择",
                onPrimaryClick = onOpenDevicePicker,
            )
        }

        AnimatedVisibility(visible = syncSummary.failedCount > 0) {
            AlertCard(
                title = "有待办同步失败",
                message = "当前有 ${syncSummary.failedCount} 条待办同步失败。请检查网络或配置，然后点击立即同步重试。",
                accent = Color(0xFFEE6B5A),
                primaryLabel = "立即同步",
                onPrimaryClick = onRequestSync,
                secondaryLabel = "去设置",
                onSecondaryClick = onOpenSettings,
            )
        }

        AnimatedVisibility(visible = syncSummary.conflictCount > 0) {
            AlertCard(
                title = "发现云端删除冲突",
                message = "带黄色标记的待办表示云端已删除、本地仍保留。点击卡片中的标记，可选择删除本地或恢复到云端。",
                accent = Color(0xFFF2A94A),
            )
        }
    }
}

@Composable
private fun AlertCard(
    title: String,
    message: String,
    accent: Color,
    primaryLabel: String? = null,
    onPrimaryClick: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = accent.copy(alpha = 0.11f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = message,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (primaryLabel != null && onPrimaryClick != null) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Button(
                        onClick = onPrimaryClick,
                        modifier = Modifier.pressScale(),
                    ) {
                        Text(primaryLabel)
                    }
                    if (secondaryLabel != null && onSecondaryClick != null) {
                        TextButton(
                            onClick = onSecondaryClick,
                            modifier = Modifier.pressScale(),
                        ) {
                            Text(secondaryLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncSummaryBar(summary: TodoSyncSummary) {
    AnimatedVisibility(
        visible = summary.pendingCount > 0 || summary.failedCount > 0 || summary.conflictCount > 0,
    ) {
        val accent = when {
            summary.failedCount > 0 -> Color(0xFFEE6B5A)
            summary.conflictCount > 0 -> Color(0xFFF2A94A)
            else -> MaterialTheme.colorScheme.secondary
        }
        val message = buildString {
            if (summary.pendingCount > 0) append("同步中 ${summary.pendingCount} 条")
            if (summary.failedCount > 0) {
                if (isNotEmpty()) append("，")
                append("失败 ${summary.failedCount} 条")
            }
            if (summary.conflictCount > 0) {
                if (isNotEmpty()) append("，")
                append("冲突 ${summary.conflictCount} 条")
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            shape = RoundedCornerShape(20.dp),
            color = accent.copy(alpha = 0.10f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accent,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    selected: TodoFilter,
    allCount: Int,
    activeCount: Int,
    completedCount: Int,
    onSelected: (TodoFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilterChip(
            selected = selected == TodoFilter.ALL,
            onClick = { onSelected(TodoFilter.ALL) },
            label = { Text("全部 $allCount") },
            modifier = Modifier.pressScale(),
        )
        FilterChip(
            selected = selected == TodoFilter.ACTIVE,
            onClick = { onSelected(TodoFilter.ACTIVE) },
            label = { Text("未完成 $activeCount") },
            modifier = Modifier.pressScale(),
        )
        FilterChip(
            selected = selected == TodoFilter.COMPLETED,
            onClick = { onSelected(TodoFilter.COMPLETED) },
            label = { Text("已完成 $completedCount") },
            modifier = Modifier.pressScale(),
        )
    }
}

@Composable
private fun TodoEmptyState(
    modifier: Modifier = Modifier,
    hasRequiredSyncConfig: Boolean,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp),
                )
                Text(
                    text = "还没有待办",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (hasRequiredSyncConfig) {
                        "点右下角加号，新建一条会先写进本地，然后由后台继续同步。"
                    } else {
                        "点右下角加号先记录，等设置好平台地址和 API Key 后再同步到云端。"
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
