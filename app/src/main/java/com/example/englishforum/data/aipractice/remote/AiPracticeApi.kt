package com.example.englishforum.data.aipractice.remote

import com.example.englishforum.data.aipractice.remote.model.GenerateFromTextRequest
import com.example.englishforum.data.aipractice.remote.model.GenerateResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AiPracticeApi {
    @POST("/ai/generate-from-text")
    suspend fun generateFromText(
        @Header("Authorization") bearer: String,
        @Body request: GenerateFromTextRequest
    ): GenerateResponse
}
