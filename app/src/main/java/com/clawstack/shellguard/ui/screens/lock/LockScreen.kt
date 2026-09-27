package com.clawstack.shellguard.ui.screens.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.clawstack.shellguard.crypto.BiometricAuthManager
import com.clawstack.shellguard.ui.theme.*

@Composable
fun LockScreen(
    viewModel: LockViewModel,
    onUnlocked: () -> Unit,
    onFallbackToGateway: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var biometricError by remember { mutableStateOf<String?>(null) }
    val canUseBiometrics = remember {
        activity != null && BiometricAuthManager.canAuthenticate(context)
    }

    fun triggerBiometrics() {
        if (activity == null || !canUseBiometrics) return
        biometricError = null

        BiometricAuthManager.authenticate(
            activity = activity,
            title = "Unlock ShellGuard Vault",
            subtitle = "Verify your biometric identity to restore active access",
            onSuccess = {
                viewModel.unlock()
                onUnlocked()
            },
            onError = { code, err ->
                if (code != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                    code != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    biometricError = err.toString()
                }
            },
            onFailed = {
                biometricError = "Biometric recognition failed. Please try again."
            }
        )
    }

    LaunchedEffect(Unit) {
        if (canUseBiometrics) {
            triggerBiometrics()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OceanDark)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                ReefPink.copy(alpha = 0.25f),
                                BrandClawCyan.copy(alpha = 0.25f)
                            )
                        )
                    )
                    .border(2.dp, ReefPink.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Vault Locked",
                    tint = ReefPink,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Vault Locked",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Session active for ${viewModel.username}",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            if (viewModel.serverUrl.isNotBlank()) {
                Text(
                    text = viewModel.serverUrl,
                    fontSize = 12.sp,
                    color = BrandClawCyan,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (biometricError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = biometricError ?: "",
                    color = WarningText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (canUseBiometrics) {
                Button(
                    onClick = { triggerBiometrics() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReefPink,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unlock with Biometrics",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedButton(
                onClick = {
                    onFallbackToGateway()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Text("Enter Master Key / Re-authenticate", fontWeight = FontWeight.SemiBold)
            }
        }

        TextButton(
            onClick = {
                viewModel.logout()
                onFallbackToGateway()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Log Out & Switch Server", fontSize = 13.sp)
        }
    }
}
