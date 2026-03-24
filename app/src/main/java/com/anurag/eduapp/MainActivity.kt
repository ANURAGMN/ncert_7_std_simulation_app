package com.anurag.eduapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.anurag.eduapp.ui.navigation.LoginNavigator
import com.anurag.eduapp.ui.theme.AdaptiveTheme
import com.anurag.eduapp.ui.theme.EduAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdaptiveTheme {
                EduAITheme {
                    LoginNavigator()
                }
            }
        }
    }
}
