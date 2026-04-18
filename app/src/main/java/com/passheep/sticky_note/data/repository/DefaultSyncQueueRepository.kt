package com.passheep.sticky_note.data.repository

import com.passheep.sticky_note.core.model.QueueStatus
import com.passheep.sticky_note.core.model.SyncAction
import com.passheep.sticky_note.core.model.SyncEntityType
import com.passheep.sticky_note.core.model.SyncQueueItem
import com.passheep.sticky_note.core.repository.SyncQueueRepository
import com.passheep.sticky_note.data.local.dao.SyncQueueDao
import com.passheep.sticky_note.data.local.entity.toEntity
import com.passheep.sticky_note.data.local.entity.toModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DefaultSyncQueueRepository @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
) : SyncQueueRepository {

    override fun observeQueue(status: QueueStatus?): Flow<List<SyncQueueItem>> =
        syncQueueDao.observeQueue(status).map { entities -> entities.map { it.toModel() } }

    override suspend fun enqueue(item: SyncQueueItem): Long = syncQueueDao.insert(item.toEntity())

    override suspend fun getExecutableItems(now: Long, limit: Int): List<SyncQueueItem> =
        syncQueueDao.getExecutableItems(now, limit).map { it.toModel() }

    override suspend fun findOpenItem(
        entityType: SyncEntityType,
        entityLocalId: String,
        action: SyncAction,
    ): SyncQueueItem? = syncQueueDao.findOpenItem(entityType, entityLocalId, action)?.toModel()

    override suspend fun update(item: SyncQueueItem) {
        syncQueueDao.update(item.toEntity())
    }

    override suspend fun updateState(
        id: Long,
        status: QueueStatus,
        attemptCount: Int,
        nextRetryAt: Long?,
        lastError: String?,
        updatedAt: Long,
    ) {
        syncQueueDao.updateState(id, status, attemptCount, nextRetryAt, lastError, updatedAt)
    }

    override suspend fun deleteById(id: Long) {
        syncQueueDao.deleteById(id)
    }

    override suspend fun deleteForEntity(entityType: SyncEntityType, entityLocalId: String) {
        syncQueueDao.deleteForEntity(entityType, entityLocalId)
    }

    override suspend fun clearCompleted() {
        syncQueueDao.clearCompleted()
    }
}
