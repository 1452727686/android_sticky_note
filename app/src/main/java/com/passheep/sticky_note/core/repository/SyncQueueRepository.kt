package com.passheep.sticky_note.core.repository

import com.passheep.sticky_note.core.model.QueueStatus
import com.passheep.sticky_note.core.model.SyncAction
import com.passheep.sticky_note.core.model.SyncEntityType
import com.passheep.sticky_note.core.model.SyncQueueItem
import kotlinx.coroutines.flow.Flow

interface SyncQueueRepository {
    fun observeQueue(status: QueueStatus? = null): Flow<List<SyncQueueItem>>
    suspend fun enqueue(item: SyncQueueItem): Long
    suspend fun getExecutableItems(now: Long, limit: Int = 20): List<SyncQueueItem>
    suspend fun findOpenItem(
        entityType: SyncEntityType,
        entityLocalId: String,
        action: SyncAction,
    ): SyncQueueItem?
    suspend fun update(item: SyncQueueItem)
    suspend fun updateState(
        id: Long,
        status: QueueStatus,
        attemptCount: Int,
        nextRetryAt: Long?,
        lastError: String?,
        updatedAt: Long,
    )
    suspend fun deleteById(id: Long)
    suspend fun deleteForEntity(entityType: SyncEntityType, entityLocalId: String)
    suspend fun clearCompleted()
}
