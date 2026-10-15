package com.example.stockelectronico.domain.repository

import com.example.stockelectronico.domain.model.ExternalCatalogPage
import com.example.stockelectronico.domain.model.ExternalCatalogProduct
import java.io.IOException

interface ExternalCatalogRepository {
    suspend fun getProducts(limit: Int, skip: Int): ExternalCatalogResult<ExternalCatalogPage>
    suspend fun getProduct(id: Int): ExternalCatalogResult<ExternalCatalogProduct>
}

sealed interface ExternalCatalogResult<out T> {
    data class Success<T>(val value: T) : ExternalCatalogResult<T>
    data class Failure(val error: ExternalCatalogError) : ExternalCatalogResult<Nothing>
}

sealed interface ExternalCatalogError {
    data class Http(val statusCode: Int, val message: String?) : ExternalCatalogError
    data class Network(val cause: IOException) : ExternalCatalogError
    data class Serialization(val cause: Throwable) : ExternalCatalogError
    data class InvalidResponse(val reason: String) : ExternalCatalogError
}
