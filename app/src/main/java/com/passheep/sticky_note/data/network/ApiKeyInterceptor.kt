package com.passheep.sticky_note.data.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyInterceptor @Inject constructor(
    private val networkConfigStore: NetworkConfigStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val currentApiKey = networkConfigStore.snapshot.value.apiKey.trim()
        Log.d(
            TAG,
            "prepare headers for ${chain.request().method} ${chain.request().url} apiKeyPresent=${currentApiKey.isNotEmpty()} apiKeySuffix=${currentApiKey.takeLast(6)}",
        )
        val request = chain.request().newBuilder().apply {
            if (currentApiKey.isNotEmpty()) {
                header(HEADER_API_KEY, currentApiKey)
            }
        }.build()
        return chain.proceed(request)
    }

    private companion object {
        const val HEADER_API_KEY = "X-API-Key"
        const val TAG = "StickyNoteNetwork"
    }
}
