package com.lakshay.vitalink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lakshay.vitalink.data.Encounter
import com.lakshay.vitalink.ui.DashboardScreen
import com.lakshay.vitalink.ui.LoginScreen
import com.lakshay.vitalink.ui.MonitorScreen
import com.lakshay.vitalink.ui.theme.VitaLinkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VitaLinkTheme {
                val nav = rememberNavController()
                // In-memory holder for the tapped encounter (ponytail: no nav-arg serialization for a 3-screen app).
                val selected = remember { mutableStateOf<Encounter?>(null) }
                NavHost(navController = nav, startDestination = "login") {
                    composable("login") {
                        LoginScreen(onLoggedIn = { nav.navigate("dashboard") })
                    }
                    composable("dashboard") {
                        DashboardScreen(onOpen = {
                            selected.value = it
                            nav.navigate("monitor")
                        })
                    }
                    composable("monitor") {
                        selected.value?.let { enc ->
                            MonitorScreen(enc, onBack = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
