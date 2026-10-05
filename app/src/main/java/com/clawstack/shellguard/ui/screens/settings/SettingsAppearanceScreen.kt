package com.clawstack.shellguard.ui.screens.settings

import android.os.Build
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DensityMedium
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.data.local.ThemeMode
import com.clawstack.shellguard.ui.theme.BorderSubtle
import com.clawstack.shellguard.ui.theme.BrandClawCyan
import com.clawstack.shellguard.ui.theme.OceanDark
import com.clawstack.shellguard.ui.theme.ReefPink
import com.clawstack.shellguard.ui.theme.SurfaceDark
import com.clawstack.shellguard.ui.theme.TextMuted
import com.clawstack.shellguard.ui.theme.TextPrimary
import com.clawstack.shellguard.ui.theme.TextSecondary

@Composable
fun SettingsAppearanceScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings

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
                        text = "Appearance & Display",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Theme mode, dynamic colors & list density",
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
                // ── Section 1: Theme Mode ───────────────────────────────────
                item {
                    Text(
                        text = "THEME MODE",
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
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            ThemeModeOptionRow(
                                title = "System Default",
                                subtitle = "Follow system-wide light/dark schedule",
                                icon = Icons.Default.SettingsBrightness,
                                selected = settings.themeMode == ThemeMode.SYSTEM,
                                onClick = { viewModel.updateThemeMode(ThemeMode.SYSTEM) }
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(1.dp)
                                    .background(BorderSubtle.copy(alpha = 0.5f))
                            )

                            ThemeModeOptionRow(
                                title = "Abyssal Dark",
                                subtitle = "Reef deep contrast palette (battery efficient)",
                                icon = Icons.Default.DarkMode,
                                selected = settings.themeMode == ThemeMode.DARK,
                                onClick = { viewModel.updateThemeMode(ThemeMode.DARK) }
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(1.dp)
                                    .background(BorderSubtle.copy(alpha = 0.5f))
                            )

                            ThemeModeOptionRow(
                                title = "Ocean Mist (Light)",
                                subtitle = "High-key daylight contrast scheme",
                                icon = Icons.Default.LightMode,
                                selected = settings.themeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.updateThemeMode(ThemeMode.LIGHT) }
                            )
                        }
                    }
                }

                // ── Section 2: Material You Dynamic Colors ──────────────────
                item {
                    Text(
                        text = "DYNAMIC COLORS",
                        color = BrandClawCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    val isDynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    val dynamicSubtitle = if (isDynamicSupported) {
                        "Derive dynamic accent palette from system wallpaper (Android 12+)"
                    } else {
                        "Dynamic Material You palette requires Android 12 or newer"
                    }

                    SettingsToggleCard(
                        title = "Material You Dynamic Colors",
                        subtitle = dynamicSubtitle,
                        icon = Icons.Default.Palette,
                        iconColor = Color(0xFFF59E0B),
                        checked = settings.dynamicColors,
                        enabled = isDynamicSupported,
                        onCheckedChange = { viewModel.updateDynamicColors(it) }
                    )
                }

                // ── Section 3: Display Preferences ──────────────────────────
                item {
                    Text(
                        text = "VAULT DISPLAY",
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
                            SettingsToggleRow(
                                title = "Website Favicons",
                                subtitle = "Display website favicons beside matched login items",
                                icon = Icons.Default.Image,
                                iconColor = BrandClawCyan,
                                checked = settings.showFavicons,
                                onCheckedChange = { viewModel.updateShowFavicons(it) }
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(1.dp)
                                    .background(BorderSubtle.copy(alpha = 0.5f))
                            )

                            SettingsToggleRow(
                                title = "Compact Density View",
                                subtitle = "Reduce card padding to fit more items on screen",
                                icon = Icons.Default.DensityMedium,
                                iconColor = ReefPink,
                                checked = settings.compactView,
                                onCheckedChange = { viewModel.updateCompactView(it) }
                            )
                        }
                    }
                }

                // ── Section 4: Brand Accent Swatches ────────────────────────
                item {
                    Text(
                        text = "BRAND PALETTE PREVIEW",
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
                                text = "Reef Modernist Tokens",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Active brand tokens loaded across Compose theme scopes",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ColorSwatchPill(label = "Reef Pink", color = ReefPink)
                                ColorSwatchPill(label = "Claw Cyan", color = BrandClawCyan)
                                ColorSwatchPill(label = "Coral", color = Color(0xFFF97316))
                                ColorSwatchPill(label = "Emerald", color = Color(0xFF10B981))
                                ColorSwatchPill(label = "Purple", color = Color(0xFF8B5CF6))
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ThemeModeOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
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
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (selected) ReefPink.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) ReefPink else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

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

@Composable
private fun SettingsToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderSubtle),
        shape = RoundedCornerShape(14.dp)
    ) {
        SettingsToggleRow(
            title = title,
            subtitle = subtitle,
            icon = icon,
            iconColor = iconColor,
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) TextPrimary else TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ReefPink,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceDark,
                uncheckedBorderColor = BorderSubtle
            )
        )
    }
}

@Composable
private fun ColorSwatchPill(
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, BorderSubtle, CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
