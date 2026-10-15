package com.example.stockelectronico.ui.external

import com.example.stockelectronico.domain.model.Canal
import com.example.stockelectronico.domain.model.ExternalCatalogProduct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalCatalogImportMapperTest {
    @Test fun `converts USD with deterministic rounding`() {
        assertEquals(9_491L, convertUsdToClp(9.99))
        assertEquals(9_500L, convertUsdToClp(10.0))
        assertEquals(1L, convertUsdToClp(0.001))
    }

    @Test fun `maps external product with trimmed supplier channel UUID and local timestamps`() {
        val mapper = ExternalCatalogImportMapper(now = { 1234L }, newId = { "local-uuid" })
        val mapped = mapper.toProducto(product(), " Tech SPA ", Canal.ONLINE)

        assertEquals("local-uuid", mapped.id)
        assertNotEquals(product().externalId.toString(), mapped.id)
        assertEquals("Mouse", mapped.nombre)
        assertEquals("Description", mapped.descripcion)
        assertEquals("electronics", mapped.categoria)
        assertEquals("Acme", mapped.marca)
        assertEquals("SKU-7", mapped.codigo)
        assertEquals(9_491L, mapped.precio)
        assertEquals(4, mapped.stock)
        assertEquals("Tech SPA", mapped.proveedor)
        assertEquals(Canal.ONLINE, mapped.canal)
        assertEquals(1234L, mapped.createdAt)
        assertEquals(mapped.createdAt, mapped.updatedAt)
    }

    @Test fun `blank suppliers are rejected and IDs are unique per adaptation`() {
        var next = 0
        val mapper = ExternalCatalogImportMapper(newId = { "uuid-${++next}" })
        assertTrue(runCatching { mapper.toProducto(product(), "   ", Canal.AMBOS) }.isFailure)
        assertFalse(mapper.toProducto(product(), "Tech", Canal.SUCURSAL).id == mapper.toProducto(product(), "Tech", Canal.SUCURSAL).id)
    }

    private fun product() = ExternalCatalogProduct(7, "Mouse", "Description", "electronics", 9.99, 4, "Acme", "SKU-7", null, emptyList())
}
