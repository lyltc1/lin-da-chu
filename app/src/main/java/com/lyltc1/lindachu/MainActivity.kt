package com.lyltc1.lindachu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lyltc1.lindachu.ui.home.HomeScreen
import com.lyltc1.lindachu.ui.home.HomeViewModel
import com.lyltc1.lindachu.ui.home.HomeViewModelFactory
import com.lyltc1.lindachu.ui.theme.LinDaChuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LinDaChuApp).container
        setContent {
            LinDaChuTheme {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModelFactory(
                        repository = container.dishRepository,
                        appContext = applicationContext
                    )
                )
                HomeScreen(viewModel = homeViewModel)
            }
        }
    }
}
