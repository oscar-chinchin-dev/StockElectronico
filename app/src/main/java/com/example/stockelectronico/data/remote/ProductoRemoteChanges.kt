package com.example.stockelectronico.data.remote

import com.example.stockelectronico.domain.model.Producto

/** Eventos sin tipos Firebase para el coordinador de Room. */
sealed interface ProductoRemoteChange {
    data class Upsert(val producto: Producto) : ProductoRemoteChange
    data class Removed(val id: String) : ProductoRemoteChange
}

sealed interface ProductoRemoteEvent {
    data class Changes(val changes: List<ProductoRemoteChange>) : ProductoRemoteEvent
    data class Error(val error: ProductoRemoteError) : ProductoRemoteEvent
}

fun interface ProductoRemoteListenerRegistration { fun remove() }

sealed interface ProductoRemoteSyncResult {
    data object Applied : ProductoRemoteSyncResult
    data object AlreadyCurrent : ProductoRemoteSyncResult
    /** The remote document is returned so the local queue can converge immediately. */
    data class RemoteNewer(val producto: Producto) : ProductoRemoteSyncResult
    /**
     * The remote document has the same timestamp as the local upload and is the
     * canonical payload. This is deliberately distinct from AlreadyCurrent:
     * callers must merge this payload instead of marking their local copy synced.
     */
    data class RemoteCanonical(val producto: Producto) : ProductoRemoteSyncResult
    data class Failure(val error: ProductoRemoteError) : ProductoRemoteSyncResult
}
