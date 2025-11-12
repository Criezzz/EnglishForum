package com.example.englishforum.feature.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.core.image.ImageProcessingFailure
import com.example.englishforum.core.image.ImageProcessingTarget
import com.example.englishforum.core.image.ImageProcessor
import com.example.englishforum.core.image.ProcessedImage
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumProfilePost
import com.example.englishforum.core.model.forum.ForumProfileReply
import com.example.englishforum.core.model.forum.ForumUserProfile
import com.example.englishforum.data.profile.ProfileAvatarImage
import com.example.englishforum.data.profile.ProfileRepository
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

internal const val MAX_PROFILE_BIO_LENGTH = 280
private const val AVATAR_PROCESSING_GENERIC_ERROR = "Không thể xử lý ảnh đại diện đã chọn"

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val userId: String,
    private val imageProcessor: ImageProcessor
) : ViewModel() {

    private val _editState = MutableStateFlow<ProfileEditState>(ProfileEditState.Idle)
    val editState: StateFlow<ProfileEditState> = _editState.asStateFlow()

    private val _avatarState = MutableStateFlow(ProfileAvatarUiState())
    val avatarState: StateFlow<ProfileAvatarUiState> = _avatarState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private var avatarUploadJob: Job? = null
    private var pendingAvatar: ProcessedImage? = null

    val uiState: StateFlow<ProfileUiState> = combine(
        repository.observeProfile(userId),
        _isRefreshing,
        _errorMessage
    ) { profile, isRefreshing, errorMessage ->
        profile.toUiState(isRefreshing = isRefreshing, errorMessage = errorMessage)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState()
    )

    fun updateDisplayName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.updateDisplayName(userId, trimmed)
        }
    }

    fun updateBio(newBio: String) {
        val sanitized = newBio.trim()
        viewModelScope.launch {
            repository.updateBio(userId, sanitized)
        }
    }

    fun onAvatarSelected(uri: Uri) {
        avatarUploadJob?.cancel()
        avatarUploadJob = viewModelScope.launch {
            try {
                _avatarState.value = ProfileAvatarUiState(
                    previewUri = null,
                    isProcessing = true,
                    isUploading = false,
                    errorMessage = null
                )
                pendingAvatar?.outputFile?.delete()
                pendingAvatar = null

                val result = imageProcessor.process(uri, ImageProcessingTarget.Avatar)
                result.onSuccess { processed ->
                    pendingAvatar = processed
                    _avatarState.value = ProfileAvatarUiState(
                        previewUri = processed.previewUri,
                        isProcessing = false,
                        isUploading = false,
                        errorMessage = null
                    )
                }
                result.onFailure { throwable ->
                    val message = when (throwable) {
                        is ImageProcessingFailure -> throwable.message
                        else -> throwable.message
                    } ?: AVATAR_PROCESSING_GENERIC_ERROR
                    _avatarState.value = ProfileAvatarUiState(
                        previewUri = null,
                        isProcessing = false,
                        isUploading = false,
                        errorMessage = message
                    )
                }
            } finally {
                avatarUploadJob = null
            }
        }
    }

    fun updateProfile(newName: String, newBio: String) {
        val trimmedName = newName.trim()
        val sanitizedBio = newBio.trim()
        val currentOverview = uiState.value.overview
        val avatarPayload = pendingAvatar

        viewModelScope.launch {
            val nameChanged = currentOverview?.displayName != trimmedName
            val bioChanged = currentOverview?.bio.orEmpty() != sanitizedBio
            val avatarChanged = avatarPayload != null

            if (!nameChanged && !bioChanged && !avatarChanged) {
                _editState.value = ProfileEditState.Success
                return@launch
            }

            if (trimmedName.isEmpty()) {
                _editState.value = ProfileEditState.Error("Display name cannot be empty")
                return@launch
            }

            if (bioChanged && sanitizedBio.isEmpty()) {
                _editState.value = ProfileEditState.Error("Bio cannot be empty")
                return@launch
            }

            if (bioChanged && sanitizedBio.containsUnsupportedCharacters()) {
                _editState.value = ProfileEditState.Error("Bio cannot include unsupported characters like emoji")
                return@launch
            }

            if (bioChanged && sanitizedBio.length > MAX_PROFILE_BIO_LENGTH) {
                _editState.value = ProfileEditState.Error("Bio cannot exceed $MAX_PROFILE_BIO_LENGTH characters")
                return@launch
            }

            _editState.value = ProfileEditState.InProgress

            var failure: Throwable? = null
            
            // Upload avatar first if changed
            if (avatarChanged && failure == null) {
                _avatarState.value = _avatarState.value.copy(
                    isUploading = true,
                    errorMessage = null
                )
                val avatarResult = repository.updateAvatar(
                    userId,
                    ProfileAvatarImage(
                        originalUri = avatarPayload!!.originalUri,
                        file = avatarPayload.outputFile,
                        mimeType = avatarPayload.mimeType
                    )
                )
                failure = avatarResult.exceptionOrNull()
                if (failure != null) {
                    _avatarState.value = _avatarState.value.copy(
                        isUploading = false,
                        errorMessage = failure.message ?: "Could not update avatar"
                    )
                } else {
                    avatarPayload.outputFile.delete()
                    pendingAvatar = null
                    _avatarState.value = ProfileAvatarUiState()
                }
            }
            
            if (nameChanged && failure == null) {
                val nameResult = repository.updateDisplayName(userId, trimmedName)
                failure = nameResult.exceptionOrNull()
            }
            if (failure == null && bioChanged) {
                val bioResult = repository.updateBio(userId, sanitizedBio)
                failure = bioResult.exceptionOrNull()
            }

            if (failure != null) {
                val message = failure.message ?: "Could not update profile"
                _editState.value = ProfileEditState.Error(message)
            } else {
                _editState.value = ProfileEditState.Success
            }
        }
    }

    fun resetEditState() {
        _editState.value = ProfileEditState.Idle
    }

    fun resetAvatarState() {
        avatarUploadJob?.cancel()
        avatarUploadJob = null
        pendingAvatar?.outputFile?.delete()
        pendingAvatar = null
        _avatarState.value = ProfileAvatarUiState()
    }

    override fun onCleared() {
        pendingAvatar?.outputFile?.delete()
        pendingAvatar = null
        super.onCleared()
    }

    fun onPostUpvote(postId: String) {
        updatePostVote(postId, VoteState.UPVOTED)
    }

    fun onPostDownvote(postId: String) {
        updatePostVote(postId, VoteState.DOWNVOTED)
    }

    fun onReplyUpvote(replyId: String) {
        updateReplyVote(replyId, VoteState.UPVOTED)
    }

    fun onReplyDownvote(replyId: String) {
        updateReplyVote(replyId, VoteState.DOWNVOTED)
    }

    private fun updatePostVote(postId: String, targetState: VoteState) {
        viewModelScope.launch {
            _errorMessage.value = null
            val result = repository.setPostVote(userId, postId, targetState)
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Không thể cập nhật lượt bình chọn"
            }
        }
    }

    private fun updateReplyVote(replyId: String, targetState: VoteState) {
        viewModelScope.launch {
            _errorMessage.value = null
            val result = repository.setReplyVote(userId, replyId, targetState)
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Không thể cập nhật lượt bình chọn"
            }
        }
    }

    fun onErrorShown() {
        _errorMessage.value = null
    }

    fun onRefresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.refresh(userId)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun ForumUserProfile.toUiState(isRefreshing: Boolean = false, errorMessage: String? = null): ProfileUiState {
        return ProfileUiState(
            overview = ProfileOverview(
                displayName = displayName,
                avatarUrl = avatarUrl,
                bio = bio,
                stats = ProfileStats(
                    upvotes = stats.upvotes,
                    posts = stats.posts,
                    answers = stats.answers
                )
            ),
            posts = posts.map { it.toUiModel() },
            replies = replies.map { it.toUiModel() },
            isLoading = false,
            isRefreshing = isRefreshing,
            errorMessage = errorMessage
        )
    }

    private fun ForumProfilePost.toUiModel(): ProfilePost {
        return ProfilePost(
            id = id,
            title = title,
            body = body,
            timeLabel = timestampLabel,
            voteCount = voteCount,
            voteState = voteState,
            previewImageUrl = previewImageUrl
        )
    }

    private fun ForumProfileReply.toUiModel(): ProfileReply {
        val capitalizedTitle = questionTitle.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
        }
        return ProfileReply(
            id = id,
            postId = postId,
            questionTitle = capitalizedTitle,
            body = body,
            timeLabel = timestampLabel,
            voteCount = voteCount,
            voteState = voteState
        )
    }
}

private fun String.containsUnsupportedCharacters(): Boolean = any { Character.isSurrogate(it) }

class ProfileViewModelFactory(
    private val repository: ProfileRepository,
    private val userId: String,
    private val imageProcessor: ImageProcessor
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(repository, userId, imageProcessor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
