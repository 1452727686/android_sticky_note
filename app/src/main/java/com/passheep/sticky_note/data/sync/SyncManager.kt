package com.passheep.sticky_note.data.sync

import android.util.Log
import com.passheep.sticky_note.core.model.ConflictState
import com.passheep.sticky_note.core.model.QueueStatus
import com.passheep.sticky_note.core.model.SyncAction
import com.passheep.sticky_note.core.model.SyncEntityType
import com.passheep.sticky_note.core.model.SyncState
import com.passheep.sticky_note.core.model.Todo
import com.passheep.sticky_note.core.model.TodoStatus
import com.passheep.sticky_note.core.repository.DevicePushRepository
import com.passheep.sticky_note.core.repository.SyncQueueRepository
import com.passheep.sticky_note.core.settings.SettingsRepository
import com.passheep.sticky_note.data.local.dao.DeviceDao
import com.passheep.sticky_note.data.local.dao.TodoDao
import com.passheep.sticky_note.data.local.entity.toEntity
import com.passheep.sticky_note.data.local.entity.toModel
import com.passheep.sticky_note.data.network.ensureSuccess
import com.passheep.sticky_note.data.network.requireData
import com.passheep.sticky_note.data.remote.api.DeviceApiService
import com.passheep.sticky_note.data.remote.api.TodoApiService
import com.passheep.sticky_note.data.remote.toCreateRequest
import com.passheep.sticky_note.data.remote.toEpochMillis
import com.passheep.sticky_note.data.remote.toModel
import com.passheep.sticky_note.data.remote.toUpdateRequest
import com.passheep.sticky_note.widget.WidgetUpdater
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class SyncManager @Inject constructor(
    private val todoDao: TodoDao,
    private val deviceDao: DeviceDao,
    private val queueRepository: SyncQueueRepository,
    private val devicePushRepository: DevicePushRepository,
    private val todoApiService: TodoApiService,
    private val deviceApiService: DeviceApiService,
    private val settingsRepository: SettingsRepository,
    private val widgetUpdater: WidgetUpdater,
) {
    private val syncMutex = Mutex()

    suspend fun performFullSync(reason: String): SyncRunResult = syncMutex.withLock {
        val settings = settingsRepository.settings.first()
        Log.d(
            TAG,
            "performFullSync start reason=$reason baseUrl=${settings.platformUrl} apiKeyPresent=${settings.apiKey.isNotBlank()} selectedDeviceId=${settings.selectedDeviceId}",
        )
        if (settings.platformUrl.isBlank() || settings.apiKey.isBlank()) {
            Log.w(TAG, "performFullSync skipped because platformUrl/apiKey missing")
            return SyncRunResult.Skipped("未配置平台地址或 API Key")
        }

        return try {
            syncDevices(settings.selectedDeviceId)
            pullRemoteTodos(settings.selectedDeviceId)
            processQueue()
            queueRepository.clearCompleted()
            widgetUpdater.refreshAll()
            pushLatestTodosToDevice(settings.selectedDeviceId)
            Log.d(TAG, "performFullSync success reason=$reason")
            SyncRunResult.Success(reason)
        } catch (error: Throwable) {
            Log.e(TAG, "performFullSync failure reason=$reason", error)
            SyncRunResult.Failure(reason, error)
        }
    }

    suspend fun enqueueMutation(
        localId: String,
        action: SyncAction,
        payload: String? = null,
    ) {
        val existing = queueRepository.findOpenItem(
            entityType = SyncEntityType.TODO,
            entityLocalId = localId,
            action = action,
        )
        if (existing != null) {
            Log.d(TAG, "enqueueMutation skipped localId=$localId action=$action because queue item already exists")
            return
        }

        val now = System.currentTimeMillis()
        Log.d(TAG, "enqueueMutation localId=$localId action=$action")
        queueRepository.enqueue(
            com.passheep.sticky_note.core.model.SyncQueueItem(
                id = 0,
                entityType = SyncEntityType.TODO,
                entityLocalId = localId,
                action = action,
                payload = payload,
                queueStatus = QueueStatus.PENDING,
                attemptCount = 0,
                nextRetryAt = null,
                lastError = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    private suspend fun syncDevices(selectedDeviceId: String?) {
        val now = System.currentTimeMillis()
        Log.d(TAG, "syncDevices start selectedDeviceId=$selectedDeviceId")
        val devices = deviceApiService.getDevices().requireData()
            .map { it.toModel(selectedDeviceId = selectedDeviceId, now = now).toEntity() }
        Log.d(TAG, "syncDevices success count=${devices.size}")
        deviceDao.clearAll()
        if (devices.isNotEmpty()) {
            deviceDao.upsertDevices(devices)
        }
    }

    private suspend fun processQueue() {
        val queueItems = queueRepository.getExecutableItems(now = System.currentTimeMillis())
        Log.d(TAG, "processQueue items=${queueItems.size}")
        queueItems.forEach { item ->
            queueRepository.updateState(
                id = item.id,
                status = QueueStatus.RUNNING,
                attemptCount = item.attemptCount,
                nextRetryAt = null,
                lastError = null,
                updatedAt = System.currentTimeMillis(),
            )
            try {
                when (item.entityType) {
                    SyncEntityType.TODO -> processTodoMutation(item)
                    SyncEntityType.DEVICE -> queueRepository.deleteById(item.id)
                }
            } catch (error: Throwable) {
                Log.e(
                    TAG,
                    "processQueue item failed id=${item.id} action=${item.action} localId=${item.entityLocalId}",
                    error,
                )
                val attempts = item.attemptCount + 1
                queueRepository.updateState(
                    id = item.id,
                    status = QueueStatus.FAILED,
                    attemptCount = attempts,
                    nextRetryAt = calculateNextRetryAt(attempts),
                    lastError = error.message,
                    updatedAt = System.currentTimeMillis(),
                )
                todoDao.updateSyncState(
                    localId = item.entityLocalId,
                    syncState = SyncState.FAILED,
                    error = error.message,
                )
            }
        }
    }

    private suspend fun processTodoMutation(item: com.passheep.sticky_note.core.model.SyncQueueItem) {
        val entity = todoDao.getByLocalId(item.entityLocalId)
        if (entity == null) {
            Log.d(TAG, "processTodoMutation remove missing entity queueId=${item.id} localId=${item.entityLocalId}")
            queueRepository.deleteById(item.id)
            return
        }
        val todo = entity.toModel()
        when (item.action) {
            SyncAction.CREATE -> syncCreate(item.id, todo)
            SyncAction.UPDATE -> syncUpdate(item.id, todo)
            SyncAction.DELETE -> syncDelete(item.id, todo)
            SyncAction.TOGGLE_COMPLETE -> syncToggleComplete(item.id, todo)
            SyncAction.PUSH_DEVICE_CONTENT,
            SyncAction.PULL_REMOTE,
            -> queueRepository.deleteById(item.id)
        }
    }

    private suspend fun syncCreate(queueId: Long, todo: Todo) {
        Log.d(TAG, "syncCreate queueId=$queueId localId=${todo.localId} title=${todo.title}")
        val remoteTodo = todoApiService.createTodo(todo.toCreateRequest()).requireData()
        val syncedTodo = remoteTodo.toModel(existing = todo).copy(
            localUpdatedAt = maxOf(todo.localUpdatedAt, remoteTodo.updateDate.toEpochMillis() ?: 0L),
            syncState = SyncState.SYNCED,
            pendingAction = null,
            conflictState = ConflictState.NONE,
        )
        todoDao.upsert(syncedTodo.toEntity())
        queueRepository.deleteById(queueId)
    }

    private suspend fun syncUpdate(queueId: Long, todo: Todo) {
        Log.d(TAG, "syncUpdate queueId=$queueId localId=${todo.localId} remoteId=${todo.remoteId}")
        if (todo.conflictState == ConflictState.REMOTE_DELETED || todo.isDeletedRemotely) {
            Log.w(TAG, "syncUpdate aborted because todo is marked remote deleted localId=${todo.localId}")
            queueRepository.deleteById(queueId)
            return
        }
        val remoteId = todo.remoteId ?: run {
            syncCreate(queueId, todo)
            return
        }
        val remoteTodo = todoApiService.updateTodo(remoteId, todo.toUpdateRequest()).requireData()
        val syncedTodo = remoteTodo.toModel(existing = todo).copy(
            localUpdatedAt = maxOf(todo.localUpdatedAt, remoteTodo.updateDate.toEpochMillis() ?: 0L),
            syncState = SyncState.SYNCED,
            pendingAction = null,
            conflictState = ConflictState.NONE,
        )
        todoDao.upsert(syncedTodo.toEntity())
        queueRepository.deleteById(queueId)
    }

    private suspend fun syncDelete(queueId: Long, todo: Todo) {
        Log.d(TAG, "syncDelete queueId=$queueId localId=${todo.localId} remoteId=${todo.remoteId}")
        Log.w(
            TAG,
            "syncDelete treats DELETE as local-only cleanup to avoid removing cloud data localId=${todo.localId} remoteId=${todo.remoteId}",
        )
        todoDao.hardDelete(todo.localId)
        queueRepository.deleteById(queueId)
    }

    private suspend fun syncToggleComplete(queueId: Long, todo: Todo) {
        Log.d(TAG, "syncToggleComplete queueId=$queueId localId=${todo.localId} remoteId=${todo.remoteId} completed=${todo.completed}")
        if (todo.conflictState == ConflictState.REMOTE_DELETED || todo.isDeletedRemotely) {
            Log.w(TAG, "syncToggleComplete aborted because todo is marked remote deleted localId=${todo.localId}")
            queueRepository.deleteById(queueId)
            return
        }
        val remoteId = todo.remoteId
        if (remoteId == null) {
            val createdRemote = todoApiService.createTodo(todo.toCreateRequest()).requireData()
            if (todo.completed) {
                todoApiService.toggleComplete(createdRemote.id).ensureSuccess()
            }
            val syncedTodo = createdRemote.toModel(existing = todo).copy(
                completed = todo.completed,
                status = if (todo.completed) TodoStatus.COMPLETED else TodoStatus.TODO,
                syncState = SyncState.SYNCED,
                pendingAction = null,
                conflictState = ConflictState.NONE,
                remoteUpdatedAt = System.currentTimeMillis(),
                localUpdatedAt = maxOf(todo.localUpdatedAt, System.currentTimeMillis()),
            )
            todoDao.upsert(syncedTodo.toEntity())
            queueRepository.deleteById(queueId)
            return
        }

        todoApiService.toggleComplete(remoteId).ensureSuccess()
        todoDao.upsert(
            todo.copy(
                syncState = SyncState.SYNCED,
                pendingAction = null,
                remoteUpdatedAt = System.currentTimeMillis(),
                lastSyncError = null,
            ).toEntity(),
        )
        queueRepository.deleteById(queueId)
    }

    private suspend fun pullRemoteTodos(selectedDeviceId: String?) {
        Log.d(TAG, "pullRemoteTodos start selectedDeviceId=$selectedDeviceId")
        val remoteTodos = todoApiService.getTodos(deviceId = selectedDeviceId).requireData()
        Log.d(TAG, "pullRemoteTodos success count=${remoteTodos.size}")
        val remoteIds = remoteTodos.map { it.id }.toSet()

        remoteTodos.forEach { remote ->
            val existing = todoDao.getByRemoteId(remote.id)?.toModel()
            val remoteTodo = remote.toModel(existing = existing)
            val remoteUpdatedAt = remote.updateDate.toEpochMillis()
            when {
                existing == null -> {
                    Log.d(TAG, "pullRemoteTodos insert remoteId=${remote.id} because local copy is missing")
                    todoDao.upsert(remoteTodo.toEntity())
                }

                existing.isDeletedLocally -> {
                    Log.d(TAG, "pullRemoteTodos restore locally deleted todo localId=${existing.localId} remoteId=${remote.id}")
                    todoDao.upsert(remoteTodo.toEntity())
                    queueRepository.deleteForEntity(SyncEntityType.TODO, existing.localId)
                }

                existing.conflictState == ConflictState.REMOTE_DELETED || existing.isDeletedRemotely -> {
                    Log.d(TAG, "pullRemoteTodos resolve remote-deleted conflict from cloud localId=${existing.localId} remoteId=${remote.id}")
                    todoDao.upsert(remoteTodo.toEntity())
                    queueRepository.deleteForEntity(SyncEntityType.TODO, existing.localId)
                }

                shouldKeepLocalMutation(existing, remoteUpdatedAt) -> {
                    Log.d(
                        TAG,
                        "pullRemoteTodos keep local mutation localId=${existing.localId} remoteId=${remote.id} pendingAction=${existing.pendingAction}",
                    )
                }

                remoteChangedSinceLastSync(existing, remoteUpdatedAt) || remote.isNewerThan(existing) -> {
                    Log.w(
                        TAG,
                        "pullRemoteTodos cloud wins over local localId=${existing.localId} remoteId=${remote.id} pendingAction=${existing.pendingAction}",
                    )
                    todoDao.upsert(remoteTodo.toEntity())
                    queueRepository.deleteForEntity(SyncEntityType.TODO, existing.localId)
                }

                else -> {
                    Log.d(TAG, "pullRemoteTodos no-op localId=${existing.localId} remoteId=${remote.id}")
                }
            }
        }

        todoDao.getAllRemoteBackedTodos().forEach { entity ->
            val todo = entity.toModel()
            val isInsidePulledScope = selectedDeviceId.isNullOrBlank() || todo.deviceId == selectedDeviceId
            if (!isInsidePulledScope) {
                Log.d(
                    TAG,
                    "pullRemoteTodos skip missing-remote reconciliation for out-of-scope todo localId=${todo.localId} remoteId=${todo.remoteId} deviceId=${todo.deviceId}",
                )
                return@forEach
            }
            if (todo.remoteId !in remoteIds) {
                when {
                    todo.isDeletedLocally || todo.pendingAction == SyncAction.DELETE -> {
                        Log.d(TAG, "pullRemoteTodos remove stale local-only deleted todo localId=${todo.localId} remoteId=${todo.remoteId}")
                        todoDao.hardDelete(todo.localId)
                        queueRepository.deleteForEntity(SyncEntityType.TODO, todo.localId)
                    }

                    else -> {
                        Log.w(TAG, "pullRemoteTodos mark remote deleted conflict localId=${todo.localId} remoteId=${todo.remoteId}")
                        todoDao.markRemoteDeleted(todo.localId)
                        queueRepository.deleteForEntity(SyncEntityType.TODO, todo.localId)
                    }
                }
            }
        }
    }

    private fun shouldKeepLocalMutation(local: Todo, remoteUpdatedAt: Long?): Boolean {
        val pendingAction = local.pendingAction ?: return false
        if (pendingAction != SyncAction.UPDATE && pendingAction != SyncAction.TOGGLE_COMPLETE) {
            return false
        }
        val knownRemoteUpdatedAt = local.remoteUpdatedAt ?: return false
        return remoteUpdatedAt != null && remoteUpdatedAt == knownRemoteUpdatedAt
    }

    private fun remoteChangedSinceLastSync(local: Todo, remoteUpdatedAt: Long?): Boolean {
        val knownRemoteUpdatedAt = local.remoteUpdatedAt ?: return remoteUpdatedAt != null
        return remoteUpdatedAt != null && remoteUpdatedAt > knownRemoteUpdatedAt
    }

    private fun calculateNextRetryAt(attempt: Int): Long {
        val cappedAttempt = attempt.coerceAtMost(6)
        val delayMillis = 30_000L * (1L shl cappedAttempt)
        return System.currentTimeMillis() + delayMillis.coerceAtMost(60 * 60 * 1000L)
    }

    private suspend fun pushLatestTodosToDevice(selectedDeviceId: String?) {
        if (selectedDeviceId.isNullOrBlank()) {
            Log.d(TAG, "pushLatestTodosToDevice skipped because selectedDeviceId is blank")
            return
        }
        val settings = settingsRepository.settings.first()
        val todos = todoDao.getPushableTodos().map { it.toModel() }
        Log.d(
            TAG,
            "pushLatestTodosToDevice start selectedDeviceId=$selectedDeviceId todoCount=${todos.size} pushType=${settings.defaultPushType} pageId=${settings.defaultPushPageId}",
        )
        runCatching {
            devicePushRepository.pushLatestTodos(
                deviceId = selectedDeviceId,
                pageId = settings.defaultPushPageId.coerceIn(1, 5),
                pushType = settings.defaultPushType,
                todos = todos,
            )
        }.onFailure { error ->
            Log.e(TAG, "pushLatestTodosToDevice failed selectedDeviceId=$selectedDeviceId", error)
        }
    }

    private fun com.passheep.sticky_note.data.remote.dto.RemoteTodoDto.isNewerThan(local: Todo): Boolean {
        val remoteUpdatedAt = updateDate.toEpochMillis() ?: return false
        return remoteUpdatedAt > local.localUpdatedAt
    }

    private companion object {
        const val TAG = "StickyNoteSync"
    }
}
