package com.clawstack.shellguard

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.clawstack.shellguard.ui.screens.dashboard.VaultDashboardScreen
import com.clawstack.shellguard.ui.screens.dashboard.VaultDashboardViewModel
import com.clawstack.shellguard.ui.screens.detail.ItemDetailScreen
import com.clawstack.shellguard.ui.screens.detail.ItemDetailViewModel
import com.clawstack.shellguard.ui.screens.form.ItemFormScreen
import com.clawstack.shellguard.ui.screens.form.ItemFormViewModel
import com.clawstack.shellguard.ui.screens.gateway.GatewayScreen
import com.clawstack.shellguard.ui.screens.gateway.GatewayViewModel
import com.clawstack.shellguard.ui.theme.OceanDark
import com.clawstack.shellguard.ui.theme.ShellGuardTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
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
                    val startDestination = if (appContainer.deviceVault.hasActiveSession()) "dashboard" else "gateway"

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
                                    navController.navigate("gateway") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                },
                                onLogoutClick = {
                                    navController.navigate("gateway") {
                                        popUpTo("dashboard") { inclusive = true }
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
                            route = "form/{mode}/{domain}/{id}",
                            arguments = listOf(
                                navArgument("mode") { type = NavType.StringType; defaultValue = "NEW" },
                                navArgument("domain") { type = NavType.StringType; defaultValue = "PASSWORD" },
                                navArgument("id") { type = NavType.StringType; defaultValue = "new" }
                            )
                        ) { backStackEntry ->
                            val mode = backStackEntry.arguments?.getString("mode") ?: "NEW"
                            val domain = backStackEntry.arguments?.getString("domain") ?: "PASSWORD"
                            val itemId = backStackEntry.arguments?.getString("id") ?: "new"

                            val formViewModel: ItemFormViewModel = viewModel(
                                key = "form_${mode}_${domain}_${itemId}",
                                factory = object : ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        return ItemFormViewModel(appContainer, mode, domain, itemId) as T
                                    }
                                }
                            )

                            ItemFormScreen(
                                viewModel = formViewModel,
                                onCancel = { navController.popBackStack() },
                                onSaveSuccess = { _, _ -> navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

