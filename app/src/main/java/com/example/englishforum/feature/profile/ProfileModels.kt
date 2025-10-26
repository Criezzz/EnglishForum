package com.example.englishforum.feature.profile

import android.net.Uri
import com.example.englishforum.core.model.VoteState

data class ProfileOverview(
    val displayName: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val stats: ProfileStats
)

data class ProfileStats(
    val upvotes: Int,
    val posts: Int,
    val answers: Int
)

data class ProfileAvatarUiState(
    val previewUri: Uri? = null,
    val isUploading: Boolean = false,
    val errorMessage: String? = null
)

data class ProfilePost(
    val id: String,
    val title: String,
    val body: String,
    val timeLabel: String,
    val voteCount: Int,
    val voteState: VoteState = VoteState.NONE,
    val previewImageUrl: String? = null
)

data class ProfileReply(
    val id: String,
    val postId: String,
    val questionTitle: String,
    val body: String,
    val timeLabel: String,
    val voteCount: Int,
    val voteState: VoteState = VoteState.NONE
)

data class ProfileUiState(
    val overview: ProfileOverview? = null,
    val posts: List<ProfilePost> = emptyList(),
    val replies: List<ProfileReply> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

sealed interface ProfileEditState {
    data object Idle : ProfileEditState
    data object InProgress : ProfileEditState
    data object Success : ProfileEditState
    data class Error(val message: String) : ProfileEditState
}
