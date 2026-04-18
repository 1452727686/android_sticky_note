package com.passheep.sticky_note.data.repository

import com.passheep.sticky_note.core.model.Device
import com.passheep.sticky_note.core.repository.DeviceRepository
import com.passheep.sticky_note.data.local.dao.DeviceDao
import com.passheep.sticky_note.data.local.entity.toEntity
import com.passheep.sticky_note.data.local.entity.toModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DefaultDeviceRepository @Inject constructor(
    private val deviceDao: DeviceDao,
) : DeviceRepository {

    override fun observeDevices(): Flow<List<Device>> =
        deviceDao.observeDevices().map { entities -> entities.map { it.toModel() } }

    override fun observeSelectedDevice(): Flow<Device?> =
        deviceDao.observeSelectedDevice().map { it?.toModel() }

    override suspend fun upsertDevices(devices: List<Device>) {
        deviceDao.upsertDevices(devices.map(Device::toEntity))
    }

    override suspend fun updateSelectedDevice(deviceId: String?) {
        if (deviceId.isNullOrBlank()) {
            deviceDao.clearSelection()
        } else {
            deviceDao.updateSelection(deviceId)
        }
    }

    override suspend fun clearDevices() {
        deviceDao.clearAll()
    }
}

