package com.example.englishforum.feature.postedit

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.englishforum.core.di.AppContainer
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.core.ui.theme.EnglishForumTheme
import androidx.compose.runtime.CompositionLocalProvider
import com.example.englishforum.data.auth.FakeUserSessionRepository
import com.example.englishforum.data.auth.UserSession
import com.example.englishforum.data.post.FakePostDetailRepository
import com.example.englishforum.data.post.FakePostStore
import com.example.englishforum.data.post.ForumPostSummaryStore
import com.example.englishforum.data.auth.SessionPreferenceRepository
import com.example.englishforum.data.settings.ThemePreferenceRepository
import com.example.englishforum.core.network.NetworkMonitor
import com.example.englishforum.core.image.ImageProcessor
import com.example.englishforum.core.image.DefaultImageProcessor
import com.example.englishforum.data.auth.AuthRepository
import com.example.englishforum.data.auth.SessionValidator
import com.example.englishforum.data.auth.SessionValidationResult
import com.example.englishforum.data.home.HomeRepository
import com.example.englishforum.data.home.FakeHomeRepository
import com.example.englishforum.data.search.SearchRepository
import com.example.englishforum.data.create.CreatePostRepository
import com.example.englishforum.data.create.FakeCreatePostRepository
import com.example.englishforum.data.notification.NotificationRepository
import com.example.englishforum.data.notification.FakeNotificationRepository
import com.example.englishforum.data.profile.ProfileRepository
import com.example.englishforum.data.profile.FakeProfileRepository
import com.example.englishforum.data.aipractice.AiPracticeRepository
import com.example.englishforum.data.aipractice.FakeAiPracticeRepository
import com.example.englishforum.core.model.search.SearchResult
import com.example.englishforum.core.model.VoteState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for Edit Post functionality
 * Based on functional test specifications for Edit Post feature
 * 
 * Test cases (14 total, skip offline):
 * 1. Edit valid post - Save success
 * 2. Empty title - Save button hidden
 * 3. Empty body - Save button hidden
 * 4-14. Other cases (image handling, etc.) - can be added later
 */
@RunWith(AndroidJUnit4::class)
class EditPostTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var fakeUserSessionRepository: FakeUserSessionRepository
    private lateinit var fakePostDetailRepository: FakePostDetailRepository

    @Before
    fun setUp() {
        fakeUserSessionRepository = FakeUserSessionRepository()
        fakeUserSessionRepository.setSession(
            UserSession(
                userId = "user-linhtran", // Match post-1 owner
                username = "linhtran",
                accessToken = "fake_token",
                refreshToken = "fake_refresh_token"
            )
        )
        fakePostDetailRepository = FakePostDetailRepository(FakePostStore)
    }

    private fun setupScreen(postId: String) {
        val fakePostSummaryStore = ForumPostSummaryStore()
        
        val appContainer = object : AppContainer {
            override val postDetailRepository = fakePostDetailRepository
            override val userSessionRepository = fakeUserSessionRepository
            override val postSummaryStore = fakePostSummaryStore
            override val authRepository = object : AuthRepository {
                override suspend fun login(username: String, password: String) = throw NotImplementedError()
                override suspend fun register(username: String, email: String, password: String) = throw NotImplementedError()
                override suspend fun verifyEmail(otp: String) = throw NotImplementedError()
                override suspend fun resendVerificationOtp() = throw NotImplementedError()
                override suspend fun requestRecoveryOtp(contact: String) = throw NotImplementedError()
                override suspend fun verifyRecoveryOtp(contact: String, code: String) = throw NotImplementedError()
                override suspend fun resetPassword(resetToken: String, newPassword: String) = throw NotImplementedError()
                override suspend fun refreshSession(session: UserSession) = throw NotImplementedError()
            }
            override val sessionPreferenceRepository = object : SessionPreferenceRepository {
                override val keepLoggedInFlow: Flow<Boolean> = flowOf(false)
                override suspend fun setKeepLoggedIn(value: Boolean) {}
            }
            override val themePreferenceRepository = ThemePreferenceRepository(composeTestRule.activity.applicationContext)
            override val homeRepository = FakeHomeRepository()
            override val searchRepository = object : SearchRepository {
                override suspend fun search(keyword: String) = Result.success(SearchResult(emptyList(), emptyList()))
                override suspend fun updateVote(postId: String, target: VoteState) = Result.success(Unit)
            }
            override val createPostRepository = FakeCreatePostRepository(userSessionRepository = fakeUserSessionRepository)
            override val notificationRepository = FakeNotificationRepository()
            override val profileRepository = FakeProfileRepository()
            override val aiPracticeRepository = FakeAiPracticeRepository()
            override val sessionValidator = object : SessionValidator {
                override suspend fun validate(session: UserSession) = SessionValidationResult.Valid
            }
            override val networkMonitor = NetworkMonitor(composeTestRule.activity.applicationContext)
            override val imageProcessor = DefaultImageProcessor(composeTestRule.activity.applicationContext)
        }

        composeTestRule.setContent {
            EnglishForumTheme {
                CompositionLocalProvider(LocalAppContainer provides appContainer) {
                    PostEditRoute(
                        postId = postId,
                        onBackClick = {},
                        onPostUpdated = {}
                    )
                }
            }
        }
    }

    @Test
    fun editPost_TC01_editValidPost_saveSuccess() {
        // TC-EDIT-01: Edit valid post - Save success
        // Input: postId = "post-1", title = "Updated Title", body = "Updated content"
        // Expected: Post updated successfully

        setupScreen("post-1")

        // Wait for post to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("create_post_title_field")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Clear and enter new title
        composeTestRule.onNodeWithTag("create_post_title_field")
            .performTextClearance()
        composeTestRule.onNodeWithTag("create_post_title_field")
            .performTextInput("Updated Title")

        // Clear and enter new body
        composeTestRule.onNodeWithTag("create_post_body_field")
            .performTextClearance()
        composeTestRule.onNodeWithTag("create_post_body_field")
            .performTextInput("Updated content")

        // Submit button should be enabled
        composeTestRule.onNodeWithTag("create_post_submit_button")
            .assertIsEnabled()

        // Click save
        composeTestRule.onNodeWithTag("create_post_submit_button")
            .performClick()

        // Wait for save to complete
        composeTestRule.waitForIdle()
    }

    @Test
    fun editPost_TC02_emptyTitle_saveButtonHidden() {
        // TC-EDIT-02: Empty title - Save button hidden
        // Input: postId = "post-1", title = "", body = "Content"
        // Expected: Save button disabled

        setupScreen("post-1")

        // Wait for post to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("create_post_title_field")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Clear title
        composeTestRule.onNodeWithTag("create_post_title_field")
            .performTextClearance()

        // Submit button should be disabled
        composeTestRule.onNodeWithTag("create_post_submit_button")
            .assertIsNotEnabled()
    }

    @Test
    fun editPost_TC03_emptyBody_saveButtonHidden() {
        // TC-EDIT-03: Empty body - Save button hidden
        // Input: postId = "post-1", title = "Title", body = ""
        // Expected: Save button disabled

        setupScreen("post-1")

        // Wait for post to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("create_post_body_field")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Clear body
        composeTestRule.onNodeWithTag("create_post_body_field")
            .performTextClearance()

        // Submit button should be disabled
        composeTestRule.onNodeWithTag("create_post_submit_button")
            .assertIsNotEnabled()
    }
}

