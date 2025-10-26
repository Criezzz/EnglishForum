package com.example.englishforum.data.aipractice.remote

import com.example.englishforum.data.aipractice.AiPracticeFillInBlankQuestion
import com.example.englishforum.data.aipractice.AiPracticeMultipleChoiceQuestion
import com.example.englishforum.data.aipractice.AiPracticeOption
import com.example.englishforum.data.aipractice.AiPracticeQuestion
import com.example.englishforum.data.aipractice.AiPracticeRepository
import com.example.englishforum.data.aipractice.remote.model.GenerateFromTextRequest
import com.example.englishforum.data.auth.UserSessionRepository
import com.example.englishforum.data.auth.bearerToken
import kotlinx.coroutines.flow.firstOrNull
import retrofit2.HttpException
import java.io.IOException
import android.util.Log

class RemoteAiPracticeRepository(
    private val aiPracticeApi: AiPracticeApi,
    private val userSessionRepository: UserSessionRepository
) : AiPracticeRepository {
    
    // In-memory cache: Map<CacheKey, List<AiPracticeQuestion>>
    private val cache = mutableMapOf<String, List<AiPracticeQuestion>>()
    
    private fun generateCacheKey(postContent: String, type: String, numItems: Int): String {
        return "${postContent.hashCode()}_${type}_${numItems}"
    }


    override suspend fun loadQuestions(postContent: String): Result<List<AiPracticeQuestion>> {
        // This method is kept for backward compatibility
        // In the new implementation, we'll use generateQuestions instead
        // Generate 3 MCQ questions only
        return generateQuestions(postContent, "mcq", 3)
    }

    override suspend fun generateQuestions(postContent: String, type: String, numItems: Int): Result<List<AiPracticeQuestion>> {
        return try {
            Log.d("AiPractice", "generateQuestions called: type=$type, numItems=$numItems, content length=${postContent.length}")
            
            // Check cache first
            val cachedQuestions = getCachedQuestions(postContent, type, numItems)
            if (cachedQuestions != null) {
                Log.d("AiPractice", "Found cached questions: ${cachedQuestions.size} questions")
                return Result.success(cachedQuestions)
            }
            
            val session = userSessionRepository.sessionFlow.firstOrNull()
            if (session == null) {
                Log.e("AiPractice", "User not authenticated")
                return Result.failure(IllegalStateException("User not authenticated"))
            }

            val request = GenerateFromTextRequest(
                contextText = postContent,
                type = type,
                numItems = numItems
            )

            Log.d("AiPractice", "Sending generate request: $request")
            val response = aiPracticeApi.generateFromText(session.bearerToken(), request)
            Log.d("AiPractice", "Received generate response: items count=${response.items.size}, isAskable=${response.isAskable}")
            Log.d("AiPractice", "Full generate response: $response")
            
            // Check if the post is askable for AI practice
            val isAskable = response.isAskable ?: (response.items.isNotEmpty())
            if (!isAskable) {
                Log.d("AiPractice", "Post is not askable, returning failure")
                return Result.failure(IllegalStateException("AI practice is not available for this post"))
            }
            
            val questions = response.items.mapIndexed { index, item ->
                val questionId = item.question.id ?: "generated-${index}"
                Log.d("AiPractice", "Mapping item $index: type=${item.type}, prompt=${item.question.prompt.take(50)}...")
                when (item.type.lowercase()) {
                    "mcq" -> {
                        val options = item.question.options?.map { option ->
                            AiPracticeOption(
                                id = option.id,
                                label = option.label
                            )
                        } ?: emptyList()
                        
                        AiPracticeMultipleChoiceQuestion(
                            id = questionId,
                            prompt = item.question.prompt,
                            options = options,
                            correctOptionId = item.correctOptionId ?: "",
                            hint = item.hint
                        )
                    }
                    "fill" -> {
                        Log.d("AiPractice", "Created Fill: id=$questionId, answer=${item.answer}")
                        AiPracticeFillInBlankQuestion(
                            id = questionId,
                            prompt = item.question.prompt,
                            correctAnswer = item.answer ?: "",
                   
                            hint = item.hint
                        )
                    }
                    else -> {
                        // Default to multiple choice if type is unknown
                        val options = item.question.options?.map { option ->
                            AiPracticeOption(
                                id = option.id,
                                label = option.label
                            )
                        } ?: emptyList()
                        
                        AiPracticeMultipleChoiceQuestion(
                            id = questionId,
                            prompt = item.question.prompt,
                            options = options,
                            correctOptionId = item.correctOptionId ?: "",
                            hint = item.hint
                        )
                    }
                }
            }

            // Cache the generated questions
            cacheQuestions(postContent, type, numItems, questions)
            
            Result.success(questions)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Result.failure(IllegalArgumentException("Post not found"))
                422 -> Result.failure(IllegalArgumentException("Invalid request format or parameters: ${e.message}"))
                500 -> Result.failure(IllegalStateException("AI generation failed"))
                else -> Result.failure(IOException("Server error (${e.code()}): ${e.message}"))
            }
        } catch (e: IOException) {
            Result.failure(IOException("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getCachedQuestions(postContent: String, type: String, numItems: Int): List<AiPracticeQuestion>? {
        val cacheKey = generateCacheKey(postContent, type, numItems)
        return cache[cacheKey]
    }
    
    override suspend fun cacheQuestions(postContent: String, type: String, numItems: Int, questions: List<AiPracticeQuestion>) {
        val cacheKey = generateCacheKey(postContent, type, numItems)
        cache[cacheKey] = questions
        Log.d("AiPractice", "Cached ${questions.size} questions for key: $cacheKey")
    }
    
    override suspend fun clearCache() {
        cache.clear()
        Log.d("AiPractice", "Cache cleared")
    }
}
