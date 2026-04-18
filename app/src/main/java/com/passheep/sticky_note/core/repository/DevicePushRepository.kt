package com.passheep.sticky_note.core.repository

import com.passheep.sticky_note.core.model.Todo
import com.passheep.sticky_note.core.settings.PushType
import java.io.File

interface DevicePushRepository {
    suspend fun pushText(
        deviceId: String,
        text: String,
        pageId: Int,
        fontSize: Int = 20,
    ): PushResult

    suspend fun pushStructuredText(
        deviceId: String,
        title: String?,
        body: String?,
        pageId: Int,
    ): PushResult

    suspend fun pushImage(
        deviceId: String,
        images: List<File>,
        pageId: Int,
        dither: Boolean = true,
    ): PushResult

    suspend fun deletePage(deviceId: String, pageId: Int): PushResult

    suspend fun pushLatestTodos(
        deviceId: String,
        pageId: Int,
        pushType: PushType,
        todos: List<Todo>,
    ): PushResult
}

sealed interface PushResult {
    data class Success(
        val pushedPages: Int,
        val pageId: String?,
    ) : PushResult

    data class Failure(
        val message: String,
        val throwable: Throwable? = null,
    ) : PushResult
}
