package com.example.stockelectronico.data.external.remote

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DummyJsonProductsService {
    @GET("products")
    suspend fun getProducts(
        @Query("limit") limit: Int,
        @Query("skip") skip: Int
    ): Response<DummyJsonProductsResponseDto>

    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: Int): Response<DummyJsonProductDto>

    companion object {
        const val BASE_URL = "https://dummyjson.com/"
    }
}

@Serializable
data class DummyJsonProductsResponseDto(
    val products: List<DummyJsonProductDto>,
    val total: Int,
    val skip: Int,
    val limit: Int
)

@Serializable
data class DummyJsonProductDto(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val price: Double,
    val stock: Int,
    val brand: String? = null,
    val sku: String? = null,
    val thumbnail: String? = null,
    val images: List<String> = emptyList(),
    val meta: DummyJsonProductMetaDto? = null
)

@Serializable
data class DummyJsonProductMetaDto(
    val barcode: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
