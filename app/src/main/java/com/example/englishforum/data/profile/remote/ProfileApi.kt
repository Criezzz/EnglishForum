package com.example.englishforum.data.profile.remote

import com.example.englishforum.data.profile.remote.model.SimpleUserResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface ProfileApi {
    @GET("/users")
    suspend fun getCurrentUser(
        @Header("Authorization") bearer: String
    ): SimpleUserResponse
}
