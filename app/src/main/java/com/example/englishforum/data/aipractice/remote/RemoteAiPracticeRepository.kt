package com.example.englishforum.data.aipractice.remote

import com.example.englishforum.data.aipractice.AiPracticeFillInBlankQuestion
import com.example.englishforum.data.aipractice.AiPracticeMultipleChoiceQuestion
import com.example.englishforum.data.aipractice.AiPracticeOption
import com.example.englishforum.data.aipractice.AiPracticeQuestion
import com.example.englishforum.data.aipractice.AiPracticeRepository
import com.example.englishforum.data.aipractice.remote.model.GenerateFromTextRequest
import com.example.englishforum.data.auth.UserSessionRepository
import com.example.englishforum.data.auth.bearerToken
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import android.util.Log

class RemoteAiPracticeRepository(
    private val aiPracticeApi: AiPracticeApi,
    private val userSessionRepository: UserSessionRepository
) : AiPracticeRepository {

    private val cache = mutableMapOf<String, List<AiPracticeQuestion>>()
    private val inFlightMutex = Mutex()
    private val inFlightRequests = mutableMapOf<String, Deferred<Result<List<AiPracticeQuestion>>>>()
    private val inFlightJobs = mutableMapOf<String, kotlinx.coroutines.Job>()
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private fun generateCacheKey(postContent: String, type: String, numItems: Int, postId: String? = null): String {
        // Use post ID for reliable cache key
        return if (postId != null) {
            "${postId}_${type}_${numItems}"
        } else {
            // Fallback to content hash if ID not available
            "${postContent.hashCode()}_${type}_${numItems}"
        }
    }


    override suspend fun loadQuestions(postContent: String): Result<List<AiPracticeQuestion>> {
        return generateQuestions(postContent, "mcq", 3, null)
    }

    override suspend fun generateQuestions(
        postContent: String, 
        type: String, 
        numItems: Int,
        postId: String?
    ): Result<List<AiPracticeQuestion>> {
        Log.d("AiPractice", "generateQuestions called: type=$type, numItems=$numItems, content length=${postContent.length}")

        // Check cache first
        val cacheKey = generateCacheKey(postContent, type, numItems, postId)
        Log.d("AiPractice", "Cache key: $cacheKey (postId=$postId)")
        val cachedQuestions = getCachedQuestions(postContent, type, numItems, postId)
        if (cachedQuestions != null) {
            Log.d("AiPractice", "Found cached questions: ${cachedQuestions.size} questions")
            return Result.success(cachedQuestions)
        } else {
            Log.d("AiPractice", "No cached questions found")
        }

        // Deduplicate in-flight requests by cache key
        val existing = inFlightMutex.withLock { inFlightRequests[cacheKey] }
        if (existing != null) {
            Log.d("AiPractice", "Joining in-flight request for key: $cacheKey")
            return existing.await()
        }

        val deferred = repoScope.async {
                try {
                    val session = userSessionRepository.sessionFlow.firstOrNull()
                    if (session == null) {
                        Log.e("AiPractice", "User not authenticated")
                        return@async Result.failure(IllegalStateException("User not authenticated"))
                    }

                    Log.d("AiPractice", "User authenticated, token: ${session.bearerToken().take(20)}...")

                    // Clean the text by removing surrounding quotes if present
                    val cleanedContent = postContent.trim().let { content ->
                        if (content.startsWith("\"") && content.endsWith("\"")) {
                            content.substring(1, content.length - 1)
                        } else {
                            content
                        }
                    }

                    val request = GenerateFromTextRequest(
                        contextText = cleanedContent,
                        type = type,
                        numItems = numItems,
                        mode = "cot"
                    )

                    Log.d("AiPractice", "Original content length: ${postContent.length}")
                    Log.d("AiPractice", "Original content: '$postContent'")
                    Log.d("AiPractice", "Cleaned content length: ${cleanedContent.length}")
                    Log.d("AiPractice", "Cleaned content: '$cleanedContent'")
                    Log.d("AiPractice", "Request type: $type")
                    Log.d("AiPractice", "Request numItems: $numItems")
                    Log.d("AiPractice", "Request mode: ${request.mode}")
                    Log.d("AiPractice", "Full request object: $request")

                    // Log actual JSON being sent
                    try {
                        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                        val jsonAdapter = moshi.adapter(GenerateFromTextRequest::class.java)
                        val jsonString = jsonAdapter.toJson(request)
                        Log.d("AiPractice", "Request JSON: $jsonString")
                    } catch (e: Exception) {
                        Log.e("AiPractice", "Failed to serialize to JSON: ${e.message}")
                    }

                    val response = aiPracticeApi.generateFromText(session.bearerToken(), request)
                    Log.d("AiPractice", "Received generate response: items count=${response.items.size}, isAskable=${response.isAskable}")
                    Log.d("AiPractice", "Full generate response: $response")

                    // Check if the post is askable for AI practice
                    val isAskable = response.isAskable ?: (response.items.isNotEmpty())
                    Log.d("AiPractice", "isAskable check: response.isAskable=${response.isAskable}, items.isNotEmpty()=${response.items.isNotEmpty()}, final isAskable=$isAskable")
                    if (!isAskable) {
                        Log.d("AiPractice", "Post is not askable, returning failure - NOT caching empty result")
                        cache.remove(cacheKey)
                        return@async Result.failure(IllegalStateException("AI practice is not available for this post"))
                    }

                    if (response.items.isEmpty()) {
                        Log.d("AiPractice", "API returned 0 items. Treat as not ready/failed. Do not cache.")
                        return@async Result.failure(IllegalStateException("No questions generated yet"))
                    }

                    Log.d("AiPractice", "Starting to map ${response.items.size} items to questions")
                    val questions = response.items.mapIndexed { index, item ->
                        val questionId = item.question.id ?: "generated-${index}"
                        when (item.type.lowercase()) {
                            "mcq" -> {
                                val options = item.question.options?.map { option ->
                                    AiPracticeOption(id = option.id, label = option.label)
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
                                AiPracticeFillInBlankQuestion(
                                    id = questionId,
                                    prompt = item.question.prompt,
                                    correctAnswer = item.answer ?: "",
                                    hint = item.hint
                                )
                            }
                            else -> {
                                val options = item.question.options?.map { option ->
                                    AiPracticeOption(id = option.id, label = option.label)
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

                    // Cache only non-empty lists
                    if (questions.isNotEmpty()) {
                        cache[cacheKey] = questions
                        Log.d("AiPractice", "Cached ${questions.size} questions for key: $cacheKey")
                    } else {
                        Log.d("AiPractice", "Questions list empty after mapping; not caching")
                    }
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

        inFlightMutex.withLock { 
            inFlightRequests[cacheKey] = deferred
            inFlightJobs[cacheKey] = deferred
        }
        deferred.invokeOnCompletion {
            // remove when done (success or failure)
            repoScope.launch {
                inFlightMutex.withLock { inFlightRequests.remove(cacheKey) }
                inFlightMutex.withLock { inFlightJobs.remove(cacheKey) }
            }
        }
        return deferred.await()
    }

    override suspend fun cancelInFlight(postId: String, type: String, numItems: Int) {
        val key = generateCacheKey("", type, numItems, postId)
        inFlightMutex.withLock {
            inFlightJobs.remove(key)?.cancel()
            inFlightRequests.remove(key)?.cancel()
        }
        Log.d("AiPractice", "Cancelled in-flight generation for key: $key")
    }
    
    override suspend fun getCachedQuestions(postContent: String, type: String, numItems: Int, postId: String?): List<AiPracticeQuestion>? {
        // Use same cache key logic as generateQuestions
        val cacheKey = generateCacheKey(postContent, type, numItems, postId)
        return cache[cacheKey]
    }
    
    override suspend fun cacheQuestions(postContent: String, type: String, numItems: Int, questions: List<AiPracticeQuestion>, postId: String?) {
        // Use same cache key logic as other methods
        val cacheKey = generateCacheKey(postContent, type, numItems, postId)
        cache[cacheKey] = questions
        Log.d("AiPractice", "Cached ${questions.size} questions for key: $cacheKey")
    }
    
    override suspend fun clearCache() {
        cache.clear()
        Log.d("AiPractice", "Cache cleared")
    }
    
    override suspend fun clearCacheForPost(postId: String) {
        val keysToRemove = cache.keys.filter { it.startsWith("${postId}_") }
        keysToRemove.forEach { key ->
            cache.remove(key)
            Log.d("AiPractice", "Removed cache for key: $key")
        }
        Log.d("AiPractice", "Cleared ${keysToRemove.size} cache entries for post: $postId")
    }
    
    // Debug method to check cache contents
    private fun logCacheContents() {
        Log.d("AiPractice", "Cache contents:")
        cache.forEach { (key, questions) ->
            Log.d("AiPractice", "  Key: $key -> ${questions.size} questions")
        }
    }
}
