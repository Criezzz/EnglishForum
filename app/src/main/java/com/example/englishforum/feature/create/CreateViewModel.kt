package com.example.englishforum.feature.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isSubmitting: Boolean = false,
    val declineReason: String? = null,
    val errorMessage: String? = null,
    val successPostId: String? = null
) {
    val canSubmit: Boolean get() = title.isNotBlank() && body.isNotBlank() && !isSubmitting
}

class CreateViewModel(
    private val repository: CreatePostRepository = FakeCreatePostRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateUiState())
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

    fun onSubmit() {
        val currentState = _uiState.value
        if (!currentState.canSubmit || currentState.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, declineReason = null, successPostId = null) }
            val attachments = currentState.attachments.map { CreatePostAttachment(it.id, it.label) }
            val result = repository.submitPost(
                title = currentState.title,
                body = currentState.body,
                attachments = attachments
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
}
