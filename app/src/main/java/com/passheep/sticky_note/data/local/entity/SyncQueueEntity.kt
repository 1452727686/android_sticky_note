package com.passheep.sticky_note.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.passheep.sticky_note.core.model.QueueStatus
import com.passheep.sticky_note.core.model.SyncAction
import com.passheep.sticky_note.core.model.SyncEntityType
import com.passheep.sticky_note.core.model.SyncQueueItem

@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["queueStatus", "nextRetryAt"]),
        Index(value = ["entityType", "entityLocalId"]),
    ],
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: SyncEntityType,
    val entityLocalId: String,
    val action: SyncAction,
    val payload: String?,
    val queueStatus: QueueStatus,
    val attemptCount: Int,
    val nextRetryAt: Long?,
    val lastError: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

fun SyncQueueEntity.toModel(): SyncQueueItem = SyncQueueItem(
    id = id,
    entityType = entityType,
    entityLocalId = entityLocalId,
    action = action,
    payload = payload,
    queueStatus = queueStatus,
    attemptCount = attemptCount,
    nextRetryAt = nextRetryAt,
    lastError = lastError,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun SyncQueueItem.toEntity(): SyncQueueEntity = SyncQueueEntity(
    id = id,
    entityType = entityType,
    entityLocalId = entityLocalId,
    action = action,
    payload = payload,
    queueStatus = queueStatus,
    attemptCount = attemptCount,
    nextRetryAt = nextRetryAt,
    lastError = lastError,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

