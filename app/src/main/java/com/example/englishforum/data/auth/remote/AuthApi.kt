package com.example.englishforum.data.auth.remote

import com.example.englishforum.data.auth.remote.model.MessageResponse
import com.example.englishforum.data.auth.remote.model.RefreshTokenResponse
import com.example.englishforum.data.auth.remote.model.ResetTokenResponse
import com.example.englishforum.data.auth.remote.model.TokenResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

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

    @FormUrlEncoded
    @POST("/register/verify")
    suspend fun verifyEmail(
        @Header("Authorization") bearer: String,
        @Field("otp") otp: String
    ): MessageResponse

    @POST("/register/resend")
    suspend fun resendVerification(
        @Header("Authorization") bearer: String
    ): MessageResponse

    @FormUrlEncoded
    @POST("/recover")
    suspend fun requestPasswordRecovery(
        @Field("username") contact: String
    ): MessageResponse

    @FormUrlEncoded
    @POST("/recover/verify")
    suspend fun verifyRecoveryOtp(
        @Field("otp") otp: String,
        @Field("username") contact: String
    ): ResetTokenResponse

    @FormUrlEncoded
    @POST("/reset")
    suspend fun resetPassword(
        @Field("reset_token") resetToken: String,
        @Field("new_password") newPassword: String
    ): MessageResponse

    @FormUrlEncoded
    @POST("/refresh")
    suspend fun refreshAccessToken(
        @Field("refresh_token") refreshToken: String
    ): RefreshTokenResponse
}
