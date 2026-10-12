package com.clawstack.shellguard.ui.screens.lock

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.clawstack.shellguard.crypto.BiometricAuthManager
import com.clawstack.shellguard.data.local.UnlockMethod
import com.clawstack.shellguard.ui.components.PinPadView
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

    val settings by viewModel.settings.collectAsState()
    val unlockMethod = settings.unlockMethod
    val hasPinConfigured = settings.hasPinConfigured

    var activeUnlockMode by remember(unlockMethod, hasPinConfigured) {
        mutableStateOf(
            when {
                unlockMethod == UnlockMethod.PIN && hasPinConfigured -> UnlockMethod.PIN
                unlockMethod == UnlockMethod.BIOMETRIC -> UnlockMethod.BIOMETRIC
                else -> UnlockMethod.MASTER_KEY
            }
        )
    }

    var biometricError by remember { mutableStateOf<String?>(null) }
    val canUseBiometrics = remember {
        activity != null && BiometricAuthManager.canAuthenticate(context)
    }

    // PIN state
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    // Master Key Modal state
    var showMasterKeyModal by remember { mutableStateOf(false) }
    var masterKeyInput by remember { mutableStateOf("") }
    var masterKeyError by remember { mutableStateOf<String?>(null) }
    var isMasterKeyMasked by remember { mutableStateOf(true) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val content = viewModel.readClawKeyFromUri(context, uri)
            if (content != null) {
                val success = viewModel.verifyClawKeyContent(content)
                if (success) {
                    showMasterKeyModal = false
                    onUnlocked()
                } else {
                    masterKeyError = "ClawKey in selected file does not match this vault session."
                }
            } else {
                masterKeyError = "Failed to read ClawKey file."
            }
        }
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

    LaunchedEffect(activeUnlockMode) {
        if (activeUnlockMode == UnlockMethod.BIOMETRIC && canUseBiometrics) {
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
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Vault Icon ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(68.dp)
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
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Vault Locked",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Session active for ${viewModel.username}",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            if (viewModel.serverUrl.isNotBlank()) {
                Text(
                    text = viewModel.serverUrl,
                    fontSize = 12.sp,
                    color = BrandClawCyan,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Dynamic Unlock Presentation ─────────────────────────────────
            when {
                activeUnlockMode == UnlockMethod.PIN && hasPinConfigured -> {
                    if (viewModel.isPinLockedOut()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = StatusWarning.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "PIN Lockout Active",
                                    color = StatusWarning,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Too many failed attempts. Enter your Master Key to unlock.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        masterKeyError = null
                                        masterKeyInput = ""
                                        showMasterKeyModal = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ReefPink),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Enter Master Key", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        PinPadView(
                            currentPin = pinInput,
                            onPinChange = {
                                pinInput = it
                                pinError = null
                            },
                            pinLength = 6,
                            isError = pinError != null,
                            errorMessage = pinError,
                            onPinComplete = { completedPin ->
                                val success = viewModel.verifyPin(completedPin)
                                if (success) {
                                    onUnlocked()
                                } else {
                                    val remaining = viewModel.getRemainingPinAttempts()
                                    pinError = if (remaining <= 0) {
                                        "Too many failed attempts. Master Key required."
                                    } else {
                                        "Incorrect PIN. $remaining attempts remaining."
                                    }
                                    pinInput = ""
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (canUseBiometrics) {
                                TextButton(
                                    onClick = {
                                        activeUnlockMode = UnlockMethod.BIOMETRIC
                                    }
                                ) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrandClawCyan)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Use Biometrics", fontSize = 12.sp, color = BrandClawCyan)
                                }
                            }

                            TextButton(
                                onClick = {
                                    masterKeyError = null
                                    masterKeyInput = ""
                                    showMasterKeyModal = true
                                }
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp), tint = ReefPink)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Use Master Key", fontSize = 12.sp, color = ReefPink)
                            }
                        }
                    }
                }

                activeUnlockMode == UnlockMethod.BIOMETRIC -> {
                    if (biometricError != null) {
                        Text(
                            text = biometricError ?: "",
                            color = WarningText,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

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

                    if (hasPinConfigured) {
                        OutlinedButton(
                            onClick = {
                                activeUnlockMode = UnlockMethod.PIN
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text("Unlock with PIN Code", fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedButton(
                        onClick = {
                            masterKeyError = null
                            masterKeyInput = ""
                            showMasterKeyModal = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text("Enter Master Key / ClawKey", fontWeight = FontWeight.SemiBold)
                    }
                }

                else -> {
                    // Master Key Only mode
                    if (hasPinConfigured) {
                        Button(
                            onClick = {
                                activeUnlockMode = UnlockMethod.PIN
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ReefPink)
                        ) {
                            Text("Unlock with PIN Code", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedButton(
                        onClick = {
                            masterKeyError = null
                            masterKeyInput = ""
                            showMasterKeyModal = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp), tint = ReefPink)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enter Master Key / Re-authenticate", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Logout Action ───────────────────────────────────────────────
            TextButton(
                onClick = {
                    viewModel.logout()
                    onFallbackToGateway()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Out & Switch Server", fontSize = 13.sp)
            }
        }
    }

    // ── Inline Master Key / ClawKey Re-Authentication Modal ─────────────────
    if (showMasterKeyModal) {
        AlertDialog(
            onDismissRequest = {
                showMasterKeyModal = false
                masterKeyError = null
                masterKeyInput = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = ReefPink, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Re-authenticate Vault", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Enter your 67-character sovereign key (hu-...) or upload your ClawKey file to unlock your active session.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = masterKeyInput,
                        onValueChange = {
                            masterKeyInput = it
                            masterKeyError = null
                        },
                        label = { Text("Master Sovereign Key", fontSize = 12.sp) },
                        placeholder = { Text("hu-...", fontSize = 12.sp, color = TextMuted) },
                        visualTransformation = if (isMasterKeyMasked) PasswordVisualTransformation() else VisualTransformation.None,
                        trailingIcon = {
                            IconButton(onClick = { isMasterKeyMasked = !isMasterKeyMasked }) {
                                Icon(
                                    imageVector = if (isMasterKeyMasked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle key visibility",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        isError = masterKeyError != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ReefPink,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    )

                    if (masterKeyError != null) {
                        Text(
                            text = masterKeyError ?: "",
                            color = StatusError,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch("*/*")
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandClawCyan)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload ClawKey File (JSON)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.verifyMasterKey(masterKeyInput)
                        if (success) {
                            showMasterKeyModal = false
                            masterKeyInput = ""
                            masterKeyError = null
                            onUnlocked()
                        } else {
                            masterKeyError = "Invalid sovereign key. Does not match active session."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ReefPink),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Unlock Vault", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMasterKeyModal = false
                        masterKeyError = null
                        masterKeyInput = ""
                    }
                ) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
