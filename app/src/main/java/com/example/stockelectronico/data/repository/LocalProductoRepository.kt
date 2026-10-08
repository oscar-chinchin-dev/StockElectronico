package com.example.stockelectronico.data.repository

import com.example.stockelectronico.data.local.dao.ProductoDao
import com.example.stockelectronico.data.local.entity.SyncStatus
import com.example.stockelectronico.data.local.mapper.toDomain
import com.example.stockelectronico.data.local.mapper.toEntity
import com.example.stockelectronico.domain.model.Producto
import com.example.stockelectronico.domain.repository.ProductoRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalProductoRepository(
    private val productoDao: ProductoDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) : ProductoRepository {
    override fun observarProductosActivos(): Flow<List<Producto>> =
        productoDao.observarProductosActivos().map { productos -> productos.map { it.toDomain() } }

    override fun buscarProductos(texto: String): Flow<List<Producto>> =
        productoDao.buscarProductos(texto).map { productos -> productos.map { it.toDomain() } }

    override suspend fun obtenerProductoActivoPorId(id: String): Producto? =
        productoDao.obtenerProductoActivoPorId(id)?.toDomain()

    override fun observarProductoActivoPorId(id: String): Flow<Producto?> =
        productoDao.observarProductoActivoPorId(id).map { it?.toDomain() }

    override suspend fun crearProducto(producto: Producto): Producto {
        validar(producto)
        val timestamp = now()
        val creado = producto.copy(
            id = producto.id.ifBlank(newId),
            createdAt = timestamp,
            updatedAt = timestamp
        )
        productoDao.insertar(creado.toEntity(SyncStatus.PENDING))
        return creado
    }

    override suspend fun actualizarProducto(producto: Producto): Producto {
        validar(producto)
        val existente = requireNotNull(productoDao.obtenerProductoActivoPorId(producto.id)) {
            "No existe un producto activo con id=${producto.id}"
        }
        val actualizado = producto.copy(createdAt = existente.createdAt, updatedAt = now())
        check(productoDao.actualizar(actualizado.toEntity(SyncStatus.PENDING)) == 1)
        return actualizado
    }

    override suspend fun marcarEliminacionPendiente(id: String) {
        check(productoDao.marcarEliminacionPendiente(id, now()) == 1) {
            "No existe un producto activo con id=$id"
        }
    }

    override suspend fun eliminarFisicamentePorId(id: String) {
        productoDao.eliminarFisicamentePorId(id)
    }

    override suspend fun obtenerPendientesDeSincronizacion(): List<Producto> =
        productoDao.obtenerPendientesDeSincronizacion().map { it.toDomain() }

    private fun validar(producto: Producto) {
        require(producto.nombre.isNotBlank()) { "nombre es obligatorio" }
        require(producto.codigo.isNotBlank()) { "codigo es obligatorio" }
        require(producto.categoria.isNotBlank()) { "categoria es obligatoria" }
        require(producto.marca.isNotBlank()) { "marca es obligatoria" }
        require(producto.precio >= 0) { "precio no puede ser negativo" }
        require(producto.stock >= 0) { "stock no puede ser negativo" }
    }
}
