package com.example.englishforum.data.aipractice

data class AiPracticeOption(
    val id: String,
    val label: String
)

sealed interface AiPracticeQuestion {
    val id: String
    val prompt: String
    val hint: String?
}

data class AiPracticeMultipleChoiceQuestion(
    override val id: String,
    override val prompt: String,
    val options: List<AiPracticeOption>,
    val correctOptionId: String,
    override val hint: String? = null
) : AiPracticeQuestion

data class AiPracticeFillInBlankQuestion(
    override val id: String,
    override val prompt: String,
    val correctAnswer: String,
    val acceptedAnswers: List<String> = emptyList(),
    override val hint: String? = null
) : AiPracticeQuestion

interface AiPracticeRepository {
    suspend fun checkFeasibility(postId: String): Result<Boolean>

    suspend fun loadQuestions(postId: String): Result<List<AiPracticeQuestion>>
}
