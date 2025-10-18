package com.example.englishforum.data.profile.remote

import com.example.englishforum.data.auth.remote.model.MessageResponse
import com.example.englishforum.data.profile.remote.model.SimpleUserResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
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
}
