package com.example.stockelectronico.data.remote

import com.example.stockelectronico.domain.model.Producto

/** CRUD one-shot remoto. No sincroniza ni modifica la fuente local Room. */
interface ProductoRemoteDataSource {
    suspend fun create(producto: Producto): ProductoRemoteResult<Unit>
    suspend fun getById(id: String): ProductoRemoteResult<Producto>
    suspend fun getAll(): ProductoRemoteResult<List<Producto>>
    suspend fun update(producto: Producto): ProductoRemoteResult<Unit>
    /** Escritura canónica para la cola local: crea o reemplaza productos/{id}. */
    suspend fun upsert(producto: Producto): ProductoRemoteResult<Unit>
    suspend fun delete(id: String): ProductoRemoteResult<Unit>
    /** Listener lifecycle is owned by the application-level realtime coordinator. */
    fun observeProducts(listener: (ProductoRemoteEvent) -> Unit): ProductoRemoteListenerRegistration =
        ProductoRemoteListenerRegistration { }
    /** Defaults preserve small fakes and non-Firestore implementations; Firestore overrides atomically. */
    suspend fun syncUpsert(producto: Producto): ProductoRemoteSyncResult = when (val result = upsert(producto)) {
        is ProductoRemoteResult.Success -> ProductoRemoteSyncResult.Applied
        is ProductoRemoteResult.Failure -> ProductoRemoteSyncResult.Failure(result.error)
    }
    suspend fun syncDelete(producto: Producto): ProductoRemoteSyncResult = when (val result = delete(producto.id)) {
        is ProductoRemoteResult.Success -> ProductoRemoteSyncResult.Applied
        is ProductoRemoteResult.Failure -> ProductoRemoteSyncResult.Failure(result.error)
    }
}
