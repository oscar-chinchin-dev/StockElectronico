package com.example.stockelectronico.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.stockelectronico.data.local.database.StockElectronicoDatabase
import com.example.stockelectronico.data.local.entity.SyncStatus
import com.example.stockelectronico.data.repository.LocalProductoRepository
import com.example.stockelectronico.domain.model.Canal
import com.example.stockelectronico.domain.model.Producto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductoRoomTest {
    private lateinit var database: StockElectronicoDatabase
    private lateinit var repository: LocalProductoRepository
    private var clock = 1_700_000_000_000L

    @Before
    fun crearBaseEnMemoria() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            StockElectronicoDatabase::class.java
        ).build()
        repository = LocalProductoRepository(
            productoDao = database.productoDao(),
            now = { clock },
            newId = { "uuid-de-prueba" }
        )
    }

    @After
    fun cerrarBase() {
        database.close()
    }

    @Test
    fun operacionesRoomCubrenInventarioBusquedaActualizacionBajaYPendientes() = runBlocking(Dispatchers.IO) {
        val creado = repository.crearProducto(productoBase())

        // Inserción, UUID/timestamps, Long, enum y estado PENDING persistidos correctamente.
        assertEquals("uuid-de-prueba", creado.id)
        assertEquals(clock, creado.createdAt)
        assertEquals(creado.createdAt, creado.updatedAt)
        val pendienteInicial = database.productoDao().obtenerPendientesDeSincronizacion().single()
        assertEquals(987_654_321L, pendienteInicial.precio)
        assertEquals(Canal.AMBOS, pendienteInicial.canal)
        assertEquals(SyncStatus.PENDING, pendienteInicial.syncStatus)

        // Inventario observable, búsqueda por nombre y SKU, y consulta individual activa.
        assertEquals(listOf(creado), repository.observarProductosActivos().first())
        assertEquals(listOf(creado), repository.buscarProductos("teclado").first())
        assertEquals(listOf(creado), repository.buscarProductos("sku-001").first())
        assertEquals(creado, repository.obtenerProductoActivoPorId(creado.id))
        assertEquals(creado, repository.observarProductoActivoPorId(creado.id).first())

        // Actualización preserva createdAt, modifica updatedAt y queda pendiente.
        clock += 1L
        val actualizado = repository.actualizarProducto(
            creado.copy(nombre = "Teclado mecánico actualizado", precio = 100L)
        )
        assertEquals(creado.createdAt, actualizado.createdAt)
        assertEquals(clock, actualizado.updatedAt)
        assertEquals(100L, repository.obtenerProductoActivoPorId(creado.id)?.precio)
        assertEquals(SyncStatus.PENDING, database.productoDao().obtenerPendientesDeSincronizacion().single().syncStatus)

        // PENDING_DELETE se excluye de todas las lecturas activas y continúa en pendientes.
        clock += 1L
        repository.marcarEliminacionPendiente(creado.id)
        assertTrue(repository.observarProductosActivos().first().isEmpty())
        assertTrue(repository.buscarProductos("teclado").first().isEmpty())
        assertNull(repository.obtenerProductoActivoPorId(creado.id))
        assertNull(repository.observarProductoActivoPorId(creado.id).first())
        val pendienteBorrado = database.productoDao().obtenerPendientesDeSincronizacion().single()
        assertEquals(SyncStatus.PENDING_DELETE, pendienteBorrado.syncStatus)
        assertEquals(clock, pendienteBorrado.updatedAt)

        // La baja física es una operación independiente de la baja sincronizable.
        repository.eliminarFisicamentePorId(creado.id)
        assertTrue(database.productoDao().obtenerPendientesDeSincronizacion().isEmpty())
        assertFalse(repository.observarProductosActivos().first().isNotEmpty())
        assertNull(repository.obtenerProductoActivoPorId(creado.id))
    }

    private fun productoBase() = Producto(
        id = "",
        nombre = "Teclado Mecánico",
        codigo = "SKU-001",
        categoria = "Periféricos",
        marca = "Ejemplo",
        descripcion = "",
        precio = 987_654_321L,
        stock = 7,
        canal = Canal.AMBOS,
        createdAt = 0L,
        updatedAt = 0L
    )
}
