package com.clawstack.shellguard.ui.components

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.engine.TotpEngine
import com.clawstack.shellguard.engine.TotpTicker
import com.clawstack.shellguard.ui.theme.*

@Composable
fun TotpDisplayCard(
    secret: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tick by TotpTicker.createTicker(30L).collectAsState(
        initial = com.clawstack.shellguard.engine.TotpTick(30, 1.0f)
    )

    val isSteam = remember(secret) {
        secret.startsWith("steam:", ignoreCase = true)
    }

    val cleanSecret = remember(secret) {
        secret.removePrefix("steam:").removePrefix("STEAM:")
    }

    val totpCode = remember(cleanSecret, tick.remainingSeconds) {
        if (isSteam) {
            TotpEngine.generateSteamGuard(cleanSecret)
        } else {
            TotpEngine.generateTotp(cleanSecret)
        }
    }

    val formattedCode = remember(totpCode) {
        if (totpCode.length == 6) {
            "${totpCode.substring(0, 3)} ${totpCode.substring(3)}"
        } else if (totpCode.length == 8) {
            "${totpCode.substring(0, 4)} ${totpCode.substring(4)}"
        } else {
            totpCode
        }
    }

    var copiedToastVisible by remember { mutableStateOf(false) }

    LaunchedEffect(copiedToastVisible) {
        if (copiedToastVisible) {
            kotlinx.coroutines.delay(3000L)
            copiedToastVisible = false
        }
    }

    fun copyCode() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("TOTP Verification Code", totpCode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboard.setPrimaryClip(clip)
        copiedToastVisible = true
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { copyCode() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = BrandClawCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSteam) "STEAM GUARD CODE" else "VERIFICATION CODE (TOTP)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = formattedCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 2.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TotpCountdownRing(
                        progress = tick.progress,
                        remainingSeconds = tick.remainingSeconds,
                        size = 38.dp,
                        strokeWidth = 3.dp
                    )

                    IconButton(
                        onClick = { copyCode() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(OceanDark)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy TOTP Code",
                            tint = BrandClawCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (copiedToastVisible) {
            Spacer(modifier = Modifier.height(8.dp))
            ClipboardToastPill(
                visible = copiedToastVisible,
                message = "TOTP code copied to clipboard",
                autoClearProgress = 1f,
                remainingSeconds = 30,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
