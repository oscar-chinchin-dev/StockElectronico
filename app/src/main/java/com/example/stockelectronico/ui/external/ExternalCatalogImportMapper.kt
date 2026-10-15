package com.example.stockelectronico.ui.external

import com.example.stockelectronico.domain.model.Canal
import com.example.stockelectronico.domain.model.ExternalCatalogProduct
import com.example.stockelectronico.domain.model.Producto
import java.util.UUID
import kotlin.math.roundToLong

/** Tipo de cambio académico fijo para la importación desde DummyJSON. */
const val USD_TO_CLP_RATE = 950L

fun convertUsdToClp(priceUsd: Double): Long =
    (priceUsd * USD_TO_CLP_RATE).roundToLong()

/** Adapta un producto temporal del catálogo a un producto local listo para el repository. */
class ExternalCatalogImportMapper(
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    fun toProducto(external: ExternalCatalogProduct, supplier: String, channel: Canal): Producto {
        require(supplier.isNotBlank()) { "proveedor es obligatorio" }
        val timestamp = now()
        return Producto(
            id = newId(),
            nombre = external.name,
            // El repository actual exige estos campos no vacíos; son marcadores explícitos,
            // no datos atribuidos al proveedor externo.
            codigo = external.code?.takeIf { it.isNotBlank() } ?: "SIN-CODIGO",
            categoria = external.category,
            marca = external.brand?.takeIf { it.isNotBlank() } ?: "SIN-MARCA",
            descripcion = external.description,
            precio = convertUsdToClp(external.priceUsd),
            stock = external.stock,
            canal = channel,
            createdAt = timestamp,
            updatedAt = timestamp,
            proveedor = supplier.trim()
        )
    }
}
