package com.example.stockelectronico

import android.app.Application
import com.example.stockelectronico.di.AppContainer

class StockElectronicoApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
