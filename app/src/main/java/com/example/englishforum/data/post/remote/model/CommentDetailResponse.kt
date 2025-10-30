package com.example.englishforum.data.post.remote.model

import com.squareup.moshi.Json

internal data class CommentDetailResponse(
    @Json(name = "comment_id") val commentId: Int,
    @Json(name = "post_id") val postId: Int,
    @Json(name = "post_author_id") val postAuthorId: Int? = null,
    @Json(name = "author_id") val authorId: Int? = null,
    @Json(name = "author_username") val authorUsername: String? = null,
    @Json(name = "author_display_name") val authorDisplayName: String? = null,
    @Json(name = "author_name") val authorFullName: String? = null,
    @Json(name = "author_avatar") val authorAvatar: String? = null,
    val content: String? = null,
    @Json(name = "vote_count") val voteCount: Int? = null,
    @Json(name = "user_vote") val userVote: Int? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "reply_to_id") val replyToId: Int? = null
)
