package com.clawstack.shellguard.ui.navigation

sealed class Screen(val route: String) {
    data object Gateway : Screen("gateway")
    data object Dashboard : Screen("dashboard")
    data object Scanner : Screen("scanner")
    data object Lock : Screen("lock")

    // Settings Navigation Hierarchy
    data object Settings : Screen("settings")
    data object SettingsSecurity : Screen("settings/security")
    data object SettingsAutofill : Screen("settings/autofill")
    data object SettingsSync : Screen("settings/sync")
    data object SettingsAppearance : Screen("settings/appearance")
    data object SettingsBackup : Screen("settings/backup")
    data object SettingsAbout : Screen("settings/about")

    // Emergency Security Execution
    data object PanicPurgeCountdown : Screen("security/panic_countdown")

    data object Detail : Screen("detail/{domain}/{id}") {
        fun createRoute(domain: String, id: String): String = "detail/$domain/$id"
    }

    data object Form : Screen("form/{action}/{domain}/{id}") {
        fun createRoute(action: String, domain: String, id: String, url: String? = null): String {
            val base = "form/$action/$domain/$id"
            return if (url != null) "$base?url=$url" else base
        }
    }
}
