package com.passheep.sticky_note.data.sync

sealed interface SyncRunResult {
    data class Success(val reason: String) : SyncRunResult
    data class Skipped(val message: String) : SyncRunResult
    data class Failure(val reason: String, val throwable: Throwable) : SyncRunResult
}

