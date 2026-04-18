package com.passheep.sticky_note.data.network

import com.passheep.sticky_note.core.settings.DEFAULT_PLATFORM_URL

data class NetworkConfigSnapshot(
    val platformUrl: String = DEFAULT_PLATFORM_URL,
    val apiKey: String = "",
)
