package com.example.englishforum.data.auth.remote

import com.example.englishforum.data.auth.remote.model.TokenResponse
import com.example.englishforum.data.auth.remote.model.MessageResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {
    @FormUrlEncoded
    @POST("/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): TokenResponse

    @FormUrlEncoded
    @POST("/register")
    suspend fun register(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("email") email: String
    ): TokenResponse

    @POST("/register/verify")
    suspend fun verifyEmail(
        @Header("Authorization") bearer: String,
        @Query("otp") otp: String
    ): MessageResponse

    @POST("/register/resend")
    suspend fun resendVerification(
        @Header("Authorization") bearer: String
    ): MessageResponse
}
