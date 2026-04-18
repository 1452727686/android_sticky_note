package com.passheep.sticky_note.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceDto(
    @SerialName("deviceId") val deviceId: String,
    @SerialName("alias") val alias: String? = null,
    @SerialName("board") val board: String? = null,
)
