package com.passheep.sticky_note.data.local

import androidx.room.TypeConverter
import com.passheep.sticky_note.core.model.ConflictState
import com.passheep.sticky_note.core.model.QueueStatus
import com.passheep.sticky_note.core.model.RepeatType
import com.passheep.sticky_note.core.model.SyncAction
import com.passheep.sticky_note.core.model.SyncEntityType
import com.passheep.sticky_note.core.model.SyncState
import com.passheep.sticky_note.core.model.TodoStatus
import java.time.LocalDate
import java.time.LocalTime

class Converters {

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter
    fun fromRepeatType(value: RepeatType): String = value.name

    @TypeConverter
    fun toRepeatType(value: String): RepeatType =
        RepeatType.entries.firstOrNull { it.name == value } ?: RepeatType.NONE

    @TypeConverter
    fun fromTodoStatus(value: TodoStatus): String = value.name

    @TypeConverter
    fun toTodoStatus(value: String): TodoStatus =
        TodoStatus.entries.firstOrNull { it.name == value } ?: TodoStatus.TODO

    @TypeConverter
    fun fromSyncState(value: SyncState): String = value.name

    @TypeConverter
    fun toSyncState(value: String): SyncState =
        SyncState.entries.firstOrNull { it.name == value } ?: SyncState.PENDING

    @TypeConverter
    fun fromConflictState(value: ConflictState): String = value.name

    @TypeConverter
    fun toConflictState(value: String): ConflictState =
        ConflictState.entries.firstOrNull { it.name == value } ?: ConflictState.NONE

    @TypeConverter
    fun fromSyncAction(value: SyncAction?): String? = value?.name

    @TypeConverter
    fun toSyncAction(value: String?): SyncAction? =
        value?.let { raw -> SyncAction.entries.firstOrNull { it.name == raw } }

    @TypeConverter
    fun fromSyncEntityType(value: SyncEntityType): String = value.name

    @TypeConverter
    fun toSyncEntityType(value: String): SyncEntityType =
        SyncEntityType.entries.firstOrNull { it.name == value } ?: SyncEntityType.TODO

    @TypeConverter
    fun fromQueueStatus(value: QueueStatus): String = value.name

    @TypeConverter
    fun toQueueStatus(value: String): QueueStatus =
        QueueStatus.entries.firstOrNull { it.name == value } ?: QueueStatus.PENDING
}

