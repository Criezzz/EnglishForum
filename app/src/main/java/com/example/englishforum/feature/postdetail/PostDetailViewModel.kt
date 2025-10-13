package com.example.englishforum.feature.postdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.core.common.formatRelativeTime
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumComment
import com.example.englishforum.core.model.forum.ForumPostDetail
import com.example.englishforum.data.aipractice.AiPracticeRepository
import com.example.englishforum.data.aipractice.FakeAiPracticeRepository
import com.example.englishforum.data.post.FakePostDetailRepository
import com.example.englishforum.data.post.PostDetailRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PostDetailViewModel(
    private val postId: String,
    private val repository: PostDetailRepository,
    private val aiPracticeRepository: AiPracticeRepository
) : ViewModel() {

    private val isLoading = MutableStateFlow(true)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val aiPracticeChecking = MutableStateFlow(false)

    private val postStream = repository.observePost(postId)
        .onEach { isLoading.value = false }

    val uiState: StateFlow<PostDetailUiState> = combine(
        postStream,
        isLoading,
        errorMessage,
        aiPracticeChecking
    ) { post, loading, error, aiChecking ->
        val postUi = post?.toUiModel()
        val commentUi = post?.comments?.flatMapIndexed { index, comment ->
            comment.toUiModel(
                postAuthorName = post.authorName,
                depth = 0,
                isFirstChild = index == 0,
                isLastChild = index == (post.comments.size - 1)
            )
        } ?: emptyList()

        PostDetailUiState(
            isLoading = loading,
            post = postUi,
            comments = commentUi,
            errorMessage = error,
            isAiPracticeChecking = aiChecking
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = PostDetailUiState()
        )

    fun onUpvotePost() {
        updatePostVote(VoteState.UPVOTED)
    }

    fun onDownvotePost() {
        updatePostVote(VoteState.DOWNVOTED)
    }

    fun onUpvoteComment(commentId: String) {
        updateCommentVote(commentId, VoteState.UPVOTED)
    }

    fun onDownvoteComment(commentId: String) {
        updateCommentVote(commentId, VoteState.DOWNVOTED)
    }

    fun onAiPracticeClick(onAvailable: (String) -> Unit) {
        val currentPostId = uiState.value.post?.id ?: return
        if (aiPracticeChecking.value) return

        viewModelScope.launch {
            aiPracticeChecking.value = true
            errorMessage.value = null

            try {
                val result = aiPracticeRepository.checkFeasibility(currentPostId)
                result.onSuccess { available ->
                    if (available) {
                        onAvailable(currentPostId)
                    } else {
                        errorMessage.value = "Tính năng luyện tập AI hiện chưa khả dụng cho bài viết này."
                    }
                }
                result.onFailure { throwable ->
                    errorMessage.value = throwable.message ?: "Không thể kiểm tra tính năng luyện tập AI."
                }
            } catch (throwable: Throwable) {
                errorMessage.value = throwable.message ?: "Không thể kiểm tra tính năng luyện tập AI."
            } finally {
                aiPracticeChecking.value = false
            }
        }
    }

    private fun updatePostVote(target: VoteState) {
        viewModelScope.launch {
            errorMessage.value = null
            val result = repository.setPostVote(postId, target)
            if (result.isFailure) {
                errorMessage.value = result.exceptionOrNull()?.message ?: "Không thể cập nhật lượt bình chọn"
            }
        }
    }

    private fun updateCommentVote(commentId: String, target: VoteState) {
        viewModelScope.launch {
            errorMessage.value = null
            val result = repository.setCommentVote(postId, commentId, target)
            if (result.isFailure) {
                errorMessage.value = result.exceptionOrNull()?.message ?: "Không thể cập nhật bình chọn cho bình luận"
            }
        }
    }
}

class PostDetailViewModelFactory(
    private val postId: String,
    private val repository: PostDetailRepository = FakePostDetailRepository(),
    private val aiPracticeRepository: AiPracticeRepository = FakeAiPracticeRepository()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PostDetailViewModel(postId, repository, aiPracticeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private fun ForumPostDetail.toUiModel(): PostDetailUi {
    return PostDetailUi(
        id = id,
        authorName = authorName,
        relativeTimeText = formatRelativeTime(minutesAgo),
        title = title,
        body = body,
        voteCount = voteCount,
        voteState = voteState,
        commentCount = commentCount,
        previewImageUrl = previewImageUrl,
        galleryImages = galleryImages
    )
}

private fun ForumComment.toUiModel(
    postAuthorName: String,
    depth: Int,
    isFirstChild: Boolean,
    isLastChild: Boolean
): List<PostCommentUi> {
    val commentUi = PostCommentUi(
        id = id,
        authorName = authorName,
        relativeTimeText = formatRelativeTime(minutesAgo),
        body = body,
        voteCount = voteCount,
        voteState = voteState,
        isAuthor = isAuthor || authorName.equals(postAuthorName, ignoreCase = true),
        depth = depth,
        hasReplies = replies.isNotEmpty(),
        isFirstChild = isFirstChild,
        isLastChild = isLastChild
    )

    if (replies.isEmpty()) {
        return listOf(commentUi)
    }

    val nestedReplies = replies.flatMapIndexed { index, reply ->
        reply.toUiModel(
            postAuthorName = postAuthorName,
            depth = depth + 1,
            isFirstChild = index == 0,
            isLastChild = index == replies.lastIndex
        )
    }

    return listOf(commentUi) + nestedReplies
}
