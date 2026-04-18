package com.passheep.sticky_note.data.remote.api

import com.passheep.sticky_note.data.network.ApiEnvelope
import com.passheep.sticky_note.data.remote.dto.DeviceDto
import retrofit2.http.GET

interface DeviceApiService {

    @GET("/open/v1/devices")
    suspend fun getDevices(): ApiEnvelope<List<DeviceDto>>
}

