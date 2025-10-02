package com.example.englishforum.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumProfilePost
import com.example.englishforum.core.model.forum.ForumProfileReply
import com.example.englishforum.core.model.forum.ForumUserProfile
import com.example.englishforum.data.profile.FakeProfileRepository
import com.example.englishforum.data.profile.ProfileRepository
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        repository.observeProfile(userId)
            .onEach { profile ->
                _uiState.value = profile.toUiState()
            }
            .launchIn(viewModelScope)
    }

    fun updateDisplayName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.updateDisplayName(userId, trimmed)
        }
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
            repository.setPostVote(userId, postId, targetState)
        }
    }

    private fun updateReplyVote(replyId: String, targetState: VoteState) {
        viewModelScope.launch {
            repository.setReplyVote(userId, replyId, targetState)
        }
    }

    private fun ForumUserProfile.toUiState(): ProfileUiState {
        return ProfileUiState(
            overview = ProfileOverview(
                displayName = displayName,
                avatarUrl = avatarUrl,
                stats = ProfileStats(
                    upvotes = stats.upvotes,
                    posts = stats.posts,
                    answers = stats.answers
                )
            ),
            posts = posts.map { it.toUiModel() },
            replies = replies.map { it.toUiModel() },
            isLoading = false
        )
    }

    private fun ForumProfilePost.toUiModel(): ProfilePost {
        return ProfilePost(
            id = id,
            title = title,
            body = body,
            minutesAgo = minutesAgo,
            voteCount = voteCount,
            voteState = voteState
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
            minutesAgo = minutesAgo,
            voteCount = voteCount,
            voteState = voteState
        )
    }
}

class ProfileViewModelFactory(
    private val repository: ProfileRepository = FakeProfileRepository(),
    private val userId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(repository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
