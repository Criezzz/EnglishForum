package com.example.englishforum.feature.postedit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.core.model.forum.PostTag
import com.example.englishforum.data.post.PostDetailRepository
import com.example.englishforum.data.post.PostAttachmentEdit
import com.example.englishforum.data.post.PostAttachmentState
import com.example.englishforum.data.post.PostAttachmentUpload
import com.example.englishforum.feature.create.CreateUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

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
    private var initialAttachments: List<EditablePostAttachment.Existing> = emptyList()
    private val currentAttachments = mutableListOf<EditablePostAttachment>()

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

                val resolvedAttachments = post.attachments
                    .map { attachment ->
                        EditablePostAttachment.Existing(
                            id = attachment.id,
                            originalIndex = attachment.index,
                            uri = Uri.parse(attachment.url),
                            mediaType = attachment.mediaType
                        )
                    }

                if (!hasAppliedInitialPost) {
                    initialAttachments = resolvedAttachments
                    currentAttachments.clear()
                    currentAttachments.addAll(resolvedAttachments)
                    _uiState.update { state ->
                        state.copy(
                            title = post.title,
                            body = post.body,
                            selectedTag = post.tag,
                            availableTags = tagOptions,
                            imageUris = currentAttachments.map { it.uri },
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
                currentAttachments.add(
                    EditablePostAttachment.New(
                        id = UUID.randomUUID().toString(),
                        uri = uri
                    )
                )
                state.copy(imageUris = currentAttachments.map { it.uri })
            }
        }
    }

    fun onRemoveImage(uri: Uri) {
        val removed = currentAttachments.indexOfFirst { it.uri == uri }
        if (removed >= 0) {
            currentAttachments.removeAt(removed)
            _uiState.update { state ->
                state.copy(imageUris = currentAttachments.map { it.uri })
            }
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
        val attachmentEdit = buildAttachmentEdit()

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
                attachmentEdits = attachmentEdit
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

    private fun buildAttachmentEdit(): PostAttachmentEdit? {
        if (!hasAppliedInitialPost) return null
        val current = currentAttachments.toList()
        val result = calculateAttachmentEdits(initialAttachments, current)
        return result
    }
}

private sealed interface EditablePostAttachment {
    val id: String
    val uri: Uri
    val mediaType: String?

    data class Existing(
        override val id: String,
        val originalIndex: Int,
        override val uri: Uri,
        override val mediaType: String?
    ) : EditablePostAttachment

    data class New(
        override val id: String,
        override val uri: Uri,
        override val mediaType: String? = null
    ) : EditablePostAttachment
}

private fun calculateAttachmentEdits(
    initial: List<EditablePostAttachment.Existing>,
    current: List<EditablePostAttachment>
): PostAttachmentEdit? {
    val initialById = initial.associateBy { it.id }
    val currentIds = current.filterIsInstance<EditablePostAttachment.Existing>().map { it.id }.toSet()

    val removed = initial.filter { it.id !in currentIds }
        .map { RemoveInstruction(index = it.originalIndex) }

    val moves = current.mapIndexedNotNull { index, attachment ->
        when (attachment) {
            is EditablePostAttachment.Existing -> {
                val original = initialById[attachment.id] ?: return@mapIndexedNotNull null
                if (original.originalIndex != index) {
                    MoveInstruction(originalIndex = original.originalIndex, targetIndex = index)
                } else {
                    null
                }
            }

            is EditablePostAttachment.New -> null
        }
    }

    val additions = current.mapIndexedNotNull { index, attachment ->
        if (attachment is EditablePostAttachment.New) {
            AddInstruction(index = index, upload = PostAttachmentUpload(uri = attachment.uri))
        } else {
            null
        }
    }

    val hasChanges = removed.isNotEmpty() || moves.isNotEmpty() || additions.isNotEmpty()

    val orderedInstructions = buildList {
        addAll(removed.sortedBy(RemoveInstruction::index).map { "remove ${it.index}" })
        addAll(moves.sortedBy(MoveInstruction::originalIndex).map { "move ${it.originalIndex} ${it.targetIndex}" })
        addAll(additions.sortedBy(AddInstruction::index).map { "add ${it.index}" })
    }

    val instructionString = orderedInstructions.joinToString(separator = ",")

    val newUploads = additions
        .sortedBy(AddInstruction::index)
        .map(AddInstruction::upload)

    val finalState = current.mapIndexed { index, attachment ->
        PostAttachmentState(
            id = (attachment as? EditablePostAttachment.Existing)?.id,
            uri = attachment.uri,
            isRemote = attachment is EditablePostAttachment.Existing,
            mediaType = attachment.mediaType
        )
    }

    if (!hasChanges) {
        return PostAttachmentEdit(
            instructions = "",
            newAttachments = emptyList(),
            finalState = finalState
        )
    }

    return PostAttachmentEdit(
        instructions = instructionString,
        newAttachments = newUploads,
        finalState = finalState
    )
}

private data class MoveInstruction(val originalIndex: Int, val targetIndex: Int)

private data class RemoveInstruction(val index: Int)

private data class AddInstruction(val index: Int, val upload: PostAttachmentUpload)

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
