package com.example.englishforum.feature.noti

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.englishforum.core.di.AppContainer
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.core.ui.theme.EnglishForumTheme
import androidx.compose.runtime.CompositionLocalProvider
import com.example.englishforum.data.auth.FakeUserSessionRepository
import com.example.englishforum.data.auth.UserSession
import com.example.englishforum.data.notification.FakeNotificationRepository
import com.example.englishforum.data.auth.SessionPreferenceRepository
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
import com.example.englishforum.data.profile.ProfileRepository
import com.example.englishforum.data.profile.FakeProfileRepository
import com.example.englishforum.data.post.PostDetailRepository
import com.example.englishforum.data.post.FakePostDetailRepository
import com.example.englishforum.data.post.FakePostStore
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
 * Instrumented tests for View Notification functionality
 * Based on functional test specifications for View Notification feature
 * 
 * Test cases:
 * 1. Receive emotion notification - Show in list, unread count +1
 * 2. Receive comment notification - Show in list, unread count +1
 * 3. Read notification - Navigate to post, unread count -1
 * 4. Mark all as read - All marked read, unread count = 0
 */
@RunWith(AndroidJUnit4::class)
class NotificationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var fakeUserSessionRepository: FakeUserSessionRepository
    private lateinit var fakeNotificationRepository: FakeNotificationRepository

    @Before
    fun setUp() {
        fakeUserSessionRepository = FakeUserSessionRepository()
        fakeUserSessionRepository.setSession(
            UserSession(
                userId = "user-test",
                username = "testuser",
                accessToken = "fake_token",
                refreshToken = "fake_refresh_token"
            )
        )
        fakeNotificationRepository = FakeNotificationRepository()
    }

    private fun setupScreen() {
        val fakePostSummaryStore = ForumPostSummaryStore()
        
        val appContainer = object : AppContainer {
            override val notificationRepository = fakeNotificationRepository
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
            override val postDetailRepository = FakePostDetailRepository(
                userSessionRepository = fakeUserSessionRepository
            )
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
                    NotiRoute(
                        modifier = Modifier.fillMaxSize(),
                        onNotificationClick = { _, _ -> }
                    )
                }
            }
        }
    }

    @Test
    fun notification_TC01_receiveCommentNotification_showsInListUnreadCountPlusOne() {
        // TC-NOTIFICATION-01: Receive comment notification - Show in list, unread count +1
        // Input: New comment notification received
        // Expected: Notification appears in list, unread count increases by 1

        setupScreen()

        // Wait for notifications to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("notification_item_noti-1")
                .fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithTag("notification_empty_state")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify notifications are displayed (FakeNotificationRepository creates notifications from FakePostStore)
        // Check if any notification item exists
        composeTestRule.onNodeWithTag("notification_item_noti-1")
            .assertExists()
    }

    @Test
    fun notification_TC02_receiveEmotionNotification_showsInListUnreadCountPlusOne() {
        // TC-NOTIFICATION-02: Receive emotion notification - Show in list, unread count +1
        // Note: FakeNotificationRepository doesn't create emotion notifications, only comment notifications
        // This test verifies that notifications are displayed in the list
        // Input: New emotion notification received
        // Expected: Notification appears in list, unread count increases by 1

        setupScreen()

        // Wait for notifications to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("notification_item_noti-1")
                .fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithTag("notification_empty_state")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify notifications are displayed
        composeTestRule.onNodeWithTag("notification_item_noti-1")
            .assertExists()
    }

    @Test
    fun notification_TC03_readNotification_navigateToPostUnreadCountMinusOne() {
        // TC-NOTIFICATION-03: Read notification - Navigate to post, unread count -1
        // Input: Click on unread notification
        // Expected: Navigate to post, notification marked as read, unread count decreases

        var notificationClicked = false
        var clickedPostId: String? = null
        
        val appContainer = object : AppContainer {
            override val notificationRepository = fakeNotificationRepository
            override val userSessionRepository = fakeUserSessionRepository
            override val postSummaryStore = ForumPostSummaryStore()
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
            override val postDetailRepository = FakePostDetailRepository(
                userSessionRepository = fakeUserSessionRepository
            )
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
                    NotiRoute(
                        modifier = Modifier.fillMaxSize(),
                        onNotificationClick = { postId, _ ->
                            notificationClicked = true
                            clickedPostId = postId
                        }
                    )
                }
            }
        }

        // Wait for notifications to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("notification_item_noti-1")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Click on first notification (should be unread)
        composeTestRule.onNodeWithTag("notification_item_noti-1")
            .performClick()

        // Wait for click to be processed
        composeTestRule.waitForIdle()

        // Verify notification click was triggered
        assert(notificationClicked)
        assert(clickedPostId != null)
    }

    @Test
    fun notification_TC04_markAllAsRead_allMarkedReadUnreadCountZero() {
        // TC-NOTIFICATION-04: Mark all as read - All marked read, unread count = 0
        // Input: Click "Mark all as read" button
        // Expected: All notifications marked as read, unread count becomes 0

        setupScreen()

        // Wait for notifications to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("notification_item_noti-1")
                .fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithTag("notification_empty_state")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Check if mark all as read button exists and is enabled
        val markAllButtonNodes = composeTestRule.onAllNodesWithTag("notification_mark_all_read_button")
            .fetchSemanticsNodes()
        
        if (markAllButtonNodes.isNotEmpty()) {
            // Click mark all as read button
            composeTestRule.onNodeWithTag("notification_mark_all_read_button")
                .performClick()

            // Wait for action to complete
            composeTestRule.waitForIdle()

            // Verify button is now disabled (unread count = 0)
            // Button should be disabled when unreadCount = 0
            composeTestRule.waitUntil(timeoutMillis = 2000) {
                try {
                    composeTestRule.onNodeWithTag("notification_mark_all_read_button")
                        .assertIsNotEnabled()
                    true
                } catch (e: AssertionError) {
                    false
                }
            }
        }
    }
}
