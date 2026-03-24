package com.anurag.eduapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anurag.eduapp.data.local.SharedPreferenceUtils
import com.anurag.eduapp.ui.screens.loginscreen.LoginScreen
import com.anurag.eduapp.ui.screens.loginscreen.UserDetailEntryScreen
import com.anurag.eduapp.ui.screens.loginscreen.viewmodel.UserViewModel

@Composable
fun LoginNavigator() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sharedPreferenceUtils = SharedPreferenceUtils(context)

    // Track logout state
    val logoutTriggered = remember { mutableStateOf(false) }

    // Read login status - recompose when logout happens
    var isLoggedIn by remember {
        mutableStateOf(sharedPreferenceUtils.isLoggedIn())
    }

    // Create ViewModel using factory
    val userViewModel: UserViewModel = hiltViewModel()

    // When logout is triggered, update the login status
    if (logoutTriggered.value) {
        isLoggedIn = sharedPreferenceUtils.isLoggedIn()
    }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn && !logoutTriggered.value) "main" else "login"
    ) {
        composable("login") {
            LoginScreen(
                navController = navController,
                userViewModel = userViewModel
            )
        }
        composable("userDetailEntry") {
            UserDetailEntryScreen(
                navController = navController,
                userViewModel = userViewModel
            )
        }
        composable("main") {
            BottomNavBar(
                onLogout = {
                    logoutTriggered.value = true
                    // Reset the user view model state
                    userViewModel.resetLoginState()
                    userViewModel.resetUserSaveState()

                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
    }
}