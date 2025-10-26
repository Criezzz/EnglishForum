package com.example.englishforum.data.post.remote.model

import com.example.englishforum.BuildConfig
import com.squareup.moshi.Json
import java.util.Locale

/**
 * Raw attachment metadata returned by backend responses.
 */
data class AttachmentResponse(
    @Json(name = "media_url") val mediaUrl: String? = null,
    @Json(name = "media_filename") val mediaFilename: String? = null,
    @Json(name = "media_type") val mediaType: String? = null,
    @Json(name = "media_metadata") val mediaMetadata: String? = null,
    val index: Int? = null
)

/**
 * Resolve the first media url suitable for thumbnail preview.
 */
fun List<AttachmentResponse>?.resolvePreviewUrl(): String? {
    if (this.isNullOrEmpty()) return null
    return this
        .sortedWith(compareBy<AttachmentResponse> { it.index ?: Int.MAX_VALUE })
        .firstNotNullOfOrNull { it.resolveMediaUrl() }
}

/**
 * Resolve all media urls keeping backend ordering.
 */
fun List<AttachmentResponse>?.resolveGalleryUrls(): List<String>? {
    if (this.isNullOrEmpty()) return null
    val urls = this
        .sortedWith(compareBy<AttachmentResponse> { it.index ?: Int.MAX_VALUE })
        .mapNotNull { it.resolveMediaUrl() }
    return urls.takeIf { it.isNotEmpty() }
}

private fun AttachmentResponse.resolveMediaUrl(): String? {
    if (!isRenderableImage()) return null

    val explicitUrl = mediaUrl?.takeIf { it.isNotBlank() }
    if (explicitUrl != null) {
        return explicitUrl.normalizeDownloadUrl()
    }

    val filename = mediaFilename?.takeIf { it.isNotBlank() } ?: return null
    val normalizedBase = BuildConfig.API_BASE_URL.trimEnd('/')
    return "$normalizedBase/download/$filename"
}

private fun AttachmentResponse.isRenderableImage(): Boolean {
    val normalizedType = mediaType?.lowercase(Locale.ROOT)
    if (normalizedType != null && normalizedType.startsWith("image/")) {
        return true
    }

    val sanitizedFilename = mediaFilename?.lowercase(Locale.ROOT) ?: return false
    return IMAGE_EXTENSIONS.any { sanitizedFilename.endsWith(it) }
}

private fun String.normalizeDownloadUrl(): String {
    val trimmed = trim()
    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        return trimmed
    }
    val normalizedBase = BuildConfig.API_BASE_URL.trimEnd('/')
    return when {
        trimmed.startsWith("/download") -> "$normalizedBase$trimmed"
        trimmed.startsWith("download/") -> "$normalizedBase/$trimmed"
        trimmed.startsWith("/") -> "$normalizedBase$trimmed"
        else -> "$normalizedBase/download/$trimmed"
    }
}

private val IMAGE_EXTENSIONS = listOf(
    ".jpg",
    ".jpeg",
    ".png",
    ".gif",
    ".webp"
)
