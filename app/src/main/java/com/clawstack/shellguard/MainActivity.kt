package com.clawstack.shellguard

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.clawstack.shellguard.ui.scanner.QrScannerScreen
import com.clawstack.shellguard.ui.screens.dashboard.VaultDashboardScreen
import com.clawstack.shellguard.ui.screens.dashboard.VaultDashboardViewModel
import com.clawstack.shellguard.ui.screens.detail.ItemDetailScreen
import com.clawstack.shellguard.ui.screens.detail.ItemDetailViewModel
import com.clawstack.shellguard.ui.screens.form.ItemFormScreen
import com.clawstack.shellguard.ui.screens.form.ItemFormViewModel
import com.clawstack.shellguard.ui.screens.gateway.GatewayScreen
import com.clawstack.shellguard.ui.screens.gateway.GatewayViewModel
import com.clawstack.shellguard.ui.screens.lock.LockScreen
import com.clawstack.shellguard.ui.screens.lock.LockViewModel
import com.clawstack.shellguard.ui.screens.security.PanicPurgeCountdownScreen
import com.clawstack.shellguard.ui.screens.settings.SettingsAboutScreen
import com.clawstack.shellguard.ui.screens.settings.SettingsAppearanceScreen
import com.clawstack.shellguard.ui.screens.settings.SettingsAutofillScreen
import com.clawstack.shellguard.ui.screens.settings.SettingsBackupScreen
import com.clawstack.shellguard.ui.screens.settings.SettingsHubScreen
import com.clawstack.shellguard.ui.screens.settings.SettingsSecurityScreen
import com.clawstack.shellguard.ui.screens.settings.SettingsSyncScreen
import com.clawstack.shellguard.ui.screens.settings.SettingsViewModel
import com.clawstack.shellguard.ui.navigation.Screen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.clawstack.shellguard.ui.theme.BorderSubtle
import com.clawstack.shellguard.ui.theme.OceanDark
import com.clawstack.shellguard.ui.theme.ReefPink
import com.clawstack.shellguard.ui.theme.ShellGuardTheme
import com.clawstack.shellguard.ui.theme.SurfaceDark
import com.clawstack.shellguard.ui.theme.TextMuted
import com.clawstack.shellguard.ui.theme.TextPrimary

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Enforce FLAG_SECURE in release builds to prevent screen capture & task switcher leakage
        // In debug builds, skip FLAG_SECURE so ADB screencap works and legacy GPU drivers don't black out on IME
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        val appContainer = (application as ShellGuardApp).appContainer

        setContent {
            val appSettings by appContainer.settingsRepository.settingsFlow.collectAsState(
                initial = com.clawstack.shellguard.data.local.AppSettings()
            )

            val isDarkTheme = when (appSettings.themeMode) {
                com.clawstack.shellguard.data.local.ThemeMode.DARK -> true
                com.clawstack.shellguard.data.local.ThemeMode.LIGHT -> false
                com.clawstack.shellguard.data.local.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            LaunchedEffect(appSettings.allowScreenCapture) {
                if (appSettings.allowScreenCapture) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else if (!BuildConfig.DEBUG) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                }
            }

            ShellGuardTheme(
                darkTheme = isDarkTheme,
                dynamicColor = appSettings.dynamicColors
            ) {
                val navController = rememberNavController()
                val isVaultLocked by appContainer.vaultLockManager.isVaultLocked.collectAsState()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = !isVaultLocked &&
                    (currentRoute == Screen.Dashboard.route || currentRoute == Screen.Settings.route)

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = OceanDark,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (showBottomBar) {
                            Column {
                                HorizontalDivider(thickness = 1.dp, color = BorderSubtle)
                                NavigationBar(
                                    containerColor = SurfaceDark,
                                    tonalElevation = 0.dp
                                ) {
                                    val isVaultSelected = currentRoute == Screen.Dashboard.route
                                    NavigationBarItem(
                                        selected = isVaultSelected,
                                        onClick = {
                                            navController.navigate(Screen.Dashboard.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (isVaultSelected) Icons.Filled.Lock else Icons.Outlined.Lock,
                                                contentDescription = "Vault"
                                            )
                                        },
                                        label = { Text("Vault") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ReefPink,
                                            selectedTextColor = TextPrimary,
                                            indicatorColor = ReefPink.copy(alpha = 0.16f),
                                            unselectedIconColor = TextMuted,
                                            unselectedTextColor = TextMuted
                                        )
                                    )

                                    val isSettingsSelected = currentRoute == Screen.Settings.route
                                    NavigationBarItem(
                                        selected = isSettingsSelected,
                                        onClick = {
                                            navController.navigate(Screen.Settings.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSettingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                                                contentDescription = "Settings"
                                            )
                                        },
                                        label = { Text("Settings") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ReefPink,
                                            selectedTextColor = TextPrimary,
                                            indicatorColor = ReefPink.copy(alpha = 0.16f),
                                            unselectedIconColor = TextMuted,
                                            unselectedTextColor = TextMuted
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    val startDestination = if (appContainer.deviceVault.hasActiveSession()) {
                        "dashboard"
                    } else {
                        "gateway"
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            composable("gateway") {
                                val gatewayViewModel: GatewayViewModel = viewModel(
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return GatewayViewModel(appContainer) as T
                                        }
                                    }
                                )

                                GatewayScreen(
                                    viewModel = gatewayViewModel,
                                    onLoginSuccess = { _, _ ->
                                        navController.navigate("dashboard") {
                                            popUpTo("gateway") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("dashboard") {
                                val dashboardViewModel: VaultDashboardViewModel = viewModel(
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return VaultDashboardViewModel(appContainer) as T
                                        }
                                    }
                                )

                                VaultDashboardScreen(
                                    viewModel = dashboardViewModel,
                                    onItemClick = { item ->
                                        navController.navigate("detail/${item.domain.name}/${item.id}")
                                    },
                                    onAddItemClick = { domain ->
                                        navController.navigate("form/NEW/${domain.name}/new")
                                    },
                                    onLockClick = {
                                        appContainer.vaultLockManager.lockVaultNow()
                                    }
                                )
                            }

                            composable(Screen.Settings.route) {
                                val settingsViewModel: SettingsViewModel = viewModel(
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return SettingsViewModel(appContainer) as T
                                        }
                                    }
                                )

                                SettingsHubScreen(
                                    viewModel = settingsViewModel,
                                    onNavigateToSecurity = { navController.navigate(Screen.SettingsSecurity.route) },
                                    onNavigateToAutofill = { navController.navigate(Screen.SettingsAutofill.route) },
                                    onNavigateToSync = { navController.navigate(Screen.SettingsSync.route) },
                                    onNavigateToAppearance = { navController.navigate(Screen.SettingsAppearance.route) },
                                    onNavigateToBackup = { navController.navigate(Screen.SettingsBackup.route) },
                                    onNavigateToAbout = { navController.navigate(Screen.SettingsAbout.route) },
                                    onLogoutClick = {
                                        navController.navigate("gateway") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Screen.SettingsAppearance.route) {
                                val settingsViewModel: SettingsViewModel = viewModel(
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return SettingsViewModel(appContainer) as T
                                        }
                                    }
                                )

                                SettingsAppearanceScreen(
                                    viewModel = settingsViewModel,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.SettingsSync.route) {
                                val settingsViewModel: SettingsViewModel = viewModel(
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return SettingsViewModel(appContainer) as T
                                        }
                                    }
                                )

                                SettingsSyncScreen(
                                    viewModel = settingsViewModel,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.SettingsSecurity.route) {
                                val settingsViewModel: SettingsViewModel = viewModel(
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return SettingsViewModel(appContainer) as T
                                        }
                                    }
                                )

                                SettingsSecurityScreen(
                                    viewModel = settingsViewModel,
                                    onBackClick = { navController.popBackStack() },
                                    onNavigateToPanicCountdown = { navController.navigate(Screen.PanicPurgeCountdown.route) }
                                )
                            }

                            composable(Screen.PanicPurgeCountdown.route) {
                                val settingsViewModel: SettingsViewModel = viewModel(
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return SettingsViewModel(appContainer) as T
                                        }
                                    }
                                )

                                PanicPurgeCountdownScreen(
                                    viewModel = settingsViewModel,
                                    onCancel = { navController.popBackStack() },
                                    onPurgeCompleted = {
                                        navController.navigate("gateway") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Screen.SettingsAutofill.route) {
                                val settingsViewModel: SettingsViewModel = viewModel(
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return SettingsViewModel(appContainer) as T
                                        }
                                    }
                                )

                                SettingsAutofillScreen(
                                    viewModel = settingsViewModel,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.SettingsBackup.route) {
                                val settingsViewModel: SettingsViewModel = viewModel(
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return SettingsViewModel(appContainer) as T
                                        }
                                    }
                                )

                                SettingsBackupScreen(
                                    viewModel = settingsViewModel,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.SettingsAbout.route) {
                                val settingsViewModel: SettingsViewModel = viewModel(
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return SettingsViewModel(appContainer) as T
                                        }
                                    }
                                )

                                SettingsAboutScreen(
                                    viewModel = settingsViewModel,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = "detail/{domain}/{id}",
                                arguments = listOf(
                                    navArgument("domain") { type = NavType.StringType },
                                    navArgument("id") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val domain = backStackEntry.arguments?.getString("domain") ?: "PASSWORD"
                                val itemId = backStackEntry.arguments?.getString("id") ?: ""

                                val detailViewModel: ItemDetailViewModel = viewModel(
                                    key = "detail_${domain}_${itemId}",
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return ItemDetailViewModel(appContainer, domain, itemId) as T
                                        }
                                    }
                                )

                                ItemDetailScreen(
                                    viewModel = detailViewModel,
                                    onBackClick = { navController.popBackStack() },
                                    onEditClick = { d, i ->
                                        navController.navigate("form/EDIT/$d/$i")
                                    }
                                )
                            }

                            composable(
                                route = "form/{mode}/{domain}/{id}?url={url}",
                                arguments = listOf(
                                    navArgument("mode") { type = NavType.StringType; defaultValue = "NEW" },
                                    navArgument("domain") { type = NavType.StringType; defaultValue = "PASSWORD" },
                                    navArgument("id") { type = NavType.StringType; defaultValue = "new" },
                                    navArgument("url") { type = NavType.StringType; nullable = true; defaultValue = null }
                                ),
                                deepLinks = listOf(
                                    navDeepLink { uriPattern = "shellguard://app/form/{mode}/{domain}/{id}?url={url}" }
                                )
                            ) { backStackEntry ->
                                val mode = backStackEntry.arguments?.getString("mode") ?: "NEW"
                                val domain = backStackEntry.arguments?.getString("domain") ?: "PASSWORD"
                                val itemId = backStackEntry.arguments?.getString("id") ?: "new"
                                val deepLinkUrl = backStackEntry.arguments?.getString("url")

                                val formViewModel: ItemFormViewModel = viewModel(
                                    key = "form_${mode}_${domain}_${itemId}",
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return ItemFormViewModel(appContainer, mode, domain, itemId) as T
                                        }
                                    }
                                )

                                LaunchedEffect(deepLinkUrl) {
                                    if (!deepLinkUrl.isNullOrBlank() && formViewModel.uiState.value.url.isBlank()) {
                                        formViewModel.updateUrl(deepLinkUrl)
                                        if (formViewModel.uiState.value.title.isBlank()) {
                                            val cleanTitle = deepLinkUrl
                                                .removePrefix("https://")
                                                .removePrefix("http://")
                                                .removePrefix("androidapp://")
                                                .substringBefore("/")
                                            formViewModel.updateTitle(cleanTitle)
                                        }
                                    }
                                }

                                val scannedSecret = backStackEntry.savedStateHandle.get<String>("scanned_totp_secret")
                                val scannedTitle = backStackEntry.savedStateHandle.get<String>("scanned_totp_title")
                                val scannedIssuer = backStackEntry.savedStateHandle.get<String>("scanned_totp_issuer")
                                LaunchedEffect(scannedSecret) {
                                    if (!scannedSecret.isNullOrBlank()) {
                                        formViewModel.updateTotpSecret(scannedSecret)
                                        if (formViewModel.uiState.value.title.isBlank()) {
                                            val titleToUse = when {
                                                !scannedIssuer.isNullOrBlank() && !scannedTitle.isNullOrBlank() -> "$scannedIssuer ($scannedTitle)"
                                                !scannedIssuer.isNullOrBlank() -> scannedIssuer
                                                !scannedTitle.isNullOrBlank() -> scannedTitle
                                                else -> ""
                                            }
                                            if (titleToUse.isNotBlank()) {
                                                formViewModel.updateTitle(titleToUse)
                                            }
                                        }
                                        backStackEntry.savedStateHandle.remove<String>("scanned_totp_secret")
                                        backStackEntry.savedStateHandle.remove<String>("scanned_totp_title")
                                        backStackEntry.savedStateHandle.remove<String>("scanned_totp_issuer")
                                    }
                                }

                                ItemFormScreen(
                                    viewModel = formViewModel,
                                    onCancel = { navController.popBackStack() },
                                    onSaveSuccess = { _, _ -> navController.popBackStack() },
                                    onScanQrClick = { navController.navigate("scanner") }
                                )
                            }

                            composable("scanner") {
                                QrScannerScreen(
                                    onCodeScanned = { parsed ->
                                        navController.previousBackStackEntry?.savedStateHandle?.set("scanned_totp_secret", parsed.secret)
                                        if (parsed.issuer.isNotBlank()) {
                                            navController.previousBackStackEntry?.savedStateHandle?.set("scanned_totp_issuer", parsed.issuer)
                                        }
                                        if (parsed.title.isNotBlank()) {
                                            navController.previousBackStackEntry?.savedStateHandle?.set("scanned_totp_title", parsed.title)
                                        }
                                        navController.popBackStack()
                                    },
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable("lock") {
                                LaunchedEffect(Unit) {
                                    appContainer.vaultLockManager.lockVaultNow()
                                }
                            }
                        }

                        // Global Lock Screen Overlay (preserves deep links and backstack upon unlock)
                        if (isVaultLocked && appContainer.deviceVault.hasActiveSession()) {
                            val lockViewModel: LockViewModel = viewModel(
                                factory = object : ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        return LockViewModel(appContainer) as T
                                    }
                                }
                            )

                            LockScreen(
                                viewModel = lockViewModel,
                                onUnlocked = {
                                    // VaultLockManager.unlockVault() causes isVaultLocked to emit false,
                                    // dismissing this overlay automatically.
                                },
                                onFallbackToGateway = {
                                    navController.navigate("gateway") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onPause() {
        super.onPause()
        (application as? ShellGuardApp)?.appContainer?.vaultLockManager?.onAppBackgrounded()
    }

    override fun onResume() {
        super.onResume()
        (application as? ShellGuardApp)?.appContainer?.vaultLockManager?.onAppForegrounded()
    }
}

