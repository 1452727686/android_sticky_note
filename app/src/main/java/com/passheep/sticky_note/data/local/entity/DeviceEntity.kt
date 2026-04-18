package com.passheep.sticky_note.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.passheep.sticky_note.core.model.Device

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val deviceId: String,
    val alias: String,
    val board: String,
    val isSelected: Boolean,
    val syncedAt: Long,
)

fun DeviceEntity.toModel(): Device = Device(
    deviceId = deviceId,
    alias = alias,
    board = board,
    isSelected = isSelected,
    syncedAt = syncedAt,
)

fun Device.toEntity(): DeviceEntity = DeviceEntity(
    deviceId = deviceId,
    alias = alias,
    board = board,
    isSelected = isSelected,
    syncedAt = syncedAt,
)

