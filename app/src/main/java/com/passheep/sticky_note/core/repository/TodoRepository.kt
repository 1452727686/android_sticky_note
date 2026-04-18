package com.passheep.sticky_note.core.repository

import com.passheep.sticky_note.core.model.ConflictState
import com.passheep.sticky_note.core.model.RemoteDeletedResolution
import com.passheep.sticky_note.core.model.SyncState
import com.passheep.sticky_note.core.model.TodoDraftInput
import com.passheep.sticky_note.core.model.Todo
import com.passheep.sticky_note.core.model.TodoStatus
import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    fun observeTodos(status: TodoStatus? = null): Flow<List<Todo>>
    fun observeTodo(localId: String): Flow<Todo?>
    fun observeSyncSummary(): Flow<TodoSyncSummary>
    suspend fun createTodo(input: TodoDraftInput): String
    suspend fun updateTodo(localId: String, input: TodoDraftInput)
    suspend fun toggleCompleted(localId: String)
    suspend fun upsert(todo: Todo)
    suspend fun upsertAll(todos: List<Todo>)
    suspend fun markPending(localId: String, syncState: SyncState, error: String? = null)
    suspend fun markConflict(localId: String, conflictState: ConflictState)
    suspend fun markRemoteDeleted(localId: String)
    suspend fun deleteLocal(localId: String)
    suspend fun resolveRemoteDeleted(localId: String, resolution: RemoteDeletedResolution)
}

data class TodoSyncSummary(
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val conflictCount: Int = 0,
)
