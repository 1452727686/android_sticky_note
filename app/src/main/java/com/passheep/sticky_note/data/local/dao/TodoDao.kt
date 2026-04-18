package com.passheep.sticky_note.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.passheep.sticky_note.core.model.ConflictState
import com.passheep.sticky_note.core.model.SyncAction
import com.passheep.sticky_note.core.model.SyncState
import com.passheep.sticky_note.core.model.TodoStatus
import com.passheep.sticky_note.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query(
        """
        SELECT * FROM todos
        WHERE (:status IS NULL OR status = :status)
          AND isDeletedLocally = 0
        ORDER BY completed ASC, priority DESC, localUpdatedAt DESC
        """,
    )
    fun observeTodos(status: TodoStatus?): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE localId = :localId LIMIT 1")
    fun observeTodo(localId: String): Flow<TodoEntity?>

    @Query("SELECT * FROM todos WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: Long): TodoEntity?

    @Query("SELECT * FROM todos WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: String): TodoEntity?

    @Query("SELECT * FROM todos WHERE remoteId IS NOT NULL")
    suspend fun getAllRemoteBackedTodos(): List<TodoEntity>

    @Query(
        """
        SELECT * FROM todos
        WHERE isDeletedLocally = 0
        ORDER BY completed ASC, priority DESC, localUpdatedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun getPushableTodos(limit: Int = 24): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE syncState = 'PENDING' ORDER BY localUpdatedAt ASC")
    suspend fun getPendingTodos(): List<TodoEntity>

    @Query("SELECT COUNT(*) FROM todos WHERE syncState = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM todos WHERE syncState = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM todos WHERE syncState = 'FAILED'")
    fun observeFailedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM todos WHERE syncState = 'FAILED'")
    suspend fun getFailedCount(): Int

    @Query("SELECT COUNT(*) FROM todos WHERE conflictState != 'NONE' AND isDeletedLocally = 0")
    fun observeConflictCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM todos WHERE conflictState != 'NONE' AND isDeletedLocally = 0")
    suspend fun getConflictCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(todo: TodoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(todos: List<TodoEntity>)

    @Query(
        """
        UPDATE todos
        SET syncState = :syncState,
            lastSyncError = :error
        WHERE localId = :localId
        """,
    )
    suspend fun updateSyncState(localId: String, syncState: SyncState, error: String?)

    @Query(
        """
        UPDATE todos
        SET conflictState = :conflictState,
            syncState = 'CONFLICT',
            isDeletedRemotely = CASE WHEN :conflictState = 'REMOTE_DELETED' THEN 1 ELSE isDeletedRemotely END
        WHERE localId = :localId
        """,
    )
    suspend fun updateConflictState(localId: String, conflictState: ConflictState)

    @Query(
        """
        UPDATE todos
        SET isDeletedRemotely = 1,
            conflictState = 'REMOTE_DELETED',
            syncState = 'CONFLICT'
        WHERE localId = :localId
        """,
    )
    suspend fun markRemoteDeleted(localId: String)

    @Query(
        """
        UPDATE todos
        SET isDeletedLocally = 1,
            pendingAction = :action,
            syncState = 'PENDING',
            localUpdatedAt = :updatedAt
        WHERE localId = :localId
        """,
    )
    suspend fun softDelete(localId: String, action: SyncAction, updatedAt: Long)

    @Query("DELETE FROM todos WHERE localId = :localId")
    suspend fun hardDelete(localId: String)
}
