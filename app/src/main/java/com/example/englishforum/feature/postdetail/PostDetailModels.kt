package com.example.englishforum.feature.postdetail

import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.PostTag

data class PostDetailUi(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorUsername: String? = null,
    val authorAvatarUrl: String? = null,
    val relativeTimeText: String,
    val title: String,
    val body: String,
    val voteCount: Int,
    val voteState: VoteState,
    val commentCount: Int,
    val tag: PostTag,
    val previewImageUrl: String? = null,
    val galleryImages: List<String>? = null,
    val voteCountAnimationKey: Int = 0,
    val commentCountAnimationKey: Int = 0
)

data class PostCommentUi(
    val id: String,
    val authorName: String,
    val authorUsername: String? = null,
    val relativeTimeText: String,
    val body: String,
    val voteCount: Int,
    val voteState: VoteState,
    val isAuthor: Boolean,
    val isCurrentUserComment: Boolean = false,
    val depth: Int,
    val hasReplies: Boolean,
    val isFirstChild: Boolean,
    val isLastChild: Boolean,
    val isNew: Boolean = false
)

data class CommentReplyTargetUi(
    val commentId: String,
    val authorName: String,
    val authorUsername: String? = null
)

data class CommentComposerUi(
    val draft: String = "",
    val isSubmitting: Boolean = false,
    val replyTarget: CommentReplyTargetUi? = null
)

data class PostRealtimePromptUi(
    val commentIds: List<String>,
    val version: Int
) {
    val count: Int get() = commentIds.size
    val targetCommentId: String get() = commentIds.last()
}

data class PostDetailUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val post: PostDetailUi? = null,
    val comments: List<PostCommentUi> = emptyList(),
    val canLoadMoreComments: Boolean = false,
    val isLoadingMoreComments: Boolean = false,
    val errorMessage: String? = null,
    val isAiPracticeChecking: Boolean = false,
    val userMessage: String? = null,
    val isPerformingAction: Boolean = false,
    val isCurrentUserPostOwner: Boolean = false,
    val isPostDeleted: Boolean = false,
    val commentComposer: CommentComposerUi = CommentComposerUi(),
    val newlyPostedCommentId: String? = null,
    val realtimePrompt: PostRealtimePromptUi? = null
) {
    val isEmpty: Boolean get() = !isLoading && post == null && comments.isEmpty()
}
