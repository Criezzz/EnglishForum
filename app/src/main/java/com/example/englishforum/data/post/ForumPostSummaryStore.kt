package com.example.englishforum.data.post

import com.example.englishforum.core.model.forum.ForumPostDetail
import com.example.englishforum.core.model.forum.ForumPostSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Central cache of forum post summaries so different features stay in sync.
 */
class ForumPostSummaryStore {

    private val postsState = MutableStateFlow<List<ForumPostSummary>>(emptyList())

    val postsStream: StateFlow<List<ForumPostSummary>> = postsState.asStateFlow()

    val currentPosts: List<ForumPostSummary>
        get() = postsState.value

    fun replaceAll(posts: List<ForumPostSummary>) {
        postsState.value = posts
    }

    fun clear() {
        postsState.value = emptyList()
    }

    fun updatePost(postId: String, transform: (ForumPostSummary) -> ForumPostSummary): Boolean {
        var updated = false
        postsState.update { current ->
            current.map { post ->
                if (post.id == postId) {
                    updated = true
                    transform(post)
                } else {
                    post
                }
            }
        }
        return updated
    }

    fun upsertFromDetail(detail: ForumPostDetail) {
        postsState.update { current ->
            val index = current.indexOfFirst { it.id == detail.id }
            if (index == -1) {
                current
            } else {
                current.toMutableList().also { list ->
                    val previous = list[index]
                    list[index] = detail.toSummary(previous)
                }
            }
        }
    }

    fun remove(postId: String) {
        postsState.update { current -> current.filterNot { it.id == postId } }
    }
}

private fun ForumPostDetail.toSummary(previous: ForumPostSummary?): ForumPostSummary {
    return ForumPostSummary(
        id = id,
        authorName = authorName,
        authorUsername = authorUsername ?: previous?.authorUsername,
        minutesAgo = minutesAgo,
        title = title,
        body = body,
        voteCount = voteCount,
        voteState = voteState,
        commentCount = commentCount,
        tag = tag,
        authorAvatarUrl = authorAvatarUrl ?: previous?.authorAvatarUrl,
        previewImageUrl = previewImageUrl ?: previous?.previewImageUrl
    )
}
