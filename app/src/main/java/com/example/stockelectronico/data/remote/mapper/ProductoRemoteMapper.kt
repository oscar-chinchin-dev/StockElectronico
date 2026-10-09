package com.example.stockelectronico.data.remote.mapper

import com.example.stockelectronico.data.remote.ProductoRemoteError
import com.example.stockelectronico.data.remote.ProductoRemoteResult
import com.example.stockelectronico.data.remote.model.ProductoRemote
import com.example.stockelectronico.domain.model.Canal
import com.example.stockelectronico.domain.model.Producto

/** Único límite que conoce los nombres, tipos e invariantes del documento Firestore. */
object ProductoRemoteMapper {
    fun toRemote(producto: Producto): ProductoRemoteResult<ProductoRemote> {
        validate(producto)?.let { return ProductoRemoteResult.Failure(ProductoRemoteError.InvalidProduct(it)) }
        return ProductoRemoteResult.Success(
            ProductoRemote(
                nombre = producto.nombre,
                codigo = producto.codigo,
                categoria = producto.categoria,
                marca = producto.marca,
                descripcion = producto.descripcion,
                precio = producto.precio,
                stock = producto.stock,
                canal = producto.canal.name,
                createdAt = producto.createdAt,
                updatedAt = producto.updatedAt
            )
        )
    }

    fun toProducto(documentId: String, fields: Map<String, Any?>): ProductoRemoteResult<Producto> {
        fun invalid(reason: String) = ProductoRemoteResult.Failure(
            ProductoRemoteError.InvalidRemoteDocument(documentId, reason)
        )
        if (documentId.isBlank()) return invalid("El document ID es obligatorio.")

        val nombre = fields.requiredString(ProductoRemote.FIELD_NOMBRE) ?: return invalid("nombre es obligatorio.")
        val codigo = fields.requiredString(ProductoRemote.FIELD_CODIGO) ?: return invalid("codigo es obligatorio.")
        val categoria = fields.requiredString(ProductoRemote.FIELD_CATEGORIA) ?: return invalid("categoria es obligatorio.")
        val marca = fields.requiredString(ProductoRemote.FIELD_MARCA) ?: return invalid("marca es obligatorio.")
        val descripcion = fields[ProductoRemote.FIELD_DESCRIPCION] as? String
            ?: return invalid("descripcion es obligatoria y debe ser texto.")
        val precio = fields.requiredLong(ProductoRemote.FIELD_PRECIO) ?: return invalid("precio debe ser un entero Long.")
        val stockLong = fields.requiredLong(ProductoRemote.FIELD_STOCK) ?: return invalid("stock debe ser un entero Long.")
        if (stockLong !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) return invalid("stock está fuera del rango Int.")
        val canal = (fields[ProductoRemote.FIELD_CANAL] as? String)
            ?.let { runCatching { Canal.valueOf(it) }.getOrNull() }
            ?: return invalid("canal es desconocido o inválido.")
        val createdAt = fields.requiredLong(ProductoRemote.FIELD_CREATED_AT) ?: return invalid("createdAt debe ser un entero Long.")
        val updatedAt = fields.requiredLong(ProductoRemote.FIELD_UPDATED_AT) ?: return invalid("updatedAt debe ser un entero Long.")

        val producto = Producto(documentId, nombre, codigo, categoria, marca, descripcion, precio, stockLong.toInt(), canal, createdAt, updatedAt)
        validate(producto)?.let { return invalid(it) }
        return ProductoRemoteResult.Success(producto)
    }

    private fun validate(producto: Producto): String? = when {
        producto.id.isBlank() -> "id es obligatorio."
        producto.nombre.isBlank() -> "nombre es obligatorio."
        producto.codigo.isBlank() -> "codigo es obligatorio."
        producto.categoria.isBlank() -> "categoria es obligatoria."
        producto.marca.isBlank() -> "marca es obligatoria."
        producto.precio < 0 -> "precio no puede ser negativo."
        producto.stock < 0 -> "stock no puede ser negativo."
        producto.createdAt < 0 -> "createdAt no puede ser negativo."
        producto.updatedAt < 0 -> "updatedAt no puede ser negativo."
        else -> null
    }

    private fun Map<String, Any?>.requiredString(name: String): String? =
        (this[name] as? String)?.takeIf { it.isNotBlank() }

    private fun Map<String, Any?>.requiredLong(name: String): Long? = when (val value = this[name]) {
        is Long -> value
        is Int -> value.toLong()
        is Short -> value.toLong()
        is Byte -> value.toLong()
        else -> null
    }
}
