package com.example.stockelectronico.data.external.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

/** Configuración REST autoritativa y reutilizable del catálogo externo. */
object ExternalCatalogNetworkFactory {
    private val json = Json { ignoreUnknownKeys = true }

    fun createService(
        baseUrl: String = DummyJsonProductsService.BASE_URL,
        client: OkHttpClient = OkHttpClient.Builder().build()
    ): DummyJsonProductsService = createRetrofit(baseUrl, client)
        .create(DummyJsonProductsService::class.java)

    fun createRetrofit(baseUrl: String, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
