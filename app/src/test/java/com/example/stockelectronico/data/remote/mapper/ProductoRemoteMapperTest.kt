package com.example.stockelectronico.data.remote.mapper

import com.example.stockelectronico.data.remote.ProductoRemoteError
import com.example.stockelectronico.data.remote.ProductoRemoteResult
import com.example.stockelectronico.data.remote.model.ProductoRemote
import com.example.stockelectronico.domain.model.Canal
import com.example.stockelectronico.domain.model.Producto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductoRemoteMapperTest {
    @Test fun `producto valido genera contrato de once campos sin id ni sync status`() {
        val result = ProductoRemoteMapper.toRemote(producto(precio = 9_223_372_036L, canal = Canal.AMBOS, proveedor = "Proveedor X"))
        val remote = assertSuccess(result)
        val payload = remote.toFirestoreMap()

        assertEquals(11, payload.size)
        assertEquals(9_223_372_036L, payload[ProductoRemote.FIELD_PRECIO])
        assertEquals(7, payload[ProductoRemote.FIELD_STOCK])
        assertEquals("AMBOS", payload[ProductoRemote.FIELD_CANAL])
        assertEquals("Proveedor X", payload[ProductoRemote.FIELD_PROVEEDOR])
        assertFalse(payload.containsKey("id"))
        assertFalse(payload.containsKey("syncStatus"))
    }

    @Test fun `todos los canales se convierten correctamente`() {
        Canal.entries.forEach { canal ->
            assertEquals(canal.name, assertSuccess(ProductoRemoteMapper.toRemote(producto(canal = canal))).canal)
        }
    }

    @Test fun `campos validos reconstruyen producto usando document ID`() {
        val fields = assertSuccess(ProductoRemoteMapper.toRemote(producto())).toFirestoreMap()
        val mapped = assertSuccess(ProductoRemoteMapper.toProducto("document-id", fields))

        assertEquals("document-id", mapped.id)
        assertEquals(1_234_567_890_123L, mapped.precio)
        assertEquals(7, mapped.stock)
        assertEquals(Canal.SUCURSAL, mapped.canal)
        assertEquals("Proveedor X", mapped.proveedor)
    }

    @Test fun `documento legacy sin proveedor es valido y usa cadena vacia`() {
        val legacyFields = assertSuccess(ProductoRemoteMapper.toRemote(producto())).toFirestoreMap() - ProductoRemote.FIELD_PROVEEDOR

        assertEquals("", assertSuccess(ProductoRemoteMapper.toProducto("legacy-id", legacyFields)).proveedor)
    }

    @Test fun `proveedor remoto presente con tipo invalido devuelve error controlado`() {
        val fields = assertSuccess(ProductoRemoteMapper.toRemote(producto())).toFirestoreMap() +
            (ProductoRemote.FIELD_PROVEEDOR to 123)

        assertInvalidDocument(ProductoRemoteMapper.toProducto("id", fields))
    }

    @Test fun `canal desconocido devuelve error controlado`() {
        val fields = assertSuccess(ProductoRemoteMapper.toRemote(producto())).toFirestoreMap() + (ProductoRemote.FIELD_CANAL to "MAYORISTA")
        assertInvalidDocument(ProductoRemoteMapper.toProducto("id", fields))
    }

    @Test fun `campo obligatorio ausente devuelve error controlado`() {
        val fields = assertSuccess(ProductoRemoteMapper.toRemote(producto())).toFirestoreMap() - ProductoRemote.FIELD_NOMBRE
        assertInvalidDocument(ProductoRemoteMapper.toProducto("id", fields))
    }

    @Test fun `precio y stock negativos son rechazados al escribir`() {
        assertInvalidProduct(ProductoRemoteMapper.toRemote(producto(precio = -1)))
        assertInvalidProduct(ProductoRemoteMapper.toRemote(producto(stock = -1)))
    }

    @Test fun `precio remoto negativo devuelve error controlado`() {
        val fields = assertSuccess(ProductoRemoteMapper.toRemote(producto())).toFirestoreMap() +
            (ProductoRemote.FIELD_PRECIO to -1L)
        assertInvalidDocument(ProductoRemoteMapper.toProducto("id", fields))
    }

    @Test fun `stock remoto negativo devuelve error controlado`() {
        val fields = assertSuccess(ProductoRemoteMapper.toRemote(producto())).toFirestoreMap() +
            (ProductoRemote.FIELD_STOCK to -1L)
        assertInvalidDocument(ProductoRemoteMapper.toProducto("id", fields))
    }

    @Test fun `stock remoto fuera de rango Int devuelve error controlado`() {
        val fields = assertSuccess(ProductoRemoteMapper.toRemote(producto())).toFirestoreMap() +
            (ProductoRemote.FIELD_STOCK to Int.MAX_VALUE.toLong() + 1L)
        assertInvalidDocument(ProductoRemoteMapper.toProducto("id", fields))
    }

    private fun producto(
        precio: Long = 1_234_567_890_123L,
        stock: Int = 7,
        canal: Canal = Canal.SUCURSAL,
        proveedor: String = "Proveedor X"
    ) = Producto(
        id = "producto-id", nombre = "Producto", codigo = "SKU-1", categoria = "Categoría",
        marca = "Marca", descripcion = "", precio = precio, stock = stock, canal = canal,
        createdAt = 1_700_000_000_000L, updatedAt = 1_700_000_000_100L, proveedor = proveedor
    )

    private fun <T> assertSuccess(result: ProductoRemoteResult<T>): T {
        assertTrue(result is ProductoRemoteResult.Success)
        return (result as ProductoRemoteResult.Success).value
    }

    private fun assertInvalidDocument(result: ProductoRemoteResult<*>) {
        assertTrue((result as ProductoRemoteResult.Failure).error is ProductoRemoteError.InvalidRemoteDocument)
    }

    private fun assertInvalidProduct(result: ProductoRemoteResult<*>) {
        assertTrue((result as ProductoRemoteResult.Failure).error is ProductoRemoteError.InvalidProduct)
    }
}
