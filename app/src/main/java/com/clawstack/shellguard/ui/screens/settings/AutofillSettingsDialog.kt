package com.clawstack.shellguard.ui.screens.settings

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.clawstack.shellguard.services.autofill.AutofillManagerHelper
import com.clawstack.shellguard.ui.theme.BorderSubtle
import com.clawstack.shellguard.ui.theme.BrandClawCyan
import com.clawstack.shellguard.ui.theme.ReefPink
import com.clawstack.shellguard.ui.theme.StatusError
import com.clawstack.shellguard.ui.theme.StatusSuccess
import com.clawstack.shellguard.ui.theme.StatusWarning
import com.clawstack.shellguard.ui.theme.SurfaceContainerDark
import com.clawstack.shellguard.ui.theme.SurfaceDark
import com.clawstack.shellguard.ui.theme.TextMuted
import com.clawstack.shellguard.ui.theme.TextPrimary
import com.clawstack.shellguard.ui.theme.TextSecondary
import com.clawstack.shellguard.ui.theme.WarningBoxBg
import com.clawstack.shellguard.ui.theme.WarningText

/**
 * Cohesive Autofill System Guidance and Settings Dialog.
 *
 * Checks live Android Autofill provider status via AutofillManagerHelper.
 * If ShellGuard is inactive in Android Settings, displays guidance and launches
 * Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE to select ShellGuard.
 * Automatically refreshes status when returning from Android System Settings.
 */
@Composable
fun AutofillSettingsDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isSupported by remember { mutableStateOf(AutofillManagerHelper.isAutofillSupported(context)) }
    var isEnabled by remember { mutableStateOf(AutofillManagerHelper.isAutofillServiceEnabled(context)) }

    // Re-check status whenever the user returns to this screen from Android System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isSupported = AutofillManagerHelper.isAutofillSupported(context)
                isEnabled = AutofillManagerHelper.isAutofillServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerDark),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Autofill & System Integration",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Status Banner Card ──────────────────────────────────────
                val bannerBg = if (isEnabled) StatusSuccess.copy(alpha = 0.12f) else WarningBoxBg
                val bannerBorder = if (isEnabled) StatusSuccess.copy(alpha = 0.4f) else StatusWarning.copy(alpha = 0.4f)
                val statusText = if (isEnabled) "ShellGuard Autofill is Active" else "Autofill is Not Enabled in Android"
                val statusSubtext = if (isEnabled) {
                    "Android will automatically suggest credentials when you tap login fields."
                } else {
                    "To autofill logins in browsers and apps, select ShellGuard as your Autofill provider."
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bannerBg, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isEnabled) StatusSuccess else StatusWarning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = statusText,
                                color = if (isEnabled) StatusSuccess else WarningText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = statusSubtext,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Action Buttons ──────────────────────────────────────────
                if (!isEnabled) {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                try {
                                    val preferredIntent = AutofillManagerHelper.createOpenPreferredServiceSettingsIntent()
                                    context.startActivity(preferredIntent)
                                } catch (_: Exception) {
                                    val intent = AutofillManagerHelper.createSetAutofillServiceIntent(context)
                                        ?: AutofillManagerHelper.createOpenAutofillSettingsIntent()
                                    context.startActivity(intent)
                                }
                            } else {
                                val intent = AutofillManagerHelper.createSetAutofillServiceIntent(context)
                                if (intent != null) {
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val fallback = AutofillManagerHelper.createOpenAutofillSettingsIntent()
                                        context.startActivity(fallback)
                                    }
                                } else {
                                    Toast.makeText(context, "Autofill not supported on this Android version", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ReefPink)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                "Set as Preferred Service (Android 14+)"
                            } else {
                                "Enable in Android Settings"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Feature Highlights
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeatureRow(
                        title = "Keyboard Inline Chips",
                        desc = "Suggestions appear above your soft keyboard on Android 11+"
                    )
                    FeatureRow(
                        title = "Bitwarden TOTP Auto-Copy",
                        desc = "Verification codes copy to clipboard with 30s auto-scrub"
                    )
                    FeatureRow(
                        title = "Home Lab Port Isolation",
                        desc = "Disambiguates services on the same local IP across ports"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(6.dp)
                .background(BrandClawCyan, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, color = TextMuted, fontSize = 10.sp, lineHeight = 13.sp)
        }
    }
}
