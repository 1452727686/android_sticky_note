package com.passheep.sticky_note.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.passheep.sticky_note.data.local.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    @Query("SELECT * FROM devices ORDER BY isSelected DESC, alias COLLATE NOCASE ASC")
    fun observeDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE isSelected = 1 LIMIT 1")
    fun observeSelectedDevice(): Flow<DeviceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDevices(devices: List<DeviceEntity>)

    @Query("UPDATE devices SET isSelected = CASE WHEN deviceId = :deviceId THEN 1 ELSE 0 END")
    suspend fun updateSelection(deviceId: String?)

    @Query("UPDATE devices SET isSelected = 0")
    suspend fun clearSelection()

    @Query("DELETE FROM devices")
    suspend fun clearAll()
}

