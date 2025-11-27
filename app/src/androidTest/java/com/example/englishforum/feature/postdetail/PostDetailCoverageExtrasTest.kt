package com.example.englishforum.feature.postdetail

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.englishforum.core.di.AppContainer
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.ui.theme.EnglishForumTheme
import com.example.englishforum.data.aipractice.FakeAiPracticeRepository
import com.example.englishforum.data.auth.AuthRepository
import com.example.englishforum.data.auth.FakeUserSessionRepository
import com.example.englishforum.data.auth.SessionPreferenceRepository
import com.example.englishforum.data.auth.SessionValidationResult
import com.example.englishforum.data.auth.SessionValidator
import com.example.englishforum.data.auth.UserSession
import com.example.englishforum.data.create.CreatePostRepository
import com.example.englishforum.data.create.FakeCreatePostRepository
import com.example.englishforum.data.home.FakeHomeRepository
import com.example.englishforum.data.home.HomeRepository
import com.example.englishforum.data.notification.FakeNotificationRepository
import com.example.englishforum.data.notification.NotificationRepository
import com.example.englishforum.data.post.FakePostDetailRepository
import com.example.englishforum.data.post.FakePostStore
import com.example.englishforum.data.post.ForumPostSummaryStore
import com.example.englishforum.data.post.PostDetailRepository
import com.example.englishforum.data.search.SearchRepository
import com.example.englishforum.data.settings.ThemePreferenceRepository
import com.example.englishforum.data.profile.FakeProfileRepository
import com.example.englishforum.data.profile.ProfileRepository
import com.example.englishforum.core.model.search.SearchResult
import com.example.englishforum.core.network.NetworkMonitor
import com.example.englishforum.core.image.DefaultImageProcessor
import com.example.englishforum.core.image.ImageProcessor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostDetailCoverageExtrasTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var userSessionRepository: FakeUserSessionRepository
    private lateinit var postDetailRepository: FakePostDetailRepository

    @Before
    fun setUp() {
        userSessionRepository = FakeUserSessionRepository()
        postDetailRepository = FakePostDetailRepository(
            store = FakePostStore,
            userSessionRepository = userSessionRepository
        )
    }

    @Test
    fun reportPost_dialogSubmits_successPathCovered() {
        // Non-owner should see report option
        userSessionRepository.setSession(
            UserSession(
                userId = "guest-user",
                username = "guest",
                accessToken = "token",
                refreshToken = "refresh"
            )
        )

        launchScreen(postId = "post-2", repository = postDetailRepository)

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("post_detail_more_button", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("post_detail_more_button", useUnmergedTree = true)
            .performClick()

        composeTestRule.onNodeWithText("Báo cáo bài viết", useUnmergedTree = true)
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithTag("post_detail_report_reason_field", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("post_detail_report_reason_field", useUnmergedTree = true)
            .performTextInput("Nội dung spam")

        composeTestRule.onNodeWithText("Gửi báo cáo", useUnmergedTree = true)
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Đã gửi báo cáo cho bài viết.", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun reportPost_dialogSubmits_failureShowsError() {
        // Force reportPost to fail to cover error branch
        val failingRepository = object : PostDetailRepository by postDetailRepository {
            override suspend fun reportPost(postId: String, reason: String): Result<Unit> {
                return Result.failure(IllegalStateException("Báo cáo thất bại"))
            }
        }

        userSessionRepository.setSession(
            UserSession(
                userId = "another-guest",
                username = "guest2",
                accessToken = "token",
                refreshToken = "refresh"
            )
        )

        launchScreen(postId = "post-2", repository = failingRepository)

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("post_detail_more_button", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("post_detail_more_button", useUnmergedTree = true)
            .performClick()

        composeTestRule.onNodeWithText("Báo cáo bài viết", useUnmergedTree = true)
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithTag("post_detail_report_reason_field", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("post_detail_report_reason_field", useUnmergedTree = true)
            .performTextInput("Nội dung không phù hợp")

        composeTestRule.onNodeWithText("Gửi báo cáo", useUnmergedTree = true)
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Báo cáo thất bại", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun aiPracticeFab_dragAndNavigate_triggersHandlers() {
        userSessionRepository.setSession(
            UserSession(
                userId = "demo-user",
                username = "linhtran",
                accessToken = "token",
                refreshToken = "refresh"
            )
        )

        var navigatedPostId: String? = null

        launchScreen(
            postId = "post-1",
            repository = postDetailRepository,
            onNavigateToAiPractice = { navigatedPostId = it }
        )

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithContentDescription(
                "Mở câu hỏi luyện tập cùng AI",
                useUnmergedTree = true
            ).fetchSemanticsNodes().isNotEmpty()
        }

        val fabNode = composeTestRule.onNodeWithContentDescription(
            "Mở câu hỏi luyện tập cùng AI",
            useUnmergedTree = true
        )

        fabNode.performTouchInput { swipeUp() }
        fabNode.performTouchInput { swipeDown() }
        fabNode.performClick()

        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            navigatedPostId == "post-1"
        }
    }

    private fun launchScreen(
        postId: String,
        repository: PostDetailRepository,
        onNavigateToAiPractice: (String) -> Unit = {}
    ) {
        val aiPracticeRepository = FakeAiPracticeRepository()
        val postSummaryStore = ForumPostSummaryStore()

        val appContainer: AppContainer = object : AppContainer {
            override val postDetailRepository: PostDetailRepository = repository
            override val userSessionRepository: FakeUserSessionRepository = this@PostDetailCoverageExtrasTest.userSessionRepository
            override val aiPracticeRepository: FakeAiPracticeRepository = aiPracticeRepository
            override val postSummaryStore: ForumPostSummaryStore = postSummaryStore
            override val authRepository: AuthRepository = object : AuthRepository {
                override suspend fun login(username: String, password: String) = throw NotImplementedError()
                override suspend fun register(username: String, email: String, password: String) = throw NotImplementedError()
                override suspend fun verifyEmail(otp: String) = throw NotImplementedError()
                override suspend fun resendVerificationOtp() = throw NotImplementedError()
                override suspend fun requestRecoveryOtp(contact: String) = throw NotImplementedError()
                override suspend fun verifyRecoveryOtp(contact: String, code: String) = throw NotImplementedError()
                override suspend fun resetPassword(resetToken: String, newPassword: String) = throw NotImplementedError()
                override suspend fun refreshSession(session: UserSession) = throw NotImplementedError()
            }
            override val sessionPreferenceRepository: SessionPreferenceRepository = object : SessionPreferenceRepository {
                override val keepLoggedInFlow: Flow<Boolean> = flowOf(false)
                override suspend fun setKeepLoggedIn(value: Boolean) {}
            }
            override val themePreferenceRepository = ThemePreferenceRepository(composeTestRule.activity.applicationContext)
            override val homeRepository: HomeRepository = FakeHomeRepository()
            override val searchRepository: SearchRepository = object : SearchRepository {
                override suspend fun search(keyword: String) = Result.success(SearchResult(emptyList(), emptyList()))
                override suspend fun updateVote(postId: String, target: VoteState) = Result.success(Unit)
            }
            override val createPostRepository: CreatePostRepository = FakeCreatePostRepository(userSessionRepository = this@PostDetailCoverageExtrasTest.userSessionRepository)
            override val notificationRepository: NotificationRepository = FakeNotificationRepository()
            override val profileRepository: ProfileRepository = FakeProfileRepository()
            override val sessionValidator: SessionValidator = object : SessionValidator {
                override suspend fun validate(session: UserSession) = SessionValidationResult.Valid
            }
            override val networkMonitor: NetworkMonitor = NetworkMonitor(composeTestRule.activity.applicationContext)
            override val imageProcessor: ImageProcessor = DefaultImageProcessor(composeTestRule.activity.applicationContext)
        }

        composeTestRule.setContent {
            EnglishForumTheme {
                CompositionLocalProvider(LocalAppContainer provides appContainer) {
                    PostDetailRoute(
                        modifier = Modifier.fillMaxSize(),
                        postId = postId,
                        commentId = null,
                        onBackClick = {},
                        savedStateHandle = SavedStateHandle(),
                        onNavigateToAiPractice = onNavigateToAiPractice,
                        onEditPostClick = {},
                        onPostDeleted = {},
                        onAuthorClick = {},
                        createdFlag = false
                    )
                }
            }
        }
    }
}
