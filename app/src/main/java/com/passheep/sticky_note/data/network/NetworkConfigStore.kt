package com.passheep.sticky_note.data.network

import android.util.Log
import com.passheep.sticky_note.core.dispatchers.ApplicationScope
import com.passheep.sticky_note.core.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
class NetworkConfigStore @Inject constructor(
    settingsRepository: SettingsRepository,
    @ApplicationScope scope: CoroutineScope,
) {
    private val mutableSnapshot = MutableStateFlow(NetworkConfigSnapshot())
    val snapshot: StateFlow<NetworkConfigSnapshot> = mutableSnapshot.asStateFlow()

    init {
        scope.launch {
            settingsRepository.settings.collect { settings ->
                Log.d(
                    TAG,
                    "network config updated baseUrl=${settings.platformUrl} apiKeyPresent=${settings.apiKey.isNotBlank()}",
                )
                mutableSnapshot.value = NetworkConfigSnapshot(
                    platformUrl = settings.platformUrl,
                    apiKey = settings.apiKey,
                )
            }
        }
    }

    private companion object {
        const val TAG = "StickyNoteNetwork"
    }
}
