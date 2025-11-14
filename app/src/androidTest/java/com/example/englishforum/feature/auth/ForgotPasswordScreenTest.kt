package com.example.englishforum.feature.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.englishforum.data.auth.FakeAuthRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for Forgot Password Screen
 * Based on functional test specifications for Forgot Password feature
 * 
 * Test data from FakeAuthRepository:
 * - Valid OTP: "000000"
 * - Valid password: >= 8 characters
 */
@RunWith(AndroidJUnit4::class)
class ForgotPasswordScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var viewModel: ForgotPasswordViewModel

    private fun getString(resId: Int) = composeTestRule.activity.getString(resId)
    
    // Helper: Setup screen with ViewModel
    private fun setupScreen(onBackToLogin: () -> Unit = {}, onResetSuccess: () -> Unit = {}) {
        val fakeAuthRepo = FakeAuthRepository()
        viewModel = ForgotPasswordViewModel(fakeAuthRepo)
        
        composeTestRule.setContent {
            ForgotPasswordScreen(
                viewModel = viewModel,
                onBackToLogin = onBackToLogin,
                onResetSuccess = onResetSuccess
            )
        }
    }
    
    // Helper: Chờ ViewModel state update đúng cách (dùng runOnIdle)
    private fun waitUntilState(
        timeoutMillis: Long = 5_000,
        condition: () -> Boolean
    ) {
        composeTestRule.waitUntil(timeoutMillis) {
            var result = false
            composeTestRule.runOnIdle {
                result = condition()
            }
            result
        }
    }
    
    // Helper: Request OTP và CHỜ state update thực sự
    private fun requestOtp(contact: String) {
        composeTestRule.onNodeWithTag("forgot_password_contact_field", useUnmergedTree = true)
            .performTextInput(contact)
        composeTestRule.onNodeWithTag("forgot_password_send_otp_button", useUnmergedTree = true)
            .performClick()
        
        // Chờ ViewModel xử lý xong (max 5s)
        waitUntilState(timeoutMillis = 5000) {
            viewModel.uiState.isOtpRequested || viewModel.uiState.errorMessage != null
        }
    }
    
    // Helper: Enter OTP đủ 6 số và verify (auto verify, không cần click nút)
    private fun verifyOtpFull(otp: String) {
        // Wait for OTP field to appear
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("forgot_password_otp_field", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onNodeWithTag("forgot_password_otp_field", useUnmergedTree = true)
            .performTextInput(otp)
        // Không cần click nút, vì onOtpChange sẽ tự gọi verifyOtp() khi đủ 6 số
        
        waitUntilState(timeoutMillis = 5000) {
            viewModel.uiState.isOtpVerified || viewModel.uiState.otpErrorMessage != null
        }
    }
    
    // Helper: Kiểm tra node tồn tại
    private fun assertNodeExists(tag: String) {
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ========== Phase 1: Request OTP ==========

    @Test
    fun forgotPassword_TC01_validEmail_otpSentSuccess() {
        // TC-FORGOT-01: Nhập email hợp lệ và nhận OTP
        // Input: contact = "user@example.com"
        // Expected: Hiển thị thông báo "Mã OTP đã được gửi" và chuyển sang bước nhập OTP
        
        setupScreen()

        // Enter valid email and request OTP
        requestOtp("user@example.com")
        
        // Verify OTP input field appears (indicates success)
        composeTestRule.onNodeWithTag("forgot_password_otp_field").assertExists()
    }

    @Test
    fun forgotPassword_TC02_validPhone_otpSentSuccess() {
        // TC-FORGOT-02: Nhập số điện thoại hợp lệ và nhận OTP
        // Input: contact = "0123456789"
        // Expected: Hiển thị thông báo "Mã OTP đã được gửi" và chuyển sang bước nhập OTP
        
        setupScreen()

        // Enter valid phone number
        requestOtp("0123456789")
        
        // Verify OTP input field appears
        composeTestRule.onNodeWithTag("forgot_password_otp_field").assertExists()
    }

    @Test
    fun forgotPassword_TC03_emptyContact_showsError() {
        // TC-FORGOT-03: Nhập trống thông tin liên hệ
        // Input: contact = ""
        // Expected: Hiển thị lỗi "Vui lòng nhập số điện thoại hoặc email"
        
        setupScreen()

        // Act: Request OTP với contact rỗng
        composeTestRule.onNodeWithTag("forgot_password_contact_field").performTextClearance()
        composeTestRule.onNodeWithTag("forgot_password_send_otp_button").performClick()

        // Assert: Validate đồng bộ, không cần waitUntil
        composeTestRule.onNodeWithTag("forgot_password_contact_error")
            .assertExists()
    }

    @Test
    fun forgotPassword_TC04_invalidContact_showsError() {
        // TC-FORGOT-04: Nhập thông tin liên hệ không hợp lệ
        // Input: contact = "invalid"
        // Expected: Hiển thị lỗi "Thông tin liên hệ không hợp lệ"
        
        setupScreen()

        // Enter invalid contact
        requestOtp("invalid")
        
        // Verify error message
        composeTestRule.onNodeWithTag("forgot_password_contact_error").assertExists()
    }

    // ========== Phase 2: Verify OTP ==========

    @Test
    fun forgotPassword_TC05_validOTP_verificationSuccess() {
        // TC-FORGOT-05: Nhập OTP đúng
        // Input: contact = "user@example.com", otp = "000000"
        // Expected: Xác thực thành công, chuyển sang bước đặt lại mật khẩu
        
        setupScreen()

        // Step 1: Request OTP
        requestOtp("user@example.com")
        
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("forgot_password_otp_field").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Step 2: Enter valid OTP and verify (auto verify khi đủ 6 số)
        verifyOtpFull("000000")
        
        // Wait for OTP verification to complete
        waitUntilState {
            viewModel.uiState.isOtpVerified
        }
        
        // Verify password reset fields appear
        composeTestRule.onNodeWithTag("forgot_password_new_password_field").assertExists()
    }

    @Test
    fun forgotPassword_TC06_invalidOTP_showsError() {
        // TC-FORGOT-06: Nhập OTP sai
        // Input: contact = "user@example.com", otp = "123456"
        // Expected: Hiển thị lỗi "Mã OTP không đúng"
        
        setupScreen()

        // Step 1: Request OTP
        requestOtp("user@example.com")
        
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("forgot_password_otp_field").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Step 2: Enter invalid OTP (đủ 6 số, sẽ auto verify và trả về error)
        verifyOtpFull("123456")
        
        // Wait for error to appear
        waitUntilState {
            viewModel.uiState.otpErrorMessage != null
        }
        
        // Verify error message
        composeTestRule.onNodeWithTag("forgot_password_otp_error").assertExists()
    }

    @Test
    fun forgotPassword_TC07_incompleteOTP_showsError() {
        // TC-FORGOT-07: Nhập OTP không đủ 6 số
        // Input: contact = "user@example.com", otp = "123"
        // Expected: Hiển thị lỗi "Mã OTP phải gồm 6 số"
        
        setupScreen()

        // Step 1: Request OTP
        requestOtp("user@example.com")
        
        // Chờ field OTP xuất hiện
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("forgot_password_otp_field")
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Step 2: Nhập OTP thiếu và CLICK NÚT XÁC NHẬN
        composeTestRule.onNodeWithTag("forgot_password_otp_field")
            .performTextInput("123")
        composeTestRule.onNodeWithTag("forgot_password_verify_otp_button")
            .performClick()
        
        // Chờ ViewModel set error
        waitUntilState {
            viewModel.uiState.otpErrorMessage != null
        }
        
        // Assert: error hiển thị
        composeTestRule.onNodeWithTag("forgot_password_otp_error")
            .assertExists()
    }

    @Test
    fun forgotPassword_TC08_emptyOTP_showsError() {
        // TC-FORGOT-08: Nhập OTP trống
        // Input: contact = "user@example.com", otp = ""
        // Expected: Hiển thị lỗi "Vui lòng nhập mã OTP"
        
        setupScreen()

        // Step 1: Request OTP thành công
        requestOtp("user@example.com")
        
        // Chờ OTP field xuất hiện
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("forgot_password_otp_field").fetchSemanticsNodes().isNotEmpty()
        }

        // Step 2: Clear OTP và click verify (nút verify vẫn tồn tại khi OTP rỗng)
        composeTestRule.onNodeWithTag("forgot_password_otp_field").performTextClearance()
        composeTestRule.onNodeWithTag("forgot_password_verify_otp_button").performClick()

        // Assert: Kiểm tra error
        waitUntilState(timeoutMillis = 3000) {
            viewModel.uiState.otpErrorMessage != null
        }
        
        composeTestRule.onNodeWithTag("forgot_password_otp_error")
            .assertExists()
    }

    // ========== Phase 3: Reset Password ==========

    @Test
    fun forgotPassword_TC09_validNewPassword_resetSuccess() {
        // TC-FORGOT-09: Đặt lại mật khẩu hợp lệ
        // Input: contact = "user@example.com", otp = "000000", 
        //        newPassword = "NewPass@123", confirmPassword = "NewPass@123"
        // Expected: Đặt lại mật khẩu thành công, quay về màn hình đăng nhập
        
        var resetSuccess = false
        setupScreen(onResetSuccess = { resetSuccess = true })

        // Step 1: Request OTP
        requestOtp("user@example.com")
        
        // Step 2: Verify OTP (auto verify khi đủ 6 số)
        verifyOtpFull("000000")
        
        waitUntilState {
            viewModel.uiState.isOtpVerified
        }
        
        // Step 3: Enter new password
        composeTestRule.onNodeWithTag("forgot_password_new_password_field", useUnmergedTree = true)
            .performTextInput("NewPass@123")
        composeTestRule.onNodeWithTag("forgot_password_confirm_password_field", useUnmergedTree = true)
            .performTextInput("NewPass@123")
        composeTestRule.onNodeWithTag("forgot_password_change_password_button", useUnmergedTree = true)
            .performClick()
        
        // Wait for async operation to complete (check ViewModel state)
        waitUntilState(timeoutMillis = 5000) {
            !viewModel.uiState.isChangingPassword && 
            (viewModel.uiState.successMessage != null || resetSuccess)
        }
        
        // Verify reset success
        assert(resetSuccess || viewModel.uiState.successMessage != null)
    }

    @Test
    fun forgotPassword_TC10_weakNewPassword_showsError() {
        // TC-FORGOT-10: Đặt lại mật khẩu yếu (< 8 ký tự)
        // Input: contact = "user@example.com", otp = "000000", 
        //        newPassword = "123", confirmPassword = "123"
        // Expected: Hiển thị lỗi "Mật khẩu phải có ít nhất 8 ký tự"
        
        setupScreen()

        // Step 1: Request OTP
        requestOtp("user@example.com")
        
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("forgot_password_otp_field").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Step 2: Verify OTP (auto verify khi đủ 6 số)
        verifyOtpFull("000000")
        
        waitUntilState {
            viewModel.uiState.isOtpVerified
        }
        
        // Step 3: Enter weak password
        composeTestRule.onNodeWithTag("forgot_password_new_password_field").performTextInput("123")
        composeTestRule.onNodeWithTag("forgot_password_confirm_password_field").performTextInput("123")
        composeTestRule.onNodeWithTag("forgot_password_change_password_button").performClick()
        
        // Wait for error to appear
        waitUntilState(timeoutMillis = 3000) {
            viewModel.uiState.passwordErrorMessage != null
        }
        
        // Verify error message
        composeTestRule.onNodeWithTag("forgot_password_password_error").assertExists()
    }

    @Test
    fun forgotPassword_TC11_passwordMismatch_showsError() {
        // TC-FORGOT-11: Mật khẩu xác nhận không khớp
        // Input: contact = "user@example.com", otp = "000000", 
        //        newPassword = "NewPass@123", confirmPassword = "NewPass@456"
        // Expected: Hiển thị lỗi "Mật khẩu xác nhận không khớp"
        
        setupScreen()

        // Step 1 & 2: Request và verify OTP
        requestOtp("user@example.com")
        
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("forgot_password_otp_field").fetchSemanticsNodes().isNotEmpty()
        }
        
        verifyOtpFull("000000")
        
        waitUntilState {
            viewModel.uiState.isOtpVerified
        }

        // Step 3: Nhập password không khớp
        composeTestRule.onNodeWithTag("forgot_password_new_password_field").performTextInput("NewPass@123")
        composeTestRule.onNodeWithTag("forgot_password_confirm_password_field").performTextInput("NewPass@456")
        composeTestRule.onNodeWithTag("forgot_password_change_password_button").performClick()

        // Assert: Kiểm tra error
        waitUntilState(timeoutMillis = 3000) {
            viewModel.uiState.passwordErrorMessage != null
        }
        
        composeTestRule.onNodeWithTag("forgot_password_password_error")
            .assertExists()
    }

    @Test
    fun forgotPassword_displaysAllUIElements_phase1() {
        // Additional test: Verify Phase 1 UI elements
        
        setupScreen()

        // Verify Phase 1 UI elements
        composeTestRule.onNodeWithTag("forgot_password_contact_field").assertExists()
        composeTestRule.onNodeWithTag("forgot_password_send_otp_button").assertExists()
        composeTestRule.onNodeWithTag("forgot_password_back_button").assertExists()
    }

    @Test
    fun forgotPassword_backButton_navigatesToLogin() {
        // Additional test: Verify back button works
        
        var backClicked = false
        setupScreen(onBackToLogin = { backClicked = true })

        // Click back button
        composeTestRule.onNodeWithTag("forgot_password_back_button").performClick()
        
        assert(backClicked)
    }
}

