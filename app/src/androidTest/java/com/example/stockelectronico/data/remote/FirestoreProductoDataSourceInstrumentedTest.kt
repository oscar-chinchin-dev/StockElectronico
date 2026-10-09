package com.example.stockelectronico.data.remote

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.stockelectronico.StockElectronicoApplication
import com.example.stockelectronico.domain.model.Canal
import com.example.stockelectronico.domain.model.Producto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Pruebas reales, aisladas de Room, para el CRUD remoto de la Etapa 7. */
@RunWith(AndroidJUnit4::class)
class FirestoreProductoDataSourceInstrumentedTest {
    @Test fun remoteCrudCompletesAndCleansUp() = runBlocking {
        val source = source()
        source.delete(PRODUCT_ID)
        try {
            assertSuccess(source.create(producto()))
            assertEquals(PRODUCT_ID, assertSuccess(source.getById(PRODUCT_ID)).id)
            assertTrue(assertSuccess(source.getAll()).any { it.id == PRODUCT_ID })

            assertSuccess(source.update(producto(precio = 12_990, stock = 8, descripcion = UPDATED_DESCRIPTION, updatedAt = UPDATED_AT)))
            val updated = assertSuccess(source.getById(PRODUCT_ID))
            assertEquals(12_990, updated.precio)
            assertEquals(8, updated.stock)
            assertEquals(UPDATED_DESCRIPTION, updated.descripcion)
            assertEquals(CREATED_AT, updated.createdAt)
            assertEquals(UPDATED_AT, updated.updatedAt)
        } finally {
            assertSuccess(source.delete(PRODUCT_ID))
        }
        assertTrue(source.getById(PRODUCT_ID).isNotFound())
    }

    /**
     * Opt-in evidence test. Execute with instrumentation argument etapa7Evidence=true; it creates
     * and updates the document but deliberately leaves it for Firebase Console inspection.
     */
    @Test fun prepareUpdatedDocumentForFirebaseConsole() {
        if (InstrumentationRegistry.getArguments().getString("etapa7Evidence") != "true") return
        runBlocking {
            val source = source()
            source.delete(PRODUCT_ID)
            assertSuccess(source.create(producto()))
            assertSuccess(source.update(producto(precio = 12_990, stock = 8, descripcion = UPDATED_DESCRIPTION, updatedAt = UPDATED_AT)))
        }
    }

    /** Opt-in cleanup companion for the evidence test. */
    @Test fun cleanupFirebaseConsoleEvidence() {
        if (InstrumentationRegistry.getArguments().getString("etapa7Cleanup") != "true") return
        runBlocking { assertSuccess(source().delete(PRODUCT_ID)) }
    }

    private fun source() = ApplicationProvider.getApplicationContext<StockElectronicoApplication>()
        .appContainer.productoRemoteDataSource

    private fun producto(
        precio: Long = 15_990,
        stock: Int = 5,
        descripcion: String = "Producto temporal para validar CRUD remoto",
        updatedAt: Long = CREATED_AT
    ) = Producto(PRODUCT_ID, "Producto Prueba Etapa 7", "ETAPA7-001", "Pruebas", "StockElectronico", descripcion,
        precio, stock, Canal.AMBOS, CREATED_AT, updatedAt)

    private fun <T> assertSuccess(result: ProductoRemoteResult<T>): T {
        assertTrue("Resultado remoto inesperado: $result", result is ProductoRemoteResult.Success)
        return (result as ProductoRemoteResult.Success).value
    }

    private fun ProductoRemoteResult<*>.isNotFound() =
        this is ProductoRemoteResult.Failure && error is ProductoRemoteError.NotFound

    companion object {
        private const val PRODUCT_ID = "etapa7_producto_prueba"
        private const val CREATED_AT = 1_700_000_000_000L
        private const val UPDATED_AT = 1_700_000_000_500L
        private const val UPDATED_DESCRIPTION = "Producto temporal actualizado en Etapa 7"
    }
}
