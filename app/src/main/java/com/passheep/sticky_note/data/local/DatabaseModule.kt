package com.passheep.sticky_note.data.local

import android.content.Context
import androidx.room.Room
import com.passheep.sticky_note.data.local.dao.DeviceDao
import com.passheep.sticky_note.data.local.dao.SyncQueueDao
import com.passheep.sticky_note.data.local.dao.TodoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): StickyNoteDatabase = Room.databaseBuilder(
        context,
        StickyNoteDatabase::class.java,
        "sticky_note.db",
    ).fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideTodoDao(database: StickyNoteDatabase): TodoDao = database.todoDao()

    @Provides
    fun provideDeviceDao(database: StickyNoteDatabase): DeviceDao = database.deviceDao()

    @Provides
    fun provideSyncQueueDao(database: StickyNoteDatabase): SyncQueueDao = database.syncQueueDao()
}

