package com.passheep.sticky_note.core.model

import java.time.LocalDate
import java.time.LocalTime

data class TodoDraftInput(
    val title: String,
    val description: String,
    val dueDate: LocalDate?,
    val dueTime: LocalTime?,
    val repeatType: RepeatType?,
    val repeatWeekday: Int?,
    val repeatMonth: Int?,
    val repeatDay: Int?,
    val priority: Int,
    val deviceId: String?,
)

enum class RemoteDeletedResolution {
    DELETE_LOCAL,
    RESTORE_REMOTE,
}
