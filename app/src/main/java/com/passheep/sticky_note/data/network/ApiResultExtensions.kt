package com.passheep.sticky_note.data.network

class ApiException(
    val code: Int,
    override val message: String,
) : IllegalStateException(message)

fun ApiEnvelope<Unit>.ensureSuccess() {
    if (code != 0) {
        throw ApiException(code = code, message = message ?: "请求失败")
    }
}

fun <T> ApiEnvelope<T>.requireData(): T {
    if (code != 0) {
        throw ApiException(code = code, message = message ?: "请求失败")
    }
    return data ?: throw ApiException(code = code, message = message ?: "接口返回缺少 data")
}

