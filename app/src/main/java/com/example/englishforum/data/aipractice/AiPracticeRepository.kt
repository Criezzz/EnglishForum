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
    suspend fun loadQuestions(postContent: String): Result<List<AiPracticeQuestion>>
    
    suspend fun generateQuestions(postContent: String, type: String, numItems: Int): Result<List<AiPracticeQuestion>>
    
    // Cache management
    suspend fun getCachedQuestions(postContent: String, type: String, numItems: Int): List<AiPracticeQuestion>?
    
    suspend fun cacheQuestions(postContent: String, type: String, numItems: Int, questions: List<AiPracticeQuestion>)
    
    suspend fun clearCache()
}
