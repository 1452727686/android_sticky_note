package com.passheep.sticky_note.data.repository

import com.passheep.sticky_note.core.model.ConflictState
import com.passheep.sticky_note.core.model.RepeatType
import com.passheep.sticky_note.core.model.RemoteDeletedResolution
import com.passheep.sticky_note.core.model.SyncAction
import com.passheep.sticky_note.core.model.SyncEntityType
import com.passheep.sticky_note.core.model.SyncState
import com.passheep.sticky_note.core.model.TodoDraftInput
import com.passheep.sticky_note.core.model.Todo
import com.passheep.sticky_note.core.model.TodoStatus
import com.passheep.sticky_note.core.repository.TodoRepository
import com.passheep.sticky_note.core.repository.TodoSyncSummary
import com.passheep.sticky_note.core.repository.SyncQueueRepository
import com.passheep.sticky_note.core.settings.SettingsRepository
import com.passheep.sticky_note.data.local.dao.TodoDao
import com.passheep.sticky_note.data.local.entity.toEntity
import com.passheep.sticky_note.data.local.entity.toModel
import com.passheep.sticky_note.data.sync.SyncManager
import com.passheep.sticky_note.data.sync.SyncScheduler
import com.passheep.sticky_note.widget.WidgetUpdater
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class DefaultTodoRepository @Inject constructor(
    private val todoDao: TodoDao,
    private val syncManager: SyncManager,
    private val syncScheduler: SyncScheduler,
    private val settingsRepository: SettingsRepository,
    private val syncQueueRepository: SyncQueueRepository,
    private val widgetUpdater: WidgetUpdater,
) : TodoRepository {

    override fun observeTodos(status: TodoStatus?): Flow<List<Todo>> =
        todoDao.observeTodos(status).map { entities -> entities.map { it.toModel() } }

    override fun observeTodo(localId: String): Flow<Todo?> =
        todoDao.observeTodo(localId).map { it?.toModel() }

    override fun observeSyncSummary(): Flow<TodoSyncSummary> = combine(
        todoDao.observePendingCount(),
        todoDao.observeFailedCount(),
        todoDao.observeConflictCount(),
    ) { pending, failed, conflict ->
        TodoSyncSummary(
            pendingCount = pending,
            failedCount = failed,
            conflictCount = conflict,
        )
    }

    override suspend fun createTodo(input: TodoDraftInput): String {
        val now = System.currentTimeMillis()
        val selectedDeviceId = settingsRepository.settings.first().selectedDeviceId
        val localId = UUID.randomUUID().toString()
        val todo = Todo(
            localId = localId,
            remoteId = null,
            title = input.title.trim(),
            description = input.description.trim(),
            dueDate = input.dueDate,
            dueTime = input.dueTime,
            repeatType = input.repeatType ?: RepeatType.NONE,
            repeatWeekday = input.repeatWeekday,
            repeatMonth = input.repeatMonth,
            repeatDay = input.repeatDay,
            priority = input.priority,
            status = TodoStatus.TODO,
            completed = false,
            deviceId = input.deviceId ?: selectedDeviceId,
            deviceName = null,
            syncState = SyncState.PENDING,
            pendingAction = SyncAction.CREATE,
            conflictState = ConflictState.NONE,
            localUpdatedAt = now,
            remoteUpdatedAt = null,
            createdAt = now,
            remoteCreateDate = null,
            isDeletedLocally = false,
            isDeletedRemotely = false,
            lastSyncError = null,
        )
        todoDao.upsert(todo.toEntity())
        syncManager.enqueueMutation(localId = localId, action = SyncAction.CREATE)
        syncScheduler.enqueueImmediateSync()
        widgetUpdater.refreshAllAsync()
        return localId
    }

    override suspend fun updateTodo(localId: String, input: TodoDraftInput) {
        val existing = todoDao.getByLocalId(localId)?.toModel() ?: return
        val now = System.currentTimeMillis()
        val isRemoteDeletedConflict = existing.conflictState == ConflictState.REMOTE_DELETED
        val action = when {
            existing.remoteId == null && existing.completed -> SyncAction.TOGGLE_COMPLETE
            existing.remoteId == null -> SyncAction.CREATE
            else -> SyncAction.UPDATE
        }
        val updated = existing.copy(
            title = input.title.trim(),
            description = input.description.trim(),
            dueDate = input.dueDate,
            dueTime = input.dueTime,
            repeatType = input.repeatType ?: RepeatType.NONE,
            repeatWeekday = input.repeatWeekday,
            repeatMonth = input.repeatMonth,
            repeatDay = input.repeatDay,
            priority = input.priority,
            deviceId = input.deviceId,
            syncState = if (isRemoteDeletedConflict) SyncState.CONFLICT else SyncState.PENDING,
            pendingAction = if (isRemoteDeletedConflict) null else action,
            conflictState = if (isRemoteDeletedConflict) ConflictState.REMOTE_DELETED else ConflictState.NONE,
            localUpdatedAt = now,
            remoteUpdatedAt = if (isRemoteDeletedConflict) existing.remoteUpdatedAt else if (action == SyncAction.CREATE) null else existing.remoteUpdatedAt,
            remoteId = if (isRemoteDeletedConflict) existing.remoteId else if (action == SyncAction.CREATE) null else existing.remoteId,
            isDeletedRemotely = isRemoteDeletedConflict,
            lastSyncError = null,
        )
        todoDao.upsert(updated.toEntity())
        if (!isRemoteDeletedConflict) {
            syncManager.enqueueMutation(localId = localId, action = action)
            syncScheduler.enqueueImmediateSync()
        }
        widgetUpdater.refreshAllAsync()
    }

    override suspend fun toggleCompleted(localId: String) {
        val existing = todoDao.getByLocalId(localId)?.toModel() ?: return
        val now = System.currentTimeMillis()
        val targetCompleted = !existing.completed
        val targetStatus = if (targetCompleted) TodoStatus.COMPLETED else TodoStatus.TODO
        val isRemoteDeletedConflict = existing.conflictState == ConflictState.REMOTE_DELETED
        val action = if (existing.remoteId == null && !targetCompleted) SyncAction.CREATE else SyncAction.TOGGLE_COMPLETE
        val updated = existing.copy(
            completed = targetCompleted,
            status = targetStatus,
            syncState = if (isRemoteDeletedConflict) SyncState.CONFLICT else SyncState.PENDING,
            pendingAction = if (isRemoteDeletedConflict) null else action,
            conflictState = if (isRemoteDeletedConflict) ConflictState.REMOTE_DELETED else ConflictState.NONE,
            localUpdatedAt = now,
            lastSyncError = null,
        )
        todoDao.upsert(updated.toEntity())
        if (!isRemoteDeletedConflict) {
            syncQueueRepository.deleteForEntity(SyncEntityType.TODO, localId)
            syncManager.enqueueMutation(localId = localId, action = action)
            syncScheduler.enqueueImmediateSync()
        }
        widgetUpdater.refreshAllAsync()
    }

    override suspend fun upsert(todo: Todo) {
        todoDao.upsert(todo.toEntity())
    }

    override suspend fun upsertAll(todos: List<Todo>) {
        todoDao.upsertAll(todos.map(Todo::toEntity))
    }

    override suspend fun markPending(localId: String, syncState: SyncState, error: String?) {
        todoDao.updateSyncState(localId, syncState, error)
    }

    override suspend fun markConflict(localId: String, conflictState: ConflictState) {
        todoDao.updateConflictState(localId, conflictState)
    }

    override suspend fun markRemoteDeleted(localId: String) {
        todoDao.markRemoteDeleted(localId)
    }

    override suspend fun deleteLocal(localId: String) {
        val existing = todoDao.getByLocalId(localId)?.toModel() ?: return
        syncQueueRepository.deleteForEntity(SyncEntityType.TODO, localId)
        if (existing.remoteId == null || existing.conflictState == ConflictState.REMOTE_DELETED) {
            todoDao.hardDelete(localId)
        } else {
            // 云端优先：本地删除只移除本地副本，不再主动删除云端待办。
            todoDao.hardDelete(localId)
            syncScheduler.enqueueImmediateSync()
        }
        widgetUpdater.refreshAllAsync()
    }

    override suspend fun resolveRemoteDeleted(localId: String, resolution: RemoteDeletedResolution) {
        val existing = todoDao.getByLocalId(localId)?.toModel() ?: return
        when (resolution) {
            RemoteDeletedResolution.DELETE_LOCAL -> {
                syncQueueRepository.deleteForEntity(SyncEntityType.TODO, localId)
                todoDao.hardDelete(localId)
                widgetUpdater.refreshAllAsync()
            }

            RemoteDeletedResolution.RESTORE_REMOTE -> {
                val restoreAction = if (existing.completed) {
                    SyncAction.TOGGLE_COMPLETE
                } else {
                    SyncAction.CREATE
                }
                val restored = existing.copy(
                    remoteId = null,
                    remoteUpdatedAt = null,
                    isDeletedRemotely = false,
                    conflictState = ConflictState.NONE,
                    syncState = SyncState.PENDING,
                    pendingAction = restoreAction,
                    localUpdatedAt = System.currentTimeMillis(),
                    lastSyncError = null,
                )
                todoDao.upsert(restored.toEntity())
                syncQueueRepository.deleteForEntity(SyncEntityType.TODO, localId)
                syncManager.enqueueMutation(localId = localId, action = restoreAction)
                syncScheduler.enqueueImmediateSync()
                widgetUpdater.refreshAllAsync()
            }
        }
    }
}
