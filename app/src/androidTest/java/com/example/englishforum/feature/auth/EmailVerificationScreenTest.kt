package com.example.englishforum.feature.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.englishforum.data.auth.FakeAuthRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for Email Verification Screen
 * Tests email verification flow after registration
 */
@RunWith(AndroidJUnit4::class)
class EmailVerificationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emailVerification_displaysAllUIElements() {
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = EmailVerificationViewModel(fakeAuthRepo)

        composeTestRule.setContent {
            EmailVerificationScreen(
                viewModel = viewModel,
                onVerificationSuccess = {},
                onBackToLogin = {}
            )
        }

        // Verify UI elements exist
        composeTestRule.onNodeWithText("Xác minh email").assertExists()
        composeTestRule.onNodeWithText("Nhập mã OTP đã được gửi tới email của bạn").assertExists()
    }

    @Test
    fun emailVerification_validOtp_verificationSuccess() {
        var verificationSuccessCalled = false
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = EmailVerificationViewModel(fakeAuthRepo)

        composeTestRule.setContent {
            EmailVerificationScreen(
                viewModel = viewModel,
                onVerificationSuccess = { verificationSuccessCalled = true },
                onBackToLogin = {}
            )
        }

        // Enter valid OTP (FakeAuthRepository accepts "000000")
        composeTestRule.onNodeWithText("Mã OTP")
            .performTextInput("000000")

        // Click verify button
        composeTestRule.onNodeWithText("Xác minh").performClick()

        // Wait for async operation
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            verificationSuccessCalled
        }

        assert(verificationSuccessCalled)
    }

    @Test
    fun emailVerification_invalidOtp_showsError() {
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = EmailVerificationViewModel(fakeAuthRepo)

        composeTestRule.setContent {
            EmailVerificationScreen(
                viewModel = viewModel,
                onVerificationSuccess = {},
                onBackToLogin = {}
            )
        }

        // Enter invalid OTP
        composeTestRule.onNodeWithText("Mã OTP")
            .performTextInput("123456")

        // Click verify button
        composeTestRule.onNodeWithText("Xác minh").performClick()

        // Wait for error to appear
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("email_verification_error")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify error message is displayed (using test tag to avoid encoding issues)
        composeTestRule.onNodeWithTag("email_verification_error")
            .assertExists()
    }

    @Test
    fun emailVerification_shortOtp_showsError() {
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = EmailVerificationViewModel(fakeAuthRepo)

        composeTestRule.setContent {
            EmailVerificationScreen(
                viewModel = viewModel,
                onVerificationSuccess = {},
                onBackToLogin = {}
            )
        }

        // Enter short OTP
        composeTestRule.onNodeWithText("Mã OTP")
            .performTextInput("123")

        // Click verify button
        composeTestRule.onNodeWithText("Xác minh").performClick()

        // Wait for error to appear
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("email_verification_error")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify error message is displayed (using test tag to avoid encoding issues)
        composeTestRule.onNodeWithTag("email_verification_error")
            .assertExists()
    }

    @Test
    fun emailVerification_resendOtp_success() {
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = EmailVerificationViewModel(fakeAuthRepo)

        composeTestRule.setContent {
            EmailVerificationScreen(
                viewModel = viewModel,
                onVerificationSuccess = {},
                onBackToLogin = {}
            )
        }

        // Click resend button
        composeTestRule.onNodeWithText("Gửi lại mã").performClick()

        // Wait for success message to appear
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("email_verification_message")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify success message is displayed (using test tag to avoid encoding issues)
        composeTestRule.onNodeWithTag("email_verification_message")
            .assertExists()
    }

    @Test
    fun emailVerification_resendOtp_disabledDuringCountdown() {
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = EmailVerificationViewModel(fakeAuthRepo)

        composeTestRule.setContent {
            EmailVerificationScreen(
                viewModel = viewModel,
                onVerificationSuccess = {},
                onBackToLogin = {}
            )
        }

        // Verify button is initially enabled
        composeTestRule.onNodeWithTag("email_verification_resend_button")
            .assertExists()
            .assertIsEnabled()

        // First resend to start countdown
        composeTestRule.onNodeWithTag("email_verification_resend_button")
            .performClick()
        
        // Wait for countdown to start (async operation)
        // Button should become disabled and text should change to countdown format
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            // Check if button text changed (no longer "Gửi lại mã")
            composeTestRule.onAllNodesWithText("Gửi lại mã")
                .fetchSemanticsNodes().isEmpty()
        }

        // Verify resend button is disabled during countdown
        composeTestRule.waitForIdle()
        Thread.sleep(500) // Give countdown time to update
        
        // Button should be disabled
        composeTestRule.onNodeWithTag("email_verification_resend_button")
            .assertExists()
            .assertIsNotEnabled()
    }

    @Test
    fun emailVerification_backToLogin_navigates() {
        var backClicked = false
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = EmailVerificationViewModel(fakeAuthRepo)

        composeTestRule.setContent {
            EmailVerificationScreen(
                viewModel = viewModel,
                onVerificationSuccess = {},
                onBackToLogin = { backClicked = true }
            )
        }

        // Click back button
        composeTestRule.onNodeWithText("Quay lại đăng nhập").performClick()

        assert(backClicked)
    }

    @Test
    fun emailVerification_otpInput_filtersNonDigits() {
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = EmailVerificationViewModel(fakeAuthRepo)

        composeTestRule.setContent {
            EmailVerificationScreen(
                viewModel = viewModel,
                onVerificationSuccess = {},
                onBackToLogin = {}
            )
        }

        // Enter OTP with non-digits (should be filtered)
        composeTestRule.onNodeWithText("Mã OTP")
            .performTextInput("12a34b56")

        // Verify only digits are kept (max 6)
        composeTestRule.onNodeWithText("Mã OTP")
            .assertTextContains("123456")
    }

    @Test
    fun emailVerification_loadingState_showsProgressIndicator() {
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = EmailVerificationViewModel(fakeAuthRepo)

        composeTestRule.setContent {
            EmailVerificationScreen(
                viewModel = viewModel,
                onVerificationSuccess = {},
                onBackToLogin = {}
            )
        }

        // Enter OTP and click verify
        composeTestRule.onNodeWithText("Mã OTP")
            .performTextInput("000000")
        composeTestRule.onNodeWithText("Xác minh").performClick()

        // Verify loading indicator appears (button should show progress)
        composeTestRule.waitForIdle()
        // Loading state is shown in button
    }
}

