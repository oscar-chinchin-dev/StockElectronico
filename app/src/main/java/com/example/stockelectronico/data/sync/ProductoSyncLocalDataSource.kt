package com.example.stockelectronico.data.sync

import com.example.stockelectronico.data.local.dao.ProductoDao
import com.example.stockelectronico.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.Flow

/** Acceso técnico a Room que necesita la subida, sin filtrar syncStatus al dominio/UI. */
interface ProductoSyncLocalDataSource {
    suspend fun pendingUploads(): List<ProductoEntity>
    suspend fun pendingDeletes(): List<ProductoEntity>
    suspend fun markSynced(id: String, updatedAt: Long): Int
    suspend fun deleteIfStillPending(id: String, updatedAt: Long): Int
    suspend fun pendingCount(): Int
    fun observePendingCount(): Flow<Int>
}

class RoomProductoSyncLocalDataSource(private val productoDao: ProductoDao) : ProductoSyncLocalDataSource {
    override suspend fun pendingUploads(): List<ProductoEntity> = productoDao.obtenerPendientes()
    override suspend fun pendingDeletes(): List<ProductoEntity> = productoDao.obtenerEliminacionesPendientes()
    override suspend fun markSynced(id: String, updatedAt: Long): Int =
        productoDao.marcarComoSincronizadoSiCoincide(id, updatedAt)

    override suspend fun deleteIfStillPending(id: String, updatedAt: Long): Int =
        productoDao.eliminarFisicamenteSiEliminacionPendienteCoincide(id, updatedAt)

    override suspend fun pendingCount(): Int = productoDao.obtenerPendientesDeSincronizacion().size
    override fun observePendingCount(): Flow<Int> = productoDao.observarCantidadPendientes()
}
