package com.example.stockelectronico.data.external

import com.example.stockelectronico.data.external.mapper.toExternalCatalogProduct
import com.example.stockelectronico.data.external.remote.DummyJsonProductsService
import com.example.stockelectronico.domain.model.ExternalCatalogPage
import com.example.stockelectronico.domain.repository.ExternalCatalogError
import com.example.stockelectronico.domain.repository.ExternalCatalogRepository
import com.example.stockelectronico.domain.repository.ExternalCatalogResult
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.Response

class ExternalCatalogRepositoryImpl(
    private val service: DummyJsonProductsService
) : ExternalCatalogRepository {
    override suspend fun getProducts(limit: Int, skip: Int): ExternalCatalogResult<ExternalCatalogPage> =
        request { service.getProducts(limit, skip) }.map { response ->
            ExternalCatalogPage(
                products = response.products.map { it.toExternalCatalogProduct() },
                total = response.total,
                skip = response.skip,
                limit = response.limit
            )
        }

    override suspend fun getProduct(id: Int): ExternalCatalogResult<com.example.stockelectronico.domain.model.ExternalCatalogProduct> =
        request { service.getProduct(id) }.map { it.toExternalCatalogProduct() }

    private suspend fun <T> request(call: suspend () -> Response<T>): ExternalCatalogResult<T> = try {
        val response = call()
        if (response.isSuccessful) {
            response.body()?.let { ExternalCatalogResult.Success(it) }
                ?: ExternalCatalogResult.Failure(ExternalCatalogError.InvalidResponse("Respuesta HTTP exitosa sin cuerpo"))
        } else {
            ExternalCatalogResult.Failure(ExternalCatalogError.Http(response.code(), response.message()))
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: IOException) {
        ExternalCatalogResult.Failure(ExternalCatalogError.Network(exception))
    } catch (exception: SerializationException) {
        ExternalCatalogResult.Failure(ExternalCatalogError.Serialization(exception))
    }

    private fun <T, R> ExternalCatalogResult<T>.map(transform: (T) -> R): ExternalCatalogResult<R> =
        when (this) {
            is ExternalCatalogResult.Success -> ExternalCatalogResult.Success(transform(value))
            is ExternalCatalogResult.Failure -> ExternalCatalogResult.Failure(error)
        }
}
