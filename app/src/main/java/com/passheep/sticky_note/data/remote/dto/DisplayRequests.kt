package com.passheep.sticky_note.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DisplayTextRequest(
    @SerialName("text") val text: String,
    @SerialName("fontSize") val fontSize: Int? = null,
    @SerialName("pageId") val pageId: String? = null,
)

@Serializable
data class DisplayStructuredTextRequest(
    @SerialName("title") val title: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("pageId") val pageId: String? = null,
)

@Serializable
data class DisplayResponse(
    @SerialName("totalPages") val totalPages: Int,
    @SerialName("pushedPages") val pushedPages: Int,
    @SerialName("pageId") val pageId: String? = null,
)

