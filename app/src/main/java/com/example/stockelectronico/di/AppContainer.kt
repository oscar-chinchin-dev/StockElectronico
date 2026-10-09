package com.example.stockelectronico.di

import android.content.Context
import androidx.room.Room
import com.example.stockelectronico.data.remote.FirebaseConnectionVerifier
import com.example.stockelectronico.data.remote.FirestoreProductoDataSource
import com.example.stockelectronico.data.remote.ProductoRemoteDataSource
import com.example.stockelectronico.data.remote.auth.FirebaseAuthManager
import com.example.stockelectronico.data.local.database.StockElectronicoDatabase
import com.example.stockelectronico.data.repository.LocalProductoRepository
import com.example.stockelectronico.data.repository.SyncingProductoRepository
import com.example.stockelectronico.data.sync.ProductoSyncLocalDataSource
import com.example.stockelectronico.data.sync.ProductoSyncManager
import com.example.stockelectronico.data.sync.RoomProductoSyncLocalDataSource
import com.example.stockelectronico.domain.repository.ProductoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Infraestructura manual y única de la capa local. */
class AppContainer(context: Context) {
    private val database: StockElectronicoDatabase = Room.databaseBuilder(
        context.applicationContext,
        StockElectronicoDatabase::class.java,
        StockElectronicoDatabase.DATABASE_NAME
    ).build()

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val localProductoRepository = LocalProductoRepository(database.productoDao())
    val productoSyncLocalDataSource: ProductoSyncLocalDataSource = RoomProductoSyncLocalDataSource(database.productoDao())

    /** Servicios Firebase aislados de la fuente local de productos. */
    val firebaseAuthManager: FirebaseAuthManager by lazy { FirebaseAuthManager() }
    val firebaseConnectionVerifier: FirebaseConnectionVerifier by lazy {
        FirebaseConnectionVerifier(firebaseAuthManager)
    }
    /** Fuente cloud disponible para etapas futuras; no reemplaza el repositorio Room. */
    val productoRemoteDataSource: ProductoRemoteDataSource by lazy {
        FirestoreProductoDataSource(firebaseAuthManager)
    }
    val productoSyncManager: ProductoSyncManager by lazy {
        ProductoSyncManager(productoSyncLocalDataSource, productoRemoteDataSource)
    }
    /** Repository expuesto a UI: Room responde primero y la subida queda en segundo plano. */
    val productoRepository: ProductoRepository by lazy {
        SyncingProductoRepository(localProductoRepository, productoSyncManager, applicationScope)
    }
}
