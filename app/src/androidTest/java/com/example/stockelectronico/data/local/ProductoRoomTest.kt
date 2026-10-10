package com.example.stockelectronico.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.stockelectronico.data.local.database.StockElectronicoDatabase
import com.example.stockelectronico.data.local.entity.SyncStatus
import com.example.stockelectronico.data.local.entity.ProductoEntity
import com.example.stockelectronico.data.local.dao.RemoteMergeResult
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

    @Test
    fun transicionesDeSyncSonCondicionalesYElConteoIncluyeBajasPendientes() = runBlocking(Dispatchers.IO) {
        val creado = repository.crearProducto(productoBase())
        val dao = database.productoDao()

        assertEquals(1, dao.marcarComoSincronizadoSiCoincide(creado.id, creado.updatedAt))
        assertEquals(0, dao.marcarComoSincronizadoSiCoincide(creado.id, creado.updatedAt))
        assertEquals(0, dao.observarCantidadPendientes().first())

        clock += 1
        repository.actualizarProducto(creado.copy(nombre = "Nueva versión"))
        assertEquals(0, dao.marcarComoSincronizadoSiCoincide(creado.id, creado.updatedAt))
        assertEquals(1, dao.observarCantidadPendientes().first())

        clock += 1
        repository.marcarEliminacionPendiente(creado.id)
        val tombstone = dao.obtenerPendientesDeSincronizacion().single()
        assertEquals(1, dao.observarCantidadPendientes().first())
        assertEquals(0, dao.eliminarFisicamenteSiEliminacionPendienteCoincide(creado.id, tombstone.updatedAt - 1))
        assertEquals(1, dao.eliminarFisicamenteSiEliminacionPendienteCoincide(creado.id, tombstone.updatedAt))
        assertEquals(0, dao.observarCantidadPendientes().first())
    }

    @Test
    fun mergeRemotoRespetaPendingYLaEliminacionPendiente() = runBlocking(Dispatchers.IO) {
        val dao = database.productoDao()
        dao.insertar(entity("p", 200, SyncStatus.PENDING))
        assertEquals(RemoteMergeResult.KEPT_LOCAL, dao.fusionarRemoto(entity("p", 100, SyncStatus.SYNCED)))
        assertEquals(200, dao.obtenerPorIdIncluyendoEliminados("p")!!.updatedAt)
        assertEquals(RemoteMergeResult.APPLIED, dao.fusionarRemoto(entity("p", 300, SyncStatus.SYNCED)))
        assertEquals(SyncStatus.SYNCED, dao.obtenerPorIdIncluyendoEliminados("p")!!.syncStatus)
        dao.insertar(entity("d", 200, SyncStatus.PENDING_DELETE))
        assertEquals(RemoteMergeResult.KEPT_LOCAL, dao.fusionarRemoto(entity("d", 200, SyncStatus.SYNCED)))
        assertEquals(RemoteMergeResult.APPLIED, dao.fusionarRemoto(entity("d", 300, SyncStatus.SYNCED)))
        assertEquals(SyncStatus.SYNCED, dao.obtenerPorIdIncluyendoEliminados("d")!!.syncStatus)
    }

    @Test
    fun removedRemotoNoBorraPendingPeroConfirmaSyncedYTombstone() = runBlocking(Dispatchers.IO) {
        val dao = database.productoDao()
        dao.insertar(entity("pending", 100, SyncStatus.PENDING))
        dao.insertar(entity("synced", 100, SyncStatus.SYNCED))
        dao.insertar(entity("deleted", 100, SyncStatus.PENDING_DELETE))
        assertEquals(RemoteMergeResult.KEPT_LOCAL, dao.aplicarEliminacionRemota("pending"))
        assertEquals(RemoteMergeResult.DELETED, dao.aplicarEliminacionRemota("synced"))
        assertEquals(RemoteMergeResult.DELETED, dao.aplicarEliminacionRemota("deleted"))
        assertTrue(dao.obtenerPorIdIncluyendoEliminados("pending") != null)
        assertNull(dao.obtenerPorIdIncluyendoEliminados("synced"))
        assertNull(dao.obtenerPorIdIncluyendoEliminados("deleted"))
    }

    @Test
    fun matrizCompletaUpsertRemotoIncluyeEmpatesCanonicos() = runBlocking(Dispatchers.IO) {
        val dao = database.productoDao()
        // Sin local; SYNCED: nuevo, igual y antiguo.
        assertEquals(RemoteMergeResult.APPLIED, dao.fusionarRemoto(entity("none", 10, SyncStatus.SYNCED)))
        dao.insertar(entity("syncedNew", 100, SyncStatus.SYNCED)); dao.fusionarRemoto(entity("syncedNew", 200, SyncStatus.SYNCED))
        dao.insertar(entity("syncedEqual", 100, SyncStatus.SYNCED)); dao.fusionarRemoto(entity("syncedEqual", 100, SyncStatus.SYNCED).copy(nombre = "Canonico"))
        dao.insertar(entity("syncedOld", 200, SyncStatus.SYNCED)); dao.fusionarRemoto(entity("syncedOld", 100, SyncStatus.SYNCED))
        assertEquals(200, dao.obtenerPorIdIncluyendoEliminados("syncedNew")!!.updatedAt)
        assertEquals("Canonico", dao.obtenerPorIdIncluyendoEliminados("syncedEqual")!!.nombre)
        assertEquals(200, dao.obtenerPorIdIncluyendoEliminados("syncedOld")!!.updatedAt)
        // PENDING: nuevo/igual ganan, antiguo queda pendiente.
        dao.insertar(entity("pendingNew", 100, SyncStatus.PENDING)); dao.fusionarRemoto(entity("pendingNew", 200, SyncStatus.SYNCED))
        dao.insertar(entity("pendingEqual", 100, SyncStatus.PENDING)); dao.fusionarRemoto(entity("pendingEqual", 100, SyncStatus.SYNCED).copy(nombre = "Canonico"))
        dao.insertar(entity("pendingOld", 300, SyncStatus.PENDING)); dao.fusionarRemoto(entity("pendingOld", 200, SyncStatus.SYNCED))
        assertEquals(SyncStatus.SYNCED, dao.obtenerPorIdIncluyendoEliminados("pendingNew")!!.syncStatus)
        assertEquals("Canonico", dao.obtenerPorIdIncluyendoEliminados("pendingEqual")!!.nombre)
        assertEquals(SyncStatus.PENDING, dao.obtenerPorIdIncluyendoEliminados("pendingOld")!!.syncStatus)
        // PENDING_DELETE: sólo el remoto estrictamente nuevo restaura la fila.
        dao.insertar(entity("deleteOld", 300, SyncStatus.PENDING_DELETE)); dao.fusionarRemoto(entity("deleteOld", 200, SyncStatus.SYNCED))
        dao.insertar(entity("deleteEqual", 300, SyncStatus.PENDING_DELETE)); dao.fusionarRemoto(entity("deleteEqual", 300, SyncStatus.SYNCED))
        dao.insertar(entity("deleteNew", 300, SyncStatus.PENDING_DELETE)); dao.fusionarRemoto(entity("deleteNew", 400, SyncStatus.SYNCED))
        assertEquals(SyncStatus.PENDING_DELETE, dao.obtenerPorIdIncluyendoEliminados("deleteOld")!!.syncStatus)
        assertEquals(SyncStatus.PENDING_DELETE, dao.obtenerPorIdIncluyendoEliminados("deleteEqual")!!.syncStatus)
        assertEquals(SyncStatus.SYNCED, dao.obtenerPorIdIncluyendoEliminados("deleteNew")!!.syncStatus)
    }

    @Test
    fun empateRemoteCanonicalReemplazaPayloadPendingEnRoom() = runBlocking(Dispatchers.IO) {
        val dao = database.productoDao()
        dao.insertar(entity("equal", 100, SyncStatus.PENDING).copy(nombre = "LOCAL", stock = 10))

        assertEquals(
            RemoteMergeResult.APPLIED,
            dao.fusionarRemoto(entity("equal", 100, SyncStatus.SYNCED).copy(nombre = "REMOTO", stock = 20))
        )

        val stored = dao.obtenerPorIdIncluyendoEliminados("equal")!!
        assertEquals("REMOTO", stored.nombre)
        assertEquals(20, stored.stock)
        assertEquals(100, stored.updatedAt)
        assertEquals(SyncStatus.SYNCED, stored.syncStatus)
    }

    private fun entity(id: String, updatedAt: Long, status: SyncStatus) = ProductoEntity(
        id, "Producto $id", "SKU-$id", "Categoria", "Marca", "", 1, 1,
        Canal.AMBOS, 1, updatedAt, status
    )

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
