package com.example.englishforum.core.di

import android.content.Context
import com.example.englishforum.data.aipractice.AiPracticeRepository
import com.example.englishforum.data.aipractice.FakeAiPracticeRepository
import com.example.englishforum.data.auth.AuthRepository
import com.example.englishforum.data.auth.DataStoreUserSessionRepository
import com.example.englishforum.data.auth.FakeAuthRepository
import com.example.englishforum.data.auth.UserSessionRepository
import com.example.englishforum.data.create.CreatePostRepository
import com.example.englishforum.data.create.FakeCreatePostRepository
import com.example.englishforum.data.home.FakeHomeRepository
import com.example.englishforum.data.home.HomeRepository
import com.example.englishforum.data.notification.FakeNotificationRepository
import com.example.englishforum.data.notification.NotificationRepository
import com.example.englishforum.data.post.FakePostDetailRepository
import com.example.englishforum.data.post.FakePostStore
import com.example.englishforum.data.post.PostDetailRepository
import com.example.englishforum.data.profile.FakeProfileRepository
import com.example.englishforum.data.profile.ProfileRepository
import com.example.englishforum.data.settings.ThemePreferenceRepository

interface AppContainer {
    val userSessionRepository: UserSessionRepository
    val authRepository: AuthRepository
    val themePreferenceRepository: ThemePreferenceRepository
    val homeRepository: HomeRepository
    val postDetailRepository: PostDetailRepository
    val aiPracticeRepository: AiPracticeRepository
    val createPostRepository: CreatePostRepository
    val notificationRepository: NotificationRepository
    val profileRepository: ProfileRepository
}

class DefaultAppContainer(context: Context) : AppContainer {

    private val appContext = context.applicationContext
    private val postStore = FakePostStore

    override val userSessionRepository: UserSessionRepository by lazy {
        DataStoreUserSessionRepository(appContext)
    }

    override val authRepository: AuthRepository by lazy {
        FakeAuthRepository(userSessionRepository)
    }

    override val themePreferenceRepository: ThemePreferenceRepository by lazy {
        ThemePreferenceRepository(appContext)
    }

    override val homeRepository: HomeRepository by lazy {
        FakeHomeRepository(postStore)
    }

    override val postDetailRepository: PostDetailRepository by lazy {
        FakePostDetailRepository(postStore)
    }

    override val aiPracticeRepository: AiPracticeRepository by lazy {
        FakeAiPracticeRepository()
    }

    override val createPostRepository: CreatePostRepository by lazy {
        FakeCreatePostRepository(postStore)
    }

    override val notificationRepository: NotificationRepository by lazy {
        FakeNotificationRepository(postStore)
    }

    override val profileRepository: ProfileRepository by lazy {
        FakeProfileRepository()
    }
}
