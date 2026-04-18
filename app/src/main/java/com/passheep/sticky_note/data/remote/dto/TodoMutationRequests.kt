package com.passheep.sticky_note.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateTodoRequest(
    @SerialName("title") val title: String,
    @SerialName("description") val description: String = "",
    @SerialName("dueDate") val dueDate: String = "",
    @SerialName("dueTime") val dueTime: String = "",
    @SerialName("repeatType") val repeatType: String = "",
    @SerialName("repeatWeekday") val repeatWeekday: String = "",
    @SerialName("repeatMonth") val repeatMonth: String = "",
    @SerialName("repeatDay") val repeatDay: String = "",
    @SerialName("priority") val priority: Int,
    @SerialName("deviceId") val deviceId: String = "",
)

@Serializable
data class UpdateTodoRequest(
    @SerialName("title") val title: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("dueDate") val dueDate: String = "",
    @SerialName("dueTime") val dueTime: String = "",
    @SerialName("repeatType") val repeatType: String = "",
    @SerialName("repeatWeekday") val repeatWeekday: String = "",
    @SerialName("repeatMonth") val repeatMonth: String = "",
    @SerialName("repeatDay") val repeatDay: String = "",
    @SerialName("priority") val priority: Int = 0,
    @SerialName("deviceId") val deviceId: String = "",
)
