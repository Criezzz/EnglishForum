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
        val currentState = _uiState.value
        if (!currentState.canSubmit || currentState.isSubmitting) return
        val selectedTag = currentState.selectedTag ?: return

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
            val attachments = currentState.attachments.map { CreatePostAttachment(it.id, it.label) }
            val imageUploads = currentState.processedImages.map { processed ->
                CreatePostImage(
                    originalUri = processed.originalUri,
                    file = processed.outputFile,
                    mimeType = processed.mimeType
                )
            }
            val result = repository.submitPost(
                title = currentState.title,
                body = currentState.body,
                attachments = attachments,
                images = imageUploads,
                tag = selectedTag
            )

            result.onSuccess { submitResult ->
                currentState.processedImages.forEach { processed ->
                    processed.outputFile.delete()
                }
                when (submitResult) {
                    is CreatePostResult.Success -> {
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
                    }
                    is CreatePostResult.Declined -> {
                        _uiState.update { state ->
                            state.copy(
                                isSubmitting = false,
                                declineReason = submitResult.reason,
                                successMessage = null
                            )
                        }
                    }
                }
            }
            result.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "Không thể đăng bài lúc này",
                        successMessage = null
                    )
                }
            }
        }
    }

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
