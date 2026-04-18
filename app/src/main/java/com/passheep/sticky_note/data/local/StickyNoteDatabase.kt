package com.passheep.sticky_note.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.passheep.sticky_note.data.local.dao.DeviceDao
import com.passheep.sticky_note.data.local.dao.SyncQueueDao
import com.passheep.sticky_note.data.local.dao.TodoDao
import com.passheep.sticky_note.data.local.entity.DeviceEntity
import com.passheep.sticky_note.data.local.entity.SyncQueueEntity
import com.passheep.sticky_note.data.local.entity.TodoEntity

@Database(
    entities = [
        TodoEntity::class,
        DeviceEntity::class,
        SyncQueueEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class StickyNoteDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun deviceDao(): DeviceDao
    abstract fun syncQueueDao(): SyncQueueDao
}

