package com.example.englishforum.data.aipractice

data class AiPracticeOption(
    val id: String,
    val label: String
)

data class AiPracticeQuestion(
    val id: String,
    val prompt: String,
    val options: List<AiPracticeOption>,
    val correctOptionId: String,
    val hint: String? = null
)

interface AiPracticeRepository {
    suspend fun checkFeasibility(postId: String): Result<Boolean>

    suspend fun loadQuestions(postId: String): Result<List<AiPracticeQuestion>>
}
