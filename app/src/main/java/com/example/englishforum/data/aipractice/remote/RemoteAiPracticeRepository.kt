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
import com.squareup.moshi.Types
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.File

class RemoteAiPracticeRepository(
    private val aiPracticeApi: AiPracticeApi,
    private val userSessionRepository: UserSessionRepository,
    private val cacheDirectory: File
) : AiPracticeRepository {

    private val cache = mutableMapOf<String, List<AiPracticeQuestion>>()
    private val inFlightMutex = Mutex()
    private val inFlightRequests = mutableMapOf<String, Deferred<Result<List<AiPracticeQuestion>>>>()
    private val inFlightJobs = mutableMapOf<String, Job>()
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val generatingKeys = MutableStateFlow<Set<String>>(emptySet())
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private fun generateCacheKey(postContent: String, type: String, numItems: Int, postId: String? = null): String {
        // Use post ID for reliable cache key
        return if (postId != null) {
            "${postId}_${type}_${numItems}"
        } else {
            // Fallback to content hash if ID not available
            "${postContent.hashCode()}_${type}_${numItems}"
        }
    }

    private fun cacheFileFor(key: String): File = File(cacheDirectory, "$key.json")

    private fun isExpired(timestamp: Long): Boolean {
        val ageMs = System.currentTimeMillis() - timestamp
        return ageMs > DISK_CACHE_TTL_MS
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

        // Check memory/disk cache first
        val cacheKey = generateCacheKey(postContent, type, numItems, postId)
        Log.d("AiPractice", "Cache key: $cacheKey (postId=$postId)")
        getCachedFromMemoryOrDisk(cacheKey)?.let { fromCache ->
            Log.d("AiPractice", "Found cached questions: ${fromCache.size} questions")
            return Result.success(fromCache)
        }

        // Deduplicate in-flight requests by cache key
        val existing = inFlightMutex.withLock { inFlightRequests[cacheKey] }
        if (existing != null) {
            Log.d("AiPractice", "Joining in-flight request for key: $cacheKey")
            return existing.await()
        }

        // Track generating state
        generatingKeys.value = generatingKeys.value + cacheKey

        val deferred = repoScope.async {
            try {
                val session = userSessionRepository.sessionFlow.firstOrNull()
                if (session == null) {
                    Log.e("AiPractice", "User not authenticated")
                    return@async Result.failure(IllegalStateException("User not authenticated"))
                }

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

                val response = aiPracticeApi.generateFromText(session.bearerToken(), request)

                // Check if the post is askable for AI practice
                val isAskable = response.isAskable ?: (response.items.isNotEmpty())
                if (!isAskable) {
                    cache.remove(cacheKey)
                    deleteDiskCache(cacheKey)
                    return@async Result.failure(IllegalStateException("AI practice is not available for this post"))
                }

                if (response.items.isEmpty()) {
                    return@async Result.failure(IllegalStateException("No questions generated yet"))
                }

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

                // Cache non-empty list to memory and disk
                if (questions.isNotEmpty()) {
                    cache[cacheKey] = questions
                    writeDiskCache(cacheKey, questions)
                }
                Result.success(questions)
            } catch (e: HttpException) {
                when (e.code()) {
                    404 -> Result.failure(IllegalArgumentException("Không tìm thấy bài viết"))
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
            repoScope.launch {
                inFlightMutex.withLock { inFlightRequests.remove(cacheKey) }
                inFlightMutex.withLock { inFlightJobs.remove(cacheKey) }
                generatingKeys.value = generatingKeys.value - cacheKey
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
        val cacheKey = generateCacheKey(postContent, type, numItems, postId)
        getCachedFromMemoryOrDisk(cacheKey)?.let { return it }
        return null
    }

    override suspend fun cacheQuestions(postContent: String, type: String, numItems: Int, questions: List<AiPracticeQuestion>, postId: String?) {
        val cacheKey = generateCacheKey(postContent, type, numItems, postId)
        cache[cacheKey] = questions
        writeDiskCache(cacheKey, questions)
        Log.d("AiPractice", "Cached ${questions.size} questions for key: $cacheKey")
    }

    override suspend fun clearCache() {
        cache.clear()
        cacheDirectory.listFiles()?.forEach { file ->
            runCatching { file.delete() }
        }
        Log.d("AiPractice", "Cache cleared")
    }

    override suspend fun clearCacheForPost(postId: String) {
        val prefix = "${postId}_"
        cache.keys.filter { it.startsWith(prefix) }.forEach { cache.remove(it) }
        cacheDirectory.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach { file ->
            runCatching { file.delete() }
        }
        Log.d("AiPractice", "Cleared cache for post: $postId")
    }

    override fun startGeneration(postContent: String, type: String, numItems: Int, postId: String?) {
        val cacheKey = generateCacheKey(postContent, type, numItems, postId)
        if (cache.containsKey(cacheKey)) return
        // Also check disk cache to avoid duplicating work
        if (readDiskCache(cacheKey)?.questions?.isNotEmpty() == true) return
        repoScope.launch {
            val existing = inFlightMutex.withLock { inFlightRequests[cacheKey] }
            if (existing != null) return@launch
            generatingKeys.value = generatingKeys.value + cacheKey
            try {
                generateQuestions(postContent, type, numItems, postId)
            } finally {
                generatingKeys.value = generatingKeys.value - cacheKey
            }
        }
    }

    override fun observeGeneration(postContent: String, type: String, numItems: Int, postId: String?) =
        generatingKeys.map { set -> set.contains(generateCacheKey(postContent, type, numItems, postId)) }

    private data class DiskCachedQuestion(
        val kind: String,
        val id: String,
        val prompt: String,
        val hint: String? = null,
        val correctOptionId: String? = null,
        val answer: String? = null,
        val options: List<AiPracticeOption>? = null
    )

    private data class DiskCacheEnvelope(
        val timestamp: Long,
        val items: List<DiskCachedQuestion>
    )

    private fun writeDiskCache(key: String, questions: List<AiPracticeQuestion>) {
        val file = cacheFileFor(key)
        val mapped = questions.map { q ->
            when (q) {
                is AiPracticeMultipleChoiceQuestion -> DiskCachedQuestion(
                    kind = "mcq",
                    id = q.id,
                    prompt = q.prompt,
                    hint = q.hint,
                    correctOptionId = q.correctOptionId,
                    options = q.options
                )
                is AiPracticeFillInBlankQuestion -> DiskCachedQuestion(
                    kind = "fill",
                    id = q.id,
                    prompt = q.prompt,
                    hint = q.hint,
                    answer = q.correctAnswer
                )
            }
        }
        val envelope = DiskCacheEnvelope(timestamp = System.currentTimeMillis(), items = mapped)
        val adapter = moshi.adapter(DiskCacheEnvelope::class.java)
        runCatching {
            file.writeText(adapter.toJson(envelope))
        }.onFailure { e -> Log.e("AiPractice", "Failed writing disk cache: ${e.message}") }
    }

    private fun deleteDiskCache(key: String) {
        runCatching { cacheFileFor(key).delete() }
    }

    private data class DiskCacheRead(val timestamp: Long, val questions: List<AiPracticeQuestion>)

    private fun readDiskCache(key: String): DiskCacheRead? {
        val file = cacheFileFor(key)
        if (!file.exists()) return null
        val adapter = moshi.adapter(DiskCacheEnvelope::class.java)
        return runCatching {
            val envelope = adapter.fromJson(file.readText()) ?: return null
            if (isExpired(envelope.timestamp)) {
                runCatching { file.delete() }
                return null
            }
            val questions = envelope.items.map { item ->
                when (item.kind) {
                    "mcq" -> AiPracticeMultipleChoiceQuestion(
                        id = item.id,
                        prompt = item.prompt,
                        options = item.options.orEmpty(),
                        correctOptionId = item.correctOptionId.orEmpty(),
                        hint = item.hint
                    )
                    "fill" -> AiPracticeFillInBlankQuestion(
                        id = item.id,
                        prompt = item.prompt,
                        correctAnswer = item.answer.orEmpty(),
                        hint = item.hint
                    )
                    else -> AiPracticeMultipleChoiceQuestion(
                        id = item.id,
                        prompt = item.prompt,
                        options = item.options.orEmpty(),
                        correctOptionId = item.correctOptionId.orEmpty(),
                        hint = item.hint
                    )
                }
            }
            DiskCacheRead(envelope.timestamp, questions)
        }.onFailure { e -> Log.e("AiPractice", "Failed reading disk cache: ${e.message}") }.getOrNull()
    }

    private fun getCachedFromMemoryOrDisk(cacheKey: String): List<AiPracticeQuestion>? {
        cache[cacheKey]?.let { return it }
        val disk = readDiskCache(cacheKey) ?: return null
        cache[cacheKey] = disk.questions
        return disk.questions
    }

    companion object {
        private const val DISK_CACHE_TTL_MS = 48L * 60L * 60L * 1000L // 48 hours
    }
}