package com.passheep.sticky_note.core.repository

import com.passheep.sticky_note.core.model.Device
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun observeDevices(): Flow<List<Device>>
    fun observeSelectedDevice(): Flow<Device?>
    suspend fun upsertDevices(devices: List<Device>)
    suspend fun updateSelectedDevice(deviceId: String?)
    suspend fun clearDevices()
}

