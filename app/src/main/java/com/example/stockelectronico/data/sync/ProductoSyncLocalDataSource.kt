package com.example.stockelectronico.data.sync

import com.example.stockelectronico.data.local.dao.ProductoDao
import com.example.stockelectronico.data.local.entity.ProductoEntity
import com.example.stockelectronico.data.local.entity.SyncStatus
import com.example.stockelectronico.data.local.mapper.toEntity
import com.example.stockelectronico.data.local.dao.RemoteMergeResult
import com.example.stockelectronico.domain.model.Producto
import kotlinx.coroutines.flow.Flow

/** Acceso técnico a Room que necesita la subida, sin filtrar syncStatus al dominio/UI. */
interface ProductoSyncLocalDataSource {
    suspend fun pendingUploads(): List<ProductoEntity>
    suspend fun pendingDeletes(): List<ProductoEntity>
    suspend fun markSynced(id: String, updatedAt: Long): Int
    suspend fun deleteIfStillPending(id: String, updatedAt: Long): Int
    suspend fun pendingCount(): Int
    fun observePendingCount(): Flow<Int>
    /** Default keeps lightweight legacy fakes safe; Room implementation performs the merge. */
    suspend fun mergeRemote(producto: Producto): RemoteMergeResult = RemoteMergeResult.KEPT_LOCAL
    suspend fun applyRemoteRemoval(id: String): RemoteMergeResult = RemoteMergeResult.KEPT_LOCAL
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
    override suspend fun mergeRemote(producto: Producto): RemoteMergeResult =
        productoDao.fusionarRemoto(producto.toEntity(SyncStatus.SYNCED))
    override suspend fun applyRemoteRemoval(id: String): RemoteMergeResult =
        productoDao.aplicarEliminacionRemota(id)
}
