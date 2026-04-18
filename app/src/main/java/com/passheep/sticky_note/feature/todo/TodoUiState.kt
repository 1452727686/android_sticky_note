package com.passheep.sticky_note.feature.todo

import com.passheep.sticky_note.core.model.RepeatType
import com.passheep.sticky_note.core.model.Todo
import com.passheep.sticky_note.core.settings.PushType
import com.passheep.sticky_note.core.repository.TodoSyncSummary
import com.passheep.sticky_note.core.settings.ThemeMode

data class TodoHomeUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val filter: TodoFilter = TodoFilter.ALL,
    val visibleTodos: List<Todo> = emptyList(),
    val allCount: Int = 0,
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    val hasRequiredSyncConfig: Boolean = false,
    val selectedDeviceLabel: String? = null,
    val deviceCount: Int = 0,
    val syncSummary: TodoSyncSummary = TodoSyncSummary(),
    val editor: TodoEditorState? = null,
    val settings: SettingsSheetState? = null,
    val devicePicker: DevicePickerState? = null,
    val isSaving: Boolean = false,
)

enum class TodoFilter {
    ALL,
    ACTIVE,
    COMPLETED,
}

data class TodoEditorState(
    val localId: String? = null,
    val title: String = "",
    val description: String = "",
    val dueDateText: String = "",
    val dueTimeText: String = "",
    val repeatType: RepeatType = RepeatType.NONE,
    val repeatWeekday: Int? = null,
    val repeatMonthText: String = "",
    val repeatDayText: String = "",
    val priority: Int = 0,
    val deviceId: String? = null,
) {
    val isEditing: Boolean get() = localId != null
}

data class SettingsSheetState(
    val platformUrl: String = "",
    val apiKey: String = "",
    val syncIntervalMinutes: Int = 10,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val widgetTransparency: Float = 0.18f,
    val defaultPushType: PushType = PushType.STRUCTURED_TEXT,
    val defaultPushPageId: Int = 1,
    val selectedDeviceId: String? = null,
    val devices: List<DeviceOption> = emptyList(),
)

data class DeviceOption(
    val deviceId: String,
    val alias: String,
    val board: String = "",
)

data class DevicePickerState(
    val selectedDeviceId: String? = null,
    val devices: List<DeviceOption> = emptyList(),
    val hasRequiredSyncConfig: Boolean = false,
)
