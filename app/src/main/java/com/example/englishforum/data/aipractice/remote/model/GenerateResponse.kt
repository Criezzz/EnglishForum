package com.example.englishforum.data.aipractice.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateResponse(
    @Json(name = "topic")
    val topic: String? = null,
    @Json(name = "items")
    val items: List<AiGeneratedItem>,
    @Json(name = "isAskable")
    val isAskable: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class AiGeneratedItem(
    @Json(name = "type")
    val type: String,
    @Json(name = "question")
    val question: AiGeneratedQuestion,
    @Json(name = "answer")
    val answer: String? = null, // For fill-in-blank
    @Json(name = "correctOptionId")
    val correctOptionId: String? = null, // For MCQ
    @Json(name = "hint")
    val hint: String? = null
)

@JsonClass(generateAdapter = true)
data class AiGeneratedQuestion(
    @Json(name = "id")
    val id: String? = null,
    @Json(name = "prompt")
    val prompt: String,
    @Json(name = "options")
    val options: List<AiGeneratedOption>? = null
)

@JsonClass(generateAdapter = true)
data class AiGeneratedOption(
    @Json(name = "id")
    val id: String,
    @Json(name = "label")
    val label: String
)
