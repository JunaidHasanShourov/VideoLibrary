package com.example.data.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

object NetworkClient {
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

    fun newRequest(url: String, headers: Map<String, String> = emptyMap()): Request {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", DEFAULT_USER_AGENT)
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")

        headers.forEach { (k, v) ->
            builder.header(k, v)
        }

        return builder.build()
    }

    suspend fun probeUrl(url: String): MediaProbeResult {
        return try {
            val headRequest = newRequest(url).newBuilder().head().build()
            var response = okHttpClient.newCall(headRequest).execute()

            // If server rejects HEAD with 403 or 405, fallback to GET range request
            if (response.code == 403 || response.code == 405) {
                response.close()
                val getRequest = newRequest(url).newBuilder()
                    .get()
                    .header("Range", "bytes=0-1024")
                    .build()
                response = okHttpClient.newCall(getRequest).execute()
            }

            response.use { res ->
                val contentType = res.header("Content-Type") ?: ""
                val contentLength = res.header("Content-Length")?.toLongOrNull() ?: 0L
                val acceptsRanges = res.header("Accept-Ranges")?.contains("bytes", ignoreCase = true) == true

                MediaProbeResult(
                    isAccessible = res.isSuccessful,
                    statusCode = res.code,
                    contentType = contentType,
                    contentLength = contentLength,
                    acceptsRanges = acceptsRanges,
                    finalUrl = res.request.url.toString()
                )
            }
        } catch (e: Exception) {
            MediaProbeResult(
                isAccessible = false,
                statusCode = 0,
                errorMessage = e.message ?: "Failed to connect"
            )
        }
    }
}

data class MediaProbeResult(
    val isAccessible: Boolean,
    val statusCode: Int,
    val contentType: String = "",
    val contentLength: Long = 0L,
    val acceptsRanges: Boolean = false,
    val finalUrl: String = "",
    val errorMessage: String? = null
)
