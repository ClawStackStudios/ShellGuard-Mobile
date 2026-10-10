package com.clawstack.shellguard.ui.screens.form

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.clawstack.shellguard.ui.components.PasswordGeneratorSheet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.data.repository.VaultItemDomain
import com.clawstack.shellguard.domain.models.CustomFieldType
import com.clawstack.shellguard.ui.theme.BorderSubtle
import com.clawstack.shellguard.ui.theme.BrandClawCyan
import com.clawstack.shellguard.ui.theme.OceanDark
import com.clawstack.shellguard.ui.theme.ReefPink
import com.clawstack.shellguard.ui.theme.StatusError
import com.clawstack.shellguard.ui.theme.SurfaceContainerDark
import com.clawstack.shellguard.ui.theme.SurfaceDark
import com.clawstack.shellguard.ui.theme.TextMuted
import com.clawstack.shellguard.ui.theme.TextPrimary
import com.clawstack.shellguard.ui.theme.TextSecondary

@Composable
fun ItemFormScreen(
    viewModel: ItemFormViewModel,
    onCancel: () -> Unit,
    onSaveSuccess: (domain: String, id: String) -> Unit,
    onScanQrClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAddFieldDialog by remember { mutableStateOf(false) }
    var showPasswordGeneratorSheet by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }

    val attachmentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            viewModel.stageAttachment(it, context)
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ReefPink,
        unfocusedBorderColor = BorderSubtle,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = SurfaceDark,
        unfocusedContainerColor = SurfaceDark,
        cursorColor = ReefPink
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(OceanDark),
        color = OceanDark
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding()
            ) {
                // ── Pinned Top Action Bar ─────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = TextMuted
                        )
                    }

                    Text(
                        text = if (uiState.mode == FormMode.NEW) "New Vault Item" else "Edit Item",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )

                    Button(
                        onClick = {
                            viewModel.save { domain, id ->
                                Toast.makeText(context, "Item saved successfully", Toast.LENGTH_SHORT).show()
                                onSaveSuccess(domain, id)
                            }
                        },
                        enabled = uiState.title.isNotBlank() && !uiState.isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ReefPink,
                            disabledContainerColor = ReefPink.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // ── Error Banner ──────────────────────────────────────────────
                if (uiState.errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StatusError.copy(alpha = 0.15f))
                            .border(1.dp, StatusError.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(text = uiState.errorMessage ?: "", color = StatusError, fontSize = 12.sp)
                    }
                }

                // ── Scrollable Form Fields ────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Domain Segmented Control (Create Mode Only)
                    if (uiState.mode == FormMode.NEW) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceDark)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DomainSegment(
                                label = "Password",
                                icon = Icons.Default.Key,
                                isSelected = uiState.domain == VaultItemDomain.PASSWORD,
                                onClick = { viewModel.setDomain(VaultItemDomain.PASSWORD) },
                                modifier = Modifier.weight(1f)
                            )
                            DomainSegment(
                                label = "Note",
                                icon = Icons.Default.Description,
                                isSelected = uiState.domain == VaultItemDomain.NOTE,
                                onClick = { viewModel.setDomain(VaultItemDomain.NOTE) },
                                modifier = Modifier.weight(1f)
                            )
                            DomainSegment(
                                label = "SSH Key",
                                icon = Icons.Default.Terminal,
                                isSelected = uiState.domain == VaultItemDomain.SSH_KEY,
                                onClick = { viewModel.setDomain(VaultItemDomain.SSH_KEY) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Title Field (Mandatory)
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        label = { Text("Title *", fontSize = 12.sp) },
                        placeholder = { Text("e.g. GitHub Corporate, Production DB", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Field (Optional Pod)
                    OutlinedTextField(
                        value = uiState.category,
                        onValueChange = { viewModel.updateCategory(it) },
                        label = { Text("Pod / Category", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Work, Personal, Infrastructure", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Domain-Specific Inputs
                    when (uiState.domain) {
                        VaultItemDomain.PASSWORD -> {
                            // Username
                            OutlinedTextField(
                                value = uiState.username,
                                onValueChange = { viewModel.updateUsername(it) },
                                label = { Text("Username / Email", fontSize = 12.sp) },
                                placeholder = { Text("octocat@github.com", color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                colors = fieldColors
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Website URL
                            OutlinedTextField(
                                value = uiState.url,
                                onValueChange = { viewModel.updateUrl(it) },
                                label = { Text("Website URL", fontSize = 12.sp) },
                                placeholder = { Text("https://github.com", color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                colors = fieldColors
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Password
                            OutlinedTextField(
                                value = uiState.secret,
                                onValueChange = { viewModel.updateSecret(it) },
                                label = { Text("Password", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                visualTransformation = if (uiState.isSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    autoCorrectEnabled = false
                                ),
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { showPasswordGeneratorSheet = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Casino,
                                                contentDescription = "Generate Password",
                                                tint = BrandClawCyan
                                            )
                                        }
                                        IconButton(onClick = { viewModel.toggleSecretVisibility() }) {
                                            Icon(
                                                imageVector = if (uiState.isSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle password visibility",
                                                tint = if (uiState.isSecretVisible) ReefPink else TextMuted
                                            )
                                        }
                                    }
                                },
                                colors = fieldColors
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Authenticator Key (TOTP)
                            OutlinedTextField(
                                value = uiState.totpSecret,
                                onValueChange = { viewModel.updateTotpSecret(it) },
                                label = { Text("Authenticator Key (TOTP)", fontSize = 12.sp) },
                                placeholder = { Text("Base32 secret or otpauth:// URI", color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = onScanQrClick) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Scan QR Code",
                                            tint = BrandClawCyan
                                        )
                                    }
                                },
                                colors = fieldColors
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Notes
                            OutlinedTextField(
                                value = uiState.notes,
                                onValueChange = { viewModel.updateNotes(it) },
                                label = { Text("Notes", fontSize = 12.sp) },
                                placeholder = { Text("Additional notes...", color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                minLines = 3,
                                colors = fieldColors
                            )
                        }

                        VaultItemDomain.NOTE -> {
                            // Note Content
                            OutlinedTextField(
                                value = uiState.secret,
                                onValueChange = { viewModel.updateSecret(it) },
                                label = { Text("Note Content", fontSize = 12.sp) },
                                placeholder = { Text("Write encrypted secure note content here...", color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                minLines = 8,
                                colors = fieldColors
                            )
                        }

                        VaultItemDomain.SSH_KEY -> {
                            // Key Username / Comment
                            OutlinedTextField(
                                value = uiState.username,
                                onValueChange = { viewModel.updateUsername(it) },
                                label = { Text("User / Host / Key Comment", fontSize = 12.sp) },
                                placeholder = { Text("root@192.168.1.100 or deploy-key", color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                colors = fieldColors
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Private Key
                            OutlinedTextField(
                                value = uiState.secret,
                                onValueChange = { viewModel.updateSecret(it) },
                                label = { Text("Private Key", fontSize = 12.sp) },
                                placeholder = { Text("-----BEGIN OPENSSH PRIVATE KEY-----...", color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                minLines = 6,
                                visualTransformation = if (uiState.isSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    autoCorrectEnabled = false
                                ),
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.toggleSecretVisibility() }) {
                                        Icon(
                                            imageVector = if (uiState.isSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle key visibility",
                                            tint = if (uiState.isSecretVisible) ReefPink else TextMuted
                                        )
                                    }
                                },
                                colors = fieldColors
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Claw Re-Prompt Switch Card ────────────────────────────
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = ReefPink, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Require Master Re-prompt",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Challenge biometric or PIN before revealing secrets.",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = uiState.reprompt,
                                onCheckedChange = { viewModel.toggleReprompt(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ReefPink,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = SurfaceContainerDark
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (uiState.domain == com.clawstack.shellguard.data.repository.VaultItemDomain.PASSWORD) {
                        MultiUriEditorSection(
                            uris = uiState.uris,
                            onAddUri = { viewModel.addUri() },
                            onUpdateUri = { index, value -> viewModel.updateUri(index, value) },
                            onRemoveUri = { index -> viewModel.removeUri(index) },
                            fieldColors = fieldColors
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    if (uiState.domain == com.clawstack.shellguard.data.repository.VaultItemDomain.PASSWORD || uiState.domain == com.clawstack.shellguard.data.repository.VaultItemDomain.NOTE) {
                        AttachmentPickerSection(
                            attachments = uiState.attachments,
                            onLaunchPicker = { attachmentLauncher.launch("*/*") },
                            onRemoveAttachment = { viewModel.removeAttachment(it) }
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // ── Dynamic Custom Fields Editor ──────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CUSTOM FIELDS (${uiState.customFields.size})",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        TextButton(onClick = { showAddFieldDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = BrandClawCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Field", color = BrandClawCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (uiState.customFields.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.customFields.forEach { field ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(SurfaceContainerDark)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = field.type.name,
                                                    color = BrandClawCyan,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.removeCustomField(field.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Field",
                                                    tint = StatusError,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = field.label,
                                            onValueChange = { newLabel -> viewModel.updateCustomField(field.id, newLabel, field.value) },
                                            label = { Text("Label", fontSize = 11.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            colors = fieldColors
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (field.type == CustomFieldType.BOOLEAN) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Enabled State", color = TextPrimary, fontSize = 13.sp)
                                                Switch(
                                                    checked = field.value.toBooleanStrictOrNull() ?: false,
                                                    onCheckedChange = { isChecked ->
                                                        viewModel.updateCustomField(field.id, field.label, isChecked.toString())
                                                    },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = BrandClawCyan
                                                    )
                                                )
                                            }
                                        } else {
                                            OutlinedTextField(
                                                value = field.value,
                                                onValueChange = { newVal -> viewModel.updateCustomField(field.id, field.label, newVal) },
                                                label = { Text("Value", fontSize = 11.sp) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                visualTransformation = if (field.type == CustomFieldType.HIDDEN) PasswordVisualTransformation() else VisualTransformation.None,
                                                keyboardOptions = if (field.type == CustomFieldType.HIDDEN) {
                                                    KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)
                                                } else {
                                                    KeyboardOptions.Default
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = fieldColors
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Tags Section ──────────────────────────────────────────
                    Text(
                        text = "TAGS",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTagInput,
                            onValueChange = { newTagInput = it },
                            placeholder = { Text("Add a tag...", color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = fieldColors
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newTagInput.isNotBlank()) {
                                    viewModel.addTag(newTagInput)
                                    newTagInput = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerDark)
                        ) {
                            Text("Add", color = BrandClawCyan, fontSize = 12.sp)
                        }
                    }

                    if (uiState.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            uiState.tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SurfaceDark)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                                        .padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "#$tag", color = BrandClawCyan, fontSize = 11.sp)
                                        IconButton(
                                            onClick = { viewModel.removeTag(tag) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Clear, contentDescription = "Remove tag", tint = TextMuted, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // ── Add Custom Field Type Dialog ──────────────────────────────────
            if (showAddFieldDialog) {
                var selectedType by remember { mutableStateOf(CustomFieldType.TEXT) }
                var fieldLabel by remember { mutableStateOf("") }
                var fieldValue by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showAddFieldDialog = false },
                    title = { Text("Add Custom Field", color = TextPrimary, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Select Field Type:", color = TextMuted, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            CustomFieldType.values().forEach { type ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedType = type }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedType == type,
                                        onClick = { selectedType = type },
                                        colors = RadioButtonDefaults.colors(selectedColor = ReefPink)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (type) {
                                            CustomFieldType.TEXT -> "Text (Standard string)"
                                            CustomFieldType.HIDDEN -> "Hidden (Masked with eye toggle)"
                                            CustomFieldType.BOOLEAN -> "Boolean (Toggle checkbox)"
                                            CustomFieldType.LINKED -> "Linked (Pointer to another item)"
                                        },
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = fieldLabel,
                                onValueChange = { fieldLabel = it },
                                label = { Text("Field Label", fontSize = 11.sp) },
                                placeholder = { Text("e.g. Employee PIN, Server Port", color = TextMuted) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (selectedType != CustomFieldType.BOOLEAN) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = fieldValue,
                                    onValueChange = { fieldValue = it },
                                    label = { Text("Initial Value", fontSize = 11.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = fieldColors,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val finalValue = if (selectedType == CustomFieldType.BOOLEAN) "false" else fieldValue
                                viewModel.addCustomField(selectedType, fieldLabel, finalValue)
                                showAddFieldDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ReefPink),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add Field", color = TextPrimary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddFieldDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    },
                    containerColor = SurfaceDark,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // ── Password Generator Bottom Sheet ──────────────────────────────
            if (showPasswordGeneratorSheet) {
                PasswordGeneratorSheet(
                    onDismissRequest = { showPasswordGeneratorSheet = false },
                    onPasswordSelected = { generatedPassword ->
                        viewModel.updateSecret(generatedPassword)
                    }
                )
            }
        }
    }
}

@Composable
fun MultiUriEditorSection(
    uris: List<String>,
    onAddUri: () -> Unit,
    onUpdateUri: (Int, String) -> Unit,
    onRemoveUri: (Int) -> Unit,
    fieldColors: androidx.compose.material3.TextFieldColors
) {
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = SurfaceContainerDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Secondary URLs & Domains",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            uris.forEachIndexed { index, uri ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uri,
                        onValueChange = { onUpdateUri(index, it) },
                        placeholder = { Text("https://", color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors
                    )
                    androidx.compose.material3.IconButton(onClick = { onRemoveUri(index) }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove URL", tint = TextSecondary)
                    }
                }
            }

            androidx.compose.material3.OutlinedButton(
                onClick = onAddUri,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ReefPink.copy(alpha = 0.5f)),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = ReefPink)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add URL")
            }
        }
    }
}

@Composable
fun AttachmentPickerSection(
    attachments: List<String>,
    onLaunchPicker: () -> Unit,
    onRemoveAttachment: (String) -> Unit
) {
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = SurfaceContainerDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Encrypted Attachments",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            attachments.forEach { attId ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).background(SurfaceDark, RoundedCornerShape(8.dp)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Attachment Staged", color = TextMuted, fontSize = 12.sp)
                    androidx.compose.material3.IconButton(onClick = { onRemoveAttachment(attId) }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = TextSecondary)
                    }
                }
            }

            androidx.compose.material3.TextButton(
                onClick = onLaunchPicker,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = ReefPink, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add File Attachment", color = ReefPink)
            }
        }
    }
}

@Composable
fun DomainSegment(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) ReefPink.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) ReefPink else TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isSelected) TextPrimary else TextMuted,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
