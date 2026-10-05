package com.clawstack.shellguard.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.data.backup.BackupFormatType
import com.clawstack.shellguard.data.backup.BackupProtectionMode
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
fun SettingsBackupScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Export State
    var selectedProtectionMode by remember { mutableStateOf(BackupProtectionMode.ACTIVE_KEY) }
    var customPassphrase by remember { mutableStateOf("") }
    var confirmPassphrase by remember { mutableStateOf("") }
    var showPassphrase by remember { mutableStateOf(false) }

    var activeClawKey by remember { mutableStateOf("") }
    var deviceOnlyKeyFallback by remember { mutableStateOf(false) }
    var showClawKey by remember { mutableStateOf(false) }

    // Import State
    var importInputText by remember { mutableStateOf("") }
    var importPasswordOrKey by remember { mutableStateOf("") }
    var detectedFormat by remember { mutableStateOf(BackupFormatType.UNKNOWN) }

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
                        contentDescription = "Back to Settings",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Backup & Portability",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sovereign exports & Bitwarden migration",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Status Messages ─────────────────────────────────────────
                if (uiState.infoMessage != null || uiState.errorMessage != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.errorMessage != null) StatusError.copy(alpha = 0.15f)
                                else BrandClawCyan.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (uiState.errorMessage != null) StatusError else BrandClawCyan
                            )
                        ) {
                            Text(
                                text = uiState.errorMessage ?: uiState.infoMessage.orEmpty(),
                                color = if (uiState.errorMessage != null) StatusError else BrandClawCyan,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }

                // ── Section 1: Export Vault ─────────────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = ReefPink,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Export Sovereign Backup",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "Create a client-sealed JSON backup envelope compatible with ShellGuard Web and mobile companions.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                            )

                            Text(
                                text = "PROTECTION MODE",
                                color = ReefPink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            // Option 1: Active Sovereign Key
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedProtectionMode = BackupProtectionMode.ACTIVE_KEY }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedProtectionMode == BackupProtectionMode.ACTIVE_KEY,
                                    onClick = { selectedProtectionMode = BackupProtectionMode.ACTIVE_KEY },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = ReefPink,
                                        unselectedColor = TextMuted
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Active Sovereign Key (hu-...)",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Sealed via HKDF-SHA256 with active master key. No extra password needed.",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Active Key Inputs if ACTIVE_KEY
                            AnimatedVisibility(visible = selectedProtectionMode == BackupProtectionMode.ACTIVE_KEY) {
                                Column(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)) {
                                    val isKeyValid = activeClawKey.trim().matches(Regex("^hu-[0-9a-zA-Z]{64}$"))
                                    
                                    OutlinedTextField(
                                        value = activeClawKey,
                                        onValueChange = { activeClawKey = it },
                                        label = { Text("Sovereign ClawKey") },
                                        enabled = !deviceOnlyKeyFallback,
                                        visualTransformation = if (showClawKey || deviceOnlyKeyFallback) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isKeyValid && !deviceOnlyKeyFallback) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Valid",
                                                        tint = BrandClawCyan,
                                                        modifier = Modifier.padding(end = 4.dp).size(20.dp)
                                                    )
                                                }
                                                IconButton(onClick = { showClawKey = !showClawKey }, enabled = !deviceOnlyKeyFallback) {
                                                    Icon(
                                                        imageVector = if (showClawKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                        contentDescription = null,
                                                        tint = TextMuted
                                                    )
                                                }
                                            }
                                        },
                                        supportingText = {
                                            if (!deviceOnlyKeyFallback) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = if (isKeyValid) "Key is valid." else "Enter your 67-character Sovereign ClawKey.",
                                                        color = if (isKeyValid) BrandClawCyan else TextMuted
                                                    )
                                                    Text(
                                                        text = "${activeClawKey.length} / 67",
                                                        color = if (activeClawKey.length == 67) BrandClawCyan else TextMuted
                                                    )
                                                }
                                            } else {
                                                Text("Using local device session key.", color = TextMuted)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isKeyValid) BrandClawCyan else ReefPink,
                                            unfocusedBorderColor = if (isKeyValid) BrandClawCyan else BorderSubtle,
                                            focusedLabelColor = ReefPink,
                                            unfocusedLabelColor = TextMuted,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            disabledBorderColor = BorderSubtle,
                                            disabledTextColor = TextMuted,
                                            disabledLabelColor = TextMuted
                                        )
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { deviceOnlyKeyFallback = !deviceOnlyKeyFallback }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = deviceOnlyKeyFallback,
                                            onCheckedChange = { deviceOnlyKeyFallback = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = ReefPink,
                                                uncheckedColor = TextMuted
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Use local device session key",
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Device-only export — cannot be decrypted on Web without session.",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Option 2: Custom Passphrase
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedProtectionMode = BackupProtectionMode.CUSTOM_PASSPHRASE }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedProtectionMode == BackupProtectionMode.CUSTOM_PASSPHRASE,
                                    onClick = { selectedProtectionMode = BackupProtectionMode.CUSTOM_PASSPHRASE },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = ReefPink,
                                        unselectedColor = TextMuted
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Custom Passphrase",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "PBKDF2-SHA256 (600,000 iterations). 100% parity with ShellGuard Web export.",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Option 3: Plaintext
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedProtectionMode = BackupProtectionMode.PLAINTEXT }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedProtectionMode == BackupProtectionMode.PLAINTEXT,
                                    onClick = { selectedProtectionMode = BackupProtectionMode.PLAINTEXT },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = ReefPink,
                                        unselectedColor = TextMuted
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Plaintext JSON (Unencrypted)",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Raw unencrypted credentials. Store only on air-gapped secure hardware.",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Passphrase Inputs if Custom Passphrase
                            AnimatedVisibility(visible = selectedProtectionMode == BackupProtectionMode.CUSTOM_PASSPHRASE) {
                                Column(modifier = Modifier.padding(top = 10.dp)) {
                                    OutlinedTextField(
                                        value = customPassphrase,
                                        onValueChange = { customPassphrase = it },
                                        label = { Text("Export Passphrase") },
                                        visualTransformation = if (showPassphrase) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { showPassphrase = !showPassphrase }) {
                                                Icon(
                                                    imageVector = if (showPassphrase) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = null,
                                                    tint = TextMuted
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ReefPink,
                                            unfocusedBorderColor = BorderSubtle,
                                            focusedLabelColor = ReefPink,
                                            unfocusedLabelColor = TextMuted,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = confirmPassphrase,
                                        onValueChange = { confirmPassphrase = it },
                                        label = { Text("Confirm Passphrase") },
                                        visualTransformation = if (showPassphrase) VisualTransformation.None else PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ReefPink,
                                            unfocusedBorderColor = BorderSubtle,
                                            focusedLabelColor = ReefPink,
                                            unfocusedLabelColor = TextMuted,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                }
                            }

                            // Plaintext Amber Warning
                            AnimatedVisibility(visible = selectedProtectionMode == BackupProtectionMode.PLAINTEXT) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    colors = CardDefaults.cardColors(containerColor = WarningText.copy(alpha = 0.12f)),
                                    border = BorderStroke(1.dp, WarningText.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = WarningText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Warning: Plaintext exports contain all your passwords in unencrypted cleartext. Do not send via email or unencrypted chat.",
                                            color = WarningText,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Export Button
                            Button(
                                onClick = {
                                    if (selectedProtectionMode == BackupProtectionMode.ACTIVE_KEY && !deviceOnlyKeyFallback) {
                                        val isKeyValid = activeClawKey.trim().matches(Regex("^hu-[0-9a-zA-Z]{64}$"))
                                        if (!isKeyValid) {
                                            Toast.makeText(context, "Please enter a valid 67-character Sovereign ClawKey.", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                    }
                                    if (selectedProtectionMode == BackupProtectionMode.CUSTOM_PASSPHRASE) {
                                        if (customPassphrase.isBlank()) {
                                            Toast.makeText(context, "Passphrase cannot be empty.", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (customPassphrase != confirmPassphrase) {
                                            Toast.makeText(context, "Passphrases do not match.", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                    }
                                    viewModel.exportVaultBackup(
                                        protectionMode = selectedProtectionMode,
                                        customPassphrase = customPassphrase.ifBlank { null },
                                        activeClawKey = if (selectedProtectionMode == BackupProtectionMode.ACTIVE_KEY && !deviceOnlyKeyFallback) activeClawKey.trim() else null
                                    )
                                },
                                enabled = !uiState.isExporting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ReefPink),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (uiState.isExporting) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Encrypting & Sealing...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Generate & Export Backup",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Export Result Card
                            val lastExport = uiState.lastExportResult
                            if (lastExport != null) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    colors = CardDefaults.cardColors(containerColor = BrandClawCyan.copy(alpha = 0.10f)),
                                    border = BorderStroke(1.dp, BrandClawCyan.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = BrandClawCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Backup Ready (${lastExport.pearlsCount} Logins · ${lastExport.notesCount} Notes · ${lastExport.sshKeysCount} SSH Keys)",
                                                color = BrandClawCyan,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = "SHA-256: ${lastExport.checksumSha256.take(16)}...",
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("ShellGuard Backup", lastExport.jsonString))
                                                    Toast.makeText(context, "Backup copied to clipboard", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, BrandClawCyan),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = null,
                                                    tint = BrandClawCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Copy JSON", color = BrandClawCyan, fontSize = 12.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    val sendIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, lastExport.jsonString)
                                                        putExtra(Intent.EXTRA_TITLE, "shellguard-vault-backup.json")
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(Intent.createChooser(sendIntent, "Share ShellGuard Backup"))
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = BrandClawCyan),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = null,
                                                    tint = OceanDark,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Share File", color = OceanDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Section 2: Restore / Ingest Vault ───────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = BrandClawCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Restore or Import Vault",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "Restore an encrypted ShellGuard backup (.sgvault, .json) or import an unencrypted Bitwarden JSON export.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                            )

                            OutlinedTextField(
                                value = importInputText,
                                onValueChange = {
                                    importInputText = it
                                    detectedFormat = viewModel.detectBackupFormat(it)
                                },
                                label = { Text("Paste Backup JSON here") },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandClawCyan,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedLabelColor = BrandClawCyan,
                                    unfocusedLabelColor = TextMuted,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            // Detected Format Badge
                            if (detectedFormat != BackupFormatType.UNKNOWN) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "FORMAT: ",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val (badgeText, badgeColor) = when (detectedFormat) {
                                        BackupFormatType.SHELLGUARD_ENCRYPTED -> "ShellGuard Encrypted (v1)" to ReefPink
                                        BackupFormatType.SHELLGUARD_PLAIN -> "ShellGuard Plain JSON" to BrandClawCyan
                                        BackupFormatType.BITWARDEN_JSON -> "Bitwarden Unencrypted JSON" to BrandClawCyan
                                        BackupFormatType.BITWARDEN_ENCRYPTED -> "Bitwarden Encrypted (Unsupported)" to StatusError
                                        BackupFormatType.UNKNOWN -> "Unknown" to TextMuted
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(badgeColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(text = badgeText, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Passphrase input if encrypted
                            AnimatedVisibility(visible = detectedFormat == BackupFormatType.SHELLGUARD_ENCRYPTED) {
                                Column(modifier = Modifier.padding(top = 6.dp)) {
                                    OutlinedTextField(
                                        value = importPasswordOrKey,
                                        onValueChange = { importPasswordOrKey = it },
                                        label = { Text("Decryption Passphrase or Master Key") },
                                        placeholder = { Text("Leave blank to try active session key") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BrandClawCyan,
                                            unfocusedBorderColor = BorderSubtle,
                                            focusedLabelColor = BrandClawCyan,
                                            unfocusedLabelColor = TextMuted,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    if (importInputText.isBlank()) {
                                        Toast.makeText(context, "Please paste backup content first.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.importVaultBackup(
                                        rawContent = importInputText,
                                        passwordOrKey = importPasswordOrKey.ifBlank { null }
                                    )
                                },
                                enabled = !uiState.isImporting && importInputText.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandClawCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (uiState.isImporting) {
                                    CircularProgressIndicator(
                                        color = OceanDark,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Decrypting & Ingesting...", color = OceanDark)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.FileUpload,
                                        contentDescription = null,
                                        tint = OceanDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Import Items to Vault",
                                        color = OceanDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
