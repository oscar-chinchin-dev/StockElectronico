package com.example.stockelectronico.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.stockelectronico.data.local.database.StockElectronicoDatabase
import com.example.stockelectronico.data.local.entity.ProductoEntity
import com.example.stockelectronico.data.local.entity.SyncStatus
import com.example.stockelectronico.domain.model.Canal
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StockElectronicoDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun eliminarBaseDePrueba() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun migracionV1AV2ConservaProductosYEstadosYAgregaProveedorVacio() = runBlocking {
        crearBaseV1RealConDatosRepresentativos()

        val database = Room.databaseBuilder(context, StockElectronicoDatabase::class.java, DATABASE_NAME)
            .addMigrations(StockElectronicoDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        try {
            val productos = listOf("synced", "pending", "deleted").map {
                database.productoDao().obtenerPorIdIncluyendoEliminados(it)!!
            }

            assertEquals(3, productos.size)
            assertProducto(productos[0], "synced", SyncStatus.SYNCED, 100L, 101L)
            assertProducto(productos[1], "pending", SyncStatus.PENDING, 200L, 201L)
            assertProducto(productos[2], "deleted", SyncStatus.PENDING_DELETE, 300L, 301L)
        } finally {
            database.close()
        }
    }

    private fun crearBaseV1RealConDatosRepresentativos() {
        context.deleteDatabase(DATABASE_NAME)
        context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null).use { database ->
            database.execSQL(CREATE_PRODUCTOS_V1)
            database.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            database.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '$V1_IDENTITY_HASH')")
            database.execSQL("PRAGMA user_version = 1")
            insertarProductoV1(database, "synced", "SYNCED", 100L, 101L)
            insertarProductoV1(database, "pending", "PENDING", 200L, 201L)
            insertarProductoV1(database, "deleted", "PENDING_DELETE", 300L, 301L)
        }
    }

    private fun insertarProductoV1(database: android.database.sqlite.SQLiteDatabase, id: String, status: String, createdAt: Long, updatedAt: Long) {
        database.execSQL(
            "INSERT INTO productos (id, nombre, codigo, categoria, marca, descripcion, precio, stock, canal, createdAt, updatedAt, syncStatus) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf(id, "Nombre $id", "SKU-$id", "Categoría", "Marca", "Descripción", 999L, 7, "AMBOS", createdAt, updatedAt, status)
        )
    }

    private fun assertProducto(producto: ProductoEntity, id: String, syncStatus: SyncStatus, createdAt: Long, updatedAt: Long) {
        assertEquals(id, producto.id)
        assertEquals("Nombre $id", producto.nombre)
        assertEquals("SKU-$id", producto.codigo)
        assertEquals("Categoría", producto.categoria)
        assertEquals("Marca", producto.marca)
        assertEquals("Descripción", producto.descripcion)
        assertEquals(999L, producto.precio)
        assertEquals(7, producto.stock)
        assertEquals(Canal.AMBOS, producto.canal)
        assertEquals(createdAt, producto.createdAt)
        assertEquals(updatedAt, producto.updatedAt)
        assertEquals(syncStatus, producto.syncStatus)
        assertEquals("", producto.proveedor)
    }

    private companion object {
        const val DATABASE_NAME = "migration-test.db"
        const val V1_IDENTITY_HASH = "3ecdc60b5fb43041115bbd121601f6bc"
        const val CREATE_PRODUCTOS_V1 = "CREATE TABLE productos (id TEXT NOT NULL, nombre TEXT NOT NULL, codigo TEXT NOT NULL, categoria TEXT NOT NULL, marca TEXT NOT NULL, descripcion TEXT NOT NULL, precio INTEGER NOT NULL, stock INTEGER NOT NULL, canal TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, syncStatus TEXT NOT NULL, PRIMARY KEY(id))"
    }
}
