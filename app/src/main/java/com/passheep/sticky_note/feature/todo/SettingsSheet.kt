@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.passheep.sticky_note.feature.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.passheep.sticky_note.core.settings.DEFAULT_PLATFORM_URL
import com.passheep.sticky_note.core.settings.PushType
import com.passheep.sticky_note.core.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    state: SettingsSheetState,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onStateChange: (SettingsSheetState) -> Unit,
    onOpenDevicePicker: () -> Unit,
    onSave: () -> Unit,
) {
    val hasApiKey = state.apiKey.trim().isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "设置",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "URL 可以单独保存；填好 API Key 后，应用会自动拉取设备和云端待办。",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!hasApiKey) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                ) {
                    Text(
                        text = "当前还没填写 API Key。现在保存会先保存设置，不会立即同步。",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            OutlinedTextField(
                value = state.platformUrl,
                onValueChange = { onStateChange(state.copy(platformUrl = it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                label = { Text("平台 URL") },
                placeholder = { Text(DEFAULT_PLATFORM_URL) },
                supportingText = { Text("留空时会自动使用默认地址 $DEFAULT_PLATFORM_URL") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.apiKey,
                onValueChange = { onStateChange(state.copy(apiKey = it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                label = { Text("API Key") },
                supportingText = {
                    Text(
                        if (hasApiKey) {
                            "保存后会立即请求一次同步"
                        } else {
                            "可稍后再填，先保存 URL 也可以"
                        },
                    )
                },
                singleLine = true,
            )

            SheetTitle("同步间隔")
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(1, 10, 30, 60).forEach { interval ->
                    FilterChip(
                        selected = state.syncIntervalMinutes == interval,
                        onClick = { onStateChange(state.copy(syncIntervalMinutes = interval)) },
                        label = { Text("${interval}分钟") },
                        modifier = Modifier.pressScale(),
                    )
                }
            }

            SheetTitle("主题")
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.themeMode == mode,
                        onClick = { onStateChange(state.copy(themeMode = mode)) },
                        label = { Text(mode.label()) },
                        modifier = Modifier.pressScale(),
                    )
                }
            }

            SheetTitle("设备推送")
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PushType.entries.forEach { type ->
                    FilterChip(
                        selected = state.defaultPushType == type,
                        onClick = { onStateChange(state.copy(defaultPushType = type)) },
                        label = { Text(type.label()) },
                        modifier = Modifier.pressScale(),
                    )
                }
            }

            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (1..5).forEach { pageId ->
                    FilterChip(
                        selected = state.defaultPushPageId == pageId,
                        onClick = { onStateChange(state.copy(defaultPushPageId = pageId)) },
                        label = { Text("第 ${pageId} 页") },
                        modifier = Modifier.pressScale(),
                    )
                }
            }

            SheetTitle("选择设备")
            Button(
                onClick = onOpenDevicePicker,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .pressScale(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            ) {
                Text(if (state.devices.isEmpty()) "打开设备选择页" else "管理默认设备")
            }
            if (state.devices.isEmpty()) {
                Text(
                    text = if (hasApiKey) {
                        "当前还没有设备数据。保存后点立即同步，即可拉取设备列表。"
                    } else {
                        "先保存 URL，需要时再补 API Key；填完 API Key 后即可同步设备列表。"
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.devices.forEach { device ->
                        FilterChip(
                            selected = state.selectedDeviceId == device.deviceId,
                            onClick = {
                                onStateChange(
                                    state.copy(
                                        selectedDeviceId = if (state.selectedDeviceId == device.deviceId) {
                                            null
                                        } else {
                                            device.deviceId
                                        },
                                    ),
                                )
                            },
                            label = { Text(device.alias) },
                            modifier = Modifier.pressScale(),
                        )
                    }
                }
            }

            SheetTitle("小组件透明度")
            Text(
                text = "当前 ${(state.widgetTransparency * 100).toInt()}%",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = state.widgetTransparency,
                onValueChange = { onStateChange(state.copy(widgetTransparency = it)) },
                valueRange = 0f..0.9f,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = onSave,
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .pressScale(),
                ) {
                    Text(
                        when {
                            isSaving -> "保存中..."
                            hasApiKey -> "保存并同步"
                            else -> "保存设置"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 16.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}

private fun PushType.label(): String = when (this) {
    PushType.TEXT -> "文本"
    PushType.STRUCTURED_TEXT -> "标题正文"
    PushType.IMAGE -> "图片"
}
