package com.example.englishforum.data.create.remote

import com.example.englishforum.data.create.remote.model.CreatePostResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

internal interface CreatePostApi {
    @Multipart
    @POST("/posts/upload")
    suspend fun uploadPost(
        @Header("Authorization") bearer: String,
        @Part("title") title: RequestBody,
        @Part("content") content: RequestBody,
        @Part("tag") tag: RequestBody,
        @Part attachments: List<MultipartBody.Part>
    ): CreatePostResponse
}
