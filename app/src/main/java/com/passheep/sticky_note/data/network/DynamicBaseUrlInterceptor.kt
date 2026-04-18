package com.passheep.sticky_note.data.network

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val networkConfigStore: NetworkConfigStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val configuredBaseUrl = networkConfigStore.snapshot.value.platformUrl
            .trim()
            .removeSuffix("/")
        val targetBaseUrl = configuredBaseUrl.toHttpUrlOrNull()
            ?: run {
                Log.w(
                    TAG,
                    "skip base url rewrite because configuredBaseUrl is invalid: '$configuredBaseUrl'",
                )
                return chain.proceed(chain.request())
            }

        val originalRequest = chain.request()
        val newUrl = originalRequest.url.rewriteWith(targetBaseUrl)
        Log.d(
            TAG,
            "rewrite url ${originalRequest.url} -> $newUrl",
        )
        return chain.proceed(
            originalRequest.newBuilder()
                .url(newUrl)
                .build(),
        )
    }

    private fun HttpUrl.rewriteWith(baseUrl: HttpUrl): HttpUrl {
        val mergedPath = buildString {
            append(baseUrl.encodedPath.removeSuffix("/"))
            append(encodedPath)
        }.ifBlank { "/" }

        return newBuilder()
            .scheme(baseUrl.scheme)
            .host(baseUrl.host)
            .port(baseUrl.port)
            .encodedPath(mergedPath.ensureLeadingSlash())
            .build()
    }

    private fun String.ensureLeadingSlash(): String =
        if (startsWith("/")) this else "/$this"

    private companion object {
        const val TAG = "StickyNoteNetwork"
    }
}
