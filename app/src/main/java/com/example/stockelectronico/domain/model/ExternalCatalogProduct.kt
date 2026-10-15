package com.example.stockelectronico.domain.model

/** Producto recibido desde un catálogo externo; todavía no es inventario local. */
data class ExternalCatalogProduct(
    val externalId: Int,
    val name: String,
    val description: String,
    val category: String,
    val priceUsd: Double,
    val stock: Int,
    val brand: String?,
    val code: String?,
    val thumbnailUrl: String?,
    val imageUrls: List<String>
)

data class ExternalCatalogPage(
    val products: List<ExternalCatalogProduct>,
    val total: Int,
    val skip: Int,
    val limit: Int
)
