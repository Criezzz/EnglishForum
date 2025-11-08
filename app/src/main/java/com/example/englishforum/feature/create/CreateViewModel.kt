package com.example.englishforum.feature.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.core.model.forum.PostTag
import com.example.englishforum.core.image.ImageProcessingFailure
import com.example.englishforum.core.image.ImageProcessingTarget
import com.example.englishforum.core.image.ImageProcessor
import com.example.englishforum.core.image.ProcessedImage
import com.example.englishforum.data.create.CreatePostAttachment
import com.example.englishforum.data.create.CreatePostImage
import com.example.englishforum.data.create.CreatePostRepository
import com.example.englishforum.data.create.CreatePostResult
import com.example.englishforum.data.create.FakeCreatePostRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.net.UnknownHostException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.io.IOException

data class CreateAttachmentUi(
    val id: String,
    val label: String
)

data class CreateUiState(
    val title: String = "",
    val body: String = "",
    val attachments: List<CreateAttachmentUi> = emptyList(),
    val processedImages: List<ProcessedImage> = emptyList(),
    val imageUris: List<Uri> = emptyList(),
    val availableTags: List<PostTag> = emptyList(),
    val selectedTag: PostTag? = null,
    val isSubmitting: Boolean = false,
    val isImageProcessing: Boolean = false,
    val declineReason: String? = null,
    val errorMessage: String? = null,
    val successPostId: String? = null,
    val successMessage: String? = null,
    val isInitialLoading: Boolean = false
) {
    val canSubmit: Boolean
        get() = title.isNotBlank() &&
            body.isNotBlank() &&
            selectedTag != null &&
            !isSubmitting &&
            !isImageProcessing &&
            !isInitialLoading
}

class CreateViewModel(
    private val repository: CreatePostRepository,
    private val imageProcessor: ImageProcessor
) : ViewModel() {

    private val tagOptions = listOf(
        PostTag.AskQuestion,
        PostTag.Tutorial,
        PostTag.Resource,
        PostTag.Experience
    )

    private val _uiState = MutableStateFlow(
        CreateUiState(
            availableTags = tagOptions,
            selectedTag = tagOptions.first()
        )
    )
    val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onBodyChange(newBody: String) {
        _uiState.update { it.copy(body = newBody) }
    }

    fun onAddAttachment() {
        _uiState.update { state ->
            val nextIndex = state.attachments.size + 1
            val attachment = CreateAttachmentUi(
                id = UUID.randomUUID().toString(),
                label = "Ảnh $nextIndex"
            )
            state.copy(attachments = state.attachments + attachment)
        }
    }

    fun onRemoveAttachment(attachmentId: String) {
        _uiState.update { state ->
            state.copy(attachments = state.attachments.filterNot { it.id == attachmentId })
        }
    }

    fun onImageSelected(uri: Uri) {
        val currentState = _uiState.value
        if (currentState.processedImages.size >= MAX_IMAGES) {
            _uiState.update { state ->
                state.copy(errorMessage = IMAGE_LIMIT_MESSAGE)
            }
            return
        }
        if (currentState.isImageProcessing) {
            return
        }
        if (currentState.processedImages.any { it.originalUri == uri }) {
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isImageProcessing = true,
                    errorMessage = null
                )
            }

            try {
                val result = imageProcessor.process(uri, ImageProcessingTarget.PostImage)
                result.onSuccess { processed ->
                    _uiState.update { state ->
                        val updated = (state.processedImages + processed).take(MAX_IMAGES)
                        state.copy(
                            processedImages = updated,
                            imageUris = updated.map { it.previewUri },
                            isImageProcessing = false
                        )
                    }
                }
                result.onFailure { throwable ->
                    val message = when (throwable) {
                        is ImageProcessingFailure -> throwable.message
                        else -> throwable.message
                    } ?: IMAGE_PROCESSING_GENERIC_ERROR
                    _uiState.update { state ->
                        state.copy(
                            errorMessage = message
                        )
                    }
                }
            } finally {
                _uiState.update { state ->
                    if (!state.isImageProcessing) state else state.copy(isImageProcessing = false)
                }
            }
        }
    }

    fun onRemoveImage(uri: Uri) {
        _uiState.update { state ->
            val (removed, remaining) = state.processedImages.partition { it.previewUri == uri }
            removed.forEach { processed -> processed.outputFile.delete() }
            state.copy(
                processedImages = remaining,
                imageUris = remaining.map { it.previewUri }
            )
        }
    }

    fun onSubmit() {
        val initialState = _uiState.value
        if (!initialState.canSubmit || initialState.isSubmitting) return
        val selectedTag = initialState.selectedTag ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    errorMessage = null,
                    declineReason = null,
                    successPostId = null,
                    successMessage = null
                )
            }
            // Lấy trạng thái mới nhất mỗi attempt để người dùng chỉnh sửa trong lúc retry vẫn được áp dụng
            var attempt = 0
            var lastFailure: Throwable? = null
            while (attempt < MAX_RETRY_ATTEMPTS) {
                val currentState = _uiState.value
                val attachments = currentState.attachments.map { CreatePostAttachment(it.id, it.label) }
                val imageUploads = currentState.processedImages.map { processed ->
                    CreatePostImage(
                        originalUri = processed.originalUri,
                        file = processed.outputFile,
                        mimeType = processed.mimeType
                    )
                }

                val result: Result<CreatePostResult> = try {
                    repository.submitPost(
                        title = currentState.title,
                        body = currentState.body,
                        attachments = attachments,
                        images = imageUploads,
                        tag = selectedTag
                    )
                } catch (t: Throwable) {
                    Result.failure(t)
                }

                var handled = false
                result.onSuccess { submitResult ->
                    when (submitResult) {
                        is CreatePostResult.Success -> {
                            // Chỉ dọn file ảnh khi thành công
                            currentState.processedImages.forEach { it.outputFile.delete() }
                            _uiState.update { state ->
                                val postId = submitResult.postId
                                val feedback = submitResult.message.ifBlank { DEFAULT_SUCCESS_MESSAGE }
                                state.copy(
                                    isSubmitting = false,
                                    title = "",
                                    body = "",
                                    attachments = emptyList(),
                                    processedImages = emptyList(),
                                    imageUris = emptyList(),
                                    availableTags = tagOptions,
                                    selectedTag = tagOptions.first(),
                                    successPostId = postId,
                                    successMessage = if (postId == null) feedback else null
                                )
                            }
                            handled = true
                        }
                        is CreatePostResult.Declined -> {
                            // KHÔNG dọn ảnh khi bị từ chối để người dùng chỉnh sửa và gửi lại
                            _uiState.update { state ->
                                state.copy(
                                    isSubmitting = false,
                                    declineReason = submitResult.reason,
                                    successMessage = null
                                )
                            }
                            handled = true
                        }
                    }
                }
                result.onFailure { throwable -> lastFailure = throwable }

                if (handled) return@launch

                if (lastFailure != null && isNetworkError(lastFailure!!) && attempt < MAX_RETRY_ATTEMPTS - 1) {
                    delay(RETRY_DELAY_MS)
                    attempt++
                    continue
                } else {
                    // Không dọn ảnh khi lỗi mạng hoặc lỗi khác: giữ nguyên để người dùng thử lại
                    val message = if (lastFailure != null && isNetworkError(lastFailure!!)) NETWORK_ERROR_MESSAGE else lastFailure?.message ?: GENERIC_SUBMIT_ERROR
                    _uiState.update { state ->
                        state.copy(
                            isSubmitting = false,
                            errorMessage = message,
                            successMessage = null
                        )
                    }
                    return@launch
                }
            }
        }
    }

    private fun isNetworkError(t: Throwable): Boolean =
        t is UnknownHostException ||
            t is ConnectException ||
            t is SocketTimeoutException ||
            (t is IOException && t.message?.contains("Failed to connect", ignoreCase = true) == true)

    fun onDeclineReasonDismissed() {
        _uiState.update { it.copy(declineReason = null) }
    }

    fun onErrorMessageDisplayed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(successPostId = null) }
    }

    fun onSuccessMessageDisplayed() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun onTagSelected(tag: PostTag) {
        _uiState.update { state ->
            state.copy(selectedTag = tag)
        }
    }

    override fun onCleared() {
        _uiState.value.processedImages.forEach { processed ->
            processed.outputFile.delete()
        }
        super.onCleared()
    }

    companion object {
        private const val DEFAULT_SUCCESS_MESSAGE = "Đăng bài thành công"
        private const val IMAGE_PROCESSING_GENERIC_ERROR = "Không thể xử lý ảnh đã chọn"
        private const val MAX_IMAGES = 5
        private const val IMAGE_LIMIT_MESSAGE = "Đã đạt giới hạn tối đa 5 ảnh"
        private const val NETWORK_ERROR_MESSAGE = "Không có kết nối"
        private const val GENERIC_SUBMIT_ERROR = "Không thể đăng bài lúc này"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L // tổng ~2s chờ thêm
    }
}

class CreateViewModelFactory(
    private val repository: CreatePostRepository = FakeCreatePostRepository(),
    private val imageProcessor: ImageProcessor = object : ImageProcessor {
        override suspend fun process(
            uri: Uri,
            target: ImageProcessingTarget
        ): Result<ProcessedImage> = Result.failure(UnsupportedOperationException("Image processing not available"))
    }
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateViewModel::class.java)) {
            return CreateViewModel(repository, imageProcessor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
