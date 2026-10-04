package com.clawstack.shellguard

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Box
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.clawstack.shellguard.ui.theme.OceanDark
import com.clawstack.shellguard.ui.theme.ShellGuardTheme

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
            ShellGuardTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = OceanDark,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    val navController = rememberNavController()
                    val isVaultLocked by appContainer.vaultLockManager.isVaultLocked.collectAsState()

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
                                    onAddItemClick = {
                                        navController.navigate("form/NEW/PASSWORD/new")
                                    },
                                    onLockClick = {
                                        appContainer.vaultLockManager.lockVaultNow()
                                    },
                                    onLogoutClick = {
                                        navController.navigate("gateway") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
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

