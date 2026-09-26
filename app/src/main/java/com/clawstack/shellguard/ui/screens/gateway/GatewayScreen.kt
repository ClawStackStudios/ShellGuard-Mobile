package com.clawstack.shellguard.ui.screens.gateway

import android.content.ClipboardManager
import android.content.Context
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.clawstack.shellguard.ui.theme.BorderSubtle
import com.clawstack.shellguard.ui.theme.BrandClawCyan
import com.clawstack.shellguard.ui.theme.OceanDark
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

@Composable
fun GatewayScreen(
    viewModel: GatewayViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onLoginSuccess: (serverUrl: String, hashedKey: String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var isProtocolDropdownExpanded by remember { mutableStateOf(false) }
    var isHostFocused by remember { mutableStateOf(false) }
    var isPortFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isHostFocused, isPortFocused) {
        if (isHostFocused || isPortFocused) {
            scrollState.animateScrollTo(180)
        }
    }

    // JSON Identity File Picker
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val text = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                if (text != null) {
                    var fileName = "shellguard_identity.json"
                    context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                    viewModel.handleUploadedFile(fileName, text)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(OceanDark),
        color = OceanDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Navigation Bar ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(SurfaceDark, CircleShape)
                        .border(1.dp, BorderSubtle, CircleShape)
                        .clip(CircleShape)
                        .testTag("gateway_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Canonical ShellGuard Brand Emblem ───────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(ReefPink, BrandClawCyan)
                        )
                    )
                    .border(1.5.dp, ReefPink.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🐚", fontSize = 38.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Brand Title & Tagline ───────────────────────────────────────
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = ReefPink, fontWeight = FontWeight.Bold)) { append("Shell") }
                    withStyle(style = SpanStyle(color = BrandClawCyan, fontWeight = FontWeight.Bold)) { append("Guard") }
                    withStyle(style = SpanStyle(color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Normal)) { append(" ©™") }
                },
                fontSize = 22.sp,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Vault Gateway",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Login with your ShellKey©™ identity",
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Signature Segmented URL Bar (ClawStack Standard) ────────────
            val animatedPortWidth by animateDpAsState(
                targetValue = if (isPortFocused) 105.dp else 68.dp,
                label = "PortWidth"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(100f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceDark)
                            .border(
                                width = if (isHostFocused || isPortFocused || !uiState.isUrlValid) 1.5.dp else 1.dp,
                                color = if (!uiState.isUrlValid) StatusError else if (isPortFocused || isHostFocused) ReefPink else BorderSubtle,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Protocol Segment Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxHeight()
                                .background(if (!uiState.isUrlValid) StatusError else ReefPink)
                                .clickable { isProtocolDropdownExpanded = !isProtocolDropdownExpanded }
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = if (uiState.protocol == "https") "https://" else "http://",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isProtocolDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Select protocol",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Host Input Section
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (uiState.host.isEmpty()) {
                                    Text(
                                        text = "vault.example.com",
                                        color = TextMuted,
                                        fontSize = 14.sp
                                    )
                                }
                                BasicTextField(
                                    value = uiState.host,
                                    onValueChange = { viewModel.updateHost(it) },
                                    textStyle = TextStyle(
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(if (isHostFocused) ReefPink else TextPrimary),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Next,
                                        autoCorrectEnabled = false
                                    ),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isHostFocused = it.isFocused }
                                        .testTag("gateway_host_input")
                                )
                            }
                        }

                        // Vertical Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(if (!uiState.isUrlValid) StatusError else if (isPortFocused) ReefPink else BorderSubtle)
                        )

                        // Port Section (Animated Expand)
                        Box(
                            modifier = Modifier
                                .width(animatedPortWidth)
                                .height(56.dp)
                                .background(
                                    if (!uiState.isUrlValid) StatusError.copy(alpha = 0.12f)
                                    else if (isPortFocused) ReefPink.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (uiState.port.isEmpty()) {
                                        Text(
                                            text = "Port",
                                            color = if (!uiState.isUrlValid) StatusError
                                            else if (isPortFocused) ReefPink.copy(alpha = 0.6f)
                                            else TextMuted,
                                            fontSize = 14.sp
                                        )
                                    }
                                    BasicTextField(
                                        value = uiState.port,
                                        onValueChange = { viewModel.updatePort(it) },
                                        textStyle = TextStyle(
                                            color = if (!uiState.isUrlValid) StatusError else if (isPortFocused) ReefPink else TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        cursorBrush = SolidColor(if (isPortFocused) ReefPink else TextPrimary),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done,
                                            autoCorrectEnabled = false
                                        ),
                                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { isPortFocused = it.isFocused }
                                            .testTag("gateway_port_input")
                                    )
                                }
                            }
                        }
                    }

                    if (!uiState.isUrlValid && uiState.urlErrorMessage != null) {
                        Text(
                            text = uiState.urlErrorMessage ?: "",
                            color = StatusError,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }

                // Floating Protocol Dropdown Card
                androidx.compose.animation.AnimatedVisibility(
                    visible = isProtocolDropdownExpanded,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                    modifier = Modifier
                        .zIndex(110f)
                        .padding(start = 0.dp, top = 60.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerDark),
                        border = BorderStroke(1.dp, ReefPink.copy(alpha = 0.5f)),
                        modifier = Modifier.width(115.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateProtocol("http")
                                        isProtocolDropdownExpanded = false
                                    }
                                    .background(if (uiState.protocol == "http") ReefPink.copy(alpha = 0.15f) else Color.Transparent)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "http://",
                                    color = if (uiState.protocol == "http") ReefPink else TextPrimary,
                                    fontWeight = if (uiState.protocol == "http") FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                            HorizontalDivider(
                                color = BorderSubtle,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateProtocol("https")
                                        isProtocolDropdownExpanded = false
                                    }
                                    .background(if (uiState.protocol == "https") ReefPink.copy(alpha = 0.15f) else Color.Transparent)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "https://",
                                    color = if (uiState.protocol == "https") ReefPink else TextPrimary,
                                    fontWeight = if (uiState.protocol == "https") FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Dual Mode Toggles: Upload File vs Paste Key ─────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                        .background(if (uiState.inputMode == KeyInputMode.FILE) ReefPink else Color.Transparent)
                        .clickable { viewModel.setInputMode(KeyInputMode.FILE) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            tint = if (uiState.inputMode == KeyInputMode.FILE) TextPrimary else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Upload File",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (uiState.inputMode == KeyInputMode.FILE) TextPrimary else TextSecondary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                        .background(if (uiState.inputMode == KeyInputMode.PASTE) ReefPink else Color.Transparent)
                        .clickable { viewModel.setInputMode(KeyInputMode.PASTE) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = if (uiState.inputMode == KeyInputMode.PASTE) TextPrimary else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Paste ClawKey©™",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (uiState.inputMode == KeyInputMode.PASTE) TextPrimary else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Mode Views ──────────────────────────────────────────────────
            if (uiState.inputMode == KeyInputMode.FILE) {
                UploadFileView(
                    uploadedFileName = uiState.uploadedFileName,
                    onTap = { fileLauncher.launch("application/json") },
                    onClear = { viewModel.clearUploadedFile() }
                )
            } else {
                PasteKeyView(
                    keyText = uiState.clawKey,
                    isKeyValid = uiState.isKeyValid,
                    onKeyTextChanged = { viewModel.updateClawKey(it) },
                    onPasteFromClipboard = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val item = clipboard?.primaryClip?.getItemAt(0)
                        val text = item?.text?.toString() ?: ""
                        viewModel.pasteClawKey(text)
                    }
                )
            }

            // ── Error Banner ────────────────────────────────────────────────
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error",
                            tint = StatusError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusError
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Zero-Knowledge Architectural Invariant Card ─────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerDark),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Zero-Knowledge",
                            tint = BrandClawCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Zero-Knowledge Authentication",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your raw hu- ClawKey is never sent over the network. The client derives and transmits only a cryptographic SHA-256 digest to prove ownership.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Primary Action Button ───────────────────────────────────────
            Button(
                onClick = {
                    viewModel.connectToServer(onSuccess = onLoginSuccess)
                },
                enabled = !uiState.isLoading && uiState.isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ReefPink,
                    contentColor = TextPrimary,
                    disabledContainerColor = ReefPink.copy(alpha = 0.4f),
                    disabledContentColor = TextPrimary.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("gateway_login_button")
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = TextPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.inputMode == KeyInputMode.FILE) "Login with Identity File" else "Connect to ShellGuard Server",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Bottom Brand Link ───────────────────────────────────────────
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New to the reef? ",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Text(
                    text = "Molt a New Identity",
                    color = ReefPink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { /* Future: Navigate to Identity Setup */ }
                )
            }
        }
    }
}

@Composable
fun UploadFileView(
    uploadedFileName: String?,
    onTap: () -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .border(
                    width = 1.dp,
                    color = if (uploadedFileName != null) ReefPink else BorderSubtle,
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(if (uploadedFileName != null) ReefPink.copy(alpha = 0.08f) else SurfaceDark)
                .clickable { onTap() }
                .testTag("gateway_file_dropzone"),
            contentAlignment = Alignment.Center
        ) {
            if (uploadedFileName != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🐚 Identity Loaded Successfully!",
                        color = ReefPink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "File: $uploadedFileName",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to change file",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = "Upload",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap to upload your identity file",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ".json files only",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (uploadedFileName != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = BorderStroke(1.dp, BorderSubtle),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Remove File", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        WarningBox()
    }
}

@Composable
fun PasteKeyView(
    keyText: String,
    isKeyValid: Boolean,
    onKeyTextChanged: (String) -> Unit,
    onPasteFromClipboard: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = keyText,
            onValueChange = onKeyTextChanged,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gateway_key_input"),
            placeholder = { Text("Paste your hu- or lb- key here...", color = TextMuted) },
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false
            ),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide key" else "Show key",
                        tint = TextMuted
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ReefPink,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark
            )
        )

        // Valid ClawKey Format Badge
        if (isKeyValid) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StatusSuccess,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Valid 67-char ClawKey©™ format",
                    color = StatusSuccess,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Paste from Clipboard Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = onPasteFromClipboard,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ReefPink),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Paste Key", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        WarningBox()
    }
}

@Composable
fun WarningBox() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WarningBoxBg),
        border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Warning",
                tint = WarningText,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Can't find your identity file?",
                    color = WarningText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Your identity file or hu- key is the only way to access your vault. If lost, vault secrets cannot be recovered.",
                    color = WarningText.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}



