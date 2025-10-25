package com.example.englishforum.data.profile.remote

import com.example.englishforum.data.auth.remote.model.MessageResponse
import com.example.englishforum.data.profile.remote.model.SimpleUserResponse
import com.example.englishforum.data.profile.remote.model.UserCommentResponse
import com.example.englishforum.data.profile.remote.model.UserPostResponse
import okhttp3.MultipartBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ProfileApi {
    @GET("/user")
    suspend fun getCurrentUser(
        @Header("Authorization") bearer: String
    ): SimpleUserResponse

    @GET("/user/{username}")
    suspend fun getUserByUsername(
        @Header("Authorization") bearer: String,
        @Path("username") username: String
    ): SimpleUserResponse

    @GET("/user/{username}/posts")
    suspend fun getUserPosts(
        @Header("Authorization") bearer: String,
        @Path("username") username: String
    ): List<UserPostResponse>

    @GET("/user/{username}/comments")
    suspend fun getUserComments(
        @Header("Authorization") bearer: String,
        @Path("username") username: String
    ): List<UserCommentResponse>

    @FormUrlEncoded
    @PUT("/user/bio")
    suspend fun updateBio(
        @Header("Authorization") bearer: String,
        @Field("bio") bio: String
    ): MessageResponse

    @FormUrlEncoded
    @PUT("/user/username")
    suspend fun updateUsername(
        @Header("Authorization") bearer: String,
        @Field("username") username: String
    ): MessageResponse

    @Multipart
    @PUT("/user/avatar")
    suspend fun updateAvatar(
        @Header("Authorization") bearer: String,
        @Part newAvatar: MultipartBody.Part
    ): MessageResponse

    @FormUrlEncoded
    @PUT("/user/password")
    suspend fun updatePassword(
        @Header("Authorization") bearer: String,
        @Field("password") currentPassword: String,
        @Field("new_password") newPassword: String
    ): MessageResponse

    @FormUrlEncoded
    @PUT("/user/email")
    suspend fun updateEmail(
        @Header("Authorization") bearer: String,
        @Field("email") newEmail: String
    ): MessageResponse

    @FormUrlEncoded
    @retrofit2.http.POST("/user/email/confirm")
    suspend fun confirmEmailUpdate(
        @Header("Authorization") bearer: String,
        @Field("otp") otp: String
    ): MessageResponse
}
