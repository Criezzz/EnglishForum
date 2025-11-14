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
import com.example.englishforum.data.post.PostRealtimeEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log

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
    private val realtimePrompt = MutableStateFlow<PostRealtimePromptUi?>(null)
    private val newCommentIds = MutableStateFlow<Set<String>>(emptySet())
    
    // Track previous counts and animation keys for animation triggers
    private var previousVoteCount: Int? = null
    private var previousCommentCount: Int? = null
    private var voteAnimationKey = 0
    private var commentAnimationKey = 0

    init {
        viewModelScope.launch {
            repository.observeRealtimeEvents(postId).collect { event ->
                when (event) {
                    is PostRealtimeEvent.NewComment -> onRealtimeCommentReceived(event.commentId)
                }
            }
        }
    }

    private val postStream = repository.observePost(postId)
        .onEach { post ->
            if (post != null) {
                hasLoadedInitialPost = true
                isLoading.value = false
                // Observe AI generation status for this post content
                val postContent = "${post.title}\n\n${post.body}"
                viewModelScope.launch {
                    aiPracticeRepository.observeGeneration(postContent, "mcq", 3, post.id).collect { isGen ->
                        aiPracticeChecking.value = isGen
                    }
                }
            } else {
                // Post is null - either doesn't exist or was deleted
                // If we've loaded initial post before, it was deleted
                // If we haven't loaded initial post, it doesn't exist
                // In both cases, stop loading after a brief delay to allow initial check
                if (hasLoadedInitialPost) {
                    isLoading.value = false
                } else {
                    // For non-existent posts, set loading to false after initial check
                    // This allows UI to show "not found" message
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(100) // Brief delay to ensure initial state is set
                        isLoading.value = false
                    }
                }
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
        BaseStateOutput(
            inputs = inputs,
            refreshing = refreshing,
            draft = draft,
            submittingComment = submittingComment,
            target = target
        )
    }
    .combine(newCommentIds) { baseOutput, newIds ->
        val inputs = baseOutput.inputs
        val post = inputs.post
        
        // Detect count changes for animations
        val animateVoteKey = post?.let { currentPost ->
            val changed = previousVoteCount != null && 
                         previousVoteCount != currentPost.voteCount
            previousVoteCount = currentPost.voteCount
            if (changed) {
                voteAnimationKey++
            }
            voteAnimationKey
        } ?: 0
        
        val animateCommentKey = post?.let { currentPost ->
            val changed = previousCommentCount != null && 
                         previousCommentCount != currentPost.commentCount
            previousCommentCount = currentPost.commentCount
            if (changed) {
                commentAnimationKey++
            }
            commentAnimationKey
        } ?: 0
        
        val postUi = post?.toUiModel(
            voteCountAnimationKey = animateVoteKey,
            commentCountAnimationKey = animateCommentKey
        )
        val currentUserId = inputs.session?.userId
        val currentUsername = inputs.session?.username
        val commentUi = if (post != null) {
            post.comments.flatMapIndexed { index, comment ->
                comment.toUiModel(
                    postAuthorName = post.authorName,
                    currentUserId = currentUserId,
                    currentUsername = currentUsername,
                    depth = 0,
                    isFirstChild = index == 0,
                    isLastChild = index == (post.comments.size - 1),
                    newCommentIds = newIds
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
            isRefreshing = baseOutput.refreshing,
            post = postUi,
            comments = commentUi,
            errorMessage = inputs.errorMessage,
            isAiPracticeChecking = inputs.isAiChecking,
            isCurrentUserPostOwner = isOwner,
            commentComposer = CommentComposerUi(
                draft = baseOutput.draft,
                isSubmitting = baseOutput.submittingComment,
                replyTarget = baseOutput.target
            )
        )
    }

    val uiState: StateFlow<PostDetailUiState> = baseState
        .combine(userMessage) { base, message ->
            base.copy(userMessage = message)
        }
        .combine(isProcessingAction) { state, processing ->
            state.copy(isPerformingAction = processing)
        }
        .combine(postDeleted) { state, deleted ->
            state.copy(isPostDeleted = deleted)
        }
        .combine(newlyPostedCommentId) { state, newCommentId ->
            state.copy(newlyPostedCommentId = newCommentId)
        }
        .combine(realtimePrompt) { state, prompt ->
            state.copy(realtimePrompt = prompt)
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
        Log.d("PostDetail", "AI Practice button clicked")
        val currentPost = uiState.value.post ?: return

        // Navigate immediately to show loading state
        onAvailable(currentPost.id)
        
        // Start background generation that persists beyond this screen
        val postContent = "${currentPost.title}\n\n${currentPost.body}"
        aiPracticeRepository.startGeneration(postContent, "mcq", 3, currentPost.id)
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
                    realtimePrompt.value = null
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

    fun onReplyToComment(
        commentId: String,
        authorName: String,
        authorUsername: String?
    ) {
        replyTarget.value = CommentReplyTargetUi(
            commentId = commentId,
            authorName = authorName,
            authorUsername = authorUsername
        )
    }

    fun onNewCommentHighlightShown() {
        newlyPostedCommentId.value = null
    }

    fun onRealtimePromptDismissed(version: Int) {
        val current = realtimePrompt.value
        if (current?.version == version) {
            realtimePrompt.value = null
        }
    }

    fun onRealtimePromptRevealed(version: Int) {
        val current = realtimePrompt.value
        if (current?.version != version) return
        realtimePrompt.value = null
        val targetId = current.commentIds.lastOrNull() ?: return
        newlyPostedCommentId.value = targetId
    }

    private fun onRealtimeCommentReceived(commentId: String) {
        // Add to new comment IDs set
        newCommentIds.update { it + commentId }
    }
    
    fun onCommentViewed(commentId: String) {
        // Remove from new comment IDs set when viewed
        newCommentIds.update { it - commentId }
    }

    fun onEditComment(commentId: String, newContent: String) {
        if (isProcessingAction.value) return
        viewModelScope.launch {
            isProcessingAction.value = true
            errorMessage.value = null
            try {
                val result = repository.updateComment(postId, commentId, newContent)
                result.onSuccess {
                    userMessage.value = "Đã cập nhật bình luận."
                }
                result.onFailure { throwable ->
                    errorMessage.value = throwable.message ?: "Không thể cập nhật bình luận."
                }
            } catch (throwable: Throwable) {
                errorMessage.value = throwable.message ?: "Không thể cập nhật bình luận."
            } finally {
                isProcessingAction.value = false
            }
        }
    }

    fun onDeleteComment(commentId: String) {
        if (isProcessingAction.value) return
        viewModelScope.launch {
            isProcessingAction.value = true
            errorMessage.value = null
            try {
                val result = repository.deleteComment(postId, commentId)
                result.onSuccess {
                    userMessage.value = "Đã xoá bình luận."
                }
                result.onFailure { throwable ->
                    errorMessage.value = throwable.message ?: "Không thể xoá bình luận."
                }
            } catch (throwable: Throwable) {
                errorMessage.value = throwable.message ?: "Không thể xoá bình luận."
            } finally {
                isProcessingAction.value = false
            }
        }
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

private data class BaseStateOutput(
    val inputs: BaseStateInputs,
    val refreshing: Boolean,
    val draft: String,
    val submittingComment: Boolean,
    val target: CommentReplyTargetUi?
)

private fun ForumPostDetail.toUiModel(
    voteCountAnimationKey: Int = 0,
    commentCountAnimationKey: Int = 0
): PostDetailUi {
    return PostDetailUi(
        id = id,
        authorId = authorId,
        authorName = authorName,
        authorUsername = authorUsername,
        authorAvatarUrl = authorAvatarUrl,
        relativeTimeText = formatRelativeTime(minutesAgo),
        title = title,
        body = body,
        voteCount = voteCount,
        voteState = voteState,
        commentCount = commentCount,
        tag = tag,
        previewImageUrl = previewImageUrl,
        galleryImages = galleryImages,
        voteCountAnimationKey = voteCountAnimationKey,
        commentCountAnimationKey = commentCountAnimationKey
    )
}

private fun ForumComment.toUiModel(
    postAuthorName: String,
    currentUserId: String?,
    currentUsername: String?,
    depth: Int,
    isFirstChild: Boolean,
    isLastChild: Boolean,
    newCommentIds: Set<String>
): List<PostCommentUi> {
    // Determine if this comment belongs to the current user
    val isCurrentUserComment = when {
        // Primary check: Match by authorId (numeric ID from backend)
        currentUserId != null && authorId != null && 
            authorId.equals(currentUserId, ignoreCase = true) -> true
        
        // Secondary check: Match by username
        currentUsername != null && authorUsername != null && 
            authorUsername.equals(currentUsername, ignoreCase = true) -> true
        
        // Tertiary check: authorUsername might match userId (fallback)
        currentUserId != null && authorUsername != null && 
            authorUsername.equals(currentUserId, ignoreCase = true) -> true
        
        else -> false
    }
    
    val commentUi = PostCommentUi(
        id = id,
        authorName = authorName,
        authorUsername = authorUsername,
        relativeTimeText = formatRelativeTime(minutesAgo),
        body = body,
        voteCount = voteCount,
        voteState = voteState,
        isAuthor = isAuthor || authorName.equals(postAuthorName, ignoreCase = true),
        isCurrentUserComment = isCurrentUserComment,
        depth = depth,
        hasReplies = replies.isNotEmpty(),
        isFirstChild = isFirstChild,
        isLastChild = isLastChild,
        isNew = id in newCommentIds
    )

    if (replies.isEmpty()) {
        return listOf(commentUi)
    }

    val nestedReplies = replies.flatMapIndexed { index, reply ->
        reply.toUiModel(
            postAuthorName = postAuthorName,
            currentUserId = currentUserId,
            currentUsername = currentUsername,
            depth = depth + 1,
            isFirstChild = index == 0,
            isLastChild = index == replies.lastIndex,
            newCommentIds = newCommentIds
        )
    }

    return listOf(commentUi) + nestedReplies
}
