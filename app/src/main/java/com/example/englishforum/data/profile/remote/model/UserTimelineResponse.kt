package com.example.englishforum.data.profile.remote.model

import com.example.englishforum.data.post.remote.model.AttachmentResponse
import com.squareup.moshi.Json

data class UserPostResponse(
    @Json(name = "post_id") val postId: Int,
    val title: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "vote_count") val voteCount: Int? = null,
    @Json(name = "user_vote") val userVote: Int? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    val tag: String? = null,
    @Json(name = "author_username") val authorUsername: String? = null,
    @Json(name = "author_avatar") val authorAvatar: String? = null,
    val attachments: List<AttachmentResponse>? = null
)

data class UserCommentResponse(
    @Json(name = "comment_id") val commentId: Int,
    @Json(name = "post_id") val postId: Int? = null,
    @Json(name = "reply_to_id") val replyToId: Int? = null,
    val content: String? = null,
    @Json(name = "vote_count") val voteCount: Int? = null,
    @Json(name = "user_vote") val userVote: Int? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "author_username") val authorUsername: String? = null,
    @Json(name = "author_avatar") val authorAvatar: String? = null
)
