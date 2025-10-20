package com.example.englishforum.data.post.remote.model

import com.example.englishforum.data.home.remote.model.AttachmentResponse
import com.squareup.moshi.Json

internal data class PostDetailResponse(
    @Json(name = "post_id") val postId: Int,
    val title: String = "",
    @Json(name = "content") val body: String = "",
    @Json(name = "vote_count") val voteCount: Int = 0,
    @Json(name = "user_vote") val userVote: Int = 0,
    @Json(name = "comment_count") val commentCount: Int = 0,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "is_modified") val isModified: Boolean? = null,
    val tag: String? = null,
    @Json(name = "author_id") val authorId: Int? = null,
    @Json(name = "author_username") val authorUsername: String? = null,
    @Json(name = "author_display_name") val authorDisplayName: String? = null,
    @Json(name = "author_name") val authorFullName: String? = null,
    @Json(name = "author_avatar_url") val authorAvatarUrl: String? = null,
    val attachments: List<AttachmentResponse>? = null
)

internal data class PostCommentResponse(
    @Json(name = "comment_id") val commentId: Int? = null,
    @Json(name = "author_id") val authorId: Int? = null,
    @Json(name = "author_username") val authorUsername: String? = null,
    @Json(name = "author_display_name") val authorDisplayName: String? = null,
    @Json(name = "author_name") val authorFullName: String? = null,
    val content: String = "",
    @Json(name = "vote_count") val voteCount: Int = 0,
    @Json(name = "user_vote") val userVote: Int = 0,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "is_modified") val isModified: Boolean? = null,
    @Json(name = "reply_to_id") val replyToId: Int? = null
)
