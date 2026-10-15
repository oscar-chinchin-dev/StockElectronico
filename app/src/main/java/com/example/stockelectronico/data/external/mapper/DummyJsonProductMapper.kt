package com.example.stockelectronico.data.external.mapper

import com.example.stockelectronico.data.external.remote.DummyJsonProductDto
import com.example.stockelectronico.domain.model.ExternalCatalogProduct

fun DummyJsonProductDto.toExternalCatalogProduct() = ExternalCatalogProduct(
    externalId = id,
    name = title,
    description = description,
    category = category,
    priceUsd = price,
    stock = stock,
    brand = brand,
    code = sku,
    thumbnailUrl = thumbnail,
    imageUrls = images
)
