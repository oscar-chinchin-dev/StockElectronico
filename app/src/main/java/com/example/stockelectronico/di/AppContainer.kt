package com.example.stockelectronico.di

import android.content.Context
import androidx.room.Room
import com.example.stockelectronico.data.remote.FirebaseConnectionVerifier
import com.example.stockelectronico.data.remote.auth.FirebaseAuthManager
import com.example.stockelectronico.data.local.database.StockElectronicoDatabase
import com.example.stockelectronico.data.repository.LocalProductoRepository
import com.example.stockelectronico.domain.repository.ProductoRepository

/** Infraestructura manual y única de la capa local. */
class AppContainer(context: Context) {
    private val database: StockElectronicoDatabase = Room.databaseBuilder(
        context.applicationContext,
        StockElectronicoDatabase::class.java,
        StockElectronicoDatabase.DATABASE_NAME
    ).build()

    val productoRepository: ProductoRepository = LocalProductoRepository(database.productoDao())

    /** Servicios Firebase aislados de la fuente local de productos. */
    val firebaseAuthManager: FirebaseAuthManager by lazy { FirebaseAuthManager() }
    val firebaseConnectionVerifier: FirebaseConnectionVerifier by lazy {
        FirebaseConnectionVerifier(firebaseAuthManager)
    }
}
