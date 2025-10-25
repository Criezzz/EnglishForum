package com.example.englishforum.core.di

import android.content.Context
import com.example.englishforum.BuildConfig
import com.example.englishforum.core.network.NetworkMonitor
import com.example.englishforum.data.aipractice.AiPracticeRepository
import com.example.englishforum.data.aipractice.FakeAiPracticeRepository
import com.example.englishforum.data.auth.AuthRepository
import com.example.englishforum.data.auth.DataStoreUserSessionRepository
import com.example.englishforum.data.auth.SessionValidator
import com.example.englishforum.data.auth.UserSessionRepository
import com.example.englishforum.data.auth.remote.AuthApi
import com.example.englishforum.data.auth.remote.RemoteAuthRepository
import com.example.englishforum.data.auth.remote.RemoteSessionValidator
import com.example.englishforum.data.create.CreatePostRepository
import com.example.englishforum.data.create.remote.CreatePostApi
import com.example.englishforum.data.create.remote.RemoteCreatePostRepository
import com.example.englishforum.data.home.HomeRepository
import com.example.englishforum.data.home.remote.PostsApi
import com.example.englishforum.data.home.remote.RemoteHomeRepository
import com.example.englishforum.data.notification.FakeNotificationRepository
import com.example.englishforum.data.notification.NotificationRepository
import com.example.englishforum.data.post.PostDetailRepository
import com.example.englishforum.data.post.ForumPostSummaryStore
import com.example.englishforum.data.post.FakePostStore
import com.example.englishforum.data.post.remote.PostDetailApi
import com.example.englishforum.data.post.remote.RemotePostDetailRepository
import com.example.englishforum.data.profile.ProfileRepository
import com.example.englishforum.data.profile.remote.ProfileApi
import com.example.englishforum.data.profile.remote.RemoteProfileRepository
import com.example.englishforum.data.search.SearchRepository
import com.example.englishforum.data.search.remote.RemoteSearchRepository
import com.example.englishforum.data.search.remote.SearchApi
import com.example.englishforum.data.settings.ThemePreferenceRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

interface AppContainer {
    val userSessionRepository: UserSessionRepository
    val authRepository: AuthRepository
    val themePreferenceRepository: ThemePreferenceRepository
    val homeRepository: HomeRepository
    val searchRepository: SearchRepository
    val postDetailRepository: PostDetailRepository
    val aiPracticeRepository: AiPracticeRepository
    val createPostRepository: CreatePostRepository
    val notificationRepository: NotificationRepository
    val profileRepository: ProfileRepository
    val sessionValidator: SessionValidator
    val networkMonitor: NetworkMonitor
}

class DefaultAppContainer(context: Context) : AppContainer {

    private val appContext = context.applicationContext
    private val postStore = FakePostStore
    private val postSummaryStore = ForumPostSummaryStore()

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val authInterceptor: Interceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("Accept", "application/json")
            .build()
        chain.proceed(request)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    val logger = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                    addInterceptor(logger)
                }
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }

    override val userSessionRepository: UserSessionRepository by lazy {
        DataStoreUserSessionRepository(appContext)
    }

    override val authRepository: AuthRepository by lazy {
        RemoteAuthRepository(
            authApi = authApi,
            userSessionRepository = userSessionRepository
        )
    }

    override val themePreferenceRepository: ThemePreferenceRepository by lazy {
        ThemePreferenceRepository(appContext)
    }

    private val postsApi: PostsApi by lazy { retrofit.create(PostsApi::class.java) }
    private val postDetailApi: PostDetailApi by lazy { retrofit.create(PostDetailApi::class.java) }
    private val searchApi: SearchApi by lazy { retrofit.create(SearchApi::class.java) }

    override val homeRepository: HomeRepository by lazy {
        RemoteHomeRepository(
            postsApi = postsApi,
            userSessionRepository = userSessionRepository,
            postStore = postSummaryStore
        )
    }

    override val searchRepository: SearchRepository by lazy {
        RemoteSearchRepository(
            searchApi = searchApi,
            userSessionRepository = userSessionRepository,
            postInteractionRepository = homeRepository
        )
    }

    override val postDetailRepository: PostDetailRepository by lazy {
        RemotePostDetailRepository(
            api = postDetailApi,
            userSessionRepository = userSessionRepository,
            summaryStore = postSummaryStore
        )
    }

    override val aiPracticeRepository: AiPracticeRepository by lazy {
        FakeAiPracticeRepository()
    }

    private val createPostApi: CreatePostApi by lazy { retrofit.create(CreatePostApi::class.java) }

    override val createPostRepository: CreatePostRepository by lazy {
        RemoteCreatePostRepository(
            api = createPostApi,
            userSessionRepository = userSessionRepository,
            contentResolver = appContext.contentResolver
        )
    }

    override val notificationRepository: NotificationRepository by lazy {
        FakeNotificationRepository(postStore)
    }

    private val profileApi: ProfileApi by lazy { retrofit.create(ProfileApi::class.java) }

    override val profileRepository: ProfileRepository by lazy {
        RemoteProfileRepository(
            profileApi = profileApi,
            userSessionRepository = userSessionRepository,
            contentResolver = appContext.contentResolver
        )
    }

    override val sessionValidator: SessionValidator by lazy {
        RemoteSessionValidator(profileApi = profileApi)
    }

    override val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(appContext)
    }

    companion object {
        private const val NETWORK_TIMEOUT_SECONDS = 60L
    }
}
