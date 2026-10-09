package com.example.stockelectronico.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.stockelectronico.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos WHERE syncStatus != 'PENDING_DELETE' ORDER BY nombre COLLATE NOCASE ASC, id ASC")
    fun observarProductosActivos(): Flow<List<ProductoEntity>>

    @Query("""
        SELECT * FROM productos
        WHERE syncStatus != 'PENDING_DELETE'
          AND (LOWER(nombre) LIKE '%' || LOWER(:texto) || '%'
               OR LOWER(codigo) LIKE '%' || LOWER(:texto) || '%')
        ORDER BY nombre COLLATE NOCASE ASC, id ASC
    """)
    fun buscarProductos(texto: String): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE id = :id AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun obtenerProductoActivoPorId(id: String): ProductoEntity?

    @Query("SELECT * FROM productos WHERE id = :id AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    fun observarProductoActivoPorId(id: String): Flow<ProductoEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(producto: ProductoEntity)

    @Update
    suspend fun actualizar(producto: ProductoEntity): Int

    @Query("UPDATE productos SET syncStatus = 'PENDING_DELETE', updatedAt = :updatedAt WHERE id = :id AND syncStatus != 'PENDING_DELETE'")
    suspend fun marcarEliminacionPendiente(id: String, updatedAt: Long): Int

    @Query("DELETE FROM productos WHERE id = :id")
    suspend fun eliminarFisicamentePorId(id: String): Int

    @Query("SELECT * FROM productos WHERE syncStatus = 'PENDING' ORDER BY updatedAt ASC, id ASC")
    suspend fun obtenerPendientes(): List<ProductoEntity>

    @Query("SELECT * FROM productos WHERE syncStatus = 'PENDING_DELETE' ORDER BY updatedAt ASC, id ASC")
    suspend fun obtenerEliminacionesPendientes(): List<ProductoEntity>

    @Query("SELECT * FROM productos WHERE syncStatus IN ('PENDING', 'PENDING_DELETE') ORDER BY updatedAt ASC, id ASC")
    suspend fun obtenerPendientesDeSincronizacion(): List<ProductoEntity>

    @Query("UPDATE productos SET syncStatus = 'SYNCED' WHERE id = :id AND syncStatus = 'PENDING' AND updatedAt = :updatedAt")
    suspend fun marcarComoSincronizadoSiCoincide(id: String, updatedAt: Long): Int

    @Query("DELETE FROM productos WHERE id = :id AND syncStatus = 'PENDING_DELETE' AND updatedAt = :updatedAt")
    suspend fun eliminarFisicamenteSiEliminacionPendienteCoincide(id: String, updatedAt: Long): Int

    @Query("SELECT COUNT(*) FROM productos WHERE syncStatus IN ('PENDING', 'PENDING_DELETE')")
    fun observarCantidadPendientes(): Flow<Int>
}
