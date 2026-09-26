package com.clawstack.shellguard.ui.theme

import androidx.compose.ui.graphics.Color

// Reef Modernist Core Brand Tokens
val ReefPink = Color(0xFFE4048A)
val ReefPinkDark = Color(0xFFC70377)
val ReefPinkLight = Color(0xFFFF40B4)

// Neutral Dark Scales
val OceanDark = Color(0xFF0A0F14)
val SurfaceDark = Color(0xFF111822)
val SurfaceContainerDark = Color(0xFF161F2C)
val SurfaceContainerHighestDark = Color(0xFF1F2B3D)
val BorderSubtle = Color(0xFF1F2937)
val BorderMedium = Color(0xFF374151)

// Typography Text Tokens
val TextPrimary = Color(0xFFF9FAFB)
val TextSecondary = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF6B7280)

// Semantic State Colors
val StatusSuccess = Color(0xFF10B981)
val StatusWarning = Color(0xFFF59E0B)
val StatusError = Color(0xFFEF4444)
val StatusInfo = Color(0xFF3B82F6)

// Curated Theme Accents
enum class ThemeAccent(val displayName: String, val primaryColor: Color) {
    REEF_DEFAULT("Reef Pink", ReefPink),
    OCEAN_CYAN("Ocean Cyan", Color(0xFF00B4D8)),
    EMERALD("Emerald", Color(0xFF10B981)),
    AMBER_GOLD("Amber Gold", Color(0xFFF59E0B)),
    DEEP_VIOLET("Deep Violet", Color(0xFF8B5CF6)),
    CORAL("Coral", Color(0xFFF43F5E))
}
