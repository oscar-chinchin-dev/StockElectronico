package com.example.stockelectronico.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.stockelectronico.R

private const val BASE_ROUTE = "base"

@Composable
fun StockElectronicoNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = BASE_ROUTE
    ) {
        composable(BASE_ROUTE) {
            BaseScreen()
        }
    }
}

@Composable
private fun BaseScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.base_screen_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.base_screen_message),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
