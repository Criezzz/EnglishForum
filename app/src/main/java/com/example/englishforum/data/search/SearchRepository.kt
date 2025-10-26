package com.example.englishforum.data.search

import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.search.SearchResult

interface SearchRepository {
    suspend fun search(keyword: String): Result<SearchResult>

    suspend fun updateVote(postId: String, target: VoteState): Result<Unit>
}
