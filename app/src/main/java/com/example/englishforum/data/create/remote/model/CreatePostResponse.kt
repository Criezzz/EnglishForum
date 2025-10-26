package com.example.englishforum.data.create.remote.model

import com.squareup.moshi.Json

data class CreatePostResponse(
    val message: String,
    @Json(name = "post_id") val postId: Int?
)
