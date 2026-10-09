package com.example.stockelectronico.data.sync

/** Resumen de una pasada de subida; los cambios concurrentes se cuentan como pendientes. */
data class ProductoSyncResult(
    val uploaded: Int = 0,
    val deleted: Int = 0,
    val failed: Int = 0,
    val stillPending: Int = 0
)

sealed interface ProductoSyncState {
    data object Idle : ProductoSyncState
    data object Syncing : ProductoSyncState
    data class Completed(val result: ProductoSyncResult) : ProductoSyncState
}
