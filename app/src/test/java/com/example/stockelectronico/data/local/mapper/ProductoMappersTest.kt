package com.example.stockelectronico.data.local.mapper

import com.example.stockelectronico.data.local.entity.SyncStatus
import com.example.stockelectronico.domain.model.Canal
import com.example.stockelectronico.domain.model.Producto
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductoMappersTest {
    @Test
    fun domainAEntityConservaProveedor() {
        val producto = producto(proveedor = "Proveedor local")

        val entity = producto.toEntity(SyncStatus.PENDING)

        assertEquals("Proveedor local", entity.proveedor)
        assertEquals(SyncStatus.PENDING, entity.syncStatus)
    }

    @Test
    fun entityADomainConservaProveedor() {
        val entity = producto(proveedor = "Proveedor local").toEntity(SyncStatus.SYNCED)

        val producto = entity.toDomain()

        assertEquals("Proveedor local", producto.proveedor)
    }

    private fun producto(proveedor: String) = Producto(
        id = "producto-1",
        nombre = "Producto",
        codigo = "SKU-1",
        categoria = "Categoría",
        marca = "Marca",
        descripcion = "Descripción",
        precio = 100L,
        stock = 2,
        canal = Canal.AMBOS,
        createdAt = 10L,
        updatedAt = 20L,
        proveedor = proveedor
    )
}
