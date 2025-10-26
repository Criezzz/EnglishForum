package com.example.englishforum.feature.postedit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.core.model.forum.PostTag
import com.example.englishforum.data.post.PostDetailRepository
import com.example.englishforum.feature.create.CreateUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostEditViewModel(
    private val postId: String,
    private val repository: PostDetailRepository
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
            selectedTag = tagOptions.firstOrNull(),
            isInitialLoading = true
        )
    )
    val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()

    private var hasAppliedInitialPost = false

    init {
        viewModelScope.launch {
            repository.observePost(postId).collect { post ->
                if (post == null) {
                    if (!hasAppliedInitialPost) {
                        _uiState.update {
                            it.copy(
                                isInitialLoading = false,
                                errorMessage = "Không tìm thấy bài viết"
                            )
                        }
                    }
                    return@collect
                }

                val imageUris = buildList {
                    val uniqueSources = linkedSetOf<String>()
                    post.previewImageUrl?.let { uniqueSources.add(it) }
                    post.galleryImages?.forEach { uniqueSources.add(it) }
                    uniqueSources.mapTo(this) { Uri.parse(it) }
                }

                if (!hasAppliedInitialPost) {
                    _uiState.update { state ->
                        state.copy(
                            title = post.title,
                            body = post.body,
                            selectedTag = post.tag,
                            availableTags = tagOptions,
                            imageUris = imageUris,
                            isInitialLoading = false,
                            errorMessage = null
                        )
                    }
                    hasAppliedInitialPost = true
                }
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onBodyChange(newBody: String) {
        _uiState.update { it.copy(body = newBody) }
    }

    fun onTagSelected(tag: PostTag) {
        _uiState.update { it.copy(selectedTag = tag) }
    }

    fun onImageSelected(uri: Uri) {
        _uiState.update { state ->
            val maxImages = 5
            if (state.imageUris.contains(uri) || state.imageUris.size >= maxImages) {
                state
            } else {
                state.copy(imageUris = state.imageUris + uri)
            }
        }
    }

    fun onRemoveImage(uri: Uri) {
        _uiState.update { state ->
            state.copy(imageUris = state.imageUris.filterNot { it == uri })
        }
    }

    fun onAddAttachment() {
        // Attachments are not editable in the current flow
    }

    fun onRemoveAttachment(@Suppress("UNUSED_PARAMETER") attachmentId: String) {
        // Attachments are not editable in the current flow
    }

    fun onDeclineDialogDismissed() {
        _uiState.update { it.copy(declineReason = null) }
    }

    fun onErrorMessageConsumed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onSubmit() {
        val currentState = _uiState.value
        val selectedTag = currentState.selectedTag ?: return
        if (!currentState.canSubmit || currentState.isSubmitting) return

        val trimmedTitle = currentState.title.trim()
        val trimmedBody = currentState.body.trim()
        val galleryImageUrls = currentState.imageUris.map { it.toString() }
        val previewImageUrl = galleryImageUrls.firstOrNull()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    errorMessage = null,
                    successMessage = null
                )
            }
            val result = repository.updatePost(
                postId = postId,
                title = trimmedTitle,
                body = trimmedBody,
                tag = selectedTag,
                previewImageUrl = previewImageUrl,
                galleryImageUrls = galleryImageUrls
            )
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        successPostId = postId,
                        successMessage = POST_UPDATE_SUCCESS_MESSAGE
                    )
                }
            }
            result.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "Không thể cập nhật bài viết",
                        successMessage = null
                    )
                }
            }
        }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(successPostId = null) }
    }

    fun onSuccessMessageConsumed() {
        _uiState.update { it.copy(successMessage = null) }
    }

    companion object {
        private const val POST_UPDATE_SUCCESS_MESSAGE = "Đã cập nhật bài viết"
    }
}

class PostEditViewModelFactory(
    private val postId: String,
    private val repository: PostDetailRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostEditViewModel::class.java)) {
            return PostEditViewModel(postId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
