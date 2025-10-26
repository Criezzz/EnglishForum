package com.example.englishforum.data.post.remote

import com.example.englishforum.data.auth.remote.model.MessageResponse
import com.example.englishforum.data.post.remote.model.PostCommentResponse
import com.example.englishforum.data.post.remote.model.PostDetailResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.FormUrlEncoded

internal interface PostDetailApi {
    @GET("/posts/{post_id}")
    suspend fun getPostDetail(
        @Header("Authorization") bearer: String,
        @Path("post_id") postId: Int
    ): PostDetailResponse

    @GET("/posts/{post_id}/comments")
    suspend fun getPostComments(
        @Header("Authorization") bearer: String,
        @Path("post_id") postId: Int,
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null
    ): List<PostCommentResponse>

    @FormUrlEncoded
    @POST("/posts/{post_id}/vote")
    suspend fun votePost(
        @Header("Authorization") bearer: String,
        @Path("post_id") postId: Int,
        @Field("vote_type") voteType: Int
    ): MessageResponse

    @FormUrlEncoded
    @POST("/posts/{post_id}/comments")
    suspend fun postComment(
        @Header("Authorization") bearer: String,
        @Path("post_id") postId: Int,
        @Field("content") content: String,
        @Query("reply_comment_id") replyCommentId: Int? = null
    ): MessageResponse

    @POST("/comments/{comment_id}/vote")
    suspend fun voteComment(
        @Header("Authorization") bearer: String,
        @Path("comment_id") commentId: Int,
        @Query("vote_type") voteType: Int
    ): MessageResponse

    @DELETE("/posts/{post_id}")
    suspend fun deletePost(
        @Header("Authorization") bearer: String,
        @Path("post_id") postId: Int
    ): MessageResponse

    @Multipart
    @PUT("/posts/{post_id}")
    suspend fun updatePost(
        @Header("Authorization") bearer: String,
        @Path("post_id") postId: Int,
        @Part("title") title: RequestBody,
        @Part("content") content: RequestBody,
        @Part("tag") tag: RequestBody,
        @Part("attachments_update") attachmentsUpdate: RequestBody?,
        @Part attachments: List<MultipartBody.Part>
    ): MessageResponse

    @FormUrlEncoded
    @PUT("/comments/{comment_id}")
    suspend fun updateComment(
        @Header("Authorization") bearer: String,
        @Path("comment_id") commentId: Int,
        @Field("content") content: String
    ): MessageResponse

    @DELETE("/comments/{comment_id}")
    suspend fun deleteComment(
        @Header("Authorization") bearer: String,
        @Path("comment_id") commentId: Int
    ): MessageResponse
}
