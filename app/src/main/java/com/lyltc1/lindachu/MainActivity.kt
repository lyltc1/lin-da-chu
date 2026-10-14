package com.lyltc1.lindachu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lyltc1.lindachu.ui.home.HomeScreen
import com.lyltc1.lindachu.ui.theme.LinDaChuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LinDaChuTheme {
                HomeScreen()
            }
        }
    }
}
