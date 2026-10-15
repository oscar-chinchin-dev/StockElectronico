package com.example.stockelectronico.data.external

import com.example.stockelectronico.data.external.remote.DummyJsonProductsService
import com.example.stockelectronico.data.external.remote.ExternalCatalogNetworkFactory
import com.example.stockelectronico.domain.repository.ExternalCatalogError
import com.example.stockelectronico.domain.repository.ExternalCatalogResult
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExternalCatalogRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: ExternalCatalogRepositoryImpl
    private lateinit var service: DummyJsonProductsService

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        service = ExternalCatalogNetworkFactory.createService(
            baseUrl = server.url("/").toString(),
            client = OkHttpClient()
        )
        repository = ExternalCatalogRepositoryImpl(service)
    }

    @After fun tearDown() = server.shutdown()

    @Test fun `lists external products and sends expected GET query`() = runBlocking {
        server.enqueue(jsonResponse(productsResponse()))

        val result = repository.getProducts(limit = 30, skip = 5)

        val page = assertSuccess(result)
        assertEquals(1, page.total)
        assertEquals(0, page.skip)
        assertEquals(30, page.limit)
        assertEquals(101, page.products.single().externalId)
        assertEquals("Teclado Test", page.products.single().name)
        assertEquals(49.99, page.products.single().priceUsd, 0.0)
        assertEquals(listOf("https://example.test/1.jpg", "https://example.test/2.jpg"), page.products.single().imageUrls)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/products?limit=30&skip=5", request.path)
    }

    @Test fun `fetches one external product by id`() = runBlocking {
        server.enqueue(jsonResponse(productJson()))

        val result = repository.getProduct(101)

        assertEquals("TEST-101", assertSuccess(result).code)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/products/101", request.path)
    }

    @Test fun `ignores unknown fields and preserves nested meta parsing`() = runBlocking {
        server.enqueue(jsonResponse(productsResponse().replace(
            "\"meta\":",
            "\"campoQueNoConocemos\": { \"foo\": \"bar\" }, \"meta\":"
        )))

        val result = repository.getProducts(limit = 1, skip = 0)

        assertEquals("TestBrand", assertSuccess(result).products.single().brand)
    }

    @Test fun `deserializes the nested meta object`() = runBlocking {
        server.enqueue(jsonResponse(productJson()))

        val response = service.getProduct(101)

        assertEquals("123456789", response.body()?.meta?.barcode)
        assertEquals("2026-01-01T00:00:00Z", response.body()?.meta?.createdAt)
    }

    @Test fun `allows absent optional product fields`() = runBlocking {
        server.enqueue(jsonResponse("""{"products":[{"id":102,"title":"Simple","description":"Sin opcionales","category":"electronics","price":10.0,"stock":1}],"total":1,"skip":0,"limit":1}"""))

        val product = assertSuccess(repository.getProducts(limit = 1, skip = 0)).products.single()

        assertNull(product.brand)
        assertNull(product.code)
        assertNull(product.thumbnailUrl)
        assertTrue(product.imageUrls.isEmpty())
    }

    @Test fun `maps HTTP errors without turning them into empty results`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("server error"))

        val result = repository.getProducts(limit = 1, skip = 0)

        val failure = result as ExternalCatalogResult.Failure
        assertEquals(500, (failure.error as ExternalCatalogError.Http).statusCode)
    }

    @Test fun `maps invalid JSON to a controlled serialization error`() = runBlocking {
        server.enqueue(jsonResponse("{broken"))

        val result = repository.getProducts(limit = 1, skip = 0)

        val failure = result as ExternalCatalogResult.Failure
        assertTrue(failure.error is ExternalCatalogError.Serialization)
    }

    @Test fun `maps unavailable server to a controlled network error`() = runBlocking {
        server.shutdown()

        val result = repository.getProducts(limit = 1, skip = 0)

        val failure = result as ExternalCatalogResult.Failure
        assertTrue(failure.error is ExternalCatalogError.Network)
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private fun productsResponse(prefix: String = "") = """{
        $prefix
        "products": [${productJson()}], "total": 1, "skip": 0, "limit": 30
    }"""

    private fun productJson() = """{
        "id": 101, "title": "Teclado Test", "description": "Producto externo",
        "category": "electronics", "price": 49.99, "stock": 12, "brand": "TestBrand",
        "sku": "TEST-101", "thumbnail": "https://example.test/thumb.jpg",
        "images": ["https://example.test/1.jpg", "https://example.test/2.jpg"],
        "meta": {"barcode": "123456789", "createdAt": "2026-01-01T00:00:00Z", "updatedAt": "2026-01-02T00:00:00Z"}
    }"""

    private fun <T> assertSuccess(result: ExternalCatalogResult<T>): T {
        assertTrue(result is ExternalCatalogResult.Success)
        return (result as ExternalCatalogResult.Success).value
    }
}
