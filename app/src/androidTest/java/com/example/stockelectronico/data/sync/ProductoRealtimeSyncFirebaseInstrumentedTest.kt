package com.example.stockelectronico.data.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.stockelectronico.StockElectronicoApplication
import com.example.stockelectronico.data.local.database.StockElectronicoDatabase
import com.example.stockelectronico.data.local.entity.SyncStatus
import com.example.stockelectronico.domain.model.Canal
import com.example.stockelectronico.domain.model.Producto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Real opt-in infrastructure test: Firestore listener -> coordinator -> isolated Room. */
@RunWith(AndroidJUnit4::class)
class ProductoRealtimeSyncFirebaseInstrumentedTest {
    @Test fun firestoreChangesConvergeIntoRoom() = runBlocking {
        if (InstrumentationRegistry.getArguments().getString("etapa9Firebase") != "true") return@runBlocking
        val app = ApplicationProvider.getApplicationContext<StockElectronicoApplication>()
        val database = Room.inMemoryDatabaseBuilder(app, StockElectronicoDatabase::class.java).build()
        val local = RoomProductoSyncLocalDataSource(database.productoDao())
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val manager = ProductoRealtimeSyncManager(app.appContainer.firebaseAuthManager, app.appContainer.productoRemoteDataSource, local, scope)
        val remote = app.appContainer.productoRemoteDataSource
        remote.delete(ID)
        try {
            manager.start()
            await("listener") { manager.state.value is ProductoRealtimeState.Listening }
            remote.create(producto(100)).requireSuccess()
            await("added") { database.productoDao().obtenerPorIdIncluyendoEliminados(ID)?.updatedAt == 100L }
            assertEquals(SyncStatus.SYNCED, database.productoDao().obtenerPorIdIncluyendoEliminados(ID)!!.syncStatus)
            remote.update(producto(200, "Modificado")).requireSuccess()
            await("modified") { database.productoDao().obtenerPorIdIncluyendoEliminados(ID)?.updatedAt == 200L }
            remote.delete(ID).requireSuccess()
            await("removed") { database.productoDao().obtenerPorIdIncluyendoEliminados(ID) == null }
            assertNull(database.productoDao().obtenerPorIdIncluyendoEliminados(ID))
        } finally {
            manager.stop(); remote.delete(ID); scope.cancel(); database.close()
        }
    }

    private suspend fun await(label: String, predicate: suspend () -> Boolean) {
        repeat(50) { if (predicate()) return; delay(100) }
        error("Timeout esperando $label")
    }
    private fun producto(updatedAt: Long, nombre: String = "Etapa 9") = Producto(
        ID, nombre, "E9-001", "Pruebas", "StockElectronico", "Temporal", 1, 1,
        Canal.AMBOS, 1, updatedAt
    )
    private fun com.example.stockelectronico.data.remote.ProductoRemoteResult<Unit>.requireSuccess() {
        check(this is com.example.stockelectronico.data.remote.ProductoRemoteResult.Success)
    }
    private companion object { const val ID = "etapa9_realtime_test" }
}
