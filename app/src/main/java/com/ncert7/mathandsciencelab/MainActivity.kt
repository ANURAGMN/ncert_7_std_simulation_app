package com.ncert7.mathandsciencelab

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.ncert7.mathandsciencelab.ui.navigation.LoginNavigator
import com.ncert7.mathandsciencelab.ui.theme.AdaptiveTheme
import com.ncert7.mathandsciencelab.ui.theme.EduAITheme
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
