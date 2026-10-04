package com.clawstack.shellguard.ui.screens.detail

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.data.repository.VaultItemDomain
import com.clawstack.shellguard.domain.models.CustomFieldType
import com.clawstack.shellguard.ui.components.ClipboardToastPill
import com.clawstack.shellguard.ui.components.CustomFieldDisplayRow
import com.clawstack.shellguard.ui.components.TotpDisplayCard
import com.clawstack.shellguard.ui.theme.BorderSubtle
import com.clawstack.shellguard.ui.theme.BrandClawCyan
import com.clawstack.shellguard.ui.theme.LocalShellGuardColors
import com.clawstack.shellguard.ui.theme.OceanDark
import com.clawstack.shellguard.ui.theme.ReefPink
import com.clawstack.shellguard.ui.theme.StatusError
import com.clawstack.shellguard.ui.theme.SurfaceContainerDark
import com.clawstack.shellguard.ui.theme.SurfaceDark
import com.clawstack.shellguard.ui.theme.TextMuted
import com.clawstack.shellguard.ui.theme.TextPrimary
import com.clawstack.shellguard.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ItemDetailScreen(
    viewModel: ItemDetailViewModel,
    onBackClick: () -> Unit,
    onEditClick: (domain: String, id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalShellGuardColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRepromptDialog by remember { mutableStateOf(false) }
    var pendingSensitiveAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var isRepromptPassed by remember { mutableStateOf(false) }
    var isHistoryExpanded by remember { mutableStateOf(false) }

    // Clipboard auto-clear state machine
    var clipboardToastVisible by remember { mutableStateOf(false) }
    var clipboardMessage by remember { mutableStateOf("") }
    var clipboardRemainingSeconds by remember { mutableIntStateOf(30) }
    var clipboardProgress by remember { mutableFloatStateOf(1f) }
    var clipboardJob by remember { mutableStateOf<Job?>(null) }

    fun copyToClipboard(label: String, text: String, isSensitive: Boolean) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text).apply {
            if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
        }
        clipboard.setPrimaryClip(clip)

        // Android 13+ already shows a system overlay for non-sensitive or sensitive clips
        clipboardMessage = "$label copied to clipboard"
        clipboardToastVisible = true
        clipboardRemainingSeconds = 30
        clipboardProgress = 1f

        clipboardJob?.cancel()
        clipboardJob = coroutineScope.launch {
            val totalSeconds = 30
            for (sec in totalSeconds downTo 1) {
                clipboardRemainingSeconds = sec
                clipboardProgress = sec.toFloat() / totalSeconds.toFloat()
                delay(1000L)
            }
            clipboardToastVisible = false
            // Clear clipboard if still matches
            try {
                if (clipboard.primaryClip?.getItemAt(0)?.text == text) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        clipboard.clearPrimaryClip()
                    } else {
                        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                    }
                }
            } catch (_: Exception) {}
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(OceanDark),
        color = OceanDark
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is ItemDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ReefPink)
                    }
                }
                is ItemDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Error Loading Item", color = StatusError, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = state.message, color = TextMuted, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = onBackClick) {
                                Text("Back", color = TextPrimary)
                            }
                            Button(
                                onClick = { viewModel.loadItem() },
                                colors = ButtonDefaults.buttonColors(containerColor = ReefPink)
                            ) {
                                Text("Retry", color = TextPrimary)
                            }
                        }
                    }
                }
                is ItemDetailUiState.Deleted -> {
                    LaunchedEffect(Unit) {
                        onBackClick()
                    }
                }
                is ItemDetailUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        // ── Pinned Top Navigation Bar ─────────────────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary
                                )
                            }

                            Text(
                                text = state.title,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Edit Button (Bitwarden Offline Guard)
                                IconButton(
                                    onClick = {
                                        if (state.isOffline) {
                                            Toast.makeText(context, "Editing is disabled while offline.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            onEditClick(state.domain.name, state.id)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Item",
                                        tint = if (state.isOffline) TextMuted.copy(alpha = 0.4f) else BrandClawCyan
                                    )
                                }

                                // Delete Button (Bitwarden Offline Guard)
                                IconButton(
                                    onClick = {
                                        if (state.isOffline) {
                                            Toast.makeText(context, "Deletion is disabled while offline.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showDeleteDialog = true
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Item",
                                        tint = if (state.isOffline) TextMuted.copy(alpha = 0.4f) else StatusError
                                    )
                                }
                            }
                        }

                        // ── Scrollable Body ──────────────────────────────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            // Domain Badge + Category Header Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val (domainLabel, domainBg, domainColor) = when (state.domain) {
                                    VaultItemDomain.PASSWORD -> Triple("Password", ReefPink.copy(alpha = 0.15f), ReefPink)
                                    VaultItemDomain.NOTE -> Triple("Secure Note", BrandClawCyan.copy(alpha = 0.15f), BrandClawCyan)
                                    VaultItemDomain.SSH_KEY -> Triple("SSH Key", Color(0xFF9D4EDD).copy(alpha = 0.15f), Color(0xFFC77DFF))
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(domainBg)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = domainLabel.uppercase(),
                                        color = domainColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                if (!state.category.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SurfaceContainerDark)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = state.category,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                if (state.reprompt) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ReefPink.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Security,
                                                contentDescription = null,
                                                tint = ReefPink,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "RE-PROMPT",
                                                color = ReefPink,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── Primary Domain Content Card ───────────────────────────
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    when (state.domain) {
                                        VaultItemDomain.PASSWORD -> {
                                            // Password Row
                                            DetailSecretRow(
                                                label = "Password",
                                                secret = state.secret,
                                                isRevealed = state.isSecretRevealed,
                                                onToggleReveal = {
                                                    if (state.reprompt && !isRepromptPassed && !state.isSecretRevealed) {
                                                        pendingSensitiveAction = { viewModel.toggleSecretVisibility() }
                                                        showRepromptDialog = true
                                                    } else {
                                                        viewModel.toggleSecretVisibility()
                                                    }
                                                },
                                                onCopy = {
                                                    if (state.reprompt && !isRepromptPassed) {
                                                        pendingSensitiveAction = { copyToClipboard("Password", state.secret, isSensitive = true) }
                                                        showRepromptDialog = true
                                                    } else {
                                                        copyToClipboard("Password", state.secret, isSensitive = true)
                                                    }
                                                }
                                            )

                                            if (!state.username.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(14.dp))
                                                DetailTextRow(
                                                    label = "Username",
                                                    value = state.username,
                                                    onCopy = { copyToClipboard("Username", state.username, isSensitive = false) }
                                                )
                                            }

                                            if (!state.url.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(14.dp))
                                                DetailUrlRow(
                                                    label = "Website URL",
                                                    url = state.url,
                                                    onOpenUrl = {
                                                        try {
                                                            val uri = if (!state.url.startsWith("http://") && !state.url.startsWith("https://")) {
                                                                Uri.parse("https://${state.url}")
                                                            } else {
                                                                Uri.parse(state.url)
                                                            }
                                                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Could not open URL", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    onCopy = { copyToClipboard("URL", state.url, isSensitive = false) }
                                                )
                                            }
                                        }

                                        VaultItemDomain.NOTE -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "NOTE CONTENT",
                                                    color = TextMuted,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.5.sp
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(
                                                        onClick = {
                                                            if (state.reprompt && !isRepromptPassed && !state.isSecretRevealed) {
                                                                pendingSensitiveAction = { viewModel.toggleSecretVisibility() }
                                                                showRepromptDialog = true
                                                            } else {
                                                                viewModel.toggleSecretVisibility()
                                                            }
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (state.isSecretRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                            contentDescription = if (state.isSecretRevealed) "Mask Note" else "Reveal Note",
                                                            tint = if (state.isSecretRevealed) ReefPink else TextMuted,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            if (state.reprompt && !isRepromptPassed) {
                                                                pendingSensitiveAction = { copyToClipboard("Note", state.secret, isSensitive = true) }
                                                                showRepromptDialog = true
                                                            } else {
                                                                copyToClipboard("Note", state.secret, isSensitive = true)
                                                            }
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ContentCopy,
                                                            contentDescription = "Copy Note",
                                                            tint = TextMuted,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(SurfaceContainerDark)
                                                    .clickable {
                                                        if (!state.isSecretRevealed) {
                                                            if (state.reprompt && !isRepromptPassed) {
                                                                pendingSensitiveAction = { viewModel.toggleSecretVisibility() }
                                                                showRepromptDialog = true
                                                            } else {
                                                                viewModel.toggleSecretVisibility()
                                                            }
                                                        }
                                                    }
                                                    .padding(12.dp)
                                            ) {
                                                if (state.isSecretRevealed) {
                                                    Text(
                                                        text = state.secret.ifBlank { "(Empty Note)" },
                                                        color = if (state.secret.isBlank()) TextMuted else TextPrimary,
                                                        fontSize = 14.sp,
                                                        lineHeight = 20.sp
                                                    )
                                                } else {
                                                    Column {
                                                        Text(
                                                            text = "••••••••••••••••••••••••••••••••\n••••••••••••••••••••••••\n••••••••••••••••••••••••••••••••",
                                                            color = TextMuted,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 14.sp,
                                                            lineHeight = 20.sp,
                                                            letterSpacing = 2.sp
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(
                                                            text = "Tap or click eye to reveal",
                                                            color = TextMuted.copy(alpha = 0.7f),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        VaultItemDomain.SSH_KEY -> {
                                            if (!state.username.isNullOrBlank()) {
                                                DetailTextRow(
                                                    label = "User / Key Name",
                                                    value = state.username,
                                                    onCopy = { copyToClipboard("Key Name", state.username, isSensitive = false) }
                                                )
                                                Spacer(modifier = Modifier.height(14.dp))
                                            }

                                            DetailSecretRow(
                                                label = "Private Key",
                                                secret = state.secret,
                                                isRevealed = state.isSecretRevealed,
                                                isMonospace = true,
                                                onToggleReveal = {
                                                    if (state.reprompt && !isRepromptPassed && !state.isSecretRevealed) {
                                                        pendingSensitiveAction = { viewModel.toggleSecretVisibility() }
                                                        showRepromptDialog = true
                                                    } else {
                                                        viewModel.toggleSecretVisibility()
                                                    }
                                                },
                                                onCopy = {
                                                    if (state.reprompt && !isRepromptPassed) {
                                                        pendingSensitiveAction = { copyToClipboard("Private Key", state.secret, isSensitive = true) }
                                                        showRepromptDialog = true
                                                    } else {
                                                        copyToClipboard("Private Key", state.secret, isSensitive = true)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // ── TOTP Verification Code Section ───────────────────────
                            if (!state.totpSecret.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                TotpDisplayCard(secret = state.totpSecret)
                            }

                            // ── Custom Fields Section ─────────────────────────────────
                            if (state.customFields.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "CUSTOM FIELDS",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.customFields.forEach { field ->
                                        CustomFieldDisplayRow(
                                            label = field.label,
                                            value = field.value,
                                            type = field.type,
                                            onCopy = {
                                                val isSensitive = field.type == CustomFieldType.HIDDEN
                                                copyToClipboard(field.label, field.value, isSensitive = isSensitive)
                                            }
                                        )
                                    }
                                }
                            }

                            // ── Tags Section ──────────────────────────────────────────
                            if (state.tags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "TAGS",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    state.tags.forEach { tag ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(SurfaceDark)
                                                .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                color = BrandClawCyan,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            // ── Notes Section ─────────────────────────────────────────
                            if (!state.notes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "NOTES",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    IconButton(
                                        onClick = { copyToClipboard("Notes", state.notes, isSensitive = false) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Notes",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = state.notes,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            // ── Password History Section ──────────────────────────────
                            if (state.passwordHistory.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { isHistoryExpanded = !isHistoryExpanded },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.History,
                                                    contentDescription = null,
                                                    tint = ReefPink,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Password History (${state.passwordHistory.size})",
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Icon(
                                                imageVector = if (isHistoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        AnimatedVisibility(visible = isHistoryExpanded) {
                                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                                val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
                                                state.passwordHistory.forEach { entry ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(
                                                                text = entry.password,
                                                                color = TextSecondary,
                                                                fontFamily = FontFamily.Monospace,
                                                                fontSize = 12.sp
                                                            )
                                                            Text(
                                                                text = sdf.format(Date(entry.timestamp)),
                                                                color = TextMuted,
                                                                fontSize = 10.sp
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = { copyToClipboard("Past Password", entry.password, isSensitive = true) },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.ContentCopy,
                                                                contentDescription = "Copy past password",
                                                                tint = TextMuted,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }

            // ── Sensitive Clipboard Toast Pill ────────────────────────────────
            ClipboardToastPill(
                visible = clipboardToastVisible,
                message = clipboardMessage,
                autoClearProgress = clipboardProgress,
                remainingSeconds = clipboardRemainingSeconds,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // ── Delete Confirmation Dialog ────────────────────────────────────
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Vault Item?", color = TextPrimary, fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            "This action will permanently delete this item from your vault and remote server.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteDialog = false
                                viewModel.deleteItem {
                                    Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                        ) {
                            Text("Delete", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    },
                    containerColor = SurfaceDark,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // ── Claw Re-Prompt Dialog ─────────────────────────────────────────
            if (showRepromptDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showRepromptDialog = false
                        pendingSensitiveAction = null
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = ReefPink, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Claw Re-Prompt Gate", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Text(
                            "This item requires confirmation before exposing or copying its secret.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                isRepromptPassed = true
                                showRepromptDialog = false
                                pendingSensitiveAction?.invoke()
                                pendingSensitiveAction = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ReefPink)
                        ) {
                            Text("Confirm & Access", color = TextPrimary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showRepromptDialog = false
                            pendingSensitiveAction = null
                        }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    },
                    containerColor = SurfaceDark,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    }
}

@Composable
fun DetailTextRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Column {
        Text(
            text = label.uppercase(),
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun DetailUrlRow(
    label: String,
    url: String,
    onOpenUrl: () -> Unit,
    onCopy: () -> Unit
) {
    Column {
        Text(
            text = label.uppercase(),
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = url,
                color = BrandClawCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenUrl() }
            )
            Row {
                IconButton(onClick = onOpenUrl, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open Link",
                        tint = BrandClawCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy URL",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailSecretRow(
    label: String,
    secret: String,
    isRevealed: Boolean,
    isMonospace: Boolean = false,
    onToggleReveal: () -> Unit,
    onCopy: () -> Unit
) {
    Column {
        Text(
            text = label.uppercase(),
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isRevealed) secret else "••••••••••••••••••••",
                color = TextPrimary,
                fontFamily = if (isRevealed && isMonospace) FontFamily.Monospace else FontFamily.Default,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = if (isRevealed) 6 else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row {
                IconButton(onClick = onToggleReveal, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Visibility",
                        tint = if (isRevealed) ReefPink else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy $label",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
