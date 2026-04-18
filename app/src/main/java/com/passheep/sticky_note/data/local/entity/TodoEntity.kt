package com.passheep.sticky_note.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.passheep.sticky_note.core.model.ConflictState
import com.passheep.sticky_note.core.model.SyncAction
import com.passheep.sticky_note.core.model.SyncState
import com.passheep.sticky_note.core.model.Todo
import com.passheep.sticky_note.core.model.TodoStatus
import com.passheep.sticky_note.core.model.RepeatType
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "todos",
    indices = [
        Index(value = ["remoteId"], unique = true),
        Index(value = ["status", "isDeletedLocally", "isDeletedRemotely"]),
        Index(value = ["syncState", "pendingAction"]),
        Index(value = ["deviceId"]),
        Index(value = ["localUpdatedAt"]),
    ],
)
data class TodoEntity(
    @PrimaryKey val localId: String,
    val remoteId: Long?,
    val title: String,
    val description: String,
    val dueDate: LocalDate?,
    val dueTime: LocalTime?,
    val repeatType: RepeatType,
    val repeatWeekday: Int?,
    val repeatMonth: Int?,
    val repeatDay: Int?,
    val priority: Int,
    val status: TodoStatus,
    val completed: Boolean,
    val deviceId: String?,
    val deviceName: String?,
    val syncState: SyncState,
    val pendingAction: SyncAction?,
    val conflictState: ConflictState,
    val localUpdatedAt: Long,
    val remoteUpdatedAt: Long?,
    val createdAt: Long,
    val remoteCreateDate: String?,
    val isDeletedLocally: Boolean,
    val isDeletedRemotely: Boolean,
    val lastSyncError: String?,
)

fun TodoEntity.toModel(): Todo = Todo(
    localId = localId,
    remoteId = remoteId,
    title = title,
    description = description,
    dueDate = dueDate,
    dueTime = dueTime,
    repeatType = repeatType,
    repeatWeekday = repeatWeekday,
    repeatMonth = repeatMonth,
    repeatDay = repeatDay,
    priority = priority,
    status = status,
    completed = completed,
    deviceId = deviceId,
    deviceName = deviceName,
    syncState = syncState,
    pendingAction = pendingAction,
    conflictState = conflictState,
    localUpdatedAt = localUpdatedAt,
    remoteUpdatedAt = remoteUpdatedAt,
    createdAt = createdAt,
    remoteCreateDate = remoteCreateDate,
    isDeletedLocally = isDeletedLocally,
    isDeletedRemotely = isDeletedRemotely,
    lastSyncError = lastSyncError,
)

fun Todo.toEntity(): TodoEntity = TodoEntity(
    localId = localId,
    remoteId = remoteId,
    title = title,
    description = description,
    dueDate = dueDate,
    dueTime = dueTime,
    repeatType = repeatType,
    repeatWeekday = repeatWeekday,
    repeatMonth = repeatMonth,
    repeatDay = repeatDay,
    priority = priority,
    status = status,
    completed = completed,
    deviceId = deviceId,
    deviceName = deviceName,
    syncState = syncState,
    pendingAction = pendingAction,
    conflictState = conflictState,
    localUpdatedAt = localUpdatedAt,
    remoteUpdatedAt = remoteUpdatedAt,
    createdAt = createdAt,
    remoteCreateDate = remoteCreateDate,
    isDeletedLocally = isDeletedLocally,
    isDeletedRemotely = isDeletedRemotely,
    lastSyncError = lastSyncError,
)

