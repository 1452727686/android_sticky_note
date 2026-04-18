package com.passheep.sticky_note.core.model

data class SyncQueueItem(
    val id: Long,
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

enum class SyncEntityType {
    TODO,
    DEVICE,
}

enum class SyncAction {
    CREATE,
    UPDATE,
    DELETE,
    TOGGLE_COMPLETE,
    PUSH_DEVICE_CONTENT,
    PULL_REMOTE,
}

enum class QueueStatus {
    PENDING,
    RUNNING,
    FAILED,
    COMPLETED,
}

