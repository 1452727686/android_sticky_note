package com.passheep.sticky_note.data.repository

import com.passheep.sticky_note.core.model.Todo
import com.passheep.sticky_note.core.repository.DevicePushRepository
import com.passheep.sticky_note.core.repository.PushResult
import com.passheep.sticky_note.core.settings.PushType
import com.passheep.sticky_note.data.network.ensureSuccess
import com.passheep.sticky_note.data.network.requireData
import com.passheep.sticky_note.data.push.TodoImageRenderer
import com.passheep.sticky_note.data.push.formatTodosAsStructured
import com.passheep.sticky_note.data.push.formatTodosAsText
import com.passheep.sticky_note.data.remote.api.DisplayApiService
import com.passheep.sticky_note.data.remote.dto.DisplayStructuredTextRequest
import com.passheep.sticky_note.data.remote.dto.DisplayTextRequest
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class DefaultDevicePushRepository @Inject constructor(
    private val displayApiService: DisplayApiService,
    private val imageRenderer: TodoImageRenderer,
) : DevicePushRepository {

    override suspend fun pushText(
        deviceId: String,
        text: String,
        pageId: Int,
        fontSize: Int,
    ): PushResult = runCatching {
        val response = displayApiService.pushText(
            deviceId = deviceId,
            request = DisplayTextRequest(
                text = text,
                fontSize = fontSize,
                pageId = pageId.toString(),
            ),
        ).requireData()
        PushResult.Success(
            pushedPages = response.pushedPages,
            pageId = response.pageId,
        )
    }.getOrElse { error ->
        PushResult.Failure(error.message ?: "文本推送失败", error)
    }

    override suspend fun pushStructuredText(
        deviceId: String,
        title: String?,
        body: String?,
        pageId: Int,
    ): PushResult = runCatching {
        val response = displayApiService.pushStructuredText(
            deviceId = deviceId,
            request = DisplayStructuredTextRequest(
                title = title,
                body = body,
                pageId = pageId.toString(),
            ),
        ).requireData()
        PushResult.Success(
            pushedPages = response.pushedPages,
            pageId = response.pageId,
        )
    }.getOrElse { error ->
        PushResult.Failure(error.message ?: "结构化文本推送失败", error)
    }

    override suspend fun pushImage(
        deviceId: String,
        images: List<File>,
        pageId: Int,
        dither: Boolean,
    ): PushResult = runCatching {
        require(images.isNotEmpty()) { "至少需要 1 张图片" }
        require(images.size <= 5) { "最多支持 5 张图片" }
        images.forEach { image ->
            require(image.length() <= MAX_IMAGE_BYTES) { "图片大小不能超过 2MB" }
        }
        val response = displayApiService.pushImage(
            deviceId = deviceId,
            images = images.map { file ->
                MultipartBody.Part.createFormData(
                    name = "images",
                    filename = file.name,
                    body = file.asRequestBody("image/png".toMediaType()),
                )
            },
            dither = dither.toString().toRequestBody("text/plain".toMediaType()),
            pageId = pageId.toString().toRequestBody("text/plain".toMediaType()),
        ).requireData()
        PushResult.Success(
            pushedPages = response.pushedPages,
            pageId = response.pageId,
        )
    }.getOrElse { error ->
        PushResult.Failure(error.message ?: "图片推送失败", error)
    }

    override suspend fun deletePage(deviceId: String, pageId: Int): PushResult = runCatching {
        displayApiService.deletePage(deviceId = deviceId, pageId = pageId.toString()).ensureSuccess()
        PushResult.Success(
            pushedPages = 0,
            pageId = pageId.toString(),
        )
    }.getOrElse { error ->
        PushResult.Failure(error.message ?: "删除页面失败", error)
    }

    override suspend fun pushLatestTodos(
        deviceId: String,
        pageId: Int,
        pushType: PushType,
        todos: List<Todo>,
    ): PushResult = when (pushType) {
        PushType.TEXT -> pushText(
            deviceId = deviceId,
            text = formatTodosAsText(todos),
            pageId = pageId,
        )

        PushType.STRUCTURED_TEXT -> {
            val payload = formatTodosAsStructured(todos)
            pushStructuredText(
                deviceId = deviceId,
                title = payload.first,
                body = payload.second,
                pageId = pageId,
            )
        }

        PushType.IMAGE -> {
            val image = imageRenderer.renderTodosToImage(todos)
            pushImage(
                deviceId = deviceId,
                images = listOf(image),
                pageId = pageId,
            )
        }
    }

    private companion object {
        const val MAX_IMAGE_BYTES = 2L * 1024L * 1024L
    }
}
