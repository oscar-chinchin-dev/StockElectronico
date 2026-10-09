package com.example.stockelectronico.data.repository

import com.example.stockelectronico.data.sync.ProductoSyncManager
import com.example.stockelectronico.domain.model.Producto
import com.example.stockelectronico.domain.repository.ProductoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Mantiene el CRUD local inmediato y agenda la subida fuera del ciclo de vida de una pantalla. */
class SyncingProductoRepository(
    private val local: ProductoRepository,
    private val syncManager: ProductoSyncManager,
    private val applicationScope: CoroutineScope
) : ProductoRepository by local {
    override suspend fun crearProducto(producto: Producto): Producto = local.crearProducto(producto).also { requestSync() }
    override suspend fun actualizarProducto(producto: Producto): Producto = local.actualizarProducto(producto).also { requestSync() }
    override suspend fun marcarEliminacionPendiente(id: String) {
        local.marcarEliminacionPendiente(id)
        requestSync()
    }

    private fun requestSync() {
        applicationScope.launch {
            // La subida es secundaria: un fallo nunca revoca el éxito ya confirmado por Room.
            runCatching { syncManager.synchronize() }
        }
    }
}
