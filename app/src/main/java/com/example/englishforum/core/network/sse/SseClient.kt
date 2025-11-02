package com.example.englishforum.core.network.sse

import java.io.Closeable
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

interface SseConnection : Closeable {
    override fun close()
}

data class SseMessage(
    val data: String?,
    val event: String?,
    val id: String?
)

interface SseListener {
    fun onOpen() {}

    fun onMessage(message: SseMessage)

    fun onClosed() {}

    fun onFailure(cause: Throwable) {}
}

interface SseClient {
    fun open(
        path: String,
        headers: Map<String, String> = emptyMap(),
        queryParameters: Map<String, String> = emptyMap(),
        listener: SseListener
    ): SseConnection
}

class OkHttpSseClient(
    private val baseUrl: String,
    private val okHttpClient: OkHttpClient
) : SseClient {

    override fun open(
        path: String,
        headers: Map<String, String>,
        queryParameters: Map<String, String>,
        listener: SseListener
    ): SseConnection {
        val httpUrl = buildHttpUrl(path, queryParameters)
        val requestBuilder = Request.Builder()
            .url(httpUrl)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")

        headers.forEach { (name, value) ->
            requestBuilder.addHeader(name, value)
        }

        val eventSourceListener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                listener.onOpen()
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                listener.onMessage(
                    SseMessage(
                        data = data,
                        event = type,
                        id = id
                    )
                )
            }

            override fun onClosed(eventSource: EventSource) {
                listener.onClosed()
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                listener.onFailure(t ?: IllegalStateException("SSE connection failure"))
            }
        }

        val eventSourceFactory = EventSources.createFactory(okHttpClient)
        val eventSource = eventSourceFactory.newEventSource(
            requestBuilder.build(),
            eventSourceListener
        )

        return object : SseConnection {
            override fun close() {
                eventSource.cancel()
            }
        }
    }

    private fun buildHttpUrl(
        path: String,
        queryParameters: Map<String, String>
    ): HttpUrl {
        val baseHttpUrl = baseUrl.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Invalid base URL: $baseUrl")
        val builder = baseHttpUrl.newBuilder()

        val sanitizedPath = path.trim()
        if (sanitizedPath.isNotEmpty()) {
            sanitizedPath.trim('/').split('/')
                .filter { it.isNotBlank() }
                .forEach { segment -> builder.addPathSegment(segment) }
        }

        queryParameters.forEach { (name, value) ->
            builder.addQueryParameter(name, value)
        }

        return builder.build()
    }
}
