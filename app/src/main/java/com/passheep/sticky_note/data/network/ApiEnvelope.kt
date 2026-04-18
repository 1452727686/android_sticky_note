package com.passheep.sticky_note.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(
    @SerialName("code") val code: Int,
    @SerialName("msg") val message: String? = null,
    @SerialName("data") val data: T? = null,
)

