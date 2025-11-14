package com.example.englishforum.feature.postdetail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.englishforum.core.di.AppContainer
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.core.ui.theme.EnglishForumTheme
import androidx.compose.runtime.CompositionLocalProvider
import com.example.englishforum.data.auth.FakeUserSessionRepository
import com.example.englishforum.data.auth.UserSession
import com.example.englishforum.data.aipractice.FakeAiPracticeRepository
import com.example.englishforum.data.auth.SessionPreferenceRepository
import com.example.englishforum.data.post.FakePostDetailRepository
import com.example.englishforum.data.post.FakePostStore
import com.example.englishforum.data.post.ForumPostSummaryStore
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
import com.example.englishforum.core.model.search.SearchResult
import com.example.englishforum.core.model.VoteState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for Delete Post functionality
 * Based on functional test specifications for Delete Post feature
 * 
 * Test cases:
 * 1. Delete own post - Confirm dialog, post removed
 * 2. Cancel delete - Confirm dialog, post remains
 * 3. Delete other's post - No delete option
 * 4. Delete while offline - Show connection error (skipped - offline test)
 * 5. Post deleted elsewhere - Show "Post not found"
 */
@RunWith(AndroidJUnit4::class)
class DeletePostTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var fakeUserSessionRepository: FakeUserSessionRepository
    private lateinit var fakePostDetailRepository: FakePostDetailRepository

    @Before
    fun setUp() {
        fakeUserSessionRepository = FakeUserSessionRepository()
        fakePostDetailRepository = FakePostDetailRepository(FakePostStore)
    }

    private fun setupScreen(postId: String, isOwner: Boolean = false) {
        // Set user session to match post owner if needed
        val userId = if (isOwner) {
            // Get post owner ID from FakePostStore
            FakePostStore.posts.value.firstOrNull { it.id == postId }?.authorId ?: "user-test"
        } else {
            "user-other" // Different user
        }
        
        fakeUserSessionRepository.setSession(
            UserSession(
                userId = userId,
                username = "testuser",
                accessToken = "fake_token",
                refreshToken = "fake_refresh_token"
            )
        )

        val fakeAiPracticeRepository = FakeAiPracticeRepository()
        val fakePostSummaryStore = ForumPostSummaryStore()
        
        val appContainer = object : AppContainer {
            override val postDetailRepository = fakePostDetailRepository
            override val userSessionRepository = fakeUserSessionRepository
            override val aiPracticeRepository = fakeAiPracticeRepository
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
            override val sessionValidator = object : SessionValidator {
                override suspend fun validate(session: UserSession) = SessionValidationResult.Valid
            }
            override val networkMonitor = NetworkMonitor(composeTestRule.activity.applicationContext)
            override val imageProcessor = DefaultImageProcessor(composeTestRule.activity.applicationContext)
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
                        onNavigateToAiPractice = {},
                        onEditPostClick = {},
                        onPostDeleted = {},
                        onAuthorClick = {},
                        createdFlag = false
                    )
                }
            }
        }
    }

    @Test
    fun deletePost_TC01_deleteOwnPost_confirmDialogPostRemoved() {
        // TC-DELETE-01: Delete own post - Confirm dialog, post removed
        // Input: postId = "post-1" (owned by current user)
        // Expected: Confirm dialog appears, post is deleted after confirmation

        setupScreen("post-1", isOwner = true)

        // Wait for post to load (async operation)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("post_detail_more_button", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Click more button
        composeTestRule.onNodeWithTag("post_detail_more_button", useUnmergedTree = true).performClick()

        // Click delete option
        composeTestRule.onNodeWithTag("post_detail_delete_menu_item", useUnmergedTree = true).performClick()

        // Confirm delete
        composeTestRule.onNodeWithTag("delete_post_confirm_button", useUnmergedTree = true).performClick()

        // Wait for post to be deleted (async operation)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("post_detail_not_found_message", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify "Post not found" message is displayed
        composeTestRule.onNodeWithTag("post_detail_not_found_message", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun deletePost_TC02_cancelDelete_confirmDialogPostRemains() {
        // TC-DELETE-02: Cancel delete - Confirm dialog, post remains
        // Input: postId = "post-1" (owned by current user)
        // Expected: Confirm dialog appears, post remains after cancel

        setupScreen("post-1", isOwner = true)

        // Wait for post to load (async operation)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("post_detail_more_button", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Click more button
        composeTestRule.onNodeWithTag("post_detail_more_button", useUnmergedTree = true).performClick()

        // Click delete option
        composeTestRule.onNodeWithTag("post_detail_delete_menu_item", useUnmergedTree = true).performClick()

        // Cancel delete
        composeTestRule.onNodeWithTag("delete_post_cancel_button", useUnmergedTree = true).performClick()

        // Verify post is still displayed (not found message should not appear)
        composeTestRule.onNodeWithTag("post_detail_not_found_message", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun deletePost_TC03_deleteOthersPost_noDeleteOption() {
        // TC-DELETE-03: Delete other's post - No delete option
        // Input: postId = "post-1" (owned by different user)
        // Expected: Delete option not available in menu

        setupScreen("post-1", isOwner = false)

        // Wait for post to load (async operation)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("post_detail_more_button", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Click more button
        composeTestRule.onNodeWithTag("post_detail_more_button", useUnmergedTree = true).performClick()

        // Verify delete option is NOT available (only report option should be available)
        composeTestRule.onNodeWithTag("post_detail_delete_menu_item", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun deletePost_TC05_postDeletedElsewhere_showsNotFound() {
        // TC-DELETE-05: Post deleted elsewhere - Show "Post not found"
        // Input: postId = "non-existent-post"
        // Expected: "Post not found" message displayed

        setupScreen("non-existent-post")

        // Wait for error state to appear (async operation)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("post_detail_not_found_message", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify "Post not found" message is displayed
        composeTestRule.onNodeWithTag("post_detail_not_found_message", useUnmergedTree = true)
            .assertExists()
            .assertIsDisplayed()
    }
}

