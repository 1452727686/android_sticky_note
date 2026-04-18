package com.passheep.sticky_note.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.passheep.sticky_note.core.model.QueueStatus
import com.passheep.sticky_note.core.model.SyncAction
import com.passheep.sticky_note.core.model.SyncEntityType
import com.passheep.sticky_note.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {

    @Query(
        """
        SELECT * FROM sync_queue
        WHERE (:status IS NULL OR queueStatus = :status)
        ORDER BY createdAt ASC
        """,
    )
    fun observeQueue(status: QueueStatus?): Flow<List<SyncQueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity): Long

    @Query(
        """
        SELECT * FROM sync_queue
        WHERE queueStatus IN ('PENDING', 'FAILED')
          AND (nextRetryAt IS NULL OR nextRetryAt <= :now)
        ORDER BY createdAt ASC
        LIMIT :limit
        """,
    )
    suspend fun getExecutableItems(now: Long, limit: Int): List<SyncQueueEntity>

    @Query(
        """
        SELECT * FROM sync_queue
        WHERE entityType = :entityType
          AND entityLocalId = :entityLocalId
          AND action = :action
          AND queueStatus IN ('PENDING', 'FAILED', 'RUNNING')
        LIMIT 1
        """,
    )
    suspend fun findOpenItem(
        entityType: SyncEntityType,
        entityLocalId: String,
        action: SyncAction,
    ): SyncQueueEntity?

    @Update
    suspend fun update(item: SyncQueueEntity)

    @Delete
    suspend fun delete(item: SyncQueueEntity)

    @Query(
        """
        UPDATE sync_queue
        SET queueStatus = :status,
            attemptCount = :attemptCount,
            nextRetryAt = :nextRetryAt,
            lastError = :lastError,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateState(
        id: Long,
        status: QueueStatus,
        attemptCount: Int,
        nextRetryAt: Long?,
        lastError: String?,
        updatedAt: Long,
    )

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        DELETE FROM sync_queue
        WHERE entityType = :entityType
          AND entityLocalId = :entityLocalId
        """,
    )
    suspend fun deleteForEntity(entityType: SyncEntityType, entityLocalId: String)

    @Query("DELETE FROM sync_queue WHERE queueStatus = 'COMPLETED'")
    suspend fun clearCompleted()
}
