package com.example.stockelectronico.data.remote

import com.example.stockelectronico.domain.model.Producto

/** CRUD one-shot remoto. No sincroniza ni modifica la fuente local Room. */
interface ProductoRemoteDataSource {
    suspend fun create(producto: Producto): ProductoRemoteResult<Unit>
    suspend fun getById(id: String): ProductoRemoteResult<Producto>
    suspend fun getAll(): ProductoRemoteResult<List<Producto>>
    suspend fun update(producto: Producto): ProductoRemoteResult<Unit>
    suspend fun delete(id: String): ProductoRemoteResult<Unit>
}
