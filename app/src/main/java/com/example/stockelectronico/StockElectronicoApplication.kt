package com.example.stockelectronico

import android.app.Application
import com.example.stockelectronico.di.AppContainer

class StockElectronicoApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // La autenticación y el registro viven durante el proceso, no durante una pantalla.
        appContainer.startRealtimeSync()
    }
}
