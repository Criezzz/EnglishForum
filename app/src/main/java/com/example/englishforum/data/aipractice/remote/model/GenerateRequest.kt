package com.example.englishforum.data.aipractice.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateFromTextRequest(
    @Json(name = "context_text")
    val contextText: String,
    @Json(name = "type")
    val type: String,
    @Json(name = "num_items")
    val numItems: Int,
    @Json(name = "mode")
    val mode: String = "cot"
)
