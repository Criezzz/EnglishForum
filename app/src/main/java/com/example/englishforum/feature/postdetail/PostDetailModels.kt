package com.example.englishforum.feature.postdetail

import com.example.englishforum.core.model.VoteState

data class PostDetailUi(
    val id: String,
    val authorName: String,
    val relativeTimeText: String,
    val title: String,
    val body: String,
    val voteCount: Int,
    val voteState: VoteState,
    val commentCount: Int,
    val previewImageUrl: String? = null,
    val galleryImages: List<String>? = null
)

data class PostCommentUi(
    val id: String,
    val authorName: String,
    val relativeTimeText: String,
    val body: String,
    val voteCount: Int,
    val voteState: VoteState,
    val isAuthor: Boolean,
    val depth: Int,
    val hasReplies: Boolean,
    val isFirstChild: Boolean,
    val isLastChild: Boolean
)

data class PostDetailUiState(
    val isLoading: Boolean = true,
    val post: PostDetailUi? = null,
    val comments: List<PostCommentUi> = emptyList(),
    val errorMessage: String? = null,
    val isAiPracticeChecking: Boolean = false
) {
    val isEmpty: Boolean get() = !isLoading && post == null && comments.isEmpty()
}
