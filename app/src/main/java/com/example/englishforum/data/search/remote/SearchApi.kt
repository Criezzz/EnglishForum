package com.example.englishforum.data.search.remote

import com.example.englishforum.data.search.remote.model.SearchResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SearchApi {
    @GET("/search")
    suspend fun search(
        @Header("Authorization") bearer: String,
        @Query("keyword") keyword: String
    ): SearchResponse
}
