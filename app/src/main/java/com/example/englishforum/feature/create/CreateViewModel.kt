package com.example.englishforum.feature.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.core.model.forum.PostTag
import com.example.englishforum.data.create.CreatePostAttachment
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
    val imageUris: List<Uri> = emptyList(),
    val availableTags: List<PostTag> = emptyList(),
    val selectedTag: PostTag? = null,
    val isSubmitting: Boolean = false,
    val declineReason: String? = null,
    val errorMessage: String? = null,
    val successPostId: String? = null
) {
    val canSubmit: Boolean get() = title.isNotBlank() && body.isNotBlank() && selectedTag != null && !isSubmitting
}

class CreateViewModel(
    private val repository: CreatePostRepository
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
        _uiState.update { state ->
            val maxImages = 5
            if (!state.imageUris.contains(uri) && state.imageUris.size < maxImages) {
                state.copy(imageUris = state.imageUris + uri)
            } else {
                state
            }
        }
    }

    fun onRemoveImage(uri: Uri) {
        _uiState.update { state ->
            state.copy(imageUris = state.imageUris.filterNot { it == uri })
        }
    }

    fun onSubmit() {
        val currentState = _uiState.value
        if (!currentState.canSubmit || currentState.isSubmitting) return
        val selectedTag = currentState.selectedTag ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, declineReason = null, successPostId = null) }
            val attachments = currentState.attachments.map { CreatePostAttachment(it.id, it.label) }
            val result = repository.submitPost(
                title = currentState.title,
                body = currentState.body,
                attachments = attachments,
                tag = selectedTag
            )

            result.onSuccess { submitResult ->
                when (submitResult) {
                    is CreatePostResult.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                isSubmitting = false,
                                title = "",
                                body = "",
                                attachments = emptyList(),
                                imageUris = emptyList(),
                                availableTags = tagOptions,
                                selectedTag = tagOptions.first(),
                                successPostId = submitResult.postId
                            )
                        }
                    }
                    is CreatePostResult.Declined -> {
                        _uiState.update { state ->
                            state.copy(
                                isSubmitting = false,
                                declineReason = submitResult.reason
                            )
                        }
                    }
                }
            }
            result.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "Không thể đăng bài lúc này"
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

    fun onTagSelected(tag: PostTag) {
        _uiState.update { state ->
            state.copy(selectedTag = tag)
        }
    }
}

class CreateViewModelFactory(
    private val repository: CreatePostRepository = FakeCreatePostRepository()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateViewModel::class.java)) {
            return CreateViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
