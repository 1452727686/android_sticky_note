package com.passheep.sticky_note.data.cloud

import android.util.Log
import com.passheep.sticky_note.core.model.ConflictState
import com.passheep.sticky_note.core.model.Device
import com.passheep.sticky_note.core.model.RepeatType
import com.passheep.sticky_note.core.model.SyncState
import com.passheep.sticky_note.core.model.Todo
import com.passheep.sticky_note.core.model.TodoDraftInput
import com.passheep.sticky_note.core.model.TodoStatus
import com.passheep.sticky_note.core.settings.SettingsRepository
import com.passheep.sticky_note.data.network.ensureSuccess
import com.passheep.sticky_note.data.network.requireData
import com.passheep.sticky_note.data.remote.api.DeviceApiService
import com.passheep.sticky_note.data.remote.api.TodoApiService
import com.passheep.sticky_note.data.remote.dto.CreateTodoRequest
import com.passheep.sticky_note.data.remote.dto.RemoteTodoDto
import com.passheep.sticky_note.data.remote.dto.UpdateTodoRequest
import com.passheep.sticky_note.data.remote.toEpochMillis
import com.passheep.sticky_note.widget.WidgetUpdater
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class CloudTodoStore @Inject constructor(
    private val todoApiService: TodoApiService,
    private val deviceApiService: DeviceApiService,
    private val settingsRepository: SettingsRepository,
    private val widgetUpdater: WidgetUpdater,
) {
    private val todoMutex = Mutex()
    private val deviceMutex = Mutex()

    private val mutableTodos = MutableStateFlow<List<Todo>>(emptyList())
    val todos: StateFlow<List<Todo>> = mutableTodos.asStateFlow()

    private val mutableDevices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = mutableDevices.asStateFlow()

    suspend fun clearCloudState() {
        mutableTodos.value = emptyList()
        mutableDevices.value = emptyList()
        widgetUpdater.refreshAll()
    }

    suspend fun refreshTodos(
        reason: String,
        clearOnFailure: Boolean,
    ): List<Todo> = todoMutex.withLock {
        val settings = settingsRepository.settings.first()
        ensureCloudReady(settings.platformEnabled, settings.platformUrl, settings.apiKey)
        return runCatching {
            Log.d(TAG, "refreshTodos reason=$reason deviceId=${settings.selectedDeviceId}")
            val todos = fetchTodosFromApi(settings.selectedDeviceId)
            val snapshotAt = System.currentTimeMillis()
            mutableTodos.value = todos
            settingsRepository.updateLastSyncAtMillis(snapshotAt)
            widgetUpdater.refreshAll()
            Log.d(TAG, "refreshTodos success reason=$reason count=${todos.size}")
            todos
        }.getOrElse { error ->
            Log.e(TAG, "refreshTodos failed reason=$reason", error)
            if (clearOnFailure) {
                mutableTodos.value = emptyList()
                widgetUpdater.refreshAll()
            }
            throw error
        }
    }

    suspend fun refreshDevices(
        reason: String,
        clearOnFailure: Boolean,
    ): List<Device> = deviceMutex.withLock {
        val settings = settingsRepository.settings.first()
        ensureCloudReady(settings.platformEnabled, settings.platformUrl, settings.apiKey)
        return runCatching {
            Log.d(TAG, "refreshDevices reason=$reason")
            val now = System.currentTimeMillis()
            val devices = deviceApiService.getDevices().requireData()
                .map { dto ->
                    Device(
                        deviceId = dto.deviceId,
                        alias = dto.alias.orEmpty().ifBlank { dto.deviceId },
                        board = dto.board.orEmpty(),
                        isSelected = dto.deviceId == settings.selectedDeviceId,
                        syncedAt = now,
                    )
                }
            mutableDevices.value = devices
            Log.d(TAG, "refreshDevices success reason=$reason count=${devices.size}")
            devices
        }.getOrElse { error ->
            Log.e(TAG, "refreshDevices failed reason=$reason", error)
            if (clearOnFailure) {
                mutableDevices.value = emptyList()
            }
            throw error
        }
    }

    suspend fun createTodo(input: TodoDraftInput): Todo = todoMutex.withLock {
        val settings = settingsRepository.settings.first()
        ensureCloudReady(settings.platformEnabled, settings.platformUrl, settings.apiKey)
        val request = input.toCreateRequest(settings.selectedDeviceId)
        Log.d(TAG, "createTodo title=${request.title} deviceId=${request.deviceId}")
        val created = todoApiService.createTodo(request).requireData().toCloudTodo()
        refreshTodosLocked(reason = "create_after_mutation", settingsDeviceId = settings.selectedDeviceId)
        created
    }

    suspend fun updateTodo(remoteId: Long, input: TodoDraftInput): Todo = todoMutex.withLock {
        val settings = settingsRepository.settings.first()
        ensureCloudReady(settings.platformEnabled, settings.platformUrl, settings.apiKey)
        val existing = fetchTodosFromApi(settings.selectedDeviceId).firstOrNull { it.remoteId == remoteId }
        if (existing.shouldRecreateForClearedSchedule(input)) {
            Log.d(TAG, "updateTodo recreate_for_schedule_clear remoteId=$remoteId")
            return recreateTodoForClearedSchedule(
                remoteId = remoteId,
                input = input,
                settingsDeviceId = settings.selectedDeviceId,
            )
        }
        val request = input.toUpdateRequest(settings.selectedDeviceId)
        Log.d(TAG, "updateTodo remoteId=$remoteId")
        val updated = todoApiService.updateTodo(remoteId, request).requireData().toCloudTodo()
        refreshTodosLocked(reason = "update_after_mutation", settingsDeviceId = settings.selectedDeviceId)
        updated
    }

    suspend fun toggleCompleted(remoteId: Long) = todoMutex.withLock {
        val settings = settingsRepository.settings.first()
        ensureCloudReady(settings.platformEnabled, settings.platformUrl, settings.apiKey)
        val currentCompleted = fetchTodosFromApi(settings.selectedDeviceId)
            .firstOrNull { it.remoteId == remoteId }
            ?.completed
        val expectedCompleted = currentCompleted?.not()
        Log.d(TAG, "toggleCompleted remoteId=$remoteId")
        todoApiService.toggleComplete(remoteId).ensureSuccess()
        refreshTodosLockedWithConsistency(
            reason = "toggle_after_mutation",
            settingsDeviceId = settings.selectedDeviceId,
            expectedRemoteId = remoteId,
            expectedCompleted = expectedCompleted,
        )
    }

    suspend fun deleteTodo(remoteId: Long) = todoMutex.withLock {
        val settings = settingsRepository.settings.first()
        ensureCloudReady(settings.platformEnabled, settings.platformUrl, settings.apiKey)
        Log.d(TAG, "deleteTodo remoteId=$remoteId")
        todoApiService.deleteTodo(remoteId).ensureSuccess()
        refreshTodosLocked(reason = "delete_after_mutation", settingsDeviceId = settings.selectedDeviceId)
    }

    suspend fun snapshotTodos(limit: Int): List<Todo> {
        val settings = settingsRepository.settings.first()
        if (!settings.platformEnabled || settings.apiKey.isBlank()) {
            Log.d(
                TAG,
                "snapshotTodos skipped enabled=${settings.platformEnabled} apiKeyBlank=${settings.apiKey.isBlank()}",
            )
            return emptyList()
        }
        return runCatching {
            Log.d(TAG, "snapshotTodos fetch deviceId=${settings.selectedDeviceId}")
            fetchTodosFromApi(settings.selectedDeviceId)
                .also {
                    val snapshotAt = System.currentTimeMillis()
                    mutableTodos.value = it
                    settingsRepository.updateLastSyncAtMillis(snapshotAt)
                    Log.d(TAG, "snapshotTodos success count=${it.size}")
                }
        }.getOrElse { error ->
            Log.e(TAG, "snapshotTodos failed", error)
            emptyList()
        }.take(limit)
    }

    private suspend fun refreshTodosLocked(reason: String, settingsDeviceId: String?): List<Todo> {
        val todos = fetchTodosFromApi(settingsDeviceId)
        val snapshotAt = System.currentTimeMillis()
        mutableTodos.value = todos
        settingsRepository.updateLastSyncAtMillis(snapshotAt)
        widgetUpdater.refreshAll()
        Log.d(TAG, "refreshTodosLocked success reason=$reason count=${todos.size}")
        return todos
    }

    private suspend fun fetchTodosFromApi(settingsDeviceId: String?): List<Todo> =
        todoApiService.getTodos(deviceId = settingsDeviceId).requireData()
            .map { it.toCloudTodo() }

    private suspend fun refreshTodosLockedWithConsistency(
        reason: String,
        settingsDeviceId: String?,
        expectedRemoteId: Long?,
        expectedCompleted: Boolean?,
    ): List<Todo> {
        var latest = refreshTodosLocked(reason = reason, settingsDeviceId = settingsDeviceId)
        if (expectedRemoteId == null || expectedCompleted == null) {
            return latest
        }
        repeat(3) { attempt ->
            val completed = latest.firstOrNull { it.remoteId == expectedRemoteId }?.completed
            if (completed == expectedCompleted) {
                return latest
            }
            delay(900L * (attempt + 1))
            latest = refreshTodosLocked(
                reason = "${reason}_retry_$attempt",
                settingsDeviceId = settingsDeviceId,
            )
        }
        return latest
    }

    private suspend fun recreateTodoForClearedSchedule(
        remoteId: Long,
        input: TodoDraftInput,
        settingsDeviceId: String?,
    ): Todo {
        val createRequest = input.toCreateRequest(settingsDeviceId)
        val created = todoApiService.createTodo(createRequest).requireData().toCloudTodo()
        runCatching {
            todoApiService.deleteTodo(remoteId).ensureSuccess()
        }.onFailure { error ->
            Log.e(
                TAG,
                "recreateTodoForClearedSchedule delete_old failed remoteId=$remoteId newRemoteId=${created.remoteId}",
                error,
            )
            created.remoteId?.let { createdRemoteId ->
                runCatching { todoApiService.deleteTodo(createdRemoteId).ensureSuccess() }
                    .onFailure { rollbackError ->
                        Log.e(
                            TAG,
                            "recreateTodoForClearedSchedule rollback failed newRemoteId=$createdRemoteId",
                            rollbackError,
                        )
                    }
            }
            throw error
        }
        refreshTodosLocked(
            reason = "recreate_after_schedule_clear",
            settingsDeviceId = settingsDeviceId,
        )
        return created
    }

    private fun ensureCloudReady(platformEnabled: Boolean, platformUrl: String, apiKey: String) {
        require(platformEnabled) { "请先开启接入平台" }
        require(platformUrl.isNotBlank()) { "请先填写平台地址" }
        require(apiKey.isNotBlank()) { "请先填写 API 密钥" }
    }

    private fun RemoteTodoDto.toCloudTodo(): Todo {
        val statusModel = if (completed || status == 1) TodoStatus.COMPLETED else TodoStatus.TODO
        val remoteUpdatedAtMillis = updateDate.toEpochMillis() ?: System.currentTimeMillis()
        return Todo(
            localId = "remote-$id",
            remoteId = id,
            title = title,
            description = description.orEmpty(),
            dueDate = dueDate?.takeIf { it.isNotBlank() }?.let(java.time.LocalDate::parse),
            dueTime = dueTime?.takeIf { it.isNotBlank() }?.let(java.time.LocalTime::parse),
            repeatType = repeatType.orEmpty().toRepeatType(),
            repeatWeekday = repeatWeekday,
            repeatMonth = repeatMonth,
            repeatDay = repeatDay,
            priority = priority,
            status = statusModel,
            completed = statusModel == TodoStatus.COMPLETED,
            deviceId = deviceId,
            deviceName = deviceName,
            syncState = SyncState.SYNCED,
            pendingAction = null,
            conflictState = ConflictState.NONE,
            localUpdatedAt = remoteUpdatedAtMillis,
            remoteUpdatedAt = remoteUpdatedAtMillis,
            createdAt = remoteUpdatedAtMillis,
            remoteCreateDate = createDate,
            isDeletedLocally = false,
            isDeletedRemotely = false,
            lastSyncError = null,
        )
    }

    private fun String.toRepeatType(): RepeatType = when (lowercase()) {
        "daily" -> RepeatType.DAILY
        "weekly" -> RepeatType.WEEKLY
        "monthly" -> RepeatType.MONTHLY
        "yearly" -> RepeatType.YEARLY
        else -> RepeatType.NONE
    }

    private fun RepeatType.toApiValue(): String = when (this) {
        RepeatType.NONE -> "none"
        RepeatType.DAILY -> "daily"
        RepeatType.WEEKLY -> "weekly"
        RepeatType.MONTHLY -> "monthly"
        RepeatType.YEARLY -> "yearly"
    }

    private fun RepeatType?.toApiValueOrEmpty(): String = this?.toApiValue().orEmpty()

    private fun TodoDraftInput.toCreateRequest(settingsDeviceId: String?): CreateTodoRequest = CreateTodoRequest(
        title = title.trim(),
        description = description.trim(),
        dueDate = dueDate?.toString().orEmpty(),
        dueTime = dueTime?.toString().orEmpty(),
        repeatType = repeatType.toApiValueOrEmpty(),
        repeatWeekday = toApiRepeatWeekday(),
        repeatMonth = toApiRepeatMonth(),
        repeatDay = toApiRepeatDay(),
        priority = priority.coerceIn(0, 2),
        deviceId = deviceId ?: settingsDeviceId.orEmpty(),
    )

    private fun TodoDraftInput.toUpdateRequest(settingsDeviceId: String?): UpdateTodoRequest = UpdateTodoRequest(
        title = title.trim(),
        description = description.trim(),
        dueDate = dueDate?.toString().orEmpty(),
        dueTime = dueTime?.toString().orEmpty(),
        repeatType = repeatType.toApiValueOrEmpty(),
        repeatWeekday = toApiRepeatWeekday(),
        repeatMonth = toApiRepeatMonth(),
        repeatDay = toApiRepeatDay(),
        priority = priority.coerceIn(0, 2),
        deviceId = deviceId ?: settingsDeviceId.orEmpty(),
    )

    private fun Todo?.shouldRecreateForClearedSchedule(input: TodoDraftInput): Boolean {
        if (this == null) return false
        val clearedDate = dueDate != null && input.dueDate == null
        val clearedTime = dueTime != null && input.dueTime == null
        return clearedDate || clearedTime
    }

    private fun TodoDraftInput.toApiRepeatWeekday(): String =
        if (repeatType == RepeatType.WEEKLY) (repeatWeekday?.toString().orEmpty()) else ""

    private fun TodoDraftInput.toApiRepeatMonth(): String =
        if (repeatType == RepeatType.YEARLY) (repeatMonth?.toString().orEmpty()) else ""

    private fun TodoDraftInput.toApiRepeatDay(): String = when (repeatType) {
        RepeatType.MONTHLY, RepeatType.YEARLY -> repeatDay?.toString().orEmpty()
        else -> ""
    }

    private companion object {
        const val TAG = "StickyNoteCloud"
    }
}

