package com.clawstack.shellguard

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clawstack.shellguard.ui.screens.gateway.GatewayScreen
import com.clawstack.shellguard.ui.screens.gateway.GatewayViewModel
import com.clawstack.shellguard.ui.theme.OceanDark
import com.clawstack.shellguard.ui.theme.ShellGuardTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Enforce FLAG_SECURE to prevent screen capture, recording, and task switcher leakage
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            ShellGuardTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = OceanDark
                ) { innerPadding ->
                    val gatewayViewModel: GatewayViewModel = viewModel()
                    GatewayScreen(
                        viewModel = gatewayViewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}
