package com.passheep.sticky_note.data.repository

import com.passheep.sticky_note.core.repository.DeviceRepository
import com.passheep.sticky_note.core.repository.DevicePushRepository
import com.passheep.sticky_note.core.repository.SyncQueueRepository
import com.passheep.sticky_note.core.repository.TodoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTodoRepository(
        repository: DefaultTodoRepository,
    ): TodoRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(
        repository: DefaultDeviceRepository,
    ): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindDevicePushRepository(
        repository: DefaultDevicePushRepository,
    ): DevicePushRepository

    @Binds
    @Singleton
    abstract fun bindSyncQueueRepository(
        repository: DefaultSyncQueueRepository,
    ): SyncQueueRepository
}
