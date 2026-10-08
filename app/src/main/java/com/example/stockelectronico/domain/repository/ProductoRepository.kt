package com.example.stockelectronico.domain.repository

import com.example.stockelectronico.domain.model.Producto
import kotlinx.coroutines.flow.Flow

interface ProductoRepository {
    fun observarProductosActivos(): Flow<List<Producto>>
    fun buscarProductos(texto: String): Flow<List<Producto>>
    suspend fun obtenerProductoActivoPorId(id: String): Producto?
    fun observarProductoActivoPorId(id: String): Flow<Producto?>
    suspend fun crearProducto(producto: Producto): Producto
    suspend fun actualizarProducto(producto: Producto): Producto
    suspend fun marcarEliminacionPendiente(id: String)
    suspend fun eliminarFisicamentePorId(id: String)
    suspend fun obtenerPendientesDeSincronizacion(): List<Producto>
}
