package com.example.englishforum.feature.auth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.wrapContentHeight
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
    onDone: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

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

            if (!viewModel.isOtpRequested) {
                ContactStep(viewModel = viewModel, onDone = onDone)
            } else {
                OtpStep(viewModel = viewModel, onDone = onDone)
            }
        }
    }
}

@Composable
private fun ContactStep(
    viewModel: ForgotPasswordViewModel,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = viewModel.contact,
            onValueChange = viewModel::onContactChange,
            label = { Text(text = "Số điện thoại hoặc Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        viewModel.errorMessage?.let { err ->
            Text(text = err, color = MaterialTheme.colorScheme.error)
        }

        if (viewModel.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp))
        }

        val buttonLabel = if (viewModel.otpSecondsRemaining > 0) {
            "Gửi lại (${viewModel.otpSecondsRemaining}s)"
        } else {
            "Gửi OTP"
        }

        Button(
            onClick = { viewModel.submit() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isLoading && viewModel.otpSecondsRemaining == 0
        ) {
            Text(text = buttonLabel)
        }

        TextButton(
            onClick = {
                viewModel.clearMessages()
                onDone()
            },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text(text = "Quay lại")
        }
    }
}

@Composable
private fun OtpStep(
    viewModel: ForgotPasswordViewModel,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = viewModel.successMessage
                ?: "Mã OTP đã được gửi đến ${viewModel.contact}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        if (viewModel.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }

        OutlinedTextField(
            value = viewModel.otp,
            onValueChange = viewModel::onOtpChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            readOnly = viewModel.isOtpVerified,
            enabled = !viewModel.isOtpVerified,
            label = { Text(text = "Mã OTP") },
            textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { viewModel.verifyOtp() })
        )

        viewModel.otpErrorMessage?.let { err ->
            Text(text = err, color = MaterialTheme.colorScheme.error)
        }

        if (!viewModel.isOtpVerified) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { viewModel.clearMessages() }) {
                    Text(text = "Thay đổi email/số khác")
                }

                val resendEnabled = viewModel.otpSecondsRemaining == 0 && !viewModel.isLoading
                TextButton(onClick = { viewModel.submit() }, enabled = resendEnabled) {
                    val resendLabel = if (viewModel.otpSecondsRemaining > 0) {
                        "Gửi lại (${viewModel.otpSecondsRemaining}s)"
                    } else {
                        "Gửi lại OTP"
                    }
                    Text(text = resendLabel)
                }
            }
        }

        if (viewModel.isOtpVerified) {
            var newPasswordVisible by rememberSaveable { mutableStateOf(false) }
            var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.newPassword,
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
                    value = viewModel.confirmNewPassword,
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
                    keyboardActions = KeyboardActions(onDone = { viewModel.changePassword(onSuccess = onDone) })
                )

                viewModel.passwordErrorMessage?.let { err ->
                    Text(text = err, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = { viewModel.changePassword(onSuccess = onDone) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isChangingPassword
                ) {
                    if (viewModel.isChangingPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(text = "Đổi mật khẩu")
                    }
                }
            }
        }
    }
}
