package com.example.stockelectronico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.stockelectronico.ui.navigation.StockElectronicoNavHost
import com.example.stockelectronico.ui.theme.StockElectronicoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StockElectronicoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StockElectronicoNavHost(
                        productoRepository = (application as StockElectronicoApplication)
                            .appContainer.productoRepository,
                        productoSyncManager = (application as StockElectronicoApplication)
                            .appContainer.productoSyncManager,
                        productoSyncLocalDataSource = (application as StockElectronicoApplication)
                            .appContainer.productoSyncLocalDataSource
                    )
                }
            }
        }
    }
}
