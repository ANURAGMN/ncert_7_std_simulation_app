package com.anurag.eduai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anurag.eduai.data.local.SharedPreferenceUtils
import com.anurag.eduai.ui.screens.login.LoginScreen
import com.anurag.eduai.ui.screens.login.UserDetailEntryScreen
import com.anurag.eduai.ui.screens.loginscreen.LoginScreen
import com.anurag.eduai.ui.screens.loginscreen.UserRegistrationScreen
import com.anurag.eduai.ui.screens.loginscreen.viewmodel.UserViewModel
import com.anurag.eduai.ui.viewModel.UserViewModel
import com.anurag.eduai.ui.viewmodel_factory.UserViewModelFactory

@Composable
fun LoginNavigator() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sharedPreferenceUtils = SharedPreferenceUtils(context)
    val isLoggedIn: Boolean = sharedPreferenceUtils.isLoggedIn()

    // Create ViewModel using factory
    val userViewModel: UserViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "main" else "login"
    ) {
        composable("login") {
            LoginScreen(
                navController = navController,
                userViewModel = userViewModel
            )
        }
        composable("userDetailEntry") {
            UserRegistrationScreen(
                navController = navController,
                userViewModel = userViewModel
            )
        }
        composable("main") {
            BottomNavBar()
        }
    }
}