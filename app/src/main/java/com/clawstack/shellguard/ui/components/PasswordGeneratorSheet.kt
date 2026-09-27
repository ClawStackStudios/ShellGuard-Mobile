package com.clawstack.shellguard.ui.components

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.crypto.PasswordGenerator
import com.clawstack.shellguard.crypto.PasswordOptions
import com.clawstack.shellguard.crypto.PasswordStrength
import com.clawstack.shellguard.crypto.PassphraseOptions
import com.clawstack.shellguard.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorSheet(
    onDismissRequest: () -> Unit,
    onPasswordSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPassphraseMode by remember { mutableStateOf(false) }

    // Password options state
    var length by remember { mutableFloatStateOf(20f) }
    var includeUpper by remember { mutableStateOf(true) }
    var includeLower by remember { mutableStateOf(true) }
    var includeNumbers by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }
    var avoidAmbiguous by remember { mutableStateOf(true) }

    // Passphrase options state
    var wordCount by remember { mutableFloatStateOf(4f) }
    var separator by remember { mutableStateOf("-") }
    var capitalize by remember { mutableStateOf(true) }
    var includeNumberInPhrase by remember { mutableStateOf(true) }

    var generatedPassword by remember { mutableStateOf("") }
    var copiedToastVisible by remember { mutableStateOf(false) }

    LaunchedEffect(copiedToastVisible) {
        if (copiedToastVisible) {
            kotlinx.coroutines.delay(3000L)
            copiedToastVisible = false
        }
    }

    fun refreshPassword() {
        generatedPassword = if (isPassphraseMode) {
            PasswordGenerator.generatePassphrase(
                PassphraseOptions(
                    wordCount = wordCount.toInt(),
                    separator = separator,
                    capitalize = capitalize,
                    includeNumber = includeNumberInPhrase
                )
            )
        } else {
            PasswordGenerator.generatePassword(
                PasswordOptions(
                    length = length.toInt(),
                    includeUppercase = includeUpper,
                    includeLowercase = includeLower,
                    includeNumbers = includeNumbers,
                    includeSymbols = includeSymbols,
                    avoidAmbiguous = avoidAmbiguous
                )
            )
        }
    }

    LaunchedEffect(
        isPassphraseMode, length, includeUpper, includeLower, includeNumbers,
        includeSymbols, avoidAmbiguous, wordCount, separator, capitalize, includeNumberInPhrase
    ) {
        refreshPassword()
    }

    val strength = remember(generatedPassword) {
        PasswordGenerator.evaluateStrength(generatedPassword)
    }

    val strengthColor by animateColorAsState(
        targetValue = when (strength) {
            PasswordStrength.VERY_WEAK -> StatusError
            PasswordStrength.WEAK -> Color(0xFFF97316)
            PasswordStrength.FAIR -> WarningText
            PasswordStrength.STRONG -> BrandClawCyan
            PasswordStrength.VERY_STRONG -> StatusSuccess
        },
        label = "strength_color"
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = BorderSubtle) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Password Generator",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Result Display Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(OceanDark)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = generatedPassword,
                            fontFamily = FontFamily.Monospace,
                            fontSize = if (generatedPassword.length > 28) 15.sp else 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            IconButton(onClick = { refreshPassword() }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Regenerate",
                                    tint = ReefPink
                                )
                            }
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Generated Password", generatedPassword)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    clip.description.extras = PersistableBundle().apply {
                                        putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                                    }
                                }
                                clipboard.setPrimaryClip(clip)
                                copiedToastVisible = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = BrandClawCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Strength Meter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { strength.score / 5f },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = strengthColor,
                            trackColor = BorderSubtle
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = strength.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = strengthColor
                        )
                    }
                }
            }

            if (copiedToastVisible) {
                Spacer(modifier = Modifier.height(8.dp))
                ClipboardToastPill(
                    visible = copiedToastVisible,
                    message = "Password copied to clipboard",
                    autoClearProgress = 1f,
                    remainingSeconds = 30,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !isPassphraseMode,
                    onClick = { isPassphraseMode = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = ReefPink.copy(alpha = 0.2f),
                        activeContentColor = ReefPink,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = TextSecondary
                    )
                ) {
                    Text("Random Characters")
                }
                SegmentedButton(
                    selected = isPassphraseMode,
                    onClick = { isPassphraseMode = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = BrandClawCyan.copy(alpha = 0.2f),
                        activeContentColor = BrandClawCyan,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = TextSecondary
                    )
                ) {
                    Text("Passphrase")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!isPassphraseMode) {
                // Character Mode Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Length", color = TextPrimary, fontSize = 14.sp)
                    Text(
                        "${length.toInt()} characters",
                        color = ReefPink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Slider(
                    value = length,
                    onValueChange = { length = it },
                    valueRange = 8f..64f,
                    steps = 55,
                    colors = SliderDefaults.colors(
                        thumbColor = ReefPink,
                        activeTrackColor = ReefPink,
                        inactiveTrackColor = BorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                GeneratorToggleRow("Uppercase (A-Z)", includeUpper) { includeUpper = it }
                GeneratorToggleRow("Lowercase (a-z)", includeLower) { includeLower = it }
                GeneratorToggleRow("Numbers (0-9)", includeNumbers) { includeNumbers = it }
                GeneratorToggleRow("Symbols (!@#$%^&*)", includeSymbols) { includeSymbols = it }
                GeneratorToggleRow("Avoid Ambiguous (0, O, l, 1)", avoidAmbiguous) { avoidAmbiguous = it }
            } else {
                // Passphrase Mode Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Word Count", color = TextPrimary, fontSize = 14.sp)
                    Text(
                        "${wordCount.toInt()} words",
                        color = BrandClawCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Slider(
                    value = wordCount,
                    onValueChange = { wordCount = it },
                    valueRange = 3f..8f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = BrandClawCyan,
                        activeTrackColor = BrandClawCyan,
                        inactiveTrackColor = BorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Separator selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Separator", color = TextPrimary, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("-", "_", ".", " ").forEach { sep ->
                            val label = if (sep == " ") "Space" else sep
                            FilterChip(
                                selected = separator == sep,
                                onClick = { separator = sep },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandClawCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = BrandClawCyan
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                GeneratorToggleRow("Capitalize Words", capitalize) { capitalize = it }
                GeneratorToggleRow("Include Number", includeNumberInPhrase) { includeNumberInPhrase = it }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = {
                    onPasswordSelected(generatedPassword)
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ReefPink,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use This Password", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GeneratorToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = TextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ReefPink,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = BorderSubtle
            )
        )
    }
}
