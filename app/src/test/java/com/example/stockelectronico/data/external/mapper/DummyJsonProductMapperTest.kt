package com.example.stockelectronico.data.external.mapper

import com.example.stockelectronico.data.external.remote.DummyJsonProductDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DummyJsonProductMapperTest {
    @Test fun `maps DTO fields without creating local inventory semantics`() {
        val product = DummyJsonProductDto(
            id = 101, title = "Teclado", description = "Externo", category = "electronics",
            price = 49.99, stock = 12, brand = "TestBrand", sku = "TEST-101",
            thumbnail = "https://example.test/thumb.jpg", images = listOf("https://example.test/1.jpg")
        ).toExternalCatalogProduct()

        assertEquals(101, product.externalId)
        assertEquals("Teclado", product.name)
        assertEquals("Externo", product.description)
        assertEquals("electronics", product.category)
        assertEquals(49.99, product.priceUsd, 0.0)
        assertEquals(12, product.stock)
        assertEquals("TestBrand", product.brand)
        assertEquals("TEST-101", product.code)
    }

    @Test fun `keeps absent optional fields null`() {
        val product = DummyJsonProductDto(1, "Título", "Descripción", "category", 1.0, 0)
            .toExternalCatalogProduct()

        assertNull(product.brand)
        assertNull(product.code)
        assertNull(product.thumbnailUrl)
    }
}
