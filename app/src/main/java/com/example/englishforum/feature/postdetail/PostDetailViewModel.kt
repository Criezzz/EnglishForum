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
import com.example.englishforum.data.auth.UserSession
import com.example.englishforum.data.auth.UserSessionRepository
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
    private val aiPracticeRepository: AiPracticeRepository,
    private val userSessionRepository: UserSessionRepository
) : ViewModel() {

    private val isLoading = MutableStateFlow(true)
    private var hasLoadedInitialPost = false
    private val errorMessage = MutableStateFlow<String?>(null)
    private val aiPracticeChecking = MutableStateFlow(false)
    private val userMessage = MutableStateFlow<String?>(null)
    private val isProcessingAction = MutableStateFlow(false)
    private val postDeleted = MutableStateFlow(false)
    private val isRefreshing = MutableStateFlow(false)
    private val commentDraft = MutableStateFlow("")
    private val replyTarget = MutableStateFlow<CommentReplyTargetUi?>(null)
    private val isSubmittingComment = MutableStateFlow(false)
    private val newlyPostedCommentId = MutableStateFlow<String?>(null)

    private val postStream = repository.observePost(postId)
        .onEach { post ->
            if (post != null) {
                hasLoadedInitialPost = true
                isLoading.value = false
            } else if (hasLoadedInitialPost) {
                isLoading.value = false
            }
        }

    private val baseInputs = combine(
        postStream,
        userSessionRepository.sessionFlow,
        isLoading,
        errorMessage,
        aiPracticeChecking
    ) { post, session, loading, error, aiChecking ->
        BaseStateInputs(
            post = post,
            session = session,
            isLoading = loading,
            errorMessage = error,
            isAiChecking = aiChecking
        )
    }

    private val baseState = combine(
        baseInputs,
        isRefreshing,
        commentDraft,
        isSubmittingComment,
        replyTarget
    ) { inputs, refreshing, draft, submittingComment, target ->
        val post = inputs.post
        val postUi = post?.toUiModel()
        val commentUi = if (post != null) {
            post.comments.flatMapIndexed { index, comment ->
                comment.toUiModel(
                    postAuthorName = post.authorName,
                    depth = 0,
                    isFirstChild = index == 0,
                    isLastChild = index == (post.comments.size - 1)
                )
            }
        } else {
            emptyList()
        }
        val isOwner = post?.let { detail ->
            inputs.session?.let { user ->
                detail.authorId.equals(user.userId, ignoreCase = true) ||
                    detail.authorId.equals(user.username, ignoreCase = true)
            } ?: false
        } ?: false

        PostDetailUiState(
            isLoading = inputs.isLoading,
            isRefreshing = refreshing,
            post = postUi,
            comments = commentUi,
            errorMessage = inputs.errorMessage,
            isAiPracticeChecking = inputs.isAiChecking,
            isCurrentUserPostOwner = isOwner,
            commentComposer = CommentComposerUi(
                draft = draft,
                isSubmitting = submittingComment,
                replyTarget = target
            )
        )
    }

    val uiState: StateFlow<PostDetailUiState> = combine(
        baseState,
        userMessage,
        isProcessingAction,
        postDeleted,
        newlyPostedCommentId
    ) { base, message, processing, deleted, newCommentId ->
        base.copy(
            userMessage = message,
            isPerformingAction = processing,
            isPostDeleted = deleted,
            newlyPostedCommentId = newCommentId
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

    fun onReportPost(reason: String) {
        if (isProcessingAction.value) return
        viewModelScope.launch {
            isProcessingAction.value = true
            errorMessage.value = null
            try {
                val result = repository.reportPost(postId, reason)
                result.onSuccess {
                    userMessage.value = "Đã gửi báo cáo cho bài viết."
                }
                result.onFailure { throwable ->
                    errorMessage.value = throwable.message ?: "Không thể gửi báo cáo."
                }
            } catch (throwable: Throwable) {
                errorMessage.value = throwable.message ?: "Không thể gửi báo cáo."
            } finally {
                isProcessingAction.value = false
            }
        }
    }

    fun onDeletePost() {
        if (isProcessingAction.value) return
        viewModelScope.launch {
            isProcessingAction.value = true
            errorMessage.value = null
            try {
                val result = repository.deletePost(postId)
                result.onSuccess {
                    postDeleted.value = true
                }
                result.onFailure { throwable ->
                    errorMessage.value = throwable.message ?: "Không thể xoá bài viết."
                }
            } catch (throwable: Throwable) {
                errorMessage.value = throwable.message ?: "Không thể xoá bài viết."
            } finally {
                isProcessingAction.value = false
            }
        }
    }

    fun onUserMessageShown() {
        userMessage.value = null
    }

    fun onPostDeletionHandled() {
        postDeleted.value = false
    }

    fun onPostUpdatedExternally() {
        userMessage.value = "Đã cập nhật bài viết."
    }

    fun onRefresh() {
        if (isRefreshing.value) return
        viewModelScope.launch {
            isRefreshing.value = true
            errorMessage.value = null
            try {
                val result = repository.refreshPost(postId)
                if (result.isFailure) {
                    errorMessage.value = result.exceptionOrNull()?.message ?: "Không thể tải lại bài viết."
                }
            } catch (throwable: Throwable) {
                errorMessage.value = throwable.message ?: "Không thể tải lại bài viết."
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun onCommentDraftChanged(text: String) {
        commentDraft.value = text
    }

    fun onSubmitComment() {
        val originalDraft = commentDraft.value
        val trimmedDraft = originalDraft.trim()
        if (trimmedDraft.isEmpty() || isSubmittingComment.value) {
            if (trimmedDraft.isEmpty() && originalDraft != trimmedDraft) {
                commentDraft.value = trimmedDraft
            }
            return
        }

        viewModelScope.launch {
            isSubmittingComment.value = true
            errorMessage.value = null
            
            // Store existing comment IDs before submission
            val existingCommentIds = uiState.value.comments.map { it.id }.toSet()
            val targetId = replyTarget.value?.commentId
            
            try {
                val result = repository.addComment(postId, trimmedDraft, targetId)
                result.onSuccess {
                    commentDraft.value = ""
                    replyTarget.value = null
                    
                    // Wait a bit for the post to refresh and find the new comment
                    kotlinx.coroutines.delay(300)
                    val newComments = uiState.value.comments
                    
                    // Find the comment that wasn't in the list before
                    val newComment = newComments.firstOrNull { it.id !in existingCommentIds }
                    if (newComment != null) {
                        newlyPostedCommentId.value = newComment.id
                    }
                }
                result.onFailure { throwable ->
                    errorMessage.value = throwable.message ?: "Không thể đăng bình luận."
                }
            } catch (throwable: Throwable) {
                errorMessage.value = throwable.message ?: "Không thể đăng bình luận."
            } finally {
                isSubmittingComment.value = false
            }
        }
    }

    fun onCancelReplyTarget() {
        replyTarget.value = null
    }

    fun onReplyToComment(commentId: String, authorName: String) {
        replyTarget.value = CommentReplyTargetUi(
            commentId = commentId,
            authorName = authorName
        )
    }

    fun onNewCommentHighlightShown() {
        newlyPostedCommentId.value = null
    }
}

class PostDetailViewModelFactory(
    private val postId: String,
    private val repository: PostDetailRepository = FakePostDetailRepository(),
    private val aiPracticeRepository: AiPracticeRepository = FakeAiPracticeRepository(),
    private val userSessionRepository: UserSessionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PostDetailViewModel(postId, repository, aiPracticeRepository, userSessionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private data class BaseStateInputs(
    val post: ForumPostDetail?,
    val session: UserSession?,
    val isLoading: Boolean,
    val errorMessage: String?,
    val isAiChecking: Boolean
)

private fun ForumPostDetail.toUiModel(): PostDetailUi {
    return PostDetailUi(
        id = id,
        authorId = authorId,
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
