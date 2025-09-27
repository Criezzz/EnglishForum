package com.example.englishforum.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.englishforum.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onBackToLogin: () -> Unit,
    onResetSuccess: () -> Unit
) {
    val scrollState = rememberScrollState()
    val uiState = viewModel.uiState

    BackHandler {
        viewModel.clearMessages()
        onBackToLogin()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!uiState.isOtpRequested) {
                ContactStep(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBackToLogin = onBackToLogin
                )
            } else {
                OtpStep(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBackToLogin = onBackToLogin,
                    onResetSuccess = onResetSuccess
                )
            }
        }
    }
}

@Composable
private fun ContactStep(
    uiState: ForgotPasswordUiState,
    viewModel: ForgotPasswordViewModel,
    onBackToLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = uiState.contact,
            onValueChange = viewModel::onContactChange,
            label = { Text(text = "Số điện thoại hoặc Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        uiState.errorMessage?.let { err ->
            Text(text = err, color = MaterialTheme.colorScheme.error)
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp))
        }

        val buttonLabel = if (uiState.otpSecondsRemaining > 0) {
            "Gửi lại (${uiState.otpSecondsRemaining}s)"
        } else {
            "Gửi OTP"
        }

        Button(
            onClick = { viewModel.submit() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && uiState.otpSecondsRemaining == 0
        ) {
            Text(text = buttonLabel)
        }

        TextButton(
            onClick = {
                viewModel.clearMessages()
                onBackToLogin()
            },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text(text = "Quay lại")
        }
    }
}

@Composable
private fun OtpStep(
    uiState: ForgotPasswordUiState,
    viewModel: ForgotPasswordViewModel,
    onBackToLogin: () -> Unit,
    onResetSuccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = uiState.successMessage
                ?: "Mã OTP đã được gửi đến ${uiState.contact}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }

        OutlinedTextField(
            value = uiState.otp,
            onValueChange = viewModel::onOtpChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            readOnly = uiState.isOtpVerified,
            enabled = !uiState.isOtpVerified,
            label = { Text(text = "Mã OTP") },
            textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { viewModel.verifyOtp() })
        )

        uiState.otpErrorMessage?.let { err ->
            Text(text = err, color = MaterialTheme.colorScheme.error)
        }

        if (!uiState.isOtpVerified) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { viewModel.clearMessages() }) {
                    Text(text = "Thay đổi email/số khác")
                }

                val resendEnabled = uiState.otpSecondsRemaining == 0 && !uiState.isLoading
                TextButton(onClick = { viewModel.submit() }, enabled = resendEnabled) {
                    val resendLabel = if (uiState.otpSecondsRemaining > 0) {
                        "Gửi lại (${uiState.otpSecondsRemaining}s)"
                    } else {
                        "Gửi lại OTP"
                    }
                    Text(text = resendLabel)
                }
            }
        }

        if (uiState.isOtpVerified) {
            var newPasswordVisible by rememberSaveable { mutableStateOf(false) }
            var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.newPassword,
                    onValueChange = viewModel::onNewPasswordChange,
                    label = { Text(text = "Mật khẩu mới") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Text(
                            text = if (newPasswordVisible) "Ẩn" else "Hiện",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable { newPasswordVisible = !newPasswordVisible }
                        )
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = uiState.confirmNewPassword,
                    onValueChange = viewModel::onConfirmNewPasswordChange,
                    label = { Text(text = "Xác nhận mật khẩu") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Text(
                            text = if (confirmPasswordVisible) "Ẩn" else "Hiện",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable { confirmPasswordVisible = !confirmPasswordVisible }
                        )
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.changePassword(onSuccess = onResetSuccess) })
                )

                uiState.passwordErrorMessage?.let { err ->
                    Text(text = err, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = { viewModel.changePassword(onSuccess = onResetSuccess) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isChangingPassword
                ) {
                    if (uiState.isChangingPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(text = "Đổi mật khẩu")
                    }
                }
            }
        }
    }
}
