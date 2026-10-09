package com.example.stockelectronico.data.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.stockelectronico.StockElectronicoApplication
import com.example.stockelectronico.data.local.database.StockElectronicoDatabase
import com.example.stockelectronico.data.local.entity.ProductoEntity
import com.example.stockelectronico.data.local.entity.SyncStatus
import com.example.stockelectronico.data.remote.ProductoRemoteResult
import com.example.stockelectronico.domain.model.Canal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in prueba real aislada; no toca datos de usuario ni corre en la suite normal. */
@RunWith(AndroidJUnit4::class)
class ProductoSyncFirebaseInstrumentedTest {
    @Test fun uploadUpdateAndDeleteAgainstFirestore() = runBlocking {
        if (InstrumentationRegistry.getArguments().getString("etapa8Firebase") != "true") return@runBlocking
        val context = ApplicationProvider.getApplicationContext<StockElectronicoApplication>()
        val remote = context.appContainer.productoRemoteDataSource
        val database = Room.inMemoryDatabaseBuilder(context, StockElectronicoDatabase::class.java).build()
        val dao = database.productoDao()
        val manager = ProductoSyncManager(RoomProductoSyncLocalDataSource(dao), remote, wait = {})
        remote.delete(ID)
        try {
            dao.insertar(entity(SyncStatus.PENDING, precio = 100))
            assertEquals(1, manager.synchronize().uploaded)
            assertEquals(100, remote.product().precio)
            assertEquals(SyncStatus.SYNCED, dao.obtenerProductoActivoPorId(ID)?.syncStatus)

            dao.actualizar(entity(SyncStatus.PENDING, precio = 200, updatedAt = 20))
            assertEquals(1, manager.synchronize().uploaded)
            assertEquals(200, remote.product().precio)

            dao.marcarEliminacionPendiente(ID, 30)
            assertEquals(1, manager.synchronize().deleted)
            assertTrue(remote.getById(ID).isNotFound())
            assertNull(dao.obtenerProductoActivoPorId(ID))
        } finally {
            remote.delete(ID)
            database.close()
        }
    }

    private suspend fun com.example.stockelectronico.data.remote.ProductoRemoteDataSource.product() =
        (getById(ID) as ProductoRemoteResult.Success).value
    private fun ProductoRemoteResult<*>.isNotFound() =
        this is ProductoRemoteResult.Failure && error is com.example.stockelectronico.data.remote.ProductoRemoteError.NotFound
    private fun entity(status: SyncStatus, precio: Long, updatedAt: Long = 10) = ProductoEntity(
        ID, "Etapa 8", "E8-001", "Pruebas", "StockElectronico", "Temporal", precio, 1,
        Canal.AMBOS, 10, updatedAt, status
    )
    companion object { private const val ID = "etapa8_sync_create" }
}
