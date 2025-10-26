package com.example.englishforum.data.create.remote

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.englishforum.core.model.forum.PostTag
import com.example.englishforum.data.auth.UserSessionRepository
import com.example.englishforum.data.auth.bearerToken
import com.example.englishforum.data.create.CreatePostAttachment
import com.example.englishforum.data.create.CreatePostImage
import com.example.englishforum.data.create.CreatePostRepository
import com.example.englishforum.data.create.CreatePostResult
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException

internal class RemoteCreatePostRepository(
    private val api: CreatePostApi,
    private val userSessionRepository: UserSessionRepository,
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CreatePostRepository {

    override suspend fun submitPost(
        title: String,
        body: String,
        @Suppress("UNUSED_PARAMETER") attachments: List<CreatePostAttachment>,
        images: List<CreatePostImage>,
        tag: PostTag
    ): Result<CreatePostResult> {
        val session = userSessionRepository.sessionFlow.firstOrNull()
            ?: return Result.failure(IllegalStateException(SESSION_EXPIRED_MESSAGE))

        val trimmedTitle = title.trim()
        val trimmedBody = body.trim()
        if (trimmedTitle.isEmpty() || trimmedBody.isEmpty()) {
            return Result.failure(IllegalArgumentException(EMPTY_CONTENT_MESSAGE))
        }

        val tagValue = tag.serverValue
        val parts = withContext(ioDispatcher) {
            runCatching { buildAttachmentParts(images) }
        }.getOrElse { throwable ->
            return Result.failure(throwable)
        }

        val submission = withContext(ioDispatcher) {
            runCatching {
                api.uploadPost(
                    bearer = session.bearerToken(),
                    title = trimmedTitle.toRequestBody(PLAIN_TEXT_MEDIA_TYPE),
                    content = trimmedBody.toRequestBody(PLAIN_TEXT_MEDIA_TYPE),
                    tag = tagValue.toRequestBody(PLAIN_TEXT_MEDIA_TYPE),
                    attachments = parts
                )
            }
        }

        return submission
            .map { response ->
                val postId = response.postId?.takeIf { it > 0 }?.toString()
                CreatePostResult.Success(
                    postId = postId,
                    message = response.message.takeIf { it.isNotBlank() } ?: DEFAULT_SUCCESS_MESSAGE
                )
            }
            .mapFailure { it.toFriendlyException() }
    }

    private fun buildAttachmentParts(
        images: List<CreatePostImage>
    ): List<MultipartBody.Part> {
        if (images.isEmpty()) return emptyList()
        return images.mapIndexed { index, image ->
            val stream = contentResolver.openInputStream(image.uri)
                ?: throw IOException(MISSING_FILE_MESSAGE)
            stream.use { inputStream ->
                val bytes = inputStream.readBytes()
                if (bytes.isEmpty()) {
                    throw IOException(MISSING_FILE_MESSAGE)
                }
                val mediaType = resolveMimeType(image.uri)?.toMediaTypeOrNull() ?: FALLBACK_MEDIA_TYPE
                val fileName = image.displayName
                    ?: resolveDisplayName(image.uri)
                    ?: "attachment_${index + 1}.${resolveExtension(mediaType)}"
                val requestBody = bytes.toRequestBody(mediaType)
                MultipartBody.Part.createFormData(ATTACHMENT_FIELD_NAME, fileName, requestBody)
            }
        }
    }

    private fun resolveMimeType(uri: Uri): String? {
        val detected = contentResolver.getType(uri)
        if (!detected.isNullOrBlank()) return detected
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            ?.takeIf { it.isNotBlank() }
            ?.lowercase()
        return extension?.let { ext -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) cursor.getString(index) else null
                    } else {
                        null
                    }
                }
        }.getOrNull()
    }

    private fun resolveExtension(mediaType: MediaType?): String {
        if (mediaType == null) return DEFAULT_ATTACHMENT_EXTENSION
        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mediaType.toString())
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_ATTACHMENT_EXTENSION
    }

    private fun Throwable.toFriendlyException(): Throwable = when (this) {
        is HttpException -> {
            val message = parseErrorMessage(response()?.errorBody()?.string())
            IllegalStateException(message, this)
        }

        is SocketTimeoutException -> IOException(UPLOAD_TIMEOUT_MESSAGE, this)
        is UnknownHostException -> IOException(CONNECTION_ERROR_MESSAGE, this)
        is IOException -> IOException(CONNECTION_ERROR_MESSAGE, this)
        else -> this
    }

    private fun parseErrorMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return GENERIC_ERROR_MESSAGE
        return try {
            val json = JSONObject(raw)
            json.optString("detail", json.optString("message", raw))
        } catch (_: Exception) {
            raw
        }
    }

    private fun <T> Result<T>.mapFailure(transform: (Throwable) -> Throwable): Result<T> {
        return fold(
            onSuccess = { Result.success(it) },
            onFailure = { throwable -> Result.failure(transform(throwable)) }
        )
    }

    companion object {
        private val PLAIN_TEXT_MEDIA_TYPE = "text/plain".toMediaType()
        private const val SESSION_EXPIRED_MESSAGE = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
        private const val EMPTY_CONTENT_MESSAGE = "Tiêu đề và nội dung không được để trống"
        private const val CONNECTION_ERROR_MESSAGE = "Không thể kết nối tới máy chủ. Vui lòng kiểm tra lại mạng."
        private const val GENERIC_ERROR_MESSAGE = "Đã xảy ra lỗi, vui lòng thử lại"
        private const val DEFAULT_SUCCESS_MESSAGE = "Đăng bài thành công"
        private const val MISSING_FILE_MESSAGE = "Không thể đọc ảnh đã chọn"
        private const val ATTACHMENT_FIELD_NAME = "attachments"
        private const val DEFAULT_ATTACHMENT_EXTENSION = "bin"
        private val FALLBACK_MEDIA_TYPE = "application/octet-stream".toMediaType()
        private const val UPLOAD_TIMEOUT_MESSAGE = "Tải tệp mất quá lâu, vui lòng thử lại"
    }
}
