package com.example.englishforum.data.home.remote

import com.example.englishforum.data.auth.remote.model.MessageResponse
import com.example.englishforum.data.home.remote.model.FeedPostResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PostsApi {
    @GET("/")
    suspend fun getFeed(
        @Header("Authorization") bearer: String,
        @Query("criteria") criteria: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("cursor") cursor: String? = null
    ): List<FeedPostResponse>

    @FormUrlEncoded
    @POST("/posts/{post_id}/vote")
    suspend fun votePost(
        @Header("Authorization") bearer: String,
        @Path("post_id") postId: Int,
        @Field("vote_type") voteType: Int
    ): MessageResponse
}
