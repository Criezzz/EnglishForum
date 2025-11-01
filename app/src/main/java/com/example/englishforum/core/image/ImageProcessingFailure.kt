package com.example.englishforum.core.image

sealed class ImageProcessingFailure(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    data object SourceUnavailable : ImageProcessingFailure("Không thể đọc ảnh đã chọn")

    data object InvalidImage : ImageProcessingFailure("Ảnh không hợp lệ hoặc đã bị hỏng")

    class ProcessingError(cause: Throwable? = null) : ImageProcessingFailure(
        message = "Không thể xử lý ảnh, vui lòng thử lại",
        cause = cause
    )
}
