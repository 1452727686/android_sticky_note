package com.passheep.sticky_note.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteTodoDto(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("dueDate") val dueDate: String? = null,
    @SerialName("dueTime") val dueTime: String? = null,
    @SerialName("repeatType") val repeatType: String? = null,
    @SerialName("repeatWeekday") val repeatWeekday: Int? = null,
    @SerialName("repeatMonth") val repeatMonth: Int? = null,
    @SerialName("repeatDay") val repeatDay: Int? = null,
    @SerialName("status") val status: Int = 0,
    @SerialName("priority") val priority: Int = 0,
    @SerialName("completed") val completed: Boolean = false,
    @SerialName("deviceId") val deviceId: String? = null,
    @SerialName("deviceName") val deviceName: String? = null,
    @SerialName("createDate") val createDate: String? = null,
    @SerialName("updateDate") val updateDate: Long? = null,
)
