package com.clawstack.shellguard.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import com.clawstack.shellguard.crypto.BiometricAuthManager
import com.clawstack.shellguard.data.local.UnlockMethod
import com.clawstack.shellguard.ui.components.PinPadView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.crypto.LockTimeout
import com.clawstack.shellguard.ui.components.CircularDialPicker
import com.clawstack.shellguard.ui.theme.BorderSubtle
import com.clawstack.shellguard.ui.theme.BrandClawCyan
import com.clawstack.shellguard.ui.theme.OceanDark
import com.clawstack.shellguard.ui.theme.ReefPink
import com.clawstack.shellguard.ui.theme.StatusError
import com.clawstack.shellguard.ui.theme.SurfaceDark
import com.clawstack.shellguard.ui.theme.TextMuted
import com.clawstack.shellguard.ui.theme.TextPrimary
import com.clawstack.shellguard.ui.theme.TextSecondary
import com.clawstack.shellguard.ui.theme.WarningText

@Composable
fun SettingsSecurityScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onNavigateToPanicCountdown: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    var showPanicConfirmDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val canUseBiometrics = remember {
        BiometricAuthManager.canAuthenticate(context)
    }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var pinSetupStep by remember { mutableStateOf(1) }
    var firstEnteredPin by remember { mutableStateOf("") }
    var currentPinInput by remember { mutableStateOf("") }
    var pinDialogError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = OceanDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            // ── Top Bar ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "Security & Privacy",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Vault timeouts, screen shield & panic purge",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Section 0: Vault Unlock Method ──────────────────────────
                item {
                    Text(
                        text = "VAULT UNLOCK METHOD",
                        color = BrandClawCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            // Option 1: Biometrics
                            SecurityOptionRow(
                                title = "Biometrics (Fingerprint / Face)",
                                subtitle = if (canUseBiometrics) {
                                    "Fast unlock via device hardware sensor"
                                } else {
                                    "Hardware biometric sensor not enrolled or unavailable"
                                },
                                selected = settings.unlockMethod == UnlockMethod.BIOMETRIC,
                                onClick = {
                                    if (canUseBiometrics) {
                                        viewModel.updateUnlockMethod(UnlockMethod.BIOMETRIC)
                                    }
                                }
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(1.dp)
                                    .background(BorderSubtle.copy(alpha = 0.5f))
                            )

                            // Option 2: PIN Code
                            SecurityOptionRow(
                                title = "PIN Code (4–8 Digits)",
                                subtitle = if (settings.hasPinConfigured) {
                                    "Fast numeric keypad unlock (PIN Active)"
                                } else {
                                    "Quick keypad unlock (Tap to configure PIN)"
                                },
                                selected = settings.unlockMethod == UnlockMethod.PIN,
                                onClick = {
                                    if (settings.hasPinConfigured) {
                                        viewModel.updateUnlockMethod(UnlockMethod.PIN)
                                    } else {
                                        pinSetupStep = 1
                                        firstEnteredPin = ""
                                        currentPinInput = ""
                                        pinDialogError = null
                                        showPinSetupDialog = true
                                    }
                                }
                            )

                            if (settings.hasPinConfigured) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            pinSetupStep = 1
                                            firstEnteredPin = ""
                                            currentPinInput = ""
                                            pinDialogError = null
                                            showPinSetupDialog = true
                                        },
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, ReefPink.copy(alpha = 0.5f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ReefPink)
                                    ) {
                                        Text("Change PIN", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.clearPin()
                                        },
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, BorderSubtle),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                                    ) {
                                        Text("Remove PIN", fontSize = 12.sp)
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(1.dp)
                                    .background(BorderSubtle.copy(alpha = 0.5f))
                            )

                            // Option 3: Master Key / ClawKey Only
                            SecurityOptionRow(
                                title = "Master Key / ClawKey Only",
                                subtitle = "Require entering 67-char sovereign key or file on every unlock",
                                selected = settings.unlockMethod == UnlockMethod.MASTER_KEY,
                                onClick = { viewModel.updateUnlockMethod(UnlockMethod.MASTER_KEY) }
                            )
                        }
                    }
                }

                // If PIN is enabled or configured, show Max Failed Attempts
                if (settings.unlockMethod == UnlockMethod.PIN || settings.hasPinConfigured) {
                    item {
                        Text(
                            text = "MAX FAILED PIN ATTEMPTS",
                            color = BrandClawCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, BorderSubtle),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Lockout Threshold",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Exceeding limit disables PIN and forces Master Key re-authentication",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(3, 5, 10).forEach { attempts ->
                                        val isSelected = settings.maxPinAttempts == attempts
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) ReefPink.copy(alpha = 0.2f) else SurfaceDark
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) ReefPink else BorderSubtle,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { viewModel.updateMaxPinAttempts(attempts) }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$attempts Attempts",
                                                color = if (isSelected) ReefPink else TextMuted,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Section 1: Vault Lock Timeout ───────────────────────────
                item {
                    Text(
                        text = "AUTO-LOCK TIMEOUT",
                        color = BrandClawCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            LockTimeout.entries.forEachIndexed { index, timeout ->
                                SecurityOptionRow(
                                    title = timeout.label,
                                    subtitle = when (timeout) {
                                        LockTimeout.IMMEDIATE -> "Locks the moment app is backgrounded"
                                        LockTimeout.ONE_MINUTE -> "Locks after 60 seconds in background"
                                        LockTimeout.FIVE_MINUTES -> "Default recommended balance"
                                        LockTimeout.FIFTEEN_MINUTES -> "Extended working session"
                                        LockTimeout.NEVER -> "Requires explicit manual lock from menu"
                                    },
                                    selected = settings.lockTimeout == timeout,
                                    onClick = { viewModel.updateLockTimeout(timeout) }
                                )

                                if (index < LockTimeout.entries.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .height(1.dp)
                                            .background(BorderSubtle.copy(alpha = 0.5f))
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Section 2: Screen Capture Shield ────────────────────────
                item {
                    Text(
                        text = "SCREEN CAPTURE SHIELD",
                        color = BrandClawCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ReefPink.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NoPhotography,
                                        contentDescription = null,
                                        tint = ReefPink,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Allow Screen Capture",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "When disabled, Android FLAG_SECURE blocks screenshots and recents previews",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Switch(
                                    checked = settings.allowScreenCapture,
                                    onCheckedChange = { viewModel.updateAllowScreenCapture(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = ReefPink,
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = SurfaceDark,
                                        uncheckedBorderColor = BorderSubtle
                                    )
                                )
                            }

                            AnimatedVisibility(visible = settings.allowScreenCapture) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Security Warning: Screen capture exposes decrypted passwords to recording tools and system multitasking thumbnails.",
                                        color = Color(0xFFF59E0B),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Section 3: Sensitive Clipboard ──────────────────────────
                item {
                    Text(
                        text = "SENSITIVE CLIPBOARD SCRUB",
                        color = BrandClawCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BrandClawCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        tint = BrandClawCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Auto-Clear Copied Secrets",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Overwrites OS clipboard buffer after timeout",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(15, 30, 60, 120).forEach { seconds ->
                                    val isSelected = settings.clipboardClearSeconds == seconds
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) ReefPink.copy(alpha = 0.2f) else SurfaceDark
                                            )
                                            .clickable { viewModel.updateClipboardClearSeconds(seconds) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${seconds}s",
                                            color = if (isSelected) ReefPink else TextMuted,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Section 4: Emergency Panic Purge Configuration ──────────
                item {
                    Text(
                        text = "EMERGENCY PANIC PURGE",
                        color = StatusError,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StatusError.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = StatusError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Countdown Duration",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Grace period before local databases are wiped",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Custom Circular Dial / Clock-face picker
                            CircularDialPicker(
                                currentSeconds = settings.panicWipeCountdownSeconds,
                                onSecondsChanged = { viewModel.updatePanicWipeCountdownSeconds(it) }
                            )

                            Spacer(modifier = Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(BorderSubtle.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Destructive Action Trigger Button
                            Button(
                                onClick = { showPanicConfirmDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Initiate Panic Purge Flow",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Confirmation Dialog
        if (showPanicConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showPanicConfirmDialog = false },
                title = {
                    Text(
                        text = "Trigger Emergency Panic Wipe?",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "This will initiate a ${settings.panicWipeCountdownSeconds}-second countdown to permanently destroy all local encrypted databases, keys, and session credentials.\n\nYou will have ${settings.panicWipeCountdownSeconds} seconds to cancel before destruction executes.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPanicConfirmDialog = false
                            onNavigateToPanicCountdown()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                    ) {
                        Text("Proceed to Countdown", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPanicConfirmDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = SurfaceDark,
                shape = RoundedCornerShape(14.dp)
            )
        }

        if (showPinSetupDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPinSetupDialog = false
                    pinSetupStep = 1
                    firstEnteredPin = ""
                    currentPinInput = ""
                    pinDialogError = null
                },
                title = {
                    Text(
                        text = if (pinSetupStep == 1) "Set Vault PIN" else "Confirm Vault PIN",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (pinSetupStep == 1) {
                                "Enter a 6-digit PIN to secure your vault"
                            } else {
                                "Re-enter your 6-digit PIN to confirm"
                            },
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        PinPadView(
                            currentPin = currentPinInput,
                            onPinChange = {
                                currentPinInput = it
                                pinDialogError = null
                            },
                            pinLength = 6,
                            isError = pinDialogError != null,
                            errorMessage = pinDialogError,
                            onPinComplete = { completedPin ->
                                if (pinSetupStep == 1) {
                                    firstEnteredPin = completedPin
                                    currentPinInput = ""
                                    pinSetupStep = 2
                                } else {
                                    if (completedPin == firstEnteredPin) {
                                        viewModel.setupPin(
                                            pin = completedPin,
                                            onSuccess = {
                                                showPinSetupDialog = false
                                                pinSetupStep = 1
                                                firstEnteredPin = ""
                                                currentPinInput = ""
                                            },
                                            onError = { err ->
                                                pinDialogError = err
                                                pinSetupStep = 1
                                                firstEnteredPin = ""
                                                currentPinInput = ""
                                            }
                                        )
                                    } else {
                                        pinDialogError = "PINs do not match. Please try again."
                                        pinSetupStep = 1
                                        firstEnteredPin = ""
                                        currentPinInput = ""
                                    }
                                }
                            }
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = {
                        showPinSetupDialog = false
                        pinSetupStep = 1
                        firstEnteredPin = ""
                        currentPinInput = ""
                        pinDialogError = null
                    }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = SurfaceDark,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun SecurityOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }

        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = ReefPink,
                unselectedColor = TextMuted
            )
        )
    }
}
